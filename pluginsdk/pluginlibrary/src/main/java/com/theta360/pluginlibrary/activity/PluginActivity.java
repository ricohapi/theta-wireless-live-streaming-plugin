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

package com.theta360.pluginlibrary.activity;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.view.KeyEvent;

import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.values.ExitStatus;
import com.theta360.pluginlibrary.values.LedColor;
import com.theta360.pluginlibrary.values.LedTarget;
import com.theta360.pluginlibrary.values.OledDisplay;
import com.theta360.pluginlibrary.UncaughtException;
import com.theta360.pluginlibrary.receiver.KeyReceiver;
import com.theta360.pluginlibrary.values.TextArea;
import com.theta360.pluginlibrary.values.ThetaModel;

import java.util.Map;

/**
 * PluginActivity
 */
public abstract class PluginActivity extends AppCompatActivity {
    private boolean isCamera = false;
    private boolean isAutoClose = true;
    private boolean isClosed = false;
    private String mUserOption;
    private boolean isApConnected = false;

    private KeyCallback mKeyCallback;
    private KeyReceiver mKeyReceiver;
    private KeyReceiver.Callback onKeyReceiver = new KeyReceiver.Callback() {
        @Override
        public void onKeyDownCallback(int keyCode, KeyEvent event) {
            if (event.getKeyCode() == KeyReceiver.KEYCODE_MEDIA_RECORD
                    && event.isLongPress()) {
                if (mKeyCallback != null) {
                    mKeyCallback.onKeyLongPress(keyCode, event);
                }
                if (isAutoClose) {
                    close();
                }
            } else {
                if (mKeyCallback != null) {
                    if (event.getRepeatCount() == 0) {
                        mKeyCallback.onKeyDown(keyCode, event);
                    } else if (event.isLongPress()) {
                        mKeyCallback.onKeyLongPress(keyCode, event);
                    }
                }
            }
        }

        @Override
        public void onKeyUpCallback(int keyCode, KeyEvent event) {
            if (mKeyCallback != null) {
                mKeyCallback.onKeyUp(keyCode, event);
            }
        }
    };

    // For X
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        switch(event.getAction()) {
            case KeyEvent.ACTION_DOWN:
                if (event.getKeyCode() == KeyReceiver.KEYCODE_MEDIA_RECORD
                        && event.isLongPress()) {
                    if (mKeyCallback != null) {
                        mKeyCallback.onKeyLongPress(keyCode, event);
                    }
                } else {
                    if (mKeyCallback != null) {
                        if (event.getRepeatCount() == 0) {
                            mKeyCallback.onKeyDown(keyCode, event);
                        } else if (event.isLongPress()) {
                            mKeyCallback.onKeyLongPress(keyCode, event);
                        }
                    }
                }
                break;
            case KeyEvent.ACTION_UP:
                if (mKeyCallback != null) {
                    mKeyCallback.onKeyUp(keyCode, event);
                }
                break;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Fix to be portrait
        UncaughtException uncaughtException = new UncaughtException(getApplicationContext(),
                new UncaughtException.Callback() {
                    @Override
                    public void onException(String message) {
                        notificationError(message);
                    }
                });
        Thread.setDefaultUncaughtExceptionHandler(uncaughtException);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (intent != null) {
            mUserOption = intent.getStringExtra(Constants.USER_OPTION);
            isApConnected = intent.getBooleanExtra(Constants.IS_AP_CONNECTED, false);
        }

        mKeyReceiver = new KeyReceiver(onKeyReceiver);
        IntentFilter keyFilter = new IntentFilter();
        keyFilter.addAction(KeyReceiver.ACTION_KEY_DOWN);
        keyFilter.addAction(KeyReceiver.ACTION_KEY_UP);
        registerReceiver(mKeyReceiver, keyFilter);
    }

