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

package com.theta360.pluginlibrary.factory;

import android.content.Context;
import android.media.CamcorderProfile;
import android.view.Surface;

import com.theta360.pluginlibrary.ThetaModelException;

import java.io.IOException;

public class VMediaRecorder extends MediaRecorder {
    private android.hardware.Camera mCamera;
    private android.media.MediaRecorder mMediaRecorder;
    private android.media.MediaRecorder.OnInfoListener infoListener;
    private android.media.MediaRecorder.OnErrorListener errorListener;

    private android.media.MediaRecorder.OnInfoListener onInfoListener = new android.media.MediaRecorder.OnInfoListener() {
        @Override
        public void onInfo(android.media.MediaRecorder mr, int what, int extra) {
            infoListener.onInfo(mr, what, extra);
        }
    };
    private android.media.MediaRecorder.OnErrorListener onErrorListener = new android.media.MediaRecorder.OnErrorListener() {
        @Override
        public void onError(android.media.MediaRecorder mediaRecorder, int what, int extra) {
            errorListener.onError(mediaRecorder, what, extra);
        }
    };

    @Override
    public void newMediaRecorder() {
        if(mMediaRecorder == null) {
            mMediaRecorder = new android.media.MediaRecorder();
        }
    }

    @Override
    public void setCamera(Camera camera) {
        if(mMediaRecorder == null) {
            mMediaRecorder = new android.media.MediaRecorder();
            mCamera = camera.getVCamera();
            mMediaRecorder.setCamera(mCamera);
        }
    }

    @Override
    public void setMediaRecorderContext(Context context) {
        try {
            throw new ThetaModelException(getClass().getName()+  " setMediaRecorderContext(Context) XMediaRecorderOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        try {
            throw new ThetaModelException(getClass().getName()+  " setMicDeviceId(int) XMediaRecorderOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setMicGain(int micGain) {
        try {
            throw new ThetaModelException(getClass().getName()+  " setMicGain(int) XMediaRecorderOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setMicSamplingFormat(int micSamplingFormat) {
        try {
            throw new ThetaModelException(getClass().getName()+  " setMicSamplingFormat(int) XMediaRecorderOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setAudioSamplingRate(int audioSamplingRate) {
        try {
            throw new ThetaModelException(getClass().getName()+  " setAudioSamplingRate(int) XMediaRecorderOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setAudioEncodingBitRate(int audioEncodingBitRate) {
        mMediaRecorder.setAudioEncodingBitRate(audioEncodingBitRate);
    }

    @Override
    public void setOutputFormat(int outputFormat) {
        mMediaRecorder.setOutputFormat(outputFormat);
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
        try {
            throw new ThetaModelException(getClass().getName()+  " setVideoEncodingProfileLevel(int, int) XMediaRecorderOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setProfile(CamcorderProfile profile) {
        mMediaRecorder.setProfile(profile);
    }

    @Override
    public void setProfile(theta360.media.CamcorderProfile profile) {
        try {
            throw new ThetaModelException(getClass().getName()+  " setProfile(theta360.media.CamcorderProfile) XMediaRecorderOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setVideoEncodingIFrameInterval(float interval) {
        try {
            throw new ThetaModelException(getClass().getName()+  " setVideoEncodingIFrameInterval(float) XMediaRecorderOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        try {
            throw new ThetaModelException(getClass().getName()+  " getExternalDeviceId() XMediaRecorderOnlyClass");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
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
