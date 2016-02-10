/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kobeyoung81.snappyvpn;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ConfigurationActivity extends Activity implements View.OnClickListener {
    private TextView mServerAddress;
    private TextView mServerPort;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.form);

        mServerAddress = (TextView) findViewById(R.id.address);
        mServerPort = (TextView) findViewById(R.id.port);

        findViewById(R.id.connect).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            String prefix = getPackageName();
            Intent intent = new Intent(this, SnappyVPNService.class)
                    .putExtra(prefix + ".ADDRESS", mServerAddress.getText().toString())
                    .putExtra(prefix + ".PORT", mServerPort.getText().toString());
            startService(intent);
        }
    }
}
