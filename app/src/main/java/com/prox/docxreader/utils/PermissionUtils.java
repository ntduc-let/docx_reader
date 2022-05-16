package com.prox.docxreader.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.prox.docxreader.BuildConfig;
import com.prox.docxreader.R;

public class PermissionUtils {
    public static final int REQUEST_PERMISSION_MANAGE = 10;
    public static final int REQUEST_PERMISSION_READ_WRITE = 11;

    private static AlertDialog dialogRequest;

    public static boolean permission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
            return (write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED);
        }
    }

    public static void requestPermissions(Context context, Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            openDialogAccessAllFile(context, activity);
        } else {
            String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            activity.requestPermissions(permissions, REQUEST_PERMISSION_READ_WRITE);
        }
    }

    public static void openDialogAccessAllFile(Context context, Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.dialog_message)
                .setTitle(R.string.dialog_title);

        builder.setPositiveButton(R.string.txt_ok, (dialog, id) -> requestAccessAllFile(activity));
        builder.setNegativeButton(R.string.txt_cancel, (dialog, id) -> {
            FirebaseUtils.sendEventRequestPermission(context);
            activity.finish();
        });

        dialogRequest = builder.create();
        dialogRequest.show();
    }

    public static void requestAccessAllFile(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                activity.startActivityForResult(intent, REQUEST_PERMISSION_MANAGE);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activity.startActivityForResult(intent, REQUEST_PERMISSION_MANAGE);
            }
        } else {
            try {
                Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri);
                activity.startActivityForResult(intent, REQUEST_PERMISSION_READ_WRITE);
            } catch (Exception e) {

            }
        }
    }

    public static void cancelDialogAccessAllFile() {
        if (dialogRequest != null && dialogRequest.isShowing()) {
            dialogRequest.cancel();
        }
    }
}