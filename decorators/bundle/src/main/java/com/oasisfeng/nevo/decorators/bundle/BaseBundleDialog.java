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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Richard on 2015/10/3.
 */
public abstract class BaseBundleDialog extends DialogFragment {

    public final static String KEY_BUNDLES = "KEY_BUNDLES";
    public final static String KEY_PACKAGE_RULE = "KEY_PACKAGE_RULE";
    public final static String KEY_TITLE_RULE = "KEY_TITLE_RULE";

    private final String TAG = BundleActionDialog.class.getName();

    private OnSetBundleRule onSetBundleRule;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_bundle_action, null);
        HashSet<String> strings = Sets.newHashSet(getArguments().getStringArrayList(KEY_BUNDLES));
        Collections.addAll(strings, getResources().getStringArray(R.array.predefined_bundles));
        final String[] bundles = strings.toArray(new String[strings.size()]);
        Arrays.sort(bundles);
        ListAdapter adapter = new BundlesAdapter(getActivity(),
                android.R.layout.simple_list_item_single_choice,
                android.R.id.text1,
                bundles, getCurrentBundle());
        ((ListView) view.findViewById(R.id.bundle_list)).setAdapter(adapter);
        ((ListView) view.findViewById(R.id.bundle_list)).setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(final AdapterView<?> parent, final View view1, final int position, final long id) {
            BaseBundleDialog.this.onApply(bundles[position]);
        }});

        TextView tvCreateNewBundle = (TextView) view.findViewById(R.id.bundle_new);
        tvCreateNewBundle.setText(R.string.new_bundle);
        tvCreateNewBundle.setOnClickListener(new View.OnClickListener() { @Override public void onClick(final View v) {
            final View dialogView = LayoutInflater.from(BaseBundleDialog.this.getActivity()).inflate(R.layout.dialog_set_rules, null);
            final EditText editText = (EditText) dialogView.findViewById(android.R.id.edit);
            new AlertDialog.Builder(BaseBundleDialog.this.getActivity())
                    .setTitle(R.string.add_bundle_hint)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() { @Override public void onClick(final DialogInterface dialog, final int which) {
                        dialog.dismiss();
                        if (editText.getText().length() == 0)
                            Toast.makeText(BaseBundleDialog.this.getActivity(), R.string.bundle_name_hint, Toast.LENGTH_SHORT).show();
                        else
                            BaseBundleDialog.this.onApply(editText.getText().toString());
                    }})
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() { @Override public void onClick(final DialogInterface dialog, final int which) {
                        dialog.dismiss();
                    }})
                    .create().show();
        }
        });
        TextView tvRemoveText = (TextView) view.findViewById(R.id.bundle_remove);
        tvRemoveText.setOnClickListener(new View.OnClickListener() { @Override public void onClick(final View v) {
            BaseBundleDialog.this.onRemove();
        }});
        onSetRemoveText(tvRemoveText);

        return new AlertDialog.Builder(getActivity())
                .setTitle(getDialogTitle())
                .setView(view)
                .create();
    }

    protected String getPackage() { return NevoActionActivities.getPackageName(getActivity()); }
    protected CharSequence getAppName() { return NevoActionActivities.getPackageLabel(getActivity()); }
    protected String getTitle() { return NevoActionActivities.getNotificationTitle(getActivity()); }
    protected String getPackageRule() { return getArguments().getString(KEY_PACKAGE_RULE); }
    protected String getTitleRule() { return getArguments().getString(KEY_TITLE_RULE); }


    protected abstract void onSetRemoveText(TextView textView);
    protected abstract void onRemove();
    protected abstract void onApply(String newBundle);
    protected abstract String getDialogTitle();
    protected abstract String getCurrentBundle();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        onSetBundleRule = (OnSetBundleRule) activity;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        getActivity().finish();
        getActivity().overridePendingTransition(0, 0);
    }

    protected void setBundleRule(String pkg, String title, String bundle) {
        onSetBundleRule.onSetBundleRule(pkg, title, bundle);
        dismiss();
    }

    public interface OnSetBundleRule {
        void onSetBundleRule(String pkg, String title, String rule);
    }

    public static void showSettingsDialog(List<String> bundles, String rulePackage, String ruleTitle, FragmentManager fm) {
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(KEY_BUNDLES, Lists.newArrayList(bundles));
        bundle.putString(KEY_PACKAGE_RULE, rulePackage);
        bundle.putString(KEY_TITLE_RULE, ruleTitle);

        BundleSettingsDialog dialog = new BundleSettingsDialog();
        dialog.setArguments(bundle);
        dialog.show(fm, BundleSettingsDialog.class.getName());
    }

    public static void showActionDialog(List<String> bundles, String rulePackage, String ruleTitle, FragmentManager fm) {
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(KEY_BUNDLES, Lists.newArrayList(bundles));
        bundle.putString(KEY_PACKAGE_RULE, rulePackage);
        bundle.putString(KEY_TITLE_RULE, ruleTitle);

        BundleActionDialog dialog = new BundleActionDialog();
        dialog.setArguments(bundle);
        dialog.show(fm, BundleActionDialog.class.getName());
    }
}
