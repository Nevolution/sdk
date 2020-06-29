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
import android.content.LocusId;
import android.graphics.drawable.Icon;
import android.os.Parcel;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

import static android.os.Build.VERSION_CODES.O;
import static android.os.Build.VERSION_CODES.Q;
import static android.support.annotation.RestrictTo.Scope.LIBRARY;

/**
 * Extend {@link Notification} with convenient mutability.
 *
 * BEWARE: DO NOT mutate the various {@link android.widget.RemoteViews} members directly, instead replace the reference with a clone and mutate on that.
 *
 * Created by Oasis on 2018/4/3.
 */
@Keep public abstract class MutableNotification extends Notification {

	/* Setters for immutable fields in Notification */

	/** @see Notification.Builder#setGroup(String) */
	public abstract void setGroup(String groupKey);
	/** @see Notification.Builder#setSortKey(String) */
	public abstract void setSortKey(String sortKey);
	/** @see Notification.Builder#setSmallIcon(Icon) */
	public abstract void setSmallIcon(final Icon icon);
	/** @see Notification.Builder#setLargeIcon(Icon) */
	public abstract void setLargeIcon(Icon icon);
	/** Currently only supported on Android O+. TODO: If you want it supported on earlier Android versions, please file a feature request on issue tracker
	    @see Notification.Builder#setTimeoutAfter(long) */
	public abstract void setTimeoutAfter(long durationMs);
	/** @see Notification.Builder#setChannelId(String) */
	@RequiresApi(O) public abstract void setChannelId(String channelId);
	/** @see Notification.Builder#setGroupAlertBehavior(int) */
	@RequiresApi(O) public abstract void setGroupAlertBehavior(int behavior);
	/** @see Notification.Builder#setSettingsText(CharSequence) */
	@RequiresApi(O) void setSettingsText(final String text) {}			// TODO: Please file a feature request if you want it eagerly.
	/** @see Notification.Builder#setBubbleMetadata(BubbleMetadata) */
	@RequiresApi(Q) public abstract void setBubbleMetadata(BubbleMetadata metadata);
	/** @see Notification.Builder#setAllowSystemGeneratedContextualActions(boolean) */
	@RequiresApi(Q) public abstract void setAllowSystemGeneratedContextualActions(boolean allowed);
	/** @see Notification.Builder#setLocusId(LocusId) */
	@RequiresApi(Q) public abstract void setLocusId(@Nullable LocusId locusId);

	/* Helpers */

	/** Add action (existent action with the same title will be replaced) */
	public abstract void addAction(final Action action);
	/** @see Notification.Builder#addPerson(String) */
	public abstract void addPerson(final String uri);

	@Override public String toString() { return "Mutable" + super.toString(); }

	@RestrictTo(LIBRARY) MutableNotification() {}
	@RestrictTo(LIBRARY) MutableNotification(final Parcel parcel) { super(parcel); }
}
