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

package com.oasisfeng.nevo.decorator;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.BadParcelableException;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.NetworkOnMainThreadException;
import android.os.Process;
import android.os.RemoteException;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.oasisfeng.nevo.StatusBarNotificationEvo;
import com.oasisfeng.nevo.engine.INevoController;

import java.util.Arrays;
import java.util.List;

/**
 * Interface for notification decorator.
 *
 * @author Oasis
 */
public abstract class NevoDecoratorService extends Service {

	/** The action to bind {@link NevoDecoratorService} */
	public static final String ACTION_DECORATOR_SERVICE = "com.oasisfeng.nevo.Decorator";

	/**
	 * Add this extra to indicate that the big content view of the evolving notification should be rebuilt with this style.
	 *
	 * The string value is the class name of the big content style to be rebuilt with. Only the Android built-in styles are supported.
	 *
	 * @see #STYLE_BIG_TEXT
	 * @see #STYLE_INBOX
	 * @see #STYLE_BIG_PICTURE
	 * @see #STYLE_MEDIA
	 */
	public static final String EXTRA_REBUILD_STYLE = "nevo.rebuild.style";

	public static final String STYLE_BIG_TEXT = "android.app.Notification$BigTextStyle";
	public static final String STYLE_INBOX = "android.app.Notification$InboxStyle";
	public static final String STYLE_BIG_PICTURE = "android.app.Notification$BigPictureStyle";
	public static final String STYLE_MEDIA = "android.app.Notification$MediaStyle";

	/**
	 * Apply this decorator to the notification. <b>Implementation should be idempotent</b>,
	 * assuming the given notification may be (or may be not) already evolved by this decorator before.
	 *
	 * <p>Notice: Since the notification might be evolved already, the tag and ID could be altered, only the key is immutable.
	 *
	 * <p>Beware: Not all notifications can be modified, the decoration made here may be ignored if it is not modifiable.
	 * For example, sticky notification ({@link StatusBarNotification#isClearable()} is false) is not modifiable at present.
	 *
	 * @param evolving the incoming notification evolved by preceding decorators and to be evolved by this decorator,
	 *                 or an already evolved notification (with or without this decorator).
	 */
	protected void apply(final StatusBarNotificationEvo evolving) throws Exception {}

	/** Override this method to perform initial process. */
	protected void onConnected() throws Exception {}

	/** Called when notification (no matter decorated or not) from packages with this decorator enabled is removed. */
	protected void onNotificationRemoved(final String key) throws Exception {}

	/**
	 * Called when notification (no matter decorated or not) from packages with this decorator enabled is removed.
	 *
	 * If notification payload is not relevant, please consider overriding {@link #onNotificationRemoved(String)} instead.
	 */
	protected void onNotificationRemoved(final StatusBarNotificationEvo notification) throws Exception {}

