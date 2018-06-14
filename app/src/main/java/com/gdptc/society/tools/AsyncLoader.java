package com.gdptc.society.tools;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.gdptc.society.Public;
import com.gdptc.society.R;
import com.gdptc.society.base.BaseActivity;
import com.gdptc.society.manager.DBManager;
import com.gdptc.society.ui.activity.AlarmActivity;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.gdptc.society.apiServer.ApiParam.TIMEOUT;
import static com.gdptc.society.manager.ApplicationManager.directoryManager;
import static com.gdptc.society.manager.ApplicationManager.memoryManager;

/**
 * Created by Administrator on 2017/7/28/028.
 */

/*
 * 加载策略：一次加载，根据需求切成多种形态
 * 缓存策略：保存同一张图片的多种不同形态的位图
 * 硬盘缓存策略：存原图
 */

public class AsyncLoader {
    private final Handler handler;
    private final QUALITY quality;                                                              //是否启用加载完全相同分辨率模式

    private final ImageUtil.MODE[] modes = { ImageUtil.MODE.NORMAL, ImageUtil.MODE.ROUND,
            ImageUtil.MODE.NORMAL_BORDER, ImageUtil.MODE.ROUND_BORDER };

    private static LruCache<Key, BitmapInfo> imageCache;                                        //物理一级缓存
    private static LruCache<Key, BitmapInfo> snapBitmaps;                                       //临时二级缓存（减轻CPU硬盘压力，减少GC次数）
    private static Set<SoftReference<BitmapInfo>> reusableBitmaps;                              //复用三级缓存（减少GC次数，平缓内存波动）
    private static DiskLruCache internalCache;                                                  //本地内置四级缓存
    private static DiskLruCache externalCache;                                                  //本地外置四级缓存
    private static DiskLruCache dateCache;                                                      //本地分区四级缓存
    private static ConcurrentHashMap<Object, BitmapInfo> fullSizeInfo;                          //记录原图分辨率

    // 为了防止影响上一个界面的图片内存，登记在线Activity
    // 不在线则直接复用图片内存，在线则进行bitmap引用的搜索
    // 确定bitmap无引用后复用该内存，减少一次GC内存分配操作
    private static Set<FragmentActivity> activitySet;
    private static MediaReceiver mediaReceiver;

    private Map<Object, LoadRunnable> taskMap;
    private Map<Object, ConcurrentHashMap<Key, List<WeakReference<Object>>>> taskListener;
    private ImageLoadListener mImageLoadListener;
    private Resources resources;
    private FragmentActivity activity;

    private ContentResolver contentResolver;

    private DBManager.NetworkDBAdapter dbAdapter;

    private boolean pause = false;
    private int threadCount = 0;

    private ExecutorService executorService;
    private DisplayMetrics displayMetrics;

    public enum QUALITY {
        //PROBABLY：加载大概尺寸（大于指定尺寸直接返回，否则返回最低限度不小于指定尺寸一半的图片）
        PROBABLY_SIZE(), FULL_SIZE()
    }

    public abstract static class ImageLoadListener {
        public abstract void onImageLoadDone(Object parent, Object id, BitmapInfo bitmapInfo, int width, int height, ImageUtil.MODE mode);
        public abstract void onImageLoadFailure(Object parent, Object id, int width, int height, ImageUtil.MODE mode, Exception e);
        public void onAllImageLoadDone() {}
    }

