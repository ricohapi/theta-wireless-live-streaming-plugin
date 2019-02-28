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

import android.media.MediaCodec;
import android.os.Build;
import android.support.annotation.RequiresApi;
import com.pedro.rtplibrary.view.OpenGlView;
import java.nio.ByteBuffer;
import net.ossrs.rtmp.ConnectCheckerRtmp;
import net.ossrs.rtmp.SrsFlvMuxer;


/**
 * Transmits video and audio data (stream) in Flv container.
 */
public class RtmpExtend extends RtmpExtendBase {

    private SrsFlvMuxer srsFlvMuxer;

    /**
     * Constructor
     *
     * @param openGlView OpneGLView of pedro library
     * @param connectChecker Callback class called by connection state
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public RtmpExtend(OpenGlView openGlView, ConnectCheckerRtmp connectChecker) {
        super(openGlView);
        srsFlvMuxer = new SrsFlvMuxer(connectChecker);
    }

    /**
     * Start sending to server
     *
     * @param url Server URL
     */
    @Override
    protected void startStreamRtp(String url) {
        if (videoEncoder.getRotation() == 90 || videoEncoder.getRotation() == 270) {
            srsFlvMuxer.setVideoResolution(videoEncoder.getHeight(), videoEncoder.getWidth());
        } else {
            srsFlvMuxer.setVideoResolution(videoEncoder.getWidth(), videoEncoder.getHeight());
        }

        srsFlvMuxer.start(url);
    }

    /**
     * End transmission to server
     */
    @Override
    protected void stopStreamRtp() {
        srsFlvMuxer.stop();
    }

    /**
     * Send audio to server
     */
    @Override
    protected void getAacDataRtp(ByteBuffer aacBuffer, MediaCodec.BufferInfo info) {
        srsFlvMuxer.sendAudio(aacBuffer, info);
    }

    /**
     * Send video (H.264) to server
     */
    @Override
    protected void getH264DataRtp(ByteBuffer h264Buffer, MediaCodec.BufferInfo info) {
        srsFlvMuxer.sendVideo(h264Buffer, info);
    }

    /**
     * Setting of SPS / PPS (data used in containers)
     *
     * @param sps (Sequence Parameter Set)
     * @param pps (Picture Parameter Set)
     */
    @Override
    protected void onSPSandPPSRtp(ByteBuffer sps, ByteBuffer pps) {
        srsFlvMuxer.setSpsPPs(sps, pps);
    }


    @Override
    protected void prepareAudioRtp(boolean isStereo, int sampleRate) {
        srsFlvMuxer.setIsStereo(isStereo);
        srsFlvMuxer.setSampleRate(sampleRate);
    }


}
