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
import android.app.Person;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.O;
import static android.os.Build.VERSION_CODES.P;
import static android.os.Build.VERSION_CODES.Q;
import static android.support.annotation.RestrictTo.Scope.LIBRARY;

/**
 * Implementation of {@link MutableNotification} with all mutations stored in {@link #extras}, thus no parceling difference than {@link Notification}.
 *
 * Created by Oasis on 2018/4/3.
 */
@RestrictTo(LIBRARY) @RequiresApi(M) class MutableNotificationBaseImpl extends MutableNotification {

	static final String EXTRA_GROUP = "nevo.group";
	static final String EXTRA_SORT_KEY = "nevo.sort";
	static final String EXTRA_ICON_SMALL = "nevo.icon";
	static final String EXTRA_ICON_LARGE = "nevo.icon.large";
	static final String EXTRA_TIMEOUT_AFTER = "nevo.timeout";
	static final String EXTRA_APP_CHANNEL = "nevo.channel";
	static final String EXTRA_GROUP_ALERT_BEHAVIOR = "nevo.group.alert";
	static final String EXTRA_BUBBLE_METADATA = "nevo.bubble";
	static final String EXTRA_ALLOW_SYS_GEN_ACTIONS = "nevo.allow.actions";

	@Override public void setGroup(final String groupKey) {
		if (Objects.equals(groupKey, super.getGroup())) extras.remove(EXTRA_GROUP);
		else extras.putString(EXTRA_GROUP, groupKey);
	}
	@Override public void setSortKey(final String sortKey) {
		if (Objects.equals(sortKey, super.getSortKey())) extras.remove(EXTRA_SORT_KEY);
		else extras.putString(EXTRA_SORT_KEY, sortKey);
	}
	@Override public void setSmallIcon(final Icon icon) {
		if (icon == super.getSmallIcon()) extras.remove(EXTRA_ICON_SMALL);	// TODO: Implement equality check for Icon.
		else extras.putParcelable(EXTRA_ICON_SMALL, icon);
	}
	@Override public void setLargeIcon(final Icon icon) {
		if (icon == super.getLargeIcon()) extras.remove(EXTRA_ICON_LARGE);	// TODO: Implement equality check for Icon.
		else extras.putParcelable(EXTRA_ICON_LARGE, icon);
	}
	/** Currently only supported on Android O+. TODO: If you want it supported on earlier Android versions, please file a feature request on issue tracker */
	@RequiresApi(O) @Override public void setTimeoutAfter(final long durationMs) {
		if (durationMs == super.getTimeoutAfter()) extras.remove(EXTRA_TIMEOUT_AFTER);
		else extras.putLong(EXTRA_TIMEOUT_AFTER, durationMs);
	}
	@RequiresApi(O) @Override public void setChannelId(final String channelId) {
		if (Objects.equals(channelId, super.getChannelId())) extras.remove(EXTRA_APP_CHANNEL);
		else extras.putString(EXTRA_APP_CHANNEL, channelId);
	}
	@RequiresApi(O) @Override public void setGroupAlertBehavior(final int behavior) {
		if (behavior == super.getGroupAlertBehavior()) extras.remove(EXTRA_GROUP_ALERT_BEHAVIOR);
		else extras.putInt(EXTRA_GROUP_ALERT_BEHAVIOR, behavior);
	}
	@RequiresApi(Q) @Override public void setBubbleMetadata(final BubbleMetadata metadata) {
		if (Objects.equals(metadata, super.getBubbleMetadata())) extras.remove(EXTRA_BUBBLE_METADATA);
		else extras.putParcelable(EXTRA_BUBBLE_METADATA, metadata);
	}
	@RequiresApi(Q) @Override public void setAllowSystemGeneratedContextualActions(final boolean allowed) {
		if (allowed == super.getAllowSystemGeneratedContextualActions()) extras.remove(EXTRA_ALLOW_SYS_GEN_ACTIONS);
		else extras.putBoolean(EXTRA_ALLOW_SYS_GEN_ACTIONS, allowed);
	}

	// Helpers

	public void addAction(final Action action) {
		if (actions != null) {
			for (int i = 0; i < actions.length; i++) {		// Replace if action with the same title already exists
				final Action existent_action = actions[i];
				if (existent_action.title == null || ! existent_action.title.equals(action.title)) continue;
				actions[i] = action;
				return;
			}
			actions = Arrays.copyOf(actions, actions.length + 1);
			actions[actions.length - 1] = action;
		} else actions = new Action[] { action };
	}

	public void addPerson(final String uri) {
		if (SDK_INT >= P) {
			ArrayList<Person> persons = extras.getParcelableArrayList(EXTRA_PEOPLE_LIST);
			if (persons == null) persons = new ArrayList<>(1);
			persons.add(new Person.Builder().setUri(uri).build());
		} else {
			String[] persons = extras.getStringArray(EXTRA_PEOPLE);
			if (persons != null) {
				persons = Arrays.copyOf(persons, persons.length + 1);
				persons[persons.length - 1] = uri;
			} else persons = new String[] { uri };
			extras.putStringArray(EXTRA_PEOPLE, persons);
		}
	}

