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

package com.oasisfeng.android.service.notification;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Backward support for {@link android.service.notification.StatusBarNotification StatusBarNotification}
 *
 * Created by Oasis on 2014/11/28.
 */
public class StatusBarNotificationCompat extends StatusBarNotification {

	private final String key;
	private final String groupKey;

	public StatusBarNotificationCompat(final String pkg, final String opPkg, final int id, final String tag, final int uid, final int initialPid, final int score, final Notification notification, final UserHandle user, final long postTime) {
		super(pkg, opPkg, id, tag, uid, initialPid, score, notification, user, postTime);
		key = SbnCompat.keyOf(this); groupKey = SbnCompat.groupKeyOf(this);
	}

	@Override public String getKey() { return key; }
	@Override public String getGroupKey() { return groupKey; }
	@TargetApi(VERSION_CODES.KITKAT_WATCH) private String getBaseKey() { return super.getKey(); }
	@TargetApi(VERSION_CODES.LOLLIPOP) private String getBaseGroupKey() { return super.getGroupKey(); }

	@SuppressLint("NewApi") /** {@link StatusBarNotification#getUser()} is hidden but accessible in API level 18~20 */
    @Override public UserHandle getUser() {
        try { return super.getUser(); }
        catch (final Throwable t) { return android.os.Process.myUserHandle(); }
    }

    @SuppressLint("NewApi") /** {@link StatusBarNotification#getUser()} is hidden but accessible in API level 18~20 */
    public static UserHandle getUser(final StatusBarNotification sbn) {
        return sbn.getUser();
    }

    public static Key parseKey(final String key) {
		if (VERSION.SDK_INT >= VERSION_CODES.KITKAT_WATCH) {
			// userId + "|" + pkg + "|" + id + "|" + tag + "|" + uid
			final int pos_last = key.lastIndexOf('|');
			final String[] user_pkg_id_tag = key.substring(0, pos_last).split("\\|", 4);
			String tag = user_pkg_id_tag[3];
			if ("null".equals(tag)) tag = null;             // Nasty hole (Google should fix this!)
			UserHandle user; int id = 0;
			try {
				user = toUserHandle(Integer.parseInt(user_pkg_id_tag[0]));
			} catch (final NumberFormatException e) {
				Log.w(TAG, "Malformed key: " + key);
				user = android.os.Process.myUserHandle();
			}
			try {
				id = Integer.parseInt(user_pkg_id_tag[2]);
			} catch (final NumberFormatException e) { Log.w(TAG, "Malformed key: " + key); }
			return new Key(user, user_pkg_id_tag[1], id, tag, Integer.parseInt(key.substring(pos_last + 1)));
		}
		final String[] pkg_id_tag = key.split("\\|", 3);    // Tag is optional
		return new Key(null, pkg_id_tag[0], Integer.valueOf(pkg_id_tag[1]), pkg_id_tag.length > 2 ? pkg_id_tag[2] : null, -1);
	}

	// TODO: Reflection?
	private static UserHandle toUserHandle(final int user) {
		final Parcel parcel = Parcel.obtain();
		try {
			parcel.writeInt(user);
			parcel.setDataPosition(0);
			return UserHandle.readFromParcel(parcel);
		} finally {
			parcel.recycle();
		}
	}

	public static final class Key {
		Key(final UserHandle user, final String pkg, final int id, final String tag, final int uid) {
			this.user = user; this.pkg = pkg; this.id = id; this.tag = tag; this.uid = uid;
		}
		/** User of this notification, or null on Android prior to 5.0 */
		public final UserHandle user;
		public final String pkg;
		public final int id;
		public final String tag;
		/** The UID of app who posted this notification, or -1 on Android prior to 5.0 */
		public final int uid;
	}

	public StatusBarNotificationCompat(final Parcel parcel) { super(parcel); key = SbnCompat.keyOf(this); groupKey = SbnCompat.groupKeyOf(this); }

	public static final Parcelable.Creator<StatusBarNotificationCompat> CREATOR = new Parcelable.Creator<StatusBarNotificationCompat>() {
		public StatusBarNotificationCompat createFromParcel(final Parcel parcel) { return new StatusBarNotificationCompat(parcel); }
		public StatusBarNotificationCompat[] newArray(final int size) { return new StatusBarNotificationCompat[size]; }
	};

	private static final String TAG = "SbnCompat";

	/** Shortcut for cleaner code */
	public static class SbnCompat {

		public static String keyOf(final StatusBarNotification sbn) {
			if (sbn instanceof StatusBarNotificationCompat) {
				final String key = ((StatusBarNotificationCompat) sbn).key;
				if (key != null) return key;				// May actually be null when called by the constructor
			}
			if (VERSION.SDK_INT >= VERSION_CODES.KITKAT_WATCH) return StatusBarNotificationCompat20.getKey(sbn);
			final StringBuilder key = new StringBuilder(sbn.getPackageName()).append('|').append(sbn.getId());
			if (sbn.getTag() != null) key.append('|').append(sbn.getTag()); // Tag must be the last one since it may contain '|'
			return key.toString();
		}

		public static String groupKeyOf(final StatusBarNotification sbn) {
			if (sbn instanceof StatusBarNotificationCompat) {
				final String group_key = ((StatusBarNotificationCompat) sbn).groupKey;
				if (group_key != null) return group_key;	// May actually be null when called by the constructor
			}
			if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) return StatusBarNotificationCompat21.getGroupKey(sbn);
			final String group = NotificationCompat.getGroup(sbn.getNotification());
			final String sortKey = NotificationCompat.getSortKey(sbn.getNotification());
			if (group == null && sortKey == null) return keyOf(sbn);        // a group of one
			return sbn.getPackageName() + "|" + (group == null ? "p:" + sbn.getNotification().priority : "g:" + group);
		}
	}

	@TargetApi(VERSION_CODES.KITKAT_WATCH)
	private static class StatusBarNotificationCompat20 {

		public static String getKey(final StatusBarNotification sbn) {
			if (sbn instanceof StatusBarNotificationCompat)
				return ((StatusBarNotificationCompat) sbn).getBaseKey();
			return sbn.getKey();
		}
	}

	@TargetApi(VERSION_CODES.LOLLIPOP)
	private static class StatusBarNotificationCompat21 {

		public static String getGroupKey(final StatusBarNotification sbn) {
			if (sbn instanceof StatusBarNotificationCompat)
				return ((StatusBarNotificationCompat) sbn).getBaseGroupKey();
			return sbn.getGroupKey();
		}
	}
}
