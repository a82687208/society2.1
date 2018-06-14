package com.gdptc.society.manager;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.util.Log;

import com.gdptc.society.Public;
import com.gdptc.society.R;
import com.gdptc.society.ui.activity.AlarmActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

/**
 * Created by hasee on 2017/12/12.
 */

public class DirectoryManager {
    private String PATH_INTERNAL_APP;
    private String PATH_INTERNAL_PIC;
    private String PATH_INTERNAL_IMG_CACHE;
    private String PATH_INTERNAL_VOICE;

    private String PATH_EXTERNAL_APP;
    private String PATH_EXTERNAL_PIC ;
    private String PATH_EXTERNAL_IMG_CACHE;
    private String PATH_EXTERNAL_VOICE;

    private String PATH_DATA_APP;
    private String PATH_DATA_IMG_CACHE;
    private String PATH_DATA_VOICE;
    private String PATH_DATA_VOICE_SYS;

    public final String PATH_SQL;

    private final String CACHE_NAME = "bitmap";
    private final String PIC_NAME = "Image";
    private final String VOICE_NAME = "dat";
    private final String VOICE_SYS = "ttsSys";

    public static final String CFG_INFO = "info";
    public static final String INFO_PHONE = "phone";
    public static final String INFO_QQ = "qqId";
    public static final String INFO_PSW = "psw";
    public static final String INFO_INIT = "registerRecord";
    public static final String INFO_EXIT = "exit";

    public static final String CFG_APP = "setting";
    public static final String APP_THEME = "theme";
    public static final String APP_BOOT = "boot";
    public static final String APP_TTS_INIT = "tts";
    public static final String APP_VOICE_SPEAKER = "VoiceSpeaker";
    public static final String APP_VOICE_TONE = "VoiceTone";
    public static final String APP_VOICE_SPEECH = "VoiceSpeech";
    public static final String APP_INPUT_LINE = "line";
    public static final String APP_INPUT_NUM = "num";
    public static final String APP_PUSH = "push";
    public static final String APP_GREAT = "great";
    public static final String APP_REWARD = "reward";
    public static final String APP_REVIEWS = "reviews";
    public static final String APP_HEADUP = "headUp";

    public static final int externalDiskCacheSize = 500;
    public static final int InternalDiskCacheSize = 300;
    public static final int dataDiskCacheSize = 50;

    public static final int dateLimitSize = 30;
    public static final int dateAlarmSize = 80;
    public static final int allAlarmSize = 200;

    private File internalApp, internalPic, internalCache, internalVoice;
    private File externalApp, externalPic, externalCache, externalVoice;
    private File dataApp, dataCache, dataVoice;

    private File[][] fileTable = new File[3][4];

    private ApplicationManager application;

    DirectoryManager(Context context) {
        PATH_INTERNAL_APP = null;
        PATH_INTERNAL_PIC = null;
        PATH_INTERNAL_IMG_CACHE = null;

        PATH_EXTERNAL_APP = null;
        PATH_EXTERNAL_PIC = null;
        PATH_EXTERNAL_IMG_CACHE = null;

        PATH_DATA_APP = null;
        PATH_DATA_IMG_CACHE = null;

        PATH_SQL = context.getDir("dat", Context.MODE_PRIVATE).getPath();
    }

    DirectoryManager(ApplicationManager applicationManager) {
        this.application = applicationManager;
        PATH_SQL = application.getDir("dat", Context.MODE_PRIVATE).getPath();
        notifyMediaChange();
    }

