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
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;
import static android.support.annotation.RestrictTo.Scope.LIBRARY;

/**
 * Mutable version of {@link StatusBarNotification} with effective parceling.
 *
 * Created by Oasis on 2015/1/18.
 */
@Keep @RequiresApi(M) public class MutableStatusBarNotification extends StatusBarNotification {

	@Override public MutableNotification getNotification() { return (MutableNotification) super.getNotification(); }

	public void setTag(final @Nullable String tag) {
		if (Objects.equals(tag, mTag)) return;
		mTag = tag;
		updateKey();
	}

	public void setId(final int id) {
		if (mId == id) return;
		mId = id;
		updateKey();
	}

	private void updateKey() {
		if (! Objects.equals(mTag, super.getTag()) || mId != super.getId()) {		// Initial PID and score has no contribution to generated key.
			final StatusBarNotification sbn = new StatusBarNotification(getPackageName(), null, getId(), getTag(),
					getUid(this), 0, 0, super.getNotification(), getUser(), getPostTime());
			if (SDK_INT >= N) sbn.setOverrideGroupKey(getOverrideGroupKey());
			mKey = sbn.getKey();
		} else mKey = null;
	}

	@Override public String getTag() { return mTag; }
	@Override public int getId() { return mId; }
	@Override public String getKey() { return mKey != null ? mKey : super.getKey(); }
	public String getOriginalKey() { return super.getKey(); }
	public String getOriginalTag() { return super.getTag(); }
	public int getOriginalId() { return super.getId(); }

	@RequiresApi(N) @Override public void setOverrideGroupKey(final String override_group_key) {	// Use extra to keep initial value.
		final String value_before = getOverrideGroupKey();
		if (Objects.equals(override_group_key, value_before)) return;
		final Bundle extras = super.getNotification().extras;
		if (! extras.containsKey(EXTRA_ORIGINAL_OVERRIDE_GROUP)) {
			if (value_before != null) extras.putString(EXTRA_ORIGINAL_OVERRIDE_GROUP, value_before);
		} else if (Objects.equals(override_group_key, extras.getString(EXTRA_ORIGINAL_OVERRIDE_GROUP)))
			extras.remove(EXTRA_ORIGINAL_OVERRIDE_GROUP);
		super.setOverrideGroupKey(override_group_key);
	}

	@Override public String toString() {
		final StringBuilder string = new StringBuilder("StatusBarNotificationEvo(key=");
		string.append(getOriginalKey());
		if (mKey != null) string.append(" -> ").append(mKey);
		string.append(')');
		return string.toString();
	}

	@RestrictTo(LIBRARY) protected MutableStatusBarNotification(final String pkg, final String opPkg, final int id, final String tag,
			final int uid, final int initialPid, final MutableNotification notification, final UserHandle user, final long postTime) {
		super(pkg, opPkg, id, tag, uid, initialPid, 0/* unused */, notification, user, postTime);
		mTag = tag;
		mId = id;
	}

	@RestrictTo(LIBRARY) static String getOpPkg(final StatusBarNotification sbn) { return sbn.getPackageName(); }	// TODO
	@RestrictTo(LIBRARY) static int getInitialPid(final StatusBarNotification sbn) { return 0; }					// TODO
	@RestrictTo(LIBRARY) protected static int getUid(final StatusBarNotification sbn) {
		if (sMethodGetUid != null)
			try { return (int) sMethodGetUid.invoke(sbn); } catch (final Exception ignored) {}
		if (sFieldUid != null)
			try { return (int) sFieldUid.get(sbn); } catch (final IllegalAccessException ignored) {}
		// TODO: PackageManager.getPackageUid()
		Log.e(TAG, "Incompatible ROM: StatusBarNotification");
		return 0;
	}

	private static final @Nullable Method sMethodGetUid;
	private static final @Nullable Field sFieldUid;
	static {
		Method method = null; Field field = null;
		try {
			method = StatusBarNotification.class.getMethod("getUid");
		} catch (final NoSuchMethodException ignored) {}
		sMethodGetUid = method;
		if (method == null) try {       // If no such method, try accessing the field
			field = StatusBarNotification.class.getDeclaredField("uid");
			field.setAccessible(true);
		} catch (final NoSuchFieldException ignored) {}
		sFieldUid = field;
	}

	@RestrictTo(LIBRARY) public void setAllowIncrementalWriteBack() { mAllowIncWriteBack = true; }

	@Override public void writeToParcel(final Parcel out, final int flags) {
		if (mAllowIncWriteBack && (flags & PARCELABLE_WRITE_RETURN_VALUE) != 0) {
			writeMutableFieldsToParcel(out);
			// Use remote implementation to ensure the consistency of parceling across SDK versions.
			final MutableNotification mutable = getNotification();
			RemoteImplementation.writeBackToParcel(out, flags, mutable, ((MutableNotificationBaseImpl) mutable).getOriginalMutableKeeper());
		} else {	// Store original values in extras if mutated, and write to parcel as StatusBarNotification with mutated values.
			final Bundle extras = super.getNotification().extras;
			final boolean tag_mutated, id_mutated;
			if (tag_mutated = (mTag == null ? super.getTag() != null : ! mTag.equals(super.getTag())))
				extras.putString(EXTRA_ORIGINAL_TAG, super.getTag());
			if (id_mutated = (mId != super.getId())) extras.putInt(EXTRA_ORIGINAL_ID, super.getId());
			writeToParcelAsSuper(out, flags);
			if (tag_mutated) extras.remove(EXTRA_ORIGINAL_TAG);
			if (id_mutated) extras.remove(EXTRA_ORIGINAL_ID);
		}
	}

