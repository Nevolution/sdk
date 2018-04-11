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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Binder;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.graphics.ColorUtils;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.oasisfeng.nevo.sdk.MutableNotification;
import com.oasisfeng.nevo.sdk.MutableStatusBarNotification;
import com.oasisfeng.nevo.sdk.NevoDecoratorService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static android.content.pm.PackageManager.GET_UNINSTALLED_PACKAGES;

/**
 * Bundle notifications in same category into one "bundle notification"
 *
 * Created by Oasis on 2015/1/5.
 *
 * TODO: Re-bundle notifications a while after expanding
 * TODO: Show another bundle notification with more notifications if a bundle with more than 4 notifications is swiped.
 * TODO: An option to set the priority (with status-bar icon or not)
 * TODO: An option to configure specific bundle as "unswipeable" (can only be removed explicitly via action)
 */
public class BundleDecorator extends NevoDecoratorService {

	static final String ACTION_BUNDLE_EXPAND = "com.oasisfeng.nevo.bundle.action.EXPAND";
	static final String ACTION_BUNDLE_CLEAR = "com.oasisfeng.nevo.bundle.action.CLEAR";	// For Android 4.x
	private static final String EXTRA_KEYS = "com.oasisfeng.nevo.bundle.extra.KEYS";	// ArrayList<String>
	private static final String SCHEME_BUNDLE = "bundle";
	private static final String TAG_PREFIX = "B>";
	private static final String GROUP_PREFIX = "B>";

	private static final int MIN_NUM_TO_BUNDLE = 2;

	@Override protected void onConnected() {
		Log.i(TAG, "Retrieving active bundles...");
		for (final StatusBarNotification existent : Objects.requireNonNull(getSystemService(NotificationManager.class)).getActiveNotifications()) {
			if (existent.getTag() == null || ! existent.getTag().startsWith(TAG_PREFIX)) continue;
			final String bundle = existent.getTag().substring(TAG_PREFIX.length());
			final List<String> keys = existent.getNotification().extras.getStringArrayList(EXTRA_KEYS);
			if (keys == null) {
				Log.w(TAG, "Bundle notification without bundled keys: " + existent.getKey());
				continue;
			}
			Log.i(TAG, keys.size() + " in \"" + bundle + "\": " + keys);
			for (final String key : keys)
				mBundles.setNotificationBundle(key, bundle);
		}
	}

	@Override protected void apply(final MutableStatusBarNotification evolved) {
		String bundle = mBundles.queryRuleForNotification(evolved);
		if (bundle == null)		// No explicit bundle set, default to app name
			bundle = getSourceNames(Collections.singleton(evolved.getPackageName()));
		else if (bundle.isEmpty()) return;		// No matched rule or configured to be not bundled (empty for exclusion)
		bundle(evolved, bundle);
	}

	@Override protected void onNotificationRemoved(final String key, final int reason) {
		if (mPendingRevival.remove(key)) return;				// This removal is caused by the glitch before revival.
		mBundles.setNotificationBundle(key, null);		// Remove it from bundle since it should not be shown in bundle any more.
	}

	private void bundle(final MutableStatusBarNotification evolving, final String bundle) {
		final String key = evolving.getKey();
		Log.i(TAG, "Bundle into " + bundle + ": " + key);
		mBundles.setNotificationBundle(key, bundle);
		final MutableNotification n = evolving.getNotification();
		n.setGroup(GROUP_PREFIX + bundle);

		final String token = bundle.intern();
		mHandler.removeCallbacksAndMessages(token);
		mHandler.postAtTime(new Runnable() { @Override public void run() {	// Postpone for quick decoration.
			try {
				showAsBundleIfAppropriate(bundle);
			} catch (final Exception e) {	// Catch all exceptions to avoid crashing the process
				Log.e(TAG, "Error showing bundle: " + bundle, e);
			}
		}}, token, SystemClock.uptimeMillis());
	}

