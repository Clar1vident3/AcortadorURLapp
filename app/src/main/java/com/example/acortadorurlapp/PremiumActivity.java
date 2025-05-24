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

import java.util.Calendar;

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
                String cardNumber = etCardNumber.getText().toString().trim();
                String expiryDate = etExpiryDate.getText().toString().trim();
                String cvv = etCvv.getText().toString().trim();
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

                if (!isValidCardNumber(cardNumber)) {
                    etCardNumber.setError("Número de tarjeta inválido");
                    return;
                }

                if (!isValidExpiryDate(expiryDate)) {
                    etExpiryDate.setError("Fecha de vencimiento inválida (MM/AA) o tarjeta vencida");
                    return;
                }

                if (!isValidCvv(cvv)) {
                    etCvv.setError("CVV inválido (3 o 4 dígitos)");
                    return;
                }

                if (!isValidCardHolderName(cardHolderName)) {
                    etCardHolderName.setError("Nombre inválido (solo letras y espacios)");
                    return;
                }

                // Si pasa todas las validaciones
                Toast.makeText(PremiumActivity.this, "Procesando suscripción (simulada)...", Toast.LENGTH_SHORT).show();
                updateUserToPremium();

            }
        });
    }

    /**
     * Actualiza el campo 'premium' del usuario actual en Firestore a true.
     */
    private void updateUserToPremium() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .update("premium", true) // Actualiza el campo 'isPremium'
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

    // Metodo para validar el número de tarjeta usando el algoritmo de Luhn
    private boolean isValidCardNumber(String cardNumber) {
        String cleanedNumber = cardNumber.replaceAll("[^0-9]", "");

        // Verificar longitud básica, 16 digitos
        if (cleanedNumber.length() != 16) {
            return false;
        }

        // Algoritmo de Luhn
        int sum = 0;
        boolean alternate = false;
        for (int i = cleanedNumber.length() - 1; i >= 0; i--) {
            int digit = Integer.parseInt(cleanedNumber.substring(i, i + 1));
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            sum += digit;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    // Metodo para validar fecha de expiración (MM/AA)
    private boolean isValidExpiryDate(String expiryDate) {
        if (!expiryDate.matches("^(0[1-9]|1[0-2])/?([0-9]{2})$")) {
            return false;
        }

        String[] parts = expiryDate.split("/");
        int month = Integer.parseInt(parts[0]);
        int year = Integer.parseInt(parts[1]);

        // Obtener año y mes actual
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR) % 100;
        int currentMonth = calendar.get(Calendar.MONTH) + 1;

        // Validar que no esté vencida
        if (year < currentYear || (year == currentYear && month < currentMonth)) {
            return false;
        }

        return true;
    }

    // Metodo para validar CVV
    private boolean isValidCvv(String cvv) {
        return cvv.matches("^[0-9]{3,4}$");
    }

    // Metodo para validar nombre del titular
    private boolean isValidCardHolderName(String name) {
    return name.matches("^[a-zA-Z\\s]{3,}$");
    }

}