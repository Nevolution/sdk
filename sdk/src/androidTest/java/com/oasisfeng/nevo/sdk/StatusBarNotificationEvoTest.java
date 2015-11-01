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

import android.app.Notification;
import android.os.*;
import android.os.Process;

import com.oasisfeng.android.service.notification.StatusBarNotificationCompat.SbnCompat;
import com.oasisfeng.nevo.StatusBarNotificationEvo;

public class StatusBarNotificationEvoTest extends StatusBarNotificationCompatTest {

	public void testCreatorAndHelpers() { super.testCreatorAndHelpers(); }
	public void testKeyParsing() { super.testKeyParsing(); }
	public void testParcel() { super.testParcel(); }

	public void testTagOverride() {
		// Override tag (non-null initially)
		StatusBarNotificationEvo sbne = create("pkg", "TAG"/* initially non-null */, 1, Process.myUserHandle(), 12, n(), System.currentTimeMillis());
		String key = SbnCompat.keyOf(sbne);
		assertEquals("TAG", sbne.getTag());
		sbne.setTag("a|b.c");
		assertEquals("a|b.c", sbne.getTag());
		assertEquals(key, sbne.getKey());
		sbne.setTag("");
		assertEquals("", sbne.getTag());
		assertEquals(key, sbne.getKey());
		sbne.setTag(null);
		assertNull(sbne.getTag());
		assertEquals(key, sbne.getKey());
		sbne.setTag("TAG");
		assertEquals("TAG", sbne.getTag());
		assertEquals(key, sbne.getKey());
		// Override tag (null initially)
		sbne = create("pkg", null/* initially null */, 1, Process.myUserHandle(), 12, n(), System.currentTimeMillis());
		key = SbnCompat.keyOf(sbne);
		assertNull(sbne.getTag());
		sbne.setTag("a|b.c");
		assertEquals("a|b.c", sbne.getTag());
		assertEquals(key, sbne.getKey());
		sbne.setTag("");
		assertEquals("", sbne.getTag());
		assertEquals(key, sbne.getKey());
		sbne.setTag(null);
		assertNull(sbne.getTag());
		assertEquals(key, sbne.getKey());
	}

	public void testIdOverride() {
		// Override id
		final StatusBarNotificationEvo sbne = create("pkg", "TAG", - 1, Process.myUserHandle(), 12, n(), System.currentTimeMillis());
		final String key = SbnCompat.keyOf(sbne);
		assertEquals(- 1, sbne.getId());
		sbne.setId(0);
		assertEquals(0, sbne.getId());
		assertEquals(key, sbne.getKey());
		sbne.setId(1);
		assertEquals(1, sbne.getId());
		assertEquals(key, sbne.getKey());
		sbne.setId(- 1);
		assertEquals(- 1, sbne.getId());
		assertEquals(key, sbne.getKey());
	}

	public void testINotification() throws RemoteException {
		final StatusBarNotificationEvo sbne = create("pkg", "TAG", - 1, Process.myUserHandle(), 12, n(), System.currentTimeMillis());
		sbne.notification().addFlags(0b1110);
		assertEquals(0b1110, sbne.notification().getFlags());
		assertEquals(0b1110, sbne.getNotification().flags);
		sbne.notification().removeFlags(0b110);
		assertEquals(0b1000, sbne.notification().getFlags());
		assertEquals(0b1000, sbne.getNotification().flags);

		sbne.notification().setNumber(2);
		assertEquals(2, sbne.getNotification().number);
	}

	public void testExtrasViaIBundle() throws RemoteException {
		final StatusBarNotificationEvo sbne = create("pkg", "TAG", - 1, Process.myUserHandle(), 12, n(), System.currentTimeMillis());
		sbne.notification().addFlags(0b1110);
		assertEquals(0b1110, sbne.notification().getFlags());
		assertEquals(0b1110, sbne.getNotification().flags);
		sbne.notification().removeFlags(0b110);
		assertEquals(0b1000, sbne.notification().getFlags());
		assertEquals(0b1000, sbne.getNotification().flags);

		sbne.notification().setNumber(2);
		assertEquals(2, sbne.getNotification().number);
	}

	public void testNotificationHolder() throws IllegalAccessException {
		final StatusBarNotificationEvo sbne = create("pkg", "TAG", - 1, Process.myUserHandle(), 12, n(), System.currentTimeMillis());

		final StatusBarNotificationEvo unparcel;
		{	// Notification holder ensures no new instance of notification.
			Parcel parcel = Parcel.obtain();
			sbne.writeToParcel(parcel, 0);
			parcel.setDataPosition(0);
			unparcel = StatusBarNotificationEvo.CREATOR.createFromParcel(parcel);
			assertSame(sbne.getNotification(), unparcel.getNotification());
			parcel.recycle();
		} {
			unparcel.getNotification();		// <-- The difference
			Parcel parcel = Parcel.obtain();
			unparcel.writeToParcel(parcel, 0);
			parcel.setDataPosition(0);
			final StatusBarNotificationEvo unparcel_again = StatusBarNotificationEvo.CREATOR.createFromParcel(parcel);
			assertSame(unparcel.getNotification(), unparcel_again.getNotification());
			parcel.recycle();
		}
		// TODO: Multi-process tests
	}

	protected StatusBarNotificationEvo create(final String pkg, final String tag, final int id, final UserHandle user, final int uid, final Notification n, final long time) {
		return new StatusBarNotificationEvo(pkg, null, id, tag, uid, 0, 0, n, user, time);
	}

	protected StatusBarNotificationEvo createFromParcel(final Parcel parcel) {
		return StatusBarNotificationEvo.CREATOR.createFromParcel(parcel);
	}

	private Notification n() { return new Notification(); }
}
