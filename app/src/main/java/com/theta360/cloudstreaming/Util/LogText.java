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

import android.os.Environment;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

/**
 * Log forming class for debugging
 * This is called within Timber, forms the log and outputs it.
 */
class LogText {

    static private String logFilePath;

    /**
     * Log preservation initialization method.
     *
     * @param logFileDirPath Folder path of Log file
     * @param logFileName Log file name
     */
    public static void Init(String logFileDirPath, String logFileName) {

        logFilePath = logFileDirPath + File.separator + logFileName;

        // Create without log storage folder
        File root = new File(logFileDirPath);
        if (!root.exists()) {
            root.mkdir();
        }
    }

    /**
     * Write the same contents as logcat to the log file.
     * Output stack trace to standard error output.
     * If an exception occurs during log recording, there is a possibility of entering an infinite loop if it is recorded in a file.
     *
     * @param tag tag
     * @param msg message
     */
    public static void Println(String tag, String msg) {

        File file = new File(logFilePath);

        // Output file
        try {
            FileOutputStream fos = new FileOutputStream(file, true);

            // Use UTF-8
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            BufferedWriter bw = new BufferedWriter(osw);

            // Write text
            bw.write("[" + tag + "]: " + msg + "\n");
            bw.flush();
            bw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
