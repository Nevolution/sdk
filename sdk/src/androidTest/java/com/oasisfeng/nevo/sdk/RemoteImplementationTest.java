package com.oasisfeng.nevo.sdk;

import android.app.Notification;
import android.os.Parcel;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

/**
 * Created by Oasis on 2018/4/6.
 */
public class RemoteImplementationTest {

	@Test public void testFactory() throws InvocationTargetException, IllegalAccessException {
		final Parcel parcel = Parcel.obtain();
		final Notification n = TestUtils.n();
		final MutableNotification mutable = TestUtils.mutable(n);
		mutable.setGroup("group");
		RemoteImplementation.writeBackToParcel(parcel, 0, mutable, ((MutableNotificationBaseImpl) mutable).getOriginalMutableKeeper());

		parcel.setDataPosition(0);
		final MutableNotification replied = TestUtils.mutable(n);
		readBackFromParcel.invoke(null, parcel, replied);
		assertEquals("group", replied.getGroup());
	}

	public static void readBackFrom(final Parcel parcel, final MutableNotification mutable) throws ReflectiveOperationException {
		readBackFromParcel.invoke(null, parcel, mutable);
	}

	private static final Method readBackFromParcel;

	static {
		RemoteImplementation.initializeIfNotYet(InstrumentationRegistry.getTargetContext());
		try {
			readBackFromParcel = RemoteImplementation.sClass.getMethod("readBackFromParcel", Parcel.class, Notification.class);
		} catch (final NoSuchMethodException e) {
			throw new IllegalStateException("Incompatible engine installed");
		}
	}
}
