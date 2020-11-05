//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class VerticalSeekBar_Reverse extends SeekBar {
    public VerticalSeekBar_Reverse(Context var1) {
        super(var1);
    }

    public VerticalSeekBar_Reverse(Context var1, AttributeSet var2) {
        super(var1, var2);
    }

    public VerticalSeekBar_Reverse(Context var1, AttributeSet var2, int var3) {
        super(var1, var2, var3);
    }

    protected void onDraw(Canvas var1) {
        var1.rotate(90.0F);
        var1.translate(0.0F, (float)(-getWidth()));
        super.onDraw(var1);
    }

    protected void onMeasure(int var1, int var2) {
        try {
            super.onMeasure(var2, var1);
            setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
        } finally {
        }

    }

    protected void onSizeChanged(int var1, int var2, int var3, int var4) {
        super.onSizeChanged(var2, var1, var4, var3);
    }

    public boolean onTouchEvent(MotionEvent var1) {
        if (!isEnabled()) {
            return false;
        } else {
            switch(var1.getAction()) {
                case 0:
                case 1:
                case 2:
                    setProgress(100 - (getMax() - (int)((float)getMax() * var1.getY() / (float)getHeight())));
                    onSizeChanged(getWidth(), getHeight(), 0, 0);
                default:
                    return true;
            }
        }
    }
}
