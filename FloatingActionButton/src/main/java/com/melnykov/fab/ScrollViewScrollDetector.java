package com.melnykov.fab;

import android.widget.ScrollView;

abstract class ScrollViewScrollDetector implements ObservableScrollView.OnScrollChangedListener {
    private int mLastScrollY;
    private int mScrollThreshold;
    private int mSlipHeight;

    abstract void onScrollUp();

    abstract void onScrollDown(boolean isChange);

    protected abstract void setImage(int id);

    @Override
    public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
        boolean isSignificantDelta = Math.abs(t - mLastScrollY) > mScrollThreshold;
        if (isSignificantDelta) {
            if (t > mLastScrollY) {
                onScrollUp();
                mSlipHeight = 0;
                setImage(-1);
            } else {
                if (mSlipHeight >= 50) {
                    setImage(R.drawable.fat_arrow);
                    onScrollDown(true);
                }
                onScrollDown(false);
                mSlipHeight++;
            }
        }
        mLastScrollY = t;
    }

    public void setScrollThreshold(int scrollThreshold) {
        mScrollThreshold = scrollThreshold;
    }
}