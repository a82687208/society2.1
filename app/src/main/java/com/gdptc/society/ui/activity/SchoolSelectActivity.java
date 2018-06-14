package com.gdptc.society.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.basemodel.BaseRecyclerView;
import com.example.basemodel.Value;
import com.gdptc.society.Public;
import com.gdptc.society.R;
import com.gdptc.society.apiServer.ApiServer;
import com.gdptc.society.apiServer.SchoolInfo;
import com.gdptc.society.base.BaseActivity;
import com.gdptc.society.listener.RecyclerItemClickListener;
import com.gdptc.society.manager.DBManager;
import com.gdptc.society.tools.AnimationUtil;
import com.gdptc.society.tools.AsyncLoader;
import com.gdptc.society.tools.BitmapInfo;
import com.gdptc.society.tools.ImageUtil;
import com.gdptc.society.tools.SearchBoxUtil;
import com.gdptc.society.tools.TitleBarUtil;
import com.gdptc.society.tools.TypedValueUtil;
import com.gdptc.society.ui.view.SideBar;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.Runtime.getRuntime;

/**
 * Created by Administrator on 2017/5/4/004.
 */

public class SchoolSelectActivity extends BaseActivity {
    private BaseRecyclerView rcyProvince, rcySchool;
    private SideBar sideBar;
    private LinearLayout lytLoadingLayout;
    private TextView tvTitle, tvSearchNoData;

    private DBManager.NetworkDBAdapter dbAdapter;
    private ApiServer apiServer;
    private AsyncLoader asyncLoader;

    private SearchBoxUtil searchBoxUtil;

    private List<SchoolInfo> searchList = new ArrayList<>();
    private HashMap<Character, List<String>> provinceMap = new HashMap<>();
    private SparseArray<Character> labelMap = new SparseArray<>();
    private HashMap<String, List<SchoolInfo>> schoolMap = new HashMap<>();

    private Animation inAnimation;

    private int[] unClickPosition = new int[26];
    private int[] dataSizes = new int[26];

    private int logoSize;

    private int categoryHeight;
    private int childHeight;
    private int paddingLeft;
    private int keyCount = 0;

    private boolean inSearch = false;

    private Handler handler;

    private Bitmap normalPic;

    private String toastMsg, provinceSelectKey;

