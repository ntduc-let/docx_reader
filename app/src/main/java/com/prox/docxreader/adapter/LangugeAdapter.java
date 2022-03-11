package com.prox.docxreader.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.prox.docxreader.OnClickLanguageListener;
import com.prox.docxreader.R;

import java.util.Locale;

public class LangugeAdapter extends RecyclerView.Adapter<LangugeAdapter.LanguageViewHolder> {
    private final String[] languages;
    private final String[] typeLanguages;
    private ImageView imgChecked;
    private final OnClickLanguageListener onClickLanguageListener;

    public LangugeAdapter(String[] languages, String[] typeLanguages, OnClickLanguageListener onClickLanguageListener){
        this.languages = languages;
        this.typeLanguages = typeLanguages;
        this.onClickLanguageListener = onClickLanguageListener;
    }

    @NonNull
    @Override
    public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_language, parent, false);
        return new LanguageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
        holder.txtLanguage.setText(languages[position]);
        if (Locale.getDefault().getLanguage().contains(typeLanguages[position])){
            holder.imgChecked.setVisibility(View.VISIBLE);
            imgChecked = holder.imgChecked;
        }else{
            holder.imgChecked.setVisibility(View.INVISIBLE);
        }

        holder.itemLanguage.setOnClickListener(v -> {
            imgChecked.setVisibility(View.INVISIBLE);
            holder.imgChecked.setVisibility(View.VISIBLE);
            imgChecked = holder.imgChecked;

            onClickLanguageListener.onClickLanguage(typeLanguages[position]);
        });
    }

    @Override
    public int getItemCount() {
        return languages.length;
    }

    public static class LanguageViewHolder extends RecyclerView.ViewHolder{
        private final TextView txtLanguage;
        private final ImageView imgChecked;
        private final ConstraintLayout itemLanguage;

        public LanguageViewHolder(@NonNull View itemView) {
            super(itemView);
            txtLanguage = itemView.findViewById(R.id.txt_language);
            imgChecked = itemView.findViewById(R.id.img_checked);
            itemLanguage = itemView.findViewById(R.id.item_language);
        }
    }
}