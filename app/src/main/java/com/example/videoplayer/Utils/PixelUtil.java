package com.example.videoplayer.Utils;

import android.content.Context;

/**
 * 像素转换工具类
 * 注意: 要使用此工具类，需要先进性初始化上下文的操作
 */
public class PixelUtil {
    private static Context mContext;

    public static void initContext(Context context) {
        mContext = context;
    }

    /**
     * dp转px
     */
    public static int dp2px(float value) {
        final float scale = mContext.getResources().getDisplayMetrics().densityDpi;
        return (int) (value * (scale / 160) + 0.5f);
    }
}




























