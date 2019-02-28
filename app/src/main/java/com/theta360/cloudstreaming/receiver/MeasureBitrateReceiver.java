/**
 * Copyright 2018 Ricoh Company, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.theta360.cloudstreaming.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

/**
 * Receive broadcast of bit rate measurement
 */
public class MeasureBitrateReceiver extends BroadcastReceiver {
    public static final String MEASURE_BITRATE = "com.theta360.cloudstreaming.measure-bitrate";

    public static final String KEY_SERVER_URL = "KeyServerUrl";
    public static final String KEY_STREAM_NAME = "KeyStreamName";
    public static final String KEY_WIDTH = "KeyWidth";
    public static final String KEY_HEIGHT = "KeyHeight";

    private Callback mCallback;

    public MeasureBitrateReceiver(Callback callback) {
        mCallback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        String serverUrl = intent.getStringExtra(KEY_SERVER_URL);
        String streamName = intent.getStringExtra(KEY_STREAM_NAME);
        int width = intent.getIntExtra(KEY_WIDTH, 0);
        int height = intent.getIntExtra(KEY_HEIGHT, 0);

        if (MEASURE_BITRATE.equals(action)) {
            mCallback.callMeasureBitrateCallback(serverUrl, streamName, width, height);
        }
    }

    public interface Callback {
        void callMeasureBitrateCallback(String serverUrl, String streamName, int width, int height);
    }
}
