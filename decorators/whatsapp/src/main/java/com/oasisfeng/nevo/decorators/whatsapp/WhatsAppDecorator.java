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

import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;

import com.oasisfeng.android.os.IBundle;
import com.oasisfeng.nevo.INotification;
import com.oasisfeng.nevo.StatusBarNotificationEvo;
import com.oasisfeng.nevo.decorator.NevoDecoratorService;

import java.util.ArrayList;
import java.util.List;

import static android.support.v4.app.NotificationCompat.EXTRA_SUMMARY_TEXT;
import static android.support.v4.app.NotificationCompat.EXTRA_TEXT_LINES;

/**
 * App-specific decorator for WhatsApp
 *
 * Created by Oasis on 2016/3/1.
 */
public class WhatsAppDecorator extends NevoDecoratorService {

	@Override public void apply(final StatusBarNotificationEvo evolving) throws RemoteException {
		final INotification n = evolving.notification();
		final IBundle extras = n.extras();

		@SuppressWarnings("unchecked") final List<CharSequence> lines = extras.getCharSequenceArray(EXTRA_TEXT_LINES);
		if (lines == null || lines.size() <= 1) return;

		final CharSequence last = lines.get(lines.size() - 1);
		final CharSequence[] _p = extract(last);
		final CharSequence who = _p[0], group = _p[1], message = _p[2];

		if (group != null) {
			extras.putCharSequence(NotificationCompat.EXTRA_TITLE, group);
			extras.putCharSequence(NotificationCompat.EXTRA_TITLE_BIG, group);
			extras.putCharSequence(NotificationCompat.EXTRA_TEXT, who + ": " + message);
			evolving.setId(group.toString().hashCode() + 1/* Distinct from direct chat with the same name */);
		} else if (who != null) {
			extras.putCharSequence(NotificationCompat.EXTRA_TITLE, who);
			extras.putCharSequence(NotificationCompat.EXTRA_TITLE_BIG, who);
			extras.putCharSequence(NotificationCompat.EXTRA_TEXT, message);
			evolving.setId(who.toString().hashCode());
		}	// No need to alter title and ID for other messages.

		final List<CharSequence> new_lines = new ArrayList<>(lines.size());
		for (final CharSequence line : lines) {
			final CharSequence[] parts = extract(line);
			if (group != null) {			// Group chat, keep messages within the same group.
				if (group.equals(parts[1])) new_lines.add(parts[0] + ": " + parts[2]);
			} else if (who != null) {		// Direct chat, keep messages from the same person (excluding group chat)
				if (who.equals(parts[0]) && parts[1] == null) new_lines.add(parts[2]);
			} else new_lines.add(line);		// Other messages
		}
		extras.putCharSequenceArray(EXTRA_TEXT_LINES, new_lines);
		extras.remove(EXTRA_SUMMARY_TEXT);
		extras.putString(EXTRA_REBUILD_STYLE, STYLE_INBOX);

		if (new_lines.size() > 1) n.setNumber(new_lines.size());
	}

	private CharSequence[] extract(final CharSequence line) {
		final int pos_colon = line.toString().indexOf(':');
		if (pos_colon < 0) return new CharSequence[] { null, null, line };	// Non-chat message
		final int pos_message = pos_colon + 1;
		final CharSequence message = trim(line.subSequence(pos_message, line.length()));
		final CharSequence from = line.subSequence(0, pos_colon), who, group;
		final int pos_at = from.toString().indexOf('@');
		if (pos_at > 0) {	// Group chat: "name @ group"
			who = trim(from.subSequence(0, pos_at));
			group = trim(from.subSequence(pos_at + 1, from.length()));
		} else { group = null; who = from; }
		return new CharSequence[] { who, group, message };
	}

	private CharSequence trim(final CharSequence cs) {
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
