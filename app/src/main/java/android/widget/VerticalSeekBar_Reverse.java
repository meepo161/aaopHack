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
        var1.translate(0.0F, (float)(-this.getWidth()));
        super.onDraw(var1);
    }

    protected void onMeasure(int var1, int var2) {
        synchronized(this){}

        try {
            super.onMeasure(var2, var1);
            this.setMeasuredDimension(this.getMeasuredHeight(), this.getMeasuredWidth());
        } finally {
            ;
        }

    }

    protected void onSizeChanged(int var1, int var2, int var3, int var4) {
        super.onSizeChanged(var2, var1, var4, var3);
    }

    public boolean onTouchEvent(MotionEvent var1) {
        if (!this.isEnabled()) {
            return false;
        } else {
            switch(var1.getAction()) {
                case 0:
                case 1:
                case 2:
                    this.setProgress(100 - (this.getMax() - (int)((float)this.getMax() * var1.getY() / (float)this.getHeight())));
                    this.onSizeChanged(this.getWidth(), this.getHeight(), 0, 0);
                default:
                    return true;
            }
        }
    }
}
