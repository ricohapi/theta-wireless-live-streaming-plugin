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

package com.theta360.cloudstreaming.settingdata;


import com.google.gson.Gson;
import com.theta360.cloudstreaming.httpserver.AndroidWebServer;

/**
 * Define the data type of the setting that the user decides.
 *
 * If the bit rate is auto, AUTO_BITRATE is used
 */
public class SettingData {

    // Streaming Settings
    private String serverUrl;   // Server Url
    private String streamName;  // Stream name / Key
    private String cryptText;  // Encrypted stream name / Key

    // Video Settings
    private int movieWidth;  // Dimension, Width
    private int movieHeight; // Dimension, Height
    private double fps;         // Frame rate
    private String bitRate;     // Bit rate
    private String autoBitRate;     // Auto Bit rate
    private int noOperationTimeoutMinute;  // No operation timeout [seconds]
    private int audioSamplingRate;   // Audio sample Rate[Hz]

    // Information
    private String status;  // Status

    /**
     * Constructor
     */
    public SettingData() {
        this.serverUrl = "";
        this.streamName = "";
        this.cryptText = "";
        this.movieWidth = Bitrate.MOVIE_WIDTH_4K;
        this.movieHeight = Bitrate.MOVIE_HEIGHT_2K;
        this.fps = Bitrate.FPS_4K_30;
        this.bitRate = Bitrate.BITRATE_4K_DEFAULT;
        this.autoBitRate = "";
        this.noOperationTimeoutMinute = AndroidWebServer.TIMEOUT_DEFAULT_MINUTE;
        this.audioSamplingRate = AndroidWebServer.DEFAULT_AUDIO_SAMPLING_RATE;
        this.status = StatusType.RUNNING.getCode();
    }

    /**
     * Get ServerUrl
     *
     * @return Server Url
     */
    public String getServerUrl() {
        return this.serverUrl;
    }

    /**
     * Set ServerUrl
     *
     * @param serverUrl Server URL
     */
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    /**
     * Get StreamName
     *
     * @return Stream name / key
     */
    public String getStreamName() {
        return this.streamName;
    }

    /**
     * Set StreamName
     *
     * @param streamName Stream name / key
     */
    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }


    /**
     * Get StreamName
     *
     * @return Encrypted stream name / Key
     */
    public String getCryptText() {
        return this.cryptText;
    }

    /**
     * Set StreamName
     *
     * @param cryptText Encrypted stream name / Key
     */
    public void setCryptText(String cryptText) {
        this.cryptText = cryptText;
    }

    /**
     * Get width
     *
     * @return Width
     */
    public int getMovieWidth() {
        return this.movieWidth;
    }

    /**
     * Set width
     *
     * @param movieWidth Width
     */
    public void setMovieWidth(int movieWidth) {
        this.movieWidth = movieWidth;
    }

    /**
     * Get height
     *
     * @return Height
     */
    public int getMovieHeight() {
        return this.movieHeight;
    }

    /**
     * Set height
     *
     * @param movieHeight Height
     */
    public void setMovieHeight(int movieHeight) {
        this.movieHeight = movieHeight;
    }

    /**
     * Get Fps
     *
     * @return Frame rate
     */
    public double getFps() {
        return this.fps;
    }

    /**
     * Set Fps
     *
     * @param fps Frame rate
     */
    public void setFps(double fps) {
        this.fps = fps;
    }

    /**
     * Set BitRate
     *
     * @return Bit rate
     */
    public String getBitRate() {
        return this.bitRate;
    }

    /**
     * Get BitRate
     *
     * @param bitRate Bit rate
     */
    public void setBitRate(String bitRate) {
        this.bitRate = bitRate;
    }

    /**
     * Get Audio Sampling Rate
     *
     * @return Audio Sampling Rate
     */
    public int getAudioSamplingRate() {
        return this.audioSamplingRate;
    }

    /**
     * Set Audio Sampling Rate
     *
     * @param audioSamplingRate  Audio Sampling Rate
     */
    public void setAudioSamplingRate(int audioSamplingRate) {
        this.audioSamplingRate = audioSamplingRate;
    }

    /**
     * Get No operation timeout
     *
     * @return No operation timeout
     */
    public int getNoOperationTimeoutMinute() {
        return this.noOperationTimeoutMinute;
    }

    /**
     * Set No operation timeout
     *
     * @param noOperationTimeoutMinute No operation timeout
     */
    public void setNoOperationTimeoutMinute(int noOperationTimeoutMinute) {
        this.noOperationTimeoutMinute = noOperationTimeoutMinute;
    }

    /**
     * Get Status
     *
     * @return status
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * Set Status
     *
     * @param status Status
     */
    public void setStatus(String status) {
        if (status != null)
            this.status = status;
    }

    /**
     * Convert to Json myself
     *
     * @return [description]
     */
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}
