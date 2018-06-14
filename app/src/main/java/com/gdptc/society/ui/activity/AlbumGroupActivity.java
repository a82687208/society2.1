package com.gdptc.society.ui.activity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.basemodel.BaseListView;
import com.example.basemodel.Value;
import com.example.tools.MaterialDesignCompat;
import com.gdptc.society.Public;
import com.gdptc.society.R;
import com.gdptc.society.apiServer.ImgInfo;
import com.gdptc.society.base.BaseActivity;
import com.gdptc.society.manager.ApplicationManager;
import com.gdptc.society.tools.AsyncLoader;
import com.gdptc.society.tools.BitmapInfo;
import com.gdptc.society.tools.ImageUtil;
import com.gdptc.society.tools.TitleBarUtil;
import com.gdptc.society.ui.view.BorderImage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static java.lang.Runtime.getRuntime;

/**
 * Created by Administrator on 2017/9/19/019.
 */

public class AlbumGroupActivity extends BaseActivity implements AdapterView.OnItemClickListener {
    private final int REQUEST_SELECTOR = 0;
    private HashMap<String, ArrayList<ImgInfo>> dataMap = new HashMap<>();
    private HashMap<String, Integer> resultSelectNum = new HashMap<>();
    private boolean[] isSelector;
    private int clickPosition = 0, lastClickPosition = 0;
    private int lastPosition;
    private AsyncLoader asyncLoader;
    private BaseListView mListView;
    private Object[] mBucketNameArray;

    private int selectNum = 0, colorUI;
    private int picSize;

    private Bitmap mBmPicLoad;

    private TextView mTitle;
    private int totalSelectNum = 0, cappedNum;
    private ApplicationManager applicationManager;
    private boolean mRadio;

    private String key = AlbumGroupActivity.class.getSimpleName();

