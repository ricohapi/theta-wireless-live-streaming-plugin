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

package com.theta360.cloudstreaming.Extend;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import com.pedro.encoder.audio.AudioEncoder;
import com.pedro.encoder.audio.GetAacData;
import com.pedro.encoder.input.audio.GetMicrophoneData;
import com.pedro.encoder.input.audio.MicrophoneManager;
import com.pedro.encoder.input.decoder.AudioDecoderInterface;
import com.pedro.encoder.input.video.Camera1ApiManager;
import com.pedro.encoder.input.video.GetCameraData;
import com.pedro.encoder.video.FormatVideoEncoder;
import com.pedro.encoder.video.GetH264Data;
import com.pedro.encoder.video.VideoEncoder;
import com.pedro.rtplibrary.view.OpenGlView;
import java.nio.ByteBuffer;
import java.util.HashMap;


/**
 * Encode video and audio
 */
public abstract class RtmpExtendBase implements GetMicrophoneData, AudioDecoderInterface, GetAacData, GetCameraData, GetH264Data {

    protected Context context;
    protected Camera1ApiManager cameraManager;
    protected MicrophoneManager microphoneManager;
    protected VideoEncoder videoEncoder;
    protected AudioEncoder audioEncoder;
    protected OpenGlView openGlView;
    protected boolean streaming;
    protected MediaMuxer mediaMuxer;
    protected int videoTrack = -1;
    protected int audioTrack = -1;
    protected boolean recording = false;
    protected boolean canRecord = false;
    protected boolean onPreview = false;
    protected MediaFormat videoFormat;
    protected MediaFormat audioFormat;

    private HashMap streamingParamMap;

    @RequiresApi(
        api = 18
    )

    /**
     * Constructor
     * @param  openGlView     OpneGLView of pedro library
     */
    public RtmpExtendBase(OpenGlView openGlView) {
        this.context = openGlView.getContext();
        this.openGlView = openGlView;
        this.videoEncoder = new VideoEncoder(this);
        this.microphoneManager = new MicrophoneManager(this);
        this.audioEncoder = new AudioEncoder(this);
        this.streaming = false;
    }

    protected abstract void startStreamRtp(String var1);

    protected abstract void stopStreamRtp();

    public boolean isStreaming() {
        return this.streaming;
    }

    protected abstract void getAacDataRtp(ByteBuffer var1, MediaCodec.BufferInfo var2);

    /**
     * Acquire encoded audio data
     *
     * @param aacBuffer out/Audio data
     * @param info out/Information of audio data, e.g. sampling rate, etc.
     */
    public void getAacData(ByteBuffer aacBuffer, MediaCodec.BufferInfo info) {
        if (Build.VERSION.SDK_INT >= 18 && this.recording && this.audioTrack != -1 && this.canRecord) {
            this.mediaMuxer.writeSampleData(this.audioTrack, aacBuffer, info);
        }
        this.getAacDataRtp(aacBuffer, info);
    }

    protected abstract void getH264DataRtp(ByteBuffer var1, MediaCodec.BufferInfo var2);

    /**
     * Acquire video data
     * Actually, still images in H.264 format
     *
     * @param h264Buffer out/video data
     * @param info out/Information of video data, e.g. frame rate, etc.
     */
    public void getH264Data(ByteBuffer h264Buffer, MediaCodec.BufferInfo info) {
        if (Build.VERSION.SDK_INT >= 18 && this.recording && this.videoTrack != -1) {
            if (info.flags == 1) {
                this.canRecord = true;
            }

            if (this.canRecord) {
                this.mediaMuxer.writeSampleData(this.videoTrack, h264Buffer, info);
            }
        }

        this.getH264DataRtp(h264Buffer, info);
    }

    protected abstract void onSPSandPPSRtp(ByteBuffer var1, ByteBuffer var2);

    /**
     * Setting of pixel data before encoding
     * YUV format(Color difference)
     *
     * @param buffer Pixel data
     */
    public void inputYUVData(byte[] buffer) {
        this.videoEncoder.inputYUVData(buffer);
    }

    /**
     * Setting of audio data before encoding
     *
     * @param buffer Audio buffer
     * @param size Length
     */
    @Override
    public void inputPCMData(byte[] buffer, int size) {
        audioEncoder.inputPCMData(buffer, size);
    }


    /**
     * SPS and PPS configuration callback
     *
     * @param sps (Sequence Parameter Set)
     * @param pps (Picture Parameter Set)
     */
    public void onSPSandPPS(ByteBuffer sps, ByteBuffer pps) {
        this.onSPSandPPSRtp(sps, pps);
    }

    /**
     * Movie format setting callback
     *
     * @param mediaFormat Media format
     */
    public void onVideoFormat(MediaFormat mediaFormat) {
        this.videoFormat = mediaFormat;
    }

    /**
     * Audio format setup callback
     *
     * @param mediaFormat Media format
     */
    public void onAudioFormat(MediaFormat mediaFormat) {
        this.audioFormat = mediaFormat;
    }

    /**
     * Encode and start sending to server
     *
     * @param url Server URL
     */
    public void startStream(String url) {
        if (openGlView != null && Build.VERSION.SDK_INT >= 18) {
            openGlView.setEncoderSize(videoEncoder.getWidth(), videoEncoder.getHeight());
            openGlView.startGLThread();
            openGlView.addMediaCodecSurface(videoEncoder.getInputSurface());
        }

        startStreamRtp(url);
        videoEncoder.start();
        audioEncoder.start();
        microphoneManager.start();
        streaming = true;
    }

    /**
     * Stop encoding and server transmission
     */
    public void stopStream() {
        microphoneManager.stop();
        this.stopStreamRtp();
        this.videoEncoder.stop();
        this.audioEncoder.stop();
        if (this.openGlView != null && Build.VERSION.SDK_INT >= 18) {
            this.openGlView.stopGlThread();
            this.openGlView.removeMediaCodecSurface();
        }
        this.streaming = false;
    }

    /**
     * Set parameter list of streaming
     *
     * @param value Information such as vertical and horizontal sizes
     */
    public void setStreamingParamMap(HashMap value) {
        this.streamingParamMap = value;
    }

    /**
     * Get parameter list of streaming
     *
     * @return Information such as vertical and horizontal sizes
     */
    private HashMap getStreamingParamMap() {
        return streamingParamMap;
    }

    /**
     * Movie preparation
     *
     * @return Success: true, failed: false
     */
    public boolean prepareVideo() {
        if (this.openGlView == null) {
            this.cameraManager.prepareCamera();
            return this.videoEncoder.prepareVideoEncoder();
        } else {
            return this.videoEncoder.prepareVideoEncoder(
                Integer.parseInt(getStreamingParamMap().get("width").toString()),
                Integer.parseInt(getStreamingParamMap().get("height").toString()),
                Integer.parseInt(getStreamingParamMap().get("fps").toString()),
                Integer.parseInt(getStreamingParamMap().get("bitrate").toString()),
                0, false, FormatVideoEncoder.SURFACE);
        }
    }


    protected abstract void prepareAudioRtp(boolean isStereo, int sampleRate);

    /**
     * Audio preparation
     *
     * @return Success: true, failed: false
     */
    public boolean prepareAudio() {
        microphoneManager.createMicrophone(44100, false, false, true);
        // If the argument is omitted, 128 * 1024, 44100, true, false, false
        return this.audioEncoder.prepareAudioEncoder(128 * 1024, 44100, false);
    }


    /**
     * Audio decode end callback
     *
     */
    @Override
    public void onAudioDecoderFinished() {
    }

}