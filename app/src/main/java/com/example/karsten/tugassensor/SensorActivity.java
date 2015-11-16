package com.example.karsten.tugassensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import java.util.List;

public class SensorActivity extends AppCompatActivity implements SensorEventListener{

    private SensorManager sensorManager;
    private Sensor accelSensor;
    private Sensor gyroSensor;
    private Sensor lightSensor;
    private float gravity[];
    private float linear_acceleration[];
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float[] deltaRotationVector;
    private float timestamp;
    private static final double EPSILON = 0.1f;
    private TextView accelText[],gyroText[], lightText;
    private RecyclerView rv;
    private RecyclerView.Adapter adapter;
    private LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);
        accelText = new TextView[3];
        accelText[0] = (TextView) findViewById(R.id.textView);
        accelText[1] = (TextView) findViewById(R.id.textView1);
        accelText[2] = (TextView) findViewById(R.id.textView2);
        gyroText = new TextView[4];
        gyroText[0] = (TextView) findViewById(R.id.textView3);
        gyroText[1] = (TextView) findViewById(R.id.textView4);
        gyroText[2] = (TextView) findViewById(R.id.textView5);
        lightText = (TextView) findViewById(R.id.textView6);

        gravity = new float[3];
        linear_acceleration= new float[3];
        deltaRotationVector = new float[4];
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener( this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        adapter = new ListSensorAdapter(deviceSensors);

        layoutManager = new LinearLayoutManager(this);
        rv = (RecyclerView) findViewById(R.id.rv);
        rv.setLayoutManager(layoutManager);
        rv.setAdapter(adapter);

    }
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    public void onSensorChanged(SensorEvent event){
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            final float alpha = 0.8f;

            // Isolate the force of gravity with the low-pass filter.
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            // Remove the gravity contribution with the high-pass filter.
            linear_acceleration[0] = event.values[0] - gravity[0];
            linear_acceleration[1] = event.values[1] - gravity[1];
            linear_acceleration[2] = event.values[2] - gravity[2];
            String label[]={"X: ", "Y: ", "Z: "};
            for( int i=0;i<3;i++){
                accelText[i].setText(label[i] + String.format("%.1f",linear_acceleration[i]));
            }
        }
        else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            // This timestep's delta rotation to be multiplied by the current rotation
            // after computing it from the gyro sample data.
            if (timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
                // Axis of the rotation sample, not normalized yet.
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];

                String label[] = {"X: ", "Y: ", "Z: "};
                for (int i = 0; i < 3; i++) {
                    gyroText[i].setText(label[i] + String.format("%.1f",event.values[i]));
                }
            }
            timestamp = event.timestamp;
            float[] deltaRotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
            // User code should concatenate the delta rotation we computed with the current rotation
            // in order to get the updated rotation.
            // rotationCurrent = rotationCurrent * deltaRotationMatrix;
        }
        else if(sensor.getType() == Sensor.TYPE_LIGHT){
            lightText.setText(String.format("%.1f",event.values[0]));
        }
    }
}