    @Override
    protected void onPause() {
        if (!isClosed && isAutoClose) {
            close();
        }
        unregisterReceiver(mKeyReceiver);

        super.onPause();
    }

    public void setKeyCallback(KeyCallback keyCallback) {
        mKeyCallback = keyCallback;
    }

    /**
     * Auto close setting
     *
     * @param autoClose true : auto close / false : not auto close
     */
    public void setAutoClose(boolean autoClose) {
        isAutoClose = autoClose;
    }

    /**
     * End processing
     */
    public void close() {
        isClosed = true;
        if (isCamera) {
            notificationCameraOpen();
        }
        notificationSuccess();
    }

    public String getUserOption() {
        return mUserOption;
    }

    public boolean isApConnected() {
        return isApConnected;
    }

    public void notificationCameraOpen() {
        isCamera = false;
        sendBroadcast(new Intent(Constants.ACTION_MAIN_CAMERA_OPEN));
    }

    public void notificationCameraClose() {
        isCamera = true;
        sendBroadcast(new Intent(Constants.ACTION_MAIN_CAMERA_CLOSE));
    }

    /**
     * Sound of normal capture
     */
    public void notificationAudioShutter() {
        sendBroadcast(new Intent(Constants.ACTION_AUDIO_SHUTTER));
    }

    /**
     * Sound of starting long exposure capture
     */
    public void notificationAudioOpen() {
        if (ThetaModel.isVCameraModel()) {
            sendBroadcast(new Intent(Constants.ACTION_AUDIO_SH_OPEN));
        } else {
            sendBroadcast(new Intent(Constants.ACTION_AUDIO_SHUTTER_OPEN));
        }
    }

    /**
     * Sound of ending long exposure capture
     */
    public void notificationAudioClose() {
        if (ThetaModel.isVCameraModel()) {
            sendBroadcast(new Intent(Constants.ACTION_AUDIO_SH_CLOSE));
        } else {
            sendBroadcast(new Intent(Constants.ACTION_AUDIO_SHUTTER_CLOSE));
        }
    }

    /**
     * Sound of starting movie recording
     */
    public void notificationAudioMovStart() {
        if (ThetaModel.isVCameraModel()) {
            sendBroadcast(new Intent(Constants.ACTION_AUDIO_MOVSTART));
        } else {
            sendBroadcast(new Intent(Constants.ACTION_AUDIO_MOVIE_START));
        }
    }

    /**
     * Sound of stopping movie recording
     */
    public void notificationAudioMovStop() {
        if (ThetaModel.isVCameraModel()) {
            sendBroadcast(new Intent(Constants.ACTION_AUDIO_MOVSTOP));
        } else {
            sendBroadcast(new Intent(Constants.ACTION_AUDIO_MOVIE_STOP));
        }
    }

    /**
     * Sound of working self-timer
     */
    public void notificationAudioSelf() {
        sendBroadcast(new Intent(Constants.ACTION_AUDIO_SELF));
    }

    /**
     * Sound of warning
     */
    public void notificationAudioWarning() {
        sendBroadcast(new Intent(Constants.ACTION_AUDIO_WARNING));
    }

    /**
     * Brightness of display
     *
     * @param brightness brightness 0-100(percent)
     */
    public void notificationScreenBrightnessSet(@NonNull int brightness) {
        if (brightness < 0) {
            brightness = 0;
        }
        if (brightness > 100) {
            brightness = 100;
        }

        Intent intent = new Intent(Constants.ACTION_SCREEN_BRIGHTNESS_SET);
        intent.putExtra(Constants.BRIGHTNESS, brightness);
        sendBroadcast(intent);
    }

    /**
     * Turn on LED3 with color
     *
     * @param ledColor target LED
     */
    public void notificationLed3Show(@NonNull LedColor ledColor) {
        Intent intent = new Intent(Constants.ACTION_LED_SHOW);
        intent.putExtra(Constants.TARGET, LedTarget.LED3.toString());
        intent.putExtra(Constants.COLOR, ledColor.toString());
        sendBroadcast(intent);
    }

