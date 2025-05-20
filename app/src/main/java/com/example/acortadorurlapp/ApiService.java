package com.example.acortadorurlapp;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @POST("/api/shorten")
    Call<ShortenResponse> shortenUrl(@Body ShortenRequest request);

    @GET("/api/urls")
    Call<List<ShortenResponse>> getUrls();

    @DELETE("/api/urls/{shortCode}")
    Call<Void> deleteUrl(@Path("shortCode") String shortCode);

}
