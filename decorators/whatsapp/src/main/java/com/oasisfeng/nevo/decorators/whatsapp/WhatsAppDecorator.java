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

package com.oasisfeng.nevo.decorators.whatsapp;

import android.app.Notification;
import android.os.Build;
import android.os.RemoteException;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

import com.oasisfeng.android.os.IBundle;
import com.oasisfeng.nevo.INotification;
import com.oasisfeng.nevo.StatusBarNotificationEvo;
import com.oasisfeng.nevo.decorator.NevoDecoratorService;

import java.util.ArrayList;
import java.util.List;

import static android.graphics.Typeface.BOLD;
import static android.support.v4.app.NotificationCompat.EXTRA_SUMMARY_TEXT;
import static android.support.v4.app.NotificationCompat.EXTRA_TEXT;
import static android.support.v4.app.NotificationCompat.EXTRA_TEXT_LINES;
import static android.support.v4.app.NotificationCompat.EXTRA_TITLE;
import static android.support.v4.app.NotificationCompat.EXTRA_TITLE_BIG;
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

/**
 * App-specific decorator for WhatsApp
 *
 * Created by Oasis on 2016/3/1.
 */
public class WhatsAppDecorator extends NevoDecoratorService {

	private static final int DEFAULT_COLOR = 0xFF075E54;

	@Override public void apply(final StatusBarNotificationEvo evolving) throws RemoteException {
		final INotification n = evolving.notification();
		final IBundle extras = n.extras();

		final CharSequence who, group, message;
		@SuppressWarnings("unchecked") final List<CharSequence> lines = extras.getCharSequenceArray(EXTRA_TEXT_LINES);
		final boolean has_lines = lines != null && ! lines.isEmpty();
		final CharSequence title = extras.getCharSequence(EXTRA_TITLE);
		final CharSequence last = has_lines ? lines.get(lines.size() - 1) : extras.getCharSequence(EXTRA_TEXT);

		final CharSequence[] last_parts = extract(title, last);
		who = last_parts[0]; group = last_parts[1]; message = last_parts[2];

		if (group != null) evolving.setTag(".Group");
		else if (who != null) evolving.setTag(".Direct");
		else return;	// Nothing to do for other messages.

		final CharSequence new_title = group != null ? group : who;
		evolving.setId(new_title.toString().hashCode());
		if (n.getColor() == 0) n.setColor(DEFAULT_COLOR);	// Fix the missing color in some notifications

		if (! has_lines) return;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
			n.removeFlags(Notification.FLAG_GROUP_SUMMARY);
		else extras.remove("android.support.isGroupSummary");

		extras.putCharSequence(EXTRA_TITLE, new_title);
		extras.putCharSequence(EXTRA_TITLE_BIG, new_title);
		extras.putCharSequence(EXTRA_TEXT, group != null ? who + ": " + message : message);

		final List<CharSequence> new_lines = new ArrayList<>(lines.size());
		for (final CharSequence line : lines) {
			final CharSequence[] parts = extract(title, line);
			if (group != null) {			// Group chat, keep messages within the same group.
				if (! group.equals(parts[1])) continue;
				final SpannableStringBuilder new_line = new SpannableStringBuilder();
				new_line.append(parts[0]).setSpan(new StyleSpan(BOLD), 0, new_line.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
				new_lines.add(new_line.append(": ").append(parts[2]));
			} else if (who.equals(parts[0]) && parts[1] == null)
				new_lines.add(parts[2]);	// Direct chat, keep messages from the same person (excluding group chat)
		}
		extras.putCharSequenceArray(EXTRA_TEXT_LINES, new_lines);
		extras.remove(EXTRA_SUMMARY_TEXT);
		extras.putString(EXTRA_REBUILD_STYLE, STYLE_INBOX);
		if (new_lines.size() > 1) n.setNumber(new_lines.size());
	}

	/**
	 * Patterns
	 *
	 * 		Title			Line
	 * 		-----			----
	 * 	1.	Sender			Message
	 * 	2.	Sender @ Group	Message
	 * 	3.	WhatsApp		Sender: Message
	 * 	4.	Group			Sender: Message
	 * 	5.	Summary			Sender @ Group: Message
	 *
	 * @return CharSequence[] { who, group, message }
	 */
	private static CharSequence[] extract(final CharSequence title, final CharSequence line) {
		final int pos_colon = line.toString().indexOf(':');
		if (pos_colon < 0) {	// Pattern 1 or 2
			final int pos_at = title.toString().indexOf('@');
			if (pos_at <= 0) return new CharSequence[] { title, null, line };	// Pattern 1
			final CharSequence who = trim(title.subSequence(0, pos_at));
			final CharSequence group = trim(title.subSequence(pos_at + 1, title.length()));
			return new CharSequence[] { who, group, line };						// Pattern 2
		}	// Pattern 3, 4 or 5
		final CharSequence message = trim(line.subSequence(pos_colon + 1, line.length()));
		final CharSequence from = line.subSequence(0, pos_colon), who, group;
		final int pos_at = from.toString().indexOf('@');
		if (pos_at <= 0) {
			group = "WhatsApp".equals(title) ? null : title;
			return new CharSequence[] { from, group, message };					// Pattern 3 or 4
		} else {
			who = trim(from.subSequence(0, pos_at));
			group = trim(from.subSequence(pos_at + 1, from.length()));
			return new CharSequence[] { who, group, message };					// Pattern 5
		}
	}

	private static CharSequence trim(final CharSequence cs) {
		final int last = cs.length() - 1; int start = 0; int end = last;
		while ((start <= end) && (cs.charAt(start) <= ' '))
			start++;
		while ((end >= start) && (cs.charAt(end) <= ' '))
			end--;
		if (start == 0 && end == last)
			return cs;
		return cs.subSequence(start, end + 1);
	}
}
