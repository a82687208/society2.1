/*
 * Copyright (c) 2014 Android Alliance, LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.basemodel;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import com.example.basemodel.R;

public class BaseContextWrapper extends ContextWrapper {
    private final String TAG = BaseContextWrapper.class.getSimpleName();

    private ResourcesEdgeEffect mResourcesEdgeEffect;
    private int mColor;
    private Drawable mEdgeDrawable;
    private Drawable mGlowDrawable;

    public BaseContextWrapper(Context context) {
        this(context, 0);
    }

    public BaseContextWrapper(Context context, int color) {
        super(context);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mColor = color;
            Resources resources = context.getResources();
            mResourcesEdgeEffect = new ResourcesEdgeEffect(resources.getAssets(), resources.getDisplayMetrics(), resources.getConfiguration());
        }
    }

    public void setEdgeEffectColor(int color) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mColor = color;
            if (mEdgeDrawable != null) mEdgeDrawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
            if (mGlowDrawable != null) mGlowDrawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        }
    }

    @Override
    public Resources getResources() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ? mResourcesEdgeEffect : super.getResources();
    }

    private class ResourcesEdgeEffect extends Resources {
        private int overScrollEdgeId = getPlatformDrawableId("overscroll_edge");
        private int overScrollGlowId = getPlatformDrawableId("overscroll_glow");

        public ResourcesEdgeEffect(AssetManager assets, DisplayMetrics metrics, Configuration config) {
            super(assets, metrics, config);
        }

        private int getPlatformDrawableId(String name) {
            try {
                return (int) (Integer) Class.forName("com.android.internal.R$drawable").getField(name).get(null);
            }
            catch (ClassNotFoundException e) {
                Log.e(TAG, "Cannot find internal resource class");
            }
            catch (NoSuchFieldException e1) {
                Log.e(TAG, "Internal resource id does not exist: " + name);
            }
            catch (IllegalArgumentException | IllegalAccessException e2) {
                Log.e(TAG, "Cannot access internal resource id: " + name);
            }
            return 0;
        }

        @Override
        public Drawable getDrawable(int resId) throws NotFoundException {
            Drawable ret;
            if (resId == this.overScrollEdgeId) {
                mEdgeDrawable = BaseContextWrapper.this.getBaseContext().getResources().getDrawable(R.drawable.overscroll_edge);
                ret = mEdgeDrawable;
            }
            else if (resId == this.overScrollGlowId) {
                mGlowDrawable = BaseContextWrapper.this.getBaseContext().getResources().getDrawable(R.drawable.overscroll_glow);
                ret = mGlowDrawable;
            }
            else return super.getDrawable(resId);

            if (ret != null)
                ret.setColorFilter(mColor, PorterDuff.Mode.MULTIPLY);

            return ret;
        }
    }
}
