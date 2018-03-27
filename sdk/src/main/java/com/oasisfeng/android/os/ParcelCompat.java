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

import android.os.BadParcelableException;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.HashMap;

import static android.support.annotation.RestrictTo.Scope.LIBRARY;

/**
 * Compatibility helper for {@link Parcel}
 *
 * Created by Oasis on 2015/5/25.
 */
@RestrictTo(LIBRARY) public class ParcelCompat {

	private static final String TAG = "ParcelCompat";

	public static void writeParcelableCreator(Parcel parcel, Parcelable p) {
		String name = p.getClass().getName();
		parcel.writeString(name);
	}

	public static <T extends Parcelable> T readCreator(Parcel parcel, Parcelable.Creator<T> creator, ClassLoader loader) {
		if (creator instanceof Parcelable.ClassLoaderCreator<?>) {
			return ((Parcelable.ClassLoaderCreator<T>)creator).createFromParcel(parcel, loader);
		}
		return creator.createFromParcel(parcel);
	}

	public static <T extends Parcelable> Parcelable.Creator<T> readParcelableCreator(Parcel parcel, ClassLoader loader) {
		String name = parcel.readString();
		//noinspection ConstantConditions
		if (name == null) {
			return null;
		}
		Parcelable.Creator<T> creator;
		synchronized (mCreators) {
			HashMap<String,Parcelable.Creator> map = mCreators.get(loader);
			if (map == null) {
				map = new HashMap<>();
				mCreators.put(loader, map);
			}
			//noinspection unchecked
			creator = map.get(name);
			if (creator == null) {
				try {
					Class c = loader == null ?
							Class.forName(name) : Class.forName(name, true, loader);
					Field f = c.getField("CREATOR");
					//noinspection unchecked
					creator = (Parcelable.Creator) f.get(null);
				}
				catch (IllegalAccessException e) {
					Log.e(TAG, "Illegal access when unmarshalling: "
							+ name, e);
					throw new BadParcelableException(
							"IllegalAccessException when unmarshalling: " + name);
				}
				catch (ClassNotFoundException e) {
					Log.e(TAG, "Class not found when unmarshalling: "
							+ name, e);
					throw new BadParcelableException(
							"ClassNotFoundException when unmarshalling: " + name);
				}
				catch (ClassCastException | NoSuchFieldException e) {
					throw new BadParcelableException("Parcelable protocol requires a "
							+ "Parcelable.Creator object called "
							+ " CREATOR on class " + name);
				} catch (NullPointerException e) {
					throw new BadParcelableException("Parcelable protocol requires "
							+ "the CREATOR object to be static on class " + name);
				}
				if (creator == null) {
					throw new BadParcelableException("Parcelable protocol requires a "
							+ "Parcelable.Creator object called "
							+ " CREATOR on class " + name);
				}

				map.put(name, creator);
			}
		}

		return creator;
	}

	// Cache of previously looked up CREATOR.createFromParcel() methods for particular classes.
	// Keys are the names of the classes, values are Method objects.
	private static final HashMap<ClassLoader,HashMap<String,Parcelable.Creator>> mCreators = new HashMap<>();
}
