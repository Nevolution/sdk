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

package com.oasisfeng.android.os;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * To represent a {@link android.os.Parcelable} in {@link IBundle}.
 *
 * Created by Oasis on 2015/8/31.
 */
public class ParcelableReference implements Parcelable {

	public ParcelableReference(final Parcelable value) {
		this.value = value;
	}

	public <T extends Parcelable> T get() {
		try {	//noinspection unchecked
			return (T) value;
		} catch (final ClassCastException e) {
			Log.w(TAG, e.toString());
			return null;
		}
	}

	@Override public int describeContents() { return 0; }
	@Override public void writeToParcel(final Parcel dest, final int flags) { dest.writeParcelable(value, flags); }
	ParcelableReference(final Parcel in, final ClassLoader loader) {
		value = in.readParcelable(loader == null ? getClass().getClassLoader() : loader);
	}
	public static final Parcelable.ClassLoaderCreator<ParcelableReference> CREATOR = new Parcelable.ClassLoaderCreator<ParcelableReference>() {
		@Override public ParcelableReference createFromParcel(final Parcel source) { return new ParcelableReference(source, null); }
		@Override public ParcelableReference createFromParcel(final Parcel source, final ClassLoader loader) { return new ParcelableReference(source, loader); }
		public ParcelableReference[] newArray(final int size) { return new ParcelableReference[size]; }
	};

	private final Parcelable value;

	private static final String TAG = "ParcelRef";
}
