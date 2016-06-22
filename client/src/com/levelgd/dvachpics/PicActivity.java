package com.levelgd.dvachpics;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.PopupMenu;
import android.widget.Toast;

import java.io.File;

/**
 * Created by lvlgd on 09.01.2016.
 */
public class PicActivity extends Activity implements PopupMenu.OnMenuItemClickListener {

    WebView webView;
    ActionBar actionBar;

    String picLink;

    String currentBoard = "";
    String currentThreadNumber = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.picview);

        Intent intent = getIntent();
        picLink = intent.getStringExtra("pic");
        currentBoard = intent.getStringExtra("currentBoard");
        currentThreadNumber = intent.getStringExtra("currentThreadNumber");

        try{
            actionBar = getActionBar();
        }catch (NullPointerException e){
            //
        }

        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);

            actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#212121")));
            actionBar.setTitle(MainActivity.coloredSpanned(getString(R.string.action_bar_text_color),picLink.replaceFirst(".*/","")));
        }

        webView = (WebView)findViewById(R.id.webViewPic);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });

        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        if( Build.VERSION.SDK_INT > 16) webView.getSettings().setMediaPlaybackRequiresUserGesture(false);

        if(picLink != null && picLink.length() > 0){
            webView.loadUrl(picLink);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.pic_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_pic_download:
                downloadFile();
                return true;

            case R.id.action_pic_share:
                shareFile();
                return true;

            case R.id.action_pic_more:
                showActions(findViewById(R.id.action_pic_more));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.pic_menu_save:
                downloadFile();
                return true;

            case R.id.pic_menu_share:
                shareFile();
                return true;

            case R.id.pic_to_post:
                Toast.makeText(getApplicationContext(),"Получение ссылки на пост...",Toast.LENGTH_SHORT).show();
                //Клиент настроен на работу с существующим сервером.
                (new RequestPostLinkTask()).execute(
                        "https://sosach.herokuapp.com/post?board=" + currentBoard + "&thread=" + currentThreadNumber + "&thumblink=" + picLink
                );
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showActions(View v){
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.actionbar_pic_options);
        popupMenu.show();
    }

    private void shareFile(){

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/2ch/" + picLink.replaceFirst(".*/",""));

        if(!file.exists()){
            Toast.makeText(getApplicationContext(), "Сначала сохраните файл", Toast.LENGTH_LONG).show();
            return;
        }

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+file.getAbsolutePath()));

        String ext = picLink.replaceFirst(".*\\.","");

        if(ext.equals("webm")){
            shareIntent.setType("video/webm");
        }else{
            shareIntent.setType("image/"+ext);
        }

        startActivity(Intent.createChooser(shareIntent, "Поделиться"));
    }

    private void downloadFile(){

        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/2ch");
        if(!dir.exists()) {
            if(!dir.mkdirs()){
                Log.e("MKDIRS","Невозможно создать папку " + dir.getPath());

                // Для Андроида 6 нужно вручную указать разрешение на доступ к записи в настройках, если система не предложила разрешить этот доступ
                if(Build.VERSION.SDK_INT > 22){
                    Toast.makeText(getApplicationContext(), "Нужно вручную дать разрешение приложению для работы с памятью (Свойства приложения -> Разрешения)", Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(), "Невозможно создать папку Pictures/2ch, создайте ее вручную", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }

        Toast.makeText(getApplicationContext(), "Файл скачивается в папку " + Environment.DIRECTORY_PICTURES + "/2ch", Toast.LENGTH_SHORT).show();

        DownloadManager downloadManager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(picLink));

        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI)
                .setAllowedOverRoaming(false)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES + "/2ch",picLink.replaceFirst(".*/",""));

        try{
            downloadManager.enqueue(request);
        }catch (SecurityException e){
            Toast.makeText(getApplicationContext(), "Нет разрешения на сохранение файла. На Android 6 выставляется вручную в свойствах приложения", Toast.LENGTH_LONG).show();
        }
    }

    class RequestPostLinkTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... urls) {
            try {
                HttpRequest request =  HttpRequest.get(urls[0]);
                String body;
                if (request.ok()) {
                    body = request.body();
                }else{
                    body = "error";
                }
                return body;
            } catch (HttpRequest.HttpRequestException exception) {
                Log.e("HttpRequestException",exception.getMessage());
                return "request exception";
            }
        }

        protected void onPostExecute(String body) {
            if(body.contains("#")){
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(body)));
            }else{
                Toast.makeText(getApplicationContext(),"Ошибка при получении ссылки на пост",Toast.LENGTH_LONG).show();
            }
        }
    }
}
