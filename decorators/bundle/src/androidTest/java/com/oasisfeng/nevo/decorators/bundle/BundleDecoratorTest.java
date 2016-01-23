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

package com.oasisfeng.nevo.decorators.bundle;

import android.app.Notification;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.service.notification.StatusBarNotification;
import android.test.ServiceTestCase;

import com.oasisfeng.android.content.pm.ParceledListSlice;
import com.oasisfeng.nevo.StatusBarNotificationEvo;
import com.oasisfeng.nevo.decorator.INevoDecorator;
import com.oasisfeng.nevo.engine.INevoController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Test cases for {@link BundleDecorator}
 *
 * Created by Oasis on 2015/7/31.
 */
public class BundleDecoratorTest extends ServiceTestCase<BundleDecorator> {

	private static final String DUMMY_PKG = "com.oasisfeng.nevo.test";
	private static final String DUMMY_BUNDLE = "test";

	public void testPackageOnlyRule() throws RemoteException {
		final INotificationBundle.Stub bundle_service = new DummyNotificationBundle();
		// Inject test stub of INotificationBundle.
		setContext(new ContextWrapper(getContext()) {

			@Override public boolean bindService(final Intent service, final ServiceConnection conn, final int flags) {
				if (INotificationBundle.class.getName().equals(service.getAction())) {
					assertNull(mConnection);
					mConnection = conn;
					new Handler(Looper.getMainLooper()).post(new Runnable() { @Override public void run() {
						conn.onServiceConnected(null, bundle_service);
					}});
					return true;
				}
				return super.bindService(service, conn, flags);
			}

			@Override public void unbindService(final ServiceConnection conn) {
				if (mConnection != null && conn == mConnection) {
					mConnection = null;
					return;
				}
				super.unbindService(conn);
			}

			private ServiceConnection mConnection;
		});

		final DummyNevoController controller = new DummyNevoController();
		createBundleDecorator(controller);
		final BundleDecorator decorator = getService();
		bundle_service.setRule(DUMMY_PKG, null, DUMMY_BUNDLE);

		// Add 3 notifications to bundle
		final StatusBarNotificationEvo n1, n2, n3;
		decorator.apply(controller.put(n1 = sbn("T1", 100, "1")));
		assertEquals(Collections.singletonList(n1.getKey()), bundle_service.getBundledNotificationKeys(DUMMY_BUNDLE));
		decorator.apply(controller.put(n2 = sbn("T2", 100, "2")));
		assertEquals(Arrays.asList(n2.getKey(), n1.getKey()), bundle_service.getBundledNotificationKeys(DUMMY_BUNDLE));
		decorator.apply(controller.put(n3 = sbn("T3", 100, "3")));
		assertEquals(Arrays.asList(n3.getKey(), n2.getKey(), n1.getKey()), bundle_service.getBundledNotificationKeys(DUMMY_BUNDLE));
		// Simulate the removal of bundle notification
		getContext().sendBroadcast(new Intent(BundleDecorator.ACTION_BUNDLE_CLEAR));
	}

	private void createBundleDecorator(final INevoController controller) throws RemoteException {
//		final BundleDecorator decorator = new BundleDecorator();
//		decorator.onCreate();
//		final INevoDecorator wrapper = (INevoDecorator) decorator.onBind(null);
		final INevoDecorator wrapper = (INevoDecorator) bindService(new Intent(INevoDecorator.class.getName()));
		assertNotNull(wrapper);
		wrapper.onConnected(controller, null);
	}

	private static StatusBarNotificationEvo sbn(final String tag, final int id, final String ticker) {
		final Notification n = new Notification();
		n.tickerText = ticker;
		return new StatusBarNotificationEvo(DUMMY_PKG, DUMMY_PKG, id, tag, 0, 0, 0, n, android.os.Process.myUserHandle(), 0);
	}

	public BundleDecoratorTest() { super(BundleDecorator.class); }

	private static class DummyNotificationBundle extends INotificationBundle.Stub {

		@Override public String queryRuleForNotification(final StatusBarNotificationEvo sbn) {
			return queryRule(sbn.getKey(), null);
		}

		@Override public void setRule(final String pkg, final String title, final String bundle) {
			mRules.put(title == null ? pkg : pkg + ":" + title, bundle);
		}

		@Override public Map<String, String> getAllRules() {
			return Collections.unmodifiableMap(mRules);
		}

		@Override public String queryRule(final String pkg, final String title) {
			return mRules.get(pkg);
		}

		@Override public void setNotificationBundle(final String key, final String bundle) {
			mBundles.put(key, bundle);
		}

		@Override public List<String> getBundledNotificationKeys(final String bundle) {
			final List<String> keys = new ArrayList<>();
			for (final Map.Entry<String, String> entry : mBundles.entrySet())
				if (bundle.equals(entry.getValue())) keys.add(entry.getKey());
			return keys;
		}

		@Override public List<String> getDefinedBundles() {
			return new ArrayList<>(new HashSet<>(mRules.values()));
		}

		private final Map<String/* key */, String/* bundle */> mBundles = new HashMap<>();
		private final Map<String/* pkg */, String/* bundle */> mRules = new HashMap<>();
	}

	private static class DummyNevoController extends INevoController.Stub {

		StatusBarNotificationEvo put(final StatusBarNotificationEvo sbn) {
			mNotifications.put(sbn.getKey(), sbn);
			return sbn;
		}

		@Override public ParceledListSlice<StatusBarNotificationEvo> getActiveNotifications(final INevoDecorator token) {
			return null;
		}

		@Override public ParceledListSlice<StatusBarNotificationEvo> getArchivedNotifications(final INevoDecorator token, final String key, final int limit) throws RemoteException {
			return null;
		}

		@Override public ParceledListSlice<StatusBarNotificationEvo> getNotifications(final INevoDecorator token, final List<String> keys) throws RemoteException {
			final List<StatusBarNotificationEvo> notifications = new ArrayList<>();
			for (final String key : keys) {
				final StatusBarNotification notification = mNotifications.get(key);
				if (notification != null) notifications.add(StatusBarNotificationEvo.from(notification));
			}
			return new ParceledListSlice<>(notifications);
		}

		@Override public void cancelNotification(final INevoDecorator token, final String key) throws RemoteException {}
		@Override public void reviveNotification(final INevoDecorator token, final String key) throws RemoteException {}

		private final Map<String, StatusBarNotification> mNotifications = new HashMap<>();
	}
}
