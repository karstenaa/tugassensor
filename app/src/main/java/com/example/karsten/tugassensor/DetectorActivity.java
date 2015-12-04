package com.example.karsten.tugassensor;

import android.app.DownloadManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetectorActivity extends AppCompatActivity implements SensorEventListener{

    private SensorManager sensorManager;
    private Sensor accelSensor;

    private TextView calibrateText;
   // private float interval;
    private final float GRAVITY = 9.8f;
    private final float DELTA_SUDDEN = 3f;
    private final float RADIUS_CIRCLE = 3f;
    private float linearAcceleration;
    private float minAccel;
    private Date minTime;
    private float maxAccel;
    private Date maxTime;
    private float answer;
    private final float dataStand[] = {8.95f, 10.65f, 8.05f, 8.27f ,9.70f, 9.91f, 8.46f, 9.45f, 8.26f, 8.57f, 6.02f, 5.62f, 5.31f, 7.11f,  6.90f};
    private final float dataSit[] = {21.00f, 23.08f, 21.05f, 23.33f ,20.22f, 17.38f, 20.23f, 19.85f, 20.99f, 20.00f, 19.65f, 20.81f, 21.85f, 20.17f, 20.18f};
    private long history[];
    private int historyIter;
    private boolean isChange;
    private boolean isChangePrev;
    private int result[];
    private int state;
    private int iterate;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);
        calibrateText = (TextView) findViewById(R.id.textView7);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
        minAccel=maxAccel = 9.8f;
        minTime = maxTime = new Date();
        history = new long[6];
        result = new int[3];
    }
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    public void onSensorChanged(SensorEvent event){
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
           // final float alpha = 0.8f;
           // gravity = alpha * gravity + (1 - alpha) * event.values[2];
            linearAcceleration = event.values[2]; //- gravity;
            String label[] = {"X: ", "Y: ", "Z: "};
           // calibrateText.setText(String.valueOf(nowHeight));
            if(linearAcceleration<GRAVITY-DELTA_SUDDEN){
                minAccel = linearAcceleration;
                minTime = new Date();
                isChange=true;
            }
            else if(linearAcceleration>GRAVITY+DELTA_SUDDEN){
                maxAccel = linearAcceleration;
                maxTime = new Date();
                isChange=true;
            }
            else{
                isChange =false;
            }

            evaluate();
            checkState();
           // Log.d("debug ", String.format("%.2f %.2f %d", minAccel, maxAccel,   maxTime.getTime() -  minTime.getTime()));
        }
    }
    public void checkState(){
        iterate++;
        result[state]++;
        if(iterate==80){
            String status;
            if(result[0] > result[1] && result[0] > result[2]){
                status="Duduk";
            }
            else if(result[1] > result[2]){
                status="Berdiri";
            }
            else{
                status="Berjalan";
            }
            for(int i=0;i<3;i++){
                result[i]=0;
            }
            iterate=0;
            sendRequest(status);
        }
    }
    public void sendRequest(final String status){
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest sr = new StringRequest(Request.Method.POST,"http://10.151.43.80:3000/sensor_readings", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("response",response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("error", error.getMessage());
            }
        }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<String, String>();
                params.put("status",status);
                return params;
            }
        };
        queue.add(sr);

    }
    public void evaluate(){
        if(maxAccel-minAccel <DELTA_SUDDEN){
            return;
        }
        else if(minTime.before(maxTime)){
            answer = maxAccel+minAccel;
        }
        else{
            answer = maxAccel-minAccel;
        }
        long nowTime = new Date().getTime();
        long pastTime = history[historyIter];
        if(isChange && !isChangePrev){
            isChangePrev=true;
        }
        else if(!isChange && isChangePrev){
            history[historyIter]=nowTime;
            historyIter= (historyIter+1)%history.length;
            isChangePrev=false;
        }

        if(nowTime-pastTime<2000){
            calibrateText.setText("BERJALAN");
            state=2;
            return;
        }
        int countSit=0,countStand=0;
        for(int i=0;i<dataSit.length;i++){
            if(dataSit[i]+RADIUS_CIRCLE >=answer && dataSit[i]-RADIUS_CIRCLE<=answer){
                countSit++;
            }
        }
        for(int i=0;i<dataStand.length;i++){
            if(dataStand[i]+RADIUS_CIRCLE >=answer && dataStand[i]-RADIUS_CIRCLE<=answer){
                countStand++;
            }
        }
        float probSit = (float)countSit/(dataSit.length + dataStand.length);
        float probStand = (float)countStand/(dataSit.length + dataStand.length);
        if(probSit>probStand){
            calibrateText.setText("DUDUK");
            state=0;
        }
        else{
            calibrateText.setText("BERDIRI");
            state=1;
        }
        //Log.d("debug ", String.format("%d %d", countStand, countSit));
        //minAccel=maxAccel=GRAVITY;
        //minTime=maxTime=new Date();
    }
}