    private Runnable toastRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(SchoolSelectActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
        }
    };

    private void Init() {
        tvSearchNoData = (TextView) findViewById(R.id.tv_school_select_noDataResult);
        tvTitle = (TextView) findViewById(R.id.tv_toolbar_title);
        lytLoadingLayout = (LinearLayout) findViewById(R.id.lyt_item_loading_mainLayout);
        searchBoxUtil.setVisibility(View.GONE);

        sideBar = (SideBar) findViewById(R.id.activity_channel_siBar);

        rcyProvince = (BaseRecyclerView) findViewById(R.id.rcy_school_select_province);
        rcyProvince.setLayoutManager(new LinearLayoutManager(this));
        rcySchool = (BaseRecyclerView) findViewById(R.id.rcy_school_select_school);
        rcySchool.setLayoutManager(new LinearLayoutManager(this));
        categoryHeight = (int) TypedValueUtil.dip2px(SchoolSelectActivity.this, 25);
        childHeight = (int) TypedValueUtil.dip2px(SchoolSelectActivity.this, 45);
        paddingLeft = (int) TypedValueUtil.dip2px(SchoolSelectActivity.this, 10);

        inAnimation = AnimationUtil.getAlphaAnimation(0.0f, 1.0f, 300, false);
        normalPic = ((BitmapDrawable) getResources().getDrawable(R.mipmap.ic_launcher)).getBitmap();
    }

    private void setListener() {
        searchBoxUtil.addTextChangedListener(textWatcher);
        inAnimation.setAnimationListener(animationListener);
        RecyclerItemClickListener itemClickListener = new RecyclerItemClickListener(this, onItemClickListener);
        rcyProvince.addOnItemTouchListener(itemClickListener);
        rcySchool.addOnItemTouchListener(itemClickListener);
        rcySchool.setOnScrollListener(onScrollListener);
        sideBar.setOnStrSelectCallBack(sideBarSelectCallBack);
        new TitleBarUtil(this, true, true).setTitle("选择省份").init();
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (searchBoxUtil.length() == 0) {
                inSearch = false;
                tvSearchNoData.setVisibility(View.GONE);
                handler.removeCallbacks(rubSearch);
                if (provinceSelectKey == null) {
                    rcyProvince.setVisibility(View.VISIBLE);
                    rcyProvince.startAnimation(inAnimation);
                    rcySchool.setVisibility(View.GONE);
                    sideBar.setVisibility(View.VISIBLE);
                    tvTitle.setText("选择省份");
                } else {
                    rcySchool.setVisibility(View.VISIBLE);
                    rcySchool.startAnimation(inAnimation);
                    rcySchool.setAdapter(schoolAdapter);
                    tvTitle.setText("选择学校");
                }
            } else {
                handler.removeCallbacks(rubSearch);
                tvSearchNoData.setVisibility(View.GONE);
                handler.postDelayed(rubSearch, 500);
                if (provinceSelectKey == null) {
                    rcyProvince.setVisibility(View.GONE);
                    sideBar.setVisibility(View.GONE);
                } else
                    rcySchool.setVisibility(View.GONE);
                rcySchool.setAdapter(null);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private Animation.AnimationListener animationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (animation.equals(inAnimation)) {
                rcyProvince.setAnimation(null);
                rcySchool.setAnimation(null);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    };

    private RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE)
                asyncLoader.restartAllTask();
            else
                asyncLoader.pauseRunTask();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_select);
        enabledScrollFinish(false);

        handler = new Handler();
        searchBoxUtil = new SearchBoxUtil(this);
        searchBoxUtil.setTitle("搜索学校");
        searchBoxUtil.setHint("请输入学校名称");
        asyncLoader = new AsyncLoader(this, getRuntime().availableProcessors() * 2);
        asyncLoader.setImageLoadListener(imageLoadListener);
        dbAdapter = DBManager.getInstance().openForNetWork(dbResultListener);
        apiServer = ApiServer.getInstance();

        logoSize = getResources().getDimensionPixelSize(R.dimen.item_book_comment_userPicSize);

        Init();
        setListener();
        onUIColorChange(Value.getColorUI());
        dbAdapter.open();
    }

    private class ProvinceViewHolder extends RecyclerView.ViewHolder {
        TextView content;
        View line;

        ProvinceViewHolder(View itemView) {
            super(itemView);
            content = (TextView) itemView.findViewById(R.id.tv_item_school_select_province_name);
            line = itemView.findViewById(R.id.view_item_school_select_province_line);
        }
    }

    private class SchoolViewHolder extends RecyclerView.ViewHolder {
        ImageView logo;
        TextView name, oldName;
        View line;

        public SchoolViewHolder(View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.circleImg_item_school_select_pic);
            name = itemView.findViewById(R.id.tv_item_school_select_school_name);
            oldName = itemView.findViewById(R.id.tv_item_school_select_school_old_name);
            line = itemView.findViewById(R.id.view_item_school_select_school_line);
        }
    }

    private RecyclerView.Adapter provinceAdapter = new RecyclerView.Adapter() {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ProvinceViewHolder(LayoutInflater.from(SchoolSelectActivity.this)
                    .inflate(R.layout.item_school_select_province, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ProvinceViewHolder provinceViewHolder = (ProvinceViewHolder) holder;
            int[] location = getDataLocation(position);

            if (labelMap.indexOfKey(position) >= 0) {
                provinceViewHolder.itemView.getLayoutParams().height = categoryHeight;
                provinceViewHolder.content.setPadding(0, 0, 0, 0);
                provinceViewHolder.itemView.setEnabled(false);
                provinceViewHolder.itemView.setBackgroundColor(getResources().getColor(R.color.backgroundColor));
                provinceViewHolder.content.setText(String.valueOf(labelMap.get(position)));
                return;
            }

            String text = provinceMap.get((char) ('A' + location[0])).get(location[1]);
            provinceViewHolder.content.setText(text);
            provinceViewHolder.itemView.getLayoutParams().height = childHeight;
            provinceViewHolder.content.setPadding(paddingLeft, 0, 0, 0);
            provinceViewHolder.itemView.setBackgroundResource(R.drawable.layout_selector_white);
            provinceViewHolder.itemView.setEnabled(true);
        }

        @Override
        public int getItemCount() {
            int size = 0;
            for (int i : dataSizes)
                size += i;

            return size + keyCount;
        }
    };

    private RecyclerView.Adapter schoolAdapter = new RecyclerView.Adapter() {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new SchoolViewHolder(LayoutInflater.from(SchoolSelectActivity.this)
                    .inflate(R.layout.item_school_select_school, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
            SchoolViewHolder holder = (SchoolViewHolder) viewHolder;
            SchoolInfo schoolInfo = inSearch ? searchList.get(i) : schoolMap.get(provinceSelectKey).get(i);

            if (holder.logo.getTag() != null)
                asyncLoader.cancelTask(holder.logo, holder.logo.getTag(), ImageUtil.MODE.ROUND, logoSize, logoSize);
            holder.logo.setTag(schoolInfo.getLogo());
            Bitmap bitmap = asyncLoader.loadImgForNetWork(holder.logo,
                    schoolInfo.getLogo(), logoSize, logoSize, ImageUtil.MODE.ROUND);
            if (bitmap != null)
                holder.logo.setImageBitmap(bitmap);
            else
                holder.logo.setImageBitmap(normalPic);

            holder.name.setText(schoolInfo.getName());
            if (schoolInfo.getOld_name() != null) {
                holder.oldName.setVisibility(View.VISIBLE);
                holder.oldName.setText("曾用名: " + schoolInfo.getOld_name());
            } else
                holder.oldName.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return inSearch ? searchList.size() : schoolMap.get(provinceSelectKey).size();
        }

    };

    private RecyclerItemClickListener.OnItemClickListener onItemClickListener =
            new RecyclerItemClickListener.OnItemClickListener() {
                @Override
                public void onItemClick(RecyclerView parent, View child, int position, long id) {
                    if (parent.equals(rcyProvince)) {
                        if (labelMap.indexOfKey(position) < 0) {
                            int location[] = getDataLocation(position);

                            String province = provinceMap.get((char) ('A' + location[0])).get(location[1]);
                            if (!schoolMap.containsKey(province)) {
                                rcyProvince.setVisibility(View.GONE);
                                lytLoadingLayout.setVisibility(View.VISIBLE);
                                dbAdapter.query(DBManager.TABLE.SCHOOL, DBManager.SCHOOL_SIMPLE_RESULT, DBManager.SCHOOL_PROVINCE + " like '"
                                        + province + "'", null, null, null, null, "加载学校列表", province);
                            } else {
                                provinceSelectKey = province;
                                rcyProvince.setVisibility(View.GONE);
                                sideBar.setVisibility(View.GONE);
                                rcySchool.setVisibility(View.VISIBLE);
                                rcySchool.startAnimation(inAnimation);
                                rcySchool.setAdapter(schoolAdapter);
                                tvTitle.setText("选择学校");
                            }
                            // Toast.makeText(SchoolSelectActivity.this, province, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Intent intent = new Intent(SchoolSelectActivity.this,MainActivity.class);
                         //Bundle date=new Bundle();


                        if (!inSearch) {
                            intent.putExtra(Public.SELECT, schoolMap.get(provinceSelectKey).get(position));
                             // intent.putExtra( "date",date);
                        } else {
                            intent.putExtra(Public.SELECT, searchList.get(position));
                             // intent.putExtra( "date",date);
                        }

                        /*SharedPreferences preferences = getApplicationContext().getSharedPreferences("school", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("info",String.valueOf(schoolMap.get(provinceSelectKey).get(position)));
                        editor.commit();*/

                        setResult(2, intent);
                        Toast.makeText(SchoolSelectActivity.this, schoolMap.get(provinceSelectKey).get(position).getName(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onItemLongClick(RecyclerView parent, View child, int position, long id) {
                }
            };

    /*
    * 返回：数组[0] 从'A'开始的ASCII码偏移量，数组[1] 对应数据组内的下标值
    * 返回-1说明计算有误
     */
    private int[] getDataLocation(int position) {
        int[] location = new int[2];

        //对应从标签'A'开始数据组大小
        for (int i = 0; i < dataSizes.length; ++i) {
            if (dataSizes[i] != 0) {                    //如果这个字母标签有数据则计算
                --position;                             //每成功读取到一个标签就消耗掉了一个位置
                if (position > dataSizes[i])            //如果当前位置还是大于数据组大小的话则消耗
                    position -= dataSizes[i];
                else {                                  //否则就定位到对应数据的位置
                    location[0] = i;                    //读取了多少标签就有多少偏移量
                    location[1] = position;             //position已被消耗到正确对应的位置
                    return location;
                }
            }
        }

        location[0] = -1;
        location[1] = -1;
        return location;
    }

    private DBManager.ResultListener dbResultListener = new DBManager.ResultListener() {

        @Override
        public void dbOpen() {
            for (int i = 'A'; i <= 'Z'; ++i) {
                dbAdapter.query(DBManager.TABLE.PROVINCE, new String[]{DBManager.PROVINCE_NAME},
                        DBManager.PROVINCE_INDEX + " like '" + (char) i + "'", null, null, null, null,
                        "搜索省份", i - 'A');
            }
        }

        @Override
        public void insert(Object o1, Object o2, int result) {
        }

        @Override
        public void delete(Object o1, Object o2, int result) {
        }

        @Override
        public void update(Object o1, Object o2, int result) {
        }

        @Override
        public void query(Object o1, Object o2, ResultSet resultSet) {
            switch ((String) o1) {
                case "搜索省份":
                    int i = (int) o2;
                    try {
                        if (!resultSet.wasNull()) {
                            List<String> list = new ArrayList<>();
                            while (resultSet.next())
                                list.add(resultSet.getString(DBManager.PROVINCE_NAME));
                            if (list.size() > 0) {
                                provinceMap.put((char) (i + 'A'), list);
                                dataSizes[i] = list.size();
                                ++keyCount;
                            } else
                                dataSizes[i] = 0;
                        } else {
                            toastMsg = "连接数据库失败";
                            handler.post(toastRunnable);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    int lastIndex = 0;
                    int lastSize = 0;
                    if (i + 'A' == 'Z') {
//                    for (i = 0; i < dataSizes.length; ++i)
//                        Log.e(SchoolSelectActivity.class.getSimpleName(), String.valueOf((char) (i + 'A')) + ": " + dataSizes[i] + "");
                        final ArrayList<String> arrayList = new ArrayList<>();
                        for (int j = 'A'; j <= 'Z'; ++j) {
                            List<String> list = provinceMap.get((char) (j));      //根据标签获取对应的list大小

                            int index = j - 'A';
                            if (list != null) {                              //上一个不可点击位置加上上一个list大小再加上多出了一个标签即是当前不可点击位置
                                if (lastSize == 0)                           //第一个标签位置始终为0
                                    unClickPosition[index] = 0;
                                else {                                      //因此导致对应值出现偏移，需特殊处理
                                    if (unClickPosition[lastIndex] == 0)
                                        unClickPosition[index] = lastSize + list.size();
                                    else
                                        unClickPosition[index] = unClickPosition[lastIndex] + lastSize + 1;
                                }
                                lastIndex = index;
                                lastSize = list.size();
                                labelMap.put(unClickPosition[index], (char) (j));
                                arrayList.add(String.valueOf((char) j));
                            } else                                            //不存在的标签将位置设为-1
                                unClickPosition[index] = -1;
                        }
//                    for (i = 0; i < unClickPosition.length; ++i)
//                        Log.e(SchoolSelectActivity.class.getSimpleName(), String.valueOf((char) ('A' + i)) + ": " + unClickPosition[i]);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                lytLoadingLayout.setVisibility(View.GONE);
                                searchBoxUtil.setVisibility(View.VISIBLE);
                                rcyProvince.setVisibility(View.VISIBLE);
                                sideBar.setVisibility(View.VISIBLE);
                                sideBar.setLetters(arrayList.toArray(new String[]{}));
                                arrayList.clear();
                                rcyProvince.setAdapter(provinceAdapter);
                            }
                        });
                    }
                    break;
                case "加载学校列表":
                    try {
                        if (!resultSet.wasNull()) {
                            List<SchoolInfo> infoList = new ArrayList<>();

                            while (resultSet.next())
                                addSchoolInfo(resultSet, infoList);

                            if (infoList.size() != 0) {
                                provinceSelectKey = (String) o2;
                                schoolMap.put(provinceSelectKey, infoList);
                                handler.post(rubSchoolDataResult);
                            }
                        } else
                            onFailure(o1, o2, new Exception("数据库异常"));
                    } catch (SQLException e) {
                        onFailure(o1, o2, new Exception("数据库异常"));
                    }
                    break;
                case "搜索学校":
                    try {
                        if (!resultSet.wasNull()) {
                            while (resultSet.next())
                                addSchoolInfo(resultSet, searchList);
                            if (searchList.size() != 0)
                                handler.post(rubSchoolDataResult);
                            else
                                runOnUiThread(rubShowNoData);
                        } else
                            runOnUiThread(rubShowNoData);
                    } catch (SQLException e) {
                        runOnUiThread(rubShowNoData);
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onFailure(Object o1, Object o2, Exception e) {
            if (!dbAdapter.isOpen()) {
                toastMsg = "连接数据库失败";
                handler.post(toastRunnable);
            }
            e.printStackTrace();
        }
    };

    private void addSchoolInfo(ResultSet resultSet, List<SchoolInfo> targetList) throws SQLException {
        SchoolInfo schoolInfo = new SchoolInfo();
        schoolInfo.setCollege_id(resultSet.getLong(DBManager.SCHOOL_ID));
        schoolInfo.setAddr(resultSet.getString(DBManager.SCHOOL_ADDR));
        schoolInfo.setLogo(resultSet.getString(DBManager.SCHOOL_LOGO));
        schoolInfo.setType(resultSet.getString(DBManager.SCHOOL_TYPE));
        schoolInfo.setProvince(resultSet.getString(DBManager.SCHOOL_PROVINCE));
        schoolInfo.setName(resultSet.getString(DBManager.SCHOOL_NAME));
        schoolInfo.setOld_name(resultSet.getString(DBManager.SCHOOL_OLD_NAME));
        targetList.add(schoolInfo);
    }

    private Runnable rubSearch = new Runnable() {
        @Override
        public void run() {
            inSearch = true;
            searchList.clear();
            String name = searchBoxUtil.getText().toString();
            dbAdapter.query(DBManager.TABLE.SCHOOL, DBManager.SCHOOL_SIMPLE_RESULT, DBManager.SCHOOL_NAME
                            + " like '?' or " + DBManager.SCHOOL_OLD_NAME + " like '%?%'",
                    new String[]{name, name}, null, null, null, "搜索学校", provinceSelectKey + "," + name);
        }
    };

    private Runnable rubShowNoData = new Runnable() {
        @Override
        public void run() {
            tvSearchNoData.setVisibility(View.VISIBLE);
        }
    };

    private Runnable rubChangeTitle = new Runnable() {
        @Override
        public void run() {
            tvTitle.setText("选择学校");
        }
    };

    private Runnable rubSchoolDataResult = new Runnable() {
        @Override
        public void run() {
            lytLoadingLayout.setVisibility(View.GONE);
            rcyProvince.setVisibility(View.GONE);
            sideBar.setVisibility(View.GONE);
            rcySchool.setVisibility(View.VISIBLE);
            rcySchool.startAnimation(inAnimation);
            rcySchool.setAdapter(schoolAdapter);
            tvTitle.setText("选择学校");
            schoolAdapter.notifyDataSetChanged();
        }
    };

    private SideBar.ISideBarSelectCallBack sideBarSelectCallBack = new SideBar.ISideBarSelectCallBack() {
        private int itemNumber = 0;

        @Override
        public void onSelectStr(int index, String selectStr) {
            int firstItem = rcyProvince.getChildLayoutPosition(rcyProvince.getChildAt(0));
            int lastItem = rcyProvince.getChildLayoutPosition(rcyProvince.getChildAt(rcyProvince.getChildCount() - 1));
            if (itemNumber == 0)
                itemNumber = lastItem - firstItem;
            if (index < unClickPosition.length - 1) {
                int position = unClickPosition[selectStr.charAt(0) - 'A'];
                if (position < firstItem)
                    rcyProvince.smoothScrollToPosition(position);
                else if (position <= lastItem) {
                    int movePosition = position - firstItem;
                    if (movePosition >= 0 && movePosition < rcyProvince.getChildCount()) {
                        int top = rcyProvince.getChildAt(movePosition).getTop();
                        rcyProvince.smoothScrollBy(0, top);
                    }
                } else
                    rcyProvince.smoothScrollToPosition(position + itemNumber - 1);
            } else if (index == unClickPosition.length - 1)
                rcyProvince.smoothScrollToPosition(provinceAdapter.getItemCount() - 1);
        }
    };

    private AsyncLoader.ImageLoadListener imageLoadListener = new AsyncLoader.ImageLoadListener() {
        @Override
        public void onImageLoadDone(Object parent, Object id, BitmapInfo bitmapInfo, int width, int height, ImageUtil.MODE mode) {
            ImageView circleImageView = (ImageView) parent;
            Bitmap bitmap = bitmapInfo.getBitmap(SchoolSelectActivity.this, circleImageView);
            if (circleImageView.getTag().equals(id))
                circleImageView.setImageBitmap(bitmap);
            asyncLoader.saveBitmapToLru(id, bitmapInfo, width, height, mode);
        }

        @Override
        public void onImageLoadFailure(Object parent, Object id, int width, int height, ImageUtil.MODE mode, Exception e) {

        }
    };

    @Override
    public void onUIColorChange(Value.COLOR color) {
        sideBar.setTextColor(color.toValue());
        rcySchool.setEdgeEffectColor(color.toValue());
        rcyProvince.setEdgeEffectColor(color.toValue());
    }

    @Override
    public void onBackPressed() {
        if (searchBoxUtil.length() != 0)
            searchBoxUtil.setText(null);
        else if (provinceSelectKey != null) {
            provinceSelectKey = null;
            rcySchool.setVisibility(View.GONE);
            rcySchool.setAdapter(null);
            rcyProvince.setVisibility(View.VISIBLE);
            rcyProvince.startAnimation(inAnimation);
            sideBar.setVisibility(View.VISIBLE);
            tvTitle.setText("选择省份");
        } else
            super.onBackPressed();
    }

    @Override
    public void finish() {
        dbAdapter.close();
        asyncLoader.exitLoader();
        super.finish();
    }
}