    public void initData() {
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = getContentResolver().query(
                uri, new String[]{ MediaStore.Files.FileColumns._ID, MediaStore.Images.Thumbnails.DATA,
                        MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME }, null, null, MediaStore.Files.FileColumns._ID + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String path = cursor.getString(1);
                String bucketName = cursor.getString(2);
                if (bucketName.equals("ImageCache") || isCache(path))
                    continue;
                ArrayList<ImgInfo> list = dataMap.get(bucketName);
                if (list == null) {
                    list = new ArrayList<>();
                    dataMap.put(bucketName, list);
                }

                ImgInfo info = new ImgInfo();
                info.id = cursor.getString(0);
                info.path = path;
                info.bucketName = bucketName;
                list.add(info);
            }

            cursor.close();
        }
    }

    private boolean isCache(String path) {
        int startIndex = path.indexOf("/") + 1;

        while (startIndex != -1) {
            int endIndex = path.indexOf("/", startIndex);
            if (endIndex == -1)
                break;
            String temp = path.substring(startIndex, endIndex);
            startIndex = endIndex + 1;
            if (temp.equals("ImageCache"))
                return true;
        }

        return false;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_selector);

        asyncLoader = new AsyncLoader(this, getRuntime().availableProcessors());
        asyncLoader.setImageLoadListener(imageLoadListener);
        applicationManager = (ApplicationManager) getApplication();
        mListView = (BaseListView) findViewById(R.id.activity_albumSelector_listView);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mBmPicLoad = BitmapFactory.decodeResource(getResources(), R.drawable.album);

        picSize = getResources().getDimensionPixelSize(R.dimen.item_album_group_thumbSize);

        Intent intent = getIntent();
        mRadio = intent.getBooleanExtra(Public.RADIO, false);
        cappedNum = intent.getIntExtra(Public.CAPPED, -1);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_bar);
        mTitle = (TextView) toolbar.findViewById(R.id.tv_toolbar_title);
        new TitleBarUtil(this, toolbar, "相册", true, true).init();

        initData();
        isSelector = new boolean[dataMap.size()];
        Collection values = dataMap.keySet();
        mBucketNameArray = values.toArray();

        onUIColorChange(Value.getColorUI());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        lastClickPosition = clickPosition;
        clickPosition = position;
        Intent intent = new Intent(AlbumGroupActivity.this, AlbumActivity.class);
        applicationManager.dataMap.put(key, dataMap.get(mBucketNameArray[position]));
        intent.putExtra(Public.CONTEXT, key);
        intent.putExtra(Public.NUMBER, mRadio ? selectNum : totalSelectNum);
        intent.putExtra(Public.RADIO, mRadio);
        intent.putExtra(Public.CAPPED, cappedNum);
        startActivityForResult(intent, REQUEST_SELECTOR);
    }

    private class ViewHolder {
        TextView count;
        TextView path;
        TextView bucketName;
        BorderImage img;
        ImageView checkable;
        long id;
    }

    private BaseAdapter mAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return dataMap.size();
        }

        @Override
        public Object getItem(int position) {
            return mBucketNameArray[position].toString();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            String bucketName = (String) getItem(position);
            List<ImgInfo> list = dataMap.get(bucketName);
            ImgInfo firstInfo = list.get(0);

            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(AlbumGroupActivity.this).inflate(R.layout.item_album_select_child, null);
                convertView = MaterialDesignCompat.addRipple(convertView, MaterialDesignCompat.DEFAULT_COLOR);
                viewHolder.bucketName = (TextView) convertView.findViewById(R.id.item_album_selector_bucketName);
                viewHolder.path = (TextView) convertView.findViewById(R.id.item_album_selector_path);
                viewHolder.count = (TextView) convertView.findViewById(R.id.item_album_select_count);
                viewHolder.img = (BorderImage) convertView.findViewById(R.id.item_album_selector_img);
                viewHolder.img.setBorderSize(picSize / 8);
                viewHolder.checkable = (ImageView) convertView.findViewById(R.id.item_album_select_child_checkedImg);
                convertView.setTag(viewHolder);
            }
            else
                viewHolder = (ViewHolder) convertView.getTag();

            viewHolder.id = Long.parseLong(firstInfo.id);
            viewHolder.bucketName.setText(bucketName);
            viewHolder.count.setText(list.size() + "张");
            viewHolder.path.setText(firstInfo.path.substring(0, firstInfo.path.lastIndexOf("/")));
            viewHolder.checkable.setVisibility(isSelector[position] ? View.VISIBLE : View.GONE);
            viewHolder.checkable.setColorFilter(colorUI);
            Bitmap bitmap = asyncLoader.loadImgForThumb(convertView, viewHolder.id);
            if (bitmap == null) {
                viewHolder.img.setImageBitmap(mBmPicLoad);
            }
            else {
                viewHolder.img.setImageBitmap(bitmap);
            }

            return convertView;
        }
    };

    private AsyncLoader.ImageLoadListener imageLoadListener = new AsyncLoader.ImageLoadListener() {
        @Override
        public void onImageLoadDone(Object parent, Object id, BitmapInfo bitmapInfo, int width, int height, ImageUtil.MODE mode) {
            ViewHolder viewHolder = (ViewHolder) ((View) parent).getTag();
            if (viewHolder.id == (long) id) {
                viewHolder.img.setColorFilter(null);
                viewHolder.img.setImageBitmap(bitmapInfo.getBitmap(AlbumGroupActivity.this, viewHolder.img));
            }
            asyncLoader.saveBitmapToLru(id, bitmapInfo, width, height, mode);
        }

        @Override
        public void onImageLoadFailure(Object parent, Object id, int width, int height, ImageUtil.MODE mode, Exception e) {

        }
    };

    @Override
    public void onUIColorChange(Value.COLOR color) {
        colorUI = color.toValue();
        mListView.setEdgeEffectColor(colorUI);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SELECTOR && resultCode == RESULT_OK) {
            if (data != null) {
                ImgInfo info;
                if (!mRadio) {
                    String key = data.getStringExtra(Public.CONTEXT);
                    ArrayList<ImgInfo> selectList = (ArrayList<ImgInfo>) applicationManager.dataMap.get(key);
                    applicationManager.dataMap.remove(key);

                    info = selectList.get(0);
                    dataMap.put(info.bucketName, selectList);
                }
                else
                    info = data.getParcelableExtra(Public.DATA);

                boolean upload = data.getBooleanExtra(Public.UPLOAD, false);

                if (upload) {
                    Intent intent = new Intent();
                    if (!mRadio) {
                        ArrayList<String> uploadList = new ArrayList<>();
                        Collection values = dataMap.keySet();
                        mBucketNameArray = values.toArray();
                        for (Object bucketName : mBucketNameArray) {
                            String value = bucketName.toString();
                            List<ImgInfo> list = dataMap.get(value);
                            for (ImgInfo imgInfo : list)
                                if (imgInfo.selector)
                                    uploadList.add(imgInfo.path);
                            intent.putExtra(Public.DATA, uploadList);
                        }
                    }
                    else {
                        String path = data.getStringExtra(Public.PATH);
                        if (path == null)
                            path = dataMap.get(mBucketNameArray[lastClickPosition]).get(lastPosition).path;
                        intent.putExtra(Public.PATH, path);
                    }
                    setResult(RESULT_OK, intent);
                    finish();
                }
                else {
                    int selectNum = data.getIntExtra(Public.NUMBER, 0);
                    if (selectNum < 0)
                        selectNum = 0;
                    isSelector[clickPosition] = selectNum != 0;
                    if (!mRadio) {
                        resultSelectNum.put(info.bucketName, selectNum);
                        totalSelectNum = 0;
                        Collection<Integer> collection = resultSelectNum.values();
                        for (Integer integer : collection)
                            totalSelectNum += integer;

                        mTitle.setText(totalSelectNum == 0 ? "相册" : "相册(" + totalSelectNum + "张)");
                    }
                    else if (info != null) {
                        for (int i = 0; i < isSelector.length; ++i)
                            if (i != clickPosition)
                                isSelector[i] = false;
                        int position = data.getIntExtra(Public.ID, -1);
                        if (position != -1 && clickPosition != lastClickPosition || position != lastPosition) {
                            dataMap.get(mBucketNameArray[clickPosition]).get(position).selector = true;
                            dataMap.get(mBucketNameArray[lastClickPosition]).get(lastPosition).selector = false;
                            resultSelectNum.clear();
                            resultSelectNum.put(info.bucketName, selectNum);
                            lastPosition = position;
                        }
                        this.selectNum = 1;
                    }
                    else {
                        isSelector[clickPosition] = false;
                        dataMap.get(mBucketNameArray[clickPosition]).get(lastPosition).selector = false;
                        this.selectNum = 0;
                    }
                    mAdapter.notifyDataSetChanged();
                }
            }
        }
    }

}
