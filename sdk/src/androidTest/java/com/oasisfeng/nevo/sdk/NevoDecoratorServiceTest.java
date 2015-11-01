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
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.test.ServiceTestCase;

import com.oasisfeng.nevo.StatusBarNotificationEvo;
import com.oasisfeng.nevo.decorator.INevoDecorator;
import com.oasisfeng.nevo.decorator.NevoDecoratorService;

public class NevoDecoratorServiceTest extends ServiceTestCase<NevoDecoratorServiceTest.TestNevoDecoratorService> {

	public void testExceptionInApply() {
		final IBinder binder = bindService(new Intent()/* Not used */);
		assertNotNull(binder);
		final INevoDecorator decorator = INevoDecorator.Stub.asInterface(binder);
		final StatusBarNotificationEvo sbne = new StatusBarNotificationEvo("", null, 0, null, 0, 0, 0, new Notification(), android.os.Process.myUserHandle(), 0);
		try {
			decorator.apply(sbne, null);
			fail("No exception thrown");
		} catch (final RemoteException e) {
			fail("Unexpected exception: " + e);
		} catch (final NullPointerException ignored) {}
	}

	public NevoDecoratorServiceTest() {
		super(TestNevoDecoratorService.class);
	}

	public static class TestNevoDecoratorService extends NevoDecoratorService {

		@Override protected void apply(final StatusBarNotificationEvo evolved) {
			throw new NullPointerException();
		}
	}
}
