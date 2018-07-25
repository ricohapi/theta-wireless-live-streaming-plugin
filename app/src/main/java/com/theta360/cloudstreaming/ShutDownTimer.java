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

package com.theta360.cloudstreaming;

/**
 * Timeout monitoring
 */
public class ShutDownTimer implements Runnable {

    private static long endTime = 3 * 60 * 1000;

    private ShutDownTimerCallBack callBack;
    private long origin;
    private boolean isStreaming;
    private boolean isExit;

    public ShutDownTimer(ShutDownTimerCallBack callBack, long time) {
        this.callBack = callBack;
        this.endTime = time;
        reset(false, endTime);
    }

    /**
     * Set the starting time and state
     *
     * @param state state
     */
    public void reset(boolean state, long time) {
        origin = System.currentTimeMillis();
        isStreaming = state;
        endTime = time;
    }

    /**
     * End thread
     */
    public void exit() {
        isExit = true;
    }

    @Override
    public void run() {
        while (!isExit) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }

            // End if it is not currently being delivered and timer is longer than the specified value.
            // Invalid when the value is 0 or less.
            if (!isStreaming && System.currentTimeMillis() - origin > endTime && endTime > 0) {
                // Call back call on exit
                this.callBack.callBack();
                break;
            }
        }
    }


}

