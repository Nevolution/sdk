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
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Richard on 2015/10/2.
 */
public class BundleSettingsDialog extends BaseBundleDialog {

    @Override
    protected void onSetRemoveText(TextView textView) {
        if (TextUtils.isEmpty(getPackageRule()))
            textView.setVisibility(View.GONE);
        else {
            textView.setVisibility(View.VISIBLE);
            textView.setText(getString(R.string.bundle_remove_rule, getPackageRule()));
        }
    }

    @Override
    protected void onRemove() {
        setBundleRule(getPackage(), null, "");
        getActivity().setResult(Activity.RESULT_OK);
    }

    @Override
    protected void onApply(String newBundle) {
        setBundleRule(getPackage(), null, newBundle);
        getActivity().setResult(Activity.RESULT_OK);
    }

    @Override
    protected String getDialogTitle() {
        return getString(R.string.bundle_settings_title_primary, getAppName());
    }

    @Override
    protected String getCurrentBundle() {
        return getPackageRule();
    }
}
