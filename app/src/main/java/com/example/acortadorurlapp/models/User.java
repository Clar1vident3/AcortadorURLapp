// Archivo: app/src/main/java/com/example/acortadorurlapp/models/User.java
package com.example.acortadorurlapp.models;

public class User {
    private String email;
    private String displayName;
    private boolean isPremium;
    private int freeAttemptsRemaining;

    public User() {
        // Constructor vacío requerido por Firestore para deserialización
    }

    public User(String email, String displayName, boolean isPremium, int freeAttemptsRemaining) {
        this.email = email;
        this.displayName = displayName;
        this.isPremium = isPremium;
        this.freeAttemptsRemaining = freeAttemptsRemaining;
    }

    // Getters
    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public int getFreeAttemptsRemaining() {
        return freeAttemptsRemaining;
    }

    // Setters
    public void setEmail(String email) {
        this.email = email;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setPremium(boolean premium) {
        isPremium = premium;
    }

    public void setFreeAttemptsRemaining(int freeAttemptsRemaining) {
        this.freeAttemptsRemaining = freeAttemptsRemaining;
    }
}