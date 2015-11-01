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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.oasisfeng.nevo.NevoConstants;

/**
 * Created by Richard on 2015/10/2.
 */
public class NevoSettingsActivities {
    public static String getDecorator(Activity activity) {
        return activity.getIntent().getStringExtra(NevoConstants.EXTRA_DECORATOR_COMPONENT);
    }

    public static String getPackageName(Activity activity) {
        return activity.getIntent().getStringExtra(NevoConstants.EXTRA_NOTIFICATION_PACKAGE);
    }

    public static CharSequence getPackageLabel(Activity activity) {
        final PackageManager pm = activity.getPackageManager();
        try {
            final ApplicationInfo info = pm.getApplicationInfo(getPackageName(activity), 0);
            return pm.getApplicationLabel(info);
        } catch (PackageManager.NameNotFoundException ignored) { }
        return null;
    }
}
