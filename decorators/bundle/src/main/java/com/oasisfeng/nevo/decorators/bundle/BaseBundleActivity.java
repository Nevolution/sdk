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
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;

/**
 * Created by Richard on 2015/10/3.
 */
public abstract class BaseBundleActivity extends Activity implements BaseBundleDialog.OnSetBundleRule {
    private ServiceConnection mConnection;
    private INotificationBundle iNotificationBundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);

        Intent intent = new Intent(INotificationBundle.class.getName()).setPackage(getPackageName());

        if (!bindService(intent, mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                iNotificationBundle = INotificationBundle.Stub.asInterface(service);
                try {
                    onShowDialog();
                } catch (RemoteException e) {
                    e.printStackTrace();
                    finish();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                unbindService(this);
                mConnection = null;
                finish();
            }
        }, BIND_AUTO_CREATE)) {
            Toast.makeText(this, R.string.hint_bundle_bind_failed, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onSetBundleRule(String pkg, String title, String rule) {
        try {
            iNotificationBundle.setRule(pkg, title, rule);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        if (mConnection != null) {
            unbindService(mConnection);
        }
        overridePendingTransition(0, 0);
        super.onDestroy();
    }

    protected INotificationBundle getBundleBinder() { return iNotificationBundle; }

    protected abstract void onShowDialog() throws RemoteException;
}
