package com.oasisfeng.nevo.decorators;

import android.content.res.Resources;
import android.support.annotation.IdRes;
import android.util.Log;
import android.widget.RemoteViews;

import com.oasisfeng.nevo.INotification;
import com.oasisfeng.nevo.StatusBarNotificationEvo;
import com.oasisfeng.nevo.decorator.NevoDecoratorService;

import java.util.List;

import static android.support.v4.app.NotificationCompat.EXTRA_TEXT_LINES;

/**
 * Stretch the single-lined text in the Inbox style into multi-lines when there are only a few lines,
 * to make the most of the available space.
 *
 * Created by Oasis on 2016/3/8.
 */
public class ElasticInboxDecorator extends NevoDecoratorService {

	private static final int MAX_INBOX_ENTRIES = 7;
	private static final int MAX_TOTAL_LINES = 10;

	@Override protected void apply(final StatusBarNotificationEvo evolving) throws Exception {
		final INotification n = evolving.notification();
		if (! n.hasBigContentView()) return;
		@SuppressWarnings("unchecked") final List<CharSequence> lines = n.extras().getCharSequenceArray(EXTRA_TEXT_LINES);
		if (lines == null || lines.isEmpty()) return;
		final int num_lines_left = MAX_TOTAL_LINES - lines.size();
		if (num_lines_left <= 0) return;
		final RemoteViews inbox = n.getBigContentView();
		if (inbox == null) return;

		final int num_entries = Math.min(lines.size(), MAX_INBOX_ENTRIES);
		final int[] line_length = new int[num_entries];
		int length_sum = 0;
		for (int i = 0; i < num_entries; i ++) length_sum += line_length[i] = lines.get(i).length();
		final StringBuilder log_buffer = new StringBuilder(32).append("Assign: ");
		boolean updated = false;
		for (int i = 0; i < num_entries; i ++) {
			final int view_id = sInboxLinesViewId[i];
			if (view_id == 0) continue;
			// Weight of extra lines for each item is its text length proportion (not accurate enough)
			final int max_lines = 1 + (num_lines_left * line_length[i] + length_sum / 2) / length_sum;
			if (max_lines > 1) {
				updated = true;
				setMaxLines(inbox, view_id, max_lines);
			}
			log_buffer.append(max_lines).append('/');
		}
		if (! updated) return;
		n.setBigContentView(inbox);
		Log.d(TAG, log_buffer.substring(0, log_buffer.length() - 1));
	}

	private void setMaxLines(final RemoteViews rvs, final @IdRes int res, final int max_lines) {
		rvs.setBoolean(res, "setSingleLine", false);
		rvs.setInt(res, "setMaxLines", max_lines);
	}

	private static final int[] sInboxLinesViewId = new int[MAX_INBOX_ENTRIES];
	static {
		final Resources res = Resources.getSystem();
		for (int i = 0; i < MAX_INBOX_ENTRIES; i ++)
			sInboxLinesViewId[i] = res.getIdentifier("inbox_text" + i, "id", "android");
	}
}