    /**
     * Turn on LED
     *
     * @param ledTarget target LED
     */
    public void notificationLedShow(@NonNull LedTarget ledTarget) {
        if (ledTarget == LedTarget.LED3) {
            notificationLed3Show(LedColor.BLUE);
        } else {
            Intent intent = new Intent(Constants.ACTION_LED_SHOW);
            intent.putExtra(Constants.TARGET, ledTarget.toString());
            sendBroadcast(intent);
        }
    }

    /**
     * Blink LED
     *
     * @param ledTarget target LED
     * @param ledColor color
     * @param period period 250-2000 (msec)
     */
    public void notificationLedBlink(@NonNull LedTarget ledTarget, LedColor ledColor, int period) {
        if (ledColor == null) {
            ledColor = LedColor.BLUE;
        }
        if (period < 250) {
            period = 250;
        }
        if (period > 2000) {
            period = 2000;
        }

        Intent intent = new Intent(Constants.ACTION_LED_BLINK);
        intent.putExtra(Constants.TARGET, ledTarget.toString());
        intent.putExtra(Constants.COLOR, ledColor.toString());
        intent.putExtra(Constants.PERIOD, period);
        sendBroadcast(intent);
    }

    /**
     * Turn off LED
     *
     * @param ledTarget target LED
     */
    public void notificationLedHide(@NonNull LedTarget ledTarget) {
        Intent intent = new Intent(Constants.ACTION_LED_HIDE);
        intent.putExtra(Constants.TARGET, ledTarget.toString());
        sendBroadcast(intent);
    }

    /**
     * Set LED or OLED brightness
     *
     * @param ledTarget target LED or OLED
     * @param brightness brightness 0-100 (percent)
     */
    public void notificationLedBrightnessSet(@NonNull LedTarget ledTarget, int brightness) {
        if (brightness < 0) {
            brightness = 0;
        }
        if (brightness > 100) {
            brightness = 100;
        }

        Intent intent = new Intent(Constants.ACTION_LED_BRIGHTNESS_SET);
        intent.putExtra(Constants.TARGET, ledTarget.toString());
        intent.putExtra(Constants.BRIGHTNESS, brightness);
        sendBroadcast(intent);
    }

    /**
     * Display bitmap on OLED
     *
     * @param bitmap Bitmap displayed on OLED (Bitmap size is height 24 or 36 and width 128)
     */
    public void notificationOledImageShow(@NonNull Bitmap bitmap) {
        if ((bitmap.getHeight() == 24 || bitmap.getHeight() == 36)
                && bitmap.getWidth() == 128) {
            Intent intent = new Intent(Constants.ACTION_OLED_IMAGE_SHOW);
            intent.putExtra(Constants.BITMAP, bitmap);
            sendBroadcast(intent);
        }
    }

    /**
     * Blink bitmap on OLED
     *
     * @param bitmap Bitmap displayed on OLED (Bitmap size is height 24 or 36 and width 128)
     * @param period period 250-2000 (msec)
     */
    public void notificationOledImageBlink(@NonNull Bitmap bitmap, int period) {
        if ((bitmap.getHeight() == 24 || bitmap.getHeight() == 36)
                && bitmap.getWidth() == 128) {
            if (period < 250) {
                period = 250;
            }
            if (period > 2000) {
                period = 2000;
            }

            Intent intent = new Intent(Constants.ACTION_OLED_IMAGE_BLINK);
            intent.putExtra(Constants.BITMAP, bitmap);
            intent.putExtra(Constants.PERIOD, period);
            sendBroadcast(intent);
        }
    }

