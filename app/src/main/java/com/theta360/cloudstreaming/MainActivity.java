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
import android.graphics.Color;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import com.github.faucamp.simplertmp.origin.SequenceNumberStorage;
import com.github.faucamp.simplertmp.origin.CheckIsStreamVideo;
import com.pedro.rtplibrary.view.OpenGlView;
import com.pedro.rtplibrary.rtmp.RtmpCamera1;
import com.theta360.cloudstreaming.Util.LogUtilDebugTree;
import com.theta360.cloudstreaming.camera.CameraPreview;
import com.theta360.cloudstreaming.httpserver.AndroidWebServer;
import com.theta360.cloudstreaming.httpserver.Theta360SQLiteOpenHelper;
import com.theta360.cloudstreaming.receiver.LiveStreamingReceiver;
import com.theta360.cloudstreaming.settingdata.Bitrate;
import com.theta360.cloudstreaming.settingdata.SettingData;
import com.theta360.cloudstreaming.settingdata.StatusType;
import com.theta360.pluginlibrary.activity.PluginActivity;
import com.theta360.pluginlibrary.activity.ThetaInfo;
import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.receiver.KeyReceiver;
import com.theta360.pluginlibrary.values.LedColor;
import com.theta360.pluginlibrary.values.LedTarget;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.pedro.rtmp.utils.ConnectCheckerRtmp;
import com.theta360.pluginlibrary.values.TextArea;
import com.theta360.pluginlibrary.values.ThetaModel;

import timber.log.Timber;

import static android.view.KeyEvent.KEYCODE_POWER;
import static com.theta360.cloudstreaming.httpserver.AndroidWebServer.PRIMARY_KEY_ID;
import static com.theta360.cloudstreaming.httpserver.AndroidWebServer.decodeStreamName;
import static com.theta360.cloudstreaming.httpserver.AndroidWebServer.encodeStreamName;
import static com.theta360.pluginlibrary.values.ThetaModel.isZ1Model;
import static java.lang.Math.pow;

public class MainActivity extends PluginActivity implements ConnectCheckerRtmp {

    private final int BYTE_TO_BIT = 8;
    private final double BIT_TO_MBIT = 1 / pow(10, 6);
    private final long CONNECTION_FAILED_INTERVAL_MSEC = 2000;
    private final int LOG_DELETE_ELAPSED_DAYS = 30;
    private final String LABELS_MOVIE_SIZE_4K = "4K(3840x2160) 30fps";
    private final String LABELS_MOVIE_SIZE_4K_15FPS = "4K(3840x2160) 15fps";
    private final String LABELS_MOVIE_SIZE_2K = "2K(1920x1080) 30fps";
    private final String LABELS_MOVIE_SIZE_2K_15FPS = "2K(1920x1080) 15fps";
    private final String LABELS_MOVIE_SIZE_1K = "1K(1024x576) 30fps";
    private final String LABELS_MOVIE_SIZE_1K_15FPS = "1K(1024x576) 15fps";
    private final String LABELS_MOVIE_SIZE_06K = "0.6K(640x320) 30fps";
    private final String LABELS_BITRATE_4K_40 = "40Mbps";
    private final String LABELS_BITRATE_4K_20 = "20Mbps";
    private final String LABELS_BITRATE_4K_12 = "12Mbps";
    private final String LABELS_BITRATE_4K_10 = "10Mbps";
    private final String LABELS_BITRATE_4K_6 = "6Mbps";
    private final String LABELS_BITRATE_2K_16 = "16Mbps";
    private final String LABELS_BITRATE_2K_8 = "8Mbps";
    private final String LABELS_BITRATE_2K_6 = "6Mbps";
    private final String LABELS_BITRATE_2K_3 = "3Mbps";
    private final String LABELS_BITRATE_2K_1_5 = "1.5Mbps";
    private final String LABELS_BITRATE_1K_2 = "2Mbps";
    private final String LABELS_BITRATE_1K_1 = "1Mbps";
    private final String LABELS_BITRATE_1K_085 = "0.85Mbps";
    private final String LABELS_BITRATE_1K_05 = "0.5Mbps";
    private final String LABELS_BITRATE_1K_042 = "0.42Mbps";
    private final String LABELS_BITRATE_1K_025 = "0.25Mbps";
    private final String LABELS_BITRATE_06K_1 = "1Mbps";
    private final String LABELS_BITRATE_06K_036 = "0.36Mbps";
    private final String LABELS_BITRATE_06K_025 = "0.25Mbps";
    private final String LABELS_SAMPLING_RATE_48000 = "48.0KHz";
    private final String LABELS_SAMPLING_RATE_44100 = "44.1KHz";

    private RtmpCamera1  mRtmpCamera1;

    private AudioManager am;
    private TextView status;
    private TextView statusText;
    private Button liveButton;
    private Spinner movieSize;
    private Spinner bitrate;
    private ArrayAdapter adapter2;
    private Spinner samplingRate;
    private EditText etUrl;
    private EditText streamName;
    private int lcdBrightness = 0;
    private int ledPowerBrightness = 0;
    private int ledStatusBrightness = 0;
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
    private Boolean spinnerSelected = false;
    private Boolean isConnectionSuccess = false;

    private static Object lock;
    private Handler handler;

    private int noOperationTimeoutMSec = AndroidWebServer.TIMEOUT_DEFAULT_MINUTE * 60 * 1000;

