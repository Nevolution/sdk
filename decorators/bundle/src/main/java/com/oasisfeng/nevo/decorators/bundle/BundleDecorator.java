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

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.oasisfeng.android.os.IBundle;
import com.oasisfeng.nevo.INotification;
import com.oasisfeng.nevo.NevoConstants;
import com.oasisfeng.nevo.StatusBarNotificationEvo;
import com.oasisfeng.nevo.decorator.NevoDecoratorService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.content.pm.PackageManager.GET_UNINSTALLED_PACKAGES;

/**
 * Bundle notifications in same category into one "bundle notification"
 *
 * Created by Oasis on 2015/1/5.
 *
 * TODO: Re-bundle notifications a while after explicit expanding
 * TODO: An option to set the priority (with status-bar icon or not)
 * TODO: An option to configure specific bundle as "unswipeable" (can only be removed explicitly via action)
 */
public class BundleDecorator extends NevoDecoratorService {

	static final String ACTION_BUNDLE_EXPAND = "com.oasisfeng.nevo.bundle.action.EXPAND";
	private static final String EXTRA_KEYS = "com.oasisfeng.nevo.bundle.extra.KEYS";	// ArrayList<String>
	private static final String SCHEME_BUNDLE = "bundle";
	private static final String TAG_PREFIX = "B>";

	private static final int MIN_NUM_TO_BUNDLE = 2;

	@Override protected void onConnected() throws Exception {
		super.onConnected();
		Log.i(TAG, "Retrieving active bundles...");
		for (final StatusBarNotificationEvo existent : getMyActiveNotifications()) {
			if (existent.getTag() == null || ! existent.getTag().startsWith(TAG_PREFIX)) continue;
			final String bundle = existent.getTag().substring(TAG_PREFIX.length());
			final List<String> keys = existent.notification().extras().getStringArrayList(EXTRA_KEYS);
			if (keys == null) {
				Log.w(TAG, "Bundle notification without bundled keys: " + existent.getKey());
				continue;
			}
			Log.i(TAG, keys.size() + " in \"" + bundle + "\": " + keys);
			for (final String key : keys)
				mBundles.setNotificationBundle(key, bundle);
		}
	}

	@Override protected void apply(final StatusBarNotificationEvo evolved) throws RemoteException {
		final String bundle = mBundles.queryRuleForNotification(evolved);
		if (bundle == null || bundle.isEmpty()) return;		// No matched rule or configured to be not bundled (empty for exclusion)
		bundle(evolved, bundle);
	}

	@Override protected void onNotificationRemoved(final String key) throws RemoteException {
		if (mPendingRevival.remove(key)) return;		// This removal is caused by the glitch before revival.
		mBundles.setNotificationBundle(key, null);		// Remove it from bundle since it should not be shown in bundle any more.
	}

	private void bundle(final StatusBarNotificationEvo evolving, final String bundle) {
		final String key = evolving.getKey();
		Log.i(TAG, "Bundle into " + bundle + ": " + key);
		mBundles.setNotificationBundle(key, bundle);
		final INotification n = evolving.notification();
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) n.setGroup(TAG_PREFIX + bundle);
			else n.extras().putString("android.support.groupKey", bundle);

