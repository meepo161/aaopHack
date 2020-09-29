package ru.avem.aaophack;


import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.ScaleGestureDetector.OnScaleGestureListener;

public class Plot2d extends View {
    private int axesLblVisible = 1;
    public int axisColor = -16777216;
    public int backColor = -1;
    private boolean frame;
    private final GestureDetector gestureDetector;
    DashPathEffect gridDPE;
    private float locxAxis;
    private float locyAxis;
    private float margin;
    DashPathEffect markDPE;
    private float maxx;
    private float maxy;
    private float minx;
    private float miny;
    private Activity pActivity;
    private Paint paint;
    private int plotCount = 0;
    public Plot2d.Plot[] plots;
    private float scaleFactorX = 1.0F;
    private float scaleFactorY = 1.0F;
    private final ScaleGestureDetector scaleGestureDetector;
    private float smaxx;
    private float smaxy;
    private float sminx;
    private float sminy;
    private float strokeWidth = 4.0F;
    Rect textBounds;
    private int vectorLength;
    private int vectorOffset;
    Path wallpath;
    private Plot2d.Axis xAxis;
    public Plot2d.XMark[] xmarks;
    private Plot2d.Axis yAxis;

    public Plot2d(Context context, int var2, int _const101, int axesLblVisible) {
        super(context);
        this.pActivity = (Activity)context;
        this.gestureDetector = new GestureDetector(context, new Plot2d.p2dGestureListener());
        this.gestureDetector.setOnDoubleTapListener(new Plot2d.p2dDoubleTapListener());
        this.scaleGestureDetector = new ScaleGestureDetector(context, new Plot2d.p2dScaleGestureListener());
        this.plots = new Plot2d.Plot[var2];
        this.axesLblVisible = axesLblVisible;
        this.plotCount = this.plots.length;
        if (this.plotCount > 0) {
            for(var2 = 0; var2 < this.plotCount; ++var2) {
                this.plots[var2] = new Plot2d.Plot(_const101);
            }

            this.setVectorLength(this.plots[0].xvalues.length);
        } else {
            this.setVectorLength(0);
        }

        this.vectorOffset = 0;
        this.margin = 0.05F;
        this.frame = true;
        this.paint = new Paint();
        this.xAxis = new Plot2d.Axis();
        this.yAxis = new Plot2d.Axis();
        this.xmarks = new Plot2d.XMark[10];

        for(var2 = 0; var2 < 10; ++var2) {
            this.xmarks[var2] = new Plot2d.XMark(var2);
        }

        this.gridDPE = new DashPathEffect(new float[]{2.0F, 5.0F}, 0.0F);
        this.markDPE = new DashPathEffect(new float[]{5.0F, 5.0F}, 0.0F);
        this.wallpath = new Path();
        this.textBounds = new Rect();
        this.getAxes();
    }

    private float fromPixel(float var1, float var2, float var3, int var4, float var5) {
        return (float)((double)((var3 - var2) * ((float)var4 - var5 * var1) / ((1.0F - 2.0F * var5) * var1) + var2));
    }

    private void getAxes() {
        this.maxx = this.getMaxX();
        this.maxy = this.getMaxY();
        this.minx = this.getMinX();
        this.miny = this.getMinY();
        if (this.maxx == this.minx) {
            this.maxx = (float)((double)this.maxx + 0.5D);
            this.minx = (float)((double)this.minx - 0.5D);
        }

        if (this.maxy == this.miny) {
            this.maxy = (float)((double)this.maxy + 0.5D);
            this.miny = (float)((double)this.miny - 0.5D);
        }

        if (this.yAxis.auto) {
            if (this.minx >= 0.0F) {
                this.locyAxis = this.minx;
            } else if (this.minx < 0.0F && this.maxx >= 0.0F) {
                this.locyAxis = 0.0F;
            } else {
                this.locyAxis = this.maxx;
            }
        } else {
            this.locyAxis = this.yAxis.zero;
        }

        if (this.xAxis.auto) {
            if (this.miny >= 0.0F) {
                this.locxAxis = this.miny;
            } else if (this.miny < 0.0F && this.maxy >= 0.0F) {
                this.locxAxis = 0.0F;
            } else {
                this.locxAxis = this.maxy;
            }
        } else {
            this.locxAxis = this.xAxis.zero;
        }
    }

