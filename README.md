# pra_scatter_middleware
a scatter protocol sample for mobile on EOS chain


(1)About scatter

scatter is the most used wallet protocol on EOS chain. 

Scatter is a browser extension that allows you to sign transactions for multiple blockchains and provide personal information to web applications without ever exposing your keys or filling out forms.

https://get-scatter.com/

https://github.com/GetScatter/ScatterWebExtension



(2)why scatter_middleware

while scatter is famous on Chrome as a plugin,  it is not friendly in mobile currently, there are more than 30 EOS mobile wallet in the market, but the dapps are usually deploy as web version first, 
this middle sovle the problem of the web dapp [EOS] 's sign transaction problem while running on mobile browser.




====================================================

(3)how to use

[3.1] For iOS

1. Copy pra_middleware.js file into your project,

2. implementation a scatterWebview controller similar with the on in iOS/PCScatterViewContrller
you need to using your local native EOS wallet to replace the folowing function


-(void)scatter_getIdentity:(NSString*)callbackUrl

-(void)scatter_ForgetIdentity:(NSString*)callbackUrl

-(void)scatter_authenticate:(NSString*)key withCallback:(NSString*)callbackUrl

-(void)scatter_getArbitrarySignature:(NSString*)signData;

-(void)scatter_transaction:(NSString*)callbackUrl withParams:(NSDictionary*)params

3. using your customize webview to load any url support scatter protocol ,
after the remote url is finish loaded, load the pra_middleware.js in the webview, to hook the scatter obj,
then you can using your native wallet to 
play with EOS chain.






=====================================================

[3.2] For Android


1. Copy pra_middleware.js file into your project,
2. implemetation web interface contains the following logic

scatterWebInterface:

 @JavascriptInterface
    public String getIdentity( String contract,
                               String action,
                               String param,
                               String callbackStr);
                               
                               
                               
                               
                               
@JavascriptInterface
    public void forgetIdentity(String contract,
                               String action,
                               String param,
                               String callbackStr)
                               



@JavascriptInterface
    public String authenticate(String contract,
                               String action,
                               String param,
                               String callbackStr)





@JavascriptInterface
    public String getArbitrarySignature(String contract,
                                        String action,
                                        String param,
                                        String callbackStr)
                                        



@JavascriptInterface
    public void transaction(String contract,
                         String action,
                         String param,
                         String callbackStr)
                         
                         
scatterWebview:

implemetation the js inject, init the webview , 


3.  using your customize webview to load any url support scatter protocol ,
after the remote url is finish loaded, load the pra_middleware.js in the webview, to hook the scatter obj,
then you can using your native wallet to 
play with EOS chain.

