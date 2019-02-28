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

/**
 * ビットレート自動調整機能向けに、シーケンス番号を連携します。
 */
public class SequenceNumberStorage {

    private static final long SEQUENCE_NUMBER_MAX = 2147483647;

    private static int firstSequenceNumber = -1;
    private static int lastSequenceNumber = -1;

    private static int loopCount = 0;

    public static void initSequenceNumber() {
        firstSequenceNumber = -1;
        lastSequenceNumber = -1;
        loopCount = 0;
    }

    public static void setSequenceNumber(int sequenceNumber) {
        if (firstSequenceNumber == -1) {
            firstSequenceNumber = sequenceNumber;
        }
        if (sequenceNumber < lastSequenceNumber) {
            loopCount++;
        }
        lastSequenceNumber = sequenceNumber;
    }

    public static long calculateByte() {
        if (loopCount == 0) {
            return lastSequenceNumber - firstSequenceNumber;
        } else {
            return lastSequenceNumber + (SEQUENCE_NUMBER_MAX - firstSequenceNumber) + SEQUENCE_NUMBER_MAX * (loopCount - 1);
        }
    }
}
