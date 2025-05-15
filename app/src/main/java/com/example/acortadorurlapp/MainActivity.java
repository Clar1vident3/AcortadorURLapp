package com.example.acortadorurlapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.acortadorurlapp.models.User; // Asegúrate de que esta importación exista
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.acortadorurlapp.ShortenRequest;
import com.example.acortadorurlapp.ShortenResponse;
import com.example.acortadorurlapp.RetrofitClient;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private EditText etUrl;
    private TextView tvResult;
    private Button btnShorten, btnCopy; // Eliminar btnMakePremium de aquí
    private TextView tvAttemptsCounter; // Tu TextView de intentos
    private Button btnGoPremium; // Tu botón "Hazte Premium"
    private Button btnSignOut; // Tu botón "Cerrar Sesión"

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    private User currentUserModel; // Para almacenar el modelo de usuario de Firestore

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase Auth y Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configurar Google Sign-Out
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Inicialización de Vistas de la UI
        etUrl = findViewById(R.id.et_url);
        tvResult = findViewById(R.id.tv_result);
        btnShorten = findViewById(R.id.btn_shorten);
        btnCopy = findViewById(R.id.btn_copy);
        tvAttemptsCounter = findViewById(R.id.tvAttemptsCounter); // ID de tu XML
        btnGoPremium = findViewById(R.id.btnGoPremium); // ID de tu XML
        btnSignOut = findViewById(R.id.btnSignOut); // ID de tu XML

        // Listeners
        btnShorten.setOnClickListener(v -> shortenUrl());
        btnCopy.setOnClickListener(v -> copyUrlToClipboard());

        btnGoPremium.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PremiumActivity.class);
            startActivity(intent);
        });

        btnSignOut.setOnClickListener(v -> signOut());
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
        } else {
            loadUserData(currentUser.getUid());
        }
    }

    private void loadUserData(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentUserModel = documentSnapshot.toObject(User.class);
                if (currentUserModel != null) {
                    updateUserUI(); // Llamar a la función de actualización de UI
                } else {
                    Log.e(TAG, "Error: User object is null after converting document. UID: " + uid);
                    Toast.makeText(this, "Error al cargar datos del usuario. Por favor, reinicia la app.", Toast.LENGTH_LONG).show();
                    navigateToLogin(); // Por seguridad, redirigir al login
                }
            } else {
                Log.w(TAG, "Documento de usuario no encontrado en Firestore para UID: " + uid + ". Redirigiendo a Login.");
                navigateToLogin(); // Si no existe el documento, algo salió mal o es un usuario viejo.
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error al cargar datos del usuario desde Firestore: ", e);
            Toast.makeText(this, "Error al cargar datos del usuario: " + e.getMessage(), Toast.LENGTH_LONG).show();
            navigateToLogin();
        });
    }

    // --- Tu función updateUI actualizada ---
    private void updateUserUI() {
        if (currentUserModel == null) {
            // Esto no debería pasar si loadUserData funciona correctamente, pero es una buena verificación.
            Log.w(TAG, "updateUserUI llamado con currentUserModel nulo.");
            return;
        }

        if (currentUserModel.isPremium()) {
            tvAttemptsCounter.setText("¡Usuario Premium!");
            btnGoPremium.setVisibility(View.GONE); // Oculta el botón premium si ya es premium
            btnShorten.setEnabled(true); // El botón de acortar siempre está habilitado para premium
            etUrl.setEnabled(true); // Asegúrate de que el EditText esté habilitado para premium
        } else {
            tvAttemptsCounter.setText("Intentos Gratis: " + currentUserModel.getFreeAttemptsRemaining());
            btnGoPremium.setVisibility(View.VISIBLE); // Muestra el botón premium

            // Habilita/deshabilita el botón de acortar y el EditText según los intentos restantes
            boolean hasAttempts = currentUserModel.getFreeAttemptsRemaining() > 0;
            btnShorten.setEnabled(hasAttempts);
            etUrl.setEnabled(hasAttempts);

            if (!hasAttempts) {
                Toast.makeText(this, "Has agotado tus intentos gratuitos. ¡Hazte Premium!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void shortenUrl() {
        String originalUrl = etUrl.getText().toString().trim();
        if (originalUrl.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa una URL.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ya validamos los intentos en updateUserUI y deshabilitamos el botón,
        // pero esta es una doble verificación.
        if (currentUserModel == null || (!currentUserModel.isPremium() && currentUserModel.getFreeAttemptsRemaining() <= 0)) {
            Toast.makeText(this, "No tienes intentos restantes o tu estado no es válido. Hazte Premium.", Toast.LENGTH_LONG).show();
            return;
        }

        ShortenRequest request = new ShortenRequest(originalUrl);
        RetrofitClient.getApiService().shortenUrl(request).enqueue(new Callback<ShortenResponse>() {
            @Override
            public void onResponse(Call<ShortenResponse> call, Response<ShortenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String shortUrl = response.body().getShortUrl();
                    tvResult.setText("URL Acortada: " + shortUrl);
                    tvResult.setVisibility(View.VISIBLE);
                    btnCopy.setVisibility(View.VISIBLE);

                    // Decrementar intentos si no es premium
                    if (!currentUserModel.isPremium()) {
                        decrementAttempts();
                    }
                } else {
                    String errorMsg = "Error al acortar URL. Código: " + response.code();
                    // ... (resto de tu manejo de errores) ...
                    Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Response failed: " + response.errorBody());
                }
            }

            @Override
            public void onFailure(Call<ShortenResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Network error: ", t);
            }
        });
    }

    private void decrementAttempts() {
        if (currentUserModel != null && !currentUserModel.isPremium()) {
            int newAttempts = currentUserModel.getFreeAttemptsRemaining() - 1;
            currentUserModel.setFreeAttemptsRemaining(newAttempts); // Actualiza el modelo local

            // Actualizar en Firestore
            DocumentReference userRef = db.collection("users").document(mAuth.getCurrentUser().getUid());
            userRef.update("freeAttemptsRemaining", newAttempts)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Intentos restantes actualizados en Firestore.");
                        updateUserUI(); // Vuelve a llamar a updateUI para reflejar los cambios en la interfaz
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al actualizar intentos en Firestore.", e);
                        Toast.makeText(MainActivity.this, "Error al guardar intentos.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void copyUrlToClipboard() {
        String urlToCopy = tvResult.getText().toString().replace("URL Acortada: ", "");
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("URL Acortada", urlToCopy);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "URL copiada al portapapeles", Toast.LENGTH_SHORT).show();
        }
    }

    private void signOut() {
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Log.d(TAG, "Google Sign-Out complete.");
            navigateToLogin();
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}