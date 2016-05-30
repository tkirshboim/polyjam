package com.kirshboim.polyjam;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;

public class PlayActivity extends Activity implements SensorEventListener {

    private static final float Pi = (float) Math.PI;
    private static final float halfPi = (float) Math.PI / 2;

    private OSCInputHandler inputHandler;
    private String ip = null;
    private int port = -1;

    private BroadcastReceiver receiver;
    private Handler handler;
    private Runnable serverDataRunnable;

    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float orientation[] = {0, 0, 0};
    private float[] mGravity;
    private float[] mGeomagnetic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.multitouch);

        handler = new Handler();
        serverDataRunnable = new Runnable() {
            @Override
            public void run() {
                sendBroadcast(new Intent(PolyjamService.Events.GetPlayData));
                handler.postDelayed(this, 1000);
            }
        };

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        inputHandler = new OSCInputHandler();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (PolyjamService.Events.PlayData.equals(intent.getAction())) {

                    String serverIpNew = intent.getStringExtra("ip");
                    int serverPortNew = intent.getIntExtra("port", -1);

                    if (serverPortNew == -1) {
                        if (port == -1) {
                            finish();
                        } else {
                            startActivity(new Intent(PlayActivity.this, PostPlayActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                        }
                        return;
                    }

                    if (port != -1 && (port != serverPortNew || !serverIpNew.equals(ip))) {
                        inputHandler.stop();

                        startInput(serverIpNew, serverPortNew);
                    } else if (port == -1) {
                        startInput(serverIpNew, serverPortNew);
                    }
                }
            }
        };
    }

    private void startInput(String serverIpNew, int serverPortNew) {
        port = serverPortNew;
        ip = serverIpNew;

        PolyPlayView multiTouchView = (PolyPlayView) findViewById(R.id.multi_touch);
        multiTouchView.setOnTouchListener(createOnMultiTouchListener());

        inputHandler.start(serverIpNew, serverPortNew);
    }

    private OnTouchListener createOnMultiTouchListener() {
        return new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int pointerIndex = event.getActionIndex();
                int pointerId = event.getPointerId(pointerIndex);
                int maskedAction = event.getActionMasked();
                float width = v.getWidth();
                float height = v.getHeight();

                switch (maskedAction) {

                    // TODO do the orientation math somewhere else
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                    case MotionEvent.ACTION_MOVE: {
                        for (int size = Math.min(event.getPointerCount(),
                                Configuration.MAX_POINTER_COUNT), i = 0; i < size; i++) {
                            inputHandler.pointer(
                                    event.getPointerId(i),
                                    event.getX(i) / width,
                                    (height - event.getY(i)) / height,
                                    event.getPressure(i),

                                    orientation[0],
                                    orientation[1],
                                    orientation[2]);
                        }
                        break;
                    }

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        if (pointerId < Configuration.MAX_POINTER_COUNT) {
                            inputHandler.pointer(pointerId, -1f, -1f, -1f, -1f, -1f, -1f);
                        }
                        break;
                    }
                }

                return false;
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(PolyjamService.Events.PlayData));

        handler.post(serverDataRunnable);

        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(receiver);
        handler.removeCallbacks(serverDataRunnable);

        inputHandler.stop();
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values;
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                SensorManager.getOrientation(R, orientation);
                orientation[0] = orientation[0] / Pi;
                orientation[1] = orientation[1] / halfPi;
                orientation[2] = 1 - Math.min(2 * orientation[2] / Pi, 1f);
                return;
            }
        }

        if (mGravity != null && mGravity.length >= 2) {
            orientation[1] = Math.min(Math.max(mGravity[1] / -10f, -1f), 1f);

            orientation[2] = Math.min(Math.max(mGravity[0] * 0.2f, -2f), 2f);
            orientation[2] = orientation[2] < 0 ? orientation[2] * 0.5f : orientation[2];
            orientation[2] = orientation[2] + 1f;
        }
    }

}
