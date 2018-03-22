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

import android.app.Notification.Action;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.MessagingStyle;
import android.support.v4.util.LongSparseArray;
import android.util.Log;

import com.oasisfeng.android.os.IBundle;
import com.oasisfeng.nevo.INotification;
import com.oasisfeng.nevo.StatusBarNotificationEvo;
import com.oasisfeng.nevo.decorator.NevoDecoratorService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.KITKAT_WATCH;
import static android.os.Build.VERSION_CODES.N;
import static android.support.v4.app.NotificationCompat.EXTRA_REMOTE_INPUT_HISTORY;

/**
 * Bring state-of-art notification experience to WeChat.
 *
 * Created by Oasis on 2015/6/1.
 */
public class WeChatDecorator extends NevoDecoratorService {

	private static final int KMaxNumLines = 10;
	private static final String SENDER_PLACEHOLDER = " ";	// Cannot be empty (both empty and null will be replaced with user display name)

	private static final int NID_CONVERSATION_START = 4096;
	private static final String EXTRA_CAR_EXTENDER = "android.car.EXTENSIONS";
	private static final String EXTRA_CONVERSATION = "car_conversation";
	/* Following keys are originally defined in Notification.CarExtender.UnreadConversation */
	private static final String KEY_AUTHOR = "author";
	private static final String KEY_TEXT = "text";
	private static final String KEY_MESSAGES = "messages";
	private static final String KEY_REMOTE_INPUT = "remote_input";
	private static final String KEY_ON_REPLY = "on_reply";
	private static final String KEY_ON_READ = "on_read";
	private static final String KEY_PARTICIPANTS = "participants";
	private static final String KEY_TIMESTAMP = "timestamp";

	//private static final String KEY_TIMESTAMP = "timestamp";
	private static final String KEY_WECHAT_CONVERSATION_ID = "key_username";	// The internal conversation ID in WeChat.

	private static final String ACTION_REPLY = "REPLY";
	private static final String SCHEME_KEY = "key";
	private static final String EXTRA_PENDING_INTENT = "pending_intent";
	private static final String EXTRA_RESULT_KEY = "result_key";

	private static final @ColorInt int PRIMARY_COLOR = 0xFF33B332;

	@Override public void apply(final StatusBarNotificationEvo evolving) throws RemoteException {
		final INotification n = evolving.notification();
		final IBundle extras = n.extras();
		final CharSequence title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE);
		if (title == null || title.length() == 0) { Log.e(TAG, "Title is missing: " + evolving); return; }

		n.setColor(PRIMARY_COLOR);    // Tint the small icon

		// WeChat uses dynamic counter as notification ID, which unfortunately will be reset upon evolving (removal, to be exact) by us,
		// causing all messages combined into one notification. So we split them by re-coding the notification ID by title.
		final int original_id = evolving.getId();
		if (original_id < NID_CONVERSATION_START) {
			Log.d(TAG, "Skip further process for non-conversation notification. ID: " + original_id);	// E.g. web login confirmation notification.
			return;
		}
		evolving.setId(title.hashCode());
		extras.putBoolean(NotificationCompat.EXTRA_SHOW_WHEN, true);

		final IBundle car_extender = extras.getBundle(EXTRA_CAR_EXTENDER);
		MessagingStyle messaging = car_extender != null ? buildMessagingFromCarExtender(evolving.getKey(), n, car_extender, extras) : null;
		if (messaging == null) messaging = buildMessagingFromArchive(evolving, n, title, extras);
		if (messaging == null) return;

		// Add additional replies filled by us in the proxied procedure of direct-reply.
		@SuppressWarnings("unchecked") final List<CharSequence> inputs = extras.getCharSequenceArray(EXTRA_REMOTE_INPUT_HISTORY);
		if (inputs != null) for (final CharSequence input : inputs) messaging.addMessage(input, 0, null);

