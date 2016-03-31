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

package com.oasisfeng.nevo.decorators.media;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.Notification.Action;
import android.app.PendingIntent;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.oasisfeng.nevo.INotification;
import com.oasisfeng.nevo.StatusBarNotificationEvo;
import com.oasisfeng.nevo.decorator.NevoDecoratorService;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Convert custom content view with playback actions into standard {@link android.app.Notification.MediaStyle}
 *
 * Created by Oasis on 2015/10/22.
 */
public class MediaPlayerDecorator extends NevoDecoratorService {

	@Override protected void apply(final StatusBarNotificationEvo evolving) throws Exception {
		if (evolving.isClearable()) return;		// Just sticky notification, to reduce the overhead.
		final INotification n = evolving.notification();
		RemoteViews content_view = n.getBigContentView();	// Prefer big content view since it usually contains more actions
		if (content_view == null) content_view = n.getContentView();
		if (content_view == null) return;
		final AtomicReference<IntentSender> sender_holder = new AtomicReference<>();
		final View views = content_view.apply(new ContextWrapper(this) {
			@Override public void startIntentSender(final IntentSender intent, final Intent fillInIntent, final int flagsMask, final int flagsValues, final int extraFlags) throws IntentSender.SendIntentException {
				startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags, null);
			}
			@Override public void startIntentSender(final IntentSender intent, final Intent fillInIntent, final int flagsMask, final int flagsValues, final int extraFlags, final Bundle options) throws IntentSender.SendIntentException {
				sender_holder.set(intent);
			}
		}, null);
		if (! (views instanceof ViewGroup)) return;

		final List<NotificationCompat.Action> actions = new ArrayList<>(8);
		findClickable((ViewGroup) views, new Predicate<View>() { @Override public boolean apply(final View v) {
			sender_holder.set(null);
			v.performClick();        // trigger startIntentSender() above
			final IntentSender sender = sender_holder.get();
			if (sender == null) {
				Log.w(TAG, v + " has OnClickListener but no PendingIntent in it.");
				return true;
			}

			final PendingIntent pending_intent = getPendingIntent(sender);
			if (v instanceof TextView) {
				final CharSequence text = ((TextView) v).getText();
				actions.add(new NotificationCompat.Action(0, text, pending_intent));
			} else if (v instanceof ImageView) {
				final int res = getImageViewResource((ImageView) v);
				CharSequence title = v.getContentDescription();
				if (title == null) title = "<" + System.identityHashCode(v);
				actions.add(new NotificationCompat.Action(res, title, pending_intent));
			} else {
				CharSequence title = v.getContentDescription();
				if (title == null) title = "<" + System.identityHashCode(v);
				actions.add(new NotificationCompat.Action(0, title, pending_intent));
			}
			return true;
		}});

		final Notification mirror; final String pkg = evolving.getPackageName();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			final Notification.Builder b = new Notification.Builder(createPackageContext(pkg, 0))
					.setContentTitle(n.extras().getCharSequence(NotificationCompat.EXTRA_TITLE));
			for (final NotificationCompat.Action action : actions)
				b.addAction(new Action.Builder(Icon.createWithResource(pkg, action.getIcon()),
						action.getTitle(), action.getActionIntent()).build());
			mirror = b.build();
		} else {
			final NotificationCompat.Builder b = new NotificationCompat.Builder(createPackageContext(pkg, 0))
					.setContentTitle(n.extras().getCharSequence(NotificationCompat.EXTRA_TITLE))
					.setLargeIcon(n.extras().getParcelable(NotificationCompat.EXTRA_LARGE_ICON).<Bitmap>get())
					.setSmallIcon(android.R.drawable.ic_media_play);
			for (final NotificationCompat.Action action : actions) b.addAction(action);
			mirror = b.build();
		}
		final NotificationManagerCompat nm = NotificationManagerCompat.from(this);
		nm.notify("M<" + evolving.getPackageName() + (evolving.getTag() != null ? ">" + evolving.getTag() : ">"), evolving.getId(), mirror);
	}

	/** @return continue or not */
	private static boolean findClickable(final ViewGroup views, final Predicate<View> callback) {
		for (int i = 0; i < views.getChildCount(); i ++) {
			final View view = views.getChildAt(i);
			if (view instanceof ViewGroup) {
				if (! findClickable((ViewGroup) view, callback)) return false;
			} else if (view.hasOnClickListeners())
				if (! callback.apply(view)) return false;
		}
		return true;
	}

	/** Tiny hack to convert IntentSender to PendingIntent */
	private static PendingIntent getPendingIntent(final IntentSender sender) {
		final Parcel parcel = Parcel.obtain();
		try {
			parcel.setDataPosition(0);
			sender.writeToParcel(parcel, 0);
			parcel.setDataPosition(0);
			return PendingIntent.CREATOR.createFromParcel(parcel);
		} finally {
			parcel.recycle();
		}
	}

	private int getImageViewResource(final ImageView v) {
		if (ImageView_mResource == null) return 0;
		try {
			return (Integer) ImageView_mResource.get(v);
		} catch (final IllegalAccessException e) { return 0; }	// Should never happen
	}

	@TargetApi(Build.VERSION_CODES.M)
	private Icon fixIconPkg(final Icon icon, final String pkg) {
		if (Icon_getType != null && Icon_mString1 != null) try {
			final int type = (Integer) Icon_getType.invoke(icon);
			if (type == 2) Icon_mString1.set(icon, pkg);
			return icon;
		} catch (final IllegalAccessException | InvocationTargetException ignored) {}	// Should not happen
		return Icon.createWithResource("", android.R.drawable.ic_media_play);	// Fall-back to default icon
	}

	public MediaPlayerDecorator() {
		Field field = null;
		try {
			field = ImageView.class.getDeclaredField("mResource");
			field.setAccessible(true);
			if (field.getType() != int.class) {
				Log.w(TAG, "Incompatible ROM: Incorrect field type of ImageView.mResource - " + field.getType());
				field = null;
			}
		} catch (final NoSuchFieldException e) {
			Log.w(TAG, "Incompatible ROM: No field ImageView.mResource");
		}
		ImageView_mResource = field;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			Method method = null;
			try {
				method = Icon.class.getMethod("getType");
				if (method.getReturnType() != int.class) {
					Log.w(TAG, "Incompatible ROM: Incorrect method return type of Icon.getType() - " + method.getReturnType());
					method = null;
				}
			} catch (final NoSuchMethodException e) {
				Log.e(TAG, "Incompatible ROM: No method Icon.getType()");
			}
			Icon_getType = method;

			field = null;
			try {
				field = Icon.class.getDeclaredField("mString1");
				field.setAccessible(true);
				if (field.getType() != String.class) {
					Log.e(TAG, "Incompatible ROM: Incorrect field type of Icon.mString1 - " + field.getType());
					field = null;
				}
			} catch (final NoSuchFieldException e) {
				Log.e(TAG, "Incompatible ROM: No field Icon.mString1");
			}
			Icon_mString1 = field;
		} else { Icon_getType = null; Icon_mString1 = null; }
	}

	private final @Nullable Field ImageView_mResource;
	private final @Nullable Method Icon_getType;
	private final @Nullable Field Icon_mString1;

	private static final String TAG = "MediaPlayerDecorator";

	private interface Predicate<T> { boolean apply(T t); }
}
