
//
//  PCScatterViewController.m
//  wallet
//
//  Created by Alex on 2018/10/10.
//  Copyright © 2018年 ProChain. All rights reserved.
//

#import "PCScatterViewController.h"
#import <WebKit/WebKit.h>
#import <Masonry/Masonry.h>
#import <ReactiveObjC/ReactiveObjC.h>
#import <ReactiveObjC/RACEXTScope.h>
#import "transferModel.h"
#import "scatterAction.h"



@interface PCScatterViewController ()<WKNavigationDelegate,WKScriptMessageHandler>
@property (strong, nonatomic) WKWebView *webview;
@property (assign, nonatomic) int EOSNET_TYPE;
@end

@implementation PCScatterViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    
    
    [self setTitle:self.title];
    
   
    WKWebViewConfiguration *config = [[WKWebViewConfiguration alloc] init];
    
    //注册js方法
    [config.userContentController addScriptMessageHandler:self name: @"Prochain"];
    
    
    //config.preferences.minimumFontSize = 8;
    _webview = [[WKWebView alloc] initWithFrame:CGRectZero configuration:config];
    [_webview setNavigationDelegate:self];
    [self.view addSubview:_webview];
    [_webview mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.equalTo(self.view);
        make.left.equalTo(self.view);
        make.bottom.equalTo(self.view);
        make.right.equalTo(self.view);
    }];
  
    
    
     [self loadData];
    // Do any additional setup after loading the view.
}



- (void)loadData {
    [_webview loadRequest:[NSURLRequest requestWithURL:[NSURL URLWithString:self.url]]];
}


