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

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.github.faucamp.simplertmp.origin.SequenceNumberStorage;
import com.github.faucamp.simplertmp.origin.CheckIsStreamVideo;
import com.pedro.rtplibrary.view.OpenGlView;
import com.theta360.cloudstreaming.Extend.RtmpExtend;
import com.theta360.cloudstreaming.Util.LogUtilDebugTree;
import com.theta360.cloudstreaming.camera.CameraPreview;
import com.theta360.cloudstreaming.httpserver.AndroidWebServer;
import com.theta360.cloudstreaming.httpserver.Theta360SQLiteOpenHelper;
import com.theta360.cloudstreaming.receiver.LiveStreamingReceiver;
import com.theta360.cloudstreaming.receiver.MeasureBitrateReceiver;
import com.theta360.cloudstreaming.settingdata.Bitrate;
import com.theta360.cloudstreaming.settingdata.SettingData;
import com.theta360.cloudstreaming.settingdata.StatusType;
import com.theta360.pluginlibrary.activity.PluginActivity;
import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.receiver.KeyReceiver;
import com.theta360.pluginlibrary.values.LedColor;
import com.theta360.pluginlibrary.values.LedTarget;
import java.io.File;
import java.io.FileFilter;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.ossrs.rtmp.ConnectCheckerRtmp;
import timber.log.Timber;

import static com.theta360.cloudstreaming.httpserver.AndroidWebServer.PRIMARY_KEY_ID;
import static java.lang.Math.pow;

public class MainActivity extends PluginActivity implements ConnectCheckerRtmp {

    private final int BYTE_TO_BIT = 8;
    private final double BIT_TO_MBIT = 1 / pow(10, 6);
    private final long CONNECTION_FAILED_INTERVAL_MSEC = 2000;
    private final int LOG_DELETE_ELAPSED_DAYS = 30;

    private RtmpExtend rtmpExtend;

    private AudioManager am;
    private EditText etUrl;
    private Context con;
    private OpenGlView openGlView;
    private final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private SettingData settingData;
    private ShutDownTimer shutDownTimer;
    private SettingPolling settingPolling;
    private ScheduleStreaming scheduleStreaming;
    private DelayJudgment delayJudgment;
    private ExecutorService shutDownTimerService = null;
    private ExecutorService settingPollingService = null;
    private ExecutorService scheduleStreamingService = null;
    private ExecutorService delayJudgmentService = null;
    private AndroidWebServer webUI;
    private Boolean settingFix;
    private CameraPreview cameraPreview;

    private static Object lock;
    private Handler handler;

    private int noOperationTimeoutMSec = AndroidWebServer.TIMEOUT_DEFAULT_MINUTE * 60 * 1000;

    private SQLiteDatabase dbObject;

    private long lastConnectionFailedErrorTime = 0;

    private LiveStreamingReceiver mLiveStreamingReceiver;
    private LiveStreamingReceiver.Callback onLiveStreamingReceiver = new LiveStreamingReceiver.Callback() {
        @Override
        public void callStreamingCallback() {
            if (scheduleStreaming != null)
                scheduleStreaming.setSchedule();
        }
    };

    private MeasureBitrateReceiver mMeasureBitrateReceiver;
    private MeasureBitrateReceiver.Callback onMeasureBitrateReceiver = new MeasureBitrateReceiver.Callback() {
        @Override
        public void callMeasureBitrateCallback(String serverUrl, String streamName, int width, int height) {
            if (scheduleStreaming != null)
                scheduleStreaming.setMeasureBitrate(serverUrl, streamName, width, height);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Exclusion control
        lock = new Object();
        handler = new Handler();

        con = getApplicationContext();

        // Initializa log file
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        long currentTimeMillis = System.currentTimeMillis();
        String logFileDirPath = con.getFilesDir().getAbsolutePath();
        String logFileName = String.format("app_%s.log", dateFormat.format(new Date(currentTimeMillis)));
        Timber.plant(new LogUtilDebugTree(logFileDirPath, logFileName));
        // Log Header
        Timber.i("\n\n*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*\n"
            + "*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*\n"
            + "\n    logging start ... " + df.format(new Date(System.currentTimeMillis()))
            + "\n\n");

        // Delete log after LOG_DELETE_ELAPSED_DAYS days
        long logDeleteElapsedMillis = currentTimeMillis - LOG_DELETE_ELAPSED_DAYS * (1000L * 60L * 60L * 24L);
        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (!(file.getName().matches("app_\\d{8}.log"))) {
                    return false;
                }
                return file.lastModified() <= logDeleteElapsedMillis;
            }
        };
        for (File file : new File(logFileDirPath).listFiles(fileFilter)) {
            file.delete();
        }

