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

import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Keep track of updated entries in local
 *
 * <p><b>Derivation note</b>: Make sure to override {@link #getBundle(String)} and return the derived instance.
 * Use {@link OnDemandBundleHolder} as a delegate to implement the derived one.
 * See also {@link com.oasisfeng.nevo.ChangeTrackingBundleHolder}.
 *
 * <p>Created by Oasis on 2015/6/4.
 */
public class BundleHolder extends IBundle.Stub {

	public static final Object REMOVAL = new Object();

	protected final Bundle local;

	public BundleHolder(final Bundle bundle) {
		if (bundle == null) throw new NullPointerException("bundle is null");
		local = bundle;
	}

	/**
	 * Called after the change is made. (no matter whether the value is the same)
	 *
	 * @param value the new value or {@link #REMOVAL} to indicate the key is removed
	 */
	protected void onChanged(final String key, final Object value) {}

	public boolean isEmpty() { return local.isEmpty(); }

	@Override public void clear() {
		final HashSet<String> keys;
		synchronized (local) {
			local.clear();
			keys = new HashSet<>(local.keySet());
		}
		for (final String key : keys) onChanged(key, REMOVAL);
	}

	@Override public void remove(final String key) {
		synchronized (local) { local.remove(key); }
		onChanged(key, REMOVAL);
	}

	@Override public void putBoolean(final String key, final boolean value) {
		synchronized (local) { local.putBoolean(key, value); }
		onChanged(key, value);
	}
	@Override public void putInt(final String key, final int value) {
		synchronized (local) { local.putInt(key, value); }
		onChanged(key, value);
	}
	@Override public void putLong(final String key, final long value) {
		synchronized (local) { local.putLong(key, value); }
		onChanged(key, value);
	}
	@Override public void putDouble(final String key, final double value) {
		synchronized (local) { local.putDouble(key, value); }
		onChanged(key, value);
	}
	@Override public void putBooleanArray(final String key, final boolean[] value) {
		synchronized (local) { local.putBooleanArray(key, value); }
		onChanged(key, value);
	}
	@Override public void putIntArray(final String key, final int[] value) {
		synchronized (local) { local.putIntArray(key, value); }
		onChanged(key, value);
	}
	@Override public void putLongArray(final String key, final long[] value) {
		synchronized (local) { local.putLongArray(key, value); }
		onChanged(key, value);
	}
	@Override public void putDoubleArray(final String key, final double[] value) {
		synchronized (local) { local.putDoubleArray(key, value); }
		onChanged(key, value);
	}
	@Override public void putParcelable(final String key, final ParcelableReference value) {
		putParcelable(key, value.get());
	}
	protected void putParcelable(final String key, final Parcelable value) {
		synchronized (local) { local.putParcelable(key, value); }
		onChanged(key, value);
	}

	@Override public void putString(final String key, final String value) {
		synchronized (local) { local.putString(key, value); }
		onChanged(key, value);
	}
	@Override public void putStringArray(final String key, final String[] value) {
		synchronized (local) { local.putStringArray(key, value); }
		onChanged(key, value);
	}
	@Override public void putStringArrayList(final String key, final List<String> value) {
		@SuppressWarnings("unchecked") final ArrayList<String> list = (ArrayList<String>) value;
		synchronized (local) { local.putStringArrayList(key, list); }
		onChanged(key, value);
	}

	@Override public void putCharSequence(final String key, final CharSequence value) {
		synchronized (local) { local.putCharSequence(key, value); }
		onChanged(key, value);
	}
	@Override public void putCharSequenceArray(final String key, final List value) {
		@SuppressWarnings("unchecked") final List<CharSequence> list = (List<CharSequence>) value;
		synchronized (local) { local.putCharSequenceArray(key, list.toArray(new CharSequence[list.size()])); }
		onChanged(key, value);
	}
	@Override public void putCharSequenceArrayList(final String key, final List value) {
		@SuppressWarnings("unchecked") final ArrayList<CharSequence> list = (ArrayList<CharSequence>) value;
		synchronized (local) { local.putCharSequenceArrayList(key, list); }
		onChanged(key, value);
	}

	@Override public void putParcelableArray(final String key, final List value) {
		@SuppressWarnings("unchecked") final List<Parcelable> list = (List<Parcelable>) value;
		synchronized (local) { local.putParcelableArray(key, list.toArray(new Parcelable[list.size()])); }
		onChanged(key, list);
	}
	@Override public void putParcelableArrayList(final String key, final List value) {
		@SuppressWarnings("unchecked") final ArrayList<Parcelable> list = (ArrayList<Parcelable>) value;
		synchronized (local) { local.putParcelableArrayList(key, list); }
		onChanged(key, list);
	}
	@Override public void putSparseParcelableArray(final String key, final Map value) {
		@SuppressWarnings("unchecked") final Map<Integer, Parcelable> map = (Map<Integer, Parcelable>) value;
		final SparseArray<Parcelable> array = new SparseArray<>(map.size());
		for (final Map.Entry<Integer, Parcelable> entry : map.entrySet())
			array.put(entry.getKey(), entry.getValue());
		synchronized (local) { local.putSparseParcelableArray(key, array); }
		onChanged(key, map);
	}

	public Object get(final String key) { synchronized (local) { return local.get(key); }}
	@Override public boolean getBoolean(final String key, final boolean defaultValue) { synchronized (local) { return local.getBoolean(key, defaultValue); }}
	@Override public int getInt(final String key, final int defaultValue) { synchronized (local) { return local.getInt(key, defaultValue); }}
	@Override public long getLong(final String key, final long defaultValue) { synchronized (local) { return local.getLong(key, defaultValue); }}
	@Override public double getDouble(final String key, final double defaultValue) { synchronized (local) { return local.getDouble(key, defaultValue); }}
	@Override public boolean[] getBooleanArray(final String key) { synchronized (local) { return local.getBooleanArray(key); }}
	@Override public int[] getIntArray(final String key) { synchronized (local) { return local.getIntArray(key); }}
	@Override public long[] getLongArray(final String key) { synchronized (local) { return local.getLongArray(key); }}
	@Override public double[] getDoubleArray(final String key) { synchronized (local) { return local.getDoubleArray(key); }}
	@Override public ParcelableReference getParcelable(final String key) { synchronized (local) { return new ParcelableReference(getParcelableDirectly(key)); }}
	protected <T extends Parcelable> T getParcelableDirectly(final String key) { synchronized (local) { return local.getParcelable(key); }}

	@Override public String getString(final String key) { synchronized (local) { return local.getString(key); }}
	@Override public String[] getStringArray(final String key) { synchronized (local) { return local.getStringArray(key); }}
	@Override public List<String> getStringArrayList(final String key) { synchronized (local) { return local.getStringArrayList(key); }}

	@Override public CharSequence getCharSequence(final String key) { synchronized (local) { return local.getCharSequence(key); }}
	@Override public List<CharSequence> getCharSequenceArray(final String key) {
		final CharSequence[] array; synchronized (local) { array = local.getCharSequenceArray(key); }
		return array != null ? Arrays.asList(array) : null;
	}
	@Override public List<CharSequence> getCharSequenceArrayList(final String key) { synchronized (local) { return local.getCharSequenceArrayList(key); }}

	@Override public List<Parcelable> getParcelableArray(final String key) {
		final Parcelable[] array; synchronized (local) { array = local.getParcelableArray(key); }
		return array != null ? Arrays.asList(array) : null;
	}
	@Override public List<Parcelable> getParcelableArrayList(final String key) { synchronized (local) { return local.getParcelableArrayList(key); }}
	@Override public Map<Integer, Parcelable> getSparseParcelableArray(final String key) {
		final SparseArray<Parcelable> array; synchronized (local) { array = local.getSparseParcelableArray(key); }
		if (array == null) return null;
		final HashMap<Integer, Parcelable> map = new HashMap<>();
		for (int i = 0; i < array.size(); i ++) map.put(array.keyAt(i), array.valueAt(i));
		return map;
	}

	@Override public boolean containsKey(final String key) { synchronized (local) { return local.containsKey(key); }}

	@Override public IBundle getBundle(final String key) {
		final Bundle bundle; synchronized (local) { bundle = local.getBundle(key); }
		return bundle != null ? new BundleHolder(bundle) : new OnDemandBundleHolder(local, key);
	}

	protected static class OnDemandBundleHolder extends BundleHolder {

		private final Bundle mParent;
		private final String mKey;
		private boolean mAttached;

		public OnDemandBundleHolder(final Bundle parent, final String key) {
			super(new Bundle(parent.getClassLoader()));
			mParent = parent; mKey = key;
		}

		@Override public void onChanged(final String key, final Object value) {
			if (mAttached) return;		// Just need to put back only once.
			synchronized (mParent) {
				if (mAttached) return;
				mParent.putBundle(mKey, local);
				mAttached = true;
			}
		}
	}
}
