package com.theta360.pluginlibrary.factory;

import android.content.Context;
import android.view.Surface;

import java.io.IOException;

public abstract class MediaRecorder {
    public abstract void newMediaRecorder();
    public abstract void setCamera(Camera camera);
    public abstract void setMediaRecorderContext(Context context);
    public abstract void setVideoSource(int videoSource);
    public abstract void setAudioSource(int audioSource);
    public abstract void setMicDeviceId(int micDeviceId);
    public abstract void setMicGain(int micGain);
    public abstract void setMicSamplingFormat(int micSamplingFormat);
    public abstract void setAudioChannels(int numChannels);
    public abstract void setAudioSamplingRate(int audioSamplingRate);
    public abstract void setAudioEncodingBitRate(int bitRate);
    public abstract void setOutputFormat(int format);
    public abstract void setVideoSize(int var1, int var2);
    public abstract void setVideoEncoder(int videoEncoder);
    public abstract void setVideoEncodingProfileLevel(int profile, int level);
    public abstract void setProfile(android.media.CamcorderProfile profile);
    public abstract void setProfile(theta360.media.CamcorderProfile profile);
    public abstract void setVideoEncodingIFrameInterval(float interval);
    public abstract void setVideoEncodingBitRate(int videoBitRate);
    public abstract void setVideoFrameRate(int videoFrameRate);
    public abstract void setMaxDuration(int maxDuration);
    public abstract void setMaxFileSize(Long maxFileSize);
    public abstract void setOutputFile(String videoWavFile);
    public abstract void setPreviewDisplay(Surface surface);
    public abstract void setOnErrorListener(OnErrorListener errorListener);
    public abstract void setOnInfoListener(OnInfoListener infoListener);
    public abstract int getExternalDeviceId();

    public abstract void prepare() throws IOException;
    public abstract void start();
    public abstract void stop();
    public abstract void reset();
    public abstract void release();

    public interface OnErrorListener {
        void onError(android.media.MediaRecorder mediaRecorder, int what, int extra);
        void onError(theta360.media.MediaRecorder mediaRecorder, int what, int extra);
    }
    public interface OnInfoListener {
        void onInfo(android.media.MediaRecorder mediaRecorder, int what, int extra);
        void onInfo(theta360.media.MediaRecorder mediaRecorder, int what, int extra);
    }
}
