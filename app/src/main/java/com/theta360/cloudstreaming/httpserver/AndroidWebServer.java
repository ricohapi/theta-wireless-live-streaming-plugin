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


import static fi.iki.elonen.NanoHTTPD.Response.Status.NOT_FOUND;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.wifi.WifiManager;
import android.util.Base64;
import android.util.Log;

import com.theta360.cloudstreaming.receiver.LiveStreamingReceiver;
import com.theta360.cloudstreaming.settingdata.Bitrate;
import com.theta360.cloudstreaming.settingdata.SettingData;
import com.theta360.pluginlibrary.activity.ThetaInfo;
import com.theta360.pluginlibrary.values.ThetaModel;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import timber.log.Timber;


/**
 * Provide web server function
 */
public class AndroidWebServer extends Activity {

    public static final int PRIMARY_KEY_ID = 1;

    // Encryption key generation by using random UUID
    private static final String ENCRYPTION_KEY = Base64.encodeToString(UUID.randomUUID().toString().substring(0, 16).getBytes(), Base64.NO_WRAP);
    private static final String ENCRYPTION_IV = Base64.encodeToString(UUID.randomUUID().toString().substring(0, 16).getBytes(), Base64.NO_WRAP);

    private SQLiteDatabase dbObject;

    private static final int PORT = 8888;
    private SimpleHttpd server;
    private Boolean requested;

    public static final int TIMEOUT_DEFAULT_MINUTE = -1;
    public static final int DEFAULT_AUDIO_SAMPLING_RATE = 48000;
    private Context con;

    private String measuredBitrate;

    public AndroidWebServer(Context context) {
        con = context;
        create();
    }

