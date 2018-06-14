package com.gdptc.society.ui.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bm.library.PhotoView;
import com.gdptc.society.Public;
import com.gdptc.society.R;
import com.gdptc.society.apiServer.AccountInfo;
import com.gdptc.society.apiServer.ApiServer;
import com.gdptc.society.base.BaseActivity;
import com.gdptc.society.manager.ApplicationManager;
import com.gdptc.society.manager.DBManager;
import com.gdptc.society.tools.AsyncLoader;
import com.gdptc.society.tools.CameraUtil;
import com.gdptc.society.tools.ImageUtil;
import com.gdptc.society.tools.InputUtil;
import com.gdptc.society.tools.NativeUtil;
import com.gdptc.society.tools.RandomUtil;
import com.gdptc.society.tools.TitleBarUtil;
import com.gdptc.society.tools.TypedValueUtil;
import com.gdptc.society.ui.dialog.AlbumDialog;
import com.gdptc.society.ui.dialog.LoadingDialog;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.gdptc.society.manager.ApplicationManager.directoryManager;

/**
 * Created by Administrator on 2018/6/13/013.
 */

public class AddSocietyActivity extends BaseActivity implements View.OnClickListener, View.OnTouchListener {
    private final int REQUEST_ALBUM = 0x000000002;
    private final int MAX_UPLOAD = 3;

    private final String[] errMsg = { "活动名称", "最大参与人数", "活动地点", "联系号码", "活动介绍" };

    private EditText edtTxtName, edtTxtMax, edtTxtWelfare, edtTxtAddress, edtTxtPhone, edtTxtMsg;

    private AccountInfo accountInfo;

    private ImageView imgAddPic;

    private TextView tvSubmit, tvTime;

    private TitleBarUtil titleBarUtil;

    private RecyclerView rcyPic;

    private AlbumDialog albumDialog;
    private LoadingDialog loadingDialog;

    private int bitmapSize;

    private File filePic;

    private Handler handler;

    private List<EditText> editTextList = new ArrayList<>();
    private List<Bitmap> bitmapList = new ArrayList<>();
    private List<String> pathList = new ArrayList<>();

    private ApplicationManager applicationManager;

    private DBManager.NetworkDBAdapter dbAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_society);
        disableTransition();

        handler = new Handler();
        applicationManager = (ApplicationManager) getApplication();
        //accountInfo = ApiServer.getAccountInfo();
        titleBarUtil = new TitleBarUtil(this, true, true).setTitle("发布活动");
        titleBarUtil.init();

        dbAdapter = DBManager.getInstance().openForNetWork();

        bitmapSize = (int) TypedValueUtil.dip2px(this, 50);

