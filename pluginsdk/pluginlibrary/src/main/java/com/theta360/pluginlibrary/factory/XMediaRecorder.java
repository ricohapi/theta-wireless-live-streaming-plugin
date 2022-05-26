package com.theta360.pluginlibrary.factory;

import android.content.Context;
import android.media.CamcorderProfile;
import android.view.Surface;

import com.theta360.pluginlibrary.ThetaModelException;

import java.io.IOException;

public class XMediaRecorder extends MediaRecorder {
    private theta360.hardware.Camera mCamera;
    private theta360.media.MediaRecorder mMediaRecorder;
    private theta360.media.MediaRecorder.OnInfoListener infoListener;
    private theta360.media.MediaRecorder.OnErrorListener errorListener;

    private theta360.media.MediaRecorder.OnInfoListener onInfoListener = new theta360.media.MediaRecorder.OnInfoListener() {
        @Override
        public void onInfo(theta360.media.MediaRecorder mediaRecorder, int what, int extra) {
            infoListener.onInfo(mediaRecorder, what, extra);
        }
    };
    private theta360.media.MediaRecorder.OnErrorListener onErrorListener = new theta360.media.MediaRecorder.OnErrorListener() {
        @Override
        public void onError(theta360.media.MediaRecorder mediaRecorder, int what, int extra) {
            errorListener.onError(mediaRecorder, what, extra);
        }
    };

    @Override
    public void newMediaRecorder() {
        if(mMediaRecorder == null) {
            mMediaRecorder = new theta360.media.MediaRecorder();
        }
    }

    @Override
    public void setCamera(Camera camera) {
        mCamera = camera.getXCamera();
        mMediaRecorder.setCamera(mCamera);
    }

    @Override
    public void setMediaRecorderContext(Context context) {
        mMediaRecorder.setMediaRecorderContext(context);
    }

    @Override
    public void setVideoSource(int videoSource) {
        mMediaRecorder.setVideoSource(videoSource);
    }

    @Override
    public void setAudioSource(int audioSource) {
        mMediaRecorder.setAudioSource(audioSource);
    }

    @Override
    public void setMicDeviceId(int micDeviceId) {
        mMediaRecorder.setMicDeviceId(micDeviceId);
    }

    @Override
    public void setMicGain(int micGain) {
        mMediaRecorder.setMicGain(micGain);
    }

    @Override
    public void setMicSamplingFormat(int micSamplingFormat) {
        mMediaRecorder.setMicSamplingFormat(micSamplingFormat);
    }

    @Override
    public void setAudioSamplingRate(int audioSamplingRate) {
        mMediaRecorder.setAudioSamplingRate(audioSamplingRate);
    }

    @Override
    public void setAudioEncodingBitRate(int bitRate) {
        mMediaRecorder.setAudioEncodingBitRate(bitRate);
    }

    @Override
    public void setOutputFormat(int format) {
        mMediaRecorder.setOutputFormat(format);
    }

    @Override
    public void setVideoSize(int var1, int var2) {
        mMediaRecorder.setVideoSize(var1, var2);
    }

    @Override
    public void setVideoEncoder(int videoEncoder) {
        mMediaRecorder.setVideoEncoder(videoEncoder);
    }

    @Override
    public void setVideoEncodingProfileLevel(int profile, int level) {
        mMediaRecorder.setVideoEncodingProfileLevel(profile, level);
    }

    @Override
    public void setProfile(CamcorderProfile profile) {
        try {
            throw new ThetaModelException(getClass().getName()+  " setProfile(CamcorderProfile) VMediaRecorderOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setProfile(theta360.media.CamcorderProfile profile) {
        mMediaRecorder.setProfile(profile);
    }

    @Override
    public void setVideoEncodingIFrameInterval(float interval) {
        mMediaRecorder.setVideoEncodingIFrameInterval(interval);
    }

    @Override
    public void setVideoEncodingBitRate(int videoBitRate) {
        mMediaRecorder.setVideoEncodingBitRate(videoBitRate);
    }

    @Override
    public void setVideoFrameRate(int videoFrameRate) {
        mMediaRecorder.setVideoFrameRate(videoFrameRate);
    }

    @Override
    public void setMaxDuration(int maxDuration) {
        mMediaRecorder.setMaxDuration(maxDuration);
    }

    @Override
    public void setMaxFileSize(Long maxFileSize) {
        mMediaRecorder.setMaxFileSize(maxFileSize);
    }

    @Override
    public void setOutputFile(String videoWavFile) {
        mMediaRecorder.setOutputFile(videoWavFile);
    }

    @Override
    public void setPreviewDisplay(Surface surface) {
        mMediaRecorder.setPreviewDisplay(surface);
    }

    @Override
    public void setOnErrorListener(OnErrorListener listener) {
        mMediaRecorder.setOnErrorListener(errorListener);
    }

    @Override
    public void setOnInfoListener(OnInfoListener listener) {
        mMediaRecorder.setOnInfoListener(infoListener);
    }

    @Override
    public int getExternalDeviceId() {
        return mMediaRecorder.getExternalDeviceId();
    }

    @Override
    public void prepare() throws IOException {
        mMediaRecorder.prepare();
    }

    @Override
    public void start() {
        mMediaRecorder.start();
    }

    @Override
    public void stop() {
        mMediaRecorder.stop();
    }

    @Override
    public void reset() {
        mMediaRecorder.reset();
    }

    @Override
    public void release() {
        mMediaRecorder.release();
    }

    @Override
    public void setAudioChannels(int channels) {
        mMediaRecorder.setAudioChannels(channels);
    }
}
