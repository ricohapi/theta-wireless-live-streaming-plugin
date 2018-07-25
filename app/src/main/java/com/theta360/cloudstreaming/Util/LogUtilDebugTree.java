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

package com.theta360.cloudstreaming.Util;

import android.util.Log;
import timber.log.Timber;

/**
 * Logging class for debugging.
 * This is called within Timber, forms the log and outputs it.
 */
public class LogUtilDebugTree extends Timber.DebugTree {

    /**
     * Constructor
     *
     * @param logFileDirPath Folder path of Log file
     * @param logFileName Log file name
     */
    public LogUtilDebugTree(String logFileDirPath, String logFileName) {
        super();
        LogText.Init(logFileDirPath, logFileName);
    }

    /**
     * Forming log
     * Called back from the Timber class.
     *
     * @param priority priority
     * @param tag tag
     * @param message message to record
     * @param t exception log
     *
     * See document of android.util.Log for details
     */
    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        StackTraceElement[] thread = new Throwable().getStackTrace();

        if (thread != null && thread.length >= 5) {
            StackTraceElement stack = thread[5];
            String className = stack.getClassName();
            String packageName = className.substring(0, className.lastIndexOf("."));
            String fileName = stack.getFileName();
            String methodName = stack.getMethodName();

            String msg =
                "at " + packageName + "(" + fileName + ":" + stack.getLineNumber() + ") . ["
                    + methodName + "] --- " + message;

            Log.println(priority, tag, msg);
            LogText.Println(tag, msg);
        } else {
            Log.println(priority, tag, message);
            LogText.Println(tag, message);
        }


    }
}

