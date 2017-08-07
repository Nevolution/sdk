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

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.BadParcelableException;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.NetworkOnMainThreadException;
import android.os.Process;
import android.os.RemoteException;
import android.service.notification.StatusBarNotification;
import android.support.annotation.CallSuper;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.util.Log;

import com.oasisfeng.nevo.StatusBarNotificationEvo;
import com.oasisfeng.nevo.engine.INevoController;

import java.util.ArrayList;
import java.util.List;

import static android.content.pm.PackageManager.GET_SIGNATURES;
import static android.content.pm.PackageManager.SIGNATURE_MATCH;

/**
 * Interface for notification decorator.
 *
 * <p><b>Decorator permission restriction:</b> Only notifications from packages enabled for this decorator are accessible via APIs below.
 *
 * @author Oasis
 */
public abstract class NevoDecoratorService extends Service {

	/** The action to bind {@link NevoDecoratorService} */
	public static final String ACTION_DECORATOR_SERVICE = "com.oasisfeng.nevo.Decorator";

	/**
	 * Add this extra to indicate that the big content view of the evolving notification should be rebuilt with this style,
	 * usually after the specific extras for built-in style of big content view are altered.
	 *
	 * <p>Don't check this key in extras, since it will be removed during decoration.</p>
	 *
	 * <p>The string value is the class name of the big content style to be rebuilt with. Only the Android built-in styles are supported.
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
	public static final String STYLE_MESSAGING = "android.app.Notification$MessagingStyle";

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
	@Keep protected void apply(final StatusBarNotificationEvo evolving) throws Exception {}

	/** Override this method to perform initial process. */
	protected void onConnected() throws Exception {}

	/** Called when notification (no matter decorated or not) from packages with this decorator enabled is removed. */
	@Keep protected void onNotificationRemoved(final String key) throws Exception {}

	/**
	 * Called when notification (no matter decorated or not) from packages with this decorator enabled is removed.
	 *
	 * If notification payload is not relevant, please consider overriding {@link #onNotificationRemoved(String)} instead.
	 */
	@Keep protected void onNotificationRemoved(final StatusBarNotificationEvo notification) throws Exception {}

	/** Retrieve active notifications posted by the caller UID. */
	protected final List<StatusBarNotificationEvo> getMyActiveNotifications() throws RemoteException {
		return getMyActiveNotifications(null);
	}

	/** Retrieve active notifications with the specified keys, posted by the caller UID. */
	protected final List<StatusBarNotificationEvo> getMyActiveNotifications(final List<String> keys) throws RemoteException {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ! (mController instanceof Binder)) {
			final StatusBarNotification[] notifications = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).getActiveNotifications();
			final List<StatusBarNotificationEvo> transformed = new ArrayList<>(notifications.length);
			for (final StatusBarNotification notification : notifications)
				transformed.add(StatusBarNotificationEvo.from(notification));
			return transformed;
		}
		return mController.getActiveNotifications(mWrapper, keys);
	}

	/**
	 * Retrieve historic notifications posted with the given key (including the incoming one without decoration at the last).
	 * The number of notifications kept in archive is undefined.
	 *
	 * Decorator permission restriction applies.
	 */
	@SuppressWarnings("unchecked") protected final List<StatusBarNotificationEvo> getArchivedNotifications(final String key, final int limit) throws RemoteException {
		return mController.getArchivedNotifications(mWrapper, key, limit).getList();
	}

	/**
	 * Retrieve notifications by key (in evolved form, no matter active or removed, only the latest for each key).
	 * Returned list may not contain entries for some of the requested keys, if not allowed or missing in archive.
	 * It may also contain multiple entries for some requested keys, since split notifications with altered tag and ID share the same key.
	 *
	 * Decorator permission restriction applies.
	 */
	@SuppressWarnings("unchecked") protected final List<StatusBarNotificationEvo> getNotifications(final List<String> keys) throws RemoteException {
		return mController.getNotifications(mWrapper, keys).getList();
	}

	/**
	 * Cancel an active notification, remove it from notification panel.
	 *
	 * Decorator permission restriction applies.
	 */
	protected final void cancelNotification(final String key) throws RemoteException {
		mController.cancelNotification(mWrapper, key);
	}

	/**
	 * Revive a previously cancelled (removed or swiped) notification, with no alerts (sound, vibration, lights).
	 * If the notification is still active, it will not be affected.
	 *
	 * Decorator permission restriction applies.
	 */
	protected final void reviveNotification(final String key) throws RemoteException {
		mController.reviveNotification(mWrapper, key);
	}

	@CallSuper @Override public IBinder onBind(final Intent intent) {
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

	private static String shorten(final String name) {
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
	protected final String TAG = "Nevo.Decorator[" + shorten(getClass().getSimpleName()) + "]";

	private class INevoDecoratorWrapper extends INevoDecorator.Stub {

		@Override public void apply(final StatusBarNotificationEvo evolving, final @Nullable Bundle options) {
			if (Binder.getCallingUid() != mCallerUid) throw new SecurityException();
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

		@Override public void onNotificationRemoved(final String key, final @Nullable Bundle options) {
			if (Binder.getCallingUid() != mCallerUid) throw new SecurityException();
			try {
				NevoDecoratorService.this.onNotificationRemoved(key);
			} catch (final Throwable t) {
				Log.e(TAG, "Error running onNotificationRemoved()", t);
				throw asParcelableException(t);
			}
		}

		@Override public void onNotificationRemovedLight(final StatusBarNotificationEvo notification, final @Nullable Bundle options) {
			if (Binder.getCallingUid() != mCallerUid) throw new SecurityException();
			try {
				NevoDecoratorService.this.onNotificationRemoved(notification);
			} catch (final Throwable t) {
				Log.e(TAG, "Error running onNotificationRemoved()", t);
				throw asParcelableException(t);
			}
		}

		@Override public int onConnected(final INevoController controller, final @Nullable Bundle options) {
			final PackageManager pm = getPackageManager();
			final int caller_uid = Binder.getCallingUid(), my_uid = Process.myUid();
			if (caller_uid != my_uid && pm.checkSignatures(caller_uid, my_uid) != SIGNATURE_MATCH) {
				final String[] caller_pkgs = pm.getPackagesForUid(caller_uid);
				if (caller_pkgs == null || caller_pkgs.length == 0) throw new SecurityException();
				try { @SuppressLint("PackageManagerGetSignatures")
				final PackageInfo caller_info = pm.getPackageInfo(caller_pkgs[0], GET_SIGNATURES);
					if (caller_info == null) throw new SecurityException();
					for (final Signature signature : caller_info.signatures)
						if (signature.hashCode() != SIGNATURE_HASH) throw new SecurityException("Caller signature mismatch");
				} catch (final PackageManager.NameNotFoundException e) { throw new SecurityException(); }	// Should not happen
			}
			mCallerUid = caller_uid;

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

		private int mCallerUid = -1;
		private static final int SIGNATURE_HASH = -541181501;
	}
}
