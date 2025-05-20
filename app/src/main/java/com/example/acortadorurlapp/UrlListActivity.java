package com.example.acortadorurlapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UrlListActivity extends AppCompatActivity {

    private RecyclerView rvUrls;
    private UrlsAdapter adapter;
    private List<ShortenResponse> urlList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_url_list);

        rvUrls = findViewById(R.id.rvUrls);
        rvUrls.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UrlsAdapter(urlList, shortCode -> deleteUrl(shortCode));
        rvUrls.setAdapter(adapter);

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        loadUrls();
    }

    private void loadUrls() {
        RetrofitClient.getApiService().getUrls().enqueue(new Callback<List<ShortenResponse>>() {
            @Override
            public void onResponse(Call<List<ShortenResponse>> call, Response<List<ShortenResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("URL_DEBUG", "Datos recibidos: " + response.body().toString());
                    urlList.clear();
                    urlList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ?
                                response.errorBody().string() : "null";
                        Log.e("API_ERROR", "Código: " + response.code() + ", Error: " + errorBody);
                        Toast.makeText(UrlListActivity.this,
                                "Error del servidor: " + response.code(), Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ShortenResponse>> call, Throwable t) {
                Log.e("API_FAILURE", "Error de conexión:", t);
                Toast.makeText(UrlListActivity.this,
                        "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void deleteUrl(String shortCode) {
        Log.d("DELETE", "Eliminando código: " + shortCode);

        RetrofitClient.getApiService().deleteUrl(shortCode).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        adapter.removeItem(shortCode);
                        Toast.makeText(UrlListActivity.this,
                                "URL eliminada", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    try {
                        String errorMsg = response.errorBody() != null ?
                                response.errorBody().string() : "Error desconocido";
                        Log.e("DELETE_ERROR", "Código: " + response.code() + ", Error: " + errorMsg);
                        runOnUiThread(() ->
                                Toast.makeText(UrlListActivity.this,
                                        "Error al eliminar: " + errorMsg, Toast.LENGTH_LONG).show());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("DELETE_FAIL", "Error completo:", t);
                runOnUiThread(() ->
                        Toast.makeText(UrlListActivity.this,
                                "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
}