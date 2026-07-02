package com.bio.facerecognition;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bio.facesdk.FaceBox;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;

public class FaceView extends View {

    private Context context;
    private Paint realPaint;
    private Paint spoofPaint;
    private Float radius;
    private Float lineLength;
    private RectF leftTopAcrRectF = new RectF();
    private RectF rightTopAcrRectF = new RectF();
    private RectF leftBottomAcrRectF = new RectF();
    private RectF rightBottomAcrRectF = new RectF();
    DecimalFormat decimalFormat = new DecimalFormat("0.000");

    private Size frameSize;

    private List<FaceBox> faceBoxes;

    public FaceView(Context context) {
        this(context, null);

        this.context = context;
        init();
    }

    public FaceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        init();
    }

    public void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        realPaint = new Paint();
        realPaint.setStyle(Paint.Style.STROKE);
        realPaint.setStrokeWidth(3);
        realPaint.setColor(ContextCompat.getColor(context, R.color.lineColor));
        realPaint.setAntiAlias(true);
        realPaint.setTextSize(50);

        spoofPaint = new Paint();
        spoofPaint.setStyle(Paint.Style.STROKE);
        spoofPaint.setStrokeWidth(3);
        spoofPaint.setColor(Color.RED);
        spoofPaint.setAntiAlias(true);
        spoofPaint.setTextSize(50);
        radius = 30F;
        lineLength = 120F;
    }

    public void setFrameSize(Size frameSize)
    {
        this.frameSize = frameSize;
    }

    public void setFaceBoxes(List<FaceBox> faceBoxes)
    {
        this.faceBoxes = faceBoxes;
        invalidate();
    }

    private void drawBBox(Canvas canvas, Paint paint, Rect rect)
    {
        // Draw left top corner
        canvas.drawLine(rect.left, rect.top + lineLength, rect.left, rect.top + radius, paint);
        leftTopAcrRectF.left = rect.left;
        leftTopAcrRectF.top = rect.top;
        leftTopAcrRectF.right = rect.left + radius * 2;
        leftTopAcrRectF.bottom = rect.top + radius * 2;
        canvas.drawArc(leftTopAcrRectF, 180F, 90F, false, paint);
        canvas.drawLine(rect.left + radius, rect.top, rect.left + lineLength, rect.top, paint);

        // Draw right top corner
        canvas.drawLine(rect.right - lineLength, rect.top, rect.right - radius, rect.top, paint);
        rightTopAcrRectF.left = rect.right - radius * 2;
        rightTopAcrRectF.top = rect.top;
        rightTopAcrRectF.right = rect.right;
        rightTopAcrRectF.bottom = rect.top + radius * 2;
        canvas.drawArc(rightTopAcrRectF, 270F, 90F, false, paint);
        canvas.drawLine(rect.right, rect.top + radius, rect.right, rect.top + lineLength, paint);

        // Draw left bottom corner
        canvas.drawLine(rect.left, rect.bottom - lineLength, rect.left, rect.bottom - radius, paint);
        leftBottomAcrRectF.left = rect.left;
        leftBottomAcrRectF.top = rect.bottom - radius * 2;
        leftBottomAcrRectF.right = rect.left + radius * 2;
        leftBottomAcrRectF.bottom = rect.bottom;
        canvas.drawArc(leftBottomAcrRectF, 180F, -90F, false, paint);
        canvas.drawLine(rect.left + radius, rect.bottom, rect.left + lineLength, rect.bottom, paint);

        // Draw right bottom corner
        canvas.drawLine(rect.right - lineLength, rect.bottom, rect.right - radius, rect.bottom, paint);
        rightBottomAcrRectF.left = rect.right - radius * 2;
        rightBottomAcrRectF.top = rect.bottom - radius * 2;
        rightBottomAcrRectF.right = rect.right;
        rightBottomAcrRectF.bottom = rect.bottom;
        canvas.drawArc(rightBottomAcrRectF, 90F, -90F, false, paint);
        canvas.drawLine(rect.right, rect.bottom - radius, rect.right, rect.bottom - lineLength, paint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (frameSize != null &&  faceBoxes != null) {
            Log.d("", "  Face Box   " + faceBoxes.size());
            Log.d("", " frame size " + this.frameSize.getWidth() + " " + this.frameSize.getHeight());
            Log.d("", " canvas size " + canvas.getWidth() + " " + canvas.getHeight());
            float x_scale = this.frameSize.getWidth() / (float)canvas.getWidth();
            float y_scale = this.frameSize.getHeight() / (float)canvas.getHeight();

            for (int i = 0; i < faceBoxes.size(); i++) {
                FaceBox faceBox = faceBoxes.get(i);

                if (faceBox.liveness < SettingsActivity.getLivenessThreshold(context))
                {
                    spoofPaint.setStrokeWidth(3);
                    spoofPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                    canvas.drawText("SPOOF " + decimalFormat.format(faceBox.liveness), (faceBox.x1 / x_scale) + 10, (faceBox.y1 / y_scale) - 30, spoofPaint);

                    spoofPaint.setStrokeWidth(5);
                    spoofPaint.setStyle(Paint.Style.STROKE);
                    drawBBox(canvas, spoofPaint, new Rect((int)(faceBox.x1 / x_scale), (int)(faceBox.y1 / y_scale),
                            (int)(faceBox.x2 / x_scale), (int)(faceBox.y2 / y_scale)));
                }
                else
                {
                    realPaint.setStrokeWidth(3);
                    realPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                    canvas.drawText("REAL " + decimalFormat.format(faceBox.liveness), (faceBox.x1 / x_scale) + 10, (faceBox.y1 / y_scale) - 30, realPaint);

                    realPaint.setStyle(Paint.Style.STROKE);
                    realPaint.setStrokeWidth(5);
                    drawBBox(canvas, realPaint, new Rect((int)(faceBox.x1 / x_scale), (int)(faceBox.y1 / y_scale),
                            (int)(faceBox.x2 / x_scale), (int)(faceBox.y2 / y_scale)));
                }
            }
        }
    }
}
