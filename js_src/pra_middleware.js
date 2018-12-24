/*自定义promise*/
function MyPromise(fn) {
  this.value;
  this.status = 'pending';
  this.resolveFunc = function () { };
  this.rejectFunc = function () { };
  fn(this.resolve.bind(this), this.reject.bind(this));
}

MyPromise.prototype = {
  resolve: function (val) {
    var self = this;
    if (this.status == 'pending') {
      this.status = 'resolved';
      this.value = val;
      setTimeout(function () {
        self.resolveFunc(self.value);
      }, 0);
    }
  },
  reject: function (val) {
    var self = this;
    if (this.status == 'pending') {
      this.status = 'rejected';
      this.value = val;
      setTimeout(function () {
        self.rejectFunc(self.value);
      }, 0);
    }
  },
  then: function (resolveFunc, rejectFunc) {
    var self = this;
    return new MyPromise(function (resolve_next, reject_next) {
      function resolveFuncWrap() {
        var result = resolveFunc(self.value);
        if (result && typeof result.then === 'function') {
          result.then(resolve_next, reject_next);
        } else {
          resolve_next(result);
        }
      }
      function rejectFuncWrap() {
        if (typeof rejectFunc !== 'function') {
          rejectFunc = function () { return self.value };
        }
        var result = rejectFunc(self.value);
        if (result && typeof result.then === 'function') {
          result.then(resolve_next, reject_next);
        } else {
          reject_next(result);
        }
      }
      self.resolveFunc = resolveFuncWrap;
      self.rejectFunc = rejectFuncWrap;
    })
  },
  catch: function (resolveFunc) {
    var self = this;
    return new MyPromise(function (resolve_next, reject_next) {
      function resolveFuncWrap() {
        if (self.status !== 'resolved') {
          var result = resolveFunc(self.value);
          if (result && typeof result.then === 'function') {
            result.then(resolve_next, reject_next);
          } else {
            resolve_next(result);
          }
        }
      }

      function rejectFuncWrap() {
        var result = resolveFunc(self.value);
        if (result && typeof result.then === 'function') {
          result.then(resolve_next, reject_next);
        } else {
          resolve_next(result);
        }
      }
      self.resolveFunc = resolveFuncWrap;
      self.rejectFunc = rejectFuncWrap;
    })
  }
}

/*
***生成随机回调函数名称***
*/
var getCallbackName = function () {
  var random = parseInt(Math.random() * 100000)
  return 'pra_callback_' + new Date().getTime() + random
}

