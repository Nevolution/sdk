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

package com.oasisfeng.nevo.decorators.bundle;

import android.app.Activity;

import com.oasisfeng.nevo.NevoConstants;

/**
 * Created by Richard on 2015/10/2.
 */
public class NevoActionActivities extends NevoSettingsActivities {
    public static String getNotificationKey(Activity activity) {
        return activity.getIntent().getStringExtra(NevoConstants.EXTRA_NOTIFICATION_KEY);
    }

    public static String getNotificationTitle(Activity activity) {
        return activity.getIntent().getStringExtra(NevoConstants.EXTRA_NOTIFICATION_TITLE);
    }
}
