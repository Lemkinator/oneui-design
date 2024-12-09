package dev.oneuiproject.oneui.utils;

import static androidx.appcompat.util.SeslRoundedCorner.ROUNDED_CORNER_NONE;

import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.reflect.DeviceInfo;

import dev.oneuiproject.oneui.utils.internal.ReflectUtils;

public class ViewUtils {
    private static final String TAG = "ViewUtils";
    @Deprecated
    public static final int SEM_ROUNDED_CORNER_NONE = 0;
    @Deprecated
    public static final int SEM_ROUNDED_CORNER_ALL = 15;
    @Deprecated
    public static final int SEM_ROUNDED_CORNER_TOP_LEFT = 1;
    @Deprecated
    public static final int SEM_ROUNDED_CORNER_TOP_RIGHT = 2;
    @Deprecated
    public static final int SEM_ROUNDED_CORNER_BOTTOM_LEFT = 4;
    @Deprecated
    public static final int SEM_ROUNDED_CORNER_BOTTOM_RIGHT = 8;

    public static int semGetRoundedCorners(@NonNull View view) {
        if (Build.VERSION.SDK_INT >= 28) {
            if (view != null) {
                return (int) ReflectUtils.genericInvokeMethod(View.class,
                        view, "semGetRoundedCorners");
            } else {
                Log.e(TAG, "semGetRoundedCorners: view is null");
            }
        } else {
            Log.e(TAG, "semGetRoundedCorners: this method is available only on " +
                    "api 28 and above");
        }

        return ROUNDED_CORNER_NONE;
    }

    public static void semSetRoundedCorners(@NonNull View view, int corners) {
        if (DeviceInfo.isOneUI() && Build.VERSION.SDK_INT >= 28) {
            if (view != null) {
                ReflectUtils.genericInvokeMethod(View.class,
                        view, "semSetRoundedCorners", corners);
            } else {
                Log.e(TAG, "semSetRoundedCorners: view is null");
            }
        } else {
            Log.e(TAG, "semSetRoundedCorners: this method is available only on " +
                    "api 28 and above");
        }
    }

    @ColorInt
    public static int semGetRoundedCornerColor(@NonNull View view, int corner) {
        if (Build.VERSION.SDK_INT >= 28) {
            if (view != null) {
                return (int) ReflectUtils.genericInvokeMethod(View.class,
                        view, "semGetRoundedCornerColor", corner);
            } else {
                Log.e(TAG, "semGetRoundedCornerColor: view is null");
            }
        } else {
            Log.e(TAG, "semSetRoundedCornerColor: this method is available only on " +
                    "api 28 and above");
        }

        return Color.WHITE;
    }

    public static void semSetRoundedCornerColor(@NonNull View view,
                                                int corners, @ColorInt int color) {
        if (Build.VERSION.SDK_INT >= 28) {
            if (view != null) {
                ReflectUtils.genericInvokeMethod(View.class,
                        view, "semSetRoundedCornerColor", corners, color);
            } else {
                Log.e(TAG, "semSetRoundedCornerColor: view is null");
            }
        } else {
            Log.e(TAG, "semSetRoundedCornerColor this method is available only on " +
                    "api 28 and above");
        }
    }
}
