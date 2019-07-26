package com.oasisfeng.nevo.sdk;

import android.os.Process;

import org.junit.Test;

import static com.oasisfeng.nevo.sdk.TestUtils.create;
import static com.oasisfeng.nevo.sdk.TestUtils.n;
import static com.oasisfeng.nevo.sdk.TestUtils.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * Created by Oasis on 2018/3/30.
 */
public class MutableStatusBarNotificationTest {

	@Test public void testNotMutated() {
		assertEquals("tag", createWithTag("tag").getTag());
		assertEquals("", createWithTag("").getTag());
		assertNull(createWithTag(null).getTag());
	}

	@Test public void testTagIdMutation() {
		testTagIdMutation(createWithTag("tag1"));
		testTagIdMutation(createWithTag(""));
		testTagIdMutation(createWithTag(null));
	}

	private static void testTagIdMutation(final MutableStatusBarNotification mutable) {
		mutable.setTag("tag2");
		assertEquals(createWithTag("tag2").getKey(), mutable.getKey());
		mutable.setTag("");
		assertEquals(createWithTag("").getKey(), mutable.getKey());
		mutable.setTag(null);
		assertNull(mutable.getTag());
		assertEquals(createWithTag(null).getKey(), mutable.getKey());
		mutable.setId(9);
		assertEquals(9, mutable.getId());
		mutable.setId(1);	// default ID
		assertEquals(createWithTag(null).getKey(), mutable.getKey());
	}

	@Test public void testOverrideGroupKeyMutation() {
		final MutableStatusBarNotification mutable = createWithTag("");
		mutable.setOverrideGroupKey("o");
		assertEquals("o", mutable.getOverrideGroupKey());
		assertFalse(mutable.getNotification().extras.containsKey(MutableStatusBarNotification.EXTRA_ORIGINAL_OVERRIDE_GROUP));

		final MutableStatusBarNotification pup = TestUtils.pup(mutable);
		assertFalse(pup.getNotification().extras.containsKey(MutableStatusBarNotification.EXTRA_ORIGINAL_OVERRIDE_GROUP));
		pup.setOverrideGroupKey("v");
		assertEquals("v", pup.getOverrideGroupKey());
		assertEquals("o", pup.getNotification().extras.getString(MutableStatusBarNotification.EXTRA_ORIGINAL_OVERRIDE_GROUP));

		pup.setOverrideGroupKey("o");		// Revert change
		assertEquals("o", pup.getOverrideGroupKey());
		assertFalse(pup.getNotification().extras.containsKey(MutableStatusBarNotification.EXTRA_ORIGINAL_OVERRIDE_GROUP));
	}

	@Test public void testExtrasMutation() {
		final MutableStatusBarNotification mutable = createWithTag("");
		final MutableNotification n = mutable.getNotification();
		n.extras.putString("test", "new");

		final MutableStatusBarNotification pup = TestUtils.pup(mutable);
		assertEquals("new", pup.getNotification().extras.getString("test"));
	}

	@Test public void testParceling() {
		final MutableStatusBarNotification sbn = create("pkg", "tag", 9, Process.myUserHandle(), 1, n(), now());
		pupAndVerify(sbn, sbn1 -> sbn1.setTag(""));
	}

	private static void pupAndVerify(final MutableStatusBarNotification sbn, final Consumer<MutableStatusBarNotification> procedure) {
		final MutableStatusBarNotification pup = TestUtils.pup(sbn);
		TestUtils.assertSbnEquals(sbn, pup);

		procedure.accept(pup);

		final MutableStatusBarNotification pup2 = TestUtils.pup(pup);
		TestUtils.assertSbnEquals(pup, pup2);
	}

	private static MutableStatusBarNotification createWithTag(final String tag) {
		return create("pkg", tag, 1, Process.myUserHandle(), 12, n(), now());
	}

	private interface Consumer<T> { void accept(T t); }
}
