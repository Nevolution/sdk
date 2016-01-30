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

package com.oasisfeng.nevo.decorator;

import android.service.notification.StatusBarNotification;
import com.oasisfeng.nevo.StatusBarNotificationEvo;
import com.oasisfeng.nevo.engine.INevoController;

/** The internal interface for decorator service */
interface INevoDecorator {

    int onConnected(in INevoController controller, in Bundle options);
    void apply(in StatusBarNotificationEvo evolved, in Bundle options);
    oneway void onNotificationRemoved(in String key, in Bundle options);
    oneway void onNotificationRemovedLight(in StatusBarNotificationEvo notification, in Bundle options);
}
