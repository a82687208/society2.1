package com.gdptc.society.ui.activity;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bm.library.PhotoView;
import com.example.basemodel.Value;
import com.gdptc.society.Public;
import com.gdptc.society.R;
import com.gdptc.society.apiServer.ImgInfo;
import com.gdptc.society.base.BaseActivity;
import com.gdptc.society.manager.ApplicationManager;
import com.gdptc.society.tools.AsyncLoader;
import com.gdptc.society.tools.BitmapInfo;
import com.gdptc.society.tools.ImageUtil;
import com.gdptc.society.tools.TitleBarUtil;
import com.gdptc.society.tools.TypedValueUtil;
import com.gdptc.society.ui.view.GridViewWithHeaderAndFooter;

import java.util.ArrayList;

import static java.lang.Runtime.getRuntime;

/**
 * Created by Administrator on 2017/9/19/019.
 */

public class AlbumActivity extends BaseActivity implements AdapterView.OnItemClickListener, View.OnClickListener {
    private final int REQUEST_RESULT = 1;

    private ArrayList<ImgInfo> dataList;
    private TextView title;
    private GridViewWithHeaderAndFooter gridView;
    private Intent data;
    private int childSize;
    private AsyncLoader asyncLoader;
    private int totalSelectNum;
    private int selectNum = 0;
    private int selectPosition = -1;
    private ApplicationManager applicationManager;

    private Bitmap check, checkable, bmPicLoad;

    private int colorUI;

    private boolean radio;
    private ViewHolder clickHolder;
    private int clickPosition = -1, cappedNum;

    private String key = AlbumActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        disableTransition();
        asyncLoader = new AsyncLoader(this, getRuntime().availableProcessors() * 2);
        asyncLoader.setImageLoadListener(imageLoadListener);

