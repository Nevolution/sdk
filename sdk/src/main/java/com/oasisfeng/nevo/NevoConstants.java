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

package com.oasisfeng.nevo;

import android.content.ComponentName;
import android.service.notification.StatusBarNotification;

import com.oasisfeng.nevo.StatusBarNotificationCompat.SbnCompat;

/**
 * Constants
 *
 * Created by Richard on 2015/4/12.
 */
public class NevoConstants {

	/**
	 * Mark the notification to be removed, in decorator during evolution.
	 *
	 * Currently only the built-in decorator has privilege to use it. It is ignored from 3rd-party decorators.
	 * Talk to us in the developer community if you do need this in your decorator.
	 */
	public static final String EXTRA_REMOVED = "nevo.removed";

	/** Mark the notification being posted to be ignored by Nevolution, thus always untouched. */
	public static final String EXTRA_PHANTOM = "nevo.phantom";

	/**
	 * The component name of the decorator.
	 * This is one of the arguments passed to decorator settings activity and action activity.
	 */
	public static final String EXTRA_DECORATOR_COMPONENT = "nevo.decorator.component";
	/**
	 * The package name of notifications to be evolved.
	 * This is one of the arguments passed to decorator settings activity and action activity.
	 */
	public static final String EXTRA_NOTIFICATION_PACKAGE = "nevo.notification.package";
	/**
	 * The key of current notification, as returned by {@link SbnCompat#keyOf(StatusBarNotification)}
	 * This is one of the arguments passed to decorator action activity.
	 */
	public static final String EXTRA_NOTIFICATION_KEY = "nevo.notification.key";
	/**
	 * The title of current notification.
	 * This is one of the arguments passed to decorator action activity.
	 */
	public static final String EXTRA_NOTIFICATION_TITLE = "nevo.notification.title";

	/**
	 * The action to start main Nevolution user interface and tell user to enable their own app-specified decorator to certain application.
	 * You should also set Category to: "android.intent.category.PREFERENCE".
	 * Note: this is only a suggestion to user, please start with startActivityForResult().
	 *		 {@link android.app.Activity#RESULT_OK}: User enabled your decorator or already enabled for that application.
	 *		 {@link android.app.Activity#RESULT_CANCELED}: This operation is canceled by user.
	 *		 {@link android.app.Activity#RESULT_FIRST_USER}: Other internal error for debug propose. Like your decorator doesn't not declared to support this application.
	 * Please pass following params:
	 * 		 {@link #EXTRA_DECORATOR_COMPONENT}: Your full decorator component name in string, see {@link ComponentName#flattenToString()}.
	 * 		 {@link #EXTRA_NOTIFICATION_PACKAGE}: The application package which you hope user enable decorator on.
	 *
	 */
	public static final String ACTION_ENABLE_DECORATOR = "com.oasisfeng.nevo.mobile.EnableDecorator";
}
