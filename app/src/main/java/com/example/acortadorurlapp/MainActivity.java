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
import androidx.core.content.ContextCompat; // Añadir esta importación para los colores

import com.example.acortadorurlapp.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration; // ¡IMPORTANTE!

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.acortadorurlapp.ShortenRequest;
import com.example.acortadorurlapp.ShortenResponse;
import com.example.acortadorurlapp.RetrofitClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // --- Declaraciones de variables
    private EditText etUrl;
    private TextView tvResult;
    private Button btnShorten, btnCopy;
    private TextView tvAttemptsCounter;
    private Button btnGoPremium;
    private Button btnSignOut;
    private Button btnViewHistory; // Declaración añadida si no estaba explícitamente

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    private User currentUserModel; // Para almacenar el modelo de usuario de Firestore
    private ListenerRegistration userStatusListener; // ¡NUEVO: Para el listener en tiempo real!
    // --- Fin de las declaraciones ---

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
        btnViewHistory = findViewById(R.id.btnViewHistory); // Asegúrate de que este también está inicializado


        // Listeners
        btnShorten.setOnClickListener(v -> shortenUrl());
        btnCopy.setOnClickListener(v -> copyUrlToClipboard());

        btnGoPremium.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PremiumActivity.class);
            startActivity(intent);
        });

        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UrlListActivity.class);
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
            // ¡CAMBIO CLAVE AQUÍ! Usar setupFirestoreListener en lugar de loadUserData
            setupFirestoreListener(currentUser.getUid());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // ¡IMPORTANTE! Eliminar el listener cuando la actividad no está visible
        if (userStatusListener != null) {
            userStatusListener.remove();
            userStatusListener = null;
            Log.d(TAG, "Firestore user status listener removed.");
        }
    }

    // ¡NUEVO MÉTODO! Este reemplaza a loadUserData
    private void setupFirestoreListener(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);

        // Si ya hay un listener activo (ej. por onStart después de onStop), lo removemos
        // para evitar que se creen múltiples listeners duplicados
        if (userStatusListener != null) {
            userStatusListener.remove();
        }

        userStatusListener = userRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                Toast.makeText(MainActivity.this, "Error de conexión con la base de datos.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                currentUserModel = snapshot.toObject(User.class);
                if (currentUserModel != null) {
                    Log.d(TAG, "User data updated from Firestore. Premium: " + currentUserModel.isPremium() + ", Attempts: " + currentUserModel.getFreeAttemptsRemaining());
                    updateUserUI(); // <--- Se llama cada vez que el documento cambia
                } else {
                    Log.e(TAG, "Error: User object is null after converting document from snapshot. Document: " + snapshot.getData());
                    Toast.makeText(MainActivity.this, "Error crítico al cargar datos del usuario.", Toast.LENGTH_LONG).show();
                    navigateToLogin(); // Por seguridad, redirigir
                }
            } else {
                Log.d(TAG, "Current user document is null or does not exist. Redirigiendo a Login.");
                navigateToLogin();
            }
        });
        Log.d(TAG, "Firestore user status listener set up.");
    }

    // ELIMINAR ESTE MÉTODO: Ya no es necesario con el SnapshotListener
    // private void loadUserData(String uid) {
    //     // ... (código anterior)
    // }

    private void updateUserUI() {
        if (currentUserModel == null) {
            Log.w(TAG, "updateUserUI llamado con currentUserModel nulo.");
            // Ocultar/deshabilitar elementos de la UI si no hay datos de usuario
            tvAttemptsCounter.setText("Cargando...");
            btnGoPremium.setVisibility(View.GONE);
            btnShorten.setEnabled(false);
            etUrl.setEnabled(false);
            return;
        }

        if (currentUserModel.isPremium()) {
            tvAttemptsCounter.setText("¡Usuario Premium!");
            tvAttemptsCounter.setTextColor(ContextCompat.getColor(this, R.color.black)); // Asume que tienes este color
            btnGoPremium.setVisibility(View.GONE); // Oculta el botón "Hazte Premium"
            btnShorten.setEnabled(true);
            etUrl.setEnabled(true);
        } else {
            tvAttemptsCounter.setText("Intentos Gratis: " + currentUserModel.getFreeAttemptsRemaining());
            tvAttemptsCounter.setTextColor(ContextCompat.getColor(this, android.R.color.black)); // O tu color predeterminado
            btnGoPremium.setVisibility(View.VISIBLE); // Muestra el botón "Hazte Premium"

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

        if (!isValidUrl(originalUrl)) {
            Toast.makeText(this, "URL no válida. Debe comenzar con http:// o https://", Toast.LENGTH_LONG).show();
            return;
        }

        // Se verifica el modelo actual, que ahora se actualiza en tiempo real
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

                    // La lógica de decrementAttempts ya usa currentUserModel que es actualizado por el listener
                    if (!currentUserModel.isPremium()) { // Solo decrementar si no es premium
                        decrementAttempts();
                    }
                } else {
                    String errorMsg = "Error al acortar URL. Código: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += ". Mensaje: " + response.errorBody().string();
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing error body", e);
                        }
                    }
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
        // Esta función ahora se basa en que currentUserModel ya está actualizado por el listener
        // y solo se llama si currentUserModel.isPremium() es false en el shortenUrl()
        if (currentUserModel != null) { // Ya verificamos !isPremium() antes de llamar a esto
            int newAttempts = currentUserModel.getFreeAttemptsRemaining() - 1;
            // No actualizamos currentUserModel directamente aquí porque el listener lo hará por nosotros
            // currentUserModel.setFreeAttemptsRemaining(newAttempts); // ¡QUITAR ESTA LÍNEA!

            DocumentReference userRef = db.collection("users").document(mAuth.getCurrentUser().getUid());
            userRef.update("freeAttemptsRemaining", newAttempts)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Intentos restantes actualizados en Firestore.");
                        // No es necesario llamar updateUserUI() aquí, el SnapshotListener lo hará automáticamente
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
    // Metodo para validar URLs
    private boolean isValidUrl(String url) {
        String urlPattern = "^(https?://)?" +
                "([\\w-]+\\.)+" +
                "([a-z\\u00a1-\\uffff]{2,63})" +
                "(/[-\\w\\u00a1-\\uffff@:%_+.~#?&/=]*)?$";

        Pattern pattern = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url);

        return matcher.matches();
    }
}