//        if (!accountInfo.isAdmin())
//            return;

        initUI();
        tvSubmit.setOnClickListener(this);
        tvTime.setOnClickListener(this);
        imgAddPic.setOnClickListener(this);
        albumDialog = new AlbumDialog(this);
        loadingDialog = new LoadingDialog(this);
        albumDialog.setOnClickListener(this);

        editTextList.add(edtTxtName);
        editTextList.add(edtTxtMax);
        editTextList.add(edtTxtAddress);
        editTextList.add(edtTxtPhone);
        editTextList.add(edtTxtMsg);

        tvSubmit.post(new Runnable() {
            @Override
            public void run() {
                ((ViewGroup.MarginLayoutParams) tvSubmit.getLayoutParams()).topMargin = titleBarUtil.getSystemBarHeight();
            }
        });
    }

    private void initUI() {
        rcyPic = (RecyclerView) findViewById(R.id.rcy_add_society_pic);
        tvSubmit = (TextView) findViewById(R.id.tv_add_society_submit);
        tvTime = (TextView) findViewById(R.id.tv_add_society_time);
        edtTxtName = (EditText) findViewById(R.id.edtTxt_add_society_name);
        edtTxtMax = (EditText) findViewById(R.id.edtTxt_add_society_max);
        edtTxtWelfare = (EditText) findViewById(R.id.edtTxt_add_society_welfare);
        edtTxtAddress = (EditText) findViewById(R.id.edtTxt_add_society_address);
        edtTxtPhone = (EditText) findViewById(R.id.edtTxt_add_society_phone);
        edtTxtMsg = (EditText) findViewById(R.id.edtTxt_add_society_msg);
        imgAddPic = (ImageView) findViewById(R.id.img_add_society_addPic);

        rcyPic.setLayoutManager(new LinearLayoutManager(this, LinearLayout.HORIZONTAL, false));
        rcyPic.setAdapter(picAdapter);
    }

    @Override
    public void onClick(View v) {
        Intent intent;

        switch (v.getId()) {
            case R.id.tv_add_society_time:
                break;
            case R.id.tv_add_society_submit:
                if (InputUtil.checkNull(this, editTextList, errMsg)) {
                    loadingDialog.show(false);
                    loadingDialog.setMessage("正在上传图片...");
                    //new Thread(submitRunnable).start();
                }
                break;
            case AlbumDialog.FIRST_SELECTOR_ID:
                filePic = new File(directoryManager.getSafePicPath() + "/" + RandomUtil.getRandomString() + ".png");
                while (filePic.exists())
                    filePic = new File(RandomUtil.getRandomString());
                CameraUtil.takePicture(this, filePic);
                break;
            case AlbumDialog.SECOND_SELECTOR_ID:
                intent = new Intent(AddSocietyActivity.this, AlbumGroupActivity.class);
                intent.putExtra(Public.CAPPED, MAX_UPLOAD - bitmapList.size());
                startActivityForResult(intent, REQUEST_ALBUM);
                break;
            case R.id.img_add_society_addPic:
                albumDialog.show();
                break;
            case R.id.photoView_item_add_book:
                String key = PhotoViewBigActivity.class.getSimpleName();
                applicationManager.dataMap.put(key, ((PhotoView) v).getInfo());
                intent = new Intent(this, PhotoViewBigActivity.class);
                intent.putExtra(Public.CONTEXT, key);
                intent.putExtra(Public.POSITION, (int) v.getTag());
                applicationManager.dataMap.put(Public.DATA, pathList);
                applicationManager.dataMap.put(Public.BITMAP, bitmapList.get((int) v.getTag()));
                startActivity(intent);
                break;
            case R.id.img_item_add_book_del:
                int index = (int) v.getTag();
                bitmapList.remove(index);
                pathList.remove(index);
                if (bitmapList.size() < MAX_UPLOAD)
                    imgAddPic.setVisibility(View.VISIBLE);
                picAdapter.notifyDataSetChanged();
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if ((v.getId() == edtTxtMsg.getId() && canVerticalScroll(edtTxtMsg))) {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            if (event.getAction() == MotionEvent.ACTION_UP)
                v.getParent().requestDisallowInterceptTouchEvent(false);
        }
        return false;
    }

    private boolean canVerticalScroll(EditText editText) {
        //滚动的距离
        int scrollY = editText.getScrollY();
        //控件内容的总高度
        int scrollRange = editText.getLayout().getHeight();
        //控件实际显示的高度
        int scrollExtent = editText.getHeight() - editText.getCompoundPaddingTop() - editText.getCompoundPaddingBottom();
        //控件内容总高度与实际显示高度的差值
        int scrollDifference = scrollRange - scrollExtent;

        return scrollDifference != 0 && ((scrollY > 0) || (scrollY < scrollDifference - 1));
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgDel;
        PhotoView photoView;

        ViewHolder(View itemView) {
            super(itemView);

            imgDel = itemView.findViewById(R.id.img_item_add_book_del);
            photoView = itemView.findViewById(R.id.photoView_item_add_book);
            imgDel.setOnClickListener(AddSocietyActivity.this);
            photoView.setOnClickListener(AddSocietyActivity.this);
            photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            photoView.disenable();
        }
    }

    private RecyclerView.Adapter picAdapter = new RecyclerView.Adapter() {
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new ViewHolder(LayoutInflater.from(AddSocietyActivity.this)
                    .inflate(R.layout.item_add_book_pic, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
            ViewHolder holder = (ViewHolder) viewHolder;
            holder.photoView.setImageBitmap(bitmapList.get(i));
            holder.photoView.setTag(i);
            holder.imgDel.setTag(i);
        }

        @Override
        public int getItemCount() {
            return bitmapList.size();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CameraUtil.CAMERA_REQUEST_CODE:
                    pathList.add(filePic.getPath());
                    bitmapList.add(ImageUtil.getBitmap(bitmapSize, bitmapSize, filePic.getPath()));
                    if (bitmapList.size() >= MAX_UPLOAD)
                        imgAddPic.setVisibility(View.GONE);
                    picAdapter.notifyDataSetChanged();
                    break;
                case REQUEST_ALBUM:
                    ArrayList<String> list = data.getStringArrayListExtra(Public.DATA);
                    pathList.addAll(list);
                    RubFormatBm rubFormatBm = new RubFormatBm();
                    rubFormatBm.pathList = new ArrayList<>(list);
                    loadingDialog.show(false);
                    loadingDialog.setMessage("请稍后...  (" + 1 + "/" + list.size() + ")");
                    new Thread(rubFormatBm).start();
                    break;
            }
        }
    }

    private class RubFormatBm implements Runnable {
        ArrayList<String> pathList;

        @Override
        public void run() {
            for (int i = 0; i < pathList.size(); ++i) {
                String path = pathList.get(i);
                bitmapList.add(ImageUtil.getBitmap(bitmapSize, bitmapSize, path));
                final int finalI = i + 1;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        loadingDialog.setMessage("请稍后...  (" + finalI+ "/" + pathList.size() + ")");
                    }
                });
            }
            handler.post(rubFormatDone);
        }
    }

    private Runnable rubFormatDone = new Runnable() {
        @Override
        public void run() {
            if (bitmapList.size() >= MAX_UPLOAD)
                imgAddPic.setVisibility(View.GONE);
            picAdapter.notifyDataSetChanged();
            loadingDialog.dismiss();
        }
    };

    private Runnable submitRunnable = new Runnable() {
        @Override
        public void run() {
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < pathList.size(); ++i) {
                final int finalI = i;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingDialog.setMessage("正在上传图片...  (" + (finalI + 1) + "/" + pathList.size() + ")");
                    }
                });
                try {
                    String path = pathList.get(i);
                    long imgId = Long.valueOf(RandomUtil.getRandomId(DBManager.IMAGE_ID_LENGTH));
                    Bitmap bm = ImageUtil.getBitmap(1080, 1920, path);
                    path = directoryManager.getSafePicPath() + imgId + ".png";
                    NativeUtil.compressBitmap(bm, path, true);
                    dbAdapter.uploadImage(imgId, path);
                    AsyncLoader.saveBitmapToDisk(path);
                    new File(path).delete();
                    builder.append(imgId);
                    builder.append(",");
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadingDialog.setMessage("请稍后...");
                }
            });
            String imgIds = builder.toString();

