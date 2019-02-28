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
 * Receive live streaming broadcasts
 */
public class LiveStreamingReceiver extends BroadcastReceiver {
    public static final String TOGGLE_LIVE_STREAMING = "com.theta360.cloudstreaming.toggle-live-streaming";

    private Callback mCallback;

    public LiveStreamingReceiver(Callback callback) {
        mCallback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (TOGGLE_LIVE_STREAMING.equals(action)) {
            mCallback.callStreamingCallback();
        }
    }

    public interface Callback {
        void callStreamingCallback();
    }
}
