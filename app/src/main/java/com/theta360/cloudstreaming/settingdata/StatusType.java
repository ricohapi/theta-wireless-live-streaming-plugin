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


/**
 * Define THETA status
 */
public enum StatusType {
    RUNNING("1", "Waiting for streaming"),
    LIVE_STREAMING("2", "On streaming"),
    STOP_STREAMING("3", "Waiting for streaming"),
    ERROR_CONNECT_SERVER("4", "Connection to the server failed"),
    ERROR_NOT_USER_SETTING("5", "User setting is not set yet"),
    TIMEOUT("6", "Connection to the server has timed out"),
    ERROR_INITIALIZATION("7", "Audio or camera initialization failed");

    private String code;
    private String message;

    StatusType(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() { return this.code; }

    public String getMessage() { return this.message; }

    public static StatusType getType(String message) {
        try {
            return StatusType.valueOf(message);
        } catch (IllegalArgumentException | NullPointerException e) {
            return StatusType.RUNNING;
        }
    }
}