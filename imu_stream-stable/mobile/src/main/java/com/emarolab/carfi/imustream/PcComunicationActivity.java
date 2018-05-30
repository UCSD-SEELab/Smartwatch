package com.emarolab.carfi.imustream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.emarolab.carfi.helpers.MqttHelper;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.DateFormat;
import java.util.Timer;

public class PcComunicationActivity extends AppCompatActivity {
    public TextView Step, HRM, TS, TS2,TS3, ipOut, portOut, textConnection;
    private Button p1_button;
    private boolean pause_flag = false;
    private BroadcastReceiver receiver;
    private Timer myTimer;
    private MqttHelper mqttHelper;
    private DateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss:SSS");
    private long driftStart_phone = 0;
    private long driftStart_watch = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pc_comunication);

        p1_button = (Button)findViewById(R.id.buttonPause);

        Intent intent = getIntent();
        String mqtt_ip = intent.getStringExtra(MqttSettingActivity.ip_message);
        String mqtt_port = intent.getStringExtra(MqttSettingActivity.port_message);
        String mqtt_user = intent.getStringExtra(MqttSettingActivity.user_message);
        String mqtt_password = intent.getStringExtra(MqttSettingActivity.password_message);

        ipOut = (TextView) findViewById(R.id.ipSending);
        portOut = (TextView) findViewById(R.id.portSending);
        ipOut.setText(mqtt_ip);
        portOut.setText(mqtt_port);
        textConnection = (TextView) findViewById(R.id.connectionS);

        Step = (TextView) findViewById(R.id.step);
        HRM = (TextView) findViewById(R.id.hrm);
        TS = (TextView) findViewById(R.id.ts);
        TS2 = (TextView) findViewById(R.id.ts2);
        TS3 = (TextView) findViewById(R.id.ts3);

        startMqtt(mqtt_ip,mqtt_port,mqtt_user,mqtt_password);

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.Broadcast");

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    float[] step = bundle.getFloatArray("stepcount");
                    float[] hrm = bundle.getFloatArray("heartrate");
                    float[] acc = bundle.getFloatArray("accelerometer");
                    float[] gyro = bundle.getFloatArray("gyroscope");
                    long time = bundle.getLong("timestamp");
                    long tmp = bundle.getLong("driftStart");
                    if (tmp!=0){
                        driftStart_phone = System.currentTimeMillis();
                        driftStart_watch = tmp;
                    }
                    String string = "step;" + step[0] + ";hrm;" + hrm[0] +";acc;"+acc[0]+";"+acc[1]+";"+acc[2]+";gyro;"+gyro[0]+";"+gyro[1]+";"+gyro[2]+";ts;"+dateFormat.format(new Date(time));
                    imuVisualization(step,hrm,time,driftStart_watch,driftStart_phone);
                    mqttHelper.onDataReceived(string);
                    connectionCheck();
                }
            }
        };

        registerReceiver(receiver, filter);
    }

    private void connectionCheck(){
        boolean connection = mqttHelper.checkConnection();
        if(connection){
            textConnection.setText("connected");
            textConnection.setTextColor(Color.GREEN);
        }else{
            textConnection.setText("disconnected");
            textConnection.setTextColor(Color.RED);
        }
    }

    private void imuVisualization(float[] step, float[] hrm, long ts,long driftStart_watch, long driftStart_phone){
        Step.setText(String.valueOf(step[0]));
        HRM.setText(String.valueOf(hrm[0]));
        TS.setText(dateFormat.format(new Date(ts)));
        TS2.setText(Long.toString(driftStart_watch));
        TS3.setText(Long.toString(driftStart_phone));

    }

    @Override
    protected void onDestroy() {
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        mqttHelper.closeClient();
        super.onBackPressed();
    }

    public void pause(View view){
        mqttHelper.setPublishPermission(pause_flag);
        pause_flag = !pause_flag;

        if(pause_flag){
            p1_button.setText("Resume");
        }else{
            p1_button.setText("Pause");
        }
    }

    private void startMqtt(String ip, String port, String user, String password){
        mqttHelper = new MqttHelper(getApplicationContext(),ip,port,user,password);
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {

            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }
}
