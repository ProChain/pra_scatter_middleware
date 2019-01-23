package pro.chain.wallet.utils.webview.scatter;

import android.util.Log;
import android.webkit.JavascriptInterface;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fancyios.eoswallet.data.remote.model.api.PushTxnResponse;
import com.fancyios.eoswallet.ui.EOSFancyManager;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import pro.chain.wallet.MessageBus.EventType;
import pro.chain.wallet.MessageBus.MessageEvent;
import pro.chain.wallet.utils.DataCenter.fancyDataCenter;
import pro.chain.wallet.utils.webview.scatter.scatterv1.accountModel;

/**
 * Created by alex on 2018/7/23.
 */

public class scatterWebAppInterface {


    @JavascriptInterface
    public void suggestNetwork(String contract,
                               String action,
                               String param,
                               String callbackStr
    ) {

        MessageEvent msg = new MessageEvent(callbackStr, EventType.EVENT_TYPE_EOS_SCATTER_CALLBACK);
        msg.param1 = "" + "true";


        EventBus.getDefault().post(msg);

    }


    @JavascriptInterface
    public String getIdentity(String contract,
                              String action,
                              String param,
                              String callbackStr) {
        // do something

        Log.d("alex_huang", "web call get account....");
        String account = fancyDataCenter.getInstance().getCurrentEOSAccount();
        String publickey = fancyDataCenter.getInstance().getCurrentEOSPublicKey();
        if (account == null) {
            account = "";
        }


        MessageEvent msg = new MessageEvent(callbackStr, EventType.EVENT_TYPE_EOS_SCATTER_CALLBACK);
        msg.param1 = account + "," + publickey;
        Log.d("alex_huang", "web call get account...."+ msg.param1);

        EventBus.getDefault().post(msg);
        return account;
    }

    @JavascriptInterface
    public void forgetIdentity(String contract,
                               String action,
                               String param,
                               String callbackStr) {


        MessageEvent msg = new MessageEvent(callbackStr, EventType.EVENT_TYPE_EOS_SCATTER_CALLBACK);
        msg.param1 = "true";

        EventBus.getDefault().post(msg);
    }

    @JavascriptInterface
    public String authenticate(String contract,
                               String action,
                               String param,
                               String callbackStr) {

        EOSFancyManager fancyManager = EOSFancyManager.getInstance();

        String key = EOSFancyManager.getInstance().getUserAuth();

        String signature = fancyManager.signWithPrivateKey(param, key);

        MessageEvent msg = new MessageEvent(callbackStr, EventType.EVENT_TYPE_EOS_SCATTER_CALLBACK);
        msg.param1 = signature;


        EventBus.getDefault().post(msg);

        return "";
    }

    @JavascriptInterface
    public String getCurrentWallet() {
        // do something

        Log.d("alex_huang", "web call get account....");
        String account = fancyDataCenter.getInstance().getCurrentEOSAccount();
        if (account == null) {
            account = "";
        }
        return account;
    }


    @JavascriptInterface
    public String getArbitrarySignature(String contract,
                                        String action,
                                        String param,
                                        String callbackStr) {


        EOSFancyManager fancyManager = EOSFancyManager.getInstance();

        JSONObject json_result = JSONObject.parseObject(param);

        String data = json_result.getString("data");
        if (fancyManager.randkey == null || fancyManager.randkey.length() == 0) {
            //need unlock wallet
            MessageEvent msg = new MessageEvent("", EventType.EVENT_TYPE_EOS_UNLOCK_WALLET);
            EventBus.getDefault().post(msg);
            return "";
        }


        String hashvalue = json_result.getString("is_hash");

        String signature;
        if (hashvalue.equals("true"))
        {
            String key = EOSFancyManager.getInstance().getUserAuth();

            signature = fancyManager.signWithPrivateKeyContentHash(data, key);

        }
        else
        {
            String key = EOSFancyManager.getInstance().getUserAuth();

            signature = fancyManager.signWithPrivateKey(data, key);

        }


        MessageEvent msg = new MessageEvent(callbackStr, EventType.EVENT_TYPE_EOS_SCATTER_CALLBACK);
        msg.param1 = signature;


        EventBus.getDefault().post(msg);

        return "";

    }