    /**
     * {@inheritDoc}
     *
     * Create Activity Callback
     */
    @SuppressLint({"SetTextI18n", "LongLogTag"})
    public void create() {

        Log.d("AndroidWebServerActivity", "onCreate");
        WifiManager wifiManager = (WifiManager) con.getSystemService(Context.WIFI_SERVICE);

        assert wifiManager != null;
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        @SuppressLint("DefaultLocale") final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));

        Timber.i("Launch server with IP [" + formatedIpAddress + "].");

        try {
            server = new SimpleHttpd(con);
            server.start();
            Log.i("AndroidWebServerActivity", "Start server");
        } catch (Exception e) {
            e.printStackTrace();
        }

        Theta360SQLiteOpenHelper hlpr = new Theta360SQLiteOpenHelper(con);
        dbObject = hlpr.getWritableDatabase();
        // Update encrypted stream name of DB
        Cursor cursor = dbObject.query("theta360_setting", null, "id=?", new String[]{String.valueOf(PRIMARY_KEY_ID)}, null, null, null, null);
        if (cursor.moveToNext()) {
            String streamName = prevent_xss(cursor.getString(cursor.getColumnIndex("stream_name")));
            ContentValues values = new ContentValues();
            values.put("crypt_text", encodeStreamName(streamName));
            dbObject.update("theta360_setting", values, "id=?", new String[]{String.valueOf(PRIMARY_KEY_ID)});
        }

        clearRequested();
    }

    /**
     * {@inheritDoc}
     *
     * Discard callback
     */
    public void destroy() {
        if (server != null) {
            server.stop();
            Log.i("AndroidWebServerActivity", "Stop server");
        }
    }

    /**
     * Convert InputStream to a string
     *
     * @param is InputStream to convert
     * @return Converted text
     */
    private String inputStreamToString(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();

        return sb.toString();
    }

    /**
     * Converts a string to an InputStream.
     *
     * @param str Text to convert
     * @return Converted InputStream
     */
    private InputStream stringToInputStream(String str) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(str.getBytes("utf-8"));
    }


    /**
     * Get a flag to stand when receiving a request
     *
     * @return Flag requested
     */
    public Boolean isRequested() {
        return requested;
    }

    /**
     * Clear the flag set when receiving the request
     */
    public void clearRequested() {
        requested = false;
    }

    /**
     * Encrypt the stream name.
     *
     * @param streamName stream name
     * @return Encrypted stream name
     */
    public static String encodeStreamName(String streamName) {
        try {
            SecretKey key = new SecretKeySpec(Base64.decode(ENCRYPTION_KEY, Base64.DEFAULT), "AES");
            AlgorithmParameterSpec iv = new IvParameterSpec(Base64.decode(ENCRYPTION_IV, Base64.DEFAULT));

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);

            // The NO_WRAP flag prevents a trailing newline
            return new String(Base64.encode(cipher.doFinal(streamName.getBytes()), Base64.NO_WRAP), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("This should not happen in production.", e);
        }

    }

    /**
     * Decrypt encrypted stream name.
     *
     * @param encrypted Encrypted stream name
     * @return Decrypted stream name
     */
    public static String decodeStreamName(String encrypted) {
        try {
            SecretKey key = new SecretKeySpec(Base64.decode(ENCRYPTION_KEY, Base64.DEFAULT), "AES");
            AlgorithmParameterSpec iv = new IvParameterSpec(Base64.decode(ENCRYPTION_IV, Base64.DEFAULT));
            byte[] decodeBase64 = Base64.decode(encrypted, Base64.DEFAULT);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, iv);

            return new String(cipher.doFinal(decodeBase64), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("This should not happen in production.", e);
        }

    }

    /**
     * Stores bit rate measurement value
     *
     * @param measuredBitrate Measured bit rate
     */
    public void setMeasuredBitrate(String measuredBitrate) {
        this.measuredBitrate = measuredBitrate;
    }

    /**
     * HTTP communication implementation class
     *
     */
    private class SimpleHttpd extends NanoHTTPD {

        private Context context;
        private final Logger LOG = Logger.getLogger(SimpleHttpd.class.getName());

        /**
         * Constructor
         *
         * @param con Application context
         */
        public SimpleHttpd(Context con) throws IOException {
            super(PORT);
            context = con;
        }

        /**
         * Request response
         *
         * @param session Off on session
         * @return resource
         */
        @Override
        public Response serve(IHTTPSession session) {
            Method method = session.getMethod();
            String uri = session.getUri();
            this.LOG.info(method + " '" + uri + "' ");

            switch (ThetaModel.getValue(ThetaInfo.getThetaModelName())){
                case THETA_X:
                    if ("/".equals(uri)) {
                        String version = ThetaInfo.getThetaFirmwareVersion(context);
                        if (version.compareTo("1.20.0") >= 0) {
                            uri = "index_x_v012000.html";
                        } else {
                            uri = "index_x.html";
                        }
                    }
                    break;
                case THETA_Z1:
                    if ("/".equals(uri)) {
                        uri = "index_z1.html";
                    }
                    break;
                case THETA_V:
                    if ("/".equals(uri)) {
                        uri = "index.html";
                    }
                    break;
            }

            //  In the case of NanoHTTPD, since the POST request is stored in a temporary file, a buffer is given for reading again
            Map<String, String> tmpRequestFile = new HashMap<>();
            if (Method.POST.equals(method)) {
                try {
                    session.parseBody(tmpRequestFile);
                } catch (IOException e) {
                    return newFixedLengthResponse(Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + e.getMessage());
                } catch (ResponseException e) {
                    return newFixedLengthResponse(e.getStatus(), MIME_PLAINTEXT, e.getMessage());
                }
            }
            Map<String, String> parms = session.getParms();

            // Update DB
            try {
                if (parms.get("update") != null) {
                    ContentValues values = new ContentValues();

                    values.put("id", PRIMARY_KEY_ID);
                    String movieType = parms.get("movie");

                    // Divide process by resolution.
                    if (MovieTypes.Movie4k.getString().equals(movieType)) {
                        // 4k
                        values.put("movie_width", Bitrate.MOVIE_WIDTH_4K);
                        values.put("movie_height", Bitrate.MOVIE_HEIGHT_4K);
                        values.put("bitrate", parms.get("bitrate4k"));
                        values.put("fps", Bitrate.FPS_4K_30);
                    } else if (MovieTypes.Movie4k_15fps.getString().equals(movieType)) {
                        // 4k
                        values.put("movie_width", Bitrate.MOVIE_WIDTH_4K);
                        values.put("movie_height", Bitrate.MOVIE_HEIGHT_4K);
                        values.put("bitrate", parms.get("bitrate4k_15fps"));
                        values.put("fps", Bitrate.FPS_4K_15);
                    } else if (MovieTypes.Movie2k.getString().equals(movieType)) {
                        // 2k
                        values.put("movie_width", Bitrate.MOVIE_WIDTH_2K);
                        values.put("movie_height", Bitrate.MOVIE_HEIGHT_2K);
                        values.put("bitrate", parms.get("bitrate2k"));
                        values.put("fps", Bitrate.FPS_2K_30);
                    } else if (MovieTypes.Movie2k_15fps.getString().equals(movieType)) {
                        // 2k
                        values.put("movie_width", Bitrate.MOVIE_WIDTH_2K);
                        values.put("movie_height", Bitrate.MOVIE_HEIGHT_2K);
                        values.put("bitrate", parms.get("bitrate2k_15fps"));
                        values.put("fps", Bitrate.FPS_2K_15);
                    } else if (MovieTypes.Movie1k.getString().equals(movieType)) {
                        // 1k
                        values.put("movie_width", Bitrate.MOVIE_WIDTH_1K);
                        values.put("movie_height", Bitrate.MOVIE_HEIGHT_1K);
                        values.put("bitrate", parms.get("bitrate1k"));
                        values.put("fps", Bitrate.FPS_1K_30);
                    } else if (MovieTypes.Movie1k_15fps.getString().equals(movieType)) {
                        // 1k
                        values.put("movie_width", Bitrate.MOVIE_WIDTH_1K);
                        values.put("movie_height", Bitrate.MOVIE_HEIGHT_1K);
                        values.put("bitrate", parms.get("bitrate1k_15fps"));
                        values.put("fps", Bitrate.FPS_1K_15);
                    } else {
                        // 0.6k
                        values.put("movie_width", Bitrate.MOVIE_WIDTH_06K);
                        values.put("movie_height", Bitrate.MOVIE_HEIGHT_06K);
                        values.put("bitrate", parms.get("bitrate06k"));
                        values.put("fps", Bitrate.FPS_06K);
                    }
                    values.put("server_url", parms.get("server_url"));

                    String crypt_text = parms.get("crypt");

                    values.put("stream_name", decodeStreamName(crypt_text));
                    values.put("crypt_text", crypt_text);

                    values.put("audio_sampling_rate", parms.get("audio_sampling_rate"));
                    values.put("no_operation_timeout_minute", parms.get("no_operation_timeout_minute"));

                    long num = dbObject.update("theta360_setting", values, "id=?", new String[]{String.valueOf(PRIMARY_KEY_ID)});
                    if (num != 1) {
                        this.LOG.severe("update line num error= " + num);
                        throw new SQLiteException("[update data] line num error");
                    } else {
                        this.LOG.info("update line num= " + num);
                    }

                    requested = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new SQLiteException("[update data] unexpected exception");
            }

            return serveFile(uri, parms);
        }


        /**
         * Sending files
         *
         * @param uri Requested URL
         * @return Resource
         */
        public Response serveFile(String uri, Map<String, String> parms) {

            SettingData settingData;

            // Get status of THETA
            if (uri.equals("/update_status")) {
                settingData = readSettingData();
                InputStream destInputStream = null;
                try {
                    destInputStream = stringToInputStream(settingData.getStatus());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Response res=newChunkedResponse(Status.OK, "text/html", destInputStream);
                res.addHeader("X-XSS-Protection", "1; mode=block");
                res.addHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'self'");
                res.addHeader("X-Frame-Options", "SAMEORIGIN");
                res.addHeader("X-Content-Type-Options", "nosniff");
                return res;
            } else if (uri.equals("/start_streaming")) {
                // Start and stop streaming
                Intent intent = new Intent(LiveStreamingReceiver.TOGGLE_LIVE_STREAMING);
                context.sendBroadcast(intent);
                return newChunkedResponse(Status.OK, "text/html", null);
            }

            String filename = uri;
            if (uri.substring(0, 1).equals("/")) {
                filename = filename.substring(1);
            }

            AssetManager as = con.getResources().getAssets();
            InputStream fis = null;
            try {
                fis = as.open(filename);
            } catch (Exception e) {

            }

            if (uri.endsWith(".jpg") || uri.endsWith(".JPG")) {
                return newChunkedResponse(Status.OK, "image/jpeg", fis);
            } else if (uri.endsWith(".png") || uri.endsWith(".PNG")) {
                return newChunkedResponse(Status.OK, "image/png", fis);
            } else if (uri.endsWith(".ico")) {
                return newChunkedResponse(Status.OK, "image/x-icon", fis);
            } else if (uri.endsWith(".svg") || uri.endsWith(".SVG")) {
                return newChunkedResponse(Status.OK, "image/svg+xml", fis);
            } else if (uri.endsWith(".js")) {
                Response res= newChunkedResponse(Status.OK, "application/javascript", fis);
                res.addHeader("X-XSS-Protection", "1; mode=block");
                res.addHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'self'");
                res.addHeader("X-Frame-Options", "SAMEORIGIN");
                res.addHeader("X-Content-Type-Options", "nosniff");
                return res;
            } else if (uri.endsWith(".properties")) {
                Response res= newChunkedResponse(Status.OK, "text/html", fis);
                res.addHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'self'");
                return res;
            } else if (uri.endsWith(".css")) {
                return newChunkedResponse(Status.OK, "text/html", fis);
            } else if (uri.endsWith(".html") || uri.endsWith(".htm")) {

                // Read data from DB
                settingData = readSettingData();

                String srcString = null;
                try {
                    srcString = inputStreamToString(fis);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Insert the parameters obtained from the DB
                // Insert by replacing the character string "#JS_INJECTION#" in html
                String JSCode = "\\$(function() {\n"
                    + "\\$('#server_url_text').val('" + settingData.getServerUrl() + "');"
                    + "\\$('#stream_name_text').val('');"  // Don't include plain text of stream name
                    + "\\$('#crypt_text').val('" + settingData.getCryptText() + "');"
                    + "\\$('#encryption_key').val('" + ENCRYPTION_KEY + "');"
                    + "\\$('#encryption_iv').val('" + ENCRYPTION_IV + "');";

                // When the width is 3840, it is judged to be 4 K, and selection of the bit rate of 2 k is made invisible.
                if (settingData.getMovieWidth() == 3840 && settingData.getFps() == 30.0) {
                    JSCode += "\\$('#movie').val('MOVIE4K');";
                    JSCode += "\\$('#bitrate4k').show();";
                    JSCode += "\\$('#bitrate4k_15fps').hide();";
                    JSCode += "\\$('#bitrate2k').hide();";
                    JSCode += "\\$('#bitrate2k_15fps').hide();";
                    JSCode += "\\$('#bitrate1k').hide();";
                    JSCode += "\\$('#bitrate1k_15fps').hide();";
                    JSCode += "\\$('#bitrate06k').hide();";
                    JSCode += "\\$('#bitrate4k').val('" + settingData.getBitRate() + "');";
                } else if (settingData.getMovieWidth() == 3840 && settingData.getFps() == 15.0) {
                    JSCode += "\\$('#movie').val('MOVIE4K_15FPS');";
                    JSCode += "\\$('#bitrate4k').hide();";
                    JSCode += "\\$('#bitrate4k_15fps').show();";
                    JSCode += "\\$('#bitrate2k').hide();";
                    JSCode += "\\$('#bitrate2k_15fps').hide();";
                    JSCode += "\\$('#bitrate1k').hide();";
                    JSCode += "\\$('#bitrate1k_15fps').hide();";
                    JSCode += "\\$('#bitrate06k').hide();";
                    JSCode += "\\$('#bitrate4k_15fps').val('" + settingData.getBitRate() + "');";
                } else if (settingData.getMovieWidth() == 1920 && settingData.getFps() == 30.0) {
                    JSCode += "\\$('#movie').val('MOVIE2K');";
                    JSCode += "\\$('#bitrate4k').hide();";
                    JSCode += "\\$('#bitrate4k_15fps').hide();";
                    JSCode += "\\$('#bitrate2k').show();";
                    JSCode += "\\$('#bitrate2k_15fps').hide();";
                    JSCode += "\\$('#bitrate1k').hide();";
                    JSCode += "\\$('#bitrate1k_15fps').hide();";
                    JSCode += "\\$('#bitrate06k').hide();";
                    JSCode += "\\$('#bitrate2k').val('" + settingData.getBitRate() + "');";
                } else if (settingData.getMovieWidth() == 1920 && settingData.getFps() == 15.0) {
                    JSCode += "\\$('#movie').val('MOVIE2K_15FPS');";
                    JSCode += "\\$('#bitrate4k').hide();";
                    JSCode += "\\$('#bitrate4k_15fps').hide();";
                    JSCode += "\\$('#bitrate2k').hide();";
                    JSCode += "\\$('#bitrate2k_15fps').show();";
                    JSCode += "\\$('#bitrate1k').hide();";
                    JSCode += "\\$('#bitrate1k_15fps').hide();";
                    JSCode += "\\$('#bitrate06k').hide();";
                    JSCode += "\\$('#bitrate2k_15fps').val('" + settingData.getBitRate() + "');";
                } else if (settingData.getMovieWidth() == 1024 && settingData.getFps() == 30.0) {
                    JSCode += "\\$('#movie').val('MOVIE1K');";
                    JSCode += "\\$('#bitrate4k').hide();";
                    JSCode += "\\$('#bitrate4k_15fps').hide();";
                    JSCode += "\\$('#bitrate2k').hide();";
                    JSCode += "\\$('#bitrate2k_15fps').hide();";
                    JSCode += "\\$('#bitrate1k').show();";
                    JSCode += "\\$('#bitrate1k_15fps').hide();";
                    JSCode += "\\$('#bitrate06k').hide();";
                    JSCode += "\\$('#bitrate1k').val('" + settingData.getBitRate() + "');";
                } else if (settingData.getMovieWidth() == 1024 && settingData.getFps() == 15.0) {
                    JSCode += "\\$('#movie').val('MOVIE1K_15FPS');";
                    JSCode += "\\$('#bitrate4k').hide();";
                    JSCode += "\\$('#bitrate4k_15fps').hide();";
                    JSCode += "\\$('#bitrate2k').hide();";
                    JSCode += "\\$('#bitrate2k_15fps').hide();";
                    JSCode += "\\$('#bitrate1k').hide();";
                    JSCode += "\\$('#bitrate1k_15fps').show();";
                    JSCode += "\\$('#bitrate06k').hide();";
                    JSCode += "\\$('#bitrate1k_15fps').val('" + settingData.getBitRate() + "');";
                } else {
                    JSCode += "\\$('#movie').val('MOVIE06K');";
                    JSCode += "\\$('#bitrate4k').hide();";
                    JSCode += "\\$('#bitrate2k').hide();";
                    JSCode += "\\$('#bitrate1k').hide();";
                    JSCode += "\\$('#bitrate06k').show();";
                    JSCode += "\\$('#bitrate06k').val('" + settingData.getBitRate() + "');";
                }

                JSCode += "\\$('#audio_sampling_rate_text').val('" + settingData.getAudioSamplingRate() + "');";

                JSCode += "\\$('#no_operation_timeout_minute_text').val('" + settingData.getNoOperationTimeoutMinute() + "');";

                JSCode += "\\$('#status_label_tmp').text('" + settingData.getStatus() + "');";

                JSCode += "});";
                String newSrcString = srcString.replaceFirst("#JS_INJECTION#", JSCode);

                InputStream destInputStream = null;
                try {
                    destInputStream = stringToInputStream(newSrcString);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                Response res = newChunkedResponse(Status.OK, "text/html", destInputStream);
                res.addHeader("X-XSS-Protection", "1; mode=block");
                res.addHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; frame-ancestors 'self'");
                res.addHeader("X-Frame-Options", "SAMEORIGIN");
                res.addHeader("X-Content-Type-Options", "nosniff");
                return res;
            } else if (uri.endsWith(".json")) {

                // Read data from DB
                settingData = readSettingData();

                InputStream destInputStream = null;
                try {
                    destInputStream = stringToInputStream(settingData.toJson());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return newChunkedResponse(Status.OK, "application/json", destInputStream);
            } else {
                return newFixedLengthResponse(NOT_FOUND, "text/plain", "");
            }
        }

        /**
         * Read configuration data from DB and return
         */
        private SettingData readSettingData() {
            SettingData settingData = null;

            Cursor cursor = dbObject.query("theta360_setting", null, "id=?", new String[]{String.valueOf(PRIMARY_KEY_ID)}, null, null, null, null);
            try {
                settingData = new SettingData();
                if (cursor.moveToNext()) {
                    settingData.setServerUrl(prevent_xss(cursor.getString(cursor.getColumnIndex("server_url"))));
                    settingData.setStreamName(prevent_xss(cursor.getString(cursor.getColumnIndex("stream_name"))));
                    settingData.setCryptText(prevent_xss(cursor.getString(cursor.getColumnIndex("crypt_text"))));
                    settingData.setMovieWidth(Integer.parseInt(prevent_xss(cursor.getString(cursor.getColumnIndex("movie_width")))));
                    settingData.setMovieHeight(Integer.parseInt(prevent_xss(cursor.getString(cursor.getColumnIndex("movie_height")))));
                    settingData.setFps(Double.parseDouble(prevent_xss(cursor.getString(cursor.getColumnIndex("fps")))));
                    settingData.setBitRate(prevent_xss(cursor.getString(cursor.getColumnIndex("bitrate"))));
                    settingData.setAudioSamplingRate(Integer.parseInt(prevent_xss(cursor.getString(cursor.getColumnIndex("audio_sampling_rate")))));
                    settingData.setNoOperationTimeoutMinute(Integer.parseInt(prevent_xss(cursor.getString(cursor.getColumnIndex("no_operation_timeout_minute")))));
                    settingData.setStatus(prevent_xss(cursor.getString(cursor.getColumnIndex("status"))));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new SQLiteException("[select data] Unexpected exception");
            } finally {
                cursor.close();
            }

            return settingData;
        }


    }

    /*
     * Escape XSS attack
     */
    private String prevent_xss(String str){
        StringBuffer safeStr = new StringBuffer();
        for(int i=0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '&':
                    safeStr.append("&amp;");
                    break;
                case '<':
                    safeStr.append("&lt;");
                    break;
                case '>':
                    safeStr.append("&gt;");
                    break;
                case '\"':
                    safeStr.append("&quot;");
                    break;
                case '\'':
                    safeStr.append("&#x27;");
                    break;
                case ' ':
                    safeStr.append("&nbsp;");
                    break;
                default:
                    safeStr.append(c);
                    break;
            }
        }
        return safeStr.toString();
    }
}
