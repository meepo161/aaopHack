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

    public Plot2d(Context context, int numOfChannel, int _const101, int axesLblVisible) {
        super(context);
        pActivity = (Activity) context;
        gestureDetector = new GestureDetector(context, new Plot2d.p2dGestureListener());
        gestureDetector.setOnDoubleTapListener(new Plot2d.p2dDoubleTapListener());
        scaleGestureDetector = new ScaleGestureDetector(context, new Plot2d.p2dScaleGestureListener());
        plots = new Plot2d.Plot[numOfChannel];
        this.axesLblVisible = axesLblVisible;
        plotCount = plots.length;
        if (plotCount > 0) {
            for (numOfChannel = 0; numOfChannel < plotCount; ++numOfChannel) {
                plots[numOfChannel] = new Plot2d.Plot(_const101);
            }

            setVectorLength(plots[0].xvalues.length);
        } else {
            setVectorLength(0);
        }

        vectorOffset = 0;
        margin = 0.05F;
        frame = true;
        paint = new Paint();
        xAxis = new Plot2d.Axis();
        yAxis = new Plot2d.Axis();
        xmarks = new Plot2d.XMark[10];

        for (numOfChannel = 0; numOfChannel < 10; ++numOfChannel) {
            xmarks[numOfChannel] = new Plot2d.XMark(numOfChannel);
        }

        gridDPE = new DashPathEffect(new float[]{2.0F, 5.0F}, 0.0F);
        markDPE = new DashPathEffect(new float[]{5.0F, 5.0F}, 0.0F);
        wallpath = new Path();
        textBounds = new Rect();
        getAxes();
    }

    private float fromPixel(float var1, float var2, float var3, int var4, float var5) {
        return (float) ((double) ((var3 - var2) * ((float) var4 - var5 * var1) / ((1.0F - 2.0F * var5) * var1) + var2));
    }

    private void getAxes() {
        maxx = getMaxX();
        maxy = getMaxY();
        minx = getMinX();
        miny = getMinY();
        if (maxx == minx) {
            maxx = (float) ((double) maxx + 0.5D);
            minx = (float) ((double) minx - 0.5D);
        }

        if (maxy == miny) {
            maxy = (float) ((double) maxy + 0.5D);
            miny = (float) ((double) miny - 0.5D);
        }

        if (yAxis.auto) {
            if (minx >= 0.0F) {
                locyAxis = minx;
            } else if (minx < 0.0F && maxx >= 0.0F) {
                locyAxis = 0.0F;
            } else {
                locyAxis = maxx;
            }
        } else {
            locyAxis = yAxis.zero;
        }

        if (xAxis.auto) {
            if (miny >= 0.0F) {
                locxAxis = miny;
            } else if (miny < 0.0F && maxy >= 0.0F) {
                locxAxis = 0.0F;
            } else {
                locxAxis = maxy;
            }
        } else {
            locxAxis = xAxis.zero;
        }
    }

    private float getMax(float[] var1, int var2) {
        int var5 = var2;
        if (var2 >= var1.length) {
            var5 = 0;
        }

        float var3;
        float var4;
        for (var3 = var1[var5]; var5 < var1.length; var3 = var4) {
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
        float var1 = xAxis.max;
        float var2 = var1;
        if (xAxis.auto) {
            if (xAxis.autorange > 0.0F && xAxis.autozero) {
                var2 = (float) ((double) xAxis.zero + 0.5D * (double) xAxis.autorange);
            } else {
                for (int var4 = 0; var4 < plotCount; var1 = var2) {
                    float var3 = getMax(plots[var4].xvalues, vectorOffset);
                    boolean var6 = var5;
                    var2 = var1;
                    if (plots[var4].visible && (!var5 || var1 < var3)) {
                        var2 = var3;
                        var6 = true;
                    }

                    ++var4;
                    var5 = var6;
                }
            }
        }

        return var2;
    }

    private float getMaxY() {
        boolean var5 = false;
        float var1 = yAxis.max;
        float var2 = var1;
        if (yAxis.auto) {
            if (yAxis.autorange > 0.0F && yAxis.autozero) {
                var2 = (float) ((double) yAxis.zero + 0.5D * (double) yAxis.autorange);
            } else {
                for (int var4 = 0; var4 < plotCount; var1 = var2) {
                    float var3 = getMax(plots[var4].yvalues, vectorOffset);
                    boolean var6 = var5;
                    var2 = var1;
                    if (plots[var4].visible && (!var5 || var1 < var3)) {
                        var2 = var3;
                        var6 = true;
                    }

                    ++var4;
                    var5 = var6;
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
        for (var3 = var1[var5]; var5 < var1.length; var3 = var4) {
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
        float var1 = xAxis.min;
        float var2 = var1;
        if (xAxis.auto) {
            if (xAxis.autorange > 0.0F) {
                var2 = getMaxX() - xAxis.autorange;
            } else {
                for (int var4 = 0; var4 < plotCount; var1 = var2) {
                    float var3 = getMin(plots[var4].xvalues, vectorOffset);
                    boolean var6 = var5;
                    var2 = var1;
                    if (plots[var4].visible && (!var5 || var1 > var3)) {
                        var2 = var3;
                        var6 = true;
                    }

                    ++var4;
                    var5 = var6;
                }
            }
        }

        return var2;
    }

    private float getMinY() {
        boolean var5 = false;
        float var1 = yAxis.min;
        float var2 = var1;
        if (yAxis.auto) {
            if (yAxis.autorange > 0.0F) {
                var2 = getMaxY() - yAxis.autorange;
            } else {
                for (int var4 = 0; var4 < plotCount; var1 = var2) {
                    float var3 = getMin(plots[var4].yvalues, vectorOffset);
                    boolean var6 = var5;
                    var2 = var1;
                    if (plots[var4].visible && (!var5 || var1 > var3)) {
                        var2 = var3;
                        var6 = true;
                    }

                    ++var4;
                    var5 = var6;
                }
            }
        }

        return var2;
    }

    private int[] toPixel(float var1, float var2, float var3, float[] plotsValues, float var5) {
        double[] var7 = new double[plotsValues.length];
        int[] var8 = new int[plotsValues.length];

        for (int i = 0; i < plotsValues.length; ++i) {
            var7[i] = (double) (var5 * var1 + (plotsValues[i] - var2) / (var3 - var2) * (1.0F - 2.0F * var5) * var1);
            var8[i] = (int) var7[i];
        }

        return var8;
    }

    private int toPixelInt(float var1, float var2, float var3, float var4, float var5) {
        return (int) ((double) (var5 * var1 + (var4 - var2) / (var3 - var2) * (1.0F - 2.0F * var5) * var1));
    }

    public void scroll(float var1, float var2) {
        float height = (float) getHeight();
        float width = (float) getWidth();
        var1 = fromPixel(width, minx, maxx, (int) var1, margin) - fromPixel(width, minx, maxx, 0, margin);
        var2 = -(fromPixel(height, miny, maxy, (int) var2, margin) - fromPixel(height, miny, maxy, 0, margin));
        setXAxis(minx + var1, maxx + var1, xAxis.zero, xAxis.gridstep, false);
        setYAxis(miny + var2, maxy + var2, yAxis.zero, yAxis.gridstep, false);
    }

    public void zoom(float var1, float var2, float var3, float var4) {
        float var6 = (float) getHeight();
        float var5 = fromPixel((float) getWidth(), minx, maxx, (int) var3, margin);
        var4 = fromPixel(var6, miny, maxy, (int) var4, margin);
        if (!Double.isNaN((double) var1) && !Double.isInfinite((double) var1)) {
            var3 = var1;
            if (Math.abs(var1) > 10.0F) {
                var3 = 1.0F;
            }
        }

        if (!Double.isNaN((double) var2) && !Double.isInfinite((double) var2)) {
            var1 = var2;
            if (Math.abs(var2) > 10.0F) {
                var1 = 1.0F;
            }
        }

        var2 = minx;
        var6 = maxx;
        if (var3 <= 0.0F) {
            setXAxis(sminx, smaxx, xAxis.zero, xAxis.gridstep, true);
        } else {
            setXAxis(var5 + (var2 - var5) * var3, var5 + (var6 - var5) * var3, xAxis.zero, xAxis.gridstep, false);
        }

        var2 = miny;
        var3 = maxy;
        if (var1 <= 0.0F) {
            setYAxis(sminy, smaxy, yAxis.zero, yAxis.gridstep, true);
        } else {
            setYAxis(var4 + (var2 - var4) * var1, var4 + (var3 - var4) * var1, yAxis.zero, yAxis.gridstep, false);
        }
    }

    public int getVectorLength() {
        return vectorLength;
    }

    protected void onDraw(Canvas canvas) {
        float var3 = (float) getHeight();
        float var4 = (float) getWidth();
        int var6 = toPixelInt(var3, miny, maxy, locxAxis, margin);
        int var7 = toPixelInt(var4, minx, maxx, locyAxis, margin);
        int var8 = toPixelInt(var3, miny, maxy, maxy, margin);
        int var9 = toPixelInt(var3, miny, maxy, miny, margin);
        int i = toPixelInt(var4, minx, maxx, minx, margin);
        int var10 = toPixelInt(var4, minx, maxx, maxx, margin);
        paint.setStrokeWidth(0.5F * (1.0F + strokeWidth));
        canvas.drawARGB(backColor >>> 24, backColor >>> 16 & 255, backColor >>> 8 & 255, backColor & 255);
        paint.setColor(axisColor);
        canvas.drawLine((float) i, var3 - (float) var6, (float) var10, var3 - (float) var6, paint);
        canvas.drawLine((float) var7, var3 - (float) var8, (float) var7, var3 - (float) var9, paint);
        paint.setPathEffect(gridDPE);
        paint.setColor(-2147483648);
        float var2;
        int var11;
        if (yAxis.gridstep > 0.0F) {
            for (var2 = locxAxis + yAxis.gridstep; var2 < maxy; var2 += yAxis.gridstep) {
                var11 = toPixelInt(var3, miny, maxy, var2, margin);
                canvas.drawLine((float) i, var3 - (float) var11, (float) var10, var3 - (float) var11, paint);
            }

            for (var2 = locxAxis - yAxis.gridstep; var2 > miny; var2 -= yAxis.gridstep) {
                var11 = toPixelInt(var3, miny, maxy, var2, margin);
                canvas.drawLine((float) i, var3 - (float) var11, (float) var10, var3 - (float) var11, paint);
            }
        }

        if (xAxis.gridstep > 0.0F) {
            for (var2 = locyAxis + xAxis.gridstep; var2 < maxy; var2 += xAxis.gridstep) {
                var11 = toPixelInt(var4, minx, maxx, var2, margin);
                canvas.drawLine((float) var11, var3 - (float) var8, (float) var11, var3 - (float) var9, paint);
            }

            for (var2 = locyAxis - xAxis.gridstep; var2 > miny; var2 -= xAxis.gridstep) {
                var11 = toPixelInt(var4, minx, maxx, var2, margin);
                canvas.drawLine((float) var11, var3 - (float) var8, (float) var11, var3 - (float) var9, paint);
            }
        }

        if (frame) {
            paint.setPathEffect(markDPE);
            canvas.drawLine((float) i, var3 - (float) var8, (float) var10, var3 - (float) var8, paint);
            canvas.drawLine((float) i, var3 - (float) var9, (float) var10, var3 - (float) var9, paint);
            canvas.drawLine((float) i, var3 - (float) var8, (float) i, var3 - (float) var9, paint);
            canvas.drawLine((float) var10, var3 - (float) var8, (float) var10, var3 - (float) var9, paint);
        }

        paint.setPathEffect((PathEffect) null);
        paint.setColor(axisColor);
        if (axesLblVisible != 0) {
            paint.setTextAlign(Align.CENTER);
            paint.setTextSize(strokeWidth * 10.0F);

            for (i = 1; i <= 4; ++i) {
                var2 = (float) (Math.round(10.0F * (minx + (float) (i - 1) * (maxx - minx) / (float) 4)) / 10);
                canvas.drawText("" + var2, (float) toPixelInt(var4, minx, maxx, var2, margin), var3 - (float) var6 + 20.0F, paint);
                var2 = (float) (Math.round(10.0F * (miny + (float) (i - 1) * (maxy - miny) / (float) 4)) / 10);
                canvas.drawText("" + var2, (float) (var7 + 20), var3 - (float) toPixelInt(var3, miny, maxy, var2, margin), paint);
            }

            canvas.drawText("" + maxx, (float) toPixelInt(var4, minx, maxx, maxx, margin), var3 - (float) var6 + 20.0F, paint);
            canvas.drawText("" + maxy, (float) (var7 + 20), var3 - (float) toPixelInt(var3, miny, maxy, maxy, margin), paint);
        }

        paint.setStrokeWidth(strokeWidth);
        var7 = vectorOffset;
        var10 = getVectorLength();

        for (i = 0; i < plotCount; ++i) {
            if (plots[i].visible) {
                int[] pixelsX = toPixel(var4, minx, maxx, plots[i].xvalues, margin);
                int[] pixelsY = toPixel(var3, miny, maxy, plots[i].yvalues, margin);

                for (var6 = var7; var6 < var10 - 1; ++var6) {
                    paint.setColor(plots[i].color);
                    canvas.drawLine((float) pixelsX[var6], var3 - (float) pixelsY[var6], (float) pixelsX[var6 + 1], var3 - (float) pixelsY[var6 + 1], paint);
                }
            }
        }

        for (i = 0; i < 10; ++i) {
            if (xmarks[i].visible) {
                paint.setPathEffect(markDPE);
                paint.setColor(xmarks[i].color);
                var6 = toPixelInt(var4, minx, maxx, xmarks[i].x, margin);
                canvas.drawLine((float) var6, var3 - (float) var8, (float) var6, var3 - (float) var9, paint);
                if (!xmarks[i].label.isEmpty()) {
                    paint.setPathEffect((PathEffect) null);
                    paint.setTextAlign(Align.CENTER);
                    paint.setTextSize(18.0F);
                    paint.getTextBounds(xmarks[i].label, 0, xmarks[i].label.length(), textBounds);
                    var7 = textBounds.width();
                    var10 = textBounds.height();
                    var11 = var10 / 3;
                    wallpath.reset();
                    wallpath.moveTo((float) (var6 - var11 - var7 / 2), var3 - (float) var8);
                    wallpath.lineTo((float) (var6 + var11 + var7 / 2), var3 - (float) var8);
                    wallpath.lineTo((float) (var6 + var11 + var7 / 2), var3 - (float) var8 + (float) var10 + (float) var11);
                    wallpath.lineTo((float) var6, var3 - (float) var8 + (float) (var10 * 2) + (float) var11);
                    wallpath.lineTo((float) (var6 - var11 - var7 / 2), var3 - (float) var8 + (float) var10 + (float) var11);
                    wallpath.lineTo((float) (var6 - var11 - var7 / 2), var3 - (float) var8);
                    canvas.drawPath(wallpath, paint);
                    paint.setColor(-16777216 | backColor);
                    canvas.drawText(xmarks[i].label, (float) var6, var3 - (float) var8 + (float) var10 + (float) var11, paint);
                }
            }
        }

    }

    public boolean onTouchEvent(MotionEvent var1) {
        scaleGestureDetector.onTouchEvent(var1);
        gestureDetector.onTouchEvent(var1);

        return true;
    }

    public void setData(int channel, float[] xValues, float[] yValues, int vectorOffset, int vectorLength) {
        if (channel >= 0 && channel < plotCount) {
            plots[channel].xvalues = xValues;
            plots[channel].yvalues = yValues;
            this.vectorOffset = vectorOffset;
            setVectorLength(vectorLength);
            getAxes();
            invalidate();
        }

    }

    public void setStrokeWidth(float var1) {
        strokeWidth = var1;
    }

    public void setVectorLength(int var1) {
        vectorLength = var1;
    }

    public void setXAutorange(float var1) {
        xAxis.autorange = var1;
        xAxis.autozero = false;
    }

    public void setXAutorange(float autoRange, boolean autoZero) {
        xAxis.autorange = autoRange;
        xAxis.autozero = autoZero;
    }

    public void setXAxis(float min, float max, float zero, float gridstep, boolean auto) {
        xAxis.min = min;
        xAxis.max = max;
        xAxis.zero = zero;
        xAxis.gridstep = gridstep;
        xAxis.auto = auto;
        getAxes();
    }

    public void setXOffset(int var1) {
        vectorOffset = var1;
    }

    public void setYAutorange(float autorange) {
        yAxis.autorange = autorange;
        yAxis.autozero = false;
    }

    public void setYAutorange(float autorange, boolean autozero) {
        yAxis.autorange = autorange;
        yAxis.autozero = autozero;
    }

    public void setYAxis(float min, float max, float zero, float gridzero, boolean auto) {
        yAxis.min = min;
        yAxis.max = max;
        yAxis.zero = zero;
        yAxis.gridstep = gridzero;
        yAxis.auto = auto;
        getAxes();
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
            xvalues = new float[_const101];
            yvalues = new float[_const101];
        }
    }

    public class XMark {
        public int color = -16744448;
        public String label;
        public boolean visible = false;
        public float x = 0.0F;

        public XMark(int var2) {
            label = "" + var2;
        }
    }

    private class p2dDoubleTapListener implements OnDoubleTapListener {
        private p2dDoubleTapListener() {
        }

        public boolean onDoubleTap(MotionEvent var1) {
            scaleFactorX = 1.0F;
            scaleFactorY = 1.0F;
            zoom(-1.0F, -1.0F, 0.0F, 0.0F);
            invalidate();
            if (pActivity != null) {
                ((Plot2d.Iplot2dListener) pActivity).onPlotDoubleTap(Plot2d.this);
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
            scroll(var3, var4);
            invalidate();
            return true;
        }
    }

    private class p2dScaleGestureListener implements OnScaleGestureListener {
        private p2dScaleGestureListener() {
        }

        public boolean onScale(ScaleGestureDetector var1) {
            scaleFactorX = (var1.getPreviousSpanX() + 10.0F) / (var1.getCurrentSpanX() + 10.0F);
            scaleFactorY = (var1.getPreviousSpanY() + 10.0F) / (var1.getCurrentSpanY() + 10.0F);
            zoom(scaleFactorX, scaleFactorY, var1.getFocusX(), var1.getFocusY());
            invalidate();
            return true;
        }

        public boolean onScaleBegin(ScaleGestureDetector var1) {
            smaxx = maxx;
            smaxy = maxy;
            sminx = minx;
            sminy = miny;
            return true;
        }

        public void onScaleEnd(ScaleGestureDetector var1) {
            smaxx = maxx;
            smaxy = maxy;
            sminx = minx;
            sminy = miny;
        }
    }
}