	// Delegated getters

	@Override public @Nullable String getGroup() { return extras.getString(EXTRA_GROUP, super.getGroup()); }
	@Override public @Nullable String getSortKey() { return extras.getString(EXTRA_SORT_KEY, super.getSortKey()); }
//	@Override public Icon getSmallIcon() { return extras.containsKey(EXTRA_ICON_SMALL) ? extras.getParcelable(EXTRA_ICON_SMALL) : super.getSmallIcon(); }
	@Override public @Nullable Icon getLargeIcon() { return extras.containsKey(EXTRA_ICON_LARGE) ? extras.getParcelable(EXTRA_ICON_LARGE) : super.getLargeIcon(); }
	@Override public long getTimeoutAfter() { return extras.getLong(EXTRA_TIMEOUT_AFTER, super.getTimeoutAfter()); }
	@Override public @Nullable String getChannelId() { return extras.getString(EXTRA_APP_CHANNEL, super.getChannelId()); }
	@Override public int getGroupAlertBehavior() { return extras.getInt(EXTRA_GROUP_ALERT_BEHAVIOR, super.getGroupAlertBehavior()); }
	@Override public @Nullable BubbleMetadata getBubbleMetadata() { return extras.containsKey(EXTRA_BUBBLE_METADATA) ? extras.getParcelable(EXTRA_BUBBLE_METADATA) : super.getBubbleMetadata(); }
	@Override public boolean getAllowSystemGeneratedContextualActions() { return extras.getBoolean(EXTRA_ALLOW_SYS_GEN_ACTIONS, super.getAllowSystemGeneratedContextualActions()); }

	// RemoteViews are intentionally always shallowly copied, to reduce cost.
	private static void copyMutableFields(final Notification source, final Notification dest) {
		source.extras.size();	// Un-parcel extras before copying, to ensure identity equaling of values for later comparison.

		dest.when = source.when;
		dest.number = source.number;
		dest.contentIntent = source.contentIntent;
		dest.deleteIntent = source.deleteIntent;
		dest.fullScreenIntent = source.fullScreenIntent;
		dest.tickerText = source.tickerText == null ? null : source.tickerText.toString();
		dest.contentView = source.contentView;
		dest.iconLevel = source.iconLevel;
		dest.sound = source.sound;
		// "audioStreamType" is still being used in NotificationRecord.calculateAttributes() when "audioAttributes" is absennt.
		dest.audioStreamType = source.audioStreamType;
		dest.audioAttributes = source.audioAttributes;
		dest.vibrate = source.vibrate == null ? null : source.vibrate.clone();
		dest.ledARGB = source.ledARGB;
		dest.ledOnMS = source.ledOnMS;
		dest.ledOffMS = source.ledOffMS;
		dest.defaults = source.defaults;
		dest.flags = source.flags;
		dest.priority = source.priority;
		dest.category = source.category;
		dest.extras = source.extras == null ? null : new Bundle(source.extras);

		if (source.actions != null) {		// Notification.Action is actually mutable due to Action.getExtras().
			dest.actions = new Action[source.actions.length];
			for (int i = 0; i < source.actions.length; i ++) {
				final Action action = source.actions[i];
				if (action != null) {
					action.getExtras().size();		// Un-parcel extras before cloning, to ensure identity equaling of values for later comparison.
					dest.actions[i] = action.clone();
				}
			}
		} else dest.actions = null;

		dest.bigContentView = source.bigContentView;
		dest.headsUpContentView = source.headsUpContentView;
		dest.visibility = source.visibility;
		dest.publicVersion = source.publicVersion == null ? null : source.publicVersion.clone();
		dest.color = source.color;
	}

	Notification getOriginalMutableKeeper() { return mOriginalMutableKeeper; }

	/** For derived class only. */
	MutableNotificationBaseImpl(final Notification original) { mOriginalMutableKeeper = original; }

	/** For derived class only. */
	MutableNotificationBaseImpl(final Notification original, final Parcel parcel) { super(parcel); mOriginalMutableKeeper = original; }

	/** This instance keeps the original immutable values and exposes mutable members, whose original values are kept in an internal Notification instance. */
	MutableNotificationBaseImpl(final Parcel parcel, final boolean initialize) {
		super(parcel);
		icon = 0;		// Notification.readFromParcelImpl() fills this field, which we never need.
		if (initialize) initialize();
	}

	/** Initialization can be postponed to reduce the cost of instance creation in certain cases. */
	private void initialize() {
		copyMutableFields(this, mOriginalMutableKeeper = new Notification());
	}

	void ensureInitialized() {
		if (mOriginalMutableKeeper == null) initialize();
	}

	private transient Notification mOriginalMutableKeeper;	// The instance with original mutable field values (intact).

	public static final Creator<MutableNotificationBaseImpl> CREATOR = new Parcelable.Creator<MutableNotificationBaseImpl>() {
		public MutableNotificationBaseImpl createFromParcel(final Parcel parcel) { return new MutableNotificationBaseImpl(parcel, true); }
		public MutableNotificationBaseImpl[] newArray(final int size) { return new MutableNotificationBaseImpl[size]; }
	};
}
