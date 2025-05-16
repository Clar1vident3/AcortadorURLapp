package com.example.acortadorurlapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.acortadorurlapp.models.User; // Importa tu clase User
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference; // Importa DocumentReference
import com.google.firebase.firestore.DocumentSnapshot; // Importa DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore; // Importa FirebaseFirestore

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity"; // Para logs
    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore db; // Instancia de Firestore

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Inicializa Firestore

        // Configuración básica de Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Listener para el botón de login con Google
        findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> signIn());
    }

    // No necesitamos un createUserIfNotExists separado para Realtime Database.
    // La lógica de Firestore la manejaremos directamente en firebaseAuthWithGoogle.

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google sign in successful, ID Token: " + account.getIdToken());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In falló, maneja el error
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Error al iniciar sesión con Google.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Autentica con Firebase usando el ID Token de Google.
     * Después de la autenticación exitosa, verifica y/o crea el documento del usuario en Firestore.
     * @param idToken El ID Token obtenido de Google Sign-In.
     */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Autenticación con Firebase exitosa
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "Firebase Auth successful: " + user.getUid());
                            // Ahora, verifica si el documento del usuario existe en Firestore
                            checkAndCreateUserDocumentInFirestore(user);
                        } else {
                            Log.e(TAG, "Firebase user is null after successful authentication.");
                            Toast.makeText(LoginActivity.this, "Error: Usuario no encontrado.", Toast.LENGTH_SHORT).show();
                            // Redirigir a login si el usuario es nulo (caso inusual)
                            updateUI(null);
                        }
                    } else {
                        // Autenticación con Firebase fallida
                        Log.e(TAG, "Firebase Auth failed", task.getException());
                        Toast.makeText(LoginActivity.this, "Autenticación fallida con Firebase.", Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }

    /**
     * Verifica si el documento del usuario existe en Firestore. Si no existe, lo crea.
     * Una vez que el documento es verificado/creado, navega a MainActivity.
     * @param firebaseUser El objeto FirebaseUser autenticado.
     */
    private void checkAndCreateUserDocumentInFirestore(FirebaseUser firebaseUser) {
        DocumentReference userRef = db.collection("users").document(firebaseUser.getUid());

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (!document.exists()) {
                    // El documento del usuario NO existe, es la primera vez que se loguea.
                    // Creamos un nuevo documento en Firestore para él con valores por defecto.
                    Log.d(TAG, "No se encontró el documento de usuario en Firestore, creando uno nuevo.");

                    String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
                    String displayName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "";

                    User newUser = new User(email, displayName, false, 5); // false para no premium, 5 intentos gratis
                    userRef.set(newUser)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Nuevo documento de usuario creado en Firestore.");
                                navigateToMainActivity();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error al crear nuevo documento de usuario en Firestore: ", e);
                                Toast.makeText(LoginActivity.this, "Error al guardar datos iniciales del usuario.", Toast.LENGTH_LONG).show();
                                // Considerar cerrar sesión o permitir reintentar si el error es crítico.
                                updateUI(null); // Vuelve a la pantalla de login si falla la creación.
                            });
                } else {
                    // El documento del usuario YA existe en Firestore.
                    Log.d(TAG, "Documento de usuario ya existe en Firestore.");
                    navigateToMainActivity();
                }
            } else {
                // Manejo de errores al intentar obtener el documento
                Log.e(TAG, "Error al obtener documento de usuario de Firestore: ", task.getException());
                Toast.makeText(LoginActivity.this, "Error al cargar datos del usuario. Inténtalo de nuevo.", Toast.LENGTH_LONG).show();
                updateUI(null); // Vuelve a la pantalla de login si falla la carga.
            }
        });
    }

    /**
     * Navega a la MainActivity y finaliza la LoginActivity.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        // Estas banderas son importantes para limpiar la pila de actividades
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Cierra LoginActivity para que el usuario no pueda volver con el botón de retroceso
    }

    /**
     * Actualiza la UI (en este caso, redirige al login si el usuario es nulo).
     * Podrías usar esto para mostrar/ocultar progress bars, mensajes, etc.
     * @param user El usuario de Firebase (nulo si no hay sesión).
     */
    private void updateUI(FirebaseUser user) {
        if (user == null) {
            // Podrías mostrar un mensaje de error o habilitar el botón de login si ya lo habías deshabilitado.
            // Actualmente, solo muestra un Toast, la redirección ya ocurre si la autenticación falla.
        }
        // Si el usuario es nulo, significa que algo falló y no debería navegar a MainActivity
    }
}