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

package com.theta360.cloudstreaming.httpserver;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


/**
 * データベースヘルパークラス
 */
public class Theta360SQLiteOpenHelper extends SQLiteOpenHelper {

    private static final String DB = "theta360_setting.db";
    private static final int DB_VERSION = 1;
    private static final String CREATE_TABLE_SQL = "create table theta360_setting ( id INTEGER primary key,server_url TEXT,stream_name TEXT,crypt_text TEXT,movie_width INTEGER,movie_height INTEGER,fps REAL,bitrate TEXT,auto_bitrate TEXT,no_operation_timeout_minute INTEGER, status TEXT);";
    private static final String DROP_TABLE_SQL = "drop table theta360_setting;";

    public Theta360SQLiteOpenHelper(Context c) {
        super(c, DB, null, DB_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SQL);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE_SQL);
        onCreate(db);
    }
}