	private boolean showAsBundleIfAppropriate(final String bundle) throws RemoteException {
		final List<String> bundled_keys = mBundles.getBundledNotificationKeys(bundle);
		if (bundled_keys.size() < MIN_NUM_TO_BUNDLE) {
			Log.d(TAG, "Not showing " + bundled_keys.size() + " notification(s) as bundle until " + MIN_NUM_TO_BUNDLE + " bundled");
			return false;
		}
		// Grouped notifications still are "active" since Lollipop
		final List<MutableStatusBarNotification> bundled_sbns = getMyActiveNotifications(bundled_keys);
		final List<String> available_bundled_keys = FluentIterable.from(bundled_sbns).transform(new Function<MutableStatusBarNotification, String>() { @Override public String apply(final MutableStatusBarNotification sbn) {
			return sbn.getKey();
		}}).toList();
		if (available_bundled_keys.size() != bundled_keys.size())
			Log.e(TAG, "Inconsistent bundled keys, expected=" + bundled_keys + ", available=" + available_bundled_keys);

		final int num_bundled = bundled_sbns.size();
		if (num_bundled != bundled_keys.size()) {
			Log.w(TAG, num_bundled + " out of " + bundled_keys.size() + " bundled notifications are retrieved successfully.");
			if (num_bundled < MIN_NUM_TO_BUNDLE) return false;
		}
		final List<MutableStatusBarNotification> visible_sbns = num_bundled <= 4 ? bundled_sbns : bundled_sbns.subList(num_bundled - 4, num_bundled);

		// Sort by post time
		final ImmutableList<MutableStatusBarNotification> sorted_sbns = FluentIterable.from(visible_sbns).toSortedList(
				Ordering.natural().reverse().onResultOf(new Function<MutableStatusBarNotification, Comparable>() { @Override public Comparable apply(final MutableStatusBarNotification sbn) {
					return sbn.getPostTime();
				}}));

		final Notification notification = buildBundleNotification(bundle, available_bundled_keys, sorted_sbns);
		mNotificationManager.notify(TAG_PREFIX + bundle, 0, notification);

		return true;
	}

	private List<MutableStatusBarNotification> getMyActiveNotifications(final List<String> keys) {
		return Collections.emptyList();		// FIXME
	}

	private Notification buildBundleNotification(final String bundle, final List<String> bundled_keys,
												 final List<MutableStatusBarNotification> visible_sbns) throws RemoteException {
		final HashSet<String> bundled_pkgs = new HashSet<>(visible_sbns.size());
		long latest_when = 0; @ColorInt int shared_color = -1; @DrawableRes int shared_icon = -1;
		for (final MutableStatusBarNotification sbn : visible_sbns) {
			final Notification n = sbn.getNotification();
			if (n.when > latest_when) latest_when = n.when;

			if (shared_color == -1) shared_color = n.color;
			else if (n.color != shared_color) shared_color = NotificationCompat.COLOR_DEFAULT;	// No shared color

			final @DrawableRes int icon = n.extras.getInt(NotificationCompat.EXTRA_SMALL_ICON, -1);
			if (shared_icon == -1) shared_icon = icon;
			else if (icon != shared_icon) shared_icon = 0;

			bundled_pkgs.add(sbn.getPackageName());
		}
		final String shared_pkg = bundled_pkgs.size() == 1 ? bundled_pkgs.iterator().next() : null;

		final Notification.Builder builder = new Notification.Builder(this).setGroup(GROUP_PREFIX + bundle).setGroupSummary(true)
				.setSmallIcon(R.drawable.ic_notification_bundle).setColor(shared_color).setLocalOnly(true);

		// Sufficient for notification bundle on Android N
		if ("N".equals(VERSION.CODENAME)) {
			final CharSequence sub_text;
			if (shared_color != NotificationCompat.COLOR_DEFAULT) {
				final SpannableString colored_sub_text = SpannableString.valueOf(bundle);
				final float[] hsl = new float[3]; ColorUtils.colorToHSL(shared_color, hsl);
				hsl[1] = 0.94f; hsl[2] = Math.min(hsl[2] * 0.6f, 0.31f);	// TODO: Correct this
				final int fg_color = ColorUtils.HSLToColor(hsl);
				colored_sub_text.setSpan(new ForegroundColorSpan(fg_color), 0, bundle.length(), 0);
				sub_text = colored_sub_text;
			} else sub_text = bundle;
			return builder.setSubText(sub_text).build();
		} else builder.setWhen(latest_when).setAutoCancel(false).setNumber(bundled_keys.size());

		if (shared_pkg != null) {
			final Bundle last_extras = visible_sbns.get(0).getNotification().extras;
			final SpannableStringBuilder info = new SpannableStringBuilder(bundle).append(" · ").append(String.valueOf(bundled_keys.size()));
			info.setSpan(new StyleSpan(Typeface.BOLD), 0, bundle.length(), 0);

			builder.setContentTitle(last_extras.getCharSequence(NotificationCompat.EXTRA_TITLE))
					.setContentText(last_extras.getCharSequence(NotificationCompat.EXTRA_TEXT))
					.setSubText(last_extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT))
					.setContentInfo(info);
		} else builder.setContentTitle(bundle).setContentText(getSourceNames(bundled_pkgs));

