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
import android.app.NotificationChannel;
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
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.CallSuper;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.oasisfeng.nevo.decorator.INevoDecorator;
import com.oasisfeng.nevo.engine.INevoController;

import java.util.Collections;
import java.util.List;

import static android.content.pm.PackageManager.GET_SIGNATURES;
import static android.content.pm.PackageManager.SIGNATURE_MATCH;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.O;
import static android.support.annotation.RestrictTo.Scope.LIBRARY;
import static java.util.Collections.singletonList;

/**
 * Interface for notification decorator.
 *
 * <p><b>Decorator permission restriction:</b> Only notifications from packages enabled for this decorator are accessible via APIs below.
 *
 * @author Oasis
 */
@RequiresApi(M) public abstract class NevoDecoratorService extends Service {

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
	 * For example, sticky notification ({@link android.app.Notification#FLAG_ONGOING_EVENT FLAG_ONGOING_EVENT}
	 * or {@link android.app.Notification#FLAG_FOREGROUND_SERVICE FLAG_FOREGROUND_SERVICE}) is not modifiable at present.
	 *
	 * @param evolving the incoming notification evolved by preceding decorators and to be evolved by this decorator,
	 *                 or an already evolved notification (with or without this decorator).
	 * @return whether decoration has been applied. Returning false causes engine to ignore any decoration actually applied.
	 */
	@Keep protected boolean apply(final MutableStatusBarNotification evolving) { return false; }

	/** Called when connected by Nevolution engine. Override this method to perform initial process. */
	@Keep protected void onConnected() {}

	/**
	 * Called when notification (no matter decorated or not) from packages with this decorator enabled is removed.
	 * <p>
	 * This is also called with original key and {@link android.service.notification.NotificationListenerService#REASON_APP_CANCEL REASON_APP_CANCEL}
	 * when originating app requests notification removal, only for ongoing notification, or if the feature "Removal-aware" of Nevolution is activated.
	 *
	 * @param key the original key for removal requested by originating app, or the real key (may be different from original key) otherwise.
	 * @param reason see REASON_XXX constants in {@link android.service.notification.NotificationListenerService}, always 0 before Android O.
	 * @return true if decorator handled this removal in its own way, thus notifications evolved from the removed one will NOT be removed automatically.
	 */
	@Keep protected boolean onNotificationRemoved(final String key, final int reason) { return false; }

	/**
	 * Called when notification (no matter decorated or not) from packages with this decorator enabled is removed.
	 *
	 * If notification payload is not relevant, please consider overriding {@link #onNotificationRemoved(String, int)} instead.
	 *
	 * @param reason see REASON_XXX constants in {@link android.service.notification.NotificationListenerService}, always 0 before Android O.
	 * @return true if decorator handled this removal in its own way, thus notifications evolved from the removed one will NOT be removed automatically.
	 */
	@Keep protected boolean onNotificationRemoved(final StatusBarNotification notification, final int reason) { return false; }

	/**
	 * Retrieve historic notifications posted with the given key (including the incoming one without decoration at the last).
	 * The number of notifications kept in archive is undefined.
	 *
	 * Decorator permission restriction applies.
	 *
	 * @deprecated This API will no longer be supported in the future
	 */
	@Deprecated protected final List<StatusBarNotification> getArchivedNotifications(final String key, final int limit) {
		try {
			return mController.getNotifications(mWrapper, TYPE_ARCHIVED, singletonList(key), limit, null);
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
	public final List<StatusBarNotification> getLatestNotifications(final List<String> keys) {
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
	 *
	 * @deprecated Notification will not be allowed to remove explicitly to eliminate the risk of notification lost due to potential bugs in decorator.
	 * @see #onNotificationRemoved(String, int)
	 * @see #onNotificationRemoved(StatusBarNotification, int)
	 */
	@Deprecated public final void cancelNotification(final String key) {
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
	 *
	 * @param key the real key (may be different from original key)
	 */
	public final void reviveNotification(final String key) {
		try {
			mController.performNotificationAction(mWrapper, ACTION_REVIVE, key, null);
		} catch (final RemoteException e) {
			Log.w(TAG, "Error reviving notification: " + key, e);
		}
	}

	/**
	 * Recast an active notification asynchronously, which will then go through the decorators (including this one) as if just posted.
	 * Useful for making additional tweaks asynchronously after initial decoration pass.
	 *
	 * Decorator permission restriction applies.
	 *
	 * @param key the original key of the notification to recast
	 * @param fillInExtras additional extras to fill in the notification being recast. These additions are only present during the recasting procedure.
	 */
	public final void recastNotification(final String key, final @Nullable Bundle fillInExtras) {
		try {
			mController.performNotificationAction(mWrapper, ACTION_RECAST, key, fillInExtras);
		} catch (final RemoteException e) {
			Log.w(TAG, "Error recasting notification: " + key, e);
		}
	}

	/**
	 * (Android O+ only) Snooze a STICKY (on-going or foreground) notification.
	 *
	 * <p>Non-sticky notifications are not allowed to snooze at present. If you need to snooze them,
	 * please file an issue to discuss the use case with us.
	 *
	 * @see android.service.notification.NotificationListenerService#snoozeNotification(String, long)
	 */
	public final void snoozeNotification(final String key, final long duration) {
		try {
			final Bundle bundle = new Bundle();
			bundle.putLong(KEY_DURATION, duration);
			mController.performNotificationAction(mWrapper, ACTION_SNOOZE, key, bundle);
		} catch (final RemoteException e) {
			Log.w(TAG, "Error recasting notification: " + key, e);
		}
	}

	/**
	 * Returns the notification channel settings for a given channel id in targeted app.
	 * If specified package is not targeted by this decorator, {@link SecurityException} will be thrown.
	 *
	 * @see android.app.NotificationManager#getNotificationChannel(String)
	 */
	@RequiresApi(O) protected final @Nullable NotificationChannel getNotificationChannel(final String pkg, final UserHandle user, final String channel) {
		if (mSupportedApiVersion < 4) return null;
		try {
			final List<NotificationChannel> channels = mController.getNotificationChannels(mWrapper, pkg, singletonList(channel), bundleIfNeeded(user));
			return channels == null || channels.isEmpty() ? null : channels.get(0);
		} catch (final RemoteException e) {
			Log.w(TAG, "Error querying notification channel in " + pkg + ": " + channel, e);
			return null;
		}
	}

	/**
	 * Create {@link NotificationChannel} for targeted app. If specified package is not targeted by this decorator, {@link SecurityException} will be thrown.
	 *
	 * @see android.app.NotificationManager#createNotificationChannel(NotificationChannel)
	 */
	@RequiresApi(O) protected final void createNotificationChannels(final String pkg, final UserHandle user, final List<NotificationChannel> channels) {
		try {
			mController.createNotificationChannels(mWrapper, pkg, channels, bundleIfNeeded(user));
		} catch (final RemoteException e) {
			Log.w(TAG, "Error creating notification channels for " + pkg + ": " + channels, e);
		}
	}

	/** @deprecated use {@link #createNotificationChannels(String, UserHandle, List)} instead */
	@Deprecated @RequiresApi(O) protected final void createNotificationChannels(final String pkg, final List<NotificationChannel> channels) {
		createNotificationChannels(pkg, Process.myUserHandle(), channels);
	}

	/**
	 * Delete {@link NotificationChannel} for targeted app. If specified package is not targeted by this decorator
	 * or specified channel is not created by this decorator, nothing will be deleted.
	 *
	 * @see android.app.NotificationManager#deleteNotificationChannel(String)
	 */
	@RequiresApi(O) protected final void deleteNotificationChannel(final String pkg, final UserHandle user, final String channel) {
		if (mSupportedApiVersion < 4) return;
		try {
			mController.deleteNotificationChannel(mWrapper, pkg, channel, bundleIfNeeded(user));
		} catch (final RemoteException e) {
			Log.w(TAG, "Error deleting notification channel for " + pkg + ": " + channel, e);
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
			detectDerivedMethod(FLAG_REMOVAL_AWARE_KEY_ONLY, clazz, "onNotificationRemoved", String.class, int.class);
			detectDerivedMethod(FLAG_REMOVAL_AWARE, clazz, "onNotificationRemoved", StatusBarNotification.class, int.class);
		}
		return mWrapper == null ? mWrapper = new INevoDecoratorWrapper() : mWrapper;
	}

	private static Bundle bundleIfNeeded(final UserHandle user) {
		if (user == null || Process.myUserHandle().equals(user)) return null;
		final Bundle bundle = new Bundle();
		bundle.putParcelable(Intent.EXTRA_USER, user);
		return bundle;
	}

	private void detectDerivedMethod(final int flag, final Class<?> clazz, final String name, final Class<?>... parameter_types) {
		if ((mFlags & flag) == 0) try {
			clazz.getDeclaredMethod(name, parameter_types);
			mFlags |= flag;
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
	@RestrictTo(LIBRARY) static final int ACTION_SNOOZE = 4;
	@RestrictTo(LIBRARY) static final int FLAG_DECORATION_AWARE = 0x1;
	@RestrictTo(LIBRARY) static final int FLAG_REMOVAL_AWARE_KEY_ONLY = 0x2;
	@RestrictTo(LIBRARY) static final int FLAG_REMOVAL_AWARE = 0x4;
	@RestrictTo(LIBRARY) static final String KEY_REASON = "reason";
	@RestrictTo(LIBRARY) static final String KEY_SUPPORTED_API_VERSION = "version";
	@RestrictTo(LIBRARY) static final String KEY_DURATION = "duration";

	protected final String TAG = "Nevo.Decorator[" + shorten(getClass().getSimpleName()) + "]";

	private class INevoDecoratorWrapper extends INevoDecorator.Stub {

		@Override public void apply(final/* inout */MutableStatusBarNotification evolving, final @Nullable Bundle options) {
			if (Binder.getCallingUid() != mCallerUid) throw new SecurityException();
			try {
				final boolean applied = NevoDecoratorService.this.apply(evolving);
				if (applied) evolving.setAllowIncrementalWriteBack();
				else evolving.setNoWriteBack();
			} catch (final Throwable t) {
				Log.e(TAG, "Error running apply()", t);
				throw asParcelableException(t);
			}
		}

		@Override public boolean onNotificationRemoved(final String key, final @Nullable Bundle options) {
			if (Binder.getCallingUid() != mCallerUid) throw new SecurityException();
			try {
				return NevoDecoratorService.this.onNotificationRemoved(key, options != null ? options.getInt(KEY_REASON) : 0);
			} catch (final Throwable t) {
				Log.e(TAG, "Error running onNotificationRemoved()", t);
				throw asParcelableException(t);
			}
		}

		@Override public boolean onNotificationRemovedLight(final StatusBarNotification notification, final @Nullable Bundle options) {
			if (Binder.getCallingUid() != mCallerUid) throw new SecurityException();
			try {
				return NevoDecoratorService.this.onNotificationRemoved(notification, options != null ? options.getInt(KEY_REASON) : 0);
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
				try { @SuppressWarnings("deprecation") @SuppressLint("PackageManagerGetSignatures")
					final PackageInfo caller_info = pm.getPackageInfo(caller_pkgs[0], GET_SIGNATURES);
					if (caller_info == null) throw new SecurityException();
					//noinspection deprecation
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