	/**
	 * Backward-compatible version of {@link NotificationManager#getActiveNotifications()}
	 *
	 * BEWARE: Unlike {@link NotificationListenerService#getActiveNotifications()},
	 * this API does not return notifications from apps other than the caller itself.
	 */
	protected final StatusBarNotificationEvo[] getMyActiveNotifications() throws RemoteException {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ! (mController instanceof Binder)) {
			final StatusBarNotification[] notifications = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).getActiveNotifications();
			final StatusBarNotificationEvo[] transformed = new StatusBarNotificationEvo[notifications.length];
			for (int i = 0; i < notifications.length; i ++) transformed[i] = StatusBarNotificationEvo.from(notifications[i]);
			return transformed;
		}
		final long identity = Binder.clearCallingIdentity();
		try {
			@SuppressWarnings("unchecked") final List<StatusBarNotificationEvo> notifications = mController.getActiveNotifications(mWrapper).getList();
			return notifications.toArray(new StatusBarNotificationEvo[notifications.size()]);
		} finally {
			Binder.restoreCallingIdentity(identity);
		}
	}

	/**
	 * All the historic notifications posted with the given key (including the incoming one without decoration at the last).
	 *
	 * Restriction: Only the key of decorated packages is accessible, empty list will be returned for others.
	 */
	@SuppressWarnings("unchecked") protected final List<StatusBarNotificationEvo> getArchivedNotifications(final String key, final int limit) throws RemoteException {
		return mController.getArchivedNotifications(mWrapper, key, limit).getList();
	}

	/**
	 * Retrieve notifications by key (in evolved form, no matter active or removed, only the latest for each key).
	 * Returned list may contain less entries than requested keys if some keys are not allowed or missing in archive.
	 *
	 * Restriction: Only the keys of notification from packages with this decorator enabled are allowed, others will be ignored.
	 */
	@SuppressWarnings("unchecked") protected final List<StatusBarNotificationEvo> getNotifications(final List<String> keys) throws RemoteException {
		return mController.getNotifications(mWrapper, keys).getList();
	}

	/**
	 * Cancel an active notification, remove it from notification panel.
	 *
	 * Restriction: Only notification from packages with this decorator enabled is allowed to cancel.
	 * */
	protected final void cancelNotification(final String key) throws RemoteException {
		mController.cancelNotification(mWrapper, key);
	}

	/**
	 * Revive a previously cancelled (removed or swiped) notification, with no alerts (sound, vibration, lights).
	 * If the notification is still active, it will not be affected.
	 *
	 * Restriction: Only notifications from packages with this decorator enabled are allowed to revive.
	 */
	protected final void reviveNotification(final String key) throws RemoteException {
		mController.reviveNotification(mWrapper, key);
	}

	@CallSuper @Override public IBinder onBind(final Intent intent) {
		if ((getApplicationContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
			// TODO: Verify signature instead
			final int caller_uid = Binder.getCallingUid();
			if (caller_uid != Process.myUid()) {
				final String[] pkgs = getPackageManager().getPackagesForUid(caller_uid);
				if (pkgs == null || ! Arrays.asList(pkgs).contains(NEVO_PACKAGE_NAME)) return null;
			}
		}
		for (Class<?> clazz = getClass(); clazz != NevoDecoratorService.class; clazz = clazz.getSuperclass()) {
			try {
				if (clazz.getDeclaredMethod("apply", StatusBarNotificationEvo.class) != null) mFlags |= FLAG_DECORATION_AWARE;
			} catch (final NoSuchMethodException ignored) {}
			try {
				if (getClass().getDeclaredMethod("onNotificationRemoved", String.class) != null) mFlags |= FLAG_REMOVAL_AWARE_KEY_ONLY;
			} catch (final NoSuchMethodException ignored) {}
			try {
				if (getClass().getDeclaredMethod("onNotificationRemoved", StatusBarNotificationEvo.class) != null) mFlags |= FLAG_REMOVAL_AWARE;
			} catch (final NoSuchMethodException ignored) {}
		}
		return mWrapper == null ? mWrapper = new INevoDecoratorWrapper() : mWrapper;
	}

	private String shorten(final String name) {
		final String suffix = "Decorator";
		return name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : name;
	}

	private INevoDecoratorWrapper mWrapper;
	private INevoController mController;
	private int mFlags;

	/** Internal flag */ static final int FLAG_DECORATION_AWARE = 0x1;
	/** Internal flag */ static final int FLAG_REMOVAL_AWARE_KEY_ONLY = 0x2;
	/** Internal flag */ static final int FLAG_REMOVAL_AWARE = 0x4;
	/** Internal flag */ static final int FLAG_INCLUDE_NO_CLEAR = 0x8;
	/** Internal extra */ static final String EXTRA_TAG_OVERRIDE = "nevo.tag.override";
	/** Internal extra */ static final String EXTRA_ID_OVERRIDE = "nevo.id.override";
	private static final String NEVO_PACKAGE_NAME = "com.oasisfeng.nevo";
	protected final String TAG = "Nevo.Decorator[" + shorten(getClass().getSimpleName()) + "]";

	private class INevoDecoratorWrapper extends INevoDecorator.Stub {

		@Override public void apply(final StatusBarNotificationEvo evolving, final @Nullable Bundle options) {
			try {
				Log.v(TAG, "Applying to " + evolving.getKey());
				final String original_tag = evolving.getTag(); final int original_id = evolving.getId();

				NevoDecoratorService.this.apply(evolving);

				final String tag = evolving.getTag(); final int id = evolving.getId();
				if (! equals(tag, original_tag)) evolving.notification().extras().putString(EXTRA_TAG_OVERRIDE, tag);
				if (id != original_id) evolving.notification().extras().putInt(EXTRA_ID_OVERRIDE, id);
			} catch (final Throwable t) {
				Log.e(TAG, "Error running apply()", t);
				throw asParcelableException(t);
			}
		}

		@Override public int onConnected(final INevoController controller, final @Nullable Bundle options) {
			mController = controller;
			try {
				Log.v(TAG, "onConnected");
				NevoDecoratorService.this.onConnected();
			} catch (final Throwable t) {
				Log.e(TAG, "Error running onConnected()", t);
				throw asParcelableException(t);
			}
			return mFlags;
		}

		@Override public void onNotificationRemoved(final String key, final @Nullable Bundle options) {
			try {
				NevoDecoratorService.this.onNotificationRemoved(key);
			} catch (final Throwable t) {
				Log.e(TAG, "Error running onNotificationRemoved()", t);
				throw asParcelableException(t);
			}
		}

		@Override public void onNotificationRemovedLight(final StatusBarNotificationEvo notification, final @Nullable Bundle options) {
			try {
				NevoDecoratorService.this.onNotificationRemoved(notification);
			} catch (final Throwable t) {
				Log.e(TAG, "Error running onNotificationRemoved()", t);
				throw asParcelableException(t);
			}
		}

		private boolean equals(final Object a, final Object b) {
			return (a == null) ? (b == null) : a.equals(b);
		}

		private RuntimeException asParcelableException(final Throwable e) {
			if (e instanceof SecurityException
					|| e instanceof BadParcelableException
					|| e instanceof IllegalArgumentException
					|| e instanceof NullPointerException
					|| e instanceof IllegalStateException
					|| e instanceof NetworkOnMainThreadException
					|| e instanceof UnsupportedOperationException)
				return (RuntimeException) e;
			return new IllegalStateException(e);
		}
	}
}
