package com.emarolab.carfi.imustream;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Timer;
import java.util.TimerTask;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SendingActivity extends WearableActivity implements SensorEventListener {

    private vectorMessage stepMsg = new vectorMessage(), hrmMsg = new vectorMessage(), accMsg = new vectorMessage(), gyroMsg = new vectorMessage(), rssiMsg = new vectorMessage();
    private float[] last_hrm = new float[1];
    private float[] last_step = new float[1];
    private float[] last_acc = new float[3];
    private float[] last_gyro = new float[3];
    private float[] last_rssi = new float[1];
    private long last_ts = System.currentTimeMillis();

    private TextView dataStep, dataHRM, dataTS;
    private SensorManager senSensorManager;
    private Sensor senStepCounter, senHRM, senAccelerometer, senGyroscope;
    private Timer myTimer;

    private long lastUpdate = 0;

    private long driftStart = 0;
    private float precision = 1000;
    private int period = 20;

    private DateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss:SSS");

    private boolean sensorUpdate = true;

    private String msg_intertial;

    private GoogleApiClient mGoogleApiClient;

    private boolean drift_flag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sending);

        // Enables Always-on
        setAmbientEnabled();

        stepMsg.setTopic("sensors/stepcounter");
        hrmMsg.setTopic("sensors/heartrate");
        accMsg.setTopic("sensors/accelerometer");
        gyroMsg.setTopic("sensors/gyroscope");
        rssiMsg.setTopic("sensors/rssi");

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        senStepCounter = senSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        senSensorManager.registerListener(this, senStepCounter , SensorManager.SENSOR_DELAY_GAME);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this,senAccelerometer,SensorManager.SENSOR_DELAY_GAME);
        senGyroscope = senSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        senSensorManager.registerListener(this,senGyroscope,SensorManager.SENSOR_DELAY_GAME);
        senHRM = senSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        senSensorManager.registerListener(this, senHRM , SensorManager.SENSOR_DELAY_GAME);


        dataStep = (TextView) findViewById(R.id.step);
        dataHRM = (TextView) findViewById(R.id.hrm);
        dataTS = (TextView) findViewById(R.id.timestamp);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                    }
                })
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                sender();
            }

        }, 0, period);

    }


    private void sender()
    {
        if (sensorUpdate) {
            sensorUpdate = false;
            syncSampleDataItem(stepMsg,hrmMsg,accMsg,gyroMsg,rssiMsg);
        }
    }

    private void syncSampleDataItem(final vectorMessage msg_step, final vectorMessage msg_hrm, final vectorMessage msg_acc, final vectorMessage msg_gyro) {
        if (mGoogleApiClient == null)
            return;

        final PutDataMapRequest putRequest = PutDataMapRequest.create("/IMU");
        final DataMap map = putRequest.getDataMap();

        map.putFloatArray(msg_step.getTopic(), msg_step.getData());
        map.putFloatArray(msg_hrm.getTopic(), msg_hrm.getData());
        map.putFloatArray(msg_acc.getTopic(),msg_acc.getData());
        map.putFloatArray(msg_gyro.getTopic(),msg_gyro.getData());
        last_ts = System.currentTimeMillis();
        map.putLong("sensors/timestamp", last_ts);
        if (drift_flag){
            driftStart = last_ts;
            drift_flag = false;
            map.putLong("sensors/driftStart",driftStart);
        }else{
            map.putLong("sensors/driftStart",0);
        }
        PutDataRequest request = putRequest.asPutDataRequest();
        request.setUrgent();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                //Log.d("sending", " Sending was successful: " + dataItemResult.getStatus()
                //        .isSuccess());
                sensorUpdate = true;
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_HEART_RATE) {
            last_hrm[0] = sensorEvent.values[0];

            dataHRM.setText("hrm: " + last_hrm[0]);
            hrmMsg.setData(last_hrm);
        }

        if (mySensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            last_step[0] = sensorEvent.values[0];

            dataStep.setText("step: " + last_step[0]);
            stepMsg.setData(last_step);
        }

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            last_acc[0] = ((int)(sensorEvent.values[0]*precision))/precision;
            last_acc[1] = ((int)(sensorEvent.values[1]*precision))/precision;
            last_acc[2] = ((int)(sensorEvent.values[2]*precision))/precision;

            accMsg.setData(last_acc);
        }

        if (mySensor.getType() == Sensor.TYPE_GYROSCOPE) {
            last_gyro[0] = ((int)(sensorEvent.values[0]*precision))/precision;
            last_gyro[1] = ((int)(sensorEvent.values[1]*precision))/precision;
            last_gyro[2] = ((int)(sensorEvent.values[2]*precision))/precision;

            gyroMsg.setData(last_gyro);
        }

        dataTS.setText("ts: " + dateFormat.format(new Date(last_ts)));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public void stopStreaming(View view) {
        senSensorManager.unregisterListener(this);
        myTimer.cancel();
        Intent intent = new Intent(this, MainActivity.class);
        finish();
        startActivity(intent);

    }


    public void driftButton(View view) {
        drift_flag = true;
    }
}