        // Launch web server
        webUI = new AndroidWebServer(con);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        openGlView = findViewById(R.id.surfaceView);
        etUrl = findViewById(R.id.et_rtp_url);

        // Initialize LED
        if (isZ1()) {
            notificationLedHide(LedTarget.LED4); // camera
        }
        changeStartupLED();

        // Update the status of THETA
        Theta360SQLiteOpenHelper hlpr = new Theta360SQLiteOpenHelper(con);
        dbObject = hlpr.getWritableDatabase();
        updateStatus(StatusType.RUNNING);

        rtmpExtend = new RtmpExtend(openGlView, this);
        cameraPreview = new CameraPreview();

        // Forbid editing EditText
        etUrl.setEnabled(false);
        ((EditText) findViewById(R.id.txtwidth)).setEnabled(false);
        ((EditText) findViewById(R.id.txtheight)).setEnabled(false);
        ((EditText) findViewById(R.id.txtframe)).setEnabled(false);
        ((EditText) findViewById(R.id.txtbitrate)).setEnabled(false);
        ((Switch) findViewById(R.id.swUsePreview)).setChecked(true);

        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//        boolean micMute = am.isMicrophoneMute();
//        Timber.d("micMute = " + micMute);

        // set callback for pressing buttons
        setKeyCallback(new KeyCallback() {
            @Override
            public void onKeyLongPress(int keyCode, KeyEvent event) {
                Timber.i("onKeyLongPress");

                if (keyCode == KeyReceiver.KEYCODE_MEDIA_RECORD) {
                    exitProcess();
                }
            }

            @Override
            public void onKeyDown(int keyCode, KeyEvent event) {
                Timber.i("onKeyDown");

                if (keyCode == KeyReceiver.KEYCODE_CAMERA) {
                    if (scheduleStreaming != null)
                        scheduleStreaming.setSchedule();
                }
            }

            @Override
            public void onKeyUp(int keyCode, KeyEvent event) {
                Timber.i("onKeyUp");
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        // set to WLAN（CL-mode）
        notificationWlanCl();

        mLiveStreamingReceiver = new LiveStreamingReceiver(onLiveStreamingReceiver);
        IntentFilter liveStreamingFilter = new IntentFilter();
        liveStreamingFilter.addAction(LiveStreamingReceiver.TOGGLE_LIVE_STREAMING);
        registerReceiver(mLiveStreamingReceiver, liveStreamingFilter);

        mMeasureBitrateReceiver = new MeasureBitrateReceiver(onMeasureBitrateReceiver);
        IntentFilter measureBitrateFilter = new IntentFilter();
        measureBitrateFilter.addAction(MeasureBitrateReceiver.MEASURE_BITRATE);
        registerReceiver(mMeasureBitrateReceiver, measureBitrateFilter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        am.setParameters("RicUseBFormat=false");

        createShutDownTimer();
        createSettingPolling();
        createScheduleStreaming();
    }

    /**
     * {@inheritDoc}
     * Called when the connection to Rtmp server is succeeded
     */
    @Override
    public void onConnectionSuccessRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Connection success", Toast.LENGTH_SHORT).show();
                Timber.i("Rtmp サーバーへ 接続成功しました。");
                shutDownTimer.reset(true, noOperationTimeoutMSec);
            }
        });
    }

    /**
     * {@inheritDoc}
     * Called when the connection to Rtmp server is NOT succeeded
     *
     * @param reason IOException Message
     */
    @Override
    public void onConnectionFailedRtmp(final String reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Connection failed. " + reason, Toast.LENGTH_SHORT).show();
                Timber.e("Connection to Rtmp server is failed." + reason);
                shutDownTimer.reset(false, noOperationTimeoutMSec);
                stopStreaming();

                updateStatus(StatusType.ERROR_CONNECT_SERVER);
                if (System.currentTimeMillis() - lastConnectionFailedErrorTime > CONNECTION_FAILED_INTERVAL_MSEC) {
                    lastConnectionFailedErrorTime = System.currentTimeMillis();
                    playPPPSoundWithErrorLED();
                }
                settingPolling.changeStart();
            }
        });
    }

    /**
     * {@inheritDoc}
     * Called when disconnected from Rtmp server
     */
    @Override
    public void onDisconnectRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                Timber.i("Rtmpサーバーから切断しました。");
                shutDownTimer.reset(false, noOperationTimeoutMSec);
            }
        });
    }

    /**
     * {@inheritDoc}
     * Called when Auth authentication to the Rtmp server is successful
     * Note: this is not used in Youtube
     */
    @Override
    public void onAuthSuccessRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Auth success", Toast.LENGTH_SHORT).show();
                Timber.i("Authenticated.");
                shutDownTimer.reset(true, noOperationTimeoutMSec);
            }
        });
    }

    /**
     * {@inheritDoc}
     * Called when Auth authentication to the Rtmp server fails
     * Note: this is not used in Youtube
     */
    @Override
    public void onAuthErrorRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
                Timber.e("Authentication failed.");
                shutDownTimer.reset(false, noOperationTimeoutMSec);
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mLiveStreamingReceiver);
        unregisterReceiver(mMeasureBitrateReceiver);
        if (rtmpExtend != null && rtmpExtend.isStreaming()) {
            rtmpExtend.stopStream();
            cameraPreview.stop();
            shutDownTimer.reset(false, noOperationTimeoutMSec);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        am.setParameters("RicUseBFormat=true");

        if (shutDownTimer != null) {
            shutDownTimer.exit();
        }
        if (settingPolling != null) {
            settingPolling.exit();
        }
        if (scheduleStreaming != null) {
            scheduleStreaming.exit();
        }
        if (delayJudgmentService != null) {
            delayJudgmentService.shutdownNow();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webUI.destroy();
        notificationCameraOpen();
        notificationSuccess();
    }

    /**
     * Call the shooting application when the distribution application ends.
     */
    @SuppressLint("WrongConstant")
    private void callRecordingApp() {
        con.sendBroadcastAsUser(new Intent("com.theta360.devicelibrary.receiver.ACTION_BOOT_BASIC"), android.os.Process.myUserHandle());
    }

    private void streaming() {
        // The setting could not be acquired right after startup
        if (settingData == null) {
            return;
        }

        // MEMO: The state that the distribution is stopped or is not being streamed, is used as the status before distribution.
        if (!rtmpExtend.isStreaming()) {
            startStreaming();
            startDelayJudgment();
        } else {
            // Stop when streaming button is pushed during streaming.
            stopStreaming();
            updateStatus(StatusType.STOP_STREAMING);
            changeReadyLED();
            settingPolling.changeStart();
        }
    }

    private void startStreaming() {
        // Interrupt configuration polling
        settingPolling.changeStop();

        notificationCameraClose();

        // Initialization before delivery
        changeReadyLED();

        // Check network status
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo nInfo = cm.getActiveNetworkInfo();

        // If the network is unusable, error handling
        if (nInfo == null || !nInfo.isConnected()) {
            Timber.i("Network connection error.");
            playPPPSoundWithErrorLED();
            settingPolling.changeStart();
            return;
        }

        synchronized (lock) {
            // Confirm user's setting
            if (!settingFix) {
                // No settings
                updateStatus(StatusType.ERROR_NOT_USER_SETTING);
                playPPPSoundWithErrorLED();
                settingPolling.changeStart();
                return;
            }
            int movieW = settingData.getMovieWidth();
            int movieH = settingData.getMovieHeight();
            double bitReate = Double.parseDouble(settingData.getBitRate());

            // When the bit rate is AUTO, use the measured bit rate
            if (bitReate == -1) {
                bitReate = Double.parseDouble(settingData.getAutoBitRate());
            }

            // Pass each parameter to RTMP module
            HashMap streamingParamMap = new HashMap();
            streamingParamMap.put("width", String.valueOf(movieW));
            streamingParamMap.put("height", String.valueOf(movieH));
            streamingParamMap.put("fps", String.valueOf((int) settingData.getFps()));
            streamingParamMap.put("bitrate", String.valueOf((int)(bitReate * 1000000)));

            Timber.i("streaming settings: " + streamingParamMap.toString());

            rtmpExtend.setStreamingParamMap(streamingParamMap);

            // Check video and audio preparation
            if (rtmpExtend.prepareAudio() && rtmpExtend.prepareVideo()) {

                // Start streaming
                CheckIsStreamVideo.init();
                rtmpExtend.startStream(settingData.getServerUrl() + "/" + settingData.getStreamName());

                if (((Switch) findViewById(R.id.swUsePreview)).isChecked()) {
                    cameraPreview.start(openGlView.getSurfaceTexture());
                } else {
                    Timber.d("Can not be started except in the preview mode.");
                }
                if (rtmpExtend.isStreaming()) {
                    changeStreamingLED();
                    updateStatus(StatusType.LIVE_STREAMING);
                } else {
                    // Illegal server URL
                    cameraPreview.stop();
                }
            } else {
                // Any hardware issues
                updateStatus(StatusType.ERROR_INITIALIZATION);
                playPPPSoundWithErrorLED();
                Timber.e("Failed to initialize audio or camera.");
            }
        }
    }

    synchronized private void stopStreaming() {
        if (delayJudgmentService != null) {
            delayJudgmentService.shutdownNow();
        }
        rtmpExtend.stopStream();
        cameraPreview.stop();
    }

    private void exitProcess() {
        Timber.i("Application was terminated.");

        changeEndLED();

        // Launch the shooting application (com.theta 360.receptor).
        callRecordingApp();

        // Quit this plug-in itself
        finishAndRemoveTask();
    }

    private void changeStartupLED() {
        if (isZ1()) {
            notificationOledHide();
            notificationOledTextShow(getString(R.string.oled_middle), "");
        } else {
            notificationLedHide(LedTarget.LED5); // Video
            notificationLedHide(LedTarget.LED6); // LIVE
            notificationLedHide(LedTarget.LED7); // Recording
            notificationLedHide(LedTarget.LED8); // Error
        }
    }

    private void changeReadyLED() {
        if (isZ1()) {
            notificationOledTextShow(getString(R.string.oled_middle), getString(R.string.oled_bottom_ready));
        } else {
            notificationLedShow(LedTarget.LED5); // Video
            notificationLedShow(LedTarget.LED6); // LIVE
            notificationLedHide(LedTarget.LED7); // Recording
            notificationLedHide(LedTarget.LED8); // Error
        }
    }

    private void changeStreamingLED() {
        if (isZ1()) {
            notificationOledTextShow(getString(R.string.oled_middle), getString(R.string.oled_bottom_streaming));
        } else {
            notificationLedShow(LedTarget.LED5); // Video
            notificationLedShow(LedTarget.LED6); // LIVE
            notificationLedBlink(LedTarget.LED7, LedColor.BLUE, 1000); // Recording
            notificationLedHide(LedTarget.LED8); // Error
        }
    }

    private void changeDelayStreamingLED() {
        if (isZ1()) {
            notificationOledTextShow(getString(R.string.oled_middle), getString(R.string.oled_bottom_streaming));
        } else {
            notificationLedShow(LedTarget.LED5); // Video
            notificationLedShow(LedTarget.LED6); // LIVE
            notificationLedBlink(LedTarget.LED7, LedColor.BLUE, 1000); // Recording
            notificationLedBlink(LedTarget.LED8, LedColor.BLUE, 1000); // Error
        }
    }

    private void changeErrorLED() {
        notificationLedShow(LedTarget.LED5); // Video
        notificationLedShow(LedTarget.LED6); // LIVE
        notificationLedHide(LedTarget.LED7); // Recording
        notificationLedBlink(LedTarget.LED8, LedColor.BLUE, 1000); // Error
    }

    private void changeEndLED() {
        if (isZ1()) {
            notificationOledHide();
        } else {
            notificationLedHide(LedTarget.LED5); // Video
            notificationLedHide(LedTarget.LED6); // LIVE
            notificationLedHide(LedTarget.LED7); // Recording
            notificationLedHide(LedTarget.LED8); // Error
        }
    }

    /**
     * PPP(Error) sound playback and error LED control
     */
    private void playPPPSoundWithErrorLED() {
        if (isZ1()) {
            notificationErrorOccured();
        } else {
            notificationAudioWarning();
            changeErrorLED();
        }
    }

    /**
     * Create an end monitoring timer
     */
    private void createShutDownTimer() {
        shutDownTimer = new ShutDownTimer(new ShutDownTimerCallBack() {
            @Override
            public void callBack() {
                updateStatus(StatusType.TIMEOUT);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                exitProcess();
            }
        }, noOperationTimeoutMSec);

        try {
            shutDownTimerService = Executors.newSingleThreadExecutor();
            shutDownTimerService.execute(shutDownTimer);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            shutDownTimerService.shutdown();
        }
    }

    private void createScheduleStreaming() {
        // Create setting monitoring class
        scheduleStreaming = new ScheduleStreaming();

        try {
            scheduleStreamingService = Executors.newSingleThreadExecutor();
            scheduleStreamingService.execute(scheduleStreaming);
        } catch (Exception e) {
            e.printStackTrace();
            unexpectedError("E002");
        } finally {
            scheduleStreamingService.shutdown();
        }
    }

    private class ScheduleStreaming implements Runnable {
        private boolean isScheduleStreaming = false;
        private boolean isMeasureBitrate = false;
        private String measureServerUrl;
        private String measureStreamName;
        private int measureWidth = 0;
        private int measureHeight = 0;
        private boolean isExit = false;

        // Schedule streaming
        public void setSchedule() {
            isScheduleStreaming = true;
        }

        // Schedule bit rate measurement
        public void setMeasureBitrate(String serverUrl, String streamName, int width, int height) {
            measureServerUrl = serverUrl;
            measureStreamName = streamName;
            measureWidth = width;
            measureHeight = height;
            isMeasureBitrate = true;
        }

        /**
         * End thread
         */
        public void exit() {
            isExit = true;
        }

        @Override
        public void run() {
            while(!isExit) {
                // Changing bit rate AUTO
                if (isMeasureBitrate) {
                    measureBitrate();
                }

                // Ignore streaming instructions during streaming preparation.
                if (!isScheduleStreaming) {
                    continue;
                }

                try {
                    streaming();
                } catch (Exception e) {
                    Timber.i("ScheduleStreaming : " + e.getMessage());
                } finally {
                    isScheduleStreaming = false;
                }
            }
        }

        private void measureBitrate() {
            int CHECK_TIME_SEC = 10;
            double THRESHOLD = 0.8;

            if (rtmpExtend.isStreaming()) {
                isMeasureBitrate = false;
                return;
            }

            double calculated_bitrate = 0;
            // Temporary evacuation of set information
            String tmpBitrate = settingData.getBitRate();
            String tmpServerUrl = settingData.getServerUrl();
            String tmpStreamName = settingData.getStreamName();
            int tmpWidth = settingData.getMovieWidth();
            int tmpHeight = settingData.getMovieHeight();
            Boolean tmpSettingFix = settingFix;
            settingData.setServerUrl(measureServerUrl);
            settingData.setStreamName(measureStreamName);
            Double maxBitrate = Double.parseDouble(Bitrate.getMaxBitrate(measureWidth));
            // Deliver at the set upper bit rate
            settingData.setBitRate(String.valueOf(maxBitrate));
            settingData.setMovieWidth(measureWidth);
            settingData.setMovieHeight(measureHeight);
            settingFix = !(measureServerUrl.equals("") || measureStreamName.equals(""));
            startStreaming();
            SequenceNumberStorage.initSequenceNumber();

            long startTimeMills = System.currentTimeMillis();
            while(rtmpExtend.isStreaming()) {
                if (System.currentTimeMillis() - startTimeMills < CHECK_TIME_SEC * 1000) {
                    continue;
                }
                long averageByteSize = SequenceNumberStorage.calculateByte() / CHECK_TIME_SEC;
                Timber.i("AverageByteSize is " + String.valueOf(averageByteSize));

                calculated_bitrate = averageByteSize * BYTE_TO_BIT * BIT_TO_MBIT * THRESHOLD;
                Timber.i("Calculated Bitrate is " + String.valueOf(calculated_bitrate));

                stopStreaming();
                changeReadyLED();
                updateStatus(StatusType.STOP_STREAMING);
                break;
            }
            settingData.setServerUrl(tmpServerUrl);
            settingData.setStreamName(tmpStreamName);
            settingData.setBitRate(tmpBitrate);
            settingData.setMovieWidth(tmpWidth);
            settingData.setMovieHeight(tmpHeight);
            settingFix = tmpSettingFix;

            // Decide bit rate
            String bitrate;
            if (calculated_bitrate < maxBitrate) {
                bitrate = String.format("%.2f", calculated_bitrate);
            } else {
                bitrate = String.format("%.2f", maxBitrate);
            }
            // Erase extra 0 of bit rate
            String bitrateLast = bitrate.substring(bitrate.length() - 1);
            while(bitrateLast.equals("0")) {
                bitrate = bitrate.substring(0, bitrate.length() - 1);
                bitrateLast = bitrate.substring(bitrate.length() - 1);
            }
            if (bitrateLast.equals(".")) {
                bitrate = bitrate.substring(0, bitrate.length() - 1);
            }
            // Pass the calculated bit rate to the screen
            webUI.setMeasuredBitrate(bitrate);

            isMeasureBitrate = false;
            isScheduleStreaming = false;
        }
    }

    /**
     * Create setting monitoring thread
     */
    private void createSettingPolling() {

        settingFix = false;

        // Create setting monitoring class
        settingPolling = new SettingPolling();

        try {
            settingPollingService = Executors.newSingleThreadExecutor();
            settingPollingService.execute(settingPolling);
        } catch (Exception e) {
            e.printStackTrace();
            unexpectedError("E003");
        } finally {
            settingPollingService.shutdown();
        }
    }


    /**
     * Inner class for monitoring setting status
     */
    private class SettingPolling implements Runnable {

        // Confirmation interval. Unit millisecond
        private static final long CHECK_INTERVAL_MSEC = 1000;
        private Boolean isExit;
        private Boolean isStop;

        public SettingPolling() {
            isExit = false;
            isStop = false;
        }

        /**
         * End thread
         */
        public void exit() {
            isExit = true;
        }


        /**
         * Start monitoring
         */
        public void changeStart() {
            isStop = false;
        }

        /**
         * Stop monitoring
         */
        public void changeStop() {
            isStop = true;
        }


        @Override
        public void run() {

            Boolean first = true;
            while (!isExit) {
                try {
                    Thread.sleep(CHECK_INTERVAL_MSEC);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }

                if (isStop) {
                    continue;
                }

                if (webUI.isRequested() || first) {
                    webUI.clearRequested();

                    if (first) {
                        first = false;
                    }

                    synchronized (lock) {
                        // Check setting
                        Theta360SQLiteOpenHelper hlpr = new Theta360SQLiteOpenHelper(con);
                        SQLiteDatabase dbObject = hlpr.getWritableDatabase();
                        Cursor cursor = dbObject.query("theta360_setting", null, "id=?", new String[]{String.valueOf(PRIMARY_KEY_ID)}, null, null, null, null);

                        try {
                            settingData = null;
                            if (cursor.moveToNext()) {
                                settingData = new SettingData();
                                settingData.setServerUrl(cursor.getString(cursor.getColumnIndex("server_url")));
                                settingData.setStreamName(cursor.getString(cursor.getColumnIndex("stream_name")));
                                settingData.setCryptText(cursor.getString(cursor.getColumnIndex("crypt_text")));
                                settingData.setMovieWidth(cursor.getInt(cursor.getColumnIndex("movie_width")));
                                settingData.setMovieHeight(cursor.getInt(cursor.getColumnIndex("movie_height")));
                                settingData.setFps(cursor.getDouble(cursor.getColumnIndex("fps")));
                                settingData.setBitRate(cursor.getString(cursor.getColumnIndex("bitrate")));
                                settingData.setAutoBitRate(cursor.getString(cursor.getColumnIndex("auto_bitrate")));
                                settingData.setNoOperationTimeoutMinute(cursor.getInt(cursor.getColumnIndex("no_operation_timeout_minute")));
                                settingData.setStatus(cursor.getString(cursor.getColumnIndex("status")));
                            } else {
                                // Create new recode if DB is empty.
                                ContentValues values = new ContentValues();

                                values.put("id", PRIMARY_KEY_ID);

                                // 4k
                                values.put("movie_width", Bitrate.MOVIE_WIDTH_4K);
                                values.put("movie_height", Bitrate.MOVIE_HEIGHT_4K);
                                values.put("bitrate", Bitrate.BITRATE_4K_DEFAULT);
                                values.put("auto_bitrate", "");
                                values.put("fps", Bitrate.FPS_4K);

                                values.put("server_url", "");
                                values.put("stream_name", "");
                                values.put("crypt_text", "");
                                values.put("no_operation_timeout_minute", AndroidWebServer.TIMEOUT_DEFAULT_MINUTE);

                                long num = dbObject.insert("theta360_setting", null, values);
                                if (num != 1) {
                                    throw new SQLiteException("[setting data] initialize database error");
                                }

                            }

                            if (settingData != null) {

                                // Keep holding minutes in milliseconds
                                noOperationTimeoutMSec = settingData.getNoOperationTimeoutMinute() * 60 * 1000;

                                // When there is setting
                                if (settingData != null && !settingData.getServerUrl().isEmpty() && !settingData.getStreamName().isEmpty()) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            // Read and adapt the user's settings.
                                            etUrl.setText(settingData.getServerUrl() + "/" + settingData.getStreamName());
                                            ((TextView) findViewById(R.id.txtwidth)).setText(String.valueOf(settingData.getMovieWidth()));
                                            ((TextView) findViewById(R.id.txtheight)).setText(String.valueOf(settingData.getMovieHeight()));
                                            ((TextView) findViewById(R.id.txtframe)).setText(String.valueOf((int) settingData.getFps()));
                                            ((TextView) findViewById(R.id.txtbitrate)).setText(String.valueOf(settingData.getBitRate()));
                                            ((TextView) findViewById(R.id.textNoOperationTimeoutMinute)).setText(String.valueOf(settingData.getNoOperationTimeoutMinute()));
                                        }
                                    });

                                    changeReadyLED();
                                    settingFix = true;
                                } else {
                                    changeStartupLED();
                                    settingFix = false;
                                }
                            }

                            // Reset auto stop timer
                            shutDownTimer.reset(false, noOperationTimeoutMSec);

                        } catch (Exception e) {
                            e.printStackTrace();
                            unexpectedError("E004");
                            throw new SQLiteException("[setting data] Unexpected exception");
                        } finally {
                            cursor.close();
                            dbObject.close();
                        }
                    }
                }
            }
        }
    }

    /**
     * Start "delay" measurement
     */
    private void startDelayJudgment() {
        // Create setting monitoring class
        delayJudgment = new DelayJudgment();

        try {
            delayJudgmentService = Executors.newSingleThreadExecutor();
            delayJudgmentService.execute(delayJudgment);
        } catch (Exception e) {
            e.printStackTrace();
            unexpectedError("E006");
        } finally {
            delayJudgmentService.shutdown();
        }
    }

    /**
     * Inner class for "delay" judgment
     */
    private class DelayJudgment implements Runnable {

        private static final int CHECK_TIME_SEC = 15;
        private final double DELAY_JUDGEMENT_THRESHOLD = 30.0;

        @Override
        public void run() {
            double byterate = Double.parseDouble(settingData.getBitRate()) / BIT_TO_MBIT / BYTE_TO_BIT;
            SequenceNumberStorage.initSequenceNumber();
            long startTime = System.currentTimeMillis();

            try {
                while (true) {
                    Thread.sleep(CHECK_TIME_SEC * 1000);
                    if (rtmpExtend.isStreaming()) {
                        double elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
                        long sentByteSize = SequenceNumberStorage.calculateByte();
                        double delaySecond = elapsedTime - sentByteSize / byterate;
                        Timber.i("elapsedTime is " + String.valueOf(elapsedTime) + " at DelayJudgment");
                        Timber.i("sentByteSize is " + String.valueOf(sentByteSize) + " at DelayJudgment");
                        Timber.i("DelaySecond is " + String.valueOf(delaySecond) + " at DelayJudgment");

                        if (delaySecond > DELAY_JUDGEMENT_THRESHOLD) {
                            Timber.i("Live Streaming is Delay");
                            changeDelayStreamingLED();
                        } else {
                            changeStreamingLED();
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Update of THETA state
     */
    private void updateStatus(StatusType status) {
        ContentValues values = new ContentValues();
        values.put("status", status.getCode());
        dbObject.update("theta360_setting", values, "id=?", new String[]{String.valueOf(PRIMARY_KEY_ID)});
        if (isZ1()) {
            switch(status) {
                case ERROR_CONNECT_SERVER:
                    notificationOledTextShow(getString(R.string.oled_middle), getString(R.string.oled_bottom_error_connection));
                    break;
                case ERROR_NOT_USER_SETTING:
                    notificationOledTextShow(getString(R.string.oled_middle), getString(R.string.oled_bottom_error_setting));
                    break;
                case TIMEOUT:
                    notificationOledTextShow(getString(R.string.oled_middle), getString(R.string.oled_bottom_error_timeout));
                    break;
                case ERROR_INITIALIZATION:
                    notificationOledTextShow(getString(R.string.oled_middle), getString(R.string.oled_bottom_error_initialize));
                    break;
            }
        }
    }

    /**
     * Unexpected Error
     */
    private void unexpectedError(String errorCode) {
        ContentValues values = new ContentValues();
        values.put("status", String.format("(%s)", errorCode));
        dbObject.update("theta360_setting", values, "id=?", new String[]{String.valueOf(PRIMARY_KEY_ID)});
        if (isZ1()) {
            notificationOledTextShow(getString(R.string.oled_middle), getString(R.string.oled_bottom_error_unexpected) + errorCode);
        }
        playPPPSoundWithErrorLED();
    }

}
