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

package com.oasisfeng.nevo.decorators.callvibrator;

import android.app.Notification;
import android.os.RemoteException;
import android.os.Vibrator;

import com.oasisfeng.android.os.IBundle;
import com.oasisfeng.nevo.StatusBarNotificationEvo;
import com.oasisfeng.nevo.decorator.NevoDecoratorService;

/**
 * App-specific decorator - Vibrator when call is answered
 *
 * Created by Oasis on 2016/2/9.
 */
public class CallVibratorDecorator extends NevoDecoratorService {

	private static final long VIBRATOR_DURATION = 500;
	private static final long MAX_DELAY_TO_VIBRATE = 2000;

	@Override public void apply(final StatusBarNotificationEvo evolving) throws RemoteException {
		if (! evolving.isOngoing()) return;
		if (! evolving.getKey().equals(mOngoingKey)) {
			mOngoingKey = evolving.getKey();
			return;
		}
		// The on-going notification is being updated in place, now check the chronometer.
		final IBundle extras = evolving.notification().extras();
		if (! extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)) return;
		// Ensure the chronometer starting time is just now.
		if (evolving.notification().getWhen() < System.currentTimeMillis() - MAX_DELAY_TO_VIBRATE) return;

		final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		vibrator.vibrate(VIBRATOR_DURATION);
	}

	@Override protected void onNotificationRemoved(final String key) throws Exception {
		if (key.equals(mOngoingKey)) mOngoingKey = null;
	}

	private String mOngoingKey;
}
