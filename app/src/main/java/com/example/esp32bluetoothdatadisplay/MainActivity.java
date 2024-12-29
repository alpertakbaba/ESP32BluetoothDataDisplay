package com.example.esp32bluetoothdatadisplay;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private TextView textViewCurrentLap;
    private TextView textViewPitArea;
    private TextView textViewDelta;
    private TextView textViewFast;
    private TextView textViewLap;

    Drawable currentBackground;
    Drawable targetBackground;

    private boolean isInPitArea = false;
    private boolean isPitAreaSoundPlayed = false;

    private ListView listViewPrevLap;
    private ArrayAdapter<String> listAdapter;
    private List<String> prevLapList = new ArrayList<>();

    private Handler handler = new Handler();

    private MediaPlayer mediaPlayerPit;       // Pit Area ses dosyası
    private MediaPlayer mediaPlayerFastestLap; // Fastest Lap ses dosyası

    private static final String ESP32_DEVICE_NAME = "ESP32LapTimer"; // ESP32 cihaz adı
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ses dosyalarını yükleyin
        mediaPlayerPit = MediaPlayer.create(this, R.raw.pitarea); // Pit alanı sesi için
        mediaPlayerFastestLap = MediaPlayer.create(this, R.raw.fastestlap1); // Fastest Lap sesi için

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN   // Durum çubuğunu gizler
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // Gezinme çubuğunu gizler
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // Kullanıcı etkileşimi sırasında çubukları gizler
        );

        textViewCurrentLap = findViewById(R.id.textViewCurrentLap);
        listViewPrevLap = findViewById(R.id.listViewPrevLap);
        textViewPitArea = findViewById(R.id.textViewPitArea);
        textViewDelta = findViewById(R.id.textViewDelta);
        textViewFast = findViewById(R.id.textViewFast);
        textViewLap = findViewById(R.id.textViewLap);

        listAdapter = new ArrayAdapter<>(this, R.layout.row, prevLapList);
        listViewPrevLap.setAdapter(listAdapter);

        initializeBluetoothConnection();
    }



    private void playFastestLapSound() {
        if (mediaPlayerFastestLap != null) {
            mediaPlayerFastestLap.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayerPit != null) {
            mediaPlayerPit.release();
            mediaPlayerPit = null;
        }
        if (mediaPlayerFastestLap != null) {
            mediaPlayerFastestLap.release();
            mediaPlayerFastestLap = null;
        }
    }

    @SuppressLint("MissingPermission")
    private void initializeBluetoothConnection() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth desteklenmiyor", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Lütfen Bluetooth'u etkinleştirin", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals(ESP32_DEVICE_NAME)) {
                connectToDevice(device);
                break;
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        Handler retryHandler = new Handler();
        Runnable connectionAttempt = new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    try {
                        bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                        bluetoothSocket.connect();
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Bluetooth bağlantısı kuruldu", Toast.LENGTH_SHORT).show());

                        receiveBluetoothData();
                        retryHandler.removeCallbacks(this);
                    } catch (IOException e) {

                        Log.e("Bluetooth", "Bağlantı başarısız", e);

                        retryHandler.postDelayed(this, 100);
                    }
                }).start();
            }
        };

        connectionAttempt.run();
    }

    private void receiveBluetoothData() {
        try {
            inputStream = bluetoothSocket.getInputStream();
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                bytes = inputStream.read(buffer);
                String incomingMessage = new String(buffer, 0, bytes).trim();

                Log.d("BluetoothData", "Received message: " + incomingMessage);

                if (incomingMessage.startsWith("Number")) {
                    String lapNumber = incomingMessage.replace("Number ", "").trim();
                    handler.post(() -> {
                        textViewLap.setText(lapNumber);
                        textViewLap.setVisibility(View.VISIBLE);

                    });

                }

                if (incomingMessage.startsWith("PIT AREA")) {
                    handler.post(() -> {
                        textViewPitArea.setText("PIT AREA"); // "PIT AREA" yazısını görünür yap
                        textViewPitArea.setVisibility(View.VISIBLE);

                        // Sesi doğrudan burada çal
                        MediaPlayer mediaPlayerPit = MediaPlayer.create(this, R.raw.pitarea);
                        mediaPlayerPit.start();

                        // Ses çalma tamamlandığında medya oynatıcıyı serbest bırak
                        mediaPlayerPit.setOnCompletionListener(mp -> {
                            mediaPlayerPit.release();
                        });
                    });
                } else if (incomingMessage.startsWith("OUTSIDE PIT")) {
                    handler.post(() -> {
                        textViewPitArea.setText(""); // "PIT AREA" yazısını temizle
                        textViewPitArea.setVisibility(View.INVISIBLE); // Pit ekranını gizle
                    });
                }



                if (incomingMessage.startsWith("-") || incomingMessage.startsWith("+") || incomingMessage.startsWith("00.00")) {
                    handler.post(() -> {
                        textViewDelta.setText(incomingMessage);
                        textViewDelta.setVisibility(View.VISIBLE);

                        // Mevcut arka plan
                        Drawable currentBackground = textViewDelta.getBackground();

                        // Mesaja göre hedef renk
                        Drawable targetBackground;
                        if (incomingMessage.startsWith("-")) {
                            targetBackground = new ColorDrawable(Color.GREEN);
                        } else if (incomingMessage.startsWith("+")) {
                            targetBackground = new ColorDrawable(Color.RED);
                        } else {
                            targetBackground = new ColorDrawable(Color.GRAY);
                        }

                        if (currentBackground == null) {
                            currentBackground = new ColorDrawable(Color.TRANSPARENT);
                        }

                        // TransitionDrawable oluştur
                        TransitionDrawable transition = new TransitionDrawable(new Drawable[]{
                                currentBackground,
                                targetBackground
                        });

                        textViewDelta.setBackground(transition);
                        transition.startTransition(300); // 300ms geçiş süresi
                    });
                }
                // Fastest Lap mesajı kontrolü ve ses çalma
                if (incomingMessage.startsWith("Fastest Lap")) {
                    String fastestLapTime = incomingMessage.replace("Fastest Lap ", "").trim();
                    handler.post(() -> {
                        textViewFast.setText(fastestLapTime);
                        textViewFast.setVisibility(View.VISIBLE);
                        playFastestLapSound();  // Fastest Lap sesi çal
                    });
                }

                if (incomingMessage.startsWith("Current Lap")) {
                    String currentLapTime = incomingMessage.replace("Current Lap ", "").trim();
                    handler.post(() -> textViewCurrentLap.setText(currentLapTime));
                }

                if (incomingMessage.startsWith("Prev Lap") && !incomingMessage.contains("Current Lap")) {
                    String prevLapTime = incomingMessage.replace("Prev Lap ", "").trim();
                    handler.post(() -> {
                        prevLapList.add(0, prevLapTime);
                        listAdapter.notifyDataSetChanged();
                    });
                }
            }
        } catch (IOException e) {
            Log.e("Bluetooth", "Veri alınamadı", e);
        }
    }
}
