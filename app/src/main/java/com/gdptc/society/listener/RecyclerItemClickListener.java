package com.gdptc.society.listener;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Administrator on 2016/11/10/010.
 */

public class RecyclerItemClickListener implements RecyclerView.OnItemTouchListener {
    private OnItemClickListener mListener = null;
    private RecyclerView view = null;
    private GestureDetector mGestureDetector = null;

    public interface OnItemClickListener {
        void onItemClick(RecyclerView parent, View child, int position, long id);
        void onItemLongClick(RecyclerView parent, View child, int position, long id);
    }


    public RecyclerItemClickListener(Context context, OnItemClickListener listener) {
        mListener = listener;
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                View childView = view.findChildViewUnder(e.getX(), e.getY());
                if (childView != null && mListener != null) {
                    mListener.onItemClick(view, childView, view.getChildPosition(childView), view.getChildItemId(childView));
                }
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                View childView = view.findChildViewUnder(e.getX(), e.getY());
                if (childView != null && mListener != null)
                    mListener.onItemLongClick(view, childView, view.getChildPosition(childView), view.getChildItemId(childView));
            }
        });
    }

    public OnItemClickListener getClickListener() {
        return mListener;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
        this.view = view;
        mGestureDetector.onTouchEvent(e);
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) { }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) { }
}
