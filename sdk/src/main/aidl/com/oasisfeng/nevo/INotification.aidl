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

import android.graphics.drawable.Icon;
import android.widget.RemoteViews;
import java.util.List;
import com.oasisfeng.android.os.IBundle;
import android.app.Notification.Action;

interface INotification {

	/** @Deprecated Never use this method to retrieve the whole notification instance, use other methods to get and modify properties instead. */
    Notification get();

    IBundle extras();

    boolean hasCustomContentView();
    RemoteViews getCustomContentView();
    /** Whenever possible, avoid custom content view. It is inefficient and not decorator-pipeline friendly */
    oneway void setCustomContentView(in RemoteViews views);

    boolean hasCustomBigContentView();
    RemoteViews getCustomBigContentView();
    /** Whenever possible, avoid custom big content view. It is inefficient and not decorator-pipeline friendly */
    oneway void setCustomBigContentView(in RemoteViews views);

    boolean hasCustomHeadsUpContentView();
    RemoteViews getCustomHeadsUpContentView();
    /** Whenever possible, avoid custom heads-up content view. It is inefficient and not decorator-pipeline friendly */
    oneway void setCustomHeadsUpContentView(in RemoteViews views);

    int getFlags();
    oneway void addFlags(int flags);
    oneway void removeFlags(int flags);

    long getWhen();
    oneway void setWhen(long when);

    int getNumber();
    oneway void setNumber(int number);

    int getColor();
    oneway void setColor(int color);

    String getGroup();
    oneway void setGroup(String group);

    int getPriority();
    oneway void setPriority(int priority);

    long[] getVibrate();
    oneway void setVibrate(in long[] vibrate);

    Action[] getActions();
    oneway void addAction(in Action action);

    oneway void setContentIntent(in PendingIntent intent);
    oneway void setDeleteIntent(in PendingIntent intent);
}
