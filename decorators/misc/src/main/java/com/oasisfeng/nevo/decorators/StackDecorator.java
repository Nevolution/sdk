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

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationCompat.InboxStyle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.widget.RemoteViews;

import com.oasisfeng.android.os.IBundle;
import com.oasisfeng.nevo.INotification;
import com.oasisfeng.nevo.StatusBarNotificationEvo;
import com.oasisfeng.nevo.decorator.NevoDecoratorService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static android.support.v4.app.NotificationCompat.EXTRA_TEXT_LINES;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;

/** @author Oasis */
public class StackDecorator extends NevoDecoratorService {

	private static final int KMaxNumLines = 10;
	private static final long KMinIntervalToShowTimestamp = 10 * 60_000;

	@Override public void apply(final StatusBarNotificationEvo evolved) throws RemoteException {
		final Collection<StatusBarNotificationEvo> history = getArchivedNotifications(evolved.getKey(), KMaxNumLines);
		if (history.size() <= 1) return;
		final INotification evolved_n = evolved.notification();
		final IBundle evolved_extras = evolved_n.extras();
		if (evolved_extras.containsKey(EXTRA_TEXT_LINES)) return;	// Never stack already inbox-styled notification.

		final Calendar calendar = Calendar.getInstance(); final List<CharSequence> lines = new ArrayList<>(KMaxNumLines);
		long previous_when = 0;
		final long latest_when = evolved_n.getWhen();
		for (final StatusBarNotificationEvo sbn : history) {
			final INotification n = sbn.notification();
			final CharSequence text = n.extras().getCharSequence(NotificationCompat.EXTRA_TEXT);
			if (text == null) continue;

			final long when = n.getWhen();
			if (when == latest_when || Math.abs(when - previous_when) <= KMinIntervalToShowTimestamp) lines.add(text);
			else {		// Add time-stamp
				final SpannableStringBuilder line = new SpannableStringBuilder();
				calendar.setTimeInMillis(when);
				final String time_text = String.format((Locale) null, "%1$02d:%2$02d ", calendar.get(HOUR_OF_DAY), calendar.get(MINUTE));
				line.append(time_text);
				line.append(text);
				line.setSpan(new StyleSpan(Typeface.BOLD), 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				lines.add(line);
			}
			previous_when = when;
		}
		if (lines.isEmpty()) return;
		Collections.reverse(lines);			// Latest first, since earliest lines will be trimmed by InboxStyle.

		final CharSequence title = evolved_extras.getCharSequence(NotificationCompat.EXTRA_TITLE);
		evolved_extras.putCharSequence(NotificationCompat.EXTRA_TITLE_BIG, title);
		evolved_extras.putCharSequenceArray(EXTRA_TEXT_LINES, lines);

		evolved_n.setBigContentView(buildBigContentView(evolved.getPackageName(), title, lines));
	}

	private RemoteViews buildBigContentView(final String pkg, final CharSequence title, final List<CharSequence> lines) {
//		final Bitmap large_icon = evolved_extras.getParcelable(NotificationCompat.EXTRA_LARGE_ICON);
		Context context; try { context = createPackageContext(pkg, 0); }	// This ensure the correct display of the small icon
		catch (final PackageManager.NameNotFoundException e) { context = this; }
		final Builder builder = new Builder(context)/*.setSmallIcon(evolved_n.icon).setLargeIcon(large_icon)*/;
		final InboxStyle inbox = new NotificationCompat.InboxStyle(builder).setBigContentTitle(title);
		for (final CharSequence line : lines) inbox.addLine(line);

		return inbox.build().bigContentView;
	}
}