		final Bundle extras = builder.getExtras();
		final ArrayList<String> bundled_key_list = new ArrayList<>(bundled_keys);
		extras.putStringArrayList(EXTRA_KEYS, bundled_key_list);
		if (bundled_pkgs.size() == 1 && shared_icon != 0)
			builder.setSmallIcon(Icon.createWithResource(shared_pkg, shared_icon));

		// Set on-click pending intent explicitly, to avoid notification drawer collapsing when bundle is clicked.
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

		notification.bigContentView = buildExpandedView(visible_sbns, click_pending_intent);
		return notification;
	}

	/** Preview the last a few notifications vertically as expanded view of bundle notification. */
	private @Nullable RemoteViews buildExpandedView(final List<MutableStatusBarNotification> sbns, final PendingIntent click_pending_intent) {
		if (sbns.isEmpty()) return null;
		final RemoteViews expanded = new RemoteViews(getPackageName(), R.layout.bundle_expanded_notification);

		// Since Lollipop, "reapply()" is used with remote views onto the current one when updating notification.
		// We must clear the view group before adding new content.
		expanded.removeAllViews(R.id.bundle_expanded_container);

		for (final MutableStatusBarNotification sbn : sbns)
			expanded.addView(R.id.bundle_expanded_container, sbn.getNotification().contentView);

		expanded.setOnClickPendingIntent(R.id.bundle_expanded_container, click_pending_intent);
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

				for (final String key : keys) {			// The removal of group summary notification will cause all the group children being removed too,
					ignoreNextNotificaitonRemoval(key);	// We should ignore this glitch in the following onNotificationRemoved().
					reviveNotification(key);
				}
				// Remove the bundle notification after restoration,
				mNotificationManager.cancel(TAG_PREFIX + bundle, 0);
				break;
			case ACTION_BUNDLE_CLEAR:
				if (keys == null || keys.isEmpty()) break;
				Log.d(TAG, "Clearing " + keys.size() + " notifications in bundle " + bundle + ": " + keys);
				for (final String key : keys)
					mBundles.setNotificationBundle(key, null);
				try {
					showAsBundleIfAppropriate(bundle);    // TODO: Place it in exactly same place as the previous one by sort key.
				} catch (final RemoteException ignored) {}
				break;
			}
		}
	};

	/** Remove notification while still keep it in bundle */
	private void ignoreNextNotificaitonRemoval(final String key) {
		// Track the keys of notification being revived
		mPendingRevival.add(key);
		mHandler.removeCallbacks(mResetPendingRevival);
		mHandler.postDelayed(mResetPendingRevival, 3000);	// Avoid potential leaks
	}

	private final Runnable mResetPendingRevival = new Runnable() { @Override public void run() {
		mPendingRevival.clear();
	}};

	// TODO: Reuse names when update
	private String getSourceNames(final Set<String> pkgs) {
		final PackageManager pm = getPackageManager();
		final StringBuilder names = new StringBuilder();
		for (final String pkg : pkgs)
			try { @SuppressWarnings("WrongConstant")
				final ApplicationInfo app_info = pm.getApplicationInfo(pkg, GET_UNINSTALLED_PACKAGES);
				names.append(", ").append(app_info.loadLabel(pm));
			} catch (final NameNotFoundException ignored) {}          // TODO: Packages from other user profiles?
		return names.length() > 2 ? names.substring(2) : "";
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
