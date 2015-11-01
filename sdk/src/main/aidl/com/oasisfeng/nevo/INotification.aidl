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

interface INotification {

    Notification get();

    IBundle extras();

    RemoteViews getContentView();
    oneway void setContentView(in RemoteViews views);

    boolean hasBigContentView();
    RemoteViews getBigContentView();
    oneway void setBigContentView(in RemoteViews views);

    boolean hasHeadsUpContentView();
    RemoteViews getHeadsUpContentView();
    oneway void setHeadsUpContentView(in RemoteViews views);

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

    /** For Android 6.0+ */
    Icon getSmallIcon();
    /** For Android 6.0+ */
    oneway void setSmallIcon(in Icon icon);
    /** For Android 6.0+ */
    Icon getLargeIcon();
    /** For Android 6.0+ */
    oneway void setLargeIcon(in Icon icon);
}
