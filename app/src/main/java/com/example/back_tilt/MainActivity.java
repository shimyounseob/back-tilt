package com.example.back_tilt;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor rotationVectorSensor; // 회전 벡터 센서: 가속도계, 자이로스코프, 지자기계 데이터를 센서 퓨전 알고리즘으로 결합, 3차원에서 회전 정보 파악

    private TextView rollAngleText;

    private float[] rotationMatrix = new float[9]; // 회전 행렬 계산
    private float[] remappedRotationMatrix = new float[9]; // 기본 좌표가 디바이스를 눕혔을때이므로, 세웠을때 모드로 매핑 다시할 거임
    private float[] orientationAngles = new float[3]; // 오일러 각도 저장 : 3차원 공간에서 물체의 회전상태 표현

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Edge to Edge 적용 (Insets 적용)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // UI 요소 초기화
        rollAngleText = findViewById(R.id.roll_angle_text);

        // SensorManager 초기화
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // 회전 벡터 센서 등록
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        if (rotationVectorSensor == null) {
            rollAngleText.setText("회전 벡터 센서를 지원하지 않습니다.");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            // 회전 행렬 계산
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

            // 좌표계 재매핑 (디바이스를 세웠을때에 맞도록 재매핑)
            // 앞뒤 방향을 나타내는 Z축을 새로운 Y축으로 사용함
            // 새로운 Z축은 오른손의 법칙으로 새로 결정됨
            SensorManager.remapCoordinateSystem(rotationMatrix,
                    SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedRotationMatrix);

            // 오일러 각도 계산
            SensorManager.getOrientation(remappedRotationMatrix, orientationAngles);

            // 라디안 단위 도 단위로 변환
            double azimuth = Math.toDegrees(orientationAngles[0]); // 방위각: 디바이스가 북쪽을 기준으로 어느 방향을 향하는지
            double pitch = Math.toDegrees(orientationAngles[1]);   // 앞뒤 기울기
            double roll = Math.toDegrees(orientationAngles[2]);    // 좌우 기울기

            // 각도 데이터 표시
            rollAngleText.setText(String.format("Azimuth: %.2f°\nPitch: %.2f°\nRoll: %.2f°", azimuth, pitch, roll));
        }
    }

    // 센서 정확도 변경 필요할 시 사용, 필요 없으면 그냥 비워둠
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 회전 벡터 센서 리스너 등록
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 센서 리스너 해제
        sensorManager.unregisterListener(this);
    }
}
