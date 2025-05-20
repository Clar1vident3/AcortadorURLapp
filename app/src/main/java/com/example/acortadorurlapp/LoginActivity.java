package com.example.acortadorurlapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.acortadorurlapp.models.User;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configuración básica de Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Listener para el botón de login con Google
        findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> {
            // Llama al nuevo método para iniciar sesión, que primero cierra la sesión
            signInAndForceAccountSelection();
        });
    }

    // --- Nuevo método para forzar la selección de cuenta ---
    private void signInAndForceAccountSelection() {
        // Cierra cualquier sesión de Google que pueda existir previamente.
        // Esto fuerza a la UI de Google a mostrar el selector de cuentas
        // o a pedir al usuario que inicie sesión si no hay cuentas en el dispositivo.
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Log.d(TAG, "Google Sign-Out complete before new sign-in attempt.");
            // Ahora que la sesión está cerrada, lanza el intento de inicio de sesión.
            // Esto mostrará la pantalla de selección de cuenta o de inicio de sesión.
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }
    // ----------------------------------------------------

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
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Error al iniciar sesión con Google.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "Firebase Auth successful: " + user.getUid());
                            checkAndCreateUserDocumentInFirestore(user);
                        } else {
                            Log.e(TAG, "Firebase user is null after successful authentication.");
                            Toast.makeText(LoginActivity.this, "Error: Usuario no encontrado.", Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    } else {
                        Log.e(TAG, "Firebase Auth failed", task.getException());
                        Toast.makeText(LoginActivity.this, "Autenticación fallida con Firebase.", Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }

    private void checkAndCreateUserDocumentInFirestore(FirebaseUser firebaseUser) {
        DocumentReference userRef = db.collection("users").document(firebaseUser.getUid());

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (!document.exists()) {
                    Log.d(TAG, "No se encontró el documento de usuario en Firestore, creando uno nuevo.");

                    String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
                    String displayName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "";

                    User newUser = new User(email, displayName, false, 5);
                    userRef.set(newUser)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Nuevo documento de usuario creado en Firestore.");
                                navigateToMainActivity();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error al crear nuevo documento de usuario en Firestore: ", e);
                                Toast.makeText(LoginActivity.this, "Error al guardar datos iniciales del usuario.", Toast.LENGTH_LONG).show();
                                updateUI(null);
                            });
                } else {
                    Log.d(TAG, "Documento de usuario ya existe en Firestore.");
                    navigateToMainActivity();
                }
            } else {
                Log.e(TAG, "Error al obtener documento de usuario de Firestore: ", task.getException());
                Toast.makeText(LoginActivity.this, "Error al cargar datos del usuario. Inténtalo de nuevo.", Toast.LENGTH_LONG).show();
                updateUI(null);
            }
        });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateUI(FirebaseUser user) {
        // No hay cambios aquí, ya que el comportamiento de UI se maneja con redirecciones/Toasts
    }
}