    /**
     * Displays a string in the specified area
     *
     * @param textMap Specify TextArea for Key and string to be displayed for Value
     */
    public void notificationOledTextShow(@NonNull Map<TextArea, String> textMap) {
        Intent intent = new Intent(Constants.ACTION_OLED_TEXT_SHOW);
        for (Map.Entry<TextArea, String> map : textMap.entrySet()) {
            intent.putExtra(map.getKey().toString(), map.getValue());
        }
        sendBroadcast(intent);
    }

    /**
     * Turn off OLED
     */
    public void notificationOledHide() {
        sendBroadcast(new Intent(Constants.ACTION_OLED_HIDE));
    }

    /**
     * Setting the OLED display during plug-in mode
     *
     * @param oledDisplay Plug-in display or basic display
     */
    public void notificationOledDisplaySet(@NonNull OledDisplay oledDisplay) {
        Intent intent = new Intent(Constants.ACTION_OLED_DISPLAY_SET);
        intent.putExtra(Constants.DISPLAY, oledDisplay.toString());
        sendBroadcast(intent);
    }

    public void notificationWlanOff() {
        sendBroadcast(new Intent(Constants.ACTION_WLAN_OFF));
    }

    public void notificationWlanAp() {
        sendBroadcast(new Intent(Constants.ACTION_WLAN_AP));
    }

    public void notificationWlanCl() {
        sendBroadcast(new Intent(Constants.ACTION_WLAN_CL));
    }

    /**
     * Updating the Database in X Models
     */
    public void notificationDatabaseUpdate(@NonNull String target) {
        Intent intent = new Intent(Constants.ACTION_DATABASE_UPDATE);
        intent.putExtra(Constants.TARGETS, target);
        sendBroadcast(intent);
    }

    /**
     * Updating the Database in V/Z1 Models
     */
    public void notificationDatabaseUpdate(@NonNull String[] targets) {
        Intent intent = new Intent(Constants.ACTION_DATABASE_UPDATE);
        intent.putExtra(Constants.TARGETS, targets);
        sendBroadcast(intent);
    }

    /**
     * Start camera attitude control sensor
     */
    public void notificationSensorStart() {
        sendBroadcast(new Intent(Constants.ACTION_MOTION_SENSOR_START));
    }

    /**
     * Stop the camera attitude control sensor
     */
    public void notificationSensorStop() {
        sendBroadcast(new Intent(Constants.ACTION_MOTION_SENSOR_STOP));
    }

    /**
     * Start of camera control by WebApi
     */
    public void notificationWebApiCameraOpen() {
        sendBroadcast(new Intent(Constants.ACTION_PLUGIN_WEBAPI_CAMERA_OPEN));
    }

    /**
     * Stop of camera control by WebApi
     */
    public void notificationWebApiCameraClose() {
        sendBroadcast(new Intent(Constants.ACTION_PLUGIN_WEBAPI_CAMERA_CLOSE));
    }

    /**
     * Notifying Completion of Plug-in when the plug-in ends normally
     */
    public void notificationSuccess() {
        Intent intent = new Intent(Constants.ACTION_FINISH_PLUGIN);
        intent.putExtra(Constants.PACKAGE_NAME, getPackageName());
        intent.putExtra(Constants.EXIT_STATUS, ExitStatus.SUCCESS.toString());
        sendBroadcast(intent);

        finishAndRemoveTask();
    }

    /**
     * Notifying Completion of Plug-in when the plug-in ends with error
     *
     * @param message error message
     */
    public void notificationError(String message) {
        Intent intent = new Intent(Constants.ACTION_FINISH_PLUGIN);
        intent.putExtra(Constants.PACKAGE_NAME, getPackageName());
        intent.putExtra(Constants.EXIT_STATUS, ExitStatus.FAILURE.toString());
        intent.putExtra(Constants.MESSAGE, message);
        sendBroadcast(intent);

        finishAndRemoveTask();
    }

    /**
     * Notifying Occurrences of Errors
     */
    public void notificationErrorOccured() {
        sendBroadcast(new Intent(Constants.ACTION_ERROR_OCCURED));
    }
}
