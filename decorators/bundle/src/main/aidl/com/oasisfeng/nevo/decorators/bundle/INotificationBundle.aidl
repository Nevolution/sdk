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

import com.oasisfeng.nevo.StatusBarNotificationEvo;

interface INotificationBundle {

    /**
     * Set a rule for "packge (+ title) -> bundle". Setting a rule will NOT affect posted notifications.
     *
     * @param title can be null to set a package-wide rule (matched after rules with title).
     * @param bundle the bundle name, empty for not bundling (exclusion), or null to remove the rule.
     */
    oneway void setRule(String pkg, String title, String bundle);

	/** Query matched bundle for the given notification, according to the configured rules */
	String queryRuleForNotification(in StatusBarNotificationEvo sbn);

	/**
	 * Query matched bundle for the given package and title, according to the configured rules.
	 *
	 * If title is null, only package-wide rule is queried, otherwise the rule with package + title
	 * will be queried first, then package-wide rule.
	 *
	 * @return bundle name, empty for not bundling (exclusion), or null if no matched rule.
	 */
	String queryRule(String pkg, String title);

    /** Set the bundle of specified notification, replace the previous bundle setting */
    oneway void setNotificationBundle(String key, String bundle);

	/** Get the keys of notifications within specified bundle, in the order that notification was added to the bundle */
    List<String> getBundledNotificationKeys(String bundle);

	/** List all bundles defined by rules */
	List<String> getDefinedBundles();

	/** Get all defined bundle rules, key - pkg or pkg:title, value - bundle name or empty string for exclusion */
	Map/*<String, String>*/ getAllRules();
}
