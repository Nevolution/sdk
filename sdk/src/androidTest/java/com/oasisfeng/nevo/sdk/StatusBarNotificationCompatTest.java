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
import android.os.Parcel;
import android.os.Process;
import android.os.UserHandle;
import android.test.AndroidTestCase;

import com.oasisfeng.nevo.StatusBarNotificationCompat;
import com.oasisfeng.nevo.StatusBarNotificationCompat.SbnCompat;

public class StatusBarNotificationCompatTest extends AndroidTestCase {

	public void testCreatorAndHelpers() {
		final long time = System.currentTimeMillis();
		final Notification n = new Notification();
		final UserHandle user = Process.myUserHandle();
		final StatusBarNotificationCompat sbnc = create("pkg", "TAG", 1, user, 12, n, time);
		assertEquals("pkg", sbnc.getPackageName());
		assertEquals(1, sbnc.getId());
		assertEquals("TAG", sbnc.getTag());
		assertEquals(n, sbnc.getNotification());
		assertEquals(user, sbnc.getUser());
		assertEquals(time, sbnc.getPostTime());

		assertEquals(sbnc.getUser(), SbnCompat.userOf(sbnc));
		assertEquals(sbnc.getKey(), SbnCompat.keyOf(sbnc));
		assertEquals(sbnc.getGroupKey(), SbnCompat.groupKeyOf(sbnc));
	}

	public void testParcel() {
		final Notification n = new Notification(); n.tickerText = "ticker";
		final StatusBarNotificationCompat sbnc = create("pkg", "TAG", 1, Process.myUserHandle(), 12, n, System.currentTimeMillis());
		final Parcel parcel = Parcel.obtain();
		sbnc.writeToParcel(parcel, 0);
		parcel.setDataPosition(0);
		final StatusBarNotificationCompat unparcelled = createFromParcel(parcel);
		assertEquals(sbnc.getPackageName(), unparcelled.getPackageName());
		assertEquals(sbnc.getId(), unparcelled.getId());
		assertEquals(sbnc.getTag(), unparcelled.getTag());
		assertEquals("ticker", unparcelled.getNotification().tickerText);
		assertEquals(sbnc.getUser(), unparcelled.getUser());
		assertEquals(sbnc.getPostTime(), unparcelled.getPostTime());

		assertEquals(sbnc.getUser(), SbnCompat.userOf(unparcelled));
		assertEquals(sbnc.getKey(), SbnCompat.keyOf(unparcelled));
		assertEquals(sbnc.getGroupKey(), SbnCompat.groupKeyOf(unparcelled));

		parcel.recycle();
	}

	protected StatusBarNotificationCompat create(final String pkg, final String tag, final int id, final UserHandle user, final int uid, final Notification n, final long time) {
		return new StatusBarNotificationCompat(pkg, null, id, tag, uid, 0, 0, n, user, time);
	}

	protected StatusBarNotificationCompat createFromParcel(final Parcel parcel) {
		return StatusBarNotificationCompat.CREATOR.createFromParcel(parcel);
	}
}
