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

import android.os.Bundle;

import com.oasisfeng.android.os.BundleHolder;
import com.oasisfeng.android.os.IBundle;

import java.util.HashSet;
import java.util.Set;

/**
 * Bundle holder with change tracking capability.
 *
 * Created by Oasis on 2015/9/10.
 */
public class ChangeTrackingBundleHolder extends BundleHolder {

	public boolean isChanged() {
		return ! mChangedKeys.isEmpty();	// No need to synchronize
	}

	public Set<String> getChangedKeys() {
		synchronized (mChangedKeys) {
			@SuppressWarnings("unchecked") final Set<String> clone = (Set<String>) mChangedKeys.clone();
			return clone;
		}
	}

	protected void onChanged(final String key, final Object value) {
		synchronized (mChangedKeys) {
			mChangedKeys.add(key);
		}
	}

	@Override public IBundle getBundle(final String key) {
		final Bundle bundle, parent;
		synchronized (local) {
			bundle = local.getBundle(key);
			parent = local;
		}
		return bundle != null ? new ChangeTrackingBundleHolder(bundle) : new OnDemandChangeTrackingBundleHolder(parent, key);
	}

	public ChangeTrackingBundleHolder(final Bundle bundle) { super(bundle); }

	private final HashSet<String> mChangedKeys = new HashSet<>();

	protected static class OnDemandChangeTrackingBundleHolder extends ChangeTrackingBundleHolder {

		public OnDemandChangeTrackingBundleHolder(final Bundle parent, final String key) {
			super(new Bundle(parent.getClassLoader()));
			mOnDemandDelegate = new OnDemandBundleHolder(parent, key);
		}

		@Override protected void onChanged(final String key, final Object value) {
			super.onChanged(key, value);
			mOnDemandDelegate.onChanged(key, value);
		}

		private final OnDemandBundleHolder mOnDemandDelegate;
	}
}
