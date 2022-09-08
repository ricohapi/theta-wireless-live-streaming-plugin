package com.theta360.pluginlibrary.activity;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import com.theta360.pluginlibrary.values.ThetaModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ThetaInfo {
    private static final String RO_PRODUCT_VERSION = "ro.product.version";
    private static final String RO_SERIALNO = "ro.serialno";

    public ThetaInfo() {
    }

    public static String getThetaModelName() {
        return Build.MODEL;
    }

    public static String getThetaFirmwareVersion() {
        String version = getProp("ro.product.version");
        return version.substring(0, 1) + "." + version.substring(1, 3) + "." + version.substring(3, 4);
    }

    public static String getThetaFirmwareVersion(Context context) {
        if (!ThetaModel.isVCameraModel()) {
            return getThetaFirmwareVersion();
        } else {
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo("com.theta360.receptor", 128);
                if (packageInfo != null) {
                    return packageInfo.versionName;
                }
            } catch (NameNotFoundException var2) {
                var2.printStackTrace();
            }

            return ".";
        }
    }

    public static long getThetaSerialNumber() {
        String serialno = getProp("ro.serialno");
        return Long.parseLong(serialno.substring(serialno.length() - 8));
    }

    private static String getProp(String prop) {
        String value = ".";
        Process ifc = null;

        try {
            ifc = Runtime.getRuntime().exec(new String[]{"getprop", prop});
            BufferedReader bis = new BufferedReader(new InputStreamReader(ifc.getInputStream()), 1024);
            value = bis.readLine();
        } catch (IOException var7) {
        } finally {
            if (ifc != null) {
                ifc.destroy();
            }

        }

        return value;
    }
}
