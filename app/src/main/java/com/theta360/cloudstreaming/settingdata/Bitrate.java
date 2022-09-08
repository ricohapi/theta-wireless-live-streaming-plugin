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

import java.util.HashMap;
import java.util.Map;

/**
 * Bit rate
 */
public class Bitrate {
    public static final int MOVIE_WIDTH_4K = 3840;
    public static final int MOVIE_HEIGHT_4K = 2160;
    public static final double FPS_4K_30 = 30.0;
    public static final double FPS_4K_15 = 15.0;
    public static final String BITRATE_4K_DEFAULT = "20";
    public static final String BITRATE_4K_6 = "6";
    public static final String BITRATE_4K_10 = "10";
    public static final String BITRATE_4K_12 = "12";
    public static final String BITRATE_4K_20 = "20";
    public static final String BITRATE_4K_40 = "40";
    private static final int DELAY_AVERAGE_BYTE_BORDER_4K_13 = 1000000;
    private static final int DELAY_AVERAGE_BYTE_BORDER_4K_30 = 2000000;
    private static final int DELAY_AVERAGE_BYTE_BORDER_4K_54 = 3000000;
    private static final Map<String, Integer> DELAY_BORDER_4K_CHOICES = new HashMap<String, Integer>() {
        {
            put(BITRATE_4K_12, DELAY_AVERAGE_BYTE_BORDER_4K_13);
            put(BITRATE_4K_20, DELAY_AVERAGE_BYTE_BORDER_4K_30);
            put(BITRATE_4K_40, DELAY_AVERAGE_BYTE_BORDER_4K_54);
        }
    };

    public static final int MOVIE_WIDTH_2K = 1920;
    public static final int MOVIE_HEIGHT_2K = 1080;
    public static final double FPS_2K_30 = 30.0;
    public static final double FPS_2K_15 = 15.0;
    public static final String BITRATE_2K_DEFAULT = "6";
    public static final String BITRATE_2K_1_5 = "1.5";
    public static final String BITRATE_2K_3 = "3";
    public static final String BITRATE_2K_6 = "6";
    public static final String BITRATE_2K_8 = "8";
    public static final String BITRATE_2K_16 = "16";
    private static final int DELAY_AVERAGE_BYTE_BORDER_2K_3 = 400000;
    private static final int DELAY_AVERAGE_BYTE_BORDER_2K_6 = 800000;
    private static final int DELAY_AVERAGE_BYTE_BORDER_2K_16 = 1500000;
    private static final Map<String, Integer> DELAY_BORDER_2K_CHOICES = new HashMap<String, Integer>() {
        {
            put(BITRATE_2K_3, DELAY_AVERAGE_BYTE_BORDER_2K_3);
            put(BITRATE_2K_6, DELAY_AVERAGE_BYTE_BORDER_2K_6);
            put(BITRATE_2K_16, DELAY_AVERAGE_BYTE_BORDER_2K_16);
        }
    };

    public static final int MOVIE_WIDTH_1K = 1024;
    public static final int MOVIE_HEIGHT_1K = 576;
    public static final double FPS_1K_30 = 30.0;
    public static final double FPS_1K_15 = 15.0;
    public static final String BITRATE_1K_DEFAULT = "0.85";
    public static final String BITRATE_1K_025 = "0.25";
    public static final String BITRATE_1K_042 = "0.42";
    public static final String BITRATE_1K_05 = "0.5";
    public static final String BITRATE_1K_085 = "0.85";
    public static final String BITRATE_1K_1 = "1";
    public static final String BITRATE_1K_2 = "2";
    private static final int DELAY_AVERAGE_BYTE_BORDER_1K_05 = 300000;
    private static final int DELAY_AVERAGE_BYTE_BORDER_1K_085 = 500000;
    private static final int DELAY_AVERAGE_BYTE_BORDER_1K_2 = 800000;
    private static final Map<String, Integer> DELAY_BORDER_1K_CHOICES = new HashMap<String, Integer>() {
        {
            put(BITRATE_1K_05, DELAY_AVERAGE_BYTE_BORDER_1K_05);
            put(BITRATE_1K_085, DELAY_AVERAGE_BYTE_BORDER_1K_085);
            put(BITRATE_1K_2, DELAY_AVERAGE_BYTE_BORDER_1K_2);
        }
    };

    public static final int MOVIE_WIDTH_06K = 640;
    public static final int MOVIE_HEIGHT_06K = 360;
    public static final double FPS_06K = 30.0;
    public static final String BITRATE_06K_DEFAULT = "0.36";
    private static final String BITRATE_06K_025 = "0.25";
    private static final String BITRATE_06K_036 = "0.36";
    private static final String BITRATE_06K_1 = "1";
    private static final int DELAY_AVERAGE_BYTE_BORDER_06K_025 = 150000;
    private static final int DELAY_AVERAGE_BYTE_BORDER_06K_036 = 250000;
    private static final int DELAY_AVERAGE_BYTE_BORDER_06K_1 = 400000;
    private static final Map<String, Integer> DELAY_BORDER_06K_CHOICES = new HashMap<String, Integer>() {
        {
            put(BITRATE_06K_025, DELAY_AVERAGE_BYTE_BORDER_06K_025);
            put(BITRATE_06K_036, DELAY_AVERAGE_BYTE_BORDER_06K_036);
            put(BITRATE_06K_1, DELAY_AVERAGE_BYTE_BORDER_06K_1);
        }
    };

    /**
     * Returns the maximum bit rate from the screen width
     *
     * @param movieWidth width
     * @return maximum bit rate
     */
    public static String getMaxBitrate(int movieWidth) {
        switch (movieWidth) {
            case MOVIE_WIDTH_4K:
                return BITRATE_4K_40;
            case MOVIE_WIDTH_2K:
                return BITRATE_2K_16;
            case MOVIE_WIDTH_1K:
                return BITRATE_1K_2;
            case MOVIE_WIDTH_06K:
                return BITRATE_06K_1;
            default:
                return BITRATE_2K_16;
        }
    }

    /**
     * Returns the average number of bytes to be "delayed" depending on the screen width and bit rate
     *
     * @param movieWidth Width
     * @return Default bit rate
     */
    public static int getAverageByteDelayBorder(int movieWidth, String bitrate) {
        switch (movieWidth) {
            case MOVIE_WIDTH_4K:
                return DELAY_BORDER_4K_CHOICES.get(bitrate);
            case MOVIE_WIDTH_2K:
                return DELAY_BORDER_2K_CHOICES.get(bitrate);
            case MOVIE_WIDTH_1K:
                return DELAY_BORDER_1K_CHOICES.get(bitrate);
            case MOVIE_WIDTH_06K:
                return DELAY_BORDER_06K_CHOICES.get(bitrate);
            default:
                return DELAY_BORDER_2K_CHOICES.get(bitrate);
        }
    }
}