    @JavascriptInterface
    public void contract(String contract,
                         String action,
                         String param,
                         String callbackStr) {
        MessageEvent msg = new MessageEvent(callbackStr, EventType.EVENT_TYPE_EOS_SCATTER_CALLBACK);
        msg.param1 = "contract_obj";

        EventBus.getDefault().post(msg);
    }



    public String formatParam(String param, String contract, String action)
    {

        JSONArray json_userparam = JSONObject.parseArray(param);
        try{



            //first not a string
            JSONObject first = json_userparam.getJSONObject(0);
            String toString = first.getString("to");
            if (toString.equals("betdiceadmin"))
            {
                ArrayList<String> formatParma = new ArrayList<>();
                formatParma.add(first.getString("from"));
                formatParma.add(first.getString("to"));
                formatParma.add(first.getString("quantity"));
                formatParma.add(first.getString("memo"));

                String paramString = JSON.toJSONString(formatParma);
                return paramString;
            }
            else if (contract.equals("eosio.token") && action.equals("transfer"))
            {
                ArrayList<String> formatParma = new ArrayList<>();
                formatParma.add(first.getString("from"));
                formatParma.add(first.getString("to"));
                formatParma.add(first.getString("quantity"));
                formatParma.add(first.getString("memo"));

                String paramString = JSON.toJSONString(formatParma);
                return paramString;

            }
            return "";

        }catch (Exception e)
        {
            return param;
        }



    }