        data = getIntent();
        radio = data.getBooleanExtra(Public.RADIO, false);
        cappedNum = data.getIntExtra(Public.CAPPED, -1);
        applicationManager = (ApplicationManager) getApplication();
        childSize = (int) TypedValueUtil.dip2px(this, 100);
        gridView = (GridViewWithHeaderAndFooter) findViewById(R.id.activity_album_gridView);
        gridView.addHeaderView(LayoutInflater.from(AlbumActivity.this).inflate(R.layout.item_album_head, null));
        gridView.addFooterView(LayoutInflater.from(AlbumActivity.this).inflate(R.layout.item_album_head, null));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_bar);
        title = (TextView) toolbar.findViewById(R.id.tv_toolbar_title);

        String key = data.getStringExtra(Public.CONTEXT);
        dataList = (ArrayList<ImgInfo>) applicationManager.dataMap.get(key);
        applicationManager.dataMap.remove(key);

        String titleStr;
        if (radio) {
            selectNum = data.getIntExtra(Public.NUMBER, 0);
            titleStr = "选择图片";
            for (int i = 0; i < dataList.size(); ++i)
                if (dataList.get(i).selector)
                    clickPosition = i;
        }
        else {
            totalSelectNum = data.getIntExtra(Public.NUMBER, 0);
            for (ImgInfo imgInfo : dataList)
                if (imgInfo.selector)
                    ++selectNum;
            titleStr = "已选: " + totalSelectNum + "/" + (cappedNum == -1 ? dataList.size() : cappedNum);
            totalSelectNum -= selectNum;
        }

        new TitleBarUtil(this, toolbar, titleStr, true, true).init();
        if (!radio)
            ((Toolbar.LayoutParams) title.getLayoutParams()).gravity = Gravity.NO_GRAVITY;
        gridView.setAdapter(baseAdapter);
        gridView.setOnItemClickListener(this);
        Resources resources = getResources();
        check = BitmapFactory.decodeResource(resources, R.drawable.checked);
        checkable = BitmapFactory.decodeResource(resources, R.drawable.checkable);
        bmPicLoad = BitmapFactory.decodeResource(resources, R.drawable.album);
        onUIColorChange(Value.getColorUI());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ImageView imageView = ((ViewHolder) view.getTag()).photo;
        Intent intent = new Intent();
        applicationManager.dataMap.put(key, dataList);
        applicationManager.dataMap.put(Public.INFO, PhotoView.getImageViewInfo(imageView));

        intent.setClass(AlbumActivity.this, GalleryActivity.class);
        //intent.putExtra(Public.POSITION, selectNum == 0 ? -1 : position);
        intent.putExtra(Public.POSITION, position);
        intent.putExtra(Public.CONTEXT, key);
        intent.putExtra(Public.NUMBER, radio ? selectNum : totalSelectNum + selectNum);
        if (selectNum == 0)
            intent.putExtra(Public.ID, -1);
        else
            intent.putExtra(Public.ID, clickHolder != null ? clickHolder.position : -1);
        intent.putExtra(Public.INFO, Public.INFO);
        intent.putExtra(Public.RADIO, data.getBooleanExtra(Public.RADIO, false));
        intent.putExtra(Public.CAPPED, cappedNum);
        intent.putExtra(Public.DATA, ((BitmapDrawable) imageView.getDrawable()).getBitmap());
        startActivityForResult(intent, REQUEST_RESULT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = new MenuInflater(this);
        menuInflater.inflate(R.menu.album_activity, menu);

        if (radio) {
            MenuItem menuItem = menu.findItem(R.id.activity_albumActivity_upload);
            menuItem.setTitle("确定");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if ((!radio || clickHolder == null) && selectNum == 0 && totalSelectNum == 0)
            Toast.makeText(AlbumActivity.this, "您尚未选择图片哟", Toast.LENGTH_SHORT).show();
        else {
            applicationManager.dataMap.put(key, dataList);

            Intent intent = new Intent();
            intent.putExtra(Public.UPLOAD, true);
            intent.putExtra(Public.CONTEXT, key);
            if (radio && selectNum > 0 && clickHolder != null)
                intent.putExtra(Public.PATH, dataList.get(clickHolder.position).path);
            setResult(RESULT_OK, intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        if (!radio) {
            applicationManager.dataMap.put(key, dataList);
            intent.putExtra(Public.CONTEXT, key);
        }
        else if (selectPosition != -1) {
            intent.putExtra(Public.ID, selectPosition);
            intent.putExtra(Public.DATA, dataList.get(selectPosition));
        }
        else
            intent.putExtra(Public.ID, -1);
        intent.putExtra(Public.NUMBER, selectNum);
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESULT) {
             if (resultCode == RESULT_OK) {
                ArrayList<ImgInfo> list = null;
                if (!radio) {
                    String key = data.getStringExtra(Public.CONTEXT);
                    list = (ArrayList<ImgInfo>) applicationManager.dataMap.get(key);
                    applicationManager.dataMap.remove(key);
                    if (list != null)
                        dataList = list;
                }

                boolean upload = data.getBooleanExtra(Public.UPLOAD, false);
                selectNum = data.getIntExtra(Public.NUMBER, 0);

                if (upload) {
                    Intent intent = new Intent();
                    if (!radio) {
                        intent.putExtra(Public.CONTEXT, this.key);
                        applicationManager.dataMap.put(this.key, list);
                    }
                    intent.putExtra(Public.PATH, data.getStringExtra(Public.PATH));
                    intent.putExtra(Public.UPLOAD, true);
                    setResult(RESULT_OK, intent);
                    finish();
                }

                if (!upload && !radio)
                    title.setText("已选: " + (selectNum + totalSelectNum) + "/" + (cappedNum == -1 ? dataList.size() : cappedNum));

                if (radio) {
                    clickPosition = data.getIntExtra(Public.ID, -1);
                    if (clickPosition != -1) {
                        dataList.get(clickPosition).selector = true;
                        selectPosition = clickPosition;
                        selectNum = 1;
                    }
                }
            }
            else if (clickPosition != -1) {
                 dataList.get(clickPosition).selector = false;
                 selectPosition = -1;
                 selectNum = 0;
             }
            baseAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.item_album_child_selectImg) {
            if (radio && clickHolder != null) {
                clickHolder.selectorImg.setImageBitmap(checkable);
                dataList.get(clickHolder.position).selector = false;
            }
            ViewHolder viewHolder = (ViewHolder) v.getTag();
            ImgInfo info = dataList.get(viewHolder.position);

            if (info.selector) {
                --selectNum;
                viewHolder.selectorImg.setImageBitmap(checkable);
                info.selector = false;
            }
            else if (cappedNum == -1){
                if (viewHolder != clickHolder) {
                    viewHolder.selectorImg.setImageBitmap(check);
                    info.selector = true;
                    selectNum = 1;
                    clickPosition = viewHolder.position;
                }
                else {
                    selectNum = 0;
                    selectPosition = -1;
                    clickHolder = null;
                    clickPosition = -1;
                    return;
                }
            }
            else if (totalSelectNum + selectNum < cappedNum) {
                viewHolder.selectorImg.setImageBitmap(check);
                info.selector = true;
                ++selectNum;
            }

            if (!radio) {
                int num = selectNum + totalSelectNum;
                title.setText("已选: " + num + "/" + (cappedNum == -1 ? dataList.size() : cappedNum));
            }
            clickHolder = viewHolder;
            selectPosition = viewHolder.position;
        }
    }

    private class ViewHolder {
        ImageView photo;
        ImageView selectorImg;
        int position;
        long id;
    }

    private BaseAdapter baseAdapter = new BaseAdapter() {
        AbsListView.LayoutParams layoutParams = null;

        @Override
        public int getCount() {
            return dataList.size();
        }

        @Override
        public Object getItem(int position) {
            return dataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImgInfo info = dataList.get(position);
            ViewHolder viewHolder;

            if (layoutParams == null)
                layoutParams = new AbsListView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, childSize);

            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(AlbumActivity.this).inflate(R.layout.item_album_child, null);
                convertView.setLayoutParams(layoutParams);
                viewHolder.photo = (ImageView) convertView.findViewById(R.id.item_album_child_img);
                viewHolder.selectorImg = (ImageView) convertView.findViewById(R.id.item_album_child_selectImg);
                viewHolder.selectorImg.setOnClickListener(AlbumActivity.this);
                viewHolder.selectorImg.setTag(viewHolder);
                viewHolder.photo.setTag(viewHolder);
                convertView.setTag(viewHolder);
            }
            else
                viewHolder = (ViewHolder) convertView.getTag();

            viewHolder.position = position;
            viewHolder.id = Long.parseLong(info.id);
            Bitmap bitmap = asyncLoader.loadImgForThumb(convertView, viewHolder.id);
            if (bitmap == null) {
                viewHolder.photo.setImageBitmap(bmPicLoad);
            }
            else {
                viewHolder.photo.setImageBitmap(bitmap);
            }

            viewHolder.selectorImg.setImageBitmap(info.selector ? check : checkable);
            viewHolder.selectorImg.setColorFilter(colorUI);
            if (radio) {
                if (info.selector)
                    clickHolder = viewHolder;
            }

            return convertView;
        }
    };

    AsyncLoader.ImageLoadListener imageLoadListener = new AsyncLoader.ImageLoadListener() {
        @Override
        public void onImageLoadDone(Object parent, Object id, BitmapInfo bitmapInfo, int width, int height, ImageUtil.MODE mode) {
            ViewHolder viewHolder = (ViewHolder) ((View) parent).getTag();
            if (viewHolder.id == (long) id) {
                viewHolder.photo.setColorFilter(null);
                viewHolder.photo.setImageBitmap(bitmapInfo.getBitmap(AlbumActivity.this, viewHolder.photo));
            }
            asyncLoader.saveBitmapToLru(id, bitmapInfo, width, height, mode);
        }

        @Override
        public void onImageLoadFailure(Object parent, Object id, int width, int height, ImageUtil.MODE mode, Exception e) {}
    };

    @Override
    public void onUIColorChange(Value.COLOR color) {
        colorUI = color.toValue();
        gridView.setEdgeEffectColor(colorUI);
    }

    @Override
    protected void onDestroy() {
        asyncLoader.exitLoader();
        super.onDestroy();
    }

}
