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

package com.github.faucamp.simplertmp.origin;

import com.github.faucamp.simplertmp.packets.Audio;
import com.github.faucamp.simplertmp.packets.RtmpPacket;
import com.github.faucamp.simplertmp.packets.Video;
import java.net.SocketException;

/**
 * Videoが配信されない状態が発生したときにエラーとならない為、強制的にエラーとします。
 */
public class CheckIsStreamVideo {

    // テスト配信が10秒なのでそれより短くしたい
    public static final long NOT_STREAM_VIDEO_MSEC = 8 * 1000;
    public static long firstAudioStreamTime = 0;
    public static boolean isStreamVideo = false;

    public static void init() {
        firstAudioStreamTime = 0;
        isStreamVideo = false;
    }

    public static void check(RtmpPacket rtmpPacket) throws SocketException {
        if (isStreamVideo) {
            return;
        }
        if (rtmpPacket instanceof Video) {
            isStreamVideo = true;
        } else if(rtmpPacket instanceof Audio) {
            if (firstAudioStreamTime == 0) {
                firstAudioStreamTime = System.currentTimeMillis();
            }
            if (System.currentTimeMillis() - firstAudioStreamTime > NOT_STREAM_VIDEO_MSEC) {
                throw new SocketException("Videoの配信が始まりません。何らかのエラーが発生した可能性があります。");
            }
        }
    }
}
