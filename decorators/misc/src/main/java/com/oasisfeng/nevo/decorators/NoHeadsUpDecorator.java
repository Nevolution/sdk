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

package com.oasisfeng.nevo.decorators;

import com.oasisfeng.nevo.sdk.MutableStatusBarNotification;
import com.oasisfeng.nevo.sdk.NevoDecoratorService;

/**
 * Disable heads-up notification
 *
 * Created by Richard on 2015/8/2.
 */
public class NoHeadsUpDecorator extends NevoDecoratorService {

	/** @see android.app.Notification */
	private static final String EXTRA_AS_HEADS_UP = "headsup";
	/** @see android.app.Notification */
	private static final int HEADS_UP_NEVER = 0;

	@Override protected void apply(final MutableStatusBarNotification evolving) {
		evolving.getNotification().extras.putInt(EXTRA_AS_HEADS_UP, HEADS_UP_NEVER);
	}
}
