package com.example.acortadorurlapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private EditText etUrl;
    private TextView tvResult;
    private Button btnShorten, btnCopy;
    private String currentShortUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar vistas
        etUrl = findViewById(R.id.et_url);
        tvResult = findViewById(R.id.tv_result);
        btnShorten = findViewById(R.id.btn_shorten);
        btnCopy = findViewById(R.id.btn_copy);

        // Botón para acortar URL
        btnShorten.setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            if (!url.isEmpty()) {
                shortenUrl(url);
            } else {
                Toast.makeText(this, "Ingresa una URL válida", Toast.LENGTH_SHORT).show();
            }
        });

        // Botón para copiar (inicialmente oculto)
        btnCopy.setVisibility(View.GONE);
        btnCopy.setOnClickListener(v -> copyToClipboard());
    }

    private void shortenUrl(String originalUrl) {
        // Mostrar carga (opcional)
        btnShorten.setEnabled(false);
        btnShorten.setText("Acortando...");

        ApiService apiService = RetrofitClient.getApiService();
        Call<ShortenResponse> call = apiService.shortenUrl(new ShortenRequest(originalUrl));

        call.enqueue(new Callback<ShortenResponse>() {
            @Override
            public void onResponse(Call<ShortenResponse> call, Response<ShortenResponse> response) {
                btnShorten.setEnabled(true);
                btnShorten.setText("Acortar URL");

                if (response.isSuccessful() && response.body() != null) {
                    currentShortUrl = response.body().getShortUrl();
                    tvResult.setText("URL corta:\n" + currentShortUrl);
                    btnCopy.setVisibility(View.VISIBLE);
                } else {
                    tvResult.setText("Error: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ShortenResponse> call, Throwable t) {
                btnShorten.setEnabled(true);
                btnShorten.setText("Acortar URL");
                tvResult.setText("Error de conexión: " + t.getMessage());
            }
        });
    }

    private void copyToClipboard() {
        if (!currentShortUrl.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("URL corta", currentShortUrl);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "URL copiada al portapapeles", Toast.LENGTH_SHORT).show();
        }
    }
}