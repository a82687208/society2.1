package com.gdptc.society.ui.holder;

import android.view.View;
import android.widget.ProgressBar;

import com.bm.library.PhotoView;
import com.gdptc.society.R;

/**
 * Created by Administrator on 2018/3/11/011.
 */

public class GalleryHolder {
    public View itemView;
    public View loadingBg;
    public PhotoView photoView;
    public ProgressBar loading;
    public String path;
    public String id;

    public GalleryHolder(View itemView) {
        this.itemView = itemView;
        photoView = itemView.findViewById(R.id.item_gallery_child_photoView);
        loading = itemView.findViewById(R.id.item_gallery_child_loading);
        loadingBg = itemView.findViewById(R.id.item_gallery_child_loadingBg);
        photoView.setTag(this);
    }
}