    private float getMax(float[] var1, int var2) {
        int var5 = var2;
        if (var2 >= var1.length) {
            var5 = 0;
        }

        float var3;
        float var4;
        for(var3 = var1[var5]; var5 < var1.length; var3 = var4) {
            var4 = var3;
            if (var1[var5] > var3) {
                var4 = var1[var5];
            }

            ++var5;
        }

        return var3;
    }

    private float getMaxX() {
        boolean var5 = false;
        float var1 = this.xAxis.max;
        float var2 = var1;
        if (this.xAxis.auto) {
            if (this.xAxis.autorange > 0.0F && this.xAxis.autozero) {
                var2 = (float)((double)this.xAxis.zero + 0.5D * (double)this.xAxis.autorange);
            } else {
                int var4 = 0;

                while(true) {
                    var2 = var1;
                    if (var4 >= this.plotCount) {
                        break;
                    }

                    float var3 = this.getMax(this.plots[var4].xvalues, this.vectorOffset);
                    boolean var6 = var5;
                    var2 = var1;
                    if (this.plots[var4].visible) {
                        label37: {
                            if (var5) {
                                var6 = var5;
                                var2 = var1;
                                if (var1 >= var3) {
                                    break label37;
                                }
                            }

                            var2 = var3;
                            var6 = true;
                        }
                    }

                    ++var4;
                    var5 = var6;
                    var1 = var2;
                }
            }
        }

        return var2;
    }

    private float getMaxY() {
        boolean var5 = false;
        float var1 = this.yAxis.max;
        float var2 = var1;
        if (this.yAxis.auto) {
            if (this.yAxis.autorange > 0.0F && this.yAxis.autozero) {
                var2 = (float)((double)this.yAxis.zero + 0.5D * (double)this.yAxis.autorange);
            } else {
                int var4 = 0;

                while(true) {
                    var2 = var1;
                    if (var4 >= this.plotCount) {
                        break;
                    }

                    float var3 = this.getMax(this.plots[var4].yvalues, this.vectorOffset);
                    boolean var6 = var5;
                    var2 = var1;
                    if (this.plots[var4].visible) {
                        label37: {
                            if (var5) {
                                var6 = var5;
                                var2 = var1;
                                if (var1 >= var3) {
                                    break label37;
                                }
                            }

                            var2 = var3;
                            var6 = true;
                        }
                    }

                    ++var4;
                    var5 = var6;
                    var1 = var2;
                }
            }
        }

        return var2;
    }

    private float getMin(float[] var1, int var2) {
        int var5 = var2;
        if (var2 >= var1.length) {
            var5 = 0;
        }

        float var3;
        float var4;
        for(var3 = var1[var5]; var5 < var1.length; var3 = var4) {
            var4 = var3;
            if (var1[var5] < var3) {
                var4 = var1[var5];
            }

            ++var5;
        }

        return var3;
    }

    private float getMinX() {
        boolean var5 = false;
        float var1 = this.xAxis.min;
        float var2 = var1;
        if (this.xAxis.auto) {
            if (this.xAxis.autorange > 0.0F) {
                var2 = this.getMaxX() - this.xAxis.autorange;
            } else {
                int var4 = 0;

                while(true) {
                    var2 = var1;
                    if (var4 >= this.plotCount) {
                        break;
                    }

                    float var3 = this.getMin(this.plots[var4].xvalues, this.vectorOffset);
                    boolean var6 = var5;
                    var2 = var1;
                    if (this.plots[var4].visible) {
                        label32: {
                            if (var5) {
                                var6 = var5;
                                var2 = var1;
                                if (var1 <= var3) {
                                    break label32;
                                }
                            }

                            var2 = var3;
                            var6 = true;
                        }
                    }

                    ++var4;
                    var5 = var6;
                    var1 = var2;
                }
            }
        }

        return var2;
    }

