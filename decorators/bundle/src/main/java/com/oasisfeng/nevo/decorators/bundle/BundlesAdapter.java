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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

/**
 * Created by Richard on 2015/10/3.
 */
public class BundlesAdapter extends ArrayAdapter {
    String currentBundle;
    public BundlesAdapter(Context context, int resource, int textViewResourceId, Object[] objects, String currentBundle) {
        super(context, resource, textViewResourceId, objects);
        this.currentBundle = currentBundle;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CheckedTextView view = (CheckedTextView) super.getView(position, convertView, parent);
        if (view.getText().equals(currentBundle))
            view.setChecked(true);
        else
            view.setChecked(false);
        return view;
    }
}
