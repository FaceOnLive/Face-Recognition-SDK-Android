package com.ttv.facedemo.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ttv.facedemo.R;


public class RecognizeAreaView extends View implements View.OnTouchListener {

    private RectF limitArea;

    private int shadowColor;

    private double[] distanceSquares = new double[4];

    public interface OnRecognizeAreaChangedListener {

        void onRecognizeAreaChanged(Rect recognizeArea);
    }

    OnRecognizeAreaChangedListener onRecognizeAreaChangedListener;

    public void setOnRecognizeAreaChangedListener(OnRecognizeAreaChangedListener onRecognizeAreaChangedListener) {
        this.onRecognizeAreaChangedListener = onRecognizeAreaChangedListener;
    }

    public RecognizeAreaView(Context context) {
        this(context, null);
    }

    public RecognizeAreaView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        shadowColor = ContextCompat.getColor(context, R.color.color_black_shadow);
        setOnTouchListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        limitArea = new RectF(0, 0, width, height);
        if (onRecognizeAreaChangedListener != null) {
            onRecognizeAreaChangedListener.onRecognizeAreaChanged(
                    new Rect(((int) limitArea.left), ((int) limitArea.top),
                            ((int) limitArea.right), ((int) limitArea.bottom))
            );
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (limitArea == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.clipOutRect(limitArea);
        } else {
            canvas.clipRect(limitArea, Region.Op.DIFFERENCE);
        }
        canvas.drawColor(shadowColor);
    }

    private void updateRecognizeArea(float x, float y) {

        distanceSquares[0] = getDistanceSquare(x, y, limitArea.left, limitArea.top);
        distanceSquares[1] = getDistanceSquare(x, y, limitArea.right, limitArea.top);
        distanceSquares[2] = getDistanceSquare(x, y, limitArea.left, limitArea.bottom);
        distanceSquares[3] = getDistanceSquare(x, y, limitArea.right, limitArea.bottom);

        int closestIndex = 0;
        double closestDistance = distanceSquares[0];
        for (int i = 1; i < distanceSquares.length; i++) {
            double distance = distanceSquares[i];
            if (closestDistance > distance) {
                closestDistance = distance;
                closestIndex = i;
            }
        }
        switch (closestIndex) {
            case 0:
                limitArea.left = x;
                limitArea.top = y;
                break;
            case 1:
                limitArea.right = x;
                limitArea.top = y;
                break;
            case 2:
                limitArea.left = x;
                limitArea.bottom = y;
                break;
            case 3:
                limitArea.right = x;
                limitArea.bottom = y;
                break;
            default:
                break;
        }
    }

    private double getDistanceSquare(float x1, float y1, float x2, float y2) {
        float deltaHorizontal = x1 - x2;
        float deltaVertical = y1 - y2;
        return deltaHorizontal * deltaHorizontal + deltaVertical * deltaVertical;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            updateRecognizeArea(event.getX(i), event.getY(i));
        }
        if (onRecognizeAreaChangedListener != null) {
            onRecognizeAreaChangedListener.onRecognizeAreaChanged(
                    new Rect(((int) limitArea.left), ((int) limitArea.top),
                            ((int) limitArea.right), ((int) limitArea.bottom))
            );
        }
        invalidate();
        return true;
    }
}