    private float getMinY() {
        boolean var5 = false;
        float var1 = this.yAxis.min;
        float var2 = var1;
        if (this.yAxis.auto) {
            if (this.yAxis.autorange > 0.0F) {
                var2 = this.getMaxY() - this.yAxis.autorange;
            } else {
                int var4 = 0;

                while(true) {
                    var2 = var1;
                    if (var4 >= this.plotCount) {
                        break;
                    }

                    float var3 = this.getMin(this.plots[var4].yvalues, this.vectorOffset);
                    boolean var6 = var5;
                    var2 = var1;
                    if (this.plots[var4].visible) {
                        label32: {
                            if (var5) {
                                var6 = var5;
                                var2 = var1;
                                if (var1 <= var3) {
                                    break label32;
                                }
                            }

                            var2 = var3;
                            var6 = true;
                        }
                    }

                    ++var4;
                    var5 = var6;
                    var1 = var2;
                }
            }
        }

        return var2;
    }

    private int[] toPixel(float var1, float var2, float var3, float[] var4, float var5) {
        double[] var7 = new double[var4.length];
        int[] var8 = new int[var4.length];

        for(int var6 = 0; var6 < var4.length; ++var6) {
            var7[var6] = (double)(var5 * var1 + (var4[var6] - var2) / (var3 - var2) * (1.0F - 2.0F * var5) * var1);
            var8[var6] = (int)var7[var6];
        }

        return var8;
    }

    private int toPixelInt(float var1, float var2, float var3, float var4, float var5) {
        return (int)((double)(var5 * var1 + (var4 - var2) / (var3 - var2) * (1.0F - 2.0F * var5) * var1));
    }

    public void Scroll(float var1, float var2) {
        float var3 = (float)this.getHeight();
        float var4 = (float)this.getWidth();
        var1 = this.fromPixel(var4, this.minx, this.maxx, (int)var1, this.margin) - this.fromPixel(var4, this.minx, this.maxx, 0, this.margin);
        var2 = -(this.fromPixel(var3, this.miny, this.maxy, (int)var2, this.margin) - this.fromPixel(var3, this.miny, this.maxy, 0, this.margin));
        this.setXAxis(this.minx + var1, this.maxx + var1, this.xAxis.zero, this.xAxis.gridstep, false);
        this.setYAxis(this.miny + var2, this.maxy + var2, this.yAxis.zero, this.yAxis.gridstep, false);
    }

    public void Zoom(float var1, float var2, float var3, float var4) {
        float var5;
        float var6;
        label32: {
            var6 = (float)this.getHeight();
            var5 = this.fromPixel((float)this.getWidth(), this.minx, this.maxx, (int)var3, this.margin);
            var4 = this.fromPixel(var6, this.miny, this.maxy, (int)var4, this.margin);
            if (!Double.isNaN((double)var1) && !Double.isInfinite((double)var1)) {
                var3 = var1;
                if (Math.abs(var1) <= 10.0F) {
                    break label32;
                }
            }

            var3 = 1.0F;
        }

        label26: {
            if (!Double.isNaN((double)var2) && !Double.isInfinite((double)var2)) {
                var1 = var2;
                if (Math.abs(var2) <= 10.0F) {
                    break label26;
                }
            }

            var1 = 1.0F;
        }

        var2 = this.minx;
        var6 = this.maxx;
        if (var3 <= 0.0F) {
            this.setXAxis(this.sminx, this.smaxx, this.xAxis.zero, this.xAxis.gridstep, true);
        } else {
            this.setXAxis(var5 + (var2 - var5) * var3, var5 + (var6 - var5) * var3, this.xAxis.zero, this.xAxis.gridstep, false);
        }

        var2 = this.miny;
        var3 = this.maxy;
        if (var1 <= 0.0F) {
            this.setYAxis(this.sminy, this.smaxy, this.yAxis.zero, this.yAxis.gridstep, true);
        } else {
            this.setYAxis(var4 + (var2 - var4) * var1, var4 + (var3 - var4) * var1, this.yAxis.zero, this.yAxis.gridstep, false);
        }
    }

    public float getStrokeWidth() {
        return this.strokeWidth;
    }

    public int getVectorLength() {
        return this.vectorLength;
    }

    public boolean getXAxisAuto() {
        return this.xAxis.auto;
    }

    public boolean getYAxisAuto() {
        return this.yAxis.auto;
    }

