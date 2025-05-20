package com.example.acortadorurlapp;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UrlsAdapter extends RecyclerView.Adapter<UrlsAdapter.UrlViewHolder> {

    private List<ShortenResponse> urls;
    private OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(String shortCode);
    }

    public UrlsAdapter(List<ShortenResponse> urls, OnDeleteClickListener deleteListener) {
        this.urls = urls;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public UrlViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_url, parent, false);
        return new UrlViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UrlViewHolder holder, int position) {
        ShortenResponse url = urls.get(position);

        holder.tvOriginalUrl.setText(url.getOriginalUrl());
        holder.tvShortUrl.setText(url.getShortUrl());
        holder.tvClicks.setText("Clicks: " + url.getClicks());

        holder.btnDelete.setOnClickListener(v -> {
            // Extraer el shortCode de la URL corta
            String[] parts = url.getShortUrl().split("/");
            String shortCode = parts[parts.length - 1];

            if (deleteListener != null) {
                deleteListener.onDeleteClick(shortCode);
            }
        });
    }

    @Override
    public int getItemCount() {
        return urls.size();
    }

    public void removeItem(String shortCode) {
        if (shortCode == null) {
            Log.e("Adapter", "ShortCode es null");
            return;
        }

        for (int i = 0; i < urls.size(); i++) {
            ShortenResponse item = urls.get(i);
            if (item != null && item.getShortUrl() != null) {
                String[] parts = item.getShortUrl().split("/");
                String codeFromUrl = parts.length > 0 ? parts[parts.length - 1] : "";

                if (shortCode.equals(codeFromUrl)) {
                    urls.remove(i);
                    notifyItemRemoved(i);
                    break;
                }
            }
        }
    }

    static class UrlViewHolder extends RecyclerView.ViewHolder {
        TextView tvOriginalUrl, tvShortUrl, tvClicks;
        Button btnDelete;

        public UrlViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOriginalUrl = itemView.findViewById(R.id.tvOriginalUrl);
            tvShortUrl = itemView.findViewById(R.id.tvShortUrl);
            tvClicks = itemView.findViewById(R.id.tvClicks);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}