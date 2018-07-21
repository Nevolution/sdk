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
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

import java.util.Arrays;

import static android.os.Build.VERSION_CODES.M;
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

	public void setGroup(final String groupKey) { extras.putString(EXTRA_GROUP, groupKey); }
	public void setSortKey(final String sortKey) { extras.putString(EXTRA_SORT_KEY, sortKey); }
	public void setSmallIcon(final Icon icon) { extras.putParcelable(EXTRA_ICON_SMALL, icon); }
	public void setLargeIcon(final Icon icon) { extras.putParcelable(EXTRA_ICON_LARGE, icon); }
	/** Currently only supported on Android O+. TODO: If you want it supported on earlier Android versions, please file a feature request on issue tracker */
	public void setTimeoutAfter(final long durationMs) { extras.putLong(EXTRA_TIMEOUT_AFTER, durationMs); }
	public void setChannelId(final String channelId) { extras.putString(EXTRA_APP_CHANNEL, channelId); }

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
		String[] persons = extras.getStringArray(EXTRA_PEOPLE);
		if (persons != null) {
			persons = Arrays.copyOf(persons, persons.length + 1);
			persons[persons.length - 1] = uri;
		} else persons = new String[] { uri };
		extras.putStringArray(EXTRA_PEOPLE, persons);
	}

	// Delegated getters

	@Override public String getGroup() { return extras.containsKey(EXTRA_GROUP) ? extras.getString(EXTRA_GROUP) : super.getGroup(); }
	@Override public String getSortKey() { return extras.containsKey(EXTRA_SORT_KEY) ? extras.getString(EXTRA_SORT_KEY) : super.getSortKey(); }
	@Override public Icon getSmallIcon() { return extras.containsKey(EXTRA_ICON_SMALL) ? extras.getParcelable(EXTRA_ICON_SMALL) : super.getSmallIcon(); }
	@Override public Icon getLargeIcon() { return extras.containsKey(EXTRA_ICON_LARGE) ? extras.getParcelable(EXTRA_ICON_LARGE) : super.getLargeIcon(); }
	@Override public String getChannelId() { return extras.containsKey(EXTRA_APP_CHANNEL) ? extras.getString(EXTRA_APP_CHANNEL) : super.getChannelId(); }

	// RemoteViews are intentionally always shallowly copied, to reduce cost.
	private static void copyMutableFields(final Notification source, final Notification dest) {
		dest.when = source.when;
		dest.number = source.number;
		dest.contentIntent = source.contentIntent;
		dest.deleteIntent = source.deleteIntent;
		dest.fullScreenIntent = source.fullScreenIntent;
		dest.tickerText = source.tickerText == null ? null : source.tickerText.toString();
		dest.contentView = source.contentView;
		dest.iconLevel = source.iconLevel;
		dest.sound = source.sound;
		//noinspection deprecation, due to still being used in NotificationRecord.calculateAttributes() when "audioAttributes" is absennt.
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

	/** This instance keeps the original immutable values and exposes mutable members, whose original values are kept in an internal Notification instance. */
	private MutableNotificationBaseImpl(final Parcel parcel) {
		super(parcel);
		//noinspection deprecation
		icon = 0;		// Notification.readFromParcelImpl() fills this field, which we never need.
		extras.size();	// Un-parcel extras before copying, to ensure identity equaling of values for later comparison.
		copyMutableFields(this, mOriginalMutableKeeper = new Notification());
	}

	@Override public void writeToParcel(final Parcel out, final int flags) {
		super.writeToParcel(out, flags);
	}

	private final transient Notification mOriginalMutableKeeper;	// The instance with original mutable field values (intact).

	public static final Creator<MutableNotificationBaseImpl> CREATOR = new Parcelable.Creator<MutableNotificationBaseImpl>() {
		public MutableNotificationBaseImpl createFromParcel(final Parcel parcel) { return new MutableNotificationBaseImpl(parcel); }
		public MutableNotificationBaseImpl[] newArray(final int size) { return new MutableNotificationBaseImpl[size]; }
	};
}
