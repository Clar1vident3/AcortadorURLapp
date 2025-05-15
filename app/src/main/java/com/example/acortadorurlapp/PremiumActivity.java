// Archivo: app/src/main/java/com/example/acortadorurlapp/PremiumActivity.java
package com.example.acortadorurlapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class PremiumActivity extends AppCompatActivity {

    private static final String TAG = "PremiumActivity";

    private EditText etCardNumber, etExpiryDate, etCvv, etCardHolderName;
    private Button btnSubscribe;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium);

        // Inicializar instancias de Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Inicializar vistas
        etCardNumber = findViewById(R.id.etCardNumber);
        etExpiryDate = findViewById(R.id.etExpiryDate);
        etCvv = findViewById(R.id.etCvv);
        etCardHolderName = findViewById(R.id.etCardHolderName);
        btnSubscribe = findViewById(R.id.btnSubscribe);

        // Listener para el botón de suscripción
        btnSubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Aquí NO procesamos pagos reales, solo simulamos la suscripción
                String cardNumber = etCardNumber.getText().toString();
                String expiryDate = etExpiryDate.getText().toString();
                String cvv = etCvv.getText().toString();
                String cardHolderName = etCardHolderName.getText().toString();

                // Validaciones básicas de campos
                if (cardNumber.isEmpty() || expiryDate.isEmpty() || cvv.isEmpty() || cardHolderName.isEmpty()) {
                    Toast.makeText(PremiumActivity.this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
                } else if (cardNumber.length() != 16) {
                    Toast.makeText(PremiumActivity.this, "Número de tarjeta inválido (16 dígitos)", Toast.LENGTH_SHORT).show();
                } else if (expiryDate.length() != 5 || !expiryDate.contains("/")) {
                    Toast.makeText(PremiumActivity.this, "Formato de fecha de vencimiento inválido (MM/AA)", Toast.LENGTH_SHORT).show();
                } else if (cvv.length() != 3) {
                    Toast.makeText(PremiumActivity.this, "CVV inválido (3 dígitos)", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(PremiumActivity.this, "Procesando suscripción (simulada)...", Toast.LENGTH_SHORT).show();
                    // Llama al método para actualizar el estado del usuario en Firestore
                    updateUserToPremium();
                }
            }
        });
    }

    /**
     * Actualiza el campo 'isPremium' del usuario actual en Firestore a true.
     */
    private void updateUserToPremium() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .update("isPremium", true) // Actualiza el campo 'isPremium'
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Usuario actualizado a premium en Firestore.");
                            Toast.makeText(PremiumActivity.this, "¡Felicidades! Ahora eres Premium.", Toast.LENGTH_LONG).show();
                            finish(); // Cierra esta actividad y regresa a MainActivity
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Error al actualizar usuario a premium: ", e);
                            Toast.makeText(PremiumActivity.this, "Error al procesar suscripción. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "No se encontró un usuario logueado.", Toast.LENGTH_SHORT).show();
            // Esto no debería pasar si la app está bien estructurada, pero es buena práctica verificar.
            finish(); // Cierra la actividad si no hay usuario logueado.
        }
    }
}