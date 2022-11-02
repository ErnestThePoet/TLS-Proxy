# TLS-Proxy

使用Java编写的HTTP代理服务器，实现了TLS1.3中的全部主要[加密认证流程](https://tls13.xargs.org/)，可为HTTP请求的数据传输提供安全保障。

## 使用方法

### 编辑配置文件
在开始使用前，需要配置代理服务器的运行选项。`configs`目录下的`configs_client.json`和`configs_server`分别是客户端和服务端的配置文件。

客户端设置选项说明如下：  
    * `port`:`number`,客户端代理服务器的监听端口号；
    * `targetHostPatterns`:`Array<string>`,正则表达式字符串数组，指定了所有需要被加密代理的主机名。所有发往其余主机的请求都会被代理服务器丢弃。

服务端设置选项说明如下：  
    * `port`:`number`,服务端代理服务器的监听端口号；
    * `proxyPasses`:`Array<{location:string;pass:string;}>`,代理转发规则列表。每个元素的`location`属性都是正则表达式，若某元素的`location`属性匹配了请求头中的主机名，那么该请求头中的主机名将被改写为`pass`属性中的值，该请求也将被转发给`pass`属性指定的主机。

### 编写证书提供与校验类
本项目不提供统一的证书格式标准，您需要自行实现`certificate`包中的`CertificateProvider`和`CertificateValidator`接口，实现您自己的证书提供和校验规则。实现后，请相应地修改两个接口的`getInstance`方法使其返回您的实现类对象。请注意TLS1.3中的传输信息签名校验也是证书校验的一部分。

### 启动程序
本代理服务器需要在客户端和服务端同时运行。客户端运行时需要添加命令行参数`CLIENT`，服务端则添加`SERVER`。
在服务端运行一个纯HTTP后端服务，如果您的配置文件正确，那么您就可以在客户端使用浏览器正常访问服务端的HTTP服务。

### 在同一台电脑上调试的说明
本项目也可以在同一台电脑上同时运行客户端与服务端进行调试。为此，您需要修改客户端`RequestHandler`类中的`connectToServer`方法代码，将其中的代码`this.serverSocket= new Socket(host,port);`中的`port`改为服务端代理服务器的运行端口，即确保浏览器访问的端口与服务端代理服务器运行端口不一致即可。
