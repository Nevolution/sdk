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

package com.oasisfeng.nevo.sdk;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Parcel;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static android.content.Context.CONTEXT_IGNORE_SECURITY;
import static android.content.Context.CONTEXT_INCLUDE_CODE;
import static android.support.annotation.RestrictTo.Scope.LIBRARY;

/**
 * Connect to remote implementation within Nevolution engine.
 *
 * This is an internal class, NEVER use it in decorator project.
 *
 * Created by Oasis on 2018/4/6.
 */
@RestrictTo(LIBRARY) class RemoteImplementation {

	private static final String ENGINE_PACKAGE = "com.oasisfeng.nevo";
	private static final String RES_REMOTE_CLASS = "string/sdk_remote_class";

	/** This operation might be destructive to the given MutableNotification. DO NOT USE THE NOTIFICATION INSTANCE AFTERWARD. */
	static void writeBackToParcel(final Parcel out, final int flags, final MutableNotification notification, final Notification original) {
		try {
			sMutableNotificationWriteBack.invoke(null, out, flags, notification, original);
		} catch (final InvocationTargetException e) {
			final Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) throw (RuntimeException) cause;
			if (cause instanceof Error) throw (Error) cause;
			throw new RuntimeException(cause);
		} catch (final IllegalAccessException e) {
			throw new IllegalStateException("Incompatible with currently installed version of Nevolution", e);
		}
	}

	static void initializeIfNotYet(final Context context) {
		if (sMutableNotificationWriteBack != null) return;
		final Context engine_context;
		try {
			if (ENGINE_PACKAGE.equals(context.getPackageName())) engine_context = context;
			else engine_context = context.createPackageContext(ENGINE_PACKAGE, CONTEXT_INCLUDE_CODE | CONTEXT_IGNORE_SECURITY);
			final ClassLoader classloader = engine_context.getClassLoader();
			final Resources res = engine_context.getResources();
			final int res_id = res.getIdentifier(RES_REMOTE_CLASS, null, ENGINE_PACKAGE);
			if (res_id == 0) throw new Resources.NotFoundException(RES_REMOTE_CLASS);
			final String name = res.getString(res_id);
			sClass = classloader.loadClass(name);
			sMutableNotificationWriteBack = sClass.getMethod("writeBackToParcel", Parcel.class, int.class, Notification.class, Notification.class);
		} catch (final PackageManager.NameNotFoundException e) {
			throw new IllegalStateException("Nevolution is not installed");
		} catch (final ClassNotFoundException | NoSuchMethodException | ClassCastException | Resources.NotFoundException e) {
			throw new IllegalStateException("Incompatible with currently installed version of Nevolution", e);
		}
	}

	@VisibleForTesting static Class<?> sClass;
	private static Method sMutableNotificationWriteBack;
}
