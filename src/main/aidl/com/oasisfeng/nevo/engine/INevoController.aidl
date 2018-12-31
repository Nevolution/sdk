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

package com.oasisfeng.nevo.engine;

import android.service.notification.StatusBarNotification;
import com.oasisfeng.nevo.decorator.INevoDecorator;
import android.app.NotificationChannel;

interface INevoController {

    List<StatusBarNotification> getNotifications(in INevoDecorator token, int type, in List<String> keys, int limit, in Bundle args);
    oneway void performNotificationAction(in INevoDecorator token, int action, String key, in Bundle args);
	void createNotificationChannels(in INevoDecorator token, String pkg, in List<NotificationChannel> channels, in Bundle args);
	/* API version 4 */
	List<NotificationChannel> getNotificationChannels(in INevoDecorator token, String pkg, in List<String> channels, in Bundle args);
	void deleteNotificationChannel(in INevoDecorator token, String pkg, String channel, in Bundle args);
}
