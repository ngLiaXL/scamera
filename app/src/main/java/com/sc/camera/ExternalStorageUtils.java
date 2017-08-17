/**
 *
 */
package com.sc.camera;

import android.os.Environment;
import android.text.TextUtils;

import java.io.File;


public class ExternalStorageUtils {

    static final String MEIDA_DRI = "a";
    static final String SEC_DIR = "b";

    static boolean mExternalStorageAvailable = false;
    static boolean mExternalStorageWriteable = false;

    /**
     *
     */
    private ExternalStorageUtils() {
    }

    public static File getDiskCacheDir(String fileName) {
        String file = ExternalStorageUtils.getAppExternalStorage(fileName, SEC_DIR);
        if (!TextUtils.isEmpty(file)) {
            return new File(file);
        }
        return null;
    }


    public static void updateExternalStorageState() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
    }

    public static File getAppExternalStorage() {
        updateExternalStorageState();
        if (mExternalStorageAvailable && mExternalStorageWriteable) {
            return Environment.getExternalStoragePublicDirectory(MEIDA_DRI);
        }

        File externalStorage = MainApp.getContext().getExternalFilesDir(MEIDA_DRI);
        if (externalStorage != null && externalStorage.isDirectory()) {
            return externalStorage;
        }
        return null;
    }

    public static String getAppExternalStorage(String fileName, String parentName) {
        File appExtraRootFile = new File(getAppExternalStorage() + File.separator + parentName);
        if (!appExtraRootFile.exists()) {
            if (!makeDir(appExtraRootFile)) {
                return null;
            }
        }
        return appExtraRootFile.getAbsolutePath() + File.separator + fileName;
    }

    public static String getAppPirvateStorage(String fileName, String parentName) {
        File appExtraRootFile = new File(MainApp.getContext().getFilesDir().getAbsolutePath()
                + File.separator + parentName);
        if (!appExtraRootFile.exists()) {
            if (!makeDir(appExtraRootFile)) {
                return null;
            }
        }
        return appExtraRootFile.getAbsolutePath() + File.separator + fileName;
    }

    public static String getAppPirvateDir(String fileType) {
        File appExtraRootFile = new File(MainApp.getContext().getFilesDir().getAbsolutePath()
                + File.separator + fileType);
        if (!appExtraRootFile.exists()) {
            if (!makeDir(appExtraRootFile)) {
                return null;
            }
        }
        return appExtraRootFile.getAbsolutePath() + File.separator;
    }

    public static String getApkExtraFilePath(String fileName) {
        File appExtraRootFile = new File(MainApp.getContext().getFilesDir() + File.separator);
        if (!appExtraRootFile.exists()) {
            if (!makeDir(appExtraRootFile)) {
                return null;
            }
        }
        return appExtraRootFile.getAbsolutePath() + File.separator + fileName;
    }

    static boolean makeDir(File dir) {
        if (!dir.getParentFile().exists()) {
            makeDir(dir.getParentFile());
        }
        return dir.mkdir();
    }

}
