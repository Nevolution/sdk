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

import com.oasisfeng.android.content.pm.ParceledListSlice;
import com.oasisfeng.nevo.decorator.INevoDecorator;

interface INevoController {

	ParceledListSlice/*<StatusBarNotificationEvo>*/ getActiveNotifications(in INevoDecorator token);
    ParceledListSlice/*<StatusBarNotificationEvo>*/ getArchivedNotifications(in INevoDecorator token, String key, int limit);
    ParceledListSlice/*<StatusBarNotificationEvo>*/ getNotifications(in INevoDecorator token, in List<String> keys);
    oneway void cancelNotification(in INevoDecorator token, String key);
    oneway void reviveNotification(in INevoDecorator token, String key);
}
