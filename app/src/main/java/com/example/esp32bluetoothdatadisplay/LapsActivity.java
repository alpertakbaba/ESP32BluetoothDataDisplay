package com.example.esp32bluetoothdatadisplay;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LapsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewLaps;
    private LapAdapter lapAdapter;
    private DatabaseHelper databaseHelper;
    private List<Lap> lapList;

    private Button buttonResetLaps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_laps);

        // ActionBar'da geri düğmesini etkinleştirme
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Veritabanı ve RecyclerView kurulumu
        databaseHelper = new DatabaseHelper(this);
        lapList = databaseHelper.getAllLaps();

        recyclerViewLaps = findViewById(R.id.recyclerViewLaps);
        if (recyclerViewLaps == null) {
            Toast.makeText(this, "RecyclerView bulunamadı!", Toast.LENGTH_SHORT).show();
            finish(); // Activity'i kapat
            return;
        }

        recyclerViewLaps.setLayoutManager(new LinearLayoutManager(this));
        lapAdapter = new LapAdapter(this, lapList);
        recyclerViewLaps.setAdapter(lapAdapter);

        // Reset Butonunu Bağlama
        buttonResetLaps = findViewById(R.id.buttonResetLaps);
        buttonResetLaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetLaps();
            }
        });
        hideSystemUI();
    }

    // ActionBar'daki geri düğmesine tıklama olayını yakalama
    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    private void resetLaps() {
        // Veritabanını temizle
        databaseHelper.clearAllLaps();
        // RecyclerView güncelle
        lapList.clear();
        lapAdapter.notifyDataSetChanged();
        Toast.makeText(this, "Turlar sıfırlandı.", Toast.LENGTH_SHORT).show();
    }
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN    // Durum çubuğunu gizler
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // Gezinme çubuğunu gizler
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }
}
