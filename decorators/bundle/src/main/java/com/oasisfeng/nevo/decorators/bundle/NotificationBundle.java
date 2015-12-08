/*
 * Copyright (C) 2015 The Nevolution Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oasisfeng.nevo.decorators.bundle;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.oasisfeng.nevo.StatusBarNotificationEvo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Notification bundle service
 *
 * Created by Oasis on 2015/1/14.
 */
public class NotificationBundle extends INotificationBundle.Stub {

	private static final String RULE_PREFS_NAME = "bundle.rules";

	@Override public void setRule(final String pkg, final String title, final @Nullable String bundle) throws RemoteException {
		if (bundle != null) mPrefs.edit().putString(buildRuleKey(pkg, title), bundle).apply();
		else mPrefs.edit().remove(buildRuleKey(pkg, title)).apply();
		mDefinedBundles = null;		// Invalidate the cache.
		// TODO: Refresh the
	}

	@Override public String queryRuleForNotification(final StatusBarNotificationEvo sbn) throws RemoteException {
		final CharSequence title = sbn.notification().extras().getCharSequence(NotificationCompat.EXTRA_TITLE);
		return queryRule(sbn.getPackageName(), title == null ? null : title.toString());
	}

	@Override public String queryRule(final String pkg, final String title) throws RemoteException {
		if (title != null) {	// Query package + title first
			final String bundle = mPrefs.getString(buildRuleKey(pkg, title), null);
			if (bundle != null) return bundle;
		}	// Then package-wide rule
		return mPrefs.getString(pkg, null);
	}

	@Override public List<String> getBundledNotificationKeys(final String bundle) {
		final Collection<String> keys = mBundledNotificationKeys.get(bundle);
		if (keys.isEmpty()) return Collections.emptyList();
		return new ArrayList<>(keys);
	}

	@Override public void setNotificationBundle(final String key, final @Nullable String bundle) {
		Preconditions.checkArgument(! TextUtils.isEmpty(key), "key is null or empty");
		final String previous_bundle = bundle != null ? mNotificationBundle.put(key, bundle) : mNotificationBundle.remove(key);
		if (Objects.equal(bundle, previous_bundle)) return;		// Unchanged

		if (previous_bundle != null) {			// Remove from previous bundle
			if (mBundledNotificationKeys.remove(previous_bundle, key)) {
				Log.i(TAG, previous_bundle + ": - " + key);
				save(previous_bundle);
			} else Log.e(TAG, "Internal inconsistency: " + key + " is expected to be but not in " + previous_bundle);
		}
		if (bundle != null) {
			if (mBundledNotificationKeys.put(bundle, key)) {
				Log.i(TAG, bundle + ": + " + key);
				save(bundle);
			} else Log.e(TAG, "Internal inconsistency: " + key + " is already in " + bundle);
			if (BuildConfig.DEBUG) logKeysInBundle(bundle);
		}
		// FIXME: Let nevolution engine observe this
	}

	private void save(final String bundle) {
		final Collection<String> keys = mBundledNotificationKeys.get(bundle);
		final Bundle data = new Bundle();
		data.putStringArrayList(bundle, Lists.newArrayList(keys));
	}

	@Override public List<String> getDefinedBundles() {
		return definedBundles();
	}

	@Override public Map<String, String> getAllRules() {
		final HashMap<String, String> rules = new HashMap<>();
		for (final Map.Entry<String, ?> entry : mPrefs.getAll().entrySet()) {
			if (entry.getValue() instanceof String) rules.put(entry.getKey(), (String) entry.getValue());
		}
		return rules;
	}

	public static String buildRuleKey(final String pkg, final String title) {
		return title == null ? pkg : pkg + ":" + title;
	}

	private List<String> definedBundles() {
		final List<String> defined_bundles = mDefinedBundles;
		if (defined_bundles != null) return defined_bundles;
		final Set<String> bundles = new HashSet<>();
		for (final Object value : mPrefs.getAll().values())
			if (value instanceof String) {
				if (! ((String) value).isEmpty()) bundles.add((String) value);	// Empty value is for "exclusion"
			} else Log.w(TAG, "Invalid value: " + value + " (" + value.getClass() + ")");
		return mDefinedBundles = Collections.unmodifiableList(new ArrayList<>(bundles));
	}

	public NotificationBundle(final Context context) {
		mPrefs = context.getSharedPreferences(RULE_PREFS_NAME, Context.MODE_PRIVATE);
	}

	private void logKeysInBundle(final String bundle) {
		final StringBuilder log = new StringBuilder("Keys in bundle ").append(bundle).append(':');
		for (final String each_key : mBundledNotificationKeys.get(bundle)) {
			log.append(each_key).append(',');
		}
		Log.v(TAG, log.toString());
	}

	private final SharedPreferences mPrefs;
	private volatile List<String> mDefinedBundles;
	private final Multimap<String/* bundle */, String/* key */> mBundledNotificationKeys = Multimaps.synchronizedSetMultimap(LinkedHashMultimap.<String, String>create());
	private final Map<String/* key */, String/* bundle */> mNotificationBundle = Collections.synchronizedMap(new HashMap<String, String>());	// Reverse map

	private static final String TAG = "Nevo.Bundle";
}
