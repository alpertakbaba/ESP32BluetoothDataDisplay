package com.example.esp32bluetoothdatadisplay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private void hideSystemUI() {
        // Sistem çubuklarını gizle
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout'u ayarla
        setContentView(R.layout.activity_splash);

        // Sistem çubuklarını gizle
        hideSystemUI();

        // Layout'un root kısmını bulun
        RelativeLayout splashBackground = findViewById(R.id.splash_background);

        // Animasyonu yükleyin
        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        // Animasyonu başlatın
        splashBackground.startAnimation(fadeInAnimation);

        // 5 saniye sonra MainActivity'ye geç
        new android.os.Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish(); // SplashActivity'yi kapat
        }, 2000);
    }
}