/*
***判断客户端类型***
*/
var getOperatingSystem = function () {
  var u = navigator.userAgent
  var isAndroid = u.indexOf('Android') > -1 || u.indexOf('Adr') > -1 // android 终端
  var isIOS = !!u.match(/\(i[^;]+;( U;)? CPU.+Mac OS X/) // ios 终端
  if (isAndroid) {
    return 'Android'
  } else if (isIOS) {
    return 'iOS'
  }
}

/*
***自定义事件兼容性处理***
*/
var createCustomEvent = function () {
  if (typeof window.CustomEvent === 'function') return false
  function CustomEvent(event, params) {
    params = params || {
      bubbles: false,
      cancelable: false,
      detail: undefined
    }
    var evt = document.createEvent('CustomEvent')
    evt.initCustomEvent(event, params.bubbles, params.cancelable, params.detail)
    return evt
  }
  CustomEvent.prototype = window.Event.prototype
  window.CustomEvent = CustomEvent
}

/*
***********转发请求到原生************
***@methodName 方法名，字符串，不可为空
***@contractName 合约名，字符串，可为空
***@actionName  行为名，字符串，可为空
***@params 附加参数，json字符串，可为空
***@cbName 回调函数名称，字符串，不可为空
*/
var sendPraRequest = function (methodName, contractName, actionName, params, cbName) {
  var device_type = getOperatingSystem()
  if (device_type === 'Android') {
    //Prochain[methodName](contractName,actionName,params,cbName)
  } else if (device_type === 'iOS') {
    var message = {
      'method': methodName,
      'contract_name': contractName,
      'action_name': actionName,
      'params': params,
      'callback': cbName
    }
    window.webkit.messageHandlers.Prochain.postMessage(message)
  }
  window.postMessage({ type: methodName, params: params, msg: cbName }, '*');
}

/*
*********实现eos的一系列方法*********
***@network 网络配置参数，对象，不可为空
***@_eos 原生Eos，不可为空
***@options 配置项，对象，可为空
***对transaction、transfer、contract及其子方法进行拦截转发***
*/
var eos = function (network, _eos, options) {
  var chainId = network.chainId;
  var protocol = network.protocol ? network.protocol : 'https';
  var port = network.port != '' ? ':' + network.port : '';
  var httpEndpoint = protocol + '://' + network.host + port;
  console.log(httpEndpoint)
  return new Proxy(_eos({ httpEndpoint: httpEndpoint, chainId: chainId }), {
    get: function (obj, prop) {
      if (prop == 'transaction') {
        return function () {
          var params = Array.prototype.slice.apply(arguments);
          var paramStr, callback, callbackType,cb;
          switch (params.length) {
            case 1:
              let [param] = params;
              if (typeof param === 'function') {
                paramStr = 'eosio.token';
                callback = param;
                callbackType = 0;
              } else {
                paramStr = JSON.stringify(param);
              }
              break;
            case 2:
              const [first, second] = params;
              const pType = Object.prototype.toString.call(first);
              if (pType.search(/function/i) > -1) {
                paramStr = JSON.stringify({
                  contract_name: 'eosio.token',
                  options: second
                });
                callback = first;
                callbackType = 1;
              } else if (pType.search(/(object object)/i) > -1) {
                if (typeof second === 'function') {
                  paramStr = JSON.stringify(first);
                  callback = second;
                  callbackType = 2;
                } else {
                  paramStr = JSON.stringify({
                    actions: first,
                    options: second
                  });
                }
              } else if (pType.search(/array/i) > -1) {
                paramStr = JSON.stringify(first);
                callback = second;
                callbackType = 3;
              } else {
                paramStr = JSON.stringify(first);
                callback = second;
                callbackType = 4;
              }
              break;
            case 3:
              let callback1;
              paramStr = JSON.stringify(params[1]);
              callback = params[0];
              cb = params[2];
              callbackType = 5;
              break;
          }
          return new MyPromise(function (resolve, reject) {
            var praCallbackFun = getCallbackName();
            window[praCallbackFun] = function (result) {
              try {
                if (result.indexOf('error') > -1) {
                  reject(result);
                  return;
                }
                var res = {
                  broadcast: true,
                  processed: {},
                  transaction: {
                    compression: "none",
                    signatures: ['SIG_K1_Jvs9Goiw56LXPT2SSB2vcAp6x2Q4idiccZ7bv54oKMu3H2u1yAbc9XEDESp5Z6sqARSpnMBYjMeWyCusbmseDy4ctY8N3r'],
                    transaction: {}
                  },
                  transaction_id: ""
                };
                result = result.replace(/\s+/g, "");
                if (result != '') {
                  if (result.search(/error/i) > -1) {
                    res.broadcast = false;
                    res.error = result;
                  } else {
                    result = result.split('transaction:');
                    res.transaction_id = result[1];
                    res.broadcast = true;
                  }
                } else {
                  res.broadcast = false;
                }
                res = new Proxy(res, {
                  get: function (obj, prop) {
                    if (obj[prop] === undefined) {
                      return new Proxy({}, {
                        get: function (obj_c, prop_c) {
                          if (obj_c[prop_c] === undefined) {
                            return function () {
                              var values = Array.prototype.slice.apply(arguments);
                              var params = JSON.stringify(values);
                              return new Promise(function (resolve, reject) {
                                var praCallbackFun = getCallbackName();
                                window[praCallbackFun] = function (res) {
                                  try {
                                    resolve(res);
                                  } catch (e) {
                                    reject(e);
                                  }
                                }
                                sendPraRequest('contract_all', prop, prop_c, params, praCallbackFun);
                              });
                            }
                          }
                          return obj_c[prop_c]
                        }
                      });
                    }
                    return obj[prop]
                  }
                });
                resolve(res);
                if (callback) {
                  if (callbackType === 0 || callbackType === 1 || callbackType === 4) {
                    callback(res.__any__);
                  } else if (callbackType === 2) {
                    callback = callback.length === 1 ? callback(res) : callback(null, res);
                  } else if (callbackType === 3) {
                    callback(res);
                  } else {
                    callback(res.__any__);
                    cb(res);
                  }
                }
              } catch (e) {
                reject(e);
              }
            }
            sendPraRequest('transaction', 'eosio.token', '', paramStr, praCallbackFun);
          });
        }
      } else if (prop == 'transfer') {
        return function () {
          var param = Array.prototype.slice.apply(arguments);
          var callback;
          if (param.length > 1) {
            if (typeof param[0] === 'object') {
              params = param[0]
            } else {
              var params = {
                from: param[0],
                to: param[1],
                quantity: param[2],
                memo: param[3]
              }
              if (typeof param[4] === 'function') {
                callback = param[4];
              }
            }
          } else {
            params = param[0]
          }
          params = JSON.stringify(params);
          return new Promise(function (resolve, reject) {
            var praCallbackFun = getCallbackName();
            window[praCallbackFun] = function (result) {
              try {
                if (result.indexOf('error') > -1) {
                  reject(result);
                } else {
                  var res = {
                    result: true,
                    transaction_id: ""
                  };
                  result = result.replace(/\s+/g, "");
                  if (result != '') {
                    result = result.split('transaction:');
                    res.transaction_id = result[1];
                    res.result = true;
                  } else {
                    res.result = false;
                  }
                  resolve(res);
                  if (callback) callback(null, res);
                }
              } catch (e) {
                reject(e);
                if (callback) callback(e)
              }
            }
            sendPraRequest('transfer', 'eosio.token', '', params, praCallbackFun);
          });
        }
      } else if (prop == 'contract') {
        return function () {
          var params = Array.prototype.slice.apply(arguments);
          var contract_name = params[0], callback, paramStr;
          switch (params.length) {
            case 1:
              callback = null;
              paramStr = JSON.stringify({
                contract_name: contract_name
              });
              break;
            case 2:
              if (typeof params[1] === 'function') {
                callback = params[1];
                paramStr = JSON.stringify({
                  contract_name: contract_name
                });
              } else {
                callback = null
                paramStr = JSON.stringify({
                  contract_name: contract_name,
                  options: params[1]
                });
              }
              break;
            case 3:
              callback = params[2];
              paramStr = JSON.stringify({
                contract_name: contract_name,
                options: params[1]
              });
              break;
          }
          return new MyPromise(function (resolve, reject) {
            var praCallbackFun = getCallbackName();
            window[praCallbackFun] = function (res) {
              var contract = new Proxy({}, {
                get: function (obj, prop) {
                  if (prop === 'then') {
                    return new Promise(function (resolve, reject) {
                      var praCallbackFun = getCallbackName();
                      window[praCallbackFun] = function (result) {
                        try {
                          resolve(res);
                        } catch (e) {
                          reject(e);
                        }
                      }
                      sendPraRequest('contract_all', contract_name, prop, '', praCallbackFun);
                    });
                  }
                  if (obj[prop] === undefined) {
                    return function () {
                      var values = Array.prototype.slice.apply(arguments);
                      var params = JSON.stringify(values);
                      return new Promise(function (resolve, reject) {
                        var praCallbackFun = getCallbackName();
                        window[praCallbackFun] = function (res) {
                          try {
                            if (typeof res === 'string' && res.indexOf('error') > -1) {
                              reject(res)
                            } else {
                              resolve(res);
                            }
                          } catch (e) {
                            reject(e);
                          }
                        }
                        sendPraRequest('contract_all', contract_name, prop, params, praCallbackFun);
                      });
                    }
                  }
                  return obj[prop]
                }
              });

              try {
                resolve(contract);
                if (callback) callback(null, contract);
              } catch (e) {
                reject(e);
                if (callback) callback(e);
              }
            }
            sendPraRequest('contract', '', '', paramStr, praCallbackFun);
          });
        }
      }
      return obj[prop];
    }
  })
}

function Pra() {
  this.identity = null;
  this.eos = eos;
  this.isExtension = true;
  this.version = '1.0.0';
  createCustomEvent();
  var event = new CustomEvent(
    'scatterLoaded', {
      'detail': { 'hazcheeseburger': true }
    }
  );
  setTimeout(function () {
    document.dispatchEvent(event);
  }, 1000);

  window.addEventListener('message', function (event) {
    if (event.data.type == 'callback') {
      var name = event.data.name;
      // console.log('callbackname:' + name)
      window[name](event.data.msg)
    }
  }, false);
}

/*
***@appname app名称，字符串，不可为空
***@options 配置项，对象，可为空
***@return 返回布尔值，true或false
*/
Pra.prototype.connect = function (appname, options) {
  if (!appname || !appname.length) throw new Error("You must specify a name for this connection");
  options = Object.assign({ initTimeout: 10000, linkTimeout: 30000 }, options);
  options = JSON.stringify(options);
  this.isExtension = false;
  return new Promise(function (resolve, reject) {
    var praCallbackFun = getCallbackName();
    window[praCallbackFun] = function (res) {
      try {
        res = JSON.parse(res);
        resolve(res);
      } catch (e) {
        reject(e);
      }
    }
    sendPraRequest('connect', '', '', options, praCallbackFun);
  });
}

/*
***断开连接，无参数***
*/
Pra.prototype.disconnect = function () {
  return new Promise(function (resolve, reject) {
    var praCallbackFun = getCallbackName();
    window[praCallbackFun] = function (res) {
      try {
        resolve(res);
      } catch (e) {
        reject(e);
      }
    }
    sendPraRequest('disconnect', '', '', '', praCallbackFun);
  });
}

/*
***判断scatter是否连接成功，无参数***
*/
Pra.prototype.isConnected = function () {
  return new Promise(function (resolve, reject) {
    var praCallbackFun = getCallbackName();
    window[praCallbackFun] = function (res) {
      try {
        resolve(res);
      } catch (e) {
        reject(e);
      }
    }
    if (window.scatter) window[praCallbackFun](true);
  });
}

/*
***判断密钥是否成对，无参数***
*/
Pra.prototype.isPaired = function () {
  return new Promise(function (resolve, reject) {
    var praCallbackFun = getCallbackName();
    window[praCallbackFun] = function (res) {
      try {
        resolve(res);
      } catch (e) {
        reject(e);
      }
    }
    if (window.scatter.identity.publicKey) window[praCallbackFun](true);
  });
}

/*
***获得scatter版本***
*/
Pra.prototype.getVersion = function () {
  var self = this;
  return new Promise(function (resolve, reject) {
    var praCallbackFun = getCallbackName();
    window[praCallbackFun] = function (res) {
      try {
        resolve(res);
      } catch (e) {
        reject(e);
      }
    }
    window[praCallbackFun](self.version);
  });
}

/*
***@fields 请求的字段，对象，可为空
***@return 返回identity对象
*/
Pra.prototype.getIdentity = function (fields) {
  var self = this;
  fields = typeof fields === 'object' ? JSON.stringify(fields) : fields;
  return new Promise(function (resolve, reject) {
    var praCallbackFun = getCallbackName();
    window[praCallbackFun] = function (identity) {
      try {
        var res = {
          hash: '8391a38e0df2a7d94518b6ecabf0ce03925b34e5eb65c4a246b38a1bf6085348',
          kyc: false,
          name: 'RandomRagdoll5342476',
          publicKey: '',
          accounts: [{
            authority: 'active',
            blockchain: 'eos',
            name: ''
          }]
        };
        identity = identity.split(',');
        res.accounts[0].name = identity[0];
        res.publicKey = identity[1];
        self.identity = res;
        resolve(res);
      } catch (e) {
        reject(e);
      }
    }
    sendPraRequest('getIdentity', '', '', fields, praCallbackFun);
  });
}

/*
***@无参数
***@从已授权账户返回identity对象
*/
Pra.prototype.getIdentityFromPermissions = function () {
  var self = this;
  return new Promise(function (resolve, reject) {
    var praCallbackFun = getCallbackName();
    window[praCallbackFun] = function (identity) {
      try {
        var res = {
          hash: '8391a38e0df2a7d94518b6ecabf0ce03925b34e5eb65c4a246b38a1bf6085348',
          kyc: false,
          name: 'RandomRagdoll5342476',
          publicKey: '',
          accounts: [{
            authority: 'active',
            blockchain: 'eos',
            name: ''
          }]
        };
        identity = identity.split(',');
        res.accounts[0].name = identity[0];
        res.publicKey = identity[1];
        self.identity = res;
        resolve(res);
      } catch (e) {
        reject(e);
      }
    }
    sendPraRequest('getIdentityFromPermissions', '', '', '', praCallbackFun);
  });
}

/*
***@str 12位随机字符串，不可为空
***@return 返回signedOrigin
*/
Pra.prototype.authenticate = function (str) {
  return new Promise(function (resolve, reject) {
    var praCallbackFun = getCallbackName();
    window[praCallbackFun] = function (result) {
      try {
        resolve(result);
      } catch (e) {
        reject(e);
      }
    }
    sendPraRequest('authenticate', '', '', str, praCallbackFun);
  });
}

/*
***@network 网络配置参数，对象，不可为空
***@return 返回被添加的网络
*/
Pra.prototype.suggestNetwork = function (network) {
  param = typeof network === 'object' ? JSON.stringify(network) : network;
  return new Promise(function (resolve, reject) {
    var praCallbackFun = getCallbackName();
    window[praCallbackFun] = function (result) {
      try {
        resolve(result);
      } catch (e) {
        reject(e);
      }
    }
    sendPraRequest('suggestNetwork', '', '', param, praCallbackFun);
  });
}

/*
***退出用户授权，无参数***
*/
Pra.prototype.forgetIdentity = function () {
  return new Promise(function (resolve, reject) {
    var praCallbackFun = getCallbackName();
    window[praCallbackFun] = function (result) {
      try {
        resolve(result);
      } catch (e) {
        reject(e);
      }
    }
    sendPraRequest('forgetIdentity', '', '', '', praCallbackFun);
  })
}

/*
***发起任意签名
***@publickKey 账户公钥，字符串，不可为空
***@data 数据，任意类型，不可为空
***@whatFor 发起签名的原因，字符串，可为空
***@isHash 是否为hash，布尔值，只有当data类型为hash时才可设为true
***@return 返回签名
*/
Pra.prototype.getArbitrarySignature = function (publicKey, data, whatFor, isHash) {
  var params = JSON.stringify({
    publick_key: publicKey,
    data: data,
    what_for: whatFor,
    is_hash: isHash
  });
  return new Promise(function (resolve, reject) {
    var praCallbackFun = getCallbackName();
    window[praCallbackFun] = function (result) {
      try {
        resolve(result);
      } catch (e) {
        reject(e);
      }
    }
    sendPraRequest('getArbitrarySignature', '', '', params, praCallbackFun);
  })
}

/*
***获得公钥
***@blockchain 链名，字符串，不可为空
***@return 返回公钥
*/
Pra.prototype.getPublicKey = function (blockchain) {
  return new Promise(function (resolve, reject) {
    var praCallbackFun = getCallbackName();
    window[praCallbackFun] = function (res) {
      try {
        resolve(res);
      } catch (e) {
        reject(e);
      }
    }
    window[praCallbackFun](window.scatter.identity.publicKey);
  });
}

if (typeof window !== 'undefined') {
  window.scatter = new Pra();
  try {
    if (typeof ScatterJS !== 'undefined') {
      window.ScatterJS = new Proxy(ScatterJS, {
        get: function (obj, prop) {
          if (prop === 'scatter') {
            return window.scatter;
          }
          return obj[prop];
        }
      })
    }
  } catch (e) {
    console.log(e);
    alert('ScatterJS does not exsit')
  }
}
