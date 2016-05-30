package com.kirshboim.polyjam;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class PolyPlayView extends View {

    final int MAX_NUMBER_OF_POINT = Configuration.MAX_POINTER_COUNT;
    float[] x = new float[MAX_NUMBER_OF_POINT];
    float[] y = new float[MAX_NUMBER_OF_POINT];
    int[] z = new int[MAX_NUMBER_OF_POINT];
    int[] touching = new int[MAX_NUMBER_OF_POINT];
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PolyPlayView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init();
    }

    public PolyPlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PolyPlayView(Context context) {
        super(context);
        init();
    }

    void init() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setTextSize(40);
        paint.setColor(Color.BLACK);
        setAmplitude(1f);
        setKeepScreenOn(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < MAX_NUMBER_OF_POINT; i++) {
            if (touching[i] != 0) {
                for (int j = 0; j < i + 1; j++) {
                    paint.setColor(z[i] | (0x00ffffff & Color.BLACK));
                    canvas.drawCircle(x[i], y[i], 75 - (j * 6), paint);
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = (event.getAction() & MotionEvent.ACTION_MASK);
        int pointCount = event.getPointerCount();

        for (int i = 0; i < pointCount; i++) {
            int id = event.getPointerId(i);

            // Ignore pointer higher than our max.
            if (id < MAX_NUMBER_OF_POINT) {
                x[id] = (int) event.getX(i);
                y[id] = (int) event.getY(i);

                if ((action == MotionEvent.ACTION_DOWN)
                        || (action == MotionEvent.ACTION_POINTER_DOWN)
                        || (action == MotionEvent.ACTION_MOVE)) {
                    touching[id] = 2;
                } else {
                    touching[id] = 0;
                }
            }
        }

        invalidate();
        return true;
    }

    public void setAmplitude(float amplitude) {
        for (int i = 0; i < MAX_NUMBER_OF_POINT; i++) {
            if (touching[i] != 1) {
                z[i] = ((int) ((amplitude * 255) + 0.5) << 24);
            }
        }
    }

}
