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
    void setCustomContentView(in RemoteViews views);

    boolean hasCustomBigContentView();
    RemoteViews getCustomBigContentView();
    /** Whenever possible, avoid custom big content view. It is inefficient and not decorator-pipeline friendly */
    void setCustomBigContentView(in RemoteViews views);

    boolean hasCustomHeadsUpContentView();
    RemoteViews getCustomHeadsUpContentView();
    /** Whenever possible, avoid custom heads-up content view. It is inefficient and not decorator-pipeline friendly */
    void setCustomHeadsUpContentView(in RemoteViews views);

    int getFlags();
    void addFlags(int flags);
    void removeFlags(int flags);

    long getWhen();
    void setWhen(long when);

    int getNumber();
    void setNumber(int number);

    int getColor();
    void setColor(int color);

    String getGroup();
    void setGroup(String group);

    int getPriority();
    void setPriority(int priority);

    long[] getVibrate();
    void setVibrate(in long[] vibrate);

    Action[] getActions();
    void addAction(in Action action);

    void setContentIntent(in PendingIntent intent);
    void setDeleteIntent(in PendingIntent intent);
}
