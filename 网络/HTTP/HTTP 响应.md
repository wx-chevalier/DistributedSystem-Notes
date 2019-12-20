[![返回目录](https://i.postimg.cc/WzXsh0MX/image.png)](https://parg.co/UdT)

# HTTP Response | HTTP 响应详解

![](http://upload-images.jianshu.io/upload_images/1724103-e8ebcab6c80b9044.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

# Header

响应头域允许服务器传递不能放在状态行的附加信息，这些域主要描述服务器的信息和 Request-URI 进一步的信息。响应头域包含 Age、Location 、 Proxy-Authenticate、Public 、 Retry- After、Server 、 Vary、Warning 、 WWW-Authenticate。对响应头域的扩展要求通讯双方都支持，如果存在不支持的响应头 域，一般将会作为实体头域处理。

| Header             | 解释                                                                                | 示例                                                  |
| ------------------ | ----------------------------------------------------------------------------------- | ----------------------------------------------------- |
| Accept-Ranges      | 表明服务器是否支持指定范围请求及哪种类型的分段请求                                  | Accept-Ranges: bytes                                  |
| Age                | 从原始服务器到代理缓存形成的估算时间(以秒计，非负)                                  | Age: 12                                               |
| Allow              | 对某网络资源的有效的请求行为，不允许则返回 405                                      | Allow: GET, HEAD                                      |
| Cache-Control      | 告诉所有的缓存机制是否可以缓存及哪种类型                                            | Cache-Control: no-cache                               |
| Content-Encoding   | web 服务器支持的返回内容压缩编码类型。                                            | Content-Encoding: gzip                                |
| Content-Language   | 响应体的语言                                                                        | Content-Language: en,zh                               |
| Content-Length     | 响应体的长度                                                                        | Content-Length: 348                                   |
| Content-Location   | 请求资源可替代的备用的另一地址                                                      | Content-Location: /index.htm                          |
| Content-MD5        | 返回资源的 MD5 校验值                                                               | Content-MD5: Q2hlY2sgSW50ZWdyaXR5IQ==                 |
| Content-Range      | 在整个返回体中本部分的字节位置                                                      | Content-Range: bytes 21010-47021/47022                |
| Content-Type       | 返回内容的 MIME 类型                                                                | Content-Type: text/html; charset=utf-8                |
| Date               | 原始服务器消息发出的时间                                                            | Date: Tue, 15 Nov 2010 08:12:31 GMT                   |
| ETag               | 请求变量的实体标签的当前值                                                          | ETag: “737060cd8c284d8af7ad3082f209582d”              |
| Expires            | 响应过期的日期和时间                                                                | Expires: Thu, 01 Dec 2010 16:00:00 GMT                |
| Last-Modified      | 请求资源的最后修改时间                                                              | Last-Modified: Tue, 15 Nov 2010 12:45:26 GMT          |
| Location           | 用来重定向接收方到非请求 URL 的位置来完成请求或标识新的资源                         | Location: http://www.zcmhi.com/archives/94.html       |
| Pragma             | 包括实现特定的指令，它可应用到响应链上的任何接收方                                  | Pragma: no-cache                                      |
| Proxy-Authenticate | 它指出认证方案和可应用到代理的该 URL 上的参数                                       | Proxy-Authenticate: Basic                             |
| refresh            | 应用于重定向或一个新的资源被创造，在 5 秒之后重定向(由网景提出，被大部分浏览器支持) | Refresh: 5; url=http://www.zcmhi.com/archives/94.html |
| Retry-After        | 如果实体暂时不可取，通知客户端在指定时间之后再次尝试                                | Retry-After: 120                                      |
| Server             | web 服务器软件名称                                                                  | Server: Apache/1.3.27 (Unix) (Red-Hat/Linux)          |
| Set-Cookie         | 设置 Http Cookie                                                                    | Set-Cookie: UserID=JohnDoe; Max-Age=3600; Version=1   |
| Trailer            | 指出头域在分块传输编码的尾部存在                                                    | Trailer: Max-Forwards                                 |
| Transfer-Encoding  | 文件传输编码                                                                        | Transfer-Encoding:chunked                             |
| Vary               | 告诉下游代理是使用缓存响应还是从原始服务器请求                                      | Vary: \*                                              |
| Via                | 告知代理客户端响应是通过哪里发送的                                                  | Via: 1.0 fred, 1.1 nowhere.com (Apache/1.1)           |
| Warning            | 警告实体可能存在的问题                                                              | Warning: 199 Miscellaneous warning                    |
| WWW-Authenticate   | 表明客户端请求实体应该使用的授权方案                                                | WWW-Authenticate: Basic                               |
