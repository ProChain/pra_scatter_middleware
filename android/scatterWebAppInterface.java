
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.greenrobot.eventbus.EventBus;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import pro.chain.wallet.MessageBus.EventType;
import pro.chain.wallet.MessageBus.MessageEvent;
import pro.chain.wallet.utils.DataCenter.fancyDataCenter;

/**
 * Created by alex on 2018/7/23.
 */

public class scatterWebAppInterface {


   
    @JavascriptInterface
    public String getIdentity( String contract,
                               String action,
                               String param,
                               String callbackStr) {
        // do something

        String account = fancyDataCenter.getInstance().getEOSAccount();
        String publickey = fancyDataCenter.getInstance().getEOSPublicKey();
        if (account == null) {
            account = "";
        }


        MessageEvent msg = new MessageEvent(callbackStr, EventType.EVENT_TYPE_EOS_SCATTER_CALLBACK);
        msg.param1 = account + "," + publickey;


        EventBus.getDefault().post(msg);
        return account;
    }

    @JavascriptInterface
    public void forgetIdentity(String contract,
                               String action,
                               String param,
                               String callbackStr)
    {


        MessageEvent msg = new MessageEvent(callbackStr, EventType.EVENT_TYPE_EOS_SCATTER_CALLBACK);
        msg.param1 = "true";

        EventBus.getDefault().post(msg);
    }

    @JavascriptInterface
    public String authenticate(String contract,
                               String action,
                               String param,
                               String callbackStr)
    {

        EOSFancyManager fancyManager = EOSFancyManager.getInstance();

        String key = EOSFancyManager.getInstance().getUserAuth();

        String signature =  fancyManager.signWithPrivateKey(param, key);

        MessageEvent msg = new MessageEvent(callbackStr, EventType.EVENT_TYPE_EOS_SCATTER_CALLBACK);
        msg.param1 = signature;


        EventBus.getDefault().post(msg);

        return "";
    }

  

    @JavascriptInterface
    public String getArbitrarySignature(String contract,
                                        String action,
                                        String param,
                                        String callbackStr)
    {


        EOSFancyManager fancyManager = EOSFancyManager.getInstance();

        JSONObject json_result = JSONObject.parseObject(param);

        String data = json_result.getString("data");
      

        String key = EOSFancyManager.getInstance().getUserAuth();

        String signature =  fancyManager.signWithPrivateKey(data, key);

        MessageEvent msg = new MessageEvent(callbackStr, EventType.EVENT_TYPE_EOS_SCATTER_CALLBACK);
        msg.param1 = signature;


        EventBus.getDefault().post(msg);

        return "";

    }







    @JavascriptInterface
    public void transaction(String contract,
                         String action,
                         String param,
                         String callbackStr)
    {


        JSONObject json_result = JSONObject.parseObject(param);
        JSONArray actions = json_result.getJSONArray("actions");
        JSONObject actionOBJ = actions.getJSONObject(0);

        contract = actionOBJ.getString("account");
        action = actionOBJ.getString("name");
        String data = actionOBJ.getString("data");


        //permisstions : todo : need to anaylst whethr
        String account = fancyDataCenter.getInstance().getEOSAccount();
        String[] permission = new String[1];
        permission[0] = new String(account + "@active");

        EOSFancyManager fancyManager = EOSFancyManager.getInstance();

       

        //mDataManager.
        fancyManager.mDataManager.pushAction(contract, action, data, permission).subscribe(new Observer<PushTxnResponse>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(PushTxnResponse pushTxnResponse) {

              
                MessageEvent msg = new MessageEvent( callbackStr, EventType.EVENT_TYPE_EOS_SCATTER_CALLBACK);
                msg.param1 = pushTxnResponse.toString();

                EventBus.getDefault().post(msg);

            }

            @Override
            public void onError(Throwable e) {

                retrofit2.adapter.rxjava2.HttpException exception = (retrofit2.adapter.rxjava2.HttpException) e;

                String error = "";
                try {
                    error = exception.response().errorBody().string();
                } catch (Exception locale) {

                }
               
                MessageEvent msg = new MessageEvent(callbackStr, EventType.EVENT_TYPE_EOS_SCATTER_CALLBACK);
                msg.param1 = error;
                EventBus.getDefault().post(msg);

            }

            @Override
            public void onComplete() {

            }
        });

    }








}
