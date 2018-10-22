
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;



import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import pro.chain.wallet.MessageBus.EventType;
import pro.chain.wallet.MessageBus.MessageEvent;
import pro.chain.wallet.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static pro.chain.wallet.MessageBus.EventType.EVENT_TYPE_EOS_UNLOCK_WALLET;
import static pro.chain.wallet.MessageBus.EventType.EVENT_TYPE_POPUP_DISMISS;

public class scatterWebView extends fancyBaseActivity {

    private LoadingWebView webview;

    private String webURL="";



    private final Set<String> offlineResources = new HashSet<>();

    private Context context;

    CommonPopupWindow popupWindow;



   

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_fancy_web_view);


        EventBus.getDefault().register(this);


        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        initView();

        setTitleCenter(strContentString);

        context = this;

        webview = (LoadingWebView) findViewById(R.id.webview);
        webview.requestFocus(View.FOCUS_DOWN);
        webview.addProgressBar();

      


        webview.loadUrl(webURL,map);



        WebSettings settings = webview.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAppCacheMaxSize(1024*1024*8);
        String appCachePath = getApplicationContext().getCacheDir().getAbsolutePath();
        settings.setAppCachePath(appCachePath);
        settings.setAllowFileAccess(true);
        settings.setAppCacheEnabled(true);

        webview.setWebViewClient(new WebViewClient(){


            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // TODO Auto-generated method stub

                //返回值是true的时候控制去WebView打开，为false调用系统浏览器或第三方浏览器

                Uri uri = Uri.parse(url);
                // 如果url的协议 = 预先约定的 js 协议
                // 就解析往下解析参数
               {

                    view.loadUrl(url);
                    return true;
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {

                //loading.show();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //loading.dismiss();

                String scatterHook =  fancyUtil.getFromAssets(context);
                scatterHook  = scatterHook + " " + ";window.scatter = new Pra();";

                //scatterHook = "function Pra(){this.app = 'prochain';this.showMe = function(){alert(this.app);}}; window.scatter = new Pra();";

                webview.evaluateJavascript(scatterHook, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {

                        Log.d("alex_huang", "scatter js init()" + s );
                    }
                });

                webview.evaluateJavascript("let event = new CustomEvent(\"scatterloaded\",{\n" +
                        "            detail: {\n" +
                        "                hazcheeseburger: true//自定义的参数\n" +
                        "            }\n" +
                        "        });document.dispatchEvent(event); ", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        Log.d("alex_huang", "scatter loaded init()" + value );
                    }
                });



            }
        });






        //add javascript call native method list
        webview.addJavascriptInterface(new scatterWebAppInterface(),"Prochain");


    }

    void startDownLoad(String url)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }





    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webview.canGoBack()) {
                webview.goBack();//返回上一页面
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public void initView()
    {
        super.initView();
    }



    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();

    }




   



    // This method will be called when a MessageEvent is posted
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        
      
        if (event.type == EventType.EVENT_TYPE_EOS_SCATTER_CALLBACK)
        {
            webview.post(new Runnable() {
                @Override
                public void run() {
                    String  str = "javascript:functionname('" + event.param1 + "');";
                    str = str.replace("functionname",event.message);
                    str = str.replace("\n","");
                    webview.loadUrl(str);


                }
            });

        }

      

        if (event.type == EVENT_TYPE_EOS_UNLOCK_WALLET)
        {
            buildUserAuth(0);
        }

    }



    public void buildUserAuth(int type) {
       //todo: unlock your local wallet logic
        
        
    }




    public void sendDismiss() {
        MessageEvent msg = new MessageEvent("dismiss", EventType.EVENT_TYPE_POPUP_DISMISS);
        EventBus.getDefault().post(msg);

    }
}

