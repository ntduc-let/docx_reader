package com.prox.docxreader.ui.dialog;

import static com.prox.docxreader.repository.DocumentRepository.SORT_NAME;
import static com.prox.docxreader.repository.DocumentRepository.SORT_TIME_ACCESS;
import static com.prox.docxreader.repository.DocumentRepository.SORT_TIME_CREATE;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.prox.docxreader.databinding.DialogSortBinding;
import com.prox.docxreader.interfaces.OnSelectSortListener;
import com.prox.docxreader.ui.fragment.FavoriteFragment;
import com.prox.docxreader.ui.fragment.HomeFragment;

public class SortDialog extends Dialog {

    public SortDialog(@NonNull Context context,
                      DialogSortBinding binding,
                      boolean isFavorite,
                      int typeSort,
                      OnSelectSortListener onSelectSortListener) {
        super(context);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(binding.getRoot());
        setCancelable(true);

        getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.gravity = Gravity.TOP|Gravity.END;
        layoutParams.y = 128;
        layoutParams.x = 16;
        getWindow().setAttributes(layoutParams);

        switch (typeSort){
            case SORT_NAME:
                binding.nameChecked.setVisibility(View.VISIBLE);
                binding.timeCreateChecked.setVisibility(View.INVISIBLE);
                binding.timeAccessChecked.setVisibility(View.INVISIBLE);
                break;
            case SORT_TIME_CREATE:
                binding.nameChecked.setVisibility(View.INVISIBLE);
                binding.timeCreateChecked.setVisibility(View.VISIBLE);
                binding.timeAccessChecked.setVisibility(View.INVISIBLE);
                break;
            case SORT_TIME_ACCESS:
                binding.nameChecked.setVisibility(View.INVISIBLE);
                binding.timeCreateChecked.setVisibility(View.INVISIBLE);
                binding.timeAccessChecked.setVisibility(View.VISIBLE);
                break;
        }

        binding.sortName.setOnClickListener(v -> {
            if (!isFavorite){
                HomeFragment.typeSort = SORT_NAME;
            }else {
                FavoriteFragment.typeSort = SORT_NAME;
            }
            onSelectSortListener.onSelectSort();
            cancel();
        });
        binding.sortTimeCreate.setOnClickListener(v -> {
            if (!isFavorite){
                HomeFragment.typeSort = SORT_TIME_CREATE;
            }else {
                FavoriteFragment.typeSort = SORT_TIME_CREATE;
            }
            onSelectSortListener.onSelectSort();
            cancel();
        });
        binding.sortTimeAccess.setOnClickListener(v -> {
            if (!isFavorite){
                HomeFragment.typeSort = SORT_TIME_ACCESS;
            }else {
                FavoriteFragment.typeSort = SORT_TIME_ACCESS;
            }
            onSelectSortListener.onSelectSort();
            cancel();
        });
    }
}
