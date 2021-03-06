package com.prox.docxreader.ui.activity;

import static com.prox.docxreader.ui.activity.SplashActivity.OPEN_OUTSIDE;
import static com.prox.docxreader.utils.PermissionUtils.CHECK_READER;
import static com.prox.docxreader.utils.PermissionUtils.PERMISSION_DENIED;
import static com.prox.docxreader.utils.PermissionUtils.PERMISSION_DENIED_NOT_SHOW;
import static com.prox.docxreader.utils.PermissionUtils.REQUEST_PERMISSION_MANAGE;
import static com.prox.docxreader.utils.PermissionUtils.REQUEST_PERMISSION_READ_WRITE;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.prox.docxreader.BuildConfig;
import com.prox.docxreader.DocxReaderApp;
import com.prox.docxreader.R;
import com.prox.docxreader.databinding.ActivityOfficeDetailBinding;
import com.prox.docxreader.utils.FirebaseUtils;
import com.prox.docxreader.utils.LanguageUtils;
import com.prox.docxreader.utils.NetworkUtils;
import com.prox.docxreader.utils.PermissionUtils;
import com.proxglobal.proxads.adsv2.callback.AdsCallback;
import com.proxglobal.purchase.ProxPurchase;
import com.proxglobal.rate.ProxRateDialog;
import com.proxglobal.rate.RatingDialogListener;
import com.wxiwei.office.constant.EventConstant;
import com.wxiwei.office.constant.MainConstant;
import com.wxiwei.office.constant.wp.WPViewConstant;
import com.wxiwei.office.officereader.AppFrame;
import com.wxiwei.office.officereader.FindToolBar;
import com.wxiwei.office.officereader.beans.AImageButton;
import com.wxiwei.office.officereader.beans.AImageCheckButton;
import com.wxiwei.office.officereader.beans.AToolsbar;
import com.wxiwei.office.officereader.beans.CalloutToolsbar;
import com.wxiwei.office.officereader.beans.PDFToolsbar;
import com.wxiwei.office.officereader.beans.PGToolsbar;
import com.wxiwei.office.officereader.beans.SSToolsbar;
import com.wxiwei.office.officereader.beans.WPToolsbar;
import com.wxiwei.office.officereader.database.DBService;
import com.wxiwei.office.res.ResKit;
import com.wxiwei.office.ss.sheetbar.SheetBar;
import com.wxiwei.office.system.FileKit;
import com.wxiwei.office.system.IControl;
import com.wxiwei.office.system.IMainFrame;
import com.wxiwei.office.system.MainControl;
import com.wxiwei.office.system.beans.pagelist.IPageListViewListener;
import com.wxiwei.office.system.dialog.ColorPickerDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReaderActivity extends AppCompatActivity implements IMainFrame {
    public static final String FILE_PATH = "FILE_PATH";

    private ActivityOfficeDetailBinding binding;
    private boolean isOpenOutside;
    private boolean isOpenned = false;

    private final Handler handler = new Handler();
    private final Runnable checkPermission = new Runnable() {
        @Override
        public void run() {
            if (PermissionUtils.permission(ReaderActivity.this)) {
                handler.removeCallbacks(this);
                if (PermissionUtils.typeCheck == CHECK_READER) {
                    Intent intent = new Intent(ReaderActivity.this, ReaderActivity.class);
                    intent.putExtra(FILE_PATH, filePath);
                    intent.putExtra(OPEN_OUTSIDE, true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            } else {
                handler.postDelayed(this, 1000);
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Load ng??n ng???
        LanguageUtils.loadLanguage(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(this.getResources().getColor(R.color.doc_color));

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        control = new MainControl(this);
        appFrame = new AppFrame(getApplicationContext());

        binding = ActivityOfficeDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (ProxPurchase.getInstance().checkPurchased() || !NetworkUtils.isNetworkAvailable(this)) {
            binding.bannerAds.setVisibility(View.GONE);
        }

        filePath = getIntent().getStringExtra(FILE_PATH);
        isOpenOutside = getIntent().getBooleanExtra(OPEN_OUTSIDE, true);

        if (filePath != null) {
            fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        }
        DocxReaderApp.instance.showBanner(this, binding.bannerAds, BuildConfig.banner, new AdsCallback() {
                    @Override
                    public void onShow() {
                        super.onShow();
                        Log.d("banner_ads", "onShow");
                    }

                    @Override
                    public void onClosed() {
                        super.onClosed();
                        Log.d("banner_ads", "onClosed");
                    }

                    @Override
                    public void onError() {
                        super.onError();
                        Log.d("banner_ads", "onError");
                    }
                }
        );

        ProxRateDialog.Config config = new ProxRateDialog.Config();
        config.setListener(new RatingDialogListener() {
            @Override
            public void onSubmitButtonClicked(int rate, String comment) {
                Log.d("rate_app", "onSubmitButtonClicked " + rate + comment);
                FirebaseUtils.sendEventSubmitRate(ReaderActivity.this, comment, rate);
            }

            @Override
            public void onLaterButtonClicked() {
                Log.d("rate_app", "onLaterButtonClicked");
                FirebaseUtils.sendEventLaterRate(ReaderActivity.this);
                if (isOpenOutside) {
                    Intent intent = new Intent(ReaderActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                finish();
            }

            @Override
            public void onChangeStar(int rate) {
                Log.d("rate_app", "onChangeStar " + rate);
                if (rate >= 4) {
                    FirebaseUtils.sendEventChangeRate(ReaderActivity.this, rate);

                    if (isOpenOutside) {
                        Intent intent = new Intent(ReaderActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                    finish();
                }
            }

            @Override
            public void onDone() {
                Log.d("rate_app", "onDone");

                if (isOpenOutside) {
                    Intent intent = new Intent(ReaderActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                finish();
            }
        });
        ProxRateDialog.init(config);

        binding.toolbarOffice.setNavigationIcon(R.drawable.ic_back_24);
        binding.toolbarOffice.setTitleTextAppearance(this, R.style.TitleToolBar);
        setSupportActionBar(binding.toolbarOffice);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.viewerOffice.removeAllViews();
        binding.viewerOffice.addView(appFrame);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        if (requestCode == REQUEST_PERMISSION_READ_WRITE) {
            FirebaseUtils.sendEventRequestPermission(this);

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                binding.viewerOffice.post(this::init);
            } else {
                if (shouldShowRequestPermissionRationale(permissions[0])
                        && shouldShowRequestPermissionRationale(permissions[1])) {
                    PermissionUtils.openDialogAccessAllFile(this, this, PERMISSION_DENIED);
                } else {
                    handler.post(checkPermission);
                    PermissionUtils.openDialogAccessAllFile(this, this, PERMISSION_DENIED_NOT_SHOW);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PERMISSION_MANAGE) {
            FirebaseUtils.sendEventRequestPermission(this);
            if (!PermissionUtils.permission(this)) {
                PermissionUtils.openDialogAccessAllFile(this, this, PERMISSION_DENIED_NOT_SHOW);
            }
        } else if (requestCode == REQUEST_PERMISSION_READ_WRITE) {
            FirebaseUtils.sendEventRequestPermission(this);
            if (!PermissionUtils.permission(this)) {
                PermissionUtils.openDialogAccessAllFile(this, this, PERMISSION_DENIED_NOT_SHOW);
            }
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_app_act,menu);
//        return super.onCreateOptionsMenu(menu);
//    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
//        } else if (itemId == R.id.share) {
//            shareToOther();
//            return true;
        }
        return super.onOptionsItemSelected(item);
    }
//    private void shareToOther() {
//        File file = new File(realPath);
//        Uri uri = FileProvider.getUriForFile(this, "com.prox.docxreader.fileprovider", file);
//
//        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
//
//        int dot = fileName.lastIndexOf('.');       //V??? tr?? d???u . cu???i c??ng
//        String type = fileName.substring(dot+1);         //??u??i file (docx ho???c doc)
//
//        intentShareFile.setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(type));
//        intentShareFile.putExtra(Intent.EXTRA_STREAM, uri);
//        intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//        Intent chooser = Intent.createChooser(intentShareFile, fileName);
//
//        List<ResolveInfo> resInfoList = this.getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);
//
//        for (ResolveInfo resolveInfo : resInfoList) {
//            String packageName = resolveInfo.activityInfo.packageName;
//            this.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        }
//
//        startActivity(chooser);
//    }

    public void setButtonEnabled(boolean enabled) {
        if (fullscreen) {
            pageUp.setEnabled(enabled);
            pageDown.setEnabled(enabled);
            penButton.setEnabled(enabled);
            eraserButton.setEnabled(enabled);
            settingsButton.setEnabled(enabled);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (PermissionUtils.permission(this)) {
            if (!isOpenned) {
                isOpenned = true;
                binding.viewerOffice.post(this::init);
            }
        } else {
            PermissionUtils.typeCheck = CHECK_READER;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                handler.post(checkPermission);
            }
            PermissionUtils.requestPermissions(this, this);
        }
    }

    protected void onPause() {
        super.onPause();

        Object obj = control.getActionValue(EventConstant.PG_SLIDESHOW, null);
        if (obj != null && (Boolean) obj) {
            wm.removeView(pageUp);
            wm.removeView(pageDown);
            wm.removeView(penButton);
            wm.removeView(eraserButton);
            wm.removeView(settingsButton);
        }
    }

    protected void onResume() {
        super.onResume();
        Object obj = control.getActionValue(EventConstant.PG_SLIDESHOW, null);
        if (obj != null && (Boolean) obj) {
            wmParams.gravity = Gravity.RIGHT | Gravity.TOP;
            wmParams.x = MainConstant.GAP;
            wm.addView(penButton, wmParams);

            wmParams.gravity = Gravity.RIGHT | Gravity.TOP;
            wmParams.x = MainConstant.GAP;
            wmParams.y = wmParams.height;
            wm.addView(eraserButton, wmParams);

            wmParams.gravity = Gravity.RIGHT | Gravity.TOP;
            wmParams.x = MainConstant.GAP;
            wmParams.y = wmParams.height * 2;
            wm.addView(settingsButton, wmParams);

            wmParams.gravity = Gravity.LEFT | Gravity.CENTER;
            wmParams.x = MainConstant.GAP;
            wmParams.y = 0;
            wm.addView(pageUp, wmParams);

            wmParams.gravity = Gravity.RIGHT | Gravity.CENTER;
            wm.addView(pageDown, wmParams);
        }
    }

    /**
     *
     */

    @Override
    public void onBackPressed() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int closeReader = preferences.getInt("close_reader", 1);
        Log.d("close_reader", String.valueOf(closeReader));
//        if (closeReader % 2 == 0 && closeReader % 3 == 0) {
//            preferences.edit().putInt("close_reader", closeReader + 1).apply();
//            DocxReaderApp.instance.showInterstitial(this, "insite", new AdsCallback() {
//                @Override
//                public void onClosed() {
//                    super.onClosed();
//                    Log.d("interstitial_global", "onClosed");
//                    SharedPreferences sp = getSharedPreferences("prox", Context.MODE_PRIVATE);
//                    if (sp.getBoolean("isRated", false)){
//                        if (isOpenOutside){
//                            Intent intent = new Intent(ReaderActivity.this, MainActivity.class);
//                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                            startActivity(intent);
//                        }
//                        finish();
//                    }else {
//                        ProxRateDialog.showIfNeed(ReaderActivity.this, getSupportFragmentManager());
//                    }
//                }
//
//                @Override
//                public void onError() {
//                    super.onError();
//                    Log.d("interstitial_global", "onError");
//                    SharedPreferences sp = getSharedPreferences("prox", Context.MODE_PRIVATE);
//                    if (sp.getBoolean("isRated", false)){
//                        if (isOpenOutside){
//                            Intent intent = new Intent(ReaderActivity.this, MainActivity.class);
//                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                            startActivity(intent);
//                        }
//                        finish();
//                    }else {
//                        ProxRateDialog.showIfNeed(ReaderActivity.this, getSupportFragmentManager());
//                    }
//
//                }
//            });
//        } else
        if (closeReader % 2 == 0) {
            preferences.edit().putInt("close_reader", closeReader + 1).apply();
            SharedPreferences sp = getSharedPreferences("prox", Context.MODE_PRIVATE);
            if (sp.getBoolean("isRated", false)) {
                if (isOpenOutside) {
                    Intent intent = new Intent(ReaderActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                finish();
            } else {
                ProxRateDialog.showIfNeed(ReaderActivity.this, getSupportFragmentManager());
            }
//        } else if (closeReader % 3 == 0) {
//            preferences.edit().putInt("close_reader", closeReader + 1).apply();
//            DocxReaderApp.instance.showInterstitial(this, "insite", new AdsCallback() {
//                @Override
//                public void onClosed() {
//                    super.onClosed();
//                    Log.d("interstitial_global", "onClosed");
//                    if (isOpenOutside){
//                        Intent intent = new Intent(ReaderActivity.this, MainActivity.class);
//                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                        startActivity(intent);
//                    }
//                    finish();
//                }
//
//                @Override
//                public void onError() {
//                    super.onError();
//                    Log.d("interstitial_global", "onError");
//                    if (isOpenOutside){
//                        Intent intent = new Intent(ReaderActivity.this, MainActivity.class);
//                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                        startActivity(intent);
//                    }
//                    finish();
//                }
//            });
        } else {
            preferences.edit().putInt("close_reader", closeReader + 1).apply();
            if (isOpenOutside) {
                Intent intent = new Intent(ReaderActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            finish();
        }
    }

//    public void onBackPressed() {
//        if (isSearchbarActive()) {
//            showSearchBar(false);
//            updateToolsbarStatus();
//        } else {
//            Object obj = control.getActionValue(EventConstant.PG_SLIDESHOW, null);
//            if (obj != null && (Boolean) obj) {
//                fullScreen(false);
//                //
//                this.control.actionEvent(EventConstant.PG_SLIDESHOW_END, null);
//            } else {
//                if (control.getReader() != null) {
//                    control.getReader().abortReader();
//                }
//                if (dbService != null) {
//                    if (marked != dbService.queryItem(MainConstant.TABLE_STAR, filePath)) {
//                        if (!marked) {
//                            dbService.deleteItem(MainConstant.TABLE_STAR, filePath);
//                        } else {
//                            dbService.insertStarFiles(MainConstant.TABLE_STAR, filePath);
//                        }
//
//                        Intent intent = new Intent();
//                        intent.putExtra(MainConstant.INTENT_FILED_MARK_STATUS, marked);
//                        setResult(RESULT_OK, intent);
//                    }
//                }
//            }
//        }
//    }

    /**
     *
     */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (isSearchbarActive()) {
            searchBar.onConfigurationChanged(newConfig);
        }
    }

    /**
     *
     */
    protected void onDestroy() {
        dispose();
        handler.removeCallbacks(checkPermission);
        super.onDestroy();
    }

    /**
     * (non-Javadoc)
     *
     * @see IMainFrame#showProgressBar(boolean)
     */
    public void showProgressBar(boolean visible) {
        setProgressBarIndeterminateVisibility(visible);
    }

    /**
     *
     */

    private void init() {
        if (dbService != null) {
            dbService = new DBService(getApplicationContext());
        }
        setTitle(fileName);

        boolean isSupport = FileKit.instance().isSupport(filePath);
        //?????????????????????
        if (isSupport && dbService != null) {
            dbService.insertRecentFiles(MainConstant.TABLE_RECENT, filePath);
        }
        // create view
        createView();
        // open file
        control.openFile(filePath);
        // initialization marked
        initMarked();
    }

    /**
     * true: show message when zooming
     * false: not show message when zooming
     *
     * @return
     */
    public boolean isShowZoomingMsg() {
        return false;
    }

    /**
     * true: pop up dialog when throw err
     * false: not pop up dialog when throw err
     *
     * @return
     */
    public boolean isPopUpErrorDlg() {
        return false;
    }

    @Override
    public void newFatalOccurs(String fatalDetail) {

    }

    @Override
    public void outOfMemoryOccurs() {

    }

    /**
     *
     */
    private void createView() {
        // word
        String file = filePath.toLowerCase();
        if (file.endsWith(MainConstant.FILE_TYPE_DOC) || file.endsWith(MainConstant.FILE_TYPE_DOCX)
                || file.endsWith(MainConstant.FILE_TYPE_TXT)
                || file.endsWith(MainConstant.FILE_TYPE_DOT)
                || file.endsWith(MainConstant.FILE_TYPE_DOTX)
                || file.endsWith(MainConstant.FILE_TYPE_DOTM)) {
            applicationType = MainConstant.APPLICATION_TYPE_WP;
            toolsbar = new WPToolsbar(getApplicationContext(), control);
        }
        // excel
        else if (file.endsWith(MainConstant.FILE_TYPE_XLS)
                || file.endsWith(MainConstant.FILE_TYPE_XLSX)
                || file.endsWith(MainConstant.FILE_TYPE_XLT)
                || file.endsWith(MainConstant.FILE_TYPE_XLTX)
                || file.endsWith(MainConstant.FILE_TYPE_XLTM)
                || file.endsWith(MainConstant.FILE_TYPE_XLSM)) {
            applicationType = MainConstant.APPLICATION_TYPE_SS;
            toolsbar = new SSToolsbar(getApplicationContext(), control);
        }
        // PowerPoint
        else if (file.endsWith(MainConstant.FILE_TYPE_PPT)
                || file.endsWith(MainConstant.FILE_TYPE_PPTX)
                || file.endsWith(MainConstant.FILE_TYPE_POT)
                || file.endsWith(MainConstant.FILE_TYPE_PPTM)
                || file.endsWith(MainConstant.FILE_TYPE_POTX)
                || file.endsWith(MainConstant.FILE_TYPE_POTM)) {
            applicationType = MainConstant.APPLICATION_TYPE_PPT;
            toolsbar = new PGToolsbar(getApplicationContext(), control);
        }
        // PDF document
        else if (file.endsWith(MainConstant.FILE_TYPE_PDF)) {
            applicationType = MainConstant.APPLICATION_TYPE_PDF;
            toolsbar = new PDFToolsbar(getApplicationContext(), control);
        } else {
            applicationType = MainConstant.APPLICATION_TYPE_WP;
            toolsbar = new WPToolsbar(getApplicationContext(), control);
        }
    }

    /**
     * @return
     */
    private boolean isSearchbarActive() {
        if (appFrame == null || isDispose) {
            return false;
        }
        int count = appFrame.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = appFrame.getChildAt(i);
            if (v instanceof FindToolBar) {
                return v.getVisibility() == View.VISIBLE;
            }
        }
        return false;
    }

    /**
     * show toolbar or search bar
     *
     * @param show
     */
    public void showSearchBar(boolean show) {
        //show search bar
        if (show) {
            if (searchBar == null) {
                searchBar = new FindToolBar(this, control);
                appFrame.addView(searchBar, 0);
            }
            searchBar.setVisibility(View.VISIBLE);
            toolsbar.setVisibility(View.GONE);
        }
        // hide search bar
        else {
            if (searchBar != null) {
                searchBar.setVisibility(View.GONE);
            }
            toolsbar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * show toolbar or search bar
     *
     * @param show
     */
    public void showCalloutToolsBar(boolean show) {
        //show callout bar
        if (show) {
            if (calloutBar == null) {
                calloutBar = new CalloutToolsbar(getApplicationContext(), control);
                appFrame.addView(calloutBar, 0);
            }
            calloutBar.setCheckState(EventConstant.APP_PEN_ID, AImageCheckButton.CHECK);
            calloutBar.setCheckState(EventConstant.APP_ERASER_ID, AImageCheckButton.UNCHECK);
            calloutBar.setVisibility(View.VISIBLE);
            toolsbar.setVisibility(View.GONE);
        }
        // hide callout bar
        else {
            if (calloutBar != null) {
                calloutBar.setVisibility(View.GONE);
            }
            toolsbar.setVisibility(View.VISIBLE);
        }
    }

    public void setPenUnChecked() {
        if (fullscreen) {
            penButton.setState(AImageCheckButton.UNCHECK);
            penButton.postInvalidate();
        } else {
            calloutBar.setCheckState(EventConstant.APP_PEN_ID, AImageCheckButton.UNCHECK);
            calloutBar.postInvalidate();
        }
    }

    public void setEraserUnChecked() {
        if (fullscreen) {
            eraserButton.setState(AImageCheckButton.UNCHECK);
            eraserButton.postInvalidate();
        } else {
            calloutBar.setCheckState(EventConstant.APP_ERASER_ID, AImageCheckButton.UNCHECK);
            calloutBar.postInvalidate();
        }
    }

    /**
     * set the find back button and find forward button state
     *
     * @param state
     */
    public void setFindBackForwardState(boolean state) {
        if (isSearchbarActive()) {
            searchBar.setEnabled(EventConstant.APP_FIND_BACKWARD, state);
            searchBar.setEnabled(EventConstant.APP_FIND_FORWARD, state);
        }
    }

    /**
     * ????????????
     */
    public void fileShare() {
        ArrayList<Uri> list = new ArrayList<Uri>();

        File file = new File(filePath);
        list.add(Uri.fromFile(file));

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.putExtra(Intent.EXTRA_STREAM, list);
        intent.setType("application/octet-stream");
        startActivity(Intent
                .createChooser(intent, getResources().getText(com.wxiwei.office.officereader.R.string.sys_share_title)));
    }

    /**
     * @return
     */
    public void initMarked() {
        if (dbService != null) {
            marked = dbService.queryItem(MainConstant.TABLE_STAR, filePath);
            if (marked) {
                toolsbar.setCheckState(EventConstant.FILE_MARK_STAR_ID, AImageCheckButton.CHECK);
            } else {
                toolsbar.setCheckState(EventConstant.FILE_MARK_STAR_ID, AImageCheckButton.UNCHECK);
            }
        }
    }

    /**
     * @return
     */
    private void markFile() {
        marked = !marked;
    }

    public void resetTitle(String title) {
        if (title != null) {
            this.setTitle(title);
        }
    }

    public FindToolBar getSearchBar() {
        return searchBar;
    }

    /**
     *
     */
    public Dialog onCreateDialog(int id) {
        return control.getDialog(this, id);
    }

    /**
     * ????????????????????????
     */
    public void updateToolsbarStatus() {
        if (appFrame == null || isDispose) {
            return;
        }
        int count = appFrame.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = appFrame.getChildAt(i);
            if (v instanceof AToolsbar) {
                ((AToolsbar) v).updateStatus();
            }
        }
    }

    /**
     *
     */
    public IControl getControl() {
        return this.control;
    }

    /**
     *
     */
    public int getApplicationType() {
        return this.applicationType;
    }

    /**
     * @return Returns the filePath.
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     *
     */
    public Activity getActivity() {
        return this;
    }

    /**
     * do action???this is method don't call <code>control.actionEvent</code> method, Easily lead to infinite loop
     *
     * @param actionID action ID
     * @param obj      acValue
     * @return True if the listener has consumed the event, false otherwise.
     */
    public boolean doActionEvent(int actionID, Object obj) {
        try {
            switch (actionID) {
                case EventConstant.SYS_RESET_TITLE_ID:
                    setTitle((String) obj);
                    break;

                case EventConstant.SYS_ONBACK_ID:
                    onBackPressed();
                    break;

                case EventConstant.SYS_UPDATE_TOOLSBAR_BUTTON_STATUS: //update toolsbar state
                    updateToolsbarStatus();
                    break;

                case EventConstant.SYS_HELP_ID: //show help net
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources()
                            .getString(com.wxiwei.office.officereader.R.string.sys_url_wxiwei)));
                    startActivity(intent);
                    break;

                case EventConstant.APP_FIND_ID: //show search bar
                    showSearchBar(true);
                    break;

                case EventConstant.APP_SHARE_ID: //share file
                    fileShare();
                    break;

                case EventConstant.FILE_MARK_STAR_ID: //mark
                    markFile();
                    break;

                case EventConstant.APP_FINDING:
                    String content = ((String) obj).trim();
                    if (content.length() > 0 && control.getFind().find(content)) {
                        setFindBackForwardState(true);
                    } else {
                        setFindBackForwardState(false);
                        toast.setText(getLocalString("DIALOG_FIND_NOT_FOUND"));
                        toast.show();
                    }
                    break;

                case EventConstant.APP_FIND_BACKWARD:
                    if (!control.getFind().findBackward()) {
                        searchBar.setEnabled(EventConstant.APP_FIND_BACKWARD, false);
                        toast.setText(getLocalString("DIALOG_FIND_TO_BEGIN"));
                        toast.show();
                    } else {
                        searchBar.setEnabled(EventConstant.APP_FIND_FORWARD, true);
                    }
                    break;

                case EventConstant.APP_FIND_FORWARD:
                    if (!control.getFind().findForward()) {
                        searchBar.setEnabled(EventConstant.APP_FIND_FORWARD, false);
                        toast.setText(getLocalString("DIALOG_FIND_TO_END"));
                        toast.show();
                    } else {
                        searchBar.setEnabled(EventConstant.APP_FIND_BACKWARD, true);
                    }
                    break;

                case EventConstant.SS_CHANGE_SHEET:
                    bottomBar.setFocusSheetButton((Integer) obj);
                    break;

                case EventConstant.APP_DRAW_ID:
                    showCalloutToolsBar(true);
                    control.getSysKit().getCalloutManager().setDrawingMode(MainConstant.DRAWMODE_CALLOUTDRAW);
                    appFrame.post(new Runnable() {

                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            control.actionEvent(EventConstant.APP_INIT_CALLOUTVIEW_ID, null);

                        }
                    });

                    break;

                case EventConstant.APP_BACK_ID:
                    showCalloutToolsBar(false);
                    control.getSysKit().getCalloutManager().setDrawingMode(MainConstant.DRAWMODE_NORMAL);
                    break;

                case EventConstant.APP_PEN_ID:
                    if ((Boolean) obj) {
                        control.getSysKit().getCalloutManager().setDrawingMode(MainConstant.DRAWMODE_CALLOUTDRAW);
                        setEraserUnChecked();
                        appFrame.post(new Runnable() {

                            @Override
                            public void run() {
                                // TODO Auto-generated method stub
                                control.actionEvent(EventConstant.APP_INIT_CALLOUTVIEW_ID, null);

                            }
                        });
                    } else {
                        control.getSysKit().getCalloutManager().setDrawingMode(MainConstant.DRAWMODE_NORMAL);
                    }
                    break;

                case EventConstant.APP_ERASER_ID:
                    if ((Boolean) obj) {
                        control.getSysKit().getCalloutManager().setDrawingMode(MainConstant.DRAWMODE_CALLOUTERASE);
                        setPenUnChecked();
                    } else {
                        control.getSysKit().getCalloutManager().setDrawingMode(MainConstant.DRAWMODE_NORMAL);
                    }
                    break;

                case EventConstant.APP_COLOR_ID:
                    ColorPickerDialog dlg = new ColorPickerDialog(this, control);
                    dlg.show();
                    dlg.setOnDismissListener(new OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            setButtonEnabled(true);
                        }
                    });
                    setButtonEnabled(false);
                    break;

                default:
                    return false;
            }
        } catch (Exception e) {
            control.getSysKit().getErrorKit().writerLog(e);
        }
        return true;
    }

    /**
     * pages count change
     */
    public void onPagesCountChange() {

    }

    /**
     * current display pags change
     */
    public void onCurrentPageChange() {

    }

    /**
     *
     */
    public void openFileFinish() {
        // ??????????????????????????????????????????
        gapView = new View(getApplicationContext());
        gapView.setBackgroundColor(Color.GRAY);
        appFrame.addView(gapView, new LayoutParams(LayoutParams.MATCH_PARENT, 1));
        //
        View app = control.getView();
        appFrame.addView(app,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        //
        /*if (applicationType == MainConstant.APPLICATION_TYPE_SS)
        {
            bottomBar = new SheetBar(getApplicationContext(), control, getResources().getDisplayMetrics().widthPixels);
            appFrame.addView(bottomBar, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        }*/
    }

    /**
     *
     */
    public int getBottomBarHeight() {
        if (bottomBar != null) {
            return bottomBar.getSheetbarHeight();
        }
        return 0;
    }

    /**
     *
     */
    public int getTopBarHeight() {
        return 0;
    }

    /**
     * event method, office engine dispatch
     *
     * @param v      event source
     * @param e1     MotionEvent instance
     * @param e2     MotionEvent instance
     * @param xValue eventNethodType is ON_SCROLL, this is value distanceX
     *               eventNethodType is ON_FLING, this is value velocityY
     *               eventNethodType is other type, this is value -1
     * @param yValue eventNethodType is ON_SCROLL, this is value distanceY
     *               eventNethodType is ON_FLING, this is value velocityY
     *               eventNethodType is other type, this is value -1
     * @see IMainFrame#ON_CLICK
     * @see IMainFrame#ON_DOUBLE_TAP
     * @see IMainFrame#ON_DOUBLE_TAP_EVENT
     * @see IMainFrame#ON_DOWN
     * @see IMainFrame#ON_FLING
     * @see IMainFrame#ON_LONG_PRESS
     * @see IMainFrame#ON_SCROLL
     * @see IMainFrame#ON_SHOW_PRESS
     * @see IMainFrame#ON_SINGLE_TAP_CONFIRMED
     * @see IMainFrame#ON_SINGLE_TAP_UP
     * @see IMainFrame#ON_TOUCH
     */
    public boolean onEventMethod(View v, MotionEvent e1, MotionEvent e2, float xValue,
                                 float yValue, byte eventMethodType) {
        return false;
    }


    public void changePage() {
    }

    /**
     *
     */
    public String getAppName() {
        return getString(com.wxiwei.office.officereader.R.string.sys_name);
    }

    /**
     * ??????????????????
     */
    public boolean isDrawPageNumber() {
        return false;
    }

    /**
     * ????????????zoom in / zoom out
     */
    public boolean isTouchZoom() {
        return true;
    }

    /**
     * Word application ????????????(Normal or Page)
     *
     * @return WPViewConstant.PAGE_ROOT or WPViewConstant.NORMAL_ROOT
     */
    public byte getWordDefaultView() {
        return WPViewConstant.PAGE_ROOT;
        //return WPViewConstant.NORMAL_ROOT;
    }

    /**
     * normal view, changed after zoom bend, you need to re-layout
     *
     * @return true   re-layout
     * false  don't re-layout
     */
    public boolean isZoomAfterLayoutForWord() {
        return true;
    }

    /**
     * init float button, for slideshow pageup/pagedown
     */
    private void initFloatButton() {
        //icon width and height
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), com.wxiwei.office.officereader.R.drawable.file_slideshow_left, opts);

        //load page up button
        Resources res = getResources();
        pageUp = new AImageButton(this, control, res.getString(com.wxiwei.office.officereader.R.string.pg_slideshow_pageup), -1,
                -1, EventConstant.APP_PAGE_UP_ID);
        pageUp.setNormalBgResID(com.wxiwei.office.officereader.R.drawable.file_slideshow_left);
        pageUp.setPushBgResID(com.wxiwei.office.officereader.R.drawable.file_slideshow_left_push);
        pageUp.setLayoutParams(new LayoutParams(opts.outWidth, opts.outHeight));

        //load page down button
        pageDown = new AImageButton(this, control, res.getString(com.wxiwei.office.officereader.R.string.pg_slideshow_pagedown),
                -1, -1, EventConstant.APP_PAGE_DOWN_ID);
        pageDown.setNormalBgResID(com.wxiwei.office.officereader.R.drawable.file_slideshow_right);
        pageDown.setPushBgResID(com.wxiwei.office.officereader.R.drawable.file_slideshow_right_push);
        pageDown.setLayoutParams(new LayoutParams(opts.outWidth, opts.outHeight));

        BitmapFactory.decodeResource(getResources(), com.wxiwei.office.officereader.R.drawable.file_slideshow_pen_normal, opts);
        // load pen button
        penButton = new AImageCheckButton(this, control,
                res.getString(com.wxiwei.office.officereader.R.string.app_toolsbar_pen_check), res.getString(com.wxiwei.office.officereader.R.string.app_toolsbar_pen),
                com.wxiwei.office.officereader.R.drawable.file_slideshow_pen_check, com.wxiwei.office.officereader.R.drawable.file_slideshow_pen_normal,
                com.wxiwei.office.officereader.R.drawable.file_slideshow_pen_normal, EventConstant.APP_PEN_ID);
        penButton.setNormalBgResID(com.wxiwei.office.officereader.R.drawable.file_slideshow_pen_normal);
        penButton.setPushBgResID(com.wxiwei.office.officereader.R.drawable.file_slideshow_pen_push);
        penButton.setLayoutParams(new LayoutParams(opts.outWidth, opts.outHeight));

        // load eraser button
        eraserButton = new AImageCheckButton(this, control,
                res.getString(com.wxiwei.office.officereader.R.string.app_toolsbar_eraser_check), res.getString(com.wxiwei.office.officereader.R.string.app_toolsbar_eraser),
                com.wxiwei.office.officereader.R.drawable.file_slideshow_eraser_check, com.wxiwei.office.officereader.R.drawable.file_slideshow_eraser_normal,
                com.wxiwei.office.officereader.R.drawable.file_slideshow_eraser_normal, EventConstant.APP_ERASER_ID);
        eraserButton.setNormalBgResID(com.wxiwei.office.officereader.R.drawable.file_slideshow_eraser_normal);
        eraserButton.setPushBgResID(com.wxiwei.office.officereader.R.drawable.file_slideshow_eraser_push);
        eraserButton.setLayoutParams(new LayoutParams(opts.outWidth, opts.outHeight));

        // load settings button
        settingsButton = new AImageButton(this, control, res.getString(com.wxiwei.office.officereader.R.string.app_toolsbar_color),
                -1, -1, EventConstant.APP_COLOR_ID);
        settingsButton.setNormalBgResID(com.wxiwei.office.officereader.R.drawable.file_slideshow_settings_normal);
        settingsButton.setPushBgResID(com.wxiwei.office.officereader.R.drawable.file_slideshow_settings_push);
        settingsButton.setLayoutParams(new LayoutParams(opts.outWidth, opts.outHeight));

        wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        wmParams = new WindowManager.LayoutParams();

        wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        wmParams.width = opts.outWidth;
        wmParams.height = opts.outHeight;
    }

    /**
     * full screen, not show top tool bar
     */
    public void fullScreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        if (fullscreen) {
            if (wm == null || wmParams == null) {
                initFloatButton();
            }

            wmParams.gravity = Gravity.RIGHT | Gravity.TOP;
            wmParams.x = MainConstant.GAP;
            wm.addView(penButton, wmParams);

            wmParams.gravity = Gravity.RIGHT | Gravity.TOP;
            wmParams.x = MainConstant.GAP;
            wmParams.y = wmParams.height;
            wm.addView(eraserButton, wmParams);

            wmParams.gravity = Gravity.RIGHT | Gravity.TOP;
            wmParams.x = MainConstant.GAP;
            wmParams.y = wmParams.height * 2;
            wm.addView(settingsButton, wmParams);

            wmParams.gravity = Gravity.LEFT | Gravity.CENTER;
            wmParams.x = MainConstant.GAP;
            wmParams.y = 0;
            wm.addView(pageUp, wmParams);

            wmParams.gravity = Gravity.RIGHT | Gravity.CENTER;
            wm.addView(pageDown, wmParams);

            //hide title and tool bar
            ((View) getWindow().findViewById(android.R.id.title).getParent())
                    .setVisibility(View.GONE);
            //hide status bar
            toolsbar.setVisibility(View.GONE);
            //
            gapView.setVisibility(View.GONE);

            penButton.setState(AImageCheckButton.UNCHECK);
            eraserButton.setState(AImageCheckButton.UNCHECK);

            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getWindow().setAttributes(params);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

            //landscape
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        } else {
            wm.removeView(pageUp);
            wm.removeView(pageDown);
            wm.removeView(penButton);
            wm.removeView(eraserButton);
            wm.removeView(settingsButton);
            //show title and tool bar
            ((View) getWindow().findViewById(android.R.id.title).getParent())
                    .setVisibility(View.VISIBLE);
            toolsbar.setVisibility(View.VISIBLE);
            gapView.setVisibility(View.VISIBLE);

            //show status bar
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setAttributes(params);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

    }

    /**
     *
     */

    public void changeZoom() {
    }

    /**
     *
     */
    public void error(int errorCode) {
    }

    /**
     * when need destroy the office engine instance callback this method
     */
    public void destroyEngine() {
        super.onBackPressed();
    }

    /**
     * get Internationalization resource
     *
     * @param resName Internationalization resource name
     */
    public String getLocalString(String resName) {
        return ResKit.instance().getLocalString(resName);
    }

    @Override
    public boolean isShowPasswordDlg() {
        return true;
    }

    @Override
    public boolean isShowProgressBar() {
        return true;
    }

    @Override
    public boolean isShowFindDlg() {
        return true;
    }

    @Override
    public boolean isShowTXTEncodeDlg() {
        return true;
    }

    /**
     * get txt default encode when not showing txt encode dialog
     *
     * @return null if showing txt encode dialog
     */
    public String getTXTDefaultEncode() {
        return "GBK";
    }

    /**
     *
     */
//    public DialogListener getDialogListener() {
//        return null;
//    }
    @Override
    public void completeLayout() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isChangePage() {
        // TODO Auto-generated method stub
        return true;
    }

    /**
     * @param saveLog
     */
    public void setWriteLog(boolean saveLog) {
        this.writeLog = saveLog;
    }

    /**
     * @return
     */
    public boolean isWriteLog() {
        return writeLog;
    }

    /**
     * @param isThumbnail
     */
    public void setThumbnail(boolean isThumbnail) {
        this.isThumbnail = isThumbnail;
    }

    /**
     * get view backgrouond
     *
     * @return
     */
    public Object getViewBackground() {
        return bg;
    }

    /**
     * set flag whether fitzoom can be larger than 100% but smaller than the max zoom
     *
     * @param ignoreOriginalSize
     */
    public void setIgnoreOriginalSize(boolean ignoreOriginalSize) {

    }

    /**
     * @return true fitzoom may be larger than 100% but smaller than the max zoom
     * false fitzoom can not larger than 100%
     */
    public boolean isIgnoreOriginalSize() {
        return false;
    }

    public byte getPageListViewMovingPosition() {
        return IPageListViewListener.Moving_Horizontal;
    }

    /**
     * @return
     */
    public boolean isThumbnail() {
        return isThumbnail;
    }

    /**
     * @param viewList
     */
    public void updateViewImages(List<Integer> viewList) {

    }

    /**
     * @return
     */
    public File getTemporaryDirectory() {
        // Get path for the file on external storage.  If external
        // storage is not currently mounted this will fail.
        File file = getExternalFilesDir(null);
        if (file != null) {
            return file;
        } else {
            return getFilesDir();
        }
    }

    /**
     * ????????????
     */
    public void dispose() {
        isDispose = true;
        if (control != null) {
            control.dispose();
            control = null;
        }
        toolsbar = null;
        searchBar = null;
        bottomBar = null;
        if (dbService != null) {
            dbService.dispose();
            dbService = null;
        }
        if (appFrame != null) {
            int count = appFrame.getChildCount();
            for (int i = 0; i < count; i++) {
                View v = appFrame.getChildAt(i);
                if (v instanceof AToolsbar) {
                    ((AToolsbar) v).dispose();
                }
            }
            appFrame = null;
        }

        if (wm != null) {
            wm = null;
            wmParams = null;
            pageUp.dispose();
            pageDown.dispose();
            penButton.dispose();
            eraserButton.dispose();
            settingsButton.dispose();
            pageUp = null;
            pageDown = null;
            penButton = null;
            eraserButton = null;
            settingsButton = null;
        }
    }

    @Override
    public FrameLayout getMainFrame() {
        return binding.viewerOffice;
    }

    //
    private boolean isDispose;
    // ??????????????????
    private boolean marked;
    //
    private int applicationType = -1;
    //
    private String fileName;
    //
    private String filePath, realPath;
    // application activity control
    private MainControl control;
    //
    private AppFrame appFrame;
    //tool bar
    private AToolsbar toolsbar;
    //search bar
    private FindToolBar searchBar;
    //
    private DBService dbService;
    //
    private SheetBar bottomBar;
    //
    private Toast toast;
    //
    private View gapView;

    //float button: PageUp/PageDown
    private WindowManager wm = null;
    private WindowManager.LayoutParams wmParams = null;
    private AImageButton pageUp;
    private AImageButton pageDown;
    private AImageCheckButton penButton;
    private AImageCheckButton eraserButton;
    private AImageButton settingsButton;

    //whether write log to temporary file
    private boolean writeLog = true;
    //open file to get thumbnail, or not
    private boolean isThumbnail;
    //view background
    private Object bg = Color.GRAY;
    //
    private CalloutToolsbar calloutBar;
    //
    private boolean fullscreen;
    //
//    private String tempFilePath;
}
