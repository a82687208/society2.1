package com.gdptc.society.card;

import android.view.View;

/**
 *
 * @描述 ${TODO}
 * @更新者 $Author$
 * @更新时间 $Date$
 * @更新描述 先给数据 在根据数据设置view
 */
public abstract  class BaseCard<E> {
    protected View mView;
    protected E datas;
    public BaseCard(String item1, String item2){
        datas = initData();
        initView();
    }
    protected abstract E initData();

    protected abstract View initView();

    protected View getRootView(){
        return  mView;
    }
}