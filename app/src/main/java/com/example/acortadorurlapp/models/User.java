// Archivo: app/src/main/java/com/example/acortadorurlapp/models/User.java
package com.example.acortadorurlapp.models;

public class User {
    private String email;
    private String displayName;
    private boolean premium;
    private int freeAttemptsRemaining;

    // Constructor vacío requerido por Firestore para deserialización
    public User() {

        this.email = "";
        this.displayName = "";
        this.premium = false; // <--- CAMBIO AQUÍ
        this.freeAttemptsRemaining = 0;
    }

    // Constructor para cuando creas un nuevo usuario por primera vez
    public User(String email, String displayName, boolean premium, int freeAttemptsRemaining) { // <--- CAMBIO AQUÍ
        this.email = email;
        this.displayName = displayName;
        this.premium = premium; // <--- CAMBIO AQUÍ
        this.freeAttemptsRemaining = freeAttemptsRemaining;
    }

    // --- Getters ---
    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    // IMPORTANTE: Mantén el getter como isPremium()
    // Firestore buscará 'isPremium' en el documento y lo mapeará a este getter
    public boolean isPremium() {
        return premium; // <--- Retorna el nuevo campo 'premium'
    }

    public int getFreeAttemptsRemaining() {
        return freeAttemptsRemaining;
    }

    // --- Setters ---
    public void setEmail(String email) {
        this.email = email;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }


    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public void setFreeAttemptsRemaining(int freeAttemptsRemaining) {
        this.freeAttemptsRemaining = freeAttemptsRemaining;
    }
}