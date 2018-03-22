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

package com.oasisfeng.nevo.decorators;

import android.os.RemoteException;

import com.oasisfeng.android.os.IBundle;
import com.oasisfeng.nevo.INotification;
import com.oasisfeng.nevo.StatusBarNotificationEvo;
import com.oasisfeng.nevo.decorator.NevoDecoratorService;

import static android.support.v4.app.NotificationCompat.EXTRA_BIG_TEXT;
import static android.support.v4.app.NotificationCompat.EXTRA_TEXT;
import static android.support.v4.app.NotificationCompat.EXTRA_TITLE;
import static android.support.v4.app.NotificationCompat.EXTRA_TITLE_BIG;

/** @author Oasis */
public class BigTextDecorator extends NevoDecoratorService {

	private static final int MIN_TEXT_LENGTH = 20;

	@Override public void apply(final StatusBarNotificationEvo evolved) throws RemoteException {
		final INotification n = evolved.notification();
		if (n.hasCustomBigContentView()) return;
		final IBundle extras = n.extras();
		final CharSequence text = extras.getCharSequence(EXTRA_TEXT);
		if (text == null) return;
// TODO	mPaint.measureText(text);
		if (text.length() < MIN_TEXT_LENGTH) return;

		extras.putCharSequence(EXTRA_TITLE_BIG, extras.getCharSequence(EXTRA_TITLE));
		extras.putCharSequence(EXTRA_BIG_TEXT, text);
		extras.putString(EXTRA_REBUILD_STYLE, STYLE_BIG_TEXT);
	}

//	private final TextPaint mPaint = new TextPaint();
}
