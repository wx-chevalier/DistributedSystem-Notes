<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [HTTP](#http)
  - [URL](#url)
  - [消息格式](#%E6%B6%88%E6%81%AF%E6%A0%BC%E5%BC%8F)
  - [Reference](#reference)
    - [Tutorials & Docs](#tutorials-&-docs)
    - [Books & Tools](#books-&-tools)
- [HTTP 前世今生](#http-%E5%89%8D%E4%B8%96%E4%BB%8A%E7%94%9F)
  - [HTTP 0.9](#http-09)
  - [HTTP 1.0](#http-10)
    - [短暂连接的缺陷](#%E7%9F%AD%E6%9A%82%E8%BF%9E%E6%8E%A5%E7%9A%84%E7%BC%BA%E9%99%B7)
  - [HTTP 1.1](#http-11)
    - [持久连接](#%E6%8C%81%E4%B9%85%E8%BF%9E%E6%8E%A5)
    - [管道机制](#%E7%AE%A1%E9%81%93%E6%9C%BA%E5%88%B6)
    - [分块传输编码](#%E5%88%86%E5%9D%97%E4%BC%A0%E8%BE%93%E7%BC%96%E7%A0%81)
  - [HTTP 2](#http-2)
    - [二进制协议支持](#%E4%BA%8C%E8%BF%9B%E5%88%B6%E5%8D%8F%E8%AE%AE%E6%94%AF%E6%8C%81)
    - [多工复用](#%E5%A4%9A%E5%B7%A5%E5%A4%8D%E7%94%A8)
    - [数据流](#%E6%95%B0%E6%8D%AE%E6%B5%81)
    - [头信息压缩](#%E5%A4%B4%E4%BF%A1%E6%81%AF%E5%8E%8B%E7%BC%A9)
    - [服务器推送](#%E6%9C%8D%E5%8A%A1%E5%99%A8%E6%8E%A8%E9%80%81)
- [HTTP General Header:HTTP 通用头](#http-general-headerhttp-%E9%80%9A%E7%94%A8%E5%A4%B4)
  - [Date](#date)
  - [Pragma](#pragma)
  - [Entity](#entity)
  - [Content-Type](#content-type)
  - [Content-Length](#content-length)
  - [Content-Encoding](#content-encoding)
- [HTTP Lint](#http-lint)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->



> 本文从属于笔者的[HTTP 理解与实践](HTTPs://github.com/wxyyxc1992/infrastructure-handbook#HTTP)系列文章，对于HTTP的学习主要包含[HTTP 基础](HTTPs://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/HTTP/HTTP.md)、[HTTP 请求头与请求体](HTTPs://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/HTTP/HTTP-request.md)、[HTTP 响应头与状态码](HTTPs://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/HTTP/HTTP-response.md)、[HTTP 缓存](HTTPs://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/HTTP/HTTP-cache.md)这四个部分，而对于HTTP相关的扩展与引申，我们还需要了解[HTTPS 理解与实践 ](HTTPs://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/HTTPS/HTTPS.md)、[HTTP/2 基础](HTTPs://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/HTTP2/HTTP2.md)、[WebSocket 基础](HTTPs://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/WebSocket/WebSocket.md)这些部分。本部分知识点同时也归纳于笔者的[我的校招准备之路:从Web前端到服务端应用架构](HTTPs://github.com/wxyyxc1992/Coder-Knowledge-Graph/blob/master/interview/my-frontend-backend-interview.md)这篇综述。

# HTTP

HTTP（HyperTextTransferProtocol）是超文本传输协议的缩写，它用于传送WWW方式的数据，关于HTTP 协议的详细内容请参考RFC2616。HTTP协议采用了请求/响应模型。客户端向服务器发送一个请求，请求头包含请求的方法、URI、协议版本、以及包含请求修饰符、客户 信息和内容的类似于MIME的消息结构。服务器以一个状态行作为响应，相应的内容包括消息协议的版本，成功或者错误编码加上包含服务器信息、实体元信息以及可能的实体内容。 
HTTP是一种无状态性的协议。这是因为此种协议不要求浏览器在每次请求中标明它自己的身份，并且浏览器以及服务器之间并没有保持一个持久性的连接用于多个页面之间的访问。当一个用户访问一个站点的时候，用户的浏览器发送一个HTTP请求到服务器，服务器返回给浏览器一个HTTP响应。其实很简单的一个概念，客户端一个请求，服务器端一个回复，这就是整个基于HTTP协议的通讯过程。

## URL
在WWW上，每一信息资源都有统一的且在网上唯一的地址，该地址就叫URL（Uniform Resource Locator,统一资源定位符），它是WWW的统一资源定位标志。URL分为绝对URL与相对URL两种。绝对URL和访问时的状态完全无关。与之相对应的是省略了部分信息的相对（relative）URL，如../file.php?text=hello+world，它需要根据当前浏览所在上下文环境里的基准URL，才能确定完整的URL地址。

![](HTTP://7xlgth.com1.z0.glb.clouddn.com/fafasfasfdsfads%E5%9B%BE%E7%89%871.png)

URL由三部分组成：资源类型、存放资源的主机域名、资源文件名。URL的一般语法格式为：(带方括号[]的为可选项)：protocol :// hostname[:port] / path / [;parameters][?query]#fragment

## 消息格式

通常HTTP消息包括客户机向服务器的请求消息和服务器向客户机的响应消息。这两种类型的消息由一个起始行，一个或者多个头域，一个只是头域结束的空行和可选的消息体组成。HTTP的头域包括**通用头，请求头，响应头和实体头**四个部分。每个头域由一个域名，冒号（:）和域值三部分组成。域名是大小写无关的，域值前可以添加任何数量的空格符，头域可以被扩展为多行，在每行开始处，使用至少一个空格或制表符。

当用户访问HTTP://example.com这个域名的时候，浏览器就会自动和服务器建立tcp/ip连接，然后发送HTTP请求到example.com的服务器的80端口。该个请求的语法如下所示：

``` 
GET / HTTP/1.1
Host: example.org
```

以上第一行叫做请求行，第二个参数(一个反斜线在这个例子中)表示所请求资源的路径。反斜线代表了根目录;服务器会转换这个根目录为服务器文件系统中的一个具体目录。

Apache的用户常用DocumentRoot这个命令来设置这个文档根路径。如果请求的url是HTTP://example.org/path/to/script.php,那么请求的路径就是/path/to/script.php。假如document root 被定义为usr/lcoal/apache/htdocs的话,整个请求的资源路径就是/usr/local/apache/htdocs/path/to/script.php。

第二行描述的是HTTP头部的语法。在这个例子中的头部是Host, 它标识了浏览器希望获取资源的域名主机。还有很多其它的请求头部可以包含在HTTP请求中，比如user-Agent头部，在php可以通过$_SERVER['HTTP_USER_AGENT']获取请求中所携带的这个头部信息。

## Reference

### Tutorials & Docs

- [全栈工程师眼中的HTTP](HTTP://www.epubit.com.cn/article/378)
- [阮一峰——HTTP协议入门](HTTP://www.ruanyifeng.com/blog/2016/08/HTTP.html?hmsr=toutiao.io&utm_medium=toutiao.io&utm_source=toutiao.io)

### Books & Tools

- [High Performance Browser Networking](HTTP://chimera.labs.oreilly.com/books/1230000000545/index.html)

# HTTP 前世今生

## HTTP 0.9

最早版本是1991年发布的0.9版。该版本极其简单，只有一个命令`GET`。
```
GET /index.html
```
上面命令表示，TCP 连接（connection）建立后，客户端向服务器请求（request）网页`index.html`。协议规定，服务器只能回应HTML格式的字符串，不能回应别的格式。

```
<html>
  <body>Hello World</body>
 </html>
```
服务器发送完毕，就关闭TCP连接。

## HTTP 1.0
1996年5月，HTTP/1.0 版本发布，内容大大增加。首先，任何格式的内容都可以发送。这使得互联网不仅可以传输文字，还能传输图像、视频、二进制文件。这为互联网的大发展奠定了基础。其次，除了`GET`命令，还引入了`POST`命令和`HEAD`命令，丰富了浏览器与服务器的互动手段。再次，HTTP请求和回应的格式也变了。除了数据部分，每次通信都必须包括头信息（HTTP header），用来描述一些元数据。其他的新增功能还包括状态码（status code）、多字符集支持、多部分发送（multi-part type）、权限（authorization）、缓存（cache）、内容编码（content encoding）等。

### 短暂连接的缺陷
HTTP 1.0规定浏览器与服务器只保持短暂的连接，浏览器的每次请求都需要与服务器建立一个TCP连接，服务器完成请求处理后立即断开TCP连接，服务器不跟踪每个客户也不记录过去的请求。但是，这也造成了一些性能上的缺陷，例如，一个包含有许多图像的网页文件中并没有包含真正的图像数据内容，而只是指明了这些图像的URL地址，当WEB浏览器访问这个网页文件时，浏览器首先要发出针对该网页文件的请求，当浏览器解析WEB服务器返回的该网页文档中的HTML内容时，发现其中的<img>图像标签后，浏览器将根据<img>标签中的src属性所指定的URL地址再次向服务器发出下载图像数据的请求：
![](http://p.blog.csdn.net/images/p_blog_csdn_net/zhangxiaoxiang/i5.jpg)
显然，访问一个包含有许多图像的网页文件的整个过程包含了多次请求和响应，每次请求和响应都需要建立一个单独的连接，每次连接只是传输一个文档和图像，上一 次和下一次请求完全分离。即使图像文件都很小，但是客户端和服务器端每次建立和关闭连接却是一个相对比较费时的过程，并且会严重影响客户机和服务器的性 能。当一个网页文件中包含Applet，JavaScript文件，CSS文件等内容时，也会出现类似上述的情况。

## HTTP 1.1
### 持久连接
在HTTP1.0中，每对Request/Response都使用一个新的连接。HTTP 1.1则支持持久连接Persistent Connection, 并且默认使用persistent  connection. 在同一个tcp的连接中可以传送多个HTTP请求和响应. 多个请求和响应可以重叠，多个请求和响应可以同时进行. 更加多的请求头和响应头(比如HTTP1.0没有host的字段).HTTP 1.1的持续连接，也需要增加新的请求头来帮助实现，例如，Connection请求头的值为Keep-Alive时，客户端通知服务器返回本次请求结果后保持连接；Connection请求头的值为close时，客户端通知服务器返回本次请求结果后关闭连接。HTTP 1.1还提供了与身份认证、状态管理和Cache缓存等机制相关的请求头和响应头。HTTP 1.0规定浏览器与服务器只保持短暂的连接，浏览器的每次请求都需要与服务器建立一个TCP连接，服务器完成请求处理后立即断开TCP连接，服务器不跟踪 每个客户也不记录过去的请求。此外，由于大多数网页的流量都比较小，一次TCP连接很少能通过slow-start区，不利于提高带宽利用率。
![](http://p.blog.csdn.net/images/p_blog_csdn_net/zhangxiaoxiang/i6.jpg)


### 管道机制
1.1 版还引入了管道机制（pipelining），即在同一个TCP连接里面，客户端可以同时发送多个请求。这样就进一步改进了HTTP协议的效率。举例来说，客户端需要请求两个资源。以前的做法是，在同一个TCP连接里面，先发送A请求，然后等待服务器做出回应，收到后再发出B请求。管道机制则是允许浏览器同时发出A请求和B请求，但是服务器还是按照顺序，先回应A请求，完成后再回应B请求。

### 分块传输编码
**分块传输编码**（**Chunked transfer encoding**）是超文本传输协议（HTTP）中的一种数据传输机制，允许HTTP由应用服务器发送给客户端应用（ 通常是网页浏览器）的数据可以分成多个部分。分块传输编码只在HTTP协议1.1版本（HTTP/1.1）中提供。通常，HTTP应答消息中发送的数据是整个发送的，Content-Length消息头字段表示数据的长度。数据的长度很重要，因为客户端需要知道哪里是应答消息的结束，以及后续应答消息的开始。然而，使用分块传输编码，数据分解成一系列数据块，并以一个或多个块发送，这样服务器可以发送数据而不需要预先知道发送内容的总大小。通常数据块的大小是一致的，但也不总是这种情况。

HTTP 1.1引入分块传输编码提供了以下几点好处：

1. HTTP分块传输编码允许服务器为动态生成的内容维持HTTP持久连接。通常，持久链接需要服务器在开始发送消息体前发送Content-Length消息头字段，但是对于动态生成的内容来说，在内容创建完之前是不可知的。**[动态内容，content-length无法预知]**
2. 分块传输编码允许服务器在最后发送消息头字段。对于那些头字段值在内容被生成之前无法知道的情形非常重要，例如消息的内容要使用散列进行签名，散列的结果通过HTTP消息头字段进行传输。没有分块传输编码时，服务器必须缓冲内容直到完成后计算头字段的值并在发送内容前发送这些头字段的值。**[散列签名，需缓冲完成才能计算]**
3. HTTP服务器有时使用压缩 （gzip或deflate）以缩短传输花费的时间。分块传输编码可以用来分隔压缩对象的多个部分。在这种情况下，块不是分别压缩的，而是整个负载进行压缩，压缩的输出使用本文描述的方案进行分块传输。在压缩的情形中，分块编码有利于一边进行压缩一边发送数据，而不是先完成压缩过程以得知压缩后数据的大小。**[gzip压缩，压缩与传输同时进行]**

一般情况HTTP的Header包含Content-Length域来指明报文体的长度。有时候服务生成HTTP回应是无法确定消息大小的，比如大文件的下载，或者后台需要复杂的逻辑才能全部处理页面的请求，这时用需要实时生成消息长度，服务器一般使用chunked编码。在进行Chunked编码传输时，在回复消息的Headers有transfer-coding域值为chunked，表示将用chunked编码传输内容。使用chunked编码的Headers如下（可以利用FireFox的FireBug插件或HttpWatch查看Headers信息）：
```
  　　Chunked-Body=*chunk   
  　　　　　　　　　"0"CRLF   
  　　　　　　　　　footer   
  　　　　　　　　　CRLF   
  　　chunk=chunk-size[chunk-ext]CRLF   
  　　　　　　chunk-dataCRLF   
    
  　　hex-no-zero=<HEXexcluding"0">   
    
  　　chunk-size=hex-no-zero*HEX   
  　　chunk-ext=*(";"chunk-ext-name["="chunk-ext-value])   
  　　chunk-ext-name=token   
  　　chunk-ext-val=token|quoted-string   
  　　chunk-data=chunk-size(OCTET)   
    

  　　footer=*entity-header 
```
编码使用若干个Chunk组成，由一个标明长度为0的chunk结束，每个Chunk有两部分组成，第一部分是该Chunk的长度和长度单位（一般不 写），第二部分就是指定长度的内容，每个部分用CRLF隔开。在最后一个长度为0的Chunk中的内容是称为footer的内容，是一些没有写的头部内容。下面给出一个Chunked的解码过程（RFC文档中有）：
```
  　　length:=0   
  　　readchunk-size,chunk-ext(ifany)andCRLF   
  　　while(chunk-size>0){   
  　　readchunk-dataandCRLF   
  　　appendchunk-datatoentity-body   
  　　length:=length+chunk-size   
  　　readchunk-sizeandCRLF   
  　　}   
  　　readentity-header   
  　　while(entity-headernotempty){   
  　　appendentity-headertoexistingheaderfields   
  　　readentity-header   
  　　}   
  　　Content-Length:=length   
  　　Remove"chunked"fromTransfer-Encoding
```
## HTTP 2
### 二进制协议支持
HTTP/1.1 版的头信息肯定是文本（ASCII编码），数据体可以是文本，也可以是二进制。HTTP/2 则是一个彻底的二进制协议，头信息和数据体都是二进制，并且统称为"帧"（frame）：头信息帧和数据帧。二进制协议的一个好处是，可以定义额外的帧。HTTP/2 定义了近十种帧，为将来的高级应用打好了基础。如果使用文本实现这种功能，解析数据将会变得非常麻烦，二进制解析则方便得多。

### 多工复用
HTTP/2 复用TCP连接，在一个连接里，客户端和浏览器都可以同时发送多个请求或回应，而且不用按照顺序一一对应，这样就避免了"队头堵塞"。举例来说，在一个TCP连接里面，服务器同时收到了A请求和B请求，于是先回应A请求，结果发现处理过程非常耗时，于是就发送A请求已经处理好的部分， 接着回应B请求，完成后，再发送A请求剩下的部分。这样双向的、实时的通信，就叫做多工（Multiplexing）。

### 数据流
因为 HTTP/2 的数据包是不按顺序发送的，同一个连接里面连续的数据包，可能属于不同的回应。因此，必须要对数据包做标记，指出它属于哪个回应。HTTP/2 将每个请求或回应的所有数据包，称为一个数据流（stream）。每个数据流都有一个独一无二的编号。数据包发送的时候，都必须标记数据流ID，用来区分它属于哪个数据流。另外还规定，客户端发出的数据流，ID一律为奇数，服务器发出的，ID为偶数。数据流发送到一半的时候，客户端和服务器都可以发送信号（RST_STREAM帧），取消这个数据流。1.1版取消数据流的唯一方法，就是关闭TCP连接。这就是说，HTTP/2 可以取消某一次请求，同时保证TCP连接还打开着，可以被其他请求使用。客户端还可以指定数据流的优先级。优先级越高，服务器就会越早回应。

### 头信息压缩
HTTP 协议不带有状态，每次请求都必须附上所有信息。所以，请求的很多字段都是重复的，比如Cookie和User Agent，一模一样的内容，每次请求都必须附带，这会浪费很多带宽，也影响速度。HTTP/2 对这一点做了优化，引入了头信息压缩机制（header compression）。一方面，头信息使用gzip或compress压缩后再发送；另一方面，客户端和服务器同时维护一张头信息表，所有字段都会存入这个表，生成一个索引号，以后就不发送同样字段了，只发送索引号，这样就提高速度了。

### 服务器推送
HTTP/2 允许服务器未经请求，主动向客户端发送资源，这叫做服务器推送（server push）。常见场景是客户端请求一个网页，这个网页里面包含很多静态资源。正常情况下，客户端必须收到网页后，解析HTML源码，发现有静态资源，再发出静态资源请求。其实，服务器可以预期到客户端请求网页后，很可能会再请求静态资源，所以就主动把这些静态资源随着网页一起发给客户端了。


# HTTP General Header:HTTP 通用头

就整个网络资源传输而言，包括message-header和message-body两部分。首先传递message-header，即**HTTP** **header**消息。HTTP header 消息通常被分为4个部分：general  header, request header, response header, entity header。但是这种分法就理解而言，感觉界限不太明确。根据维基百科对HTTP header内容的组织形式，大体分为Request和Response两部分。笔者在这里只是对于常见的协议头内容做一个列举，不同的设置会有不同的功能效果，会在下文中详细说明。本部分只介绍请求头的通用构成，具体的请求与响应参考各自章节。
> **注意每个Header的冒号后面有个空格**

通用头域包含请求和响应消息都支持的头域，通用头域包含Cache-Control、 Connection、Date、Pragma、Transfer-Encoding、Upgrade、Via。对通用头域的扩展要求通讯双方都支持此扩展，如果存在不支持的通用头域，一般将会作为实体头域处理。

## Date
Date头域表示消息发送的时间，时间的描述格式由rfc822定义。例如，Date:Mon,31Dec200104:25:57GMT。Date描述的时间表示世界标准时，换算成本地时间，需要知道用户所在的时区。

## Pragma

Pragma头域用来包含实现特定的指令，最常用的是Pragma:no-cache。在HTTP/1.1协议中，它的含义和Cache-Control:no-cache相同。 

## Entity

请求消息和响应消息都可以包含实体信息，实体信息一般由实体头域和实体组成。实体头域包含关于实体的原信息，实体头包括Allow、Content- Base、Content-Encoding、Content-Language、 Content-Length、Content-Location、Content-MD5、Content-Range、Content-Type、 Etag、Expires、Last-Modified、extension-header。extension-header允许客户端定义新的实体 头，但是这些域可能无法未接受方识别。实体可以是一个经过编码的字节流，它的编码方式由Content-Encoding或Content-Type定 义，它的长度由Content-Length或Content-Range定义。 

## Content-Type

Content-Type实体头用于向接收方指示实体的介质类型，指定HEAD方法送到接收方的实体介质类型，或GET方法发送的请求介质类型Content-Range实体头。关于字符的编码，1.0版规定，头信息必须是 ASCII 码，后面的数据可以是任何格式。因此，服务器回应的时候，必须告诉客户端，数据是什么格式。Content-Range实体头用于指定整个实体中的一部分的插入位置，他也指示了整个实体的长度。在服务器向客户返回一个部分响应，它必须描述响应覆盖的范围和整个实体长度。一般格式：
```
Content-Range:bytes-unitSPfirst-byte-pos-last-byte-pos/entity-legth
```
Content-Type表明信息类型，缺省值为" text/plain"。它包含了主要类型（primary type）和次要类型（subtype）两个部分，两者之间用"/"分割。主要类型有9种，分别是application、audio、example、image、message、model、multipart、text、video。每一种主要类型下面又有许多种次要类型，常见的有：
- text/plain：纯文本，文件扩展名.txt
- text/html：HTML文本，文件扩展名.htm和.html
- image/jpeg：jpeg格式的图片，文件扩展名.jpg
- image/gif：GIF格式的图片，文件扩展名.gif
- audio/x-wave：WAVE格式的音频，文件扩展名.wav
- audio/mpeg：MP3格式的音频，文件扩展名.mp3
- video/mpeg：MPEG格式的视频，文件扩展名.mpg
- application/zip：PK-ZIP格式的压缩文件，文件扩展名.zip

## Content-Length 
TCP 1.0中允许单个TCP连接可以传送多个回应，势必就要有一种机制，区分数据包是属于哪一个回应的。这就是Content-length字段的作用，声明本次回应的数据长度。只有当浏览器使用持久HTTP连接时才需要这个数据。如果你想要利用持久连接的优势，可以把输出文档写入ByteArrayOutputStram，完成后查看其大小，然后把该值放入Content-Length头，最后通过 byteArrayStream.writeTo(response.getOutputStream()发送内容。

## Content-Encoding
由于发送的数据可以是任何格式，因此可以把数据压缩后再发送。`Content-Encoding`字段说明数据的压缩方法。

> ```
> Content-Encoding: gzip
> Content-Encoding: compress
> Content-Encoding: deflate
> ```

客户端在请求时，用`Accept-Encoding`字段说明自己可以接受哪些压缩方法。

```
Accept-Encoding: gzip, deflate
```

# HTTP Lint

> - [Lint for HTTP:HTTPolice](HTTP://www.tuicool.com/articles/yuaeyuj)

HTTPolice是一个简单的基于命令行的对于HTTP请求格式规范进行检测的工具，可以直接使用`pip`命令进行安装:
```
pip install HTTPolice
```
当我们使用Google Chrome、Firefox或者Microsoft Edge进行网络访问时，可以使用开发者工具导出某个HAR文件，这也就是HTTP Lint工具可以用来解析的文件。使用如下命令进行分析:
```
$ httpolice -i har /path/to/file.har
------------ request: GET /1441/25776044114_0e5b9879a0_z.jpg------------ response: 200 OKC 1277 Obsolete 'X-' prefix in X-Photo-FarmC 1277 Obsolete 'X-' prefix in X-Photo-OriginE 1000 Malformed Expires headerE 1241 Date + Age is in the future
```
默认的HTTPolice以文本形式输出报告文本，如下所示
```
------------ request: PUT /articles/109226/
E 1000 Malformed If-Match header
C 1093 User-Agent contains no actual product
------------ response: 204 No Content
C 1110 204 response with no Date header
E 1221 Strict-Transport-Security without TLS
------------ request: POST /articles/109226/comments/
...
```
纯文本的方式可能比较难以理解，我们可以使用`-o html`选项来设置更详细的基于HTML风格的输出，譬如:
```
$ httpolice -i har -o html /path/to/file.har >report.html
```

![](https://coding.net/u/hoteam/p/Cache/git/raw/master/2016/8/2/6BF80CC5-C64A-4753-8EBA-FDDEA87C0297.png)


![](http://153.3.251.190:11900/HTTP)