//加载完成
- (void)webView:(WKWebView *)webView didFinishNavigation:(WKNavigation *)navigation {
    NSLog(@"加载完成");
    
    NSString *path = [[NSBundle mainBundle]
                       pathForResource:@"pra_middleware.js" ofType:nil];
    
    
    
    NSData *data = [NSData dataWithContentsOfFile:path];
    NSString * str  =[[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    NSLog(@"alex_huang %@", str);
    str = [str stringByAppendingString:@";window.scatter = new Pra();"];
    
    [self.webview evaluateJavaScript:str  completionHandler:^(id _Nullable response, NSError * _Nullable error) {
        
        NSLog(@"call back ");
    }];
    
    NSString* loadedString = [@"" stringByAppendingFormat:@"%@%@%@%@%@",
                              @"let event = new CustomEvent(\"scatterloaded\",{" ,
                              @"            detail: {\n",
                              @"                hazcheeseburger: true//自定义的参数\n",
                              @"            }\n",
                              @"        });document.dispatchEvent(event); " ];
    
    [self.webview evaluateJavaScript:loadedString  completionHandler:^(id _Nullable response, NSError * _Nullable error) {
        
        NSLog(@"call back ");
    }];
    
  
    
}





//实现js调用ios的handle委托
- (void)userContentController:(WKUserContentController *)userContentController didReceiveScriptMessage:(WKScriptMessage *)message {
    NSLog(@"%@",NSStringFromSelector(_cmd));
    NSLog(@"%@",message.body);
    
    
    NSDictionary* dict = (NSDictionary*)message.body;
    
    NSString* method = [dict valueForKey:@"method"];
    
    
   
    //{{scatter support
    
    if([method isEqualToString:@"getIdentity"])
    {
        NSString* callback = [dict valueForKey:@"callback"];
        [self scatter_getIdentity:callback];
    }
    if([method isEqualToString:@"forgetIdentity"])
    {
        NSString* callback = [dict valueForKey:@"callback"];
        [self scatter_ForgetIdentity:callback];
    }
    if([method isEqualToString:@"authenticate"])
    {
        NSString* callback = [dict valueForKey:@"callback"];
        [self scatter_authenticate:@"" withCallback:callback];
    }
    
    if ([method isEqualToString:@"transaction"]) {
        NSString* callback = [dict valueForKey:@"callback"];
        NSString* params = [dict valueForKey:@"params"];
        
        
        NSData *jsonData = [params dataUsingEncoding:NSUTF8StringEncoding];
        NSError *err;
        NSDictionary *actionsDict = [NSJSONSerialization JSONObjectWithData:jsonData
                                                            options:NSJSONReadingAllowFragments
                                                              error:&err];
        if(err)
        {
            //decode json fail.
            return;
        }
        NSArray* actions = [actionsDict objectForKey:@"actions"];
        NSDictionary* firstAction = [actions objectAtIndex:0];
        
        
        if([self checkWalletLock:callback withParams:firstAction type:0])
        {
           
        }
        
        
    }
    
    if([method isEqualToString:@"getArbitrarySignature"])
    {
        NSString* callback = [dict valueForKey:@"callback"];
        NSString* params = [dict valueForKey:@"params"];
        
        
        NSData *jsonData = [params dataUsingEncoding:NSUTF8StringEncoding];
        NSError *err;
        NSDictionary *actionsDict = [NSJSONSerialization JSONObjectWithData:jsonData
                                                                    options:NSJSONReadingAllowFragments
                                                                      error:&err];
        
    
        if(err)
        {
            //decode json fail.
            return;
        }
        NSString* data = [actionsDict objectForKey:@"data"];
        
        NSDictionary* dict =@{@"data":data};
        if([self checkWalletLock:callback withParams:dict type:1])
        {
            
        }
       
        
    }
    
    
    
    //}}end of scatter support
    
   
   
    
    
}


-(void)scatter_signData:(NSString*)data callback:(NSString*)callbackUrl;
{
    [[SELocalAccount currentAccount] signWithData:[data dataUsingEncoding:NSUTF8StringEncoding] unlockOncePasscode:nil
                                    completion:^(NSString * signature, NSError * error) {
        NSString * jsCode = [@"" stringByAppendingFormat: @"%@('param1')" ,callbackUrl];
        NSString* message = [@"" stringByAppendingFormat:@"%@", signature];
        jsCode = [jsCode stringByReplacingOccurrencesOfString:@"param1" withString:message];
        
        [self.webview evaluateJavaScript:jsCode  completionHandler:^(id _Nullable response, NSError * _Nullable error) {
            
            NSLog(@"call back ");
        }];
    }];
    
    
}

-(void)scatter_getIdentity:(NSString*)callbackUrl
{
    NSString *savedAccount = [[PCInfoSaver userDefaults] objectForKey:PC_UDKEY_ACCOUNT_EOS];
    NSString*  publicKey = [SELocalAccount currentAccount].rawPublicKey;
    //signup
    NSString * jsCode = [@"" stringByAppendingFormat: @"%@('param1')" ,callbackUrl ];
    
    NSString* message = [@"" stringByAppendingFormat:@"%@,%@", savedAccount, publicKey];
    jsCode = [jsCode stringByReplacingOccurrencesOfString:@"param1" withString:message];
    
    [self.webview evaluateJavaScript:jsCode  completionHandler:^(id _Nullable response, NSError * _Nullable error) {
        
        NSLog(@"call back ");
    }];
    
}


-(void)scatter_ForgetIdentity:(NSString*)callbackUrl
{
    NSString * jsCode = [@"" stringByAppendingFormat: @"%@('param1')" ,callbackUrl ];
    NSString* message = [@"" stringByAppendingFormat:@"%@", @"true"];
    jsCode = [jsCode stringByReplacingOccurrencesOfString:@"param1" withString:message];
    
    [self.webview evaluateJavaScript:jsCode  completionHandler:^(id _Nullable response, NSError * _Nullable error) {
        
        NSLog(@"call back ");
    }];
}


-(void)scatter_authenticate:(NSString*)key withCallback:(NSString*)callbackUrl
{
    NSData* data = [key dataUsingEncoding:NSUTF8StringEncoding];
    [[SELocalAccount currentAccount] signWithData:data unlockOncePasscode:@"" completion:^(NSString *trans, NSError *error) {
        
    }];
}

-(void)scatter_transaction:(NSString*)callbackUrl withParams:(NSDictionary*)params
{
    NSString *savedAccount = [[PCInfoSaver userDefaults] objectForKey:PC_UDKEY_ACCOUNT_EOS];
    
    
    NSString* account = [params objectForKey:@"account"];
    NSString* name = [params objectForKey:@"name"];
    NSDictionary* data= [params objectForKey:@"data"];
    
    
    NSString* dataJson = [PCFancyUtil DataTOjsonString:data];
    dataJson = [dataJson stringByReplacingOccurrencesOfString:@"\n" withString:@""];
    NSError *error = nil;
    AbiJson* abi = [[AbiJson alloc] initWithCode:account action:name json:dataJson error:&error];
    [[SELocalAccount currentAccount] pushTransactionWithAbi:abi account:savedAccount unlockOncePasscode:nil completion:^(TransactionResult *trans, NSError *error) {
        
        if (error!=NULL) {
            RPCErrorResponse* walletError = error.userInfo[RPCErrorResponse.ErrorKey];
            NSString* errorString = [@"" stringByAppendingFormat:@"%ld , %ld, %@ , %@", walletError.code, walletError.error.code,walletError.error.name, walletError.errorDescription];
            NSString * jsCode = @"callback('param1')";
            
            

            jsCode = [jsCode stringByReplacingOccurrencesOfString:@"callback" withString:callbackUrl];
            jsCode = [jsCode stringByReplacingOccurrencesOfString:@"param1" withString:errorString];
            jsCode = [jsCode stringByReplacingOccurrencesOfString:@"\n" withString:@""];
            jsCode = [jsCode stringByReplacingOccurrencesOfString:@" " withString:@""];
            [self.webview evaluateJavaScript:jsCode  completionHandler:^(id _Nullable response, NSError * _Nullable error) {
                
                NSLog(@"call back ");
            }];
        }
        else{
            NSLog(@"alex signup success: %@", trans.transactionId);
            
            //signup
            NSString * jsCode = @"callback('param1')";
            jsCode = [jsCode stringByReplacingOccurrencesOfString:@"callback" withString:callbackUrl];
            jsCode = [jsCode stringByReplacingOccurrencesOfString:@"param1" withString:trans.transactionId];
            
            [self.webview evaluateJavaScript:jsCode  completionHandler:^(id _Nullable response, NSError * _Nullable error) {
                
                NSLog(@"call back ");
            }];
            
        }
    }];
}










@end
