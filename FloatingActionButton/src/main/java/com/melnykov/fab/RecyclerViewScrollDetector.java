package com.melnykov.fab;

import android.support.v7.widget.RecyclerView;

abstract class RecyclerViewScrollDetector extends RecyclerView.OnScrollListener {
    private int mScrollThreshold;
    private int mSlipHeight;

    protected abstract void setImage(int id);

    abstract void onScrollUp();

    abstract void onScrollDown(boolean isChange);

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        boolean isSignificantDelta = Math.abs(dy) > mScrollThreshold;
        if (isSignificantDelta) {
            if (dy > 0) {
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
    }

    public void setScrollThreshold(int scrollThreshold) {
        mScrollThreshold = scrollThreshold;
    }
}