    private class MediaReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(AsyncLoader.class.getSimpleName(), intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED) ? "挂载" : "移除");
            directoryManager.notifyMediaChange();
            if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                try {
                    if (internalCache != null && !internalCache.isClosed())
                        internalCache.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (externalCache != null && !externalCache.isClosed())
                        externalCache.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else
                initDiskCache();
        }
    }

    static {
        Log.e(AsyncLoader.class.getSimpleName(), "APP内存上限: " + (int) (memoryManager.getMaxMemory() / 1024) + "MB");
        long cacheSize = (long) (memoryManager.getProcessFreeMemory() / 2f);
        long snapSize = (long) (cacheSize * 0.2f);
        cacheSize -= snapSize;
        Log.e(AsyncLoader.class.getSimpleName(), "动态缓存大小: " + cacheSize / 1024 + "MB");
        Log.e(AsyncLoader.class.getSimpleName(), "临时缓存大小: " + snapSize / 1024 + "MB");
        imageCache = new LruCache<Key, BitmapInfo>((int) cacheSize) {
            @Override
            protected int sizeOf(Key key, BitmapInfo value) {
                float size;
                if (Build.VERSION.SDK_INT >= 19)
                    size = value.bitmap.getAllocationByteCount() / 1024f;
                else
                    size = value.bitmap.getByteCount() / 1024f;
                if (size == 0 || size % 1 != 0)
                    size += 1;

                return (int) size;
            }

            @Override
            protected void entryRemoved(boolean evicted, Key key, BitmapInfo oldValue, BitmapInfo newValue) {
                reusableBitmaps.add(new SoftReference<>(oldValue));
                fullSizeInfo.remove(key.id);
                snapBitmaps.remove(key);
            }
        };
        snapBitmaps = new LruCache<Key, BitmapInfo>((int) snapSize) {
            @Override
            protected int sizeOf(Key key, BitmapInfo value) {
                float size;
                if (Build.VERSION.SDK_INT >= 19)
                    size = value.bitmap.getAllocationByteCount() / 1024f;
                else
                    size = value.bitmap.getByteCount() / 1024f;
                if (size == 0 || size % 1 != 0)
                    size += 1;

                return (int) size;
            }
        };
        reusableBitmaps = new HashSet<>();
        activitySet = new HashSet<>();
        fullSizeInfo = new ConcurrentHashMap<>();
    }

    public AsyncLoader(FragmentActivity activity, int threadCount) {
        this(activity, threadCount, QUALITY.PROBABLY_SIZE);
    }

    public AsyncLoader(FragmentActivity activity, int threadCount, QUALITY quality) {
        if (mediaReceiver == null) {
            IntentFilter filter = new IntentFilter();
            //sd卡被插入，且已经挂载
            filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            //SD卡移除
            filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            filter.addDataScheme("file");
            mediaReceiver = new MediaReceiver();
            activity.getApplication().registerReceiver(mediaReceiver, filter);
        }
        if (threadCount <= 0)
            threadCount = 1;
        this.threadCount = threadCount;
        this.activity = activity;
        handler = new Handler();
        executorService = Executors.newFixedThreadPool(threadCount);
        contentResolver = activity.getContentResolver();
        taskMap = new ConcurrentHashMap<>();
        taskListener = new ConcurrentHashMap<>();
        resources = this.activity.getResources();
        displayMetrics = resources.getDisplayMetrics();
        initDiskCache();
        if ((internalCache == null || internalCache.isClosed())
                && (externalCache == null || externalCache.isClosed())
                && (dateCache == null || dateCache.isClosed())) {
            Intent intent = new Intent(activity, AlarmActivity.class);
            intent.putExtra(Public.TITLE, resources.getString(R.string.file_error_title));
            intent.putExtra(Public.MSG, resources.getString(R.string.file_error_msg));
            intent.putExtra(Public.EXIT, true);
            activity.startActivity(intent);
        }
        this.quality = quality;
        if (activity instanceof BaseActivity) {
            PeriodicMonitoring periodicMonitoring = new PeriodicMonitoring();
            periodicMonitoring.asyncLoader = this;
            ((BaseActivity) activity).registerMonitoring(periodicMonitoring);
        }
        else {
            ViewGroup viewGroup = (ViewGroup) activity.findViewById(android.R.id.content);
            FrameLayout frameLayout = viewGroup.findViewById(R.id.asyncLoader_fragment);
            if (frameLayout == null) {
                frameLayout = new FrameLayout(activity);
                frameLayout.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
                frameLayout.setId(R.id.asyncLoader_fragment);
                frameLayout.setFocusable(false);
                frameLayout.setFocusableInTouchMode(false);
                viewGroup.addView(frameLayout);
            }
            FragmentListener fragmentListener = new FragmentListener();
            fragmentListener.asyncLoader = this;
            FragmentManager fm = activity.getSupportFragmentManager();
            fm.beginTransaction().add(R.id.asyncLoader_fragment, fragmentListener).commit();
            activitySet.add(activity);
        }
    }

    public AsyncLoader(FragmentActivity activity, int threadCount, DBManager.NetworkDBAdapter dbAdapter) {
        this(activity, threadCount, dbAdapter, QUALITY.PROBABLY_SIZE);
    }

    public AsyncLoader(FragmentActivity activity, int threadCount, DBManager.NetworkDBAdapter dbAdapter, QUALITY quality) {
        this(activity, threadCount, quality);
        this.dbAdapter = dbAdapter;
        pause = true;
        dbAdapter.addCallBack(dbResultListener);
    }

    private static void initDiskCache() {
        String path;
        if (internalCache == null || internalCache.isClosed()) {
            try {
                path = directoryManager.getInternalCachePath();
                if (path != null)
                    internalCache = DiskLruCache.open(new File(path), 1, 1,
                            directoryManager.externalDiskCacheSize * 1024 * 1024);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (externalCache == null || externalCache.isClosed()) {
            try {
                path = directoryManager.getExternalCachePath();
                if (path != null)
                    externalCache = DiskLruCache.open(new File(path), 1, 1,
                            directoryManager.InternalDiskCacheSize * 1024 * 1024);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (dateCache == null || dateCache.isClosed()) {
            try {
                path = directoryManager.getDataCachePath();
                if (path != null)
                    dateCache = DiskLruCache.open(new File(path), 1, 1,
                            directoryManager.dataDiskCacheSize * 1024 * 1024);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean deleteDiskCache(String id) throws IOException {
        if (internalCache != null)
            return internalCache.remove(id);
        else
            return false;
    }

    public static void clearSnapBitmap() {
        snapBitmaps.evictAll();
    }

    public void setImageLoadListener(ImageLoadListener imageLoadListener) {
        mImageLoadListener = imageLoadListener;
    }

    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    public void pauseRunTask() {
        pause = true;
    }

    public void cancelTask(Object parent, Object id, ImageUtil.MODE mode, int width, int height) {
        if (pause) {
            Key key = getKey(id, mode, width, height);
            LoadRunnable loadRunnable = taskMap.get(id);
            if (loadRunnable != null && loadRunnable.future == null) {
                ConcurrentHashMap<Key, List<WeakReference<Object>>> listener = taskListener.get(id);
                List<WeakReference<Object>> parents = null;
                if (listener != null)
                    parents = listener.get(key);

                if (listener == null || parents == null || parents.size() == 0)
                    taskMap.remove(id);
                else {
                    Iterator<WeakReference<Object>> iterator = parents.iterator();
                    while (iterator.hasNext()) {
                        Object p = iterator.next();
                        if (p == null || p.equals(parent))
                            iterator.remove();
                    }
                }
            }
        }
    }

    public void restartLoader() {
        if (isShutdown())
            executorService = Executors.newFixedThreadPool(threadCount);
    }

    public void exitLoader() {
        if (!isShutdown())
            executorService.shutdownNow();
        handler.removeCallbacksAndMessages(null);
    }

    public void closeCache() {
        if (imageCache != null)
            imageCache.evictAll();
        if (internalCache != null)
            try {
                internalCache.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void restartAllTask() {
        if (pause) {
            for (LoadRunnable runnable : taskMap.values()) {
                if (runnable.future == null)                             //提交尚未完成的任务
                    runnable.future = executorService.submit(runnable);
            }
            pause = false;
        }
    }

    public static String getCachePath(Object id) {
        String key = id.toString();
        int indexStart = key.lastIndexOf("/");
        int indexEnd = key.lastIndexOf(".");

        if (indexStart != -1 && indexEnd != -1)
            key = key.substring(indexStart + 1, indexEnd);
        else if (indexEnd != -1)
            key = key.substring(0, indexEnd);

        String externalPath = directoryManager.getExternalCachePath() + "/" + key + ".0";
        String internalPath = directoryManager.getInternalCachePath() + "/" + key + ".0";
        String dataPath = directoryManager.getDataCachePath() + "/" + key + ".0";

        if (directoryManager.checkFile(externalPath))
            return externalPath;
        else if (directoryManager.checkFile(internalPath))
            return internalPath;
        else if (directoryManager.checkFile(dataPath))
            return dataPath;
        else
            return null;
    }

    public void saveBitmapToLru(Object id, BitmapInfo bitmapInfo, int width, int height, ImageUtil.MODE mode) {
        saveBitmapToLru(id, bitmapInfo, width, height, mode, quality);
    }

    public static void saveBitmapToLru(Object id, BitmapInfo bitmapInfo, int width, int height, ImageUtil.MODE mode, QUALITY quality) {
        if (id == null || mode == null || bitmapInfo == null || bitmapInfo.bitmap == null)
            return;

        Key key;
        if (bitmapInfo.bitmap.getWidth() == bitmapInfo.sourceWidth
                && bitmapInfo.bitmap.getHeight() == bitmapInfo.sourceHeight) {
            if (!fullSizeInfo.containsKey(id)) {
                BitmapInfo sizeInfo = new BitmapInfo();
                sizeInfo.sourceHeight = bitmapInfo.sourceHeight;
                sizeInfo.sourceWidth = bitmapInfo.sourceWidth;
                fullSizeInfo.put(id, sizeInfo);
            }

            key = getKey(id, mode, width, height, QUALITY.FULL_SIZE);
        }
        else
            key = getKey(id, mode, width, height, quality);

        BitmapInfo info = imageCache.get(key);
        if (info != null) {
            if (info.id.equals(id)) {
                int sourceSize = info.bitmap.getWidth() * info.bitmap.getHeight();
                int targetSize = bitmapInfo.bitmap.getWidth() * bitmapInfo.bitmap.getHeight();

                if (targetSize > sourceSize) {
                    imageCache.remove(key);
                    imageCache.put(key, bitmapInfo);
                }
            }
            else {
                Log.e(AsyncLoader.class.getSimpleName(), "WARNING: HashCode发生碰撞，拒绝缓存");
                Log.e(AsyncLoader.class.getSimpleName(), "碰撞ID: " + id.toString());
            }
        }
        else
            imageCache.put(key, bitmapInfo);

    }

    public static void saveBitmapToDisk(String path) {
        try {
            FileInputStream inputStream = new FileInputStream(path);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(inputStream.available());
            int bit;
            while ((bit = inputStream.read()) != -1)
                outputStream.write(bit);
            outputStream.flush();
            saveBitmapToDisk(path, outputStream.toByteArray(), false);
            outputStream.close();
            inputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveBitmapToDisk(String path, byte[] bits, boolean isKey) {
        boolean isSave = false;
        DiskLruCache.Editor editor;
        OutputStream outputStream;
        String key;

        if (isKey)
            key = path;
        else
            key = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));

        if (externalCache != null && !externalCache.isClosed()) {
            try {
                editor = externalCache.edit(key);
                outputStream = editor.newOutputStream(0);
                outputStream.write(bits);                               //存原图
                outputStream.flush();
                editor.commit();
                outputStream.close();
                externalCache.flush();
                isSave = true;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!isSave && internalCache != null && !internalCache.isClosed()) {
            try {
                editor = internalCache.edit(key);
                outputStream = editor.newOutputStream(0);
                outputStream.write(bits);                               //存原图
                outputStream.flush();
                editor.commit();
                outputStream.close();
                internalCache.flush();
                isSave = true;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!isSave && dateCache != null && !dateCache.isClosed()) {
            try {
                editor = dateCache.edit(key);
                outputStream = editor.newOutputStream(0);
                outputStream.write(bits);                               //存原图
                outputStream.flush();
                editor.commit();
                outputStream.close();
                dateCache.flush();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class LoadRunnable implements Runnable {
        final static int TYPE_NETWORK = 0;
        final static int TYPE_LOCAL = 1;
        final static int TYPE_MINI = 2;
        final static int TYPE_RESOURCE = 3;
        final static int TYPE_DB = 4;

        final Lock taskLock = new ReentrantLock();
        final Lock sizeLock = new ReentrantLock();

        WeakReference<Object> parent;
        Object id;
        int type;
        int width, height;
        ImageUtil.MODE mode;
        Future future = null;
        Bitmap bitmap = null;
        DiskLruCache tmpDisk;

        float[] borderSize;
        int[] color;

        BitmapFactory.Options options;
        BitmapInfo bitmapInfo;

        int sourceWidth, sourceHeight;

        @Override
        public void run() {
            options = new BitmapFactory.Options();
            options.inMutable = true;
            options.inScaled = false;
            options.inSampleSize = 1;

            if (bitmap != null)
                Log.e(AsyncLoader.class.getSimpleName(), "复用临时缓存");

            if (bitmap == null) {
                switch (type) {
                    case TYPE_NETWORK:
                        String url = (String) id;
                        String name = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
                        try {
                            options.inJustDecodeBounds = true;
                            FileInputStream fileInputStream = loadDiskCache(name);
                            if (fileInputStream != null) {
                                FileDescriptor fileDescriptor = fileInputStream.getFD();
                                BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
                                sourceWidth = options.outWidth;
                                sourceHeight = options.outHeight;
                                sizeLock.lock();
                                options.inSampleSize = ImageUtil.getScale(options, width, height);
                                options.inJustDecodeBounds = false;
                                options.inBitmap = getInBitmap(options);
                                bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

                                if (bitmap == null && options.inBitmap != null) {
                                    options.inBitmap = null;
                                    bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
                                }
                                if (bitmap == null) {
                                    sizeLock.unlock();
                                    tmpDisk.remove(name);
                                }

                                fileInputStream.close();
                            }
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                            sizeLock.unlock();
                        }

                        if (bitmap == null) {
                            options.inJustDecodeBounds = true;
                            byte[] img = getImgFromUrl(url);
                            if (img != null && img.length > 0) {
                                BitmapFactory.decodeByteArray(img, 0, img.length, options);
                                if (options.outHeight > 0 && options.outWidth > 0) {          //解码成功才保存，减少脏数据
                                    saveBitmapToDisk(name, img, true);
                                    sourceWidth = options.outWidth;
                                    sourceHeight = options.outHeight;
                                    sizeLock.lock();
                                    options.inSampleSize = ImageUtil.getScale(options, width, height);    //返回压缩后的图片
                                    options.inJustDecodeBounds = false;
                                    options.inBitmap = getInBitmap(options);
                                    bitmap = BitmapFactory.decodeByteArray(img, 0, img.length, options);

                                    if (bitmap == null && options.inBitmap != null) {
                                        options.inBitmap = null;
                                        bitmap = BitmapFactory.decodeByteArray(img, 0, img.length, options);
                                    }
                                }
                            }
                        }
                        break;
                    case TYPE_LOCAL:
                        File file = new File((String) id);
                        FileInputStream inputStream;
                        FileDescriptor fileDescriptor = null;
                        try {
                            inputStream = new FileInputStream(file);
                            fileDescriptor = inputStream.getFD();
                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);   //只获取到尺寸信息
                            if (options.outHeight > 0 && options.outWidth > 0) {
                                sourceHeight = options.outHeight;
                                sourceWidth = options.outWidth;
                                sizeLock.lock();
                                options.inSampleSize = ImageUtil.getScale(options, width, height);
                                options.inBitmap = getInBitmap(options);
                                options.inJustDecodeBounds = false;
                                bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

                                if (bitmap == null && options.inBitmap != null) {
                                    options.inBitmap = null;
                                    bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
                                }
                            }

                            inputStream.close();
                        }
                        catch (Exception e) {
                            postLoadFailure(parent, id, width, height, mode, e);
//                            if (bitmap == null && options.inBitmap != null) {
//                                Log.e(AsyncLoader.class.getSimpleName(), "source: " + sourceWidth / options.inSampleSize + " " + sourceHeight / options.inSampleSize);
//                                Log.d(AsyncLoader.class.getSimpleName(), "target: " + options.inBitmap.getWidth() + " " + options.inBitmap.getHeight());
//                                Log.d(AsyncLoader.class.getSimpleName(), "scale: " + options.inSampleSize);
//                                options.inBitmap = null;
//                                bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
//                                Log.d(AsyncLoader.class.getSimpleName(), "source: " + bitmap.getWidth() + " " + bitmap.getHeight());
//                            }
                        }
                        break;
                    case TYPE_MINI:
                        Cursor cursor = contentResolver.query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                                new String[]{ MediaStore.Images.Thumbnails.WIDTH, MediaStore.Images.Thumbnails.HEIGHT },
                                MediaStore.Images.Thumbnails.IMAGE_ID + "=" + this.id, null, null);
                        if (cursor != null) {
                            if (cursor.moveToNext()) {
                                options.outWidth = cursor.getInt(0);
                                options.outHeight = cursor.getInt(1);
                            }
                            cursor.close();
                        }

                        sizeLock.lock();
                        options.inSampleSize = ImageUtil.getScale(options, width, height);
                        options.inBitmap = getInBitmap(options);

                        bitmap = MediaStore.Images.Thumbnails.getThumbnail(
                                activity.getContentResolver(), (Long) this.id,
                                MediaStore.Images.Thumbnails.MINI_KIND, options);

                        if (bitmap == null && options.inBitmap != null) {
                            options.inBitmap = null;
                            bitmap = MediaStore.Images.Thumbnails.getThumbnail(
                                    activity.getContentResolver(), (Long) this.id,
                                    MediaStore.Images.Thumbnails.MINI_KIND, options);
                        }

                        break;
                    case TYPE_RESOURCE: {
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeResource(resources, (int) id, options);
                        sourceWidth = options.outWidth;
                        sourceHeight = options.outHeight;
                        sizeLock.lock();
                        options.inSampleSize = ImageUtil.getScale(options, width, height);
                        options.inBitmap = getInBitmap(options);
                        options.inJustDecodeBounds = false;
                        bitmap = BitmapFactory.decodeResource(resources, (int) id, options);

                        if (bitmap == null && options.inBitmap != null) {
                            options.inBitmap = null;
                            bitmap = BitmapFactory.decodeResource(resources, (int) id, options);
                        }
                    }
                    break;
                    case TYPE_DB:
                        String id = String.valueOf(this.id);
                        try {
                            options.inJustDecodeBounds = true;
                            FileInputStream fileInputStream = loadDiskCache(id);
                            if (fileInputStream != null) {
                                fileDescriptor = fileInputStream.getFD();
                                BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
                                sizeLock.lock();
                                options.inSampleSize = ImageUtil.getScale(options, width, height);
                                options.inJustDecodeBounds = false;
                                options.inBitmap = getInBitmap(options);
                                sourceHeight = options.outHeight;
                                sourceWidth = options.outWidth;
                                bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
                                if (bitmap == null && options.inBitmap != null) {
                                    options.inBitmap = null;
                                    bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
                                }
                                if (bitmap == null) {
                                    sizeLock.unlock();
                                    tmpDisk.remove(id);
                                }
                                fileInputStream.close();
                            }
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                            sizeLock.unlock();
                        }

                        if (bitmap == null) {
                            byte[] bits;
                            try {
                                bits = dbAdapter.downloadImage((Long) this.id);
                            }
                            catch (SQLException e) {
                                e.printStackTrace();
                                break;
                            }
                            if (bits != null && bits.length > 0) {
                                options.inJustDecodeBounds = true;
                                BitmapFactory.decodeByteArray(bits, 0, bits.length, options);
                                if (options.outHeight > 0 && options.outWidth > 0) {
                                    saveBitmapToDisk(id, bits, true);
                                    sizeLock.lock();
                                    options.inSampleSize = ImageUtil.getScale(options, width, height);
                                    options.inJustDecodeBounds = false;
                                    options.inBitmap = getInBitmap(options);
                                    sourceHeight = options.outHeight;
                                    sourceWidth = options.outWidth;
                                    bitmap = BitmapFactory.decodeByteArray(bits, 0, bits.length, options);
                                }
                            }
                        }
                        break;
                    default:
                        break;
                }
            }

            taskLock.lock();
            if (!Thread.interrupted()) {
                Key[] listenerID = new Key[4];
                for (int i = 0; i < listenerID.length; ++i)
                    listenerID[i] = getKey(id, modes[i], width, height);

                if (bitmap != null) {
                    if (bitmap.getHeight() == sourceHeight && bitmap.getWidth() == sourceWidth) {
                        BitmapInfo sizeInfo = new BitmapInfo();
                        sizeInfo.id = id;
                        sizeInfo.sourceHeight = sourceHeight;
                        sizeInfo.sourceWidth = sourceWidth;
                        fullSizeInfo.put(listenerID[0], sizeInfo);
                    }
                    snapBitmaps.put(getKey(id, ImageUtil.MODE.NORMAL, width, height),
                            new BitmapInfo(id, bitmap, sourceWidth, sourceHeight, options.inSampleSize));

                    if (parent.get() != null) {
                        bitmapInfo = new BitmapInfo(id, formatBitmap(bitmap, mode, borderSize, color),
                                                    sourceWidth, sourceHeight, options.inSampleSize);
                        snapBitmaps.put(getKey(id, mode, width, height), bitmapInfo);
                        postLoadDone(parent, id, bitmapInfo, width, height, mode);
                    }

                    ConcurrentHashMap<Key, List<WeakReference<Object>>> listenerMap = taskListener.get(id);
                    if (listenerMap != null) {
                        for (Map.Entry<Key, List<WeakReference<Object>>> entry : listenerMap.entrySet()) {
                            Bitmap bm = null;
                            Key key = entry.getKey();

                            for (int i = 0; i < listenerID.length; ++i)
                                if (key.equals(listenerID[i]) && entry.getValue().size() > 0) {
                                    for (WeakReference<Object> parent : entry.getValue()) {
                                        if (parent.get() != null) {
                                            bm = formatBitmap(bitmap, modes[i], borderSize, color);
                                            bitmapInfo = new BitmapInfo(id, bm, sourceWidth, sourceHeight, options.inSampleSize);
                                            snapBitmaps.put(getKey(id, modes[i], width, height), bitmapInfo);
                                            break;
                                        }
                                    }

                                    if (bm != null)
                                        for (WeakReference<Object> parent : entry.getValue())
                                            postLoadDone(parent, id, bitmapInfo, width, height, modes[i]);

                                    break;
                                }
                        }
                        for (int i = 0; i < modes.length; ++i) {
                            List<WeakReference<Object>> parentList = listenerMap.get(listenerID[i]);
                            if (parentList != null && parentList.size() > 0)
                                parentList.clear();
                        }
                    }
                }
                else {
                    postLoadFailure(parent, id, width, height, mode, null);
                    ConcurrentHashMap<Key, List<WeakReference<Object>>> listenerMap = taskListener.get(id);
                    if (listenerMap != null) {
                        for (Map.Entry<Key, List<WeakReference<Object>>> entry : listenerMap.entrySet()) {
                            Key key = entry.getKey();

                            for (int i = 0; i < listenerID.length; ++i)
                                if (key.equals(listenerID[i]) && entry.getValue().size() > 0)
                                    for (WeakReference<Object> parent : entry.getValue())
                                        postLoadFailure(parent, id, width, height, modes[i], null);
                        }
                        for (int i = 0; i < modes.length; ++i) {
                            List<WeakReference<Object>> parentList = listenerMap.get(listenerID[i]);
                            if (parentList != null && parentList.size() > 0)
                                parentList.clear();
                        }
                    }
                }
            }
            taskMap.remove(id);
            if (taskMap.size() == 0)
                postAllLoadDone();
            taskLock.unlock();
        }

        //如果缓存在data分区，就移动到手机内存
        FileInputStream loadDiskCache(String key) {
            FileInputStream fileInputStream = null;

            if (externalCache != null && !externalCache.isClosed()) {
                try {
                    DiskLruCache.Snapshot snapShot = externalCache.get(key);
                    if (snapShot != null) {
                        fileInputStream = (FileInputStream) snapShot.getInputStream(0);
                        tmpDisk = externalCache;
                    }
                    else {
                        fileInputStream = loadFromData(key);
                        if (fileInputStream != null) {
                            tmpDisk = dateCache;
                            DiskLruCache.Editor editor = externalCache.edit(key);
                            OutputStream outputStream = editor.newOutputStream(0);

                            byte[] bits = new byte[4098];
                            while (fileInputStream.read(bits, 0, bits.length) != -1)
                                outputStream.write(bits);

                            outputStream.flush();
                            editor.commit();
                            outputStream.close();
                            externalCache.flush();
                            fileInputStream.close();
                            dateCache.remove(key);
                            dateCache.flush();
                            fileInputStream = (FileInputStream) externalCache.get(key).getInputStream(0);
                            tmpDisk = externalCache;
                        }
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (fileInputStream == null && internalCache != null && !internalCache.isClosed()) {
                try {
                    DiskLruCache.Snapshot snapShot = internalCache.get(key);
                    if (snapShot != null) {
                        tmpDisk = internalCache;
                        return (FileInputStream) snapShot.getInputStream(0);
                    }
                    else {
                        fileInputStream = loadFromData(key);
                        if (fileInputStream != null) {
                            tmpDisk = dateCache;
                            DiskLruCache.Editor editor = internalCache.edit(key);
                            OutputStream outputStream = editor.newOutputStream(0);

                            byte[] bits = new byte[4098];
                            while (fileInputStream.read(bits, 0, bits.length) != -1)
                                outputStream.write(bits);

                            outputStream.flush();
                            editor.commit();
                            outputStream.close();
                            internalCache.flush();
                            fileInputStream.close();
                            dateCache.remove(key);
                            dateCache.flush();
                            fileInputStream = (FileInputStream) internalCache.get(key).getInputStream(0);
                            tmpDisk = internalCache;
                        }
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return fileInputStream;
        }

        FileInputStream loadFromData(String key) {
            if (dateCache != null && !dateCache.isClosed())
                try {
                    DiskLruCache.Snapshot snapShot = dateCache.get(key);
                    if (snapShot != null)
                        return (FileInputStream) snapShot.getInputStream(0);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

            return null;
        }

    }

    public Bitmap loadImgForResource(Object parent, Integer id) {
        return loadImgForResource(parent, id, displayMetrics.widthPixels, displayMetrics.heightPixels);
    }

    public Bitmap loadImgForResource(Object parent, Integer id, int width, int height) {
        return loadImgForResource(parent, id, width, height, ImageUtil.MODE.NORMAL);
    }

    public Bitmap loadImgForResource(Object parent, Integer id, int width, int height, ImageUtil.MODE mode) {
        return loadImgForResource(parent, id, width, height, mode, null, null);
    }

    public Bitmap loadImgForResource(Object parent, Integer id, int width, int height, ImageUtil.MODE mode, float[] borderSize, int[] color) {
        Key key = getKey(id, mode, width, height);
        BitmapInfo bitmapInfo = getImgCache(key, width, height);
        if (bitmapInfo != null)
            return bitmapInfo.getBitmap(activity, parent);

        return readTask(key, parent, id, LoadRunnable.TYPE_RESOURCE, width, height, mode, borderSize, color);
    }

    public Bitmap loadImgForNetWork(Object parent, String id) {
        return loadImgForNetWork(parent, id, displayMetrics.widthPixels, displayMetrics.heightPixels);
    }

    public Bitmap loadImgForNetWork(Object parent, String id, int width, int height) {
        return loadImgForNetWork(parent, id, width, height, ImageUtil.MODE.NORMAL);
    }

    public Bitmap loadImgForNetWork(Object parent, String id, int width, int height, ImageUtil.MODE mode) {
        return loadImgForNetWork(parent, id, width, height, mode, null, null);
    }

    public Bitmap loadImgForNetWork(Object parent, String id, int width, int height, ImageUtil.MODE mode, float[] borderSize, int[] color) {
        Key key = getKey(id, mode, width, height);
        BitmapInfo bitmapInfo = getImgCache(key, width, height);
        if (bitmapInfo != null)
            return bitmapInfo.getBitmap(activity, parent);

        return readTask(key, parent, id, LoadRunnable.TYPE_NETWORK, width, height, mode, borderSize, color);
    }

    public Bitmap loadImgForLocal(Object parent, String path) {
        return loadImgForLocal(parent, path, displayMetrics.widthPixels, displayMetrics.heightPixels);
    }

    public Bitmap loadImgForLocal(Object parent, String path, int width, int height) {
        return loadImgForLocal(parent, path, width, height, ImageUtil.MODE.NORMAL);
    }

    public Bitmap loadImgForLocal(Object parent, String path, int width, int height, ImageUtil.MODE mode) {
        return loadImgForLocal(parent, path, width, height, mode, null, null);
    }

    public Bitmap loadImgForLocal(Object parent, String path, int width, int height, ImageUtil.MODE mode, float[] borderSize, int[] color) {
        Key key = getKey(path, mode, width, height);
        BitmapInfo bitmapInfo = getImgCache(key, width, height);
        if (bitmapInfo != null)
            return bitmapInfo.getBitmap(activity, parent);

        return readTask(key, parent, path, LoadRunnable.TYPE_LOCAL, width, height, mode, borderSize, color);
    }

    public Bitmap loadImgForThumb(Object parent, Long thumbId) {
        return loadImgForThumb(parent, thumbId, ImageUtil.MODE.NORMAL);
    }

    public Bitmap loadImgForThumb(Object parent, Long thumbId, ImageUtil.MODE mode) {
        return loadImgForThumb(parent, thumbId, mode, null, null);
    }

    public Bitmap loadImgForThumb(Object parent, Long thumbId, ImageUtil.MODE mode, float[] borderSize, int[] color) {
        Key key = getKey(thumbId, mode, -1, -1);
        BitmapInfo bitmapInfo = getImgCache(key, -1, -1);

        if (bitmapInfo != null)
            return bitmapInfo.getBitmap(activity, parent);

        return readTask(key, parent, thumbId, LoadRunnable.TYPE_MINI, -1, -1, mode, borderSize, color);
    }

    public Bitmap loadImgForDB(Object parent, Long id) {
        return loadImgForDB(parent, id, displayMetrics.widthPixels, displayMetrics.heightPixels);
    }

    public Bitmap loadImgForDB(Object parent, Long id, int width, int height) {
        return loadImgForDB(parent, id, width, height, ImageUtil.MODE.NORMAL);
    }

    public Bitmap loadImgForDB(Object parent, Long id, int width, int height, ImageUtil.MODE mode) {
        return loadImgForDB(parent, id, width, height, mode, null, null);
    }

    public Bitmap loadImgForDB(Object parent, Long id, int width, int height, ImageUtil.MODE mode, float[] borderSize, int[] color) {
        if (dbAdapter == null)
            throw new NullPointerException("dbAdapter can't null");
        else {
            Key key = getKey(id, mode, width, height);
            BitmapInfo bitmapInfo = getImgCache(key, width, height);
            if (bitmapInfo != null)
                return bitmapInfo.getBitmap(activity, parent);

            return readTask(key, parent, id, LoadRunnable.TYPE_DB, width, height, mode, borderSize, color);
        }
    }

    private Key getKey(Object id, ImageUtil.MODE mode, int width, int height) {
        return getKey(id, mode, width, height, quality);
    }

    private static Key getKey(Object id, ImageUtil.MODE mode, int width, int height, QUALITY quality) {
        BitmapInfo bitmapInfo = fullSizeInfo.get(id);
        StringBuilder builder = new StringBuilder();
        builder.append(id.toString());
        builder.append(mode.getValue());

        if (bitmapInfo != null) {
            builder.append(bitmapInfo.sourceWidth);
            builder.append(bitmapInfo.sourceHeight);
        }
        else if (quality == QUALITY.FULL_SIZE) {
            builder.append(width);
            builder.append(height);
        }

//        Key key = new Key(id, mode, builder.toString());
//        Log.e(AsyncLoader.class.getSimpleName(), id.toString() + "  " + mode.getValue() + "  " + key.hashCode());
        return new Key(id, mode, builder.toString());
    }

    private BitmapInfo getImgCache(Key key, int width, int height) {
        return getImgCache(key, width, height, quality);
    }

    public static BitmapInfo getImgCache(Object id, int width, int height, ImageUtil.MODE mode, QUALITY quality) {
        return getImgCache(getKey(id, mode, width, height, quality), width, height, quality);
    }

    private static BitmapInfo getImgCache(Key key, int width, int height, QUALITY quality) {
        if (quality == QUALITY.PROBABLY_SIZE) {
            BitmapInfo bitmapInfo = imageCache.get(key);

            if (bitmapInfo != null) {
                Bitmap bitmap = bitmapInfo.bitmap;
                if (bitmapInfo.sourceHeight == bitmap.getHeight() && bitmapInfo.sourceWidth == bitmap.getWidth())
                    return bitmapInfo;

                int sourceSize = bitmapInfo.bitmap.getWidth() * bitmapInfo.bitmap.getHeight();
                int targetSize = width * height;
                int scale = ImageUtil.getScale(bitmapInfo.sourceWidth, bitmapInfo.sourceHeight, width, height);

                if (sourceSize >= targetSize || targetSize / 2 <= sourceSize)
                    return bitmapInfo;

                if (scale * 2 >= bitmapInfo.scale)
                    return bitmapInfo;
            }
        }
        else
            return imageCache.get(key);

        return null;
    }

    public static byte[] getImgFromUrl(String url) {
        int len;
        byte[] b;

        try {
            URLConnection urlConnection = new URL(url).openConnection();
            urlConnection.setConnectTimeout((int) (TIMEOUT * 1000));
            BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream(), 1024 * 8);
            ByteArrayOutputStream bufferedOutputStream = new ByteArrayOutputStream(1024 * 8);

            while ((len = inputStream.read()) != -1)
                bufferedOutputStream.write(len);

            b = bufferedOutputStream.toByteArray();

            inputStream.close();
            bufferedOutputStream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return b;
    }

    private synchronized Bitmap getInBitmap(BitmapFactory.Options options) {
        Iterator<SoftReference<BitmapInfo>> iterator = reusableBitmaps.iterator();
        while (iterator.hasNext()) {
            SoftReference<BitmapInfo> softReference = iterator.next();
            BitmapInfo bitmapInfo = softReference.get();
            if (bitmapInfo == null)
                iterator.remove();
            else if ((bitmapInfo.activity == null)) {
                if (canUseInBitmap(bitmapInfo.bitmap, options)) {
                    iterator.remove();
                    return bitmapInfo.bitmap;
                }
            }
            else {
                FragmentActivity activity = bitmapInfo.activity.get();
                if (activity == null || !activitySet.contains(activity) || !hasReference(bitmapInfo)) {
                    if (canUseInBitmap(bitmapInfo.bitmap, options)) {
                        iterator.remove();
                        return bitmapInfo.bitmap;
                    }
                }
            }
        }

        return null;
    }

    private boolean canUseInBitmap(Bitmap bitmap, BitmapFactory.Options options) {
        if (!bitmap.getConfig().equals(Bitmap.Config.ARGB_8888) || options.outHeight < 1 || options.outWidth < 1)
            return false;
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return options.inSampleSize == 1 && bitmap.getHeight() == options.outHeight
                    && bitmap.getWidth() == options.outWidth;
        else
            return bitmap.getHeight() * bitmap.getWidth() >
                    (options.outHeight / options.inSampleSize) * (options.outWidth / options.inSampleSize);
    }

    private boolean hasReference(BitmapInfo bitmapInfo) {
        Iterator<WeakReference<Object>> iterator = bitmapInfo.parentReference.iterator();
        while (iterator.hasNext()) {
            Object parent = iterator.next().get();
            if (parent == null)
                iterator.remove();
            else if (parent instanceof ImageView) {
                    ImageView imageView = (ImageView) parent;
                    if (imageView.getParent() != null && imageView.getVisibility() == View.VISIBLE
                            && ((BitmapDrawable) imageView.getDrawable()).getBitmap().equals(bitmapInfo.bitmap))
                        return true;
                    else
                        iterator.remove();
                }
                else
                    return true;
        }

        return false;
    }

    private synchronized Bitmap formatBitmap(Bitmap bitmap, ImageUtil.MODE mode, float[] borderSize, int[] color) {
        if (mode != null) {
            switch (mode) {
                case ROUND:
                    bitmap = ImageUtil.getRoundBitmap(bitmap);
                    break;
                case ROUND_BORDER:
                    bitmap = ImageUtil.getRoundBitmap(bitmap);
                    bitmap = ImageUtil.getBorderBitmap(bitmap, mode, borderSize, color);
                    break;
                case NORMAL_BORDER:
                    bitmap = ImageUtil.getBorderBitmap(bitmap, mode, borderSize, color, true);
                    break;
            }
            return bitmap;
        }
        else
            return null;
    }

    private void postLoadDone(final WeakReference<Object> parent, final Object id, final BitmapInfo bitmapInfo,
                                           final int width, final int height, final ImageUtil.MODE mode) {
        if (!Thread.interrupted() && !isShutdown()) {
            final Object oj = parent.get();
            if (oj != null && mImageLoadListener != null) {
                handler.post(new Runnable() {
                    public void run() {
                        mImageLoadListener.onImageLoadDone(oj, id, bitmapInfo, width, height, mode);
                    }
                });
            }
        }
    }

    private void postLoadFailure(final WeakReference<Object> parent, final Object id,
                                              final int width, final int height, final ImageUtil.MODE mode, final Exception e) {
        if (!Thread.interrupted() && !isShutdown()) {
            if (parent.get() != null && mImageLoadListener != null) {
                handler.post(new Runnable() {
                    public void run() {
                        mImageLoadListener.onImageLoadFailure(parent.get(), id, width, height, mode, e);
                    }
                });
            }
        }
    }

    private void postAllLoadDone() {
        if (!Thread.interrupted() && !isShutdown()) {
            handler.post(new Runnable() {
                public void run() {
                    if (mImageLoadListener != null)
                        mImageLoadListener.onAllImageLoadDone();
                }
            });
        }
    }

    private Bitmap readTask(Key key, Object parent, Object id, int type, int width, int height,
                           ImageUtil.MODE mode, float[] borderSize, int[] color) {
        BitmapInfo bitmapInfo;
        Bitmap bmReuse = null;
        int sourceHeight = -1, sourceWidth = -1;

        LoadRunnable loadRunnable = taskMap.get(id);
        if (loadRunnable != null) {
            if (width > loadRunnable.width && height > loadRunnable.height && loadRunnable.sizeLock.tryLock()) {
                loadRunnable.width = width;
                loadRunnable.height = height;
                loadRunnable.sizeLock.unlock();
            }
            if (loadRunnable.taskLock.tryLock()) {
                ConcurrentHashMap<Key, List<WeakReference<Object>>> listenerMap = taskListener.get(id);
                List<WeakReference<Object>> parentList;
                if (listenerMap == null) {
                    listenerMap = new ConcurrentHashMap<>();
                    taskListener.put(id, listenerMap);
                }
                parentList = listenerMap.get(key);
                if (parentList == null) {
                    parentList = new ArrayList<>();
                    listenerMap.put(key, parentList);
                }
                parentList.add(new WeakReference<>(parent));
                loadRunnable.taskLock.unlock();
                return null;
            }
            else {
                loadRunnable.taskLock.lock();
                bitmapInfo = getImgCache(key, width, height);
                if (bitmapInfo == null) {
                    key = getKey(id, mode, width, height);
                    BitmapInfo reuseInfo = snapBitmaps.get(key);
                    if (reuseInfo == null) {
                        key = getKey(id, ImageUtil.MODE.NORMAL, width, height);
                        reuseInfo = snapBitmaps.get(key);
                        if (reuseInfo != null) {
                            bmReuse = reuseInfo.bitmap;
                            sourceWidth = reuseInfo.sourceWidth;
                            sourceHeight = reuseInfo.sourceHeight;
                            snapBitmaps.remove(key);
                        }
                    }
                    else
                        bitmapInfo = reuseInfo;
                }

                loadRunnable.taskLock.unlock();
            }
        }
        else {
            createRunnable(parent, id, null, sourceWidth, sourceHeight, type, width, height, mode, borderSize, color);
            return null;
        }

        if (bitmapInfo == null) {
            createRunnable(parent, id, bmReuse, sourceWidth, sourceHeight, type, width, height, mode, borderSize, color);
            return null;
        }
        else
            return bitmapInfo.getBitmap(activity, parent);
    }

    private void createRunnable(Object parent, Object id, Bitmap reuseBitmap, int sourceWidth,
                                int sourceHeight, int type, int width, int height,
                                ImageUtil.MODE mode, float[] borderSize, int[] color) {
        LoadRunnable runnable = new LoadRunnable();
        runnable.parent = new WeakReference<>(parent);
        runnable.id = id;
        runnable.type = type;
        runnable.width = width;
        runnable.height = height;
        runnable.mode = mode;
        runnable.borderSize = borderSize;
        runnable.color = color;
        runnable.bitmap = reuseBitmap;
        runnable.sourceWidth = sourceWidth;
        runnable.sourceHeight = sourceHeight;
        taskMap.put(id, runnable);
        if (!pause) {                                 //在滑动情况下不进行加载，同时也是因为滑动情况下parent在变化改变
            runnable.future = executorService.submit(runnable);
        }
    }

    private DBManager.ResultListener dbResultListener = new DBManager.ResultListener() {
        @Override
        public void dbOpen() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    restartAllTask();
                }
            });
        }

        @Override
        public void onFailure(Object o1, Object o2, Exception e) {
            e.printStackTrace();
        }
    };

    //监听Activity的生命周期
    public static class FragmentListener extends Fragment {
        private AsyncLoader asyncLoader;

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
            return null;
        }

        @Override
        public void onResume() {
            activitySet.add(asyncLoader.activity);
            super.onResume();
        }

        @Override
        public void onDestroy() {
            activitySet.remove(asyncLoader.activity);
            asyncLoader.exitLoader();
            asyncLoader = null;
            super.onDestroy();
        }

    }

    //BaseActivity提供的监听方法
    private static class PeriodicMonitoring extends BaseActivity.PeriodicMonitoring {
        private AsyncLoader asyncLoader;

        @Override
        public void onResume() {
            activitySet.add(asyncLoader.activity);
        }

        @Override
        public void onDestroy() {
            activitySet.remove(asyncLoader.activity);
            asyncLoader.exitLoader();
            asyncLoader = null;
        }
    }

    //降低字符串哈希碰撞几率
    private static class Key {
        int hash = 0;
        final Object id;
        final ImageUtil.MODE mode;
        final String keyStr;

        Key(Object id, ImageUtil.MODE mode, String keyStr) {
            this.id = id;
            this.mode = mode;
            this.keyStr = keyStr;
        }

        @Override
        public int hashCode() {
            if (hash == 0 && keyStr.length() > 0) {
                //FNV哈希算法
                final int p = 16777619;
                hash = (int) 2166136261L;
                for (int i = 0; i < keyStr.length(); ++i) {
                    hash = (hash ^ keyStr.charAt(i)) * p;
                    hash += hash << 13;
                    hash ^= hash >> 7;
                    hash += hash << 3;
                    hash ^= hash >> 17;
                    hash += hash << 5;
                }
            }
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;

            if (obj instanceof Key) {
                Key tmp = (Key) obj;

                if (keyStr != null && tmp.keyStr != null) {
                    if (tmp.keyStr.length() != keyStr.length())
                        return false;

                    for (int i = 0; i < tmp.keyStr.length(); ++i)
                        if (tmp.keyStr.charAt(i) != keyStr.charAt(i))
                            return false;
                }
                else
                    return false;

                return true;
            }
            else
                return false;
        }
    }

}
