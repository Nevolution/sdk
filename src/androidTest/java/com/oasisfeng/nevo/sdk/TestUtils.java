package com.oasisfeng.nevo.sdk;

import android.app.Notification;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.support.test.InstrumentationRegistry;

import java.lang.reflect.InvocationTargetException;

import static junit.framework.Assert.assertEquals;

/**
 * Created by Oasis on 2018/3/31.
 */
public class TestUtils {

	static MutableStatusBarNotification create(final String pkg, final String tag, final int id, final UserHandle user, final int uid, final Notification n, final long time) {
		return new MutableStatusBarNotification(pkg, null, id, tag, uid, 0, mutable(n), user, time);
	}

	static <T extends Parcelable> T pup(final T object) {
		final Parcel parcel = Parcel.obtain();
		try {
			parcel.writeParcelable(object, 0);
			parcel.setDataPosition(0);
			return parcel.readParcelable(object.getClass().getClassLoader());
		} finally {
			parcel.recycle();
		}
	}

	static MutableNotification incPup(final MutableNotification mutable) {
		final Parcel parcel = Parcel.obtain();
		RemoteImplementation.initializeIfNotYet(InstrumentationRegistry.getTargetContext());
		try {
			RemoteImplementation.writeBackToParcel(parcel, 0, mutable, ((MutableNotificationBaseImpl) mutable).getOriginalMutableKeeper());
			parcel.setDataPosition(0);
			RemoteImplementationTest.readBackFrom(parcel, mutable);
			return mutable;
		} catch (final ReflectiveOperationException e) {
			throw new RuntimeException(e);
		} finally {
			parcel.recycle();
		}
	}

	static void assertSbnEquals(final StatusBarNotification sbn, final MutableStatusBarNotification transformed) {
		assertEquals(sbn.getId(), transformed.getId());
		assertEquals(sbn.getTag(), transformed.getTag());
		assertEquals(sbn.getKey(), transformed.getKey());
		assertEquals(sbn.getNotification().toString(), transformed.getNotification().toString());
		for (final String key : sbn.getNotification().extras.keySet()) if (! "android.appInfo".equals(key))
			assertEquals("Extra key: " + key, sbn.getNotification().extras.get(key), transformed.getNotification().extras.get(key));
	}

	static Notification n() { return b().build(); }

	static Notification.Builder b() {
		return new Notification.Builder(InstrumentationRegistry.getContext()).setSmallIcon(android.R.drawable.stat_notify_chat).setContentTitle("Hello");
	}

	static MutableNotificationBaseImpl mutable(final Notification n) {
		if (n instanceof MutableNotification) return (MutableNotificationBaseImpl) n;
		final MutableNotificationBaseImpl mutable = new MutableNotificationBaseImpl(n);
		try { //noinspection JavaReflectionMemberAccess
			Notification.class.getMethod("cloneInto", Notification.class, boolean.class).invoke(n, mutable, true);
		} catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
		return mutable;
	}

	static long now() { return System.currentTimeMillis(); }
}
