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

package com.oasisfeng.nevo.sdk;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.BadParcelableException;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.NetworkOnMainThreadException;
import android.os.Process;
import android.os.RemoteException;
import android.service.notification.StatusBarNotification;
import android.support.annotation.CallSuper;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.oasisfeng.nevo.decorator.INevoDecorator;
import com.oasisfeng.nevo.engine.INevoController;

import java.util.Collections;
import java.util.List;

import static android.content.pm.PackageManager.GET_SIGNATURES;
import static android.content.pm.PackageManager.SIGNATURE_MATCH;
import static android.support.annotation.RestrictTo.Scope.LIBRARY;

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

	/** Optional meta-data key within the &lt;service&gt; tag, to indicate the target packages (separated by comma) of the app-specific decorator */
	public static final String META_KEY_PACKAGES = "packages";

	/** Valid constant values for {@link android.app.Notification#EXTRA_TEMPLATE} */
	public static final String TEMPLATE_BIG_TEXT	= "android.app.Notification$BigTextStyle";
	public static final String TEMPLATE_INBOX		= "android.app.Notification$InboxStyle";
	public static final String TEMPLATE_BIG_PICTURE	= "android.app.Notification$BigPictureStyle";
	public static final String TEMPLATE_MEDIA		= "android.app.Notification$MediaStyle";
	public static final String TEMPLATE_MESSAGING	= "android.app.Notification$MessagingStyle";

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
	@Keep protected void apply(final MutableStatusBarNotification evolving) {}

	/** Override this method to perform initial process. */
	protected void onConnected() {}

	/**
	 * Called when notification (no matter decorated or not) from packages with this decorator enabled is removed.
	 *
	 * @param reason see REASON_XXX constants in {@link android.service.notification.NotificationListenerService}
	 */
	@Keep protected void onNotificationRemoved(final String key, @SuppressWarnings("unused") final int reason) {}

	/**
	 * Called when notification (no matter decorated or not) from packages with this decorator enabled is removed.
	 *
	 * If notification payload is not relevant, please consider overriding {@link #onNotificationRemoved(String, int)} instead.
	 */
	@Keep protected void onNotificationRemoved(final StatusBarNotification notification, @SuppressWarnings("unused") final int reason) {}

	/**
	 * Retrieve historic notifications posted with the given key (including the incoming one without decoration at the last).
	 * The number of notifications kept in archive is undefined.
	 *
	 * Decorator permission restriction applies.
	 */
	protected final List<StatusBarNotification> getArchivedNotifications(final String key, final int limit) {
		try {
			return mController.getNotifications(mWrapper, TYPE_ARCHIVED, Collections.singletonList(key), limit, null);
		} catch (final RemoteException e) {
			Log.w(TAG, "Error retrieving archived notifications: " + key, e);
			return Collections.emptyList();
		}
	}

	/**
	 * Retrieve notifications by keys, latest one (in evolved form) for each key, no matter active or removed.
	 *
	 * The returned list may not contain entries for some of the requested keys, if not allowed or missing in archive.
	 * It may also contain multiple entries for some requested keys, since split notifications with altered tag and ID share the same key.
	 *
	 * Decorator permission restriction applies.
	 */
	protected final List<StatusBarNotification> getLatestNotifications(final List<String> keys) {
		try {
			return mController.getNotifications(mWrapper, TYPE_LATEST, keys, 0, null);
		} catch (final RemoteException e) {
			Log.w(TAG, "Error retrieving notifications", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Cancel an active notification, remove it from notification panel.
	 *
	 * Decorator permission restriction applies.
	 */
	protected final void cancelNotification(final String key) {
		try {
			mController.performNotificationAction(mWrapper, ACTION_CANCEL, key, null);
		} catch (final RemoteException e) {
			Log.w(TAG, "Error canceling notification: " + key, e);
		}
	}

	/**
	 * Revive a previously cancelled (removed or swiped) notification, with no alerts (sound, vibration, lights).
	 * If the notification is still active, it will not be affected.
	 *
	 * Decorator permission restriction applies.
	 */
	protected final void reviveNotification(final String key) {
		try {
			mController.performNotificationAction(mWrapper, ACTION_REVIVE, key, null);
		} catch (final RemoteException e) {
			Log.w(TAG, "Error reviving notification: " + key, e);
		}
	}

	/**
	 * Recast a past (either still active or already removed) notification asynchronously,
	 * which will then go through the decorators (including this one) as if just posted.
	 *
	 * Decorator permission restriction applies.
	 *
	 * @param fillInExtras additional extras to fill in the notification being recast.
	 */
	protected final void recastNotification(final String key, final @Nullable Bundle fillInExtras) {
		try {
			mController.performNotificationAction(mWrapper, ACTION_RECAST, key, fillInExtras);
		} catch (final RemoteException e) {
			Log.w(TAG, "Error recasting notification: " + key, e);
		}
	}

	/**
	 * Get the API version of Nevolution SDK supported by Nevolution engine installed on this device.
	 * If the supported API version in user's device is lowed than API version of SDK used in your project, some new APIs may not work.
	 *
	 * API version of current Nevolution SDK is defined in {@link R.integer#nevo_api_version}
	 */
	protected final int getSupportedApiVersion() {
		return mSupportedApiVersion;
	}

	@CallSuper @Override public IBinder onBind(final Intent intent) {
		for (Class<?> clazz = getClass(); clazz != NevoDecoratorService.class; clazz = clazz.getSuperclass()) {
			detectDerivedMethod(FLAG_DECORATION_AWARE, clazz, "apply", MutableStatusBarNotification.class);
			detectDerivedMethod(FLAG_REMOVAL_AWARE_KEY_ONLY, clazz, "onNotificationRemoved", String.class);
			detectDerivedMethod(FLAG_REMOVAL_AWARE_KEY_ONLY, clazz, "onNotificationRemoved", String.class, int.class);
			detectDerivedMethod(FLAG_REMOVAL_AWARE, clazz, "onNotificationRemoved", StatusBarNotification.class);
			detectDerivedMethod(FLAG_REMOVAL_AWARE, clazz, "onNotificationRemoved", StatusBarNotification.class, int.class);
		}
		return mWrapper == null ? mWrapper = new INevoDecoratorWrapper() : mWrapper;
	}

	private void detectDerivedMethod(final int flag, final Class<?> clazz, final String name, final Class<?>... parameter_types) {
		if ((mFlags & flag) == 0) try {
			if (clazz.getDeclaredMethod(name, parameter_types) != null) mFlags |= flag;
		} catch (final NoSuchMethodException ignored) {}
	}

	private static String shorten(final String name) {
		final String suffix = "Decorator";
		return name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : name;
	}

	private INevoDecoratorWrapper mWrapper;
	private INevoController mController;
	private int mSupportedApiVersion;
	private int mFlags;

	@RestrictTo(LIBRARY) static final int TYPE_LATEST   = 1;
	@RestrictTo(LIBRARY) static final int TYPE_ARCHIVED = 2;
	@RestrictTo(LIBRARY) static final int ACTION_CANCEL = 1;
	@RestrictTo(LIBRARY) static final int ACTION_REVIVE = 2;
	@RestrictTo(LIBRARY) static final int ACTION_RECAST = 3;
	@RestrictTo(LIBRARY) static final int FLAG_DECORATION_AWARE = 0x1;
	@RestrictTo(LIBRARY) static final int FLAG_REMOVAL_AWARE_KEY_ONLY = 0x2;
	@RestrictTo(LIBRARY) static final int FLAG_REMOVAL_AWARE = 0x4;
	@RestrictTo(LIBRARY) static final String KEY_REASON = "reason";
	@RestrictTo(LIBRARY) static final String KEY_SUPPORTED_API_VERSION = "version";

	protected final String TAG = "Nevo.Decorator[" + shorten(getClass().getSimpleName()) + "]";

	private class INevoDecoratorWrapper extends INevoDecorator.Stub {

		@Override public void apply(final/* inout */MutableStatusBarNotification evolving, final @Nullable Bundle options) {
			if (Binder.getCallingUid() != mCallerUid) throw new SecurityException();
			try {
				Log.v(TAG, "Applying to " + evolving.getKey());
				NevoDecoratorService.this.apply(evolving);
				evolving.setAllowIncrementalWriteBack();
			} catch (final Throwable t) {
				Log.e(TAG, "Error running apply()", t);
				throw asParcelableException(t);
			}
		}

		@Override public void onNotificationRemoved(final String key, final @Nullable Bundle options) {
			if (Binder.getCallingUid() != mCallerUid) throw new SecurityException();
			try {
				NevoDecoratorService.this.onNotificationRemoved(key, options != null ? options.getInt(KEY_REASON) : 0);
			} catch (final Throwable t) {
				Log.e(TAG, "Error running onNotificationRemoved()", t);
				throw asParcelableException(t);
			}
		}

		@Override public void onNotificationRemovedLight(final StatusBarNotification notification, final @Nullable Bundle options) {
			if (Binder.getCallingUid() != mCallerUid) throw new SecurityException();
			try {
				NevoDecoratorService.this.onNotificationRemoved(notification, options != null ? options.getInt(KEY_REASON) : 0);
			} catch (final Throwable t) {
				Log.e(TAG, "Error running onNotificationRemoved()", t);
				throw asParcelableException(t);
			}
		}

		@Override public int onConnected(final INevoController controller, final Bundle options) {
			RemoteImplementation.initializeIfNotYet(NevoDecoratorService.this);

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
			if (options != null) mSupportedApiVersion = options.getInt(KEY_SUPPORTED_API_VERSION);
			try {
				Log.v(TAG, "onConnected");
				NevoDecoratorService.this.onConnected();
			} catch (final Throwable t) {
				Log.e(TAG, "Error running onConnected()", t);
				throw asParcelableException(t);
			}
			return mFlags;
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
