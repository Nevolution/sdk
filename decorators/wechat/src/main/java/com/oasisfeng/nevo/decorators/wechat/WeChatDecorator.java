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

package com.oasisfeng.nevo.decorators.wechat;

import android.os.RemoteException;
import android.support.annotation.ColorRes;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.oasisfeng.android.os.IBundle;
import com.oasisfeng.nevo.INotification;
import com.oasisfeng.nevo.StatusBarNotificationEvo;
import com.oasisfeng.nevo.decorator.NevoDecoratorService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Example of app-specific decorator
 *
 * Created by Oasis on 2015/6/1.
 */
public class WeChatDecorator extends NevoDecoratorService {

	private static final int KMaxNumLines = 10;
	private static final @ColorRes int PRIMARY_COLOR = 0xFF33B332;

	@Override public void apply(final StatusBarNotificationEvo evolving) throws RemoteException {
		final INotification n = evolving.notification();
		final IBundle extras = n.extras();

		// WeChat use dynamic counter as notification ID, which unfortunately will be reset upon evolving (removal, to be exact) by us,
		// causing all messages combined into one notification. So we split them by re-coding the notification ID by title.
		final CharSequence title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE);
		if (title == null || title.length() == 0) return;
		evolving.setId(calcSplitId(title));		// Split into separate slots

		// Chat history in big content view
		final List<StatusBarNotificationEvo> history = getArchivedNotifications(evolving.getOriginalKey(), 20);
		if (history.isEmpty()) return;

		final List<CharSequence> lines = new ArrayList<>(KMaxNumLines);
		CharSequence text = null; int count = 0; final String redundant_prefix = title.toString() + ": ";
		for (final StatusBarNotificationEvo each : history) {
			final IBundle its_extras = each.notification().extras();
			final CharSequence its_title = its_extras.getCharSequence(NotificationCompat.EXTRA_TITLE);
			if (! title.equals(its_title)) continue;	// Skip other conversations sharing the same key.
			final CharSequence its_text = its_extras.getCharSequence(NotificationCompat.EXTRA_TEXT);
			if (its_text == null) continue;
			final int result = trimAndExtractLeadingCounter(its_text);
			if (result >= 0) {
				count = result & 0xFFFF;
				CharSequence trimmed_text = its_text.subSequence(result >> 16, its_text.length());
				if (trimmed_text.toString().startsWith(redundant_prefix))	// Remove redundant prefix
					trimmed_text = trimmed_text.subSequence(redundant_prefix.length(), trimmed_text.length());
				lines.add(text = trimmed_text);
			} else {
				count = 1;
				lines.add(text = its_text);
			}
		}
		if (lines.isEmpty()) return;

		Collections.reverse(lines);			// Latest first, since bottom lines will be trimmed by InboxStyle.

		extras.putCharSequence(NotificationCompat.EXTRA_TEXT, text);
		if (count > 1) {
			n.setNumber(count);
			extras.putCharSequence(NotificationCompat.EXTRA_TITLE_BIG, title);
			extras.putCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES, lines.size() > count ? lines.subList(0, count) : lines);
			extras.putString(EXTRA_REBUILD_STYLE, STYLE_INBOX);
		}

		n.setColor(PRIMARY_COLOR);
	}

	/** @return the extracted count in 0xFF range and start position in 0xFF00 range */
	private int trimAndExtractLeadingCounter(final CharSequence text) {
		// Parse and remove the leading "[n]" or [n条/則/…]
		if (text == null || text.length() < 4 || text.charAt(0) != '[') return -1;
		int text_start = 3, count_end;
		while (text.charAt(text_start ++) != ']') if (text_start >= text.length()) return -1;

		try {
			final String num = text.subSequence(1, text_start - 1).toString();	// may contain the suffix "条/則"
			for (count_end = 0; count_end < num.length(); count_end ++) if (! Character.isDigit(num.charAt(count_end))) break;
			if (count_end == 0) return -1;		// Not the expected "unread count"
			final int count = Integer.parseInt(num.substring(0, count_end));
			if (count < 2) return -1;

			return count < 0xFFFF ? (count & 0xFFFF) | ((text_start << 16) & 0xFFFF0000) : 0xFFFF | ((text_start << 16) & 0xFF00);
		} catch (final NumberFormatException ignored) {
			Log.d(TAG, "Failed to parse: " + text);
			return -1;
		}
	}

	private static int calcSplitId(final CharSequence title) {
		return title == null ? 0 : title.hashCode();
	}
}