    protected void onDraw(Canvas var1) {
        float var3 = (float)this.getHeight();
        float var4 = (float)this.getWidth();
        int var6 = this.toPixelInt(var3, this.miny, this.maxy, this.locxAxis, this.margin);
        int var7 = this.toPixelInt(var4, this.minx, this.maxx, this.locyAxis, this.margin);
        int var8 = this.toPixelInt(var3, this.miny, this.maxy, this.maxy, this.margin);
        int var9 = this.toPixelInt(var3, this.miny, this.maxy, this.miny, this.margin);
        int var5 = this.toPixelInt(var4, this.minx, this.maxx, this.minx, this.margin);
        int var10 = this.toPixelInt(var4, this.minx, this.maxx, this.maxx, this.margin);
        this.paint.setStrokeWidth(0.5F * (1.0F + this.strokeWidth));
        var1.drawARGB(this.backColor >>> 24, this.backColor >>> 16 & 255, this.backColor >>> 8 & 255, this.backColor & 255);
        this.paint.setColor(this.axisColor);
        var1.drawLine((float)var5, var3 - (float)var6, (float)var10, var3 - (float)var6, this.paint);
        var1.drawLine((float)var7, var3 - (float)var8, (float)var7, var3 - (float)var9, this.paint);
        this.paint.setPathEffect(this.gridDPE);
        this.paint.setColor(-2147483648);
        float var2;
        int var11;
        if (this.yAxis.gridstep > 0.0F) {
            for(var2 = this.locxAxis + this.yAxis.gridstep; var2 < this.maxy; var2 += this.yAxis.gridstep) {
                var11 = this.toPixelInt(var3, this.miny, this.maxy, var2, this.margin);
                var1.drawLine((float)var5, var3 - (float)var11, (float)var10, var3 - (float)var11, this.paint);
            }

            for(var2 = this.locxAxis - this.yAxis.gridstep; var2 > this.miny; var2 -= this.yAxis.gridstep) {
                var11 = this.toPixelInt(var3, this.miny, this.maxy, var2, this.margin);
                var1.drawLine((float)var5, var3 - (float)var11, (float)var10, var3 - (float)var11, this.paint);
            }
        }

        if (this.xAxis.gridstep > 0.0F) {
            for(var2 = this.locyAxis + this.xAxis.gridstep; var2 < this.maxy; var2 += this.xAxis.gridstep) {
                var11 = this.toPixelInt(var4, this.minx, this.maxx, var2, this.margin);
                var1.drawLine((float)var11, var3 - (float)var8, (float)var11, var3 - (float)var9, this.paint);
            }

            for(var2 = this.locyAxis - this.xAxis.gridstep; var2 > this.miny; var2 -= this.xAxis.gridstep) {
                var11 = this.toPixelInt(var4, this.minx, this.maxx, var2, this.margin);
                var1.drawLine((float)var11, var3 - (float)var8, (float)var11, var3 - (float)var9, this.paint);
            }
        }

        if (this.frame) {
            this.paint.setPathEffect(this.markDPE);
            var1.drawLine((float)var5, var3 - (float)var8, (float)var10, var3 - (float)var8, this.paint);
            var1.drawLine((float)var5, var3 - (float)var9, (float)var10, var3 - (float)var9, this.paint);
            var1.drawLine((float)var5, var3 - (float)var8, (float)var5, var3 - (float)var9, this.paint);
            var1.drawLine((float)var10, var3 - (float)var8, (float)var10, var3 - (float)var9, this.paint);
        }

        this.paint.setPathEffect((PathEffect)null);
        this.paint.setColor(this.axisColor);
        if (this.axesLblVisible != 0) {
            this.paint.setTextAlign(Align.CENTER);
            this.paint.setTextSize(this.strokeWidth * 10.0F);

            for(var5 = 1; var5 <= 4; ++var5) {
                var2 = (float)(Math.round(10.0F * (this.minx + (float)(var5 - 1) * (this.maxx - this.minx) / (float)4)) / 10);
                var1.drawText("" + var2, (float)this.toPixelInt(var4, this.minx, this.maxx, var2, this.margin), var3 - (float)var6 + 20.0F, this.paint);
                var2 = (float)(Math.round(10.0F * (this.miny + (float)(var5 - 1) * (this.maxy - this.miny) / (float)4)) / 10);
                var1.drawText("" + var2, (float)(var7 + 20), var3 - (float)this.toPixelInt(var3, this.miny, this.maxy, var2, this.margin), this.paint);
            }

            var1.drawText("" + this.maxx, (float)this.toPixelInt(var4, this.minx, this.maxx, this.maxx, this.margin), var3 - (float)var6 + 20.0F, this.paint);
            var1.drawText("" + this.maxy, (float)(var7 + 20), var3 - (float)this.toPixelInt(var3, this.miny, this.maxy, this.maxy, this.margin), this.paint);
        }

        this.paint.setStrokeWidth(this.strokeWidth);
        var7 = this.vectorOffset;
        var10 = this.getVectorLength();

        for(var5 = 0; var5 < this.plotCount; ++var5) {
            if (this.plots[var5].visible) {
                int[] var12 = this.toPixel(var4, this.minx, this.maxx, this.plots[var5].xvalues, this.margin);
                int[] var13 = this.toPixel(var3, this.miny, this.maxy, this.plots[var5].yvalues, this.margin);

                for(var6 = var7; var6 < var10 - 1; ++var6) {
                    this.paint.setColor(this.plots[var5].color);
                    var1.drawLine((float)var12[var6], var3 - (float)var13[var6], (float)var12[var6 + 1], var3 - (float)var13[var6 + 1], this.paint);
                }
            }
        }

        for(var5 = 0; var5 < 10; ++var5) {
            if (this.xmarks[var5].visible) {
                this.paint.setPathEffect(this.markDPE);
                this.paint.setColor(this.xmarks[var5].color);
                var6 = this.toPixelInt(var4, this.minx, this.maxx, this.xmarks[var5].x, this.margin);
                var1.drawLine((float)var6, var3 - (float)var8, (float)var6, var3 - (float)var9, this.paint);
                if (!this.xmarks[var5].label.isEmpty()) {
                    this.paint.setPathEffect((PathEffect)null);
                    this.paint.setTextAlign(Align.CENTER);
                    this.paint.setTextSize(18.0F);
                    this.paint.getTextBounds(this.xmarks[var5].label, 0, this.xmarks[var5].label.length(), this.textBounds);
                    var7 = this.textBounds.width();
                    var10 = this.textBounds.height();
                    var11 = var10 / 3;
                    this.wallpath.reset();
                    this.wallpath.moveTo((float)(var6 - var11 - var7 / 2), var3 - (float)var8);
                    this.wallpath.lineTo((float)(var6 + var11 + var7 / 2), var3 - (float)var8);
                    this.wallpath.lineTo((float)(var6 + var11 + var7 / 2), var3 - (float)var8 + (float)var10 + (float)var11);
                    this.wallpath.lineTo((float)var6, var3 - (float)var8 + (float)(var10 * 2) + (float)var11);
                    this.wallpath.lineTo((float)(var6 - var11 - var7 / 2), var3 - (float)var8 + (float)var10 + (float)var11);
                    this.wallpath.lineTo((float)(var6 - var11 - var7 / 2), var3 - (float)var8);
                    var1.drawPath(this.wallpath, this.paint);
                    this.paint.setColor(-16777216 | this.backColor);
                    var1.drawText(this.xmarks[var5].label, (float)var6, var3 - (float)var8 + (float)var10 + (float)var11, this.paint);
                }
            }
        }

    }