//            ContentValues contentValues = new ContentValues();
//            contentValues.put(DBManager.MARKET_ID, marketId);
//            contentValues.put(DBManager.MARKET_USER_ID, localAccount.getUserId());
//            contentValues.put(DBManager.MARKET_CONTENT, edtTxtContent.getText().toString());
//            contentValues.put(DBManager.MARKET_PRICE, Float.valueOf(edtTxtPrice.getText().toString()));
//            contentValues.put(DBManager.MARKET_MONEY, Float.valueOf(edtTxtMoney.getText().toString()));
//            contentValues.put(DBManager.MARKET_IMG_LIST, imgIds);
//            contentValues.put(DBManager.MARKET_SCHOOL_ID, localAccount.getSchoolId());
//            contentValues.put(DBManager.MARKET_ISBN, edtTxtISBN.getText().toString());
//            contentValues.put(DBManager.MARKET_BOOK_NAME, edtTxtName.getText().toString());
//            contentValues.put(DBManager.MARKET_BOOK_PUB, edtTxtPress.getText().toString());
//            contentValues.put(DBManager.MARKET_BOOK_AU, edtTxtAuthor.getText().toString());
//            contentValues.put(DBManager.MARKET_PHONE, edtTxtPhone.getText().toString());
//            dbAdapter.insert(DBManager.TABLE.MARKET, contentValues);
//            resultIntent.putExtra(Public.DATA, contentValues);
        }
    };
}
