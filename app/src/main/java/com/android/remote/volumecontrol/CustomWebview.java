package com.android.remote.volumecontrol;
import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.webkit.WebView;

public class CustomWebview extends WebView {
    Context c;
    public CustomWebview(Context context, AttributeSet attrs) {
        super(context, attrs);
        c = context;
    }

    @TargetApi(24)
    @Override
    public PointerIcon onResolvePointerIcon(MotionEvent me, int pointerIndex) {
        return PointerIcon.getSystemIcon(c, PointerIcon.TYPE_CROSSHAIR);
    }
}