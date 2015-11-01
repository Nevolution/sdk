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

import com.oasisfeng.android.os.ParcelableReference;

interface IBundle {

	oneway void clear();
	oneway void remove(String key);

	oneway void putBoolean(String key, boolean value);
	oneway void putInt(String key, int value);
	oneway void putLong(String key, long value);
	oneway void putDouble(String key, double value);
	oneway void putBooleanArray(String key, in boolean[] value);
	oneway void putIntArray(String key, in int[] value);
	oneway void putLongArray(String key, in long[] value);
	oneway void putDoubleArray(String key, in double[] value);
	oneway void putParcelable(String key, in ParcelableReference value);

	oneway void putString(String key, String value);
	oneway void putStringArray(String key, in String[] value);
	oneway void putStringArrayList(String key, in List<String> value);

	oneway void putCharSequence(String key, in CharSequence value);
	/** Use List<CharSequence> instead of CharSequence[] since the latter is not supported by AIDL */
	oneway void putCharSequenceArray(String key, in List/*<CharSequence>*/ value);
	/** Pass value argument in ArrayList<CharSequence> even if declared as List<CharSequence>, since ArrayList is not supported by AIDL */
	oneway void putCharSequenceArrayList(String key, in List/*<CharSequence>*/ value);

	boolean getBoolean(String key, boolean defaultValue);
	int getInt(String key, int defaultValue);
	long getLong(String key, long defaultValue);
	double getDouble(String key, double defaultValue);
	boolean[] getBooleanArray(String key);
	int[] getIntArray(String key);
	long[] getLongArray(String key);
	double[] getDoubleArray(String key);
	ParcelableReference getParcelable(String key);

	String getString(String key);
	String[] getStringArray(String key);
	List<String> getStringArrayList(String key);

	CharSequence getCharSequence(String key);
	/** Returns ArrayList<CharSequence> instead of CharSequence[] since the latter is not supported by AIDL */
	List/*<CharSequence>*/ getCharSequenceArray(String key);
	/** Returns ArrayList<CharSequence> despite declared as List, since ArrayList and List<CharSequence> are both not supported by AIDL */
	List/*<CharSequence>*/ getCharSequenceArrayList(String key);

	boolean containsKey(String key);
	/**
	 * Unlike {@link android.os.Bundle#getBundle(String)}, this method never returns null.
	 * There is no equivalent API to {@link android.os.Bundle#putBundle(String, Bundle)}, use the instance returned here instead.
	 */
	IBundle getBundle(String key);
}
