// Archivo: app/src/main/java/com/example/acortadorurlapp/MainActivity.java
package com.example.acortadorurlapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Importar para depuración (logs)
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.acortadorurlapp.models.User; // Importa tu clase User

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity"; // Etiqueta para mensajes de log

    // Vistas de la UI
    private EditText etUrl;
    private TextView tvResult;
    private Button btnShorten, btnCopy;
    private Button btnSignOut, btnGoPremium; // Botones de Firebase
    private TextView tvAttemptsCounter; // TextView para el contador

    private String currentShortUrl = ""; // Para almacenar la URL acortada actual

    // Instancias de Firebase
    private FirebaseAuth mAuth; // Para autenticación
    private FirebaseFirestore db; // Para la base de datos Firestore

    // Variables de estado del usuario
    private int freeAttempts = 5; // Intentos gratuitos por defecto para nuevos usuarios
    private boolean isPremiumUser = false; // Indica si el usuario es premium

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar instancias de Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- Verificación de autenticación al inicio de la actividad ---
        // Se ejecuta antes de cargar cualquier vista para asegurar que haya un usuario logueado.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Si no hay un usuario logueado, redirige a la LoginActivity
            startActivity(new Intent(this, LoginActivity.class));
            finish(); // Finaliza esta actividad para que no se pueda volver atrás con el botón físico
            return; // Detiene la ejecución de onCreate si no hay usuario
        }

        // Si el usuario está logueado, carga el layout
        setContentView(R.layout.activity_main);

        // --- Inicialización de Vistas de la UI ---
        etUrl = findViewById(R.id.et_url);
        tvResult = findViewById(R.id.tv_result);
        btnShorten = findViewById(R.id.btn_shorten);
        btnCopy = findViewById(R.id.btn_copy); // Asegúrate que este ID sea correcto en tu XML
        // Si es btn_copy, entonces btnCopy = findViewById(R.id.btn_copy);

        // Nuevos elementos de UI para el estado del usuario
        btnSignOut = findViewById(R.id.btnSignOut);
        btnGoPremium = findViewById(R.id.btnGoPremium);
        tvAttemptsCounter = findViewById(R.id.tvAttemptsCounter);

        // --- Configuración de Listeners para los botones ---

        // Listener para el botón "Acortar URL"
        btnShorten.setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            if (!url.isEmpty()) {
                shortenUrl(url); // Llama al método que maneja la lógica de acortamiento
            } else {
                Toast.makeText(this, "Por favor, ingresa una URL válida", Toast.LENGTH_SHORT).show();
            }
        });

        // Listener para el botón "Copiar URL" (inicialmente oculto)
        btnCopy.setVisibility(View.GONE); // Asegura que esté oculto al inicio
        btnCopy.setOnClickListener(v -> copyToClipboard());

        // Listener para el botón "Cerrar Sesión"
        btnSignOut.setOnClickListener(v -> signOut());

        // Listener para el botón "Hazte Premium"
        btnGoPremium.setOnClickListener(v -> {
            // Inicia la PremiumActivity
            Intent intent = new Intent(MainActivity.this, PremiumActivity.class);
            startActivity(intent);
        });

        // --- Cargar los datos del usuario desde Firestore ---
        // Este es un paso crucial para obtener el estado premium y los intentos restantes
        loadUserData(currentUser.getUid());
    }

    /**
     * Carga los datos del usuario actual (premium status, free attempts) desde Firestore.
     * Si el usuario no existe en Firestore, crea un nuevo documento para él.
     * @param userId El UID del usuario autenticado por Firebase Auth.
     */
    private void loadUserData(String userId) {
        DocumentReference userRef = db.collection("users").document(userId);

        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // El documento del usuario existe en Firestore, lo mapeamos a nuestro objeto User
                        User user = document.toObject(User.class);
                        if (user != null) {
                            isPremiumUser = user.isPremium();
                            freeAttempts = user.getFreeAttemptsRemaining();
                            updateUI(); // Actualiza la UI con los datos cargados
                            Log.d(TAG, "Datos de usuario cargados: Email=" + user.getEmail() +
                                    ", Premium=" + user.isPremium() + ", Intentos=" + user.getFreeAttemptsRemaining());
                        }
                    } else {
                        // El documento del usuario NO existe, es la primera vez que se loguea.
                        // Creamos un nuevo documento en Firestore para él.
                        Log.d(TAG, "No se encontró el documento de usuario, creando uno nuevo.");
                        createNewUserInFirestore(userId);
                    }
                } else {
                    // Manejo de errores al intentar obtener el documento
                    Log.e(TAG, "Error al obtener documento de usuario: ", task.getException());
                    Toast.makeText(MainActivity.this, "Error al cargar datos del usuario. Inténtalo de nuevo.", Toast.LENGTH_LONG).show();
                    // Opcional: Podrías forzar el cierre de sesión o inhabilitar funciones si hay un error grave.
                }
            }
        });
    }

    /**
     * Crea un nuevo documento para un usuario en Firestore con valores por defecto.
     * (No premium, 5 intentos gratuitos).
     * @param userId El UID del usuario.
     */
    private void createNewUserInFirestore(String userId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Crea un nuevo objeto User con los datos básicos y el estado por defecto
            User newUser = new User(currentUser.getEmail(), currentUser.getDisplayName(), false, 5);
            db.collection("users").document(userId).set(newUser)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Nuevo usuario creado en Firestore con ID: " + userId);
                            // Actualiza las variables de estado locales con los valores por defecto
                            isPremiumUser = newUser.isPremium();
                            freeAttempts = newUser.getFreeAttemptsRemaining();
                            updateUI(); // Refresca la UI
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Error al crear nuevo usuario en Firestore: ", e);
                            Toast.makeText(MainActivity.this, "Error al guardar los datos iniciales del usuario.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    /**
     * Actualiza la interfaz de usuario (contador de intentos, visibilidad del botón premium)
     * basándose en el estado actual de 'isPremiumUser' y 'freeAttempts'.
     */
    private void updateUI() {
        if (isPremiumUser) {
            tvAttemptsCounter.setText("¡Usuario Premium!");
            btnGoPremium.setVisibility(View.GONE); // Oculta el botón premium si ya es premium
            btnShorten.setEnabled(true); // El botón de acortar siempre está habilitado para premium
        } else {
            tvAttemptsCounter.setText("Intentos Gratis: " + freeAttempts);
            btnGoPremium.setVisibility(View.VISIBLE); // Muestra el botón premium
            // Habilita/deshabilita el botón de acortar según los intentos restantes
            btnShorten.setEnabled(freeAttempts > 0);
            if (freeAttempts <= 0) {
                // Si no hay intentos, muestra un mensaje y deshabilita el EditText de la URL también (opcional)
                Toast.makeText(this, "Has agotado tus intentos gratuitos. ¡Hazte Premium!", Toast.LENGTH_LONG).show();
                etUrl.setEnabled(false); // Opcional: deshabilita la entrada de URL
            } else {
                etUrl.setEnabled(true); // Asegúrate de que esté habilitada si hay intentos
            }
        }
    }

    /**
     * Cierra la sesión del usuario actual de Firebase y redirige a la LoginActivity.
     */
    private void signOut() {
        mAuth.signOut(); // Cierra la sesión de Firebase Authentication

        // Crea un Intent para la LoginActivity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        // Estas banderas son cruciales para limpiar la pila de actividades:
        // FLAG_ACTIVITY_NEW_TASK: Inicia la actividad en una nueva tarea.
        // FLAG_ACTIVITY_CLEAR_TASK: Borra todas las actividades anteriores en la tarea.
        // Esto previene que el usuario regrese a MainActivity usando el botón de retroceso.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finaliza MainActivity
    }

    /**
     * Lógica para acortar la URL.
     * Verifica si el usuario es premium o si tiene intentos gratuitos restantes.
     * @param originalUrl La URL que el usuario desea acortar.
     */
    private void shortenUrl(String originalUrl) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Por favor, inicia sesión para usar el acortador.", Toast.LENGTH_SHORT).show();
            // Esto es una medida de seguridad, ya que la verificación inicial de login debería haberlo prevenido.
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Mostrar un estado de carga en el botón
        btnShorten.setEnabled(false);
        btnShorten.setText("Acortando...");

        if (isPremiumUser) {
            // Si es usuario premium, llama directamente a la API sin restricciones de contador
            performShortenApiCall(originalUrl, currentUser.getUid());
        } else {
            // Si no es premium, verifica si tiene intentos gratuitos restantes
            if (freeAttempts > 0) {
                performShortenApiCall(originalUrl, currentUser.getUid());
            } else {
                // Si no tiene intentos, restaura el botón y muestra un mensaje
                btnShorten.setEnabled(true);
                btnShorten.setText("Acortar URL");
                Toast.makeText(this, "Has agotado tus intentos gratuitos. ¡Hazte Premium!", Toast.LENGTH_LONG).show();
                etUrl.setEnabled(false); // Opcional: deshabilita la entrada de URL
            }
        }
    }

    /**
     * Realiza la llamada a la API de acortamiento de URLs.
     * @param originalUrl La URL a acortar.
     * @param userId El UID del usuario (necesario para actualizar Firestore si no es premium).
     */
    private void performShortenApiCall(String originalUrl, String userId) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ShortenResponse> call = apiService.shortenUrl(new ShortenRequest(originalUrl));

        call.enqueue(new Callback<ShortenResponse>() {
            @Override
            public void onResponse(Call<ShortenResponse> call, Response<ShortenResponse> response) {
                // Restaura el estado del botón después de la respuesta de la API
                btnShorten.setEnabled(true);
                btnShorten.setText("Acortar URL");

                if (response.isSuccessful() && response.body() != null) {
                    currentShortUrl = response.body().getShortUrl();
                    tvResult.setText("URL corta:\n" + currentShortUrl);
                    btnCopy.setVisibility(View.VISIBLE); // Muestra el botón de copiar

                    // Si el usuario NO es premium, disminuye el contador en Firestore
                    if (!isPremiumUser) {
                        updateFreeAttemptsInFirestore(userId);
                    }

                } else {
                    // Manejo de errores de la API (ej. URL inválida, error del servidor)
                    String errorMessage = "Error al acortar URL.";
                    if (response.errorBody() != null) {
                        try {
                            // Intenta leer el mensaje de error del cuerpo de la respuesta
                            errorMessage += " " + response.errorBody().string();
                        } catch (Exception e) {
                            Log.e(TAG, "Error leyendo errorBody: ", e);
                        }
                    } else if (response.message() != null && !response.message().isEmpty()) {
                        errorMessage += " " + response.message();
                    }
                    tvResult.setText(errorMessage);
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ShortenResponse> call, Throwable t) {
                // Manejo de errores de conexión (ej. sin internet, servidor no disponible)
                btnShorten.setEnabled(true);
                btnShorten.setText("Acortar URL");
                String errorMessage = "Error de conexión: " + t.getMessage();
                tvResult.setText(errorMessage);
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Fallo en la llamada a la API: ", t);
            }
        });
    }

    /**
     * Disminuye el contador de intentos gratuitos del usuario en Firestore.
     * Solo se llama si el usuario no es premium y aún tiene intentos.
     * @param userId El UID del usuario.
     */
    private void updateFreeAttemptsInFirestore(String userId) {
        // Solo actualiza si hay intentos restantes y el usuario no es premium
        if (freeAttempts > 0 && !isPremiumUser) {
            db.collection("users").document(userId)
                    // Usa FieldValue.increment() para asegurar que la actualización sea atómica y segura
                    // para múltiples escrituras si ocurrieran (aunque improbable en este caso).
                    .update("freeAttemptsRemaining", freeAttempts - 1)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Contador de intentos actualizado en Firestore.");
                            freeAttempts--; // Actualiza la variable local después de la escritura exitosa
                            updateUI(); // Refresca la UI para mostrar el nuevo contador
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Error al actualizar contador de intentos en Firestore: ", e);
                            Toast.makeText(MainActivity.this, "Error al guardar el conteo de intentos.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    /**
     * Copia la URL acortada actual al portapapeles.
     */
    private void copyToClipboard() {
        if (!currentShortUrl.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("URL corta", currentShortUrl);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "URL copiada al portapapeles", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No se pudo acceder al portapapeles.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cuando MainActivity vuelve al frente (por ejemplo, después de cerrar PremiumActivity),
        // recargamos los datos del usuario para asegurarnos de que el estado premium esté actualizado.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            loadUserData(currentUser.getUid());
        }
    }
}