		final Bundle addition = new Bundle();
		messaging.addCompatExtras(addition);
		for (final String key : addition.keySet()) {    // Copy the extras generated by MessagingStyle to notification extras.
			final Object value = addition.get(key);
			if (value instanceof CharSequence) extras.putCharSequence(key, (CharSequence) value);
			else if (value instanceof Parcelable[]) extras.putParcelableArray(key, Arrays.asList((Parcelable[]) value));
			else Log.w(TAG, "Unsupported extra \"" + key + "\": " + value);
		}
		extras.putCharSequence(NotificationCompat.EXTRA_CONVERSATION_TITLE, title);
		extras.putString(NotificationCompat.EXTRA_TEMPLATE, TEMPLATE_MESSAGING);
	}

	private @Nullable MessagingStyle buildMessagingFromCarExtender(final String key, final INotification n, final IBundle car_extender, final IBundle extras) throws RemoteException {
		final IBundle conversation = car_extender.getBundle(EXTRA_CONVERSATION);
		if (conversation == null) { Log.w(TAG, EXTRA_CONVERSATION + " is missing"); return null; }
		final List<?> parcelable_messages = conversation.getParcelableArray(KEY_MESSAGES);
		if (parcelable_messages == null) { Log.w(TAG, KEY_MESSAGES + " is missing"); return null; }
		final MessagingStyle messaging = new MessagingStyle(getText(R.string.self_display_name));
		if (parcelable_messages.isEmpty()) {
			messaging.addMessage(extras.getCharSequence(NotificationCompat.EXTRA_TEXT), 0, SENDER_PLACEHOLDER);
		} else {
			final List<MessagingStyle.Message> messages = new ArrayList<>();
			boolean has_named_sender = false;
			for (final Object parcelable_message : parcelable_messages) {
				if (! (parcelable_message instanceof Bundle)) return null;
				String text = ((Bundle) parcelable_message).getString(KEY_TEXT);
				CharSequence sender = ((Bundle) parcelable_message).getString(KEY_AUTHOR);    // Always null for direct message. (non-null for group message)
				if (text == null) {
					Log.w(TAG, KEY_TEXT + " is missing");
					return null;
				}
				final int pos_colon;
				if (sender == null && (pos_colon = text.indexOf(": ")) > 0) {
					sender = text.substring(0, pos_colon);
					text = text.substring(pos_colon + 2);
				}
				if (sender != null) has_named_sender = true;
				messages.add(new MessagingStyle.Message(text, 0/* TODO: Any effect in Android Auto? */, sender));
			}
			for (final MessagingStyle.Message message : messages) {
				if (has_named_sender || message.getSender() != null) messaging.addMessage(message);	// null means "you" only if any other named sender is present. (group chat)
				else messaging.addMessage(new MessagingStyle.Message(message.getText(), message.getTimestamp(), SENDER_PLACEHOLDER));	// Otherwise, sender is unnecessary.
			}
		}

		final PendingIntent on_read = conversation.getParcelable(KEY_ON_READ).get();
		if (on_read != null) n.setDeleteIntent(on_read);									// Swipe to mark read
		final PendingIntent on_reply = conversation.getParcelable(KEY_ON_REPLY).get();
		if (on_reply != null) {
			final RemoteInput remote_input = conversation.getParcelable(KEY_REMOTE_INPUT).get();
			if (remote_input != null) {
				@SuppressWarnings("unchecked") final List<CharSequence> input_history = extras.getCharSequenceArray(EXTRA_REMOTE_INPUT_HISTORY);
				final PendingIntent proxy = proxyDirectReply(key, on_reply, remote_input, input_history);
				if (proxy != null) {
					final RemoteInput.Builder tweaked = new RemoteInput.Builder(remote_input.getResultKey()).addExtras(remote_input.getExtras())
							.setAllowFreeFormInput(remote_input.getAllowFreeFormInput());
					final String[] participants = conversation.getStringArray(KEY_PARTICIPANTS);
					if (participants != null && participants.length > 0) {
						final StringBuilder label = new StringBuilder();
						for (final String participant : participants) label.append(',').append(participant);
						tweaked.setLabel(label.subSequence(1, label.length()));
					} else tweaked.setLabel(remote_input.getResultKey());

					final Action.Builder builder = new Action.Builder(0, getString(R.string.action_reply), proxy).addRemoteInput(tweaked.build());
					if (SDK_INT >= N) builder.setAllowGeneratedReplies(true);		// Enable "Smart Reply"
					n.addAction(builder.build());
				}
			}
		}
		final long latest_timestamp = conversation.getLong(KEY_TIMESTAMP, 0);
		if (latest_timestamp != 0) n.setWhen(latest_timestamp);
		return messaging;
	}

	/** Intercept the PendingIntent in RemoteInput to update the notification with replied message upon success. */
	@RequiresApi(KITKAT_WATCH) private @Nullable PendingIntent proxyDirectReply(
			final String key, final PendingIntent on_reply, final RemoteInput remote_input, final @Nullable List<CharSequence> input_history) {
		final Intent proxy_intent = new Intent(ACTION_REPLY).setData(Uri.fromParts(SCHEME_KEY, key, null)).setPackage(getPackageName())
				.putExtra(EXTRA_PENDING_INTENT, on_reply).putExtra(EXTRA_RESULT_KEY, remote_input.getResultKey());
		if (input_history != null) proxy_intent.putCharSequenceArrayListExtra(EXTRA_REMOTE_INPUT_HISTORY, new ArrayList<>(input_history));
		return PendingIntent.getBroadcast(this, 0, proxy_intent, FLAG_UPDATE_CURRENT);
	}

	private final BroadcastReceiver mReplyReceiver = new BroadcastReceiver() { @RequiresApi(KITKAT_WATCH) @Override public void onReceive(final Context context, final Intent proxy_intent) {
		final PendingIntent pending_intent = proxy_intent.getParcelableExtra(EXTRA_PENDING_INTENT);
		final String result_key = proxy_intent.getStringExtra(EXTRA_RESULT_KEY);
		final Uri data = proxy_intent.getData();
		if (data == null) return;
		final String key = data.getSchemeSpecificPart();
		final ArrayList<CharSequence> input_history = proxy_intent.getCharSequenceArrayListExtra(EXTRA_REMOTE_INPUT_HISTORY);
		try {
			final Intent input_data = new Intent();
			input_data.setClipData(proxy_intent.getClipData());
			pending_intent.send(WeChatDecorator.this, 0, input_data, new PendingIntent.OnFinished() { @Override public void onSendFinished(final PendingIntent pendingIntent, final Intent intent, final int resultCode, final String resultData, final Bundle resultExtras) {
				final Bundle input = RemoteInput.getResultsFromIntent(input_data);
				if (input == null) return;
				final CharSequence text = input.getCharSequence(result_key);

				if (BuildConfig.DEBUG) Log.d(TAG, "Reply sent to " + intent.getStringExtra(KEY_WECHAT_CONVERSATION_ID) + ": " + text);
				final Bundle addition = new Bundle();
				final CharSequence[] inputs = input_history == null ? new CharSequence[] { text }
						: input_history.toArray(new CharSequence[input_history.add(text) ? input_history.size() : 0]);
				addition.putCharSequenceArray(EXTRA_REMOTE_INPUT_HISTORY, inputs);
				try {
					recastNotification(key, addition);
				} catch (final RemoteException e) {
					Log.e(TAG, "Failed to recast notification: " + key, e);
				}
			}}, null);
		} catch (final PendingIntent.CanceledException e) {
			Log.w(TAG, "The PendingIntent for reply is already cancelled.");
			abortBroadcast();
		}
	}};

	private MessagingStyle buildMessagingFromArchive(final StatusBarNotificationEvo evolving, final INotification n, final CharSequence title,
													 final IBundle extras) throws RemoteException {
		// Chat history in big content view
		final List<StatusBarNotificationEvo> history = getArchivedNotifications(evolving.getOriginalKey(), 20);
		if (history.isEmpty()) {
			Log.d(TAG, "No history");
			return null;
		}

		final LongSparseArray<CharSequence> lines = new LongSparseArray<>(KMaxNumLines);
		CharSequence text = null;
		int count = 0, num_lines_with_colon = 0;
		final String redundant_prefix = title.toString() + ": ";
		for (final StatusBarNotificationEvo each : history) {
			final INotification notification = each.notification();
			final IBundle its_extras = notification.extras();
			final CharSequence its_title = its_extras.getCharSequence(NotificationCompat.EXTRA_TITLE);
			if (! title.equals(its_title)) {
				Log.d(TAG, "Skip other conversation with the same key in archive: " + its_title);	// ID reset by WeChat due to notification removal in previous evolving
				continue;
			}
			final CharSequence its_text = its_extras.getCharSequence(NotificationCompat.EXTRA_TEXT);
			if (its_text == null) {
				Log.w(TAG, "No text in archived notification.");
				continue;
			}
			final int result = trimAndExtractLeadingCounter(its_text);
			if (result >= 0) {
				count = result & 0xFFFF;
				CharSequence trimmed_text = its_text.subSequence(result >> 16, its_text.length());
				if (trimmed_text.toString().startsWith(redundant_prefix))    // Remove redundant prefix
					trimmed_text = trimmed_text.subSequence(redundant_prefix.length(), trimmed_text.length());
				else if (trimmed_text.toString().indexOf(": ") > 0) num_lines_with_colon++;
				lines.put(notification.getWhen(), text = trimmed_text);
			} else {
				count = 1;
				lines.put(notification.getWhen(), text = its_text);
				if (text.toString().indexOf(": ") > 0) num_lines_with_colon++;
			}
		}
		n.setNumber(count);
		if (lines.size() == 0) {
			Log.w(TAG, "No lines extracted, expected " + count);
			return null;
		}

		extras.putCharSequence(NotificationCompat.EXTRA_TEXT, text);    // Latest message text for collapsed layout.

		final MessagingStyle messaging = new MessagingStyle(getText(R.string.self_display_name));
		final boolean sender_inline = num_lines_with_colon == lines.size();
		for (int i = 0; i < lines.size(); i++) {    // All lines have colon in text
			final long when = lines.keyAt(i);
			final CharSequence line = lines.valueAt(i);
			final int pos_colon;
			if (sender_inline && (pos_colon = line.toString().indexOf(": ")) > 0)
				messaging.addMessage(line.subSequence(pos_colon + 2, line.length()), when, line.subSequence(0, pos_colon));
			else messaging.addMessage(line, when, SENDER_PLACEHOLDER);
		}
		return messaging;
	}

	/**
	 * @return the extracted count in 0xFF range and start position in 0xFF00 range
	 */
	private static int trimAndExtractLeadingCounter(final CharSequence text) {
		// Parse and remove the leading "[n]" or [n条/則/…]
		if (text == null || text.length() < 4 || text.charAt(0) != '[') return - 1;
		int text_start = 2, count_end;
		while (text.charAt(text_start++) != ']') if (text_start >= text.length()) return - 1;

		try {
			final String num = text.subSequence(1, text_start - 1).toString();    // may contain the suffix "条/則"
			for (count_end = 0; count_end < num.length(); count_end++) if (! Character.isDigit(num.charAt(count_end))) break;
			if (count_end == 0) return - 1;        // Not the expected "unread count"
			final int count = Integer.parseInt(num.substring(0, count_end));
			if (count < 2) return - 1;

			return count < 0xFFFF ? (count & 0xFFFF) | ((text_start << 16) & 0xFFFF0000) : 0xFFFF | ((text_start << 16) & 0xFF00);
		} catch (final NumberFormatException ignored) {
			Log.d(TAG, "Failed to parse: " + text);
			return - 1;
		}
	}

	@Override protected void onConnected() {
		final IntentFilter filter = new IntentFilter(ACTION_REPLY);
		filter.addDataScheme(SCHEME_KEY);
		registerReceiver(mReplyReceiver, filter);
	}

	@Override public void onDestroy() {
		try { unregisterReceiver(mReplyReceiver); } catch (final RuntimeException ignored) {}
		super.onDestroy();
	}

	private static final String TAG = "Nevo.Decorator[WeChat]";
}