			final String token = bundle.intern();
			mHandler.removeCallbacksAndMessages(token);
			mHandler.postAtTime(new Runnable() { @Override public void run() {	// Postpone for quick decoration.
				try {
					showAsBundleIfAppropriate(bundle);
				} catch (final RemoteException e) {
					Log.w(TAG, "Failed to show bundle \"" + bundle + "\" due to " + e);
				}
			}}, token, SystemClock.uptimeMillis());
		} catch (final RemoteException e) {
			Log.w(TAG, "Failed to show bundle \"" + bundle + "\" due to " + e);
		}
	}

	private boolean showAsBundleIfAppropriate(final String bundle) throws RemoteException {
		final List<String> all_keys = mBundles.getBundledNotificationKeys(bundle);
		if (all_keys.size() < MIN_NUM_TO_BUNDLE) {
			Log.d(TAG, "Not showing " + all_keys.size() + " notification(s) as bundle until " + MIN_NUM_TO_BUNDLE + " bundled");
			return false;
		}
		final List<String> keys = all_keys.size() <= 4 ? all_keys : all_keys.subList(all_keys.size() - 4, all_keys.size()); // No more than 4

		final List<StatusBarNotificationEvo> sbns = getMyActiveNotifications(keys);
		if (sbns.size() != keys.size()) {
			Log.w(TAG, sbns.size() + " out of " + keys.size() + " bundled notifications are retrieved successfully.");
			if (sbns.size() < MIN_NUM_TO_BUNDLE) return false;
		}

		// Sort by post time
		final ImmutableList<StatusBarNotificationEvo> sorted_sbns = FluentIterable.from(sbns).toSortedList(
				Ordering.natural().reverse().onResultOf(new Function<StatusBarNotificationEvo, Comparable>() { @Override public Comparable apply(final StatusBarNotificationEvo sbn) {
					return sbn.getPostTime();
				}}));

		final Notification notification = buildBundleNotification(bundle, all_keys.size()/* total number */, keys, sorted_sbns);
		mNotificationManager.notify(TAG_PREFIX + bundle, 0, notification);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH)	// Cancel notification explicitly if group is not supported.
			for (final StatusBarNotificationEvo sbn : sbns)
				cancelNotification(sbn.getKey());
		return true;
	}

	private Notification buildBundleNotification(final String bundle, final int number, final List<String> bundled_keys, final List<StatusBarNotificationEvo> sbns) throws RemoteException {
		final Set<String> bundled_pkgs = new HashSet<>(sbns.size());
		long latest_when = 0;
		for (final StatusBarNotificationEvo sbn : sbns) {
			final long when = sbn.notification().getWhen();
			if (when > latest_when) latest_when = when;
			bundled_pkgs.add(sbn.getPackageName());
		}

		final Builder builder = new Builder(this).setGroup(TAG_PREFIX + bundle).setGroupSummary(true)
				.setContentTitle(bundle).setSmallIcon(R.drawable.ic_notification_bundle)
				.setWhen(latest_when).setAutoCancel(false).setNumber(number)/*.setPriority(PRIORITY_MIN)*/;
		if (bundled_pkgs.size() == 1) {
			final IBundle last_extras = sbns.get(0).notification().extras();
			builder.setContentText(last_extras.getCharSequence(NotificationCompat.EXTRA_TITLE))
					.setSubText(last_extras.getCharSequence(NotificationCompat.EXTRA_TEXT));
		} else builder.setContentText(getSourceNames(bundled_pkgs));

		final Bundle extras = builder.getExtras();
		final ArrayList<String> bundled_key_list = new ArrayList<>(bundled_keys);
		extras.putStringArrayList(EXTRA_KEYS, bundled_key_list);
		extras.putBoolean(NevoConstants.EXTRA_PHANTOM, true);		// Bundle notification should never be evolved or stored.

		// Set on-click pending intent explicitly, to avoid notification drawer collapse when bundle is clicked.
		final Intent click_intent = new Intent(ACTION_BUNDLE_EXPAND).setData(Uri.fromParts(SCHEME_BUNDLE, bundle, null))
				.putStringArrayListExtra(EXTRA_KEYS, bundled_key_list);
		final PendingIntent click_pending_intent = PendingIntent.getBroadcast(this, 0, click_intent, PendingIntent.FLAG_UPDATE_CURRENT);
		final Notification notification;
		if (s1UViewId != 0) {
			notification = builder.build();
			notification.contentView.setOnClickPendingIntent(s1UViewId, click_pending_intent);
		} else {
			builder.setContentIntent(click_pending_intent);	// Fallback to normal content intent (notification drawer will collapse)
			notification = builder.build();
		}

		notification.bigContentView = buildExpandedView(sbns);

		return notification;
	}

	/** Preview the last a few notifications vertically as expanded view of bundle notification. */
	private RemoteViews buildExpandedView(final List<StatusBarNotificationEvo> sbns) {
		if (sbns.isEmpty()) return null;
		final RemoteViews expanded = new RemoteViews(getPackageName(), R.layout.bundle_expanded_notification);

		// Since Lollipop, "reapply()" is used with remote views onto the current one when updating notification.
		// We must clear the view group before adding new content.
		expanded.removeAllViews(R.id.bundle_expanded_container);

		for (final StatusBarNotificationEvo sbn : sbns) try {
			expanded.addView(R.id.bundle_expanded_container, sbn.notification().getContentView());
		} catch (final RemoteException ignored) {}	// Should not happen
		return expanded;
	}

	// TODO: Use static receiver instead to withstand service down-time.
	private final BroadcastReceiver mOnBundleAction = new BroadcastReceiver() {

		@Override public void onReceive(final Context context, final Intent intent) {
			if (intent == null || intent.getData() == null) return;
			if (Binder.getCallingPid() != Process.myPid()) return;
			final String bundle = intent.getData().getSchemeSpecificPart();
			final ArrayList<String> keys = intent.getStringArrayListExtra(EXTRA_KEYS);

			switch (intent.getAction()) {
			case ACTION_BUNDLE_EXPAND:
				if (keys == null || keys.isEmpty()) break;
				Log.d(TAG, "Expanding " + keys.size() + " notifications in bundle: " + bundle);

				for (final String key : keys) try {
					// The removal of group summary notification will cause all the group children being removed too,
					// We track the keys of notification being revived to detect this glitch in onNotificationRemoved().
					mPendingRevival.add(key);
					mHandler.removeCallbacks(mResetPendingRevival);
					mHandler.postDelayed(mResetPendingRevival, 3000);	// Avoid potential leaks

					reviveNotification(key);
				} catch (final RemoteException ignored) {}		// TODO: Try reviving later?
				// Remove the bundle notification after restoration,
				mNotificationManager.cancel(TAG_PREFIX + bundle, 0);
				break;
			}
		}

		public java.lang.Runnable mResetPendingRevival = new Runnable() { @Override public void run() {
			mPendingRevival.clear();
		}};
	};

	// TODO: Reuse names when update
	private String getSourceNames(final Set<String> pkgs) {
		final PackageManager pm = getPackageManager();
		final StringBuilder names = new StringBuilder();
		for (final String pkg : pkgs)
			try { @SuppressWarnings("WrongConstant")
				final ApplicationInfo app_info = pm.getApplicationInfo(pkg, GET_UNINSTALLED_PACKAGES);
				names.append(", ").append(app_info.loadLabel(pm));
			} catch (final NameNotFoundException ignored) {}          // TODO: Packages from other user profiles?
		return names.substring(2);
	}

	@Override public void onCreate() {
		mBundles = new NotificationBundle(this);
		mNotificationManager = NotificationManagerCompat.from(getApplication());
		final IntentFilter filter = new IntentFilter(ACTION_BUNDLE_EXPAND);
		filter.addDataScheme(SCHEME_BUNDLE);
		registerReceiver(mOnBundleAction, filter);
	}

	@Override public void onDestroy() {
		unregisterReceiver(mOnBundleAction);
		super.onDestroy();
	}

	@Override public IBinder onBind(final Intent intent) {
		if (intent != null && INotificationBundle.class.getName().equals(intent.getAction())) return mBundles;
		return super.onBind(intent);
	}

	private NotificationManagerCompat mNotificationManager;
	private NotificationBundle mBundles;
	private final Set<String> mPendingRevival = new HashSet<>();
	private final Handler mHandler = new Handler();

	private static final int s1UViewId = Resources.getSystem().getIdentifier("status_bar_latest_event_content", "id", "android");
	{ if (s1UViewId == 0) Log.w(TAG, "Partially incompatible ROM: android.R.id.status_bar_latest_event_content not found."); }
}