    @JavascriptInterface
    public void contract_all(String contract,
                             String action,
                             String param,
                             String callbackStr) {

        Log.d("hanzy", "the user params json is " + param);
        if(param==null || param.length()==0)
        {
            //mark empty param skip
            return;
        }

        if (action.equals("toJSON"))
        {
            //skip invalid contract
            return;
        }

        final String formatParam = formatParam(param, contract, action);
        Log.d("hanzy", "the user params json after format " + formatParam);



        EOSFancyManager fancyManager = EOSFancyManager.getInstance();
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();


        accountModel account = new accountModel();
        account.account_name = contract;


        Gson gson = new Gson();
        //使用Gson将对象转换为json字符串
        String json = gson.toJson(account);

        //MediaType  设置Content-Type 标头中包含的媒体类型值
        RequestBody requestBody = FormBody.create(MediaType.parse("application/json; charset=utf-8")
                , json);
        final Request request = new Request.Builder()
                .url("https://block.chain.pro/eos/mainnet/v1/chain/get_abi")//请求的url
                .post(requestBody)
                .build();


        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                Log.d("hanzy", "get abi fail " + e.toString());
                MessageEvent msg = new MessageEvent(callbackStr, EventType.EVENT_TYPE_EOS_SCATTER_CALLBACK);
                msg.param1 = "#error:" + e.toString();
                EventBus.getDefault().post(msg);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.code() == 200 || response.code() == 0) {
                    String result = response.body().string();
                    Log.d("hanzy", "result : " + result);
                    JSONObject json_result = JSONObject.parseObject(result);
                    JSONObject json_abi = json_result.getJSONObject("abi");
                    JSONArray json_structs = json_abi.getJSONArray("structs");

                    for (int i = 0; i < json_structs.size(); i++) {
                        JSONObject actStruct = json_structs.getJSONObject(i);
                        String actionname = actStruct.getString("name");
                        if (actionname.equalsIgnoreCase(action)) {
                            HashMap<String, Object> params = new HashMap<>();

                            JSONArray json_fields = actStruct.getJSONArray("fields");
                            for (int j = 0; j < json_fields.size(); j++) {
                                JSONObject field = json_fields.getJSONObject(j);
                                String name = field.getString("name");
                                String type = field.getString("type");
                                if (type.equalsIgnoreCase("account_name") || type.equalsIgnoreCase("string")
                                        || type.equalsIgnoreCase("asset")
                                        || type.equalsIgnoreCase("name")
                                        ) {
                                    params.put(name, "param" + j);
                                } else {
                                    params.put(name, j);
                                }

                            }

                            String json = JSON.toJSONString(params);
                            Log.d("hanzy", "the params json is " + json);


                            {
                                //Rebuild map
                                JSONArray json_userparam = JSONObject.parseArray(formatParam);
                                for (int k = 0; k < json_userparam.size(); k++) {
                                    String value = json_userparam.getString(k);
                                    json = json.replace("param" + k, value);
                                }

                                Log.d("hanzy", "the combine params json is " + json);
                                contract_push(contract, action, json, callbackStr);
                            }

                        }
                    }

                }
            }
        });


    }


    public void contract_push(String contract, String action, String param, String callbackStr) {


        Log.d("hanzy"," contract_push");
        Log.d("hanzy"," contract =="+contract);
        Log.d("hanzy","action =="+action);
        Log.d("hanzy","param =="+param);
        Log.d("hanzy","callbackStr =="+callbackStr);


        MessageEvent msg = new MessageEvent("", EventType.EVENT_TYPE_SHOW_DIALOG);
        msg.param1=contract;
        msg.param2=action;
        msg.param3 = param;
        msg.param4 = callbackStr;
        msg.method = "contract_push";
        EventBus.getDefault().post(msg);
    }


    @JavascriptInterface
    public void transfer(String contract,
                         String action,
                         String param,
                         String callbackStr) {


        Log.d("hanzy"," transfer");
        Log.d("hanzy"," contract =="+contract);
        Log.d("hanzy","action =="+action);
        Log.d("hanzy","param =="+param);
        Log.d("hanzy","callbackStr =="+callbackStr);


        //permisstions : todo : need to anaylst whethr
        String account = fancyDataCenter.getInstance().getCurrentEOSAccount();
        String[] permission = new String[1];
        permission[0] = new String(account + "@active");


        contract = "eosio.token";
        action = "transfer";


        EOSFancyManager fancyManager = EOSFancyManager.getInstance();
        if (fancyManager.randkey == null || fancyManager.randkey.length() == 0) {
            //need unlock wallet
            MessageEvent msg = new MessageEvent("", EventType.EVENT_TYPE_EOS_UNLOCK_WALLET);
            EventBus.getDefault().post(msg);
            return;
        }


        Log.d("alex_huang", "the transfer params json is " + contract + " ;" + action + " ;" + param + " ;");
        //mDataManager.
        fancyManager.mDataManager.pushAction(contract, action, param, permission).subscribe(new Observer<PushTxnResponse>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(PushTxnResponse pushTxnResponse) {

                Log.d("alex_huang", "push action : " + pushTxnResponse.toString());

                MessageEvent msg = new MessageEvent(callbackStr, EventType.EVENT_TYPE_EOS_SCATTER_CALLBACK);
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
                Log.d("alex_huang", "push action Error: " + error);

                MessageEvent msg = new MessageEvent(callbackStr, EventType.EVENT_TYPE_EOS_SCATTER_CALLBACK);
                msg.param1 = "#error:" + error;
                EventBus.getDefault().post(msg);

            }

            @Override
            public void onComplete() {

            }
        });
    }

    @JavascriptInterface
    public void transaction(String contract,
                            String action,
                            String param,
                            String callbackStr) {

        Log.d("hanzy","transaction  param =="+param);
        Log.d("hanzy","transaction  callbackStr =="+callbackStr);

        if(contract.equals("eosio.token") && param.equals("[\"eosio.token\"]"))
        {
            //skip invalid transaction.
            MessageEvent msg = new MessageEvent(callbackStr, EventType.EVENT_TYPE_EOS_SCATTER_CALLBACK);

            EventBus.getDefault().post(msg);
            return;
        }

        JSONObject json_result = JSONObject.parseObject(param);
        JSONArray actions = json_result.getJSONArray("actions");
        if(actions==null)
        {
            //skip invalid transaction.
            MessageEvent msg = new MessageEvent(callbackStr, EventType.EVENT_TYPE_EOS_SCATTER_CALLBACK);
            EventBus.getDefault().post(msg);
            return;

        }


        MessageEvent msg = new MessageEvent("", EventType.EVENT_TYPE_SHOW_DIALOG);
        msg.param1 = contract;
        msg.param2 = action;
        msg.param3 = param;
        msg.param4 = callbackStr;
        msg.method ="transaction";
        EventBus.getDefault().post(msg);

    }


    @JavascriptInterface
    public void jumpScatter(String url, String title) {
        Log.d("hanzy", "jumpScatter url ==" + url + ", title ==" + title);

        MessageEvent msg = new MessageEvent(url, EventType.EVENT_TYPE_WEB_LOAD_URL);
        msg.param1 = title;
        EventBus.getDefault().post(msg);

    }

    @JavascriptInterface
    public void addFavor(String dappId) {

    }


}

