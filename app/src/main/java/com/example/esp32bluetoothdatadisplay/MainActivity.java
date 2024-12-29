package com.example.esp32bluetoothdatadisplay;

import android.content.Intent;
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
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

    private ListView listViewPrevLap;
    private ArrayAdapter<String> listAdapter;
    private List<String> prevLapList = new ArrayList<>();

    private boolean isPitAreaSoundPlayed = false;

    private Handler handler = new Handler();

    private MediaPlayer mediaPlayerPit;        // Pit Area ses dosyası
    private MediaPlayer mediaPlayerFastestLap; // Fastest Lap ses dosyası

    private static final String ESP32_DEVICE_NAME = "ESP32LapTimer"; // ESP32 cihaz adı
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // DatabaseHelper
    private DatabaseHelper databaseHelper;

    // Yeni buton
    private Button buttonViewLaps;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Veritabanı kurulumu
        databaseHelper = new DatabaseHelper(this);

        // Ses dosyalarını yükleyin
        mediaPlayerPit = MediaPlayer.create(this, R.raw.pitarea);       // Pit alanı sesi
        mediaPlayerFastestLap = MediaPlayer.create(this, R.raw.fastestlap1); // Fastest Lap sesi

        // Tam ekran modu
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN   // Durum çubuğunu gizler
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // Gezinme çubuğunu gizler
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // Kullanıcı etkileşimi sırasında çubukları gizler
        );

        // UI bileşenlerini bağlayın
        textViewCurrentLap = findViewById(R.id.textViewCurrentLap);
        listViewPrevLap = findViewById(R.id.listViewPrevLap);
        textViewPitArea = findViewById(R.id.textViewPitArea);
        textViewDelta = findViewById(R.id.textViewDelta);
        textViewFast = findViewById(R.id.textViewFast);
        textViewLap = findViewById(R.id.textViewLap);

        // Butonu bağlama
        buttonViewLaps = findViewById(R.id.buttonViewLaps);
        buttonViewLaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, LapsActivity.class);
                startActivity(intent);
            }
        });

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
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e("Bluetooth", "Bağlantı kapatılamadı", e);
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
                        retryHandler.postDelayed(this, 1000); // 1 saniye sonra tekrar dene
                    }
                }).start();
            }
        };

        connectionAttempt.run();
    }

    /**
     * 4. çözüm: Gelen veriyi bir buffer'da biriktirip '\n' karakterine göre parçalayıp JSON parse etmek.
     * ESP tarafında her JSON'un sonuna mutlaka '\n' eklenmesi gerekiyor.
     */
    private void receiveBluetoothData() {
        try {
            inputStream = bluetoothSocket.getInputStream();
            StringBuilder readBuffer = new StringBuilder();
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                // Veri okuma
                bytes = inputStream.read(buffer);
                if (bytes > 0) {
                    String incomingPart = new String(buffer, 0, bytes);
                    readBuffer.append(incomingPart);

                    // '\n' karakterine göre JSON mesajlarını ayır
                    int newlineIndex;
                    while ((newlineIndex = readBuffer.indexOf("\n")) != -1) {
                        // Satır sonuna kadar olan kısmı al
                        String singleJson = readBuffer.substring(0, newlineIndex).trim();
                        // İşlenen kısmı buffer'dan sil
                        readBuffer.delete(0, newlineIndex + 1);

                        if (!singleJson.isEmpty()) {
                            try {
                                JSONObject jsonObject = new JSONObject(singleJson);
                                handleJsonObject(jsonObject);
                            } catch (JSONException e) {
                                Log.e("BluetoothData", "JSON parse error: " + singleJson, e);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e("Bluetooth", "Veri alınamadı veya bağlantı kesildi", e);
            reconnectBluetooth();
        }
    }

    /**
     * Gelen JSON objesinin "event" alanına göre ilgili işlemleri çağır.
     */
    private void handleJsonObject(JSONObject jsonObject) {
        try {
            if (jsonObject.has("event")) {
                String event = jsonObject.getString("event");
                switch (event) {
                    case "pit_area_entry":
                        handlePitArea(true);
                        break;

                    case "pit_area_exit":
                        handlePitArea(false);
                        break;

                    case "current_lap":
                        long currentLapTime = jsonObject.getLong("lap_time");
                        handleCurrentLap(currentLapTime);
                        break;

                    case "prev_lap":
                        int lapNumber = jsonObject.getInt("lap_number");
                        long prevLapTime = jsonObject.getLong("lap_time");
                        handlePrevLap(lapNumber, prevLapTime);
                        break;

                    case "fastest_lap":
                        int fastestLapNumber = jsonObject.getInt("lap_number");
                        long fastestLapTime = jsonObject.getLong("lap_time");
                        handleFastestLap(fastestLapNumber, fastestLapTime);
                        break;

                    case "lap_number":
                        int currentLapNumber = jsonObject.getInt("lap_number");
                        handleLapNumber(currentLapNumber);
                        break;

                    case "segment_time_diff":
                        String difference = jsonObject.getString("difference");
                        handleSegmentTimeDiff(difference);
                        break;

                    default:
                        Log.w("BluetoothData", "Bilinmeyen event: " + event);
                        break;
                }
            }
        } catch (JSONException e) {
            Log.e("BluetoothData", "JSON parse error in handleJsonObject", e);
        }
    }

    private void handlePitArea(boolean isEntering) {
        if (isEntering) {
            runOnUiThread(() -> {
                textViewPitArea.setText("PIT AREA");
                textViewPitArea.setVisibility(View.VISIBLE);

                // Pit alanına girildiğinde delta time'ı gizle
                textViewDelta.setText("");
                textViewDelta.setVisibility(View.INVISIBLE);

                // Pit alanı sesini çal
                if (!isPitAreaSoundPlayed) {
                    mediaPlayerPit.start();
                    isPitAreaSoundPlayed = true;
                }
            });
        } else {
            runOnUiThread(() -> {
                textViewPitArea.setText("");
                textViewPitArea.setVisibility(View.INVISIBLE);
                isPitAreaSoundPlayed = false;
            });
        }
    }

    private void handleCurrentLap(long currentLapTime) {
        String formattedLapTime = formatLapTime(currentLapTime);
        runOnUiThread(() -> {
            textViewCurrentLap.setText(formattedLapTime);
        });
    }

    private void handlePrevLap(int lapNumber, long prevLapTime) {
        String formattedPrevLapTime = formatLapTime(prevLapTime);
        runOnUiThread(() -> {
            // Listeye ekle
            prevLapList.add(0, formattedPrevLapTime);
            listAdapter.notifyDataSetChanged();

            // Yeni turu veritabanına ekleyelim
            addLapToDatabase(lapNumber, formattedPrevLapTime, false);
        });
    }

    private void handleFastestLap(int lapNumber, long fastestLapTime) {
        String formattedFastestLapTime = formatLapTime(fastestLapTime);
        runOnUiThread(() -> {
            textViewFast.setText(formattedFastestLapTime);
            textViewFast.setVisibility(View.VISIBLE);
            playFastestLapSound();  // Fastest Lap sesi çal

            // En hızlı turu veritabanında işaretle
            databaseHelper.updateFastestLap(lapNumber);
        });
    }

    private void handleLapNumber(int lapNumber) {
        runOnUiThread(() -> {
            textViewLap.setText(String.valueOf(lapNumber));
            textViewLap.setVisibility(View.VISIBLE);
        });
    }

    private void handleSegmentTimeDiff(String difference) {
        runOnUiThread(() -> {
            textViewDelta.setText(difference);
            textViewDelta.setVisibility(View.VISIBLE);

            // Mevcut arka plan
            Drawable currentBackground = textViewDelta.getBackground();
            if (currentBackground == null) {
                currentBackground = new ColorDrawable(Color.TRANSPARENT);
            }

            // Mesaja göre hedef renk
            Drawable targetBackground;
            if (difference.startsWith("-")) {
                targetBackground = new ColorDrawable(Color.GREEN);
            } else if (difference.startsWith("+")) {
                targetBackground = new ColorDrawable(Color.RED);
            } else {
                targetBackground = new ColorDrawable(Color.GRAY);
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

    // Lap verilerini veritabanına ekleme (hem tarih hem saat bilgisi)
    private void addLapToDatabase(int lapNumber, String lapTime, boolean isFastest) {
        // 1) Tarih (dd/MM/yyyy)
        String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        // 2) Saat (HH:mm:ss)
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        // 3) En hızlı tur olup olmadığı
        boolean isCurrentFastest = false;
        List<Lap> existingLaps = databaseHelper.getAllLaps();
        if (existingLaps.isEmpty()) {
            isCurrentFastest = true;
        } else {
            String currentFastestTime = existingLaps.get(0).getLapTime();
            if (compareLapTimes(lapTime, currentFastestTime) < 0) {
                isCurrentFastest = true;
            }
        }

        // 4) Lap nesnesini 6 parametre ile oluştur (id, lapNumber, lapTime, date, isFastest, datetime)
        Lap lap = new Lap(
                0,                // id veritabanında autoincrement
                lapNumber,
                lapTime,
                currentDate,
                isCurrentFastest,
                currentTime       // saat bilgisi
        );

        // 5) Veritabanına ekle
        databaseHelper.addLap(lap);
    }

    // Lap sürelerini karşılaştırma (daha hızlı ise negatif, daha yavaş ise pozitif, eşitse 0 döner)
    private int compareLapTimes(String lapTime1, String lapTime2) {
        // "mm:ss.SSS" formatında olduğunu varsayıyoruz
        String[] parts1 = lapTime1.split("[:.]");
        String[] parts2 = lapTime2.split("[:.]");

        int minutes1 = Integer.parseInt(parts1[0]);
        int seconds1 = Integer.parseInt(parts1[1]);
        int millis1 = Integer.parseInt(parts1[2]);

        int minutes2 = Integer.parseInt(parts2[0]);
        int seconds2 = Integer.parseInt(parts2[1]);
        int millis2 = Integer.parseInt(parts2[2]);

        long totalMillis1 = minutes1 * 60000 + seconds1 * 1000 + millis1;
        long totalMillis2 = minutes2 * 60000 + seconds2 * 1000 + millis2;

        return Long.compare(totalMillis1, totalMillis2);
    }

    // "mm:ss.SSS" formatında gösterme
    private String formatLapTime(long time) {
        long lapMinutes = time / 60000;
        long lapSeconds = (time / 1000) % 60;
        long lapMilliseconds = time % 1000;

        // Milisaniyeleri 3 basamaklı formatta göster
        String formattedMilliseconds = String.valueOf(lapMilliseconds);
        if (lapMilliseconds < 10) {
            formattedMilliseconds = "00" + formattedMilliseconds;
        } else if (lapMilliseconds < 100) {
            formattedMilliseconds = "0" + formattedMilliseconds;
        }

        return lapMinutes + ":" + String.format("%02d", lapSeconds) + "." + formattedMilliseconds;
    }

    private void reconnectBluetooth() {
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e("Bluetooth", "Socket kapatılamadı", e);
        }

        initializeBluetoothConnection(); // Yeniden bağlanmayı deneyin
    }
}
