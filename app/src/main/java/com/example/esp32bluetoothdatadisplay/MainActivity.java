package com.example.esp32bluetoothdatadisplay;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

    private ListView listViewPrevLap;

    private ArrayAdapter<String> listAdapter;
    private List<String> prevLapList = new ArrayList<>();

    private Handler handler = new Handler();

    private String lastMessage = ""; // Son gelen veriyi saklamak için


    private static final String ESP32_DEVICE_NAME = "ESP32LapTimer"; // ESP32 cihaz adı
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewCurrentLap = findViewById(R.id.textViewCurrentLap);
        listViewPrevLap = findViewById(R.id.listViewPrevLap);
        textViewPitArea = findViewById(R.id.textViewPitArea);

        // ListView için adapter ayarla
        listAdapter = new ArrayAdapter<>(this, R.layout.row, prevLapList);
        listViewPrevLap.setAdapter(listAdapter);

        // Bluetooth başlatma ve ESP32'ye bağlanma işlemini başlat
        initializeBluetoothConnection();
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

        @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals(ESP32_DEVICE_NAME)) {
                connectToDevice(device);
                break;
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        new Thread(() -> {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();
                runOnUiThread(() -> Toast.makeText(this, "Bluetooth bağlantısı kuruldu", Toast.LENGTH_SHORT).show());

                // Verileri okumaya başla
                receiveBluetoothData();
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Bağlantı başarısız", Toast.LENGTH_SHORT).show());
                Log.e("Bluetooth", "Bağlantı başarısız", e);
            }
        }).start();
    }

    private void receiveBluetoothData() {
        try {
            inputStream = bluetoothSocket.getInputStream();
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                bytes = inputStream.read(buffer);
                String incomingMessage = new String(buffer, 0, bytes);

                // Gelen mesajı loglayarak kontrol edin
                Log.d("BluetoothData", "Received message: " + incomingMessage);

                long currentTime = System.currentTimeMillis();
                if(incomingMessage.startsWith("PIT")){


                    handler.post(() ->textViewPitArea.setText(incomingMessage));
                }
                else if (incomingMessage.startsWith("Current Lap")) {
                    // Current Lap mesajı ise ve 300 ms geçtiyse güncelle

                    handler.post(() -> textViewCurrentLap.setText(incomingMessage));
                } else if (incomingMessage.startsWith("Prev Lap")) {
                    // Gelen mesaj Prev Lap ise ListView'e ekle
                    prevLapList.add(0, incomingMessage); // Yeni gelen veri listenin başına eklenir
                    handler.post(() -> {
                        listAdapter.notifyDataSetChanged();
                        Log.d("BluetoothData", "Prev Lap added to ListView: " + incomingMessage);
                    });
                }
            }
        } catch (IOException e) {
            Log.e("Bluetooth", "Veri alınamadı", e);
        }
    }
}
