package com.example.acortadorurlapp;

import com.google.gson.annotations.SerializedName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ShortenResponse {
    @SerializedName("original_url")
    private String originalUrl;
    @SerializedName("short_url")
    private String shortUrl;
    @SerializedName("short_code")
    private String shortCode;
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("clicks")
    private int clicks;

    // Getters
    public String getOriginalUrl() { return originalUrl; }
    public String getShortUrl() { return shortUrl; }
    public String getShortCode() { return shortCode; }
    public String getCreatedAt() { return createdAt; }
    public int getClicks() { return clicks; }


    // Metodo para formatear la fecha
    public String getFormattedDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(createdAt);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return createdAt; // Aquí si el formato falla, devolver el original
        }
    }
}