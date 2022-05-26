package com.theta360.pluginlibrary.activity;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ThetaInfo {
    private static final String PROP_A_PATH = "/param/adj/Prop_A.bin";

    /**
     * Get THETA model name
     *
     * @return THETA ModelName (eg. You can get the model name like "RICOH THETA *")
     */
    public static String getThetaModelName() {
        return Build.MODEL;
    }

    /**
     * Get the THETA firmwareversion
     *
     * @return THETA FirmwareVersion
     */
    public static String getThetaFirmwareVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(Constants.RECEPTOR, PackageManager.GET_META_DATA);
            if (packageInfo != null) {
                return packageInfo.versionName;
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        return ".";
    }

    /**
     * Get the THETA SerialNumber
     *
     * @return THETA SerialNumber
     */
    public static long getThetaSerialNumber() {
        long serial = 0;
        try {
            File file = new File(PROP_A_PATH);
            if(file != null && file.exists()) {
                // int(符号付き）をlong(正の整数）へ変換する
                // 単純に(long)キャストすると 0x87654321 → 0xFFFFFFFF87654321になってしまう
                serial = readFile4byte(file, "cameraSerialNumber", 4) & 0x00000000FFFFFFFFL;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serial;
    }

    private static int readFile4byte(File adjFile, String member, int start) throws IOException {
        int result = 0;

        FileInputStream fin = new FileInputStream(adjFile);
        byte[] dates = new byte[120];
        fin.read(dates);
        fin.close();

        byte[] bytes = Arrays.copyOfRange(dates, start, start + 4);
        result = ByteBuffer.wrap(bytes).getInt();

        return result;
    }
}