    public boolean onTouchEvent(MotionEvent var1) {
        this.scaleGestureDetector.onTouchEvent(var1);
        if (this.gestureDetector.onTouchEvent(var1)) {
        }

        return true;
    }

    public void setData(int channel, float[] xValues, float[] yValues, int vectorOffset, int vectorLength) {
        if (channel >= 0 && channel < this.plotCount) {
            this.plots[channel].xvalues = xValues;
            this.plots[channel].yvalues = yValues;
            this.vectorOffset = vectorOffset;
            this.setVectorLength(vectorLength);
            this.getAxes();
            this.invalidate();
        }

    }

    public void setStrokeWidth(float var1) {
        this.strokeWidth = var1;
    }

    public void setVectorLength(int var1) {
        this.vectorLength = var1;
    }

    public void setXAutorange(float var1) {
        this.xAxis.autorange = var1;
        this.xAxis.autozero = false;
    }

    public void setXAutorange(float var1, boolean var2) {
        this.xAxis.autorange = var1;
        this.xAxis.autozero = var2;
    }

    public void setXAxis(float var1, float var2, float var3, float var4, boolean var5) {
        this.xAxis.min = var1;
        this.xAxis.max = var2;
        this.xAxis.zero = var3;
        this.xAxis.gridstep = var4;
        this.xAxis.auto = var5;
        this.getAxes();
    }

