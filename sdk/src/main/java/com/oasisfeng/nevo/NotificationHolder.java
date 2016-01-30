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

package com.oasisfeng.nevo;

import android.annotation.TargetApi;
import android.app.Notification;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

/**
 * Wrapper for a Notification object that allows transfer across a one-way binder
 * without sending large amounts of data over a one-way transaction.
 */
public final class NotificationHolder extends INotification.Stub {

	public NotificationHolder(final Notification notification) {
		n = notification;
		extras = new ChangeTrackingBundleHolder(NotificationCompat.getExtras(n));
	}

	@Override public Notification get() { return n; }

	@Override public ChangeTrackingBundleHolder extras() { return extras; }

	@Override public RemoteViews getContentView() { return n.contentView; }
	@Override public void setContentView(final RemoteViews views) {
		n.contentView = views;
		updated |= FIELD_CONTENT_VIEW;
	}

	@Override public boolean hasBigContentView() { return n.bigContentView != null; }
	@Override public RemoteViews getBigContentView() { return n.bigContentView; }
	@Override public void setBigContentView(final RemoteViews views) {
		n.bigContentView = views;
		updated |= FIELD_BIG_CONTENT_VIEW;
	}

	@Override public boolean hasHeadsUpContentView() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && n.headsUpContentView != null;
	}
	@Override public RemoteViews getHeadsUpContentView() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			return n.headsUpContentView;
		return null;
	}
	@Override public void setHeadsUpContentView(final RemoteViews view) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			n.headsUpContentView = view;
			updated |= FIELD_HEADS_UP_CONTENT_VIEW;
		}
	}

	@Override public long getWhen() { return n.when; }
	@Override public void setWhen(final long when) { n.when = when; updated |= FIELD_WHEN; }

	@Override public int getNumber() { return n.number; }
	@Override public void setNumber(final int number) { n.number = number; updated |= FIELD_NUMBER; }

	@Override public int getColor() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) return n.color;
		else return 0;
	}
	@Override public void setColor(final int color) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			n.color = color;
			updated |= FIELD_COLOR;
		}
	}

	@Override public int getFlags() { return n.flags; }
	@Override public void addFlags(final int flags) {
		if ((n.flags & flags) == flags) return;
		n.flags |= flags;
		updated |= FIELD_FLAGS;
	}
	@Override public void removeFlags(final int flags) {
		if ((n.flags & flags) == 0) return;
		n.flags &= ~ flags;
		updated |= FIELD_FLAGS;
	}

	@Override public String getGroup() { return NotificationCompat.getGroup(n); }
	@Override public void setGroup(final String group) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
			Impl20.setGroup(n, group);
		else NotificationCompat.getExtras(n).putString(KEY_GROUP, group);
		updated |= FIELD_GROUP;
	}

	@Override public int getPriority() { return n.priority; }
	@Override public void setPriority(final int priority) {
		n.priority = priority;
		updated |= FIELD_PRIORITY;
	}

	@Override public long[] getVibrate() { return n.vibrate; }
	@Override public void setVibrate(final long[] vibrate) {
		n.vibrate = vibrate;
		updated |= FIELD_VIBRATE;
	}

	@TargetApi(Build.VERSION_CODES.M)
	@Override public Icon getSmallIcon() { return n.getSmallIcon(); }
	@Override public void setSmallIcon(final Icon icon) {
		try {
			final Field Notification_mSmallIcon = Notification.class.getDeclaredField("mSmallIcon");
			Notification_mSmallIcon.setAccessible(true);
			Notification_mSmallIcon.set(n, icon);
		} catch (final NoSuchFieldException e) {
			Log.w(TAG, "Incompatible ROM: No field Notification.mSmallIcon");
		} catch (final IllegalAccessException ignored) {}
	}
	@TargetApi(Build.VERSION_CODES.M)
	@Override public Icon getLargeIcon() { return n.getLargeIcon(); }
	@Override public void setLargeIcon(final Icon icon) {
		try {
			final Field Notification_mLargeIcon = Notification.class.getDeclaredField("mLargeIcon");
			Notification_mLargeIcon.setAccessible(true);
			Notification_mLargeIcon.set(n, icon);
		} catch (final NoSuchFieldException e) {
			Log.w(TAG, "Incompatible ROM: No field Notification.mLargeIcon");
		} catch (final IllegalAccessException ignored) {}
	}

	private static final String KEY_GROUP = "android.support.groupKey";

	public static final int FIELD_EXTRAS = 1;
	public static final int FIELD_CONTENT_VIEW = 1 << 1;
	public static final int FIELD_BIG_CONTENT_VIEW = 1 << 2;
	public static final int FIELD_HEADS_UP_CONTENT_VIEW = 1 << 3;
	public static final int FIELD_NUMBER = 1 << 4;
	public static final int FIELD_WHEN = 1 << 5;
	public static final int FIELD_COLOR = 1 << 6;
	public static final int FIELD_FLAGS = 1 << 7;
	public static final int FIELD_GROUP = 1 << 8;
	public static final int FIELD_PRIORITY = 1 << 9;
	public static final int FIELD_VIBRATE = 1 << 10;

	@Retention(RetentionPolicy.SOURCE) @IntDef(value = { FIELD_EXTRAS, FIELD_CONTENT_VIEW, FIELD_BIG_CONTENT_VIEW, FIELD_HEADS_UP_CONTENT_VIEW,
			FIELD_NUMBER, FIELD_WHEN, FIELD_COLOR, FIELD_FLAGS, FIELD_GROUP, FIELD_PRIORITY, FIELD_VIBRATE }, flag = true)
	public @interface UpdatedField {}

	public int getUpdatedFields() {
		checkExtrasUpdate();
		return updated;
	}

	public boolean isFieldUpdated(@UpdatedField final int field) {
		if ((field & FIELD_EXTRAS) != 0) checkExtrasUpdate();
		return (updated & field) != 0;
	}

	private void checkExtrasUpdate() {
		if ((updated & FIELD_EXTRAS) == 0 && extras.isChanged())
			updated |= FIELD_EXTRAS;	// Check extras update on-demand
	}

	private final Notification n;
	private final ChangeTrackingBundleHolder extras;
	private @UpdatedField int updated;

	static final String TAG = "Nevo.Holder";

	static class Impl20 {

		static void setGroup(final Notification n, final String group) {
			if (Notification_mGroupKey != null) try {
				Notification_mGroupKey.set(n, group);
			} catch (IllegalAccessException | IllegalArgumentException ignored) {}	// Should never happen
		}

		private static final Field Notification_mGroupKey;
		static {
			Field f = null;
			try {
				f = Notification.class.getDeclaredField("mGroupKey");
				if (f.getType() != String.class) {
					Log.e(TAG, "Incompatible ROM: Unexpected field type - " + f);
					f = null;
				} else f.setAccessible(true);
			} catch (final NoSuchFieldException e) {
				Log.e(TAG, "Incompatible ROM: No field Notification.mGroupKey");
			}
			Notification_mGroupKey = f;
		}
	}
}
