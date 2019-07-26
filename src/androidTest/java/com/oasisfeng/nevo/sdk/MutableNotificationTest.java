package com.oasisfeng.nevo.sdk;

import android.app.Notification;
import android.app.Notification.Action;
import android.graphics.drawable.Icon;
import android.os.Parcel;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;
import static com.oasisfeng.nevo.sdk.TestUtils.b;
import static com.oasisfeng.nevo.sdk.TestUtils.mutable;
import static com.oasisfeng.nevo.sdk.TestUtils.n;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

/**
 * Created by Oasis on 2018/4/10.
 */
public class MutableNotificationTest {

	@Test public void testBasicConsistency() {
		final Notification n = n();
		final MutableNotification mutable = mutable(n);
		assertNotEquals(n.extras, mutable.extras);				// Clone, not reference copy.
		assertEquals(n.extras.size(), mutable.extras.size());
		assertEquals(n.extras.keySet(), mutable.extras.keySet());
	}

	@Test @SuppressWarnings("UnusedLabel") public void testBasicMutationsWithParceling() {
		final MutableNotification mutable = mutable(n());

		testMutation(mutable, MutableNotification::setGroup, MutableNotification::getGroup, "G1", "G2", "", null);
		testMutation(mutable, MutableNotification::setSortKey, MutableNotification::getSortKey, "S1", "S2", "", null);

		testMutation(mutable, MutableNotification::setSmallIcon, MutableNotification::getSmallIcon, RES_ICON, DATA_ICON, null);
		testMutation(mutable, MutableNotification::setLargeIcon, MutableNotification::getLargeIcon, RES_ICON, DATA_ICON, null);
		if (SDK_INT >= O) testMutation(mutable, MutableNotification::setTimeoutAfter, Notification::getTimeoutAfter, 10L, -999L, 0L);

ACTION:	testMutation(mutable, MutableNotification::addAction, m -> m.actions[0], new Action.Builder(RES_ICON, "Hello", null).build());
		testMutation(mutable, MutableNotification::addAction, m -> m.actions[1], new Action.Builder(RES_ICON, "Bye", null).build());

PERSON: testMutation(mutable, MutableNotification::addPerson, m -> requireNonNull(m.extras.getStringArray(Notification.EXTRA_PEOPLE))[0], "Tom");
		testMutation(mutable, MutableNotification::addPerson, m -> requireNonNull(m.extras.getStringArray(Notification.EXTRA_PEOPLE))[1], "Jerry");

PUBLIC: testMutation(mutable, (m, p) -> m.publicVersion = p, m -> ensureParcelingEquality(m.publicVersion), n(), b().setGroup("x").build());
	}

	@SafeVarargs private static <T> void testMutation(final MutableNotification mutable, final BiConsumer<MutableNotification, T> setter,
													  final Function<MutableNotification, T> getter, final T... values) {
		for (final T value : values) {
			setter.accept(mutable, value);
			assertParcelEquals(value, getter.apply(TestUtils.pup(mutable)));
			assertParcelEquals(value, getter.apply(TestUtils.incPup(mutable)));
		}
	}

	private static <T> void assertParcelEquals(final T expected, final T actual) {
		final Parcel obj_parcel = Parcel.obtain(), other_parcel = Parcel.obtain();
		obj_parcel.writeValue(expected);
		other_parcel.writeValue(actual);
		if (! Arrays.equals(obj_parcel.marshall(), other_parcel.marshall())) {
			fail("Not parceling equals:\n  expected: " + expected + "\n  actual:   " + actual);
		}
		obj_parcel.recycle();
		other_parcel.recycle();
	}

	/** Notification(Parcel) will set the legacy "icon" if small icon is built from resource, thus breaks parceling equality. */
	@SuppressWarnings("deprecation") private static Notification ensureParcelingEquality(final Notification n) { n.icon = 0; return n; }

	private final Icon RES_ICON = Icon.createWithResource(InstrumentationRegistry.getTargetContext(), android.R.drawable.stat_notify_chat);
	private final Icon DATA_ICON = Icon.createWithData(new byte[] { 1, 2, 3 }, 0, 3);
}