    public void setXOffset(int var1) {
        this.vectorOffset = var1;
    }

    public void setYAutorange(float autorange) {
        this.yAxis.autorange = autorange;
        this.yAxis.autozero = false;
    }

    public void setYAutorange(float autorange, boolean autozero) {
        this.yAxis.autorange = autorange;
        this.yAxis.autozero = autozero;
    }

    public void setYAxis(float var1, float var2, float var3, float var4, boolean var5) {
        this.yAxis.min = var1;
        this.yAxis.max = var2;
        this.yAxis.zero = var3;
        this.yAxis.gridstep = var4;
        this.yAxis.auto = var5;
        this.getAxes();
    }

    public class Axis {
        private boolean auto = true;
        private float autorange = 0.0F;
        private boolean autozero = false;
        private float gridstep = 20.0F;
        private float max = 100.0F;
        private float min = -100.0F;
        private float zero = 0.0F;

        public Axis() {
        }
    }

    public interface Iplot2dListener {
        void onPlotDoubleTap(Plot2d var1);
    }

    public class Plot {
        public int color = -65536;
        public boolean visible = false;
        public float[] xvalues;
        public float[] yvalues;

        public Plot(int _const101) {
            this.xvalues = new float[_const101];
            this.yvalues = new float[_const101];
        }
    }

    public class XMark {
        public int color = -16744448;
        public String label;
        public boolean visible = false;
        public float x = 0.0F;

        public XMark(int var2) {
            this.label = "" + var2;
        }
    }

    private class p2dDoubleTapListener implements OnDoubleTapListener {
        private p2dDoubleTapListener() {
        }

        public boolean onDoubleTap(MotionEvent var1) {
            Plot2d.this.scaleFactorX = 1.0F;
            Plot2d.this.scaleFactorY = 1.0F;
            Plot2d.this.Zoom(-1.0F, -1.0F, 0.0F, 0.0F);
            Plot2d.this.invalidate();
            if (Plot2d.this.pActivity != null) {
                ((Plot2d.Iplot2dListener) Plot2d.this.pActivity).onPlotDoubleTap(Plot2d.this);
            }

            return false;
        }

        public boolean onDoubleTapEvent(MotionEvent var1) {
            return false;
        }

        public boolean onSingleTapConfirmed(MotionEvent var1) {
            return false;
        }
    }

    private class p2dGestureListener extends SimpleOnGestureListener {
        private p2dGestureListener() {
        }

        public boolean onScroll(MotionEvent var1, MotionEvent var2, float var3, float var4) {
            Plot2d.this.Scroll(var3, var4);
            Plot2d.this.invalidate();
            return true;
        }
    }

    private class p2dScaleGestureListener implements OnScaleGestureListener {
        private p2dScaleGestureListener() {
        }

        public boolean onScale(ScaleGestureDetector var1) {
            Plot2d.this.scaleFactorX = (var1.getPreviousSpanX() + 10.0F) / (var1.getCurrentSpanX() + 10.0F);
            Plot2d.this.scaleFactorY = (var1.getPreviousSpanY() + 10.0F) / (var1.getCurrentSpanY() + 10.0F);
            Plot2d.this.Zoom(Plot2d.this.scaleFactorX, Plot2d.this.scaleFactorY, var1.getFocusX(), var1.getFocusY());
            Plot2d.this.invalidate();
            return true;
        }

        public boolean onScaleBegin(ScaleGestureDetector var1) {
            Plot2d.this.smaxx = Plot2d.this.maxx;
            Plot2d.this.smaxy = Plot2d.this.maxy;
            Plot2d.this.sminx = Plot2d.this.minx;
            Plot2d.this.sminy = Plot2d.this.miny;
            return true;
        }

        public void onScaleEnd(ScaleGestureDetector var1) {
            Plot2d.this.smaxx = Plot2d.this.maxx;
            Plot2d.this.smaxy = Plot2d.this.maxy;
            Plot2d.this.sminx = Plot2d.this.minx;
            Plot2d.this.sminy = Plot2d.this.miny;
        }
    }
}
