package com.levelgd.dvachpics;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
//import android.support.v4.widget.SwipeRefreshLayout;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.webkit.*;
import android.widget.*;

import java.util.Arrays;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements PopupMenu.OnMenuItemClickListener {

    WebView webView;
    View loadingSpinner;

    ActionBar actionBar;
    TextView textViewScroll;
    //SwipeRefreshLayout swipeRefreshLayout;
    SharedPreferences preferences;

    GestureDetector gestureDetector;

    int shortAnimationDuration;

    String[] threads = new String[0];

    int currentThread = 0;
    String currentBoard = "";
    String currentThreadNumber = "";
    String currentThreadURL = "";
    String currentThumbLink = "";

    String versionName = "0";

    boolean loaded = false;
    boolean swipeEnabled = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        }catch (PackageManager.NameNotFoundException e){
            versionName = "-1";
        }finally {
            Log.i("VN", versionName + "");
        }

        try{
            actionBar = getActionBar();
        }catch (NullPointerException e){
            //
        }

        if(actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#212121")));
            actionBar.setTitle(coloredSpanned(getString(R.string.action_bar_text_color),"Инициализация..."));
        }

        webView = (WebView)findViewById(R.id.webView);

        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this,"Android");

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        loadingSpinner = findViewById(R.id.frameLayoutLoading);
        webView.setVisibility(View.GONE);

        shortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        gestureDetector = new GestureDetector(this, new MyGestureListener());
        webView.setOnTouchListener(
                new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        gestureDetector.onTouchEvent(event);
                        return onTouchEvent(event);
                    }
                }
        );

        /*swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.main_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadThread(currentBoard,currentThreadNumber);
            }
        });*/

        final Pattern picPattern = Pattern.compile(".*(jpg|png|gif|webm)$");

        webView.setWebViewClient(new WebViewClient(){

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url){

                if(picPattern.matcher(url).matches()){
                    Intent picIntent = new Intent(MainActivity.this, PicActivity.class);

                    picIntent.putExtra("pic", url);
                    picIntent.putExtra("currentBoard", currentBoard);
                    picIntent.putExtra("currentThreadNumber", currentThreadNumber);

                    startActivity(picIntent);
                }else{
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    //super.shouldOverrideUrlLoading(view, url);
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon){
                showContent(false);
            }

            @Override
            public void onPageFinished(WebView view, String url){
                showContent(true);

                //swipeRefreshLayout.setRefreshing(false);

                if(webView.getTranslationX() != 0){
                    webView.setTranslationX(-webView.getX());
                    webView.animate().translationX(0);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                //Log.d("TAG", cm.message() + " at " + cm.sourceId() + ":" + cm.lineNumber());
                return true;
            }
        });

        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                unregisterForContextMenu(webView);

                WebView.HitTestResult hitTestResult = webView.getHitTestResult();
                //Log.i("RESULT",hitTestResult.getType() + " " + hitTestResult.getExtra());

                int t = hitTestResult.getType();
                String e = hitTestResult.getExtra();

                if((t == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE || t == WebView.HitTestResult.SRC_ANCHOR_TYPE) && picPattern.matcher(e).matches()){
                    currentThumbLink = e;
                    registerForContextMenu(webView);
                }

                return false;
            }
        });

        textViewScroll = (TextView) findViewById(R.id.textViewScroll);
        textViewScroll.setText("");

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        currentBoard = preferences.getString("currentBoard","b");

        enableSwipe(preferences.getBoolean("swipeEnabled",false));

        refreshAll();
    }

    @Override
    protected void onPause(){

        preferences.edit()
                .putBoolean("swipeEnabled",swipeEnabled)
                .putString("currentBoard",currentBoard)
                .apply();

        super.onPause();

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        switch (v.getId()) {
            case R.id.webView:
                menu.add(0, 1, 0, "Открыть пост (в браузере)");
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                Toast.makeText(getApplicationContext(),"Получение ссылки на пост...",Toast.LENGTH_SHORT).show();
                (new RequestPostLinkTask()).execute(
                    "https://sosach.herokuapp.com/post?board=" + currentBoard + "&thread=" + currentThreadNumber + "&thumblink=" + currentThumbLink
                );
                break;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_change_board:
                showChangeBoard();
                return true;

            case R.id.action_more:
                showActions(findViewById(R.id.action_more));
                return true;

            case R.id.action_refresh_all:
                refreshAll();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showListThreads(String raw){

        String[] items = raw.split("•");

        if(items.length > 0){

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.dialog_list_threads, null);
            builder.setView(view);
            final AlertDialog dialog = builder.create();

            TextView title = (TextView)view.findViewById(R.id.textViewTitleChooseThread);
            title.setText(title.getText() + " /" + currentBoard + "/");

            ListView listView = (ListView) view.findViewById(R.id.listViewChooseThread);

            String[] numberArray = new String[items.length];
            String[] titleArray = new String[items.length];
            String[] subItemArray = new String[items.length];

            for(int i = 0; i < items.length; i++){

                String[] titlePost = items[i].split("¶");

                if(titlePost.length == 3){
                    numberArray[i] = titlePost[0];
                    titleArray[i] = titlePost[1].length() == 0 ? "~" : titlePost[1];
                    subItemArray[i] = titlePost[2].length() == 0 ? "..." : titlePost[2].replaceAll("<.*?>","");

                    if(subItemArray[i].length() > 200) subItemArray[i] = subItemArray[i].substring(0,200) + "...";
                }else{
                    numberArray[i] = "...";
                    titleArray[i] = "...";
                    subItemArray[i] = "ошибка при загрузке";
                }
            }

            listView.setAdapter(new ThreadsAdapter(this, numberArray, titleArray, subItemArray));
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView parent, View v, int position, long id) {

                    String n = parent.getItemAtPosition(position).toString().replaceAll("№","");

                    if(n.equals("...")) return;

                    currentThreadNumber = parent.getItemAtPosition(position).toString().replaceAll("№","");
                    refreshAll();

                    dialog.dismiss();
                }

            });

            dialog.show();
        }
    }

    private void showChangeBoard(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_change_board, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();

        ListView listView = (ListView) view.findViewById(R.id.listViewChoose);
        String[] titleArray = getResources().getStringArray(R.array.boards);
        String[] subItemArray = getResources().getStringArray(R.array.boards_desc);

        listView.setAdapter(new BoardsAdapter(this,titleArray,subItemArray));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                currentBoard = parent.getItemAtPosition(position).toString().replaceAll("/","");
                currentThread = 0;
                currentThreadNumber = "";
                refreshAll();

                dialog.dismiss();
            }

        });

        dialog.show();
    }

    private void showSendMessage(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View view = getLayoutInflater().inflate(R.layout.dialog_bugreport, null);
        builder.setView(view);

        builder.setPositiveButton("Отправить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                (new RequestSendMessageTask()).execute("http://levelgd.ru/pikchi/message.php","text=" + ((EditText)view.findViewById(R.id.editTextMessage)).getText().toString());
            }
        });

        builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showActions(View v){
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.actionbar_options);
        popupMenu.show();

        popupMenu.getMenu().findItem(R.id.enable_swipe).setChecked(swipeEnabled);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.to_thread:
                if(currentThreadURL.length() <= 0){
                    if(currentThreadNumber.length() > 1){
                        currentThreadURL = "http://2ch.hk/" + currentBoard + "/res/" + currentThreadNumber + ".html";
                    }else{
                        Toast.makeText(getApplicationContext(),"Упс, ссылка на тред оказалась битой",Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(currentThreadURL)));
                return true;

            case R.id.threads_list:
                Toast.makeText(getApplicationContext(),"Получение списка тредов...",Toast.LENGTH_SHORT).show();
                (new RequestThreadsTask()).execute("https://sosach.herokuapp.com/threads?board=" + currentBoard);
                return true;

            case R.id.to_board:
                showChangeBoard();
                return true;

            case R.id.send_message:
                showSendMessage();
                return true;

            case R.id.enable_swipe:
                enableSwipe(!swipeEnabled);
                item.setChecked(swipeEnabled);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshAll(){

        showContent(false);

        textViewScroll.setText("");

        if(!isNetworkConnected()){
            Toast.makeText(getApplicationContext(),"Требуется подключение к Интернету", Toast.LENGTH_LONG).show();
            return;
        }

        (new RequestTask()).execute("https://sosach.herokuapp.com/count?board=" + currentBoard);
    }

    public void nextThread(View v){

        if(!loaded) return;

        if(v != null){
            ScaleAnimation scaleAnimationEnd =  new ScaleAnimation(0.2f, 1f, 0.2f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            scaleAnimationEnd.setDuration(shortAnimationDuration);

            v.startAnimation(scaleAnimationEnd);
        }

        webView.animate().translationX(-webView.getWidth()/2).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });

        if(currentThread + 1 < threads.length){
            currentThread++;
        }else{
            currentThread = 0;
        }

        textViewScroll.setText(setScrollDots(textViewScroll.getText().toString(),currentThread));

        if(threads.length > 0) loadThread(currentBoard, threads[currentThread]);
    }

    public void prevThread(View v){

        if(!loaded) return;

        webView.animate().translationX(webView.getWidth()/2).setListener(null);

        if(currentThread - 1 >= 0){
            currentThread--;
        }else{
            currentThread = threads.length - 1;
        }

        textViewScroll.setText(setScrollDots(textViewScroll.getText().toString(),currentThread));

        loadThread(currentBoard, threads[currentThread]);
    }

    private void loadThread(String board, String thread){

        if(!isNetworkConnected()){
            Toast.makeText(getApplicationContext(),"Требуется подключение к Интернету",Toast.LENGTH_LONG).show();
            return;
        }

        currentThreadURL = "";
        currentThreadNumber = thread;

        if(actionBar != null) {
            actionBar.setTitle(coloredSpanned(
                    getString(R.string.action_bar_text_color), "/"+currentBoard+"/ №" + thread)
            );
        }
        webView.loadUrl("https://sosach.herokuapp.com/?board="+board+"&thread="+thread+"&version="+versionName);
    }

    private void enableSwipe(boolean enable){

        swipeEnabled = enable;

        final FrameLayout frameLayout = (FrameLayout)findViewById(R.id.buttonsSwipeLayout);

        if(!enable){
            frameLayout.setAlpha(0f);
            frameLayout.setVisibility(View.VISIBLE);
            frameLayout.animate()
                    .alpha(1f)
                    .setDuration(shortAnimationDuration)
                    .setListener(null);
        }else{
            frameLayout.animate()
                    .alpha(0f)
                    .setDuration(shortAnimationDuration)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            frameLayout.setVisibility(View.GONE);
                        }
                    });
        }
    }

    private void showContent(boolean show){

        loaded = show;

        //swipeRefreshLayout.setEnabled(show);

        if(show && webView.getVisibility() == View.VISIBLE) return;
        if(!show && webView.getVisibility() == View.GONE) return;

        final View showView = show ? webView : loadingSpinner;
        final View hideView = show ? loadingSpinner : webView;

        showView.setAlpha(0f);
        showView.setVisibility(View.VISIBLE);

        showView.animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration)
                .setListener(null);

        hideView.animate()
                .alpha(0f)
                .setDuration(shortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        hideView.setVisibility(View.GONE);
                    }
                });
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_MIN_DISTANCE = 100;
        private static final int SWIPE_MAX_OFF_PATH = 100;
        private static final int SWIPE_THRESHOLD_VELOCITY = 80;

        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            if(!swipeEnabled) return false;

            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH){
                return false;
            }

            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                nextThread(null);
            }

            else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                prevThread(null);
            }
            return false;
        }
    }

    class RequestSendMessageTask extends AsyncTask<String, Void, Boolean> {
        protected Boolean doInBackground(String... urls) {
            try {
                HttpRequest request = HttpRequest.post(urls[0]).send((urls[1]));
                return request.ok();
            } catch (HttpRequest.HttpRequestException exception) {
                Log.e("HttpRequestException",exception.getMessage());
            }

            return false;
        }

        protected void onPostExecute(Boolean ok) {
            if (ok) {
                Toast.makeText(getApplicationContext(),"Сообщение отправлено!",Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(getApplicationContext(),"Ошибка при отпаравке сообщения",Toast.LENGTH_SHORT).show();
            }
        }
    }

    class RequestThreadsTask extends AsyncTask<String, Void, String> {
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
            if(body != null && body.length() > 0 && !body.equals("error")){
                showListThreads(body);
            }else{
                Toast.makeText(getApplicationContext(),"Ошибка при получении списка тредов",Toast.LENGTH_LONG).show();
            }
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

    class RequestTask extends AsyncTask<String, Void, String> {
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
            if(!body.equals("error")){

                threads = body.split(",");

                if(threads.length > 0){
                    if(currentThreadNumber.length() > 0){
                        currentThread = Arrays.asList(threads).indexOf(currentThreadNumber);

                        if(currentThread < 0){
                            currentThread = 0;
                            if(currentThreadNumber.length() > 1) Toast.makeText(getApplicationContext(),"В топ-"+threads.length+" треда не нашлось, загрузка первого треда",Toast.LENGTH_LONG).show();
                        }
                    }else{
                        currentThread = 0;
                    }

                    StringBuilder stringBuilder = new StringBuilder();
                    for(int i = 0; i < threads.length; i++){
                        stringBuilder.append('.');
                    }
                    textViewScroll.setText(setScrollDots(stringBuilder.toString(),currentThread));

                    loadThread(currentBoard,threads[currentThread]);
                }else{
                    Toast.makeText(getApplicationContext(),"Ошибка при загрузке списка тедов. Попробуйте обновить еще раз",Toast.LENGTH_LONG).show();
                }

            }else{
                Toast.makeText(getApplicationContext(),"Ошибка при подключении",Toast.LENGTH_LONG).show();
            }
        }
    }

    private Spannable setScrollDots(String dots, int dotPosition){

        Spannable spannable = new SpannableString(dots);
        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#2196F3")),dotPosition,dotPosition+1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannable;
    }

    public static Spanned coloredSpanned(String color, String text){
        return Html.fromHtml("<font color='"+color+"'>"+text+"</font>");
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    @JavascriptInterface
    public void setThread(String url){
        currentThreadURL = url;

        runOnUiThread(new Runnable() {//runOnUiThread нужен обязательно. так как процедура срабатывает по сути в асинк таске
            public void run() {
                showContent(true);

                webView.setTranslationX(-webView.getX());
                webView.animate().translationX(0);
            }
        });
    }
}