    private boolean init() {
        String appName = application.getResources().getString(R.string.app_name_en);

        //内置内存
        String internalPath = getStoragePath(application, false);
        File internalFile = Environment.getExternalStorageDirectory();      //部分系统会返回外置内存卡，无法用于分辨内外置路径
        File internalCacheFile = application.getExternalCacheDir();
        File dataFile = application.getCacheDir();

        //如果无法获取准确的内置内存路径则使用系统返回的路径
        if (internalPath == null) {
            if (internalFile != null) {
                PATH_INTERNAL_APP = internalFile.getPath() + "/" + appName;
                PATH_INTERNAL_PIC = PATH_INTERNAL_APP + "/" + PIC_NAME;
                PATH_INTERNAL_VOICE = PATH_INTERNAL_APP + "/" + VOICE_NAME;

                internalApp = new File(PATH_INTERNAL_APP);
                internalPic = new File(PATH_INTERNAL_PIC);
                internalVoice = new File(PATH_INTERNAL_VOICE);
            }
            else {
                PATH_INTERNAL_APP = null;
                PATH_INTERNAL_PIC = null;
                PATH_INTERNAL_VOICE = null;
            }
            if (internalCacheFile != null) {
                PATH_INTERNAL_IMG_CACHE = internalCacheFile.getPath() + "/" + appName;
                internalPic = new File(PATH_INTERNAL_IMG_CACHE);
            }
            else
                PATH_INTERNAL_IMG_CACHE = null;
        }
        else {
            PATH_INTERNAL_IMG_CACHE = internalPath + "/Android/data/" + application.getPackageName() + "/cache" + "/" + CACHE_NAME;
            PATH_INTERNAL_APP = internalPath + "/" + appName;
            PATH_INTERNAL_PIC = PATH_INTERNAL_APP + "/" + PIC_NAME;
            PATH_INTERNAL_VOICE = PATH_INTERNAL_APP + "/" + VOICE_NAME;

            internalApp = new File(PATH_INTERNAL_APP);
            internalPic = new File(PATH_INTERNAL_PIC);
            internalCache = new File(PATH_INTERNAL_IMG_CACHE);
            internalVoice = new File(PATH_INTERNAL_VOICE);
        }

        //外置内存卡
        String externalPath = getStoragePath(application, true);
        if (externalPath != null) {
            PATH_EXTERNAL_IMG_CACHE = externalPath + "/Android/data/" + application.getPackageName() + "/cache" + "/" + CACHE_NAME;
            PATH_EXTERNAL_APP = externalPath + "/" + appName;
            PATH_EXTERNAL_PIC = PATH_EXTERNAL_APP + "/" + PIC_NAME;
            PATH_EXTERNAL_VOICE = PATH_EXTERNAL_APP + "/" + VOICE_NAME;

            externalApp = new File(PATH_EXTERNAL_APP);
            externalPic = new File(PATH_EXTERNAL_PIC);
            externalCache = new File(PATH_EXTERNAL_IMG_CACHE);
            externalVoice = new File(PATH_EXTERNAL_VOICE);
        }
        else {
            PATH_EXTERNAL_APP = null;
            PATH_EXTERNAL_PIC = null;
            PATH_EXTERNAL_IMG_CACHE = null;
            PATH_EXTERNAL_VOICE = null;
        }

        //data分区
        if (dataFile == null) {
            PATH_DATA_APP = null;
            PATH_DATA_IMG_CACHE = null;
            PATH_DATA_VOICE = null;
            //error
            Intent intent = new Intent(application, AlarmActivity.class);
            intent.putExtra(Public.TITLE, application.getResources().getString(R.string.file_error_title));
            intent.putExtra(Public.MSG, application.getResources().getString(R.string.file_error_msg));
            intent.putExtra(Public.EXIT, true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            application.pauseActivity(AlarmActivity.class);
            application.startActivity(intent);
            return false;
        }
        else {
            PATH_DATA_APP = dataFile.getPath();
            PATH_DATA_IMG_CACHE = PATH_DATA_APP + "/" + CACHE_NAME;
            PATH_DATA_VOICE = PATH_DATA_APP + "/" + VOICE_NAME;
            PATH_DATA_VOICE_SYS = PATH_DATA_APP + "/" + VOICE_SYS;

            dataApp = new File(PATH_DATA_APP);
            dataCache = new File(PATH_DATA_IMG_CACHE);
            dataVoice = new File(PATH_DATA_VOICE);

            long total = getTotalBlock(PATH_DATA_APP) / 1024;

            if (total < dateAlarmSize) {
                Intent intent = new Intent(application, AlarmActivity.class);
                intent.putExtra(Public.TITLE, application.getResources().getString(R.string.file_alarm_title));
                intent.putExtra(Public.MSG, application.getResources().getString(R.string.file_date_alarm_msg));
                intent.putExtra(Public.EXIT, false);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                application.startActivity(intent);
            }
            else if (total < dateLimitSize) {
                Intent intent = new Intent(application, AlarmActivity.class);
                intent.putExtra(Public.TITLE, application.getResources().getString(R.string.file_limit_title));
                intent.putExtra(Public.MSG, application.getResources().getString(R.string.file_limit_msg));
                intent.putExtra(Public.EXIT, true);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                application.pauseActivity(AlarmActivity.class);
                application.startActivity(intent);
                return false;
            }
            else if (total >= dateLimitSize) {
                File file = new File(PATH_DATA_VOICE_SYS);
                if (checkOrCreateDirectory(file)) {

                }
            }
        }

        //优先使用外置内存，其次内置内存，data分区压底
        fileTable[0][0] = externalApp;
        fileTable[0][1] = externalCache;
        fileTable[0][2] = externalVoice;
        fileTable[0][3] = externalPic;
        fileTable[1][0] = internalApp;
        fileTable[1][1] = internalCache;
        fileTable[1][2] = internalVoice;
        fileTable[1][3] = internalPic;
        fileTable[2][0] = dataApp;
        fileTable[2][1] = dataCache;
        fileTable[2][2] = dataVoice;

        return true;
    }

    //rootPath：需要计算的内存路径
    //比如内存卡：传入/storage/sdcard
    //比如系统分区（data）：传入/data
    //返回单位为KB
    public long getTotalBlock(String rootPath) {
        StatFs statFs = new StatFs(rootPath);
        long size = Build.VERSION.SDK_INT >= 18 ? statFs.getBlockSizeLong() * statFs.getAvailableBlocksLong()
                : statFs.getBlockSize() * statFs.getAvailableBlocks();
        return size / 1024;
    }

    public void checkSelf() {
        for (int i = 0; i < fileTable.length; ++i) {
            for (int j = 0; j < fileTable.length; ++j)
                checkOrCreateDirectory(fileTable[i][j]);
        }

        int total = 0;
        if (internalApp != null && internalApp.exists())
            total += getTotalBlock(PATH_INTERNAL_APP) / 1024;
        if (externalApp != null && externalApp.exists())
            total += getTotalBlock(PATH_EXTERNAL_APP) / 1024;
        if (dataApp != null && dataApp.exists())
            total += getTotalBlock(PATH_DATA_APP) / 1024;

        if (total < dateLimitSize) {
            Intent intent = new Intent(application, AlarmActivity.class);
            intent.putExtra(Public.TITLE, application.getResources().getString(R.string.file_limit_title));
            intent.putExtra(Public.MSG, application.getResources().getString(R.string.file_limit_msg));
            intent.putExtra(Public.EXIT, true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            application.pauseActivity(AlarmActivity.class);
            application.startActivity(intent);
        }
        else if (total < allAlarmSize) {
            Intent intent = new Intent(application, AlarmActivity.class);
            intent.putExtra(Public.TITLE, application.getResources().getString(R.string.file_alarm_title));
            intent.putExtra(Public.MSG, application.getResources().getString(R.string.file_alarm_msg));
            intent.putExtra(Public.EXIT, false);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            application.startActivity(intent);
        }
    }

    public static boolean copyAssets(Context context, String directoryPath, String fileName) throws IOException {
        boolean success = false;
        new File(directoryPath).mkdirs();
        File file = new File(directoryPath + "/" + fileName);
        AssetManager assetManager = context.getAssets();
        BufferedOutputStream outputStream = null;
        BufferedInputStream inputStream = null;

        try {
            if (file.exists()) {
                if (file.isDirectory()) {
                    if (file.delete() && file.createNewFile()) {
                        inputStream = new BufferedInputStream(assetManager.open(fileName));
                        outputStream = new BufferedOutputStream(new FileOutputStream(file));

                        int bit;
                        while ((bit = inputStream.read()) != -1)
                            outputStream.write(bit);

                        inputStream.close();
                        outputStream.flush();
                        outputStream.close();
                        success = true;
                    }
                }
                else
                    success = true;
            }
            else if (file.createNewFile()) {
                    inputStream = new BufferedInputStream(assetManager.open(fileName));
                    outputStream = new BufferedOutputStream(new FileOutputStream(file));

                    int bit;
                    while ((bit = inputStream.read()) != -1)
                        outputStream.write(bit);

                    inputStream.close();
                    outputStream.flush();
                    outputStream.close();
                    success = true;
                }
        }
        finally {
            if (inputStream != null)
                inputStream.close();
            if (outputStream != null)
                outputStream.close();
        }

        return success;
    }

    public boolean checkOrCreateDirectory(File file) {
        if (file == null)
            return false;

        if (file.exists()) {
            if (!file.isDirectory()) {
                file.delete();
                return file.mkdirs();
            }
            else
                return true;
        }
        else
            return file.mkdirs();
    }

    public boolean checkOrCreateFile(File file) {
        if (file == null)
            return false;

        try {
            if (file.exists()) {
                if (file.isDirectory()) {
                    file.delete();
                    return file.createNewFile();
                }
                else
                    return true;
            }
            else
                return file.createNewFile();
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getInternalAppPath() {
        return PATH_INTERNAL_APP;
    }

    public String getInternalPicPath() {
        return PATH_INTERNAL_PIC;
    }

    public String getInternalCachePath() {
        return PATH_INTERNAL_IMG_CACHE;
    }

    public String getInternalVoicePath() { return PATH_INTERNAL_VOICE; }

    public String getExternalAppPath() {
        return PATH_EXTERNAL_APP;
    }

    public String getExternalPicPath() {
        return PATH_EXTERNAL_PIC;
    }

    public String getExternalCachePath() {
        return PATH_EXTERNAL_IMG_CACHE;
    }

    public String getExternalVoicePath() { return PATH_EXTERNAL_VOICE; }

    public String getDataAppPath() {
        return PATH_DATA_APP;
    }

    public String getDataCachePath() {
        return PATH_DATA_IMG_CACHE;
    }

    public String getDataVoicePath() { return PATH_DATA_VOICE; }

    public String getVoiceSysPath() { return PATH_DATA_VOICE_SYS; }

    public String getSafeAppPath() {
        for (int i = 0; i < fileTable.length; ++i)
                if (checkOrCreateDirectory(fileTable[i][0]))
                    return fileTable[i][0].getPath();

        return null;
    }

    public String getSafePicPath() {
        for (int i = 0; i < fileTable.length; ++i)
                if (checkOrCreateDirectory(fileTable[i][3]))
                    return fileTable[i][3].getPath();

        return null;
    }

    public String getSafeCachePath() {
        for (int i = 0; i < fileTable.length; ++i)
                if (checkOrCreateDirectory(fileTable[i][1]))
                    return fileTable[i][1].getPath();

        return null;
    }

    public String getSafeVoicePath() {
        for (int i = 0; i < fileTable.length; ++i)
            if (checkOrCreateDirectory(fileTable[i][2]))
                return fileTable[i][2].getPath();

        return null;
    }

    public void notifyMediaChange() {
        if (init())
            checkSelf();
    }

    public boolean checkFile(String path) {
        File file = new File(path);
        return file.exists() && file.isFile();
    }

    public boolean checkDistrory(String path) {
        File file = new File(path);
        return file.exists() && file.isDirectory();
    }

    private static String getStoragePath(Context mContext, boolean is_removale) {
        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (is_removale == removable) {
                    return path;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
