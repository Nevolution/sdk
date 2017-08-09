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

import android.app.Notification;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.RestrictTo;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.Arrays;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * Wrapper for a Notification object that allows transfer across a one-way binder
 * without sending large amounts of data over a one-way transaction.
 */
@RestrictTo(LIBRARY_GROUP)
public final class NotificationHolder extends INotification.Stub {

	@RestrictTo(LIBRARY_GROUP)
	public interface OnDemandSuppliers {
		RemoteViews getContentView(Notification n);
		/** @return whether the content view is changed */
		boolean setContentView(Notification n, RemoteViews views);
		boolean hasBigContentView(Notification n);
		RemoteViews getBigContentView(Notification n);
		/** @return whether the big content view is changed */
		boolean setBigContentView(Notification n, RemoteViews views);
		boolean hasHeadsUpContentView(Notification n);
		RemoteViews getHeadsUpContentView(Notification n);
		/** @return whether the heads-up content view is changed */
		boolean setHeadsUpContentView(Notification n, RemoteViews views);
	}

	NotificationHolder(final Notification notification, final OnDemandSuppliers suppliers) {
		n = notification;
		extras = new ChangeTrackingBundleHolder(NotificationCompat.getExtras(n));
		this.suppliers = suppliers;
	}

	public NotificationHolder(final Notification notification) {
		n = notification;
		extras = new ChangeTrackingBundleHolder(NotificationCompat.getExtras(n));
		suppliers = new OnDemandSuppliers() {
			@Override public RemoteViews getContentView(final Notification n) {
				return n.contentView;
			}
			@Override public boolean setContentView(final Notification n, final RemoteViews views) {
				n.contentView = views; return true;
			}
			@Override public boolean hasBigContentView(final Notification n) {
				return n.bigContentView != null;
			}
			@Override public RemoteViews getBigContentView(final Notification n) {
				return n.bigContentView;
			}
			@Override public boolean setBigContentView(final Notification n, final RemoteViews views) {
				n.bigContentView = views; return true;
			}
			@Override public boolean hasHeadsUpContentView(final Notification n) {
				return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && n.headsUpContentView != null;
			}
			@Override public RemoteViews getHeadsUpContentView(final Notification n) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) return n.headsUpContentView;
				return null;
			}
			@Override public boolean setHeadsUpContentView(final Notification n, final RemoteViews views) {
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false;
				n.headsUpContentView = views; return true;
			}
		};
	}

	@Override public Notification get() { return n; }	// No suppliers here, caller should be aware of this

	@Override public ChangeTrackingBundleHolder extras() { return extras; }

	@Override public RemoteViews getContentView() {
		return suppliers.getContentView(n);
	}
	@Override public void setContentView(final RemoteViews views) {
		if (suppliers.setContentView(n, views)) updated |= FIELD_CONTENT_VIEW;
	}

	@Override public boolean hasBigContentView() {
		return suppliers.hasBigContentView(n);
	}
	@Override public RemoteViews getBigContentView() {
		return suppliers.getBigContentView(n);
	}
	@Override public void setBigContentView(final RemoteViews views) {
		if (suppliers.setBigContentView(n, views)) updated |= FIELD_BIG_CONTENT_VIEW;
	}

	@Override public boolean hasHeadsUpContentView() {
		return suppliers.hasHeadsUpContentView(n);
	}
	@Override public RemoteViews getHeadsUpContentView() {
		return suppliers.getHeadsUpContentView(n);
	}
	@Override public void setHeadsUpContentView(final RemoteViews views) {
		if (suppliers.setHeadsUpContentView(n, views)) updated |= FIELD_HEADS_UP_CONTENT_VIEW;
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

    @Override public Notification.Action[] getActions() { return n.actions; }
    @Override public void addAction(final Notification.Action action) {
		if (n.actions != null) {
			for (final Notification.Action existent_action : n.actions)
				if (TextUtils.equals(existent_action.title, action.title)) return;		// De-dup
			final int length = n.actions.length;
			n.actions = Arrays.copyOf(n.actions, length + 1);
			n.actions[length] = action;
		} else n.actions = new Notification.Action[] { action };
	}

	@Override public long[] getVibrate() { return n.vibrate; }
	@Override public void setVibrate(final long[] vibrate) {
		n.vibrate = vibrate;
		updated |= FIELD_VIBRATE;
	}

	private static final String KEY_GROUP = "android.support.groupKey";

	/* Updated field will no longer reflect the changes in extras, use getExtrasChangeCount() instead.
	public static final int FIELD_EXTRAS = 1; */
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

	@Retention(RetentionPolicy.SOURCE) @IntDef(value = { FIELD_CONTENT_VIEW, FIELD_BIG_CONTENT_VIEW, FIELD_HEADS_UP_CONTENT_VIEW,
			FIELD_NUMBER, FIELD_WHEN, FIELD_COLOR, FIELD_FLAGS, FIELD_GROUP, FIELD_PRIORITY, FIELD_VIBRATE }, flag = true)
	public @interface UpdatedField {}

	public @UpdatedField int getUpdatedFields() {
		return updated;
	}

	public void resetUpdatedFields() {
		updated = 0;
	}

	public boolean isFieldUpdated(@UpdatedField final int field) {
		return (updated & field) != 0;
	}

	public int countChangedExtras() {
		return extras.countChanges();
	}

	private final Notification n;
	private final ChangeTrackingBundleHolder extras;
	private final OnDemandSuppliers suppliers;
	private @UpdatedField int updated;

	static final String TAG = "Nevo.Holder";

	@RestrictTo(LIBRARY_GROUP) static class Impl20 {

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