	// Same as AOSP super implementation (with mutated values), but still use our implementation here for consistency with MutableStatusBarNotification(Parcel).
	private void writeToParcelAsSuper(final Parcel out, final int flags) {
		out.writeString(super.getPackageName());
		out.writeString(getOpPkg(this));
		out.writeInt(getId());

		final String tag = getTag();
		if (tag != null) {
			out.writeInt(1);
			out.writeString(tag);
		} else out.writeInt(0);

		out.writeInt(getUid(this));
		out.writeInt(getInitialPid(this));
		super.getNotification().writeToParcel(out, flags);
		getUser().writeToParcel(out, flags);
		out.writeLong(getPostTime());

		if (SDK_INT < N) return;
		final String override_group_key = getOverrideGroupKey();
		if (override_group_key != null) {
			out.writeInt(1);
			out.writeString(override_group_key);
		} else out.writeInt(0);
	}

	protected MutableStatusBarNotification(final Parcel in) {
		// The only difference from AOSP super constructor: create MutableNotification instead of Notification in place.
		this(in.readString(), in.readString(), in.readInt(), in.readInt() == 0 ? null : in.readString(), in.readInt(), in.readInt(),
				MutableNotificationBaseImpl.CREATOR.createFromParcel(in), UserHandle.readFromParcel(in), in.readLong(),
				SDK_INT < N ? null : (in.readInt() != 0 ? in.readString() : null));
	}

	/** Restore original values from extras if present */
	private MutableStatusBarNotification(final String pkg, final String opPkg, final int id, final String tag, final int uid, final int initialPid,
										 final MutableNotification n, final UserHandle user, final long postTime, final String override_group) {
		super(pkg, opPkg, extractOriginal(n, EXTRA_ORIGINAL_ID, id), extractOriginal(n, EXTRA_ORIGINAL_TAG, tag), uid, initialPid, 0, n, user, postTime);
		setTag(tag);
		setId(id);
		if (SDK_INT >= N) super.setOverrideGroupKey(override_group);
	}

	private static <T> T extractOriginal(final Notification n, final String key, final T value) {
		final Bundle extras = n.extras;
		if (extras.containsKey(key)) try {
			@SuppressWarnings("unchecked") final T original = (T) extras.get(key);
			extras.remove(key);
			return original;
		} catch (final ClassCastException ignored) {}
		return value;
	}

	private void writeMutableFieldsToParcel(final Parcel out) {
		if (mTag == null ? super.getTag() != null : ! mTag.equals(super.getTag())) {
			if (mTag != null) {
				out.writeInt(1);
				out.writeString(mTag);
			} else out.writeInt(- 1);
		} else out.writeInt(0);

		out.writeInt(mId);

		if (SDK_INT >= N) {
			final Bundle extras = super.getNotification().extras;
			if (extras.containsKey(EXTRA_ORIGINAL_OVERRIDE_GROUP)) {
				if (getOverrideGroupKey() != null) {
					out.writeInt(1);
					out.writeString(getOverrideGroupKey());
				} else out.writeInt(- 1);
			} else out.writeInt(0);
		}
	}

	boolean readMutableFieldsFromParcel(final Parcel parcel) {
		boolean mutated = false;
		int mark = parcel.readInt();
		if (mark != 0) {
			mutated = true;
			if (mark == -1) mTag = null;
			else mTag = parcel.readString();
		}
		final int id = parcel.readInt();
		if (mId != id) {
			mutated = true;
			mId = id;
		}
		if (SDK_INT >= N) {
			mark = parcel.readInt();
			if (mark != 0) {
				mutated = true;
				setOverrideGroupKey(mark == - 1 ? null : parcel.readString());
			}
		}
		if (mutated) updateKey();
		return mutated;
	}

	public void readFromParcel(final Parcel reply) { throw new UnsupportedOperationException(); }		// Not implemented in SDK

	public static final Parcelable.Creator<MutableStatusBarNotification> CREATOR = new Parcelable.Creator<MutableStatusBarNotification>() {
		@Override public MutableStatusBarNotification createFromParcel(final Parcel in) { return new MutableStatusBarNotification(in); }
		@Override public MutableStatusBarNotification[] newArray(final int size) { return new MutableStatusBarNotification[size]; }
	};

	// Mutable fields
	private String mTag;
	private int mId;
	private transient String mKey;
	private transient boolean mAllowIncWriteBack;

	private static final String EXTRA_ORIGINAL_TAG = "nevo.tag";
	private static final String EXTRA_ORIGINAL_ID = "nevo.id";
	@VisibleForTesting static final String EXTRA_ORIGINAL_OVERRIDE_GROUP = "nevo.group.override";

	private static final String TAG = "Nevo.MSBN";
}