    private SQLiteDatabase dbObject;

    private long lastConnectionFailedErrorTime = 0;

    private String[] itemMovieSizeTable = null;
    private final String[] itemMovieSize_V = {LABELS_MOVIE_SIZE_4K, LABELS_MOVIE_SIZE_2K, LABELS_MOVIE_SIZE_1K, LABELS_MOVIE_SIZE_06K};
    private final String[] itemMovieSize_X = {LABELS_MOVIE_SIZE_4K, LABELS_MOVIE_SIZE_2K, LABELS_MOVIE_SIZE_1K};
    private final String[] itemMovieSize_X_v102000 = {LABELS_MOVIE_SIZE_4K, LABELS_MOVIE_SIZE_4K_15FPS, LABELS_MOVIE_SIZE_2K, LABELS_MOVIE_SIZE_2K_15FPS, LABELS_MOVIE_SIZE_1K, LABELS_MOVIE_SIZE_1K_15FPS};
    private final String[] itemSamplingRate = {LABELS_SAMPLING_RATE_48000, LABELS_SAMPLING_RATE_44100};

    private LiveStreamingReceiver mLiveStreamingReceiver;
    private LiveStreamingReceiver.Callback onLiveStreamingReceiver = new LiveStreamingReceiver.Callback() {
        @Override
        public void callStreamingCallback() {
            if (scheduleStreaming != null)
                scheduleStreaming.setSchedule();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.BLACK);

        // Exclusion control
        lock = new Object();
        handler = new Handler();

        con = getApplicationContext();

        if(ThetaModel.isVCameraModel()) {
            setAutoClose(false);
        }

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
        streamName = findViewById(R.id.et_stream_key);

        // Initialize LED
        if (ThetaModel.isVCameraModel()) {
            if (isZ1Model()) {
                notificationLedHide(LedTarget.LED4); // カメラ
            }
        }
        changeStartupLED();

        // Update the status of THETA
        Theta360SQLiteOpenHelper hlpr = new Theta360SQLiteOpenHelper(con);
        dbObject = hlpr.getWritableDatabase();
        updateStatus(StatusType.RUNNING);

        if(ThetaModel.isVCameraModel()) {
            cameraPreview = new CameraPreview();
        }
        status = findViewById(R.id.live_status);
        statusText = findViewById(R.id.status_text);
        liveButton = findViewById(R.id.live_button);
        movieSize = findViewById(R.id.txtwidth);
        bitrate = findViewById(R.id.txtbitrate);
        samplingRate = findViewById(R.id.textAudioSamplingRate);
        if(ThetaModel.isVCameraModel()) {
            itemMovieSizeTable = itemMovieSize_V;
        } else {
            String version = ThetaInfo.getThetaFirmwareVersion(con);
            if (version.compareTo("1.20.0") >= 0) {
                itemMovieSizeTable = itemMovieSize_X_v102000;
            } else {
                itemMovieSizeTable = itemMovieSize_X;
            }
        }
        ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(this, R.layout.spinner_item, itemMovieSizeTable);
        adapter1.setDropDownViewResource(R.layout.spinner_dropdown_item);
        movieSize.setAdapter(adapter1);

        ArrayAdapter<String> adapter3 = new ArrayAdapter<String>(this, R.layout.spinner_item, itemSamplingRate);
        adapter3.setDropDownViewResource(R.layout.spinner_dropdown_item);
        samplingRate.setAdapter(adapter3);
        movieSize.setSelection(1);

        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//        boolean micMute = am.isMicrophoneMute();
//        Timber.d("micMute = " + micMute);

        notificationCameraClose();
        mRtmpCamera1 = new RtmpCamera1(openGlView, this);

        findViewById(R.id.live_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (textInputCheck()) {
                    setSettingData();
                    if (scheduleStreaming != null)
                        scheduleStreaming.setSchedule();
                }
            }
        });

        movieSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!spinnerSelected) {
                    setBitrateSpinner(position);
                }
                spinnerSelected = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // set callback for pressing buttons
        setKeyCallback(new KeyCallback() {
            @Override
            public void onKeyLongPress(int keyCode, KeyEvent event) {
                Timber.i("onKeyLongPress");

                if (keyCode == KeyReceiver.KEYCODE_MEDIA_RECORD) {
                    if (ThetaModel.isVCameraModel()) {
                        exitProcess();
                    } else {
                        if (mRtmpCamera1.isStreaming()) {
                            if (scheduleStreaming != null) {
                                scheduleStreaming.setSchedule();

                                // 配信終了スレッド実行後にCloseするため遅延を入れる
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        close();
                                    }
                                }, 500);
                            }
                        } else {
                            setSettingData();
                            close();
                        }
                    }
                }
            }

            @Override
            public void onKeyDown(int keyCode, KeyEvent event) {
                Timber.i("onKeyDown");
                if (keyCode == KeyReceiver.KEYCODE_CAMERA) {
                    if (textInputCheck()) {
                        setSettingData();
                        if (scheduleStreaming != null)
                            scheduleStreaming.setSchedule();
                    }
                } else if (keyCode == KEYCODE_POWER) {
                    if (mRtmpCamera1 != null && mRtmpCamera1.isStreaming()) {
                        if (scheduleStreaming != null)
                            scheduleStreaming.setSchedule();
                    }
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
                Timber.i("Rtmp サーバーへ 接続成功しました。");
                notificationAudioMovStart();
                status.setTextColor(getColor(R.color.colorStop));
                status.setText("●");
                statusText.setText(getResources().getString(R.string.message_live_streaming));
                liveButton.setBackgroundResource(R.drawable.shape_rounded_corners_stop);
                liveButton.setText(getResources().getString(R.string.stop_live_streaming));
                etUrl.setEnabled(false);
                streamName.setEnabled(false);
                movieSize.setEnabled(false);
                bitrate.setEnabled(false);
                samplingRate.setEnabled(false);
                updateStatus(StatusType.LIVE_STREAMING);
                if(!ThetaModel.isVCameraModel()) {
                    lcdBrightness = mRtmpCamera1.getLcdBrightness();
                    mRtmpCamera1.ctrlBrightness(1);
                    // ledId 3 = BLUE
                    ledPowerBrightness = mRtmpCamera1.getLedPowerBrightness(3);
                    ledStatusBrightness = mRtmpCamera1.getLedStatusBrightness(3);
                    mRtmpCamera1.ctrlLedPowerBrightness(3, 1);
                    mRtmpCamera1.ctrlLedStatusBrightness(3, 1);
                }
                isConnectionSuccess = true;
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
                Timber.e("Connection to Rtmp server is failed." + reason);
                shutDownTimer.reset(false, noOperationTimeoutMSec);

                status.setTextColor(getColor(R.color.colorStop));
                status.setText("!");
                statusText.setText(getResources().getString(R.string.message_error_connect_server));
                updateStatus(StatusType.ERROR_CONNECT_SERVER);
                notificationAudioWarning();

                if (System.currentTimeMillis() - lastConnectionFailedErrorTime > CONNECTION_FAILED_INTERVAL_MSEC) {
                    lastConnectionFailedErrorTime = System.currentTimeMillis();
                    playPPPSoundWithErrorLED();
                }
                settingPolling.changeStart();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (scheduleStreaming != null) {
                            scheduleStreaming.setSchedule();
                        }
                    }
                }, 300);

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
                Timber.i("Rtmpサーバーから切断しました。");
                if (isConnectionSuccess) {
                    notificationAudioMovStop();
                    status.setTextColor(getColor(R.color.colorStart));
                    status.setText("●");
                    statusText.setText(getResources().getString(R.string.message_stop_streaming));
                    liveButton.setBackgroundResource(R.drawable.shape_rounded_corners_start);
                    liveButton.setText(getResources().getString(R.string.start_live_streaming));
                    etUrl.setEnabled(true);
                    streamName.setEnabled(true);
                    movieSize.setEnabled(true);
                    bitrate.setEnabled(true);
                    samplingRate.setEnabled(true);
                    if(!ThetaModel.isVCameraModel()) {
                        mRtmpCamera1.ctrlBrightness(lcdBrightness);
                        // ledId 3 = BLUE
                        mRtmpCamera1.ctrlLedPowerBrightness(3, ledPowerBrightness);
                        mRtmpCamera1.ctrlLedStatusBrightness(3, ledStatusBrightness);
                    }
                    isConnectionSuccess = false;
                } else {
                    updateStatus(StatusType.ERROR_CONNECT_SERVER);
                }
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
                Timber.e("Authentication failed.");
                shutDownTimer.reset(false, noOperationTimeoutMSec);
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mLiveStreamingReceiver);
        if (mRtmpCamera1 != null && mRtmpCamera1.isStreaming()) {
            mRtmpCamera1.stopStream();
            shutDownTimer.reset(false, noOperationTimeoutMSec);
            mRtmpCamera1.stopCamera();
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

        if (settingData.getStreamName().isEmpty() || settingData.getServerUrl().isEmpty()) {
            // No settings
            updateStatus(StatusType.ERROR_NOT_USER_SETTING);
            playPPPSoundWithErrorLED();
            settingPolling.changeStart();
            return;
        }

        if (!mRtmpCamera1.isStreaming()) {
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
            double bitRate = Double.parseDouble(settingData.getBitRate());

            int audioSamplingRate = settingData.getAudioSamplingRate();

            // Check video and audio preparation
            if (mRtmpCamera1.prepareAudio(128 * 1024,audioSamplingRate, false, false, false)
                    && mRtmpCamera1.prepareVideo(movieW, movieH, (int)settingData.getFps(), (int)(bitRate * 1000000),2,0) ) {

                // Start streaming
                CheckIsStreamVideo.init();
                mRtmpCamera1.startStream(settingData.getServerUrl() + "/" + settingData.getStreamName());

                if (ThetaModel.isVCameraModel()) {
                    cameraPreview.start(openGlView.getSurfaceTexture());
                }

                if (mRtmpCamera1.isStreaming()) {
                    changeStreamingLED();
                } else {
                    // Illegal server URL
                    if (ThetaModel.isVCameraModel()){
                        cameraPreview.stop();
                    }
                }
            } else {
                // Any hardware issues
                status.setTextColor(getColor(R.color.colorStop));
                status.setText("!");
                statusText.setText(getResources().getString(R.string.message_error_initialization));
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
        mRtmpCamera1.stopStream();
        if (ThetaModel.isVCameraModel()) {
            cameraPreview.stop();
        }
        mRtmpCamera1.stop();
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
        if (ThetaModel.isVCameraModel()) {
            if (isZ1Model()) {
                notificationOledHide();
                Map<TextArea, String> textMap = new HashMap<>();
                textMap.put(TextArea.MIDDLE, getString(R.string.oled_middle));
                textMap.put(TextArea.BOTTOM, "");
                notificationOledTextShow(textMap);
            } else {
                notificationLedHide(LedTarget.LED5); // Video
                notificationLedHide(LedTarget.LED6); // LIVE
                notificationLedHide(LedTarget.LED7); // Recording
                notificationLedHide(LedTarget.LED8); // Error
            }
        }
    }

    private void changeReadyLED() {
        if (ThetaModel.isVCameraModel()) {
            if (isZ1Model()) {
                Map<TextArea, String> textMap = new HashMap<>();
                textMap.put(TextArea.MIDDLE, getString(R.string.oled_middle));
                textMap.put(TextArea.BOTTOM, getString(R.string.oled_bottom_ready));
                notificationOledTextShow(textMap);
            } else {
                notificationLedShow(LedTarget.LED5); // Video
                notificationLedShow(LedTarget.LED6); // LIVE
                notificationLedHide(LedTarget.LED7); // Recording
                notificationLedHide(LedTarget.LED8); // Error
            }
        }
    }

    private void changeStreamingLED() {
        if (ThetaModel.isVCameraModel()) {
            if (isZ1Model()) {
                Map<TextArea, String> textMap = new HashMap<>();
                textMap.put(TextArea.MIDDLE, getString(R.string.oled_middle));
                textMap.put(TextArea.BOTTOM, getString(R.string.oled_bottom_streaming));
                notificationOledTextShow(textMap);
            } else {
                notificationLedShow(LedTarget.LED5); // Video
                notificationLedShow(LedTarget.LED6); // LIVE
                notificationLedBlink(LedTarget.LED7, LedColor.BLUE, 1000); // Recording
                notificationLedHide(LedTarget.LED8); // Error
            }
        }
    }

    private void changeDelayStreamingLED() {
        if (ThetaModel.isVCameraModel()) {
            if (isZ1Model()) {
                Map<TextArea, String> textMap = new HashMap<>();
                textMap.put(TextArea.MIDDLE, getString(R.string.oled_middle));
                textMap.put(TextArea.BOTTOM, getString(R.string.oled_bottom_streaming));
                notificationOledTextShow(textMap);
            } else {
                notificationLedShow(LedTarget.LED5); // Video
                notificationLedShow(LedTarget.LED6); // LIVE
                notificationLedBlink(LedTarget.LED7, LedColor.BLUE, 1000); // Recording
                notificationLedBlink(LedTarget.LED8, LedColor.BLUE, 1000); // Error
            }
        }
    }

    private void changeErrorLED() {
        notificationLedShow(LedTarget.LED5); // Video
        notificationLedShow(LedTarget.LED6); // LIVE
        notificationLedHide(LedTarget.LED7); // Recording
        notificationLedBlink(LedTarget.LED8, LedColor.BLUE, 1000); // Error
    }

    private void changeEndLED() {
        if (ThetaModel.isVCameraModel()) {
            if (isZ1Model()) {
                notificationOledHide();
            } else {
                notificationLedHide(LedTarget.LED5); // Video
                notificationLedHide(LedTarget.LED6); // LIVE
                notificationLedHide(LedTarget.LED7); // Recording
                notificationLedHide(LedTarget.LED8); // Error
            }
        }
    }

    /**
     * PPP(Error) sound playback and error LED control
     */
    private void playPPPSoundWithErrorLED() {
        if (ThetaModel.isVCameraModel()) {
            if (isZ1Model()) {
                notificationErrorOccured();
            } else {
                notificationAudioWarning();
                changeErrorLED();
            }
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
        private String measureServerUrl;
        private String measureStreamName;
        private int measureWidth = 0;
        private int measureHeight = 0;
        private boolean isExit = false;

        // Schedule streaming
        public void setSchedule() {
            isScheduleStreaming = true;
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
                                settingData.setAudioSamplingRate(cursor.getInt(cursor.getColumnIndex("audio_sampling_rate")));
                                settingData.setNoOperationTimeoutMinute(cursor.getInt(cursor.getColumnIndex("no_operation_timeout_minute")));
                                settingData.setStatus(cursor.getString(cursor.getColumnIndex("status")));
                            } else {
                                // Create new recode if DB is empty.
                                ContentValues values = new ContentValues();

                                values.put("id", PRIMARY_KEY_ID);

                                // 4k
                                values.put("movie_width", Bitrate.MOVIE_WIDTH_2K);
                                values.put("movie_height", Bitrate.MOVIE_HEIGHT_2K);
                                values.put("bitrate", Bitrate.BITRATE_2K_DEFAULT);
                                values.put("auto_bitrate", "");
                                values.put("fps", Bitrate.FPS_2K_30);

                                values.put("server_url", "");
                                values.put("stream_name", "");
                                values.put("crypt_text", "");
                                values.put("audio_sampling_rate", AndroidWebServer.DEFAULT_AUDIO_SAMPLING_RATE);
                                values.put("no_operation_timeout_minute", AndroidWebServer.TIMEOUT_DEFAULT_MINUTE);

                                long num = dbObject.insert("theta360_setting", null, values);
                                if (num != 1) {
                                    throw new SQLiteException("[setting data] initialize database error");
                                }
                                cursor = dbObject.query("theta360_setting", null, "id=?", new String[]{String.valueOf(PRIMARY_KEY_ID)}, null, null, null, null);
                                if (cursor.moveToNext()) {
                                    settingData = new SettingData();
                                    settingData.setServerUrl(
                                            cursor.getString(cursor.getColumnIndex("server_url")));
                                    settingData.setStreamName(
                                            cursor.getString(cursor.getColumnIndex("stream_name")));
                                    settingData.setCryptText(
                                            cursor.getString(cursor.getColumnIndex("crypt_text")));
                                    settingData.setMovieWidth(
                                            cursor.getInt(cursor.getColumnIndex("movie_width")));
                                    settingData.setMovieHeight(
                                            cursor.getInt(cursor.getColumnIndex("movie_height")));
                                    settingData
                                            .setFps(cursor.getDouble(cursor.getColumnIndex("fps")));
                                    settingData.setBitRate(
                                            cursor.getString(cursor.getColumnIndex("bitrate")));
                                    settingData.setAudioSamplingRate(cursor.getInt(
                                            cursor.getColumnIndex("audio_sampling_rate")));
                                    settingData.setNoOperationTimeoutMinute(cursor.getInt(
                                            cursor.getColumnIndex("no_operation_timeout_minute")));
                                    settingData.setStatus(
                                            cursor.getString(cursor.getColumnIndex("status")));
                                }
                            }

                            if (settingData != null) {

                                // Keep holding minutes in milliseconds
                                noOperationTimeoutMSec = settingData.getNoOperationTimeoutMinute() * 60 * 1000;

                                // When there is setting
                                if (settingData != null) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            // Read and adapt the user's settings.
                                            etUrl.setText(settingData.getServerUrl());
                                            streamName.setText(settingData.getStreamName());
                                            spinnerSelected = true;

                                            String version = ThetaInfo.getThetaFirmwareVersion(con);
                                            if ((!ThetaModel.isVCameraModel()) && (version.compareTo("1.20.0") >= 0)) {
                                                if (settingData.getMovieWidth() == 3840 && settingData.getMovieHeight() == 2160 && settingData.getFps() == 30) {
                                                    movieSize.setSelection(0);
                                                    setBitrateSpinner(0);
                                                    if (settingData.getBitRate().equals("40")) {
                                                        bitrate.setSelection(0);
                                                    } else if (settingData.getBitRate().equals("20")) {
                                                        bitrate.setSelection(1);
                                                    } else if (settingData.getBitRate().equals("12")) {
                                                        bitrate.setSelection(2);
                                                    }
                                                } else if (settingData.getMovieWidth() == 3840 && settingData.getMovieHeight() == 2160 && settingData.getFps() == 15) {
                                                    movieSize.setSelection(1);
                                                    setBitrateSpinner(1);
                                                    if (settingData.getBitRate().equals("20")){
                                                        bitrate.setSelection(0);
                                                    } else if (settingData.getBitRate().equals("10")) {
                                                        bitrate.setSelection(1);
                                                    } else if (settingData.getBitRate().equals("6")) {
                                                        bitrate.setSelection(2);
                                                    }
                                                } else if (settingData.getMovieWidth() == 1920 && settingData.getMovieHeight() == 1080 && settingData.getFps() == 30) {
                                                    movieSize.setSelection(2);
                                                    setBitrateSpinner(2);
                                                    if (settingData.getBitRate().equals("16")){
                                                        bitrate.setSelection(0);
                                                    } else if (settingData.getBitRate().equals("6")) {
                                                        bitrate.setSelection(1);
                                                    } else if (settingData.getBitRate().equals("3")) {
                                                        bitrate.setSelection(2);
                                                    }
                                                } else if (settingData.getMovieWidth() == 1920 && settingData.getMovieHeight() == 1080 && settingData.getFps() == 15) {
                                                    movieSize.setSelection(3);
                                                    setBitrateSpinner(3);
                                                    if (settingData.getBitRate().equals("8")){
                                                        bitrate.setSelection(0);
                                                    } else if (settingData.getBitRate().equals("3")) {
                                                        bitrate.setSelection(1);
                                                    } else if (settingData.getBitRate().equals("1.5")) {
                                                        bitrate.setSelection(2);
                                                    }
                                                } else if (settingData.getMovieWidth() == 1024 && settingData.getMovieHeight() == 576 && settingData.getFps() == 30) {
                                                    movieSize.setSelection(4);
                                                    setBitrateSpinner(4);
                                                    if (settingData.getBitRate().equals("2")){
                                                        bitrate.setSelection(0);
                                                    } else if (settingData.getBitRate().equals("0.85")) {
                                                        bitrate.setSelection(1);
                                                    } else if (settingData.getBitRate().equals("0.5")) {
                                                        bitrate.setSelection(2);
                                                    }
                                                } else if (settingData.getMovieWidth() == 1024 && settingData.getMovieHeight() == 576 && settingData.getFps() == 15) {
                                                    movieSize.setSelection(5);
                                                    setBitrateSpinner(5);
                                                    if (settingData.getBitRate().equals("1")){
                                                        bitrate.setSelection(0);
                                                    } else if (settingData.getBitRate().equals("0.42")) {
                                                        bitrate.setSelection(1);
                                                    } else if (settingData.getBitRate().equals("0.25")) {
                                                        bitrate.setSelection(2);
                                                    }
                                                }
                                            } else {
                                                if (settingData.getMovieWidth() == 3840 && settingData.getMovieHeight() == 2160) {
                                                    movieSize.setSelection(0);
                                                    setBitrateSpinner(0);
                                                    if (settingData.getBitRate().equals("40")){
                                                        bitrate.setSelection(0);
                                                    } else if (settingData.getBitRate().equals("20")) {
                                                        bitrate.setSelection(1);
                                                    } else if (settingData.getBitRate().equals("12")) {
                                                        bitrate.setSelection(2);
                                                    }
                                                } else if (settingData.getMovieWidth() == 1920 && settingData.getMovieHeight() == 1080) {
                                                    movieSize.setSelection(1);
                                                    setBitrateSpinner(1);
                                                    if (settingData.getBitRate().equals("16")){
                                                        bitrate.setSelection(0);
                                                    } else if (settingData.getBitRate().equals("6")) {
                                                        bitrate.setSelection(1);
                                                    } else if (settingData.getBitRate().equals("3")) {
                                                        bitrate.setSelection(2);
                                                    }
                                                } else if (settingData.getMovieWidth() == 1024 && settingData.getMovieHeight() == 576) {
                                                    movieSize.setSelection(2);
                                                    setBitrateSpinner(2);
                                                    if (settingData.getBitRate().equals("2")){
                                                        bitrate.setSelection(0);
                                                    } else if (settingData.getBitRate().equals("0.85")) {
                                                        bitrate.setSelection(1);
                                                    } else if (settingData.getBitRate().equals("0.5")) {
                                                        bitrate.setSelection(2);
                                                    }
                                                }
                                            }

                                            if (settingData.getAudioSamplingRate() == 48000) {
                                                samplingRate.setSelection(0);
                                            } else if (settingData.getAudioSamplingRate() == 44100) {
                                                samplingRate.setSelection(1);
                                            }
                                        }
                                    });

                                    if (settingData.getStreamName().isEmpty() || settingData.getServerUrl().isEmpty()) {
                                        changeStartupLED();
                                    } else {
                                        changeReadyLED();
                                    }
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
                            mRtmpCamera1.stopCamera();
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
                    if (mRtmpCamera1.isStreaming()) {
                        double elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
                        long sentByteSize = SequenceNumberStorage.calculateByte();
                        double delaySecond = elapsedTime - sentByteSize / byterate;
                        Timber.i("elapsedTime is " + String.valueOf(elapsedTime) + " at DelayJudgment");
                        Timber.i("sentByteSize is " + String.valueOf(sentByteSize) + " at DelayJudgment");
                        Timber.i("DelaySecond is " + String.valueOf(delaySecond) + " at DelayJudgment");

                        if (delaySecond > DELAY_JUDGEMENT_THRESHOLD) {
                            // 遅延
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
        if (isZ1Model()) {
            Map<TextArea, String> textMap = new HashMap<>();
            textMap.put(TextArea.MIDDLE, getString(R.string.oled_middle));
            switch(status) {
                case ERROR_CONNECT_SERVER:
                    textMap.put(TextArea.BOTTOM, getString(R.string.oled_bottom_error_connection));
                    notificationOledTextShow(textMap);
                    break;
                case ERROR_NOT_USER_SETTING:
                    textMap.put(TextArea.BOTTOM, getString(R.string.oled_bottom_error_setting));
                    notificationOledTextShow(textMap);
                    break;
                case TIMEOUT:
                    textMap.put(TextArea.BOTTOM, getString(R.string.oled_bottom_error_timeout));
                    notificationOledTextShow(textMap);
                    break;
                case ERROR_INITIALIZATION:
                    textMap.put(TextArea.BOTTOM, getString(R.string.oled_bottom_error_initialize));
                    notificationOledTextShow(textMap);
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
        if (isZ1Model()) {
            Map<TextArea, String> textMap = new HashMap<>();
            textMap.put(TextArea.MIDDLE, getString(R.string.oled_middle));
            textMap.put(TextArea.BOTTOM, getString(R.string.oled_bottom_error_unexpected) + errorCode);
            notificationOledTextShow(textMap);
        }
        playPPPSoundWithErrorLED();
    }

    private void setBitrateSpinner (int position) {
        List<String> items = new ArrayList<>();
        String label = itemMovieSizeTable[position];
        switch (label) {
            case LABELS_MOVIE_SIZE_4K:
                items.add(LABELS_BITRATE_4K_40);
                items.add(LABELS_BITRATE_4K_20);
                items.add(LABELS_BITRATE_4K_12);
                break;
            case LABELS_MOVIE_SIZE_4K_15FPS:
                items.add(LABELS_BITRATE_4K_20);
                items.add(LABELS_BITRATE_4K_10);
                items.add(LABELS_BITRATE_4K_6);
                break;
            case LABELS_MOVIE_SIZE_2K:
                items.add(LABELS_BITRATE_2K_16);
                items.add(LABELS_BITRATE_2K_6);
                items.add(LABELS_BITRATE_2K_3);
                break;
            case LABELS_MOVIE_SIZE_2K_15FPS:
                items.add(LABELS_BITRATE_2K_8);
                items.add(LABELS_BITRATE_2K_3);
                items.add(LABELS_BITRATE_2K_1_5);
                break;
            case LABELS_MOVIE_SIZE_1K:
                items.add(LABELS_BITRATE_1K_2);
                items.add(LABELS_BITRATE_1K_085);
                items.add(LABELS_BITRATE_1K_05);
                break;
            case LABELS_MOVIE_SIZE_1K_15FPS:
                items.add(LABELS_BITRATE_1K_1);
                items.add(LABELS_BITRATE_1K_042);
                items.add(LABELS_BITRATE_1K_025);
                break;
            case LABELS_MOVIE_SIZE_06K:
                items.add(LABELS_BITRATE_06K_1);
                items.add(LABELS_BITRATE_06K_036);
                items.add(LABELS_BITRATE_06K_025);
                break;
            default:
                break;
        }
        adapter2 = new ArrayAdapter(this, R.layout.spinner_item, items);
        adapter2.setDropDownViewResource(R.layout.spinner_dropdown_item);
        bitrate.setAdapter(adapter2);
        bitrate.setSelection(1);
    }

    private boolean textInputCheck() {
        if(etUrl.getText().toString().isEmpty() || streamName.getText().toString().isEmpty()) {
            status.setTextColor(getColor(R.color.colorStop));
            status.setText("!");
            statusText.setText(getResources().getString(R.string.message_error_not_user_setting));
            updateStatus(StatusType.ERROR_NOT_USER_SETTING);
            notificationAudioWarning();
            playPPPSoundWithErrorLED();
            settingPolling.changeStart();
            return false;
        } else {
            return true;
        }
    }

    private void setSettingData() {
        ContentValues values = new ContentValues();
        settingData.setServerUrl(etUrl.getText().toString());
        values.put("server_url", etUrl.getText().toString());
        String cryptText = encodeStreamName(streamName.getText().toString());
        settingData.setCryptText(cryptText);
        values.put("crypt_text", cryptText);
        String streamKey = decodeStreamName(cryptText);
        settingData.setStreamName(streamKey);
        values.put("stream_name",streamKey);
        switch (movieSize.getSelectedItem().toString()) {
            case LABELS_MOVIE_SIZE_4K:
                values.put("movie_width", Bitrate.MOVIE_WIDTH_4K);
                values.put("movie_height", Bitrate.MOVIE_HEIGHT_4K);
                values.put("fps", Bitrate.FPS_4K_30);
                settingData.setMovieWidth(Bitrate.MOVIE_WIDTH_4K);
                settingData.setMovieHeight(Bitrate.MOVIE_HEIGHT_4K);
                settingData.setFps(Bitrate.FPS_4K_30);
                if(bitrate.getSelectedItem().toString().equals(LABELS_BITRATE_4K_40)){
                    values.put("bitrate", Bitrate.BITRATE_4K_40);
                    settingData.setBitRate(Bitrate.BITRATE_4K_40);
                } else if(bitrate.getSelectedItem().toString().equals(LABELS_BITRATE_4K_20)) {
                    values.put("bitrate", Bitrate.BITRATE_4K_20);
                    settingData.setBitRate(Bitrate.BITRATE_4K_20);
                } else if(bitrate.getSelectedItem().toString().equals(LABELS_BITRATE_4K_12)){
                    values.put("bitrate", Bitrate.BITRATE_4K_12);
                    settingData.setBitRate(Bitrate.BITRATE_4K_12);
                }
                break;
            case LABELS_MOVIE_SIZE_4K_15FPS:
                values.put("movie_width", Bitrate.MOVIE_WIDTH_4K);
                values.put("movie_height", Bitrate.MOVIE_HEIGHT_4K);
                values.put("fps", Bitrate.FPS_4K_15);
                settingData.setFps(Bitrate.FPS_4K_15);
                settingData.setMovieWidth(Bitrate.MOVIE_WIDTH_4K);
                settingData.setMovieHeight(Bitrate.MOVIE_HEIGHT_4K);
                if(bitrate.getSelectedItem().toString().equals(LABELS_BITRATE_4K_20)){
                    values.put("bitrate", Bitrate.BITRATE_4K_20);
                    settingData.setBitRate(Bitrate.BITRATE_4K_20);
                } else if(bitrate.getSelectedItem().toString().equals(LABELS_BITRATE_4K_10)) {
                    values.put("bitrate", Bitrate.BITRATE_4K_10);
                    settingData.setBitRate(Bitrate.BITRATE_4K_10);
                } else if(bitrate.getSelectedItem().toString().equals(LABELS_BITRATE_4K_6)){
                    values.put("bitrate", Bitrate.BITRATE_4K_6);
                    settingData.setBitRate(Bitrate.BITRATE_4K_6);
                }
                break;
            case LABELS_MOVIE_SIZE_2K:
                values.put("movie_width", Bitrate.MOVIE_WIDTH_2K);
                values.put("movie_height", Bitrate.MOVIE_HEIGHT_2K);
                values.put("fps", Bitrate.FPS_2K_30);
                settingData.setMovieWidth(Bitrate.MOVIE_WIDTH_2K);
                settingData.setMovieHeight(Bitrate.MOVIE_HEIGHT_2K);
                settingData.setFps(Bitrate.FPS_2K_30);
                if(bitrate.getSelectedItem().toString().equals(LABELS_BITRATE_2K_16)){
                    values.put("bitrate", Bitrate.BITRATE_2K_16);
                    settingData.setBitRate(Bitrate.BITRATE_2K_16);
                } else if(bitrate.getSelectedItem().toString().equals(LABELS_BITRATE_2K_6)) {
                    values.put("bitrate", Bitrate.BITRATE_2K_6);
                    settingData.setBitRate(Bitrate.BITRATE_2K_6);
                } else if(bitrate.getSelectedItem().toString().equals(LABELS_BITRATE_2K_3)){
                    values.put("bitrate", Bitrate.BITRATE_2K_3);
                    settingData.setBitRate(Bitrate.BITRATE_2K_3);
                }
                break;
            case LABELS_MOVIE_SIZE_2K_15FPS:
                values.put("movie_width", Bitrate.MOVIE_WIDTH_2K);
                values.put("movie_height", Bitrate.MOVIE_HEIGHT_2K);
                values.put("fps", Bitrate.FPS_2K_15);
                settingData.setFps(Bitrate.FPS_2K_15);
                settingData.setMovieWidth(Bitrate.MOVIE_WIDTH_2K);
                settingData.setMovieHeight(Bitrate.MOVIE_HEIGHT_2K);
                if(bitrate.getSelectedItem().toString().equals(LABELS_BITRATE_2K_8)){
                    values.put("bitrate", Bitrate.BITRATE_2K_8);
                    settingData.setBitRate(Bitrate.BITRATE_2K_8);
                } else if(bitrate.getSelectedItem().toString().equals(LABELS_BITRATE_2K_3)) {
                    values.put("bitrate", Bitrate.BITRATE_2K_3);
                    settingData.setBitRate(Bitrate.BITRATE_2K_3);
                } else if(bitrate.getSelectedItem().toString().equals(LABELS_BITRATE_2K_1_5)){
                    values.put("bitrate", Bitrate.BITRATE_2K_1_5);
                    settingData.setBitRate(Bitrate.BITRATE_2K_1_5);
                }
                break;
            case LABELS_MOVIE_SIZE_1K:
                values.put("movie_width", Bitrate.MOVIE_WIDTH_1K);
                values.put("movie_height", Bitrate.MOVIE_HEIGHT_1K);
                values.put("fps", Bitrate.FPS_1K_30);
                settingData.setMovieWidth(Bitrate.MOVIE_WIDTH_1K);
                settingData.setMovieHeight(Bitrate.MOVIE_HEIGHT_1K);
                settingData.setFps(Bitrate.FPS_1K_30);
                if(bitrate.getSelectedItem().toString().equals(LABELS_BITRATE_1K_2)){
                    values.put("bitrate", Bitrate.BITRATE_1K_2);
                    settingData.setBitRate(Bitrate.BITRATE_1K_2);
                } else if(bitrate.getSelectedItem().toString().equals(LABELS_BITRATE_1K_085)) {
                    values.put("bitrate", Bitrate.BITRATE_1K_085);
                    settingData.setBitRate(Bitrate.BITRATE_1K_085);
                } else if(bitrate.getSelectedItem().toString().equals(LABELS_BITRATE_1K_05)){
                    values.put("bitrate", Bitrate.BITRATE_1K_05);
                    settingData.setBitRate(Bitrate.BITRATE_1K_05);
                }
                break;
            case LABELS_MOVIE_SIZE_1K_15FPS:
                values.put("movie_width", Bitrate.MOVIE_WIDTH_1K);
                values.put("movie_height", Bitrate.MOVIE_HEIGHT_1K);
                values.put("fps", Bitrate.FPS_1K_15);
                settingData.setFps(Bitrate.FPS_1K_15);
                settingData.setMovieWidth(Bitrate.MOVIE_WIDTH_1K);
                settingData.setMovieHeight(Bitrate.MOVIE_HEIGHT_1K);
                if(bitrate.getSelectedItem().toString().equals(LABELS_BITRATE_1K_1)){
                    values.put("bitrate", Bitrate.BITRATE_1K_1);
                    settingData.setBitRate(Bitrate.BITRATE_1K_1);
                } else if(bitrate.getSelectedItem().toString().equals(LABELS_BITRATE_1K_042)) {
                    values.put("bitrate", Bitrate.BITRATE_1K_042);
                    settingData.setBitRate(Bitrate.BITRATE_1K_042);
                } else if(bitrate.getSelectedItem().toString().equals(LABELS_BITRATE_1K_025)){
                    values.put("bitrate", Bitrate.BITRATE_1K_025);
                    settingData.setBitRate(Bitrate.BITRATE_1K_025);
                }
                break;
            default:
                break;
        }

        if (samplingRate.getSelectedItem().toString().equals(LABELS_SAMPLING_RATE_48000)) {
            values.put("audio_sampling_rate", 48000);
            settingData.setAudioSamplingRate(48000);
        } else if (samplingRate.getSelectedItem().toString().equals(LABELS_SAMPLING_RATE_44100)) {
            values.put("audio_sampling_rate", 44100);
            settingData.setAudioSamplingRate(44100);
        }
        dbObject.update("theta360_setting", values, "id=?", new String[]{String.valueOf(PRIMARY_KEY_ID)});
    }

    @Override
    public void onNewBitrateRtmp(long bitrate){return;}

    @Override
    public void onConnectionStartedRtmp(String str){return;}

}
