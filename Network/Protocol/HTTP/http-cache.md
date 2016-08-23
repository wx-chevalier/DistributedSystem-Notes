<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [HTTP Cache](#http-cache)
  - [Header](#header)
  - [Reference](#reference)
- [HTTP 1.0:基于Pragma&Expires的缓存实现](#http-10%E5%9F%BA%E4%BA%8Epragma&expires%E7%9A%84%E7%BC%93%E5%AD%98%E5%AE%9E%E7%8E%B0)
  - [Pragma](#pragma)
  - [Expire](#expire)
- [HTTP 1.1 Cache-Control:相对过期时间](#http-11-cache-control%E7%9B%B8%E5%AF%B9%E8%BF%87%E6%9C%9F%E6%97%B6%E9%97%B4)
- [HTTP 1.1 缓存校验](#http-11-%E7%BC%93%E5%AD%98%E6%A0%A1%E9%AA%8C)
  - [Last-Modified](#last-modified)
  - [ETag](#etag)
- [缓存策略](#%E7%BC%93%E5%AD%98%E7%AD%96%E7%95%A5)
  - [废弃和更新已缓存的响应](#%E5%BA%9F%E5%BC%83%E5%92%8C%E6%9B%B4%E6%96%B0%E5%B7%B2%E7%BC%93%E5%AD%98%E7%9A%84%E5%93%8D%E5%BA%94)
  - [缓存检查表](#%E7%BC%93%E5%AD%98%E6%A3%80%E6%9F%A5%E8%A1%A8)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->


> 本文从属于笔者的[HTTP 理解与实践](https://github.com/wxyyxc1992/infrastructure-handbook#HTTP)系列文章，对于HTTP的学习主要包含[HTTP 基础](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/HTTP/http.md)、[HTTP 请求头与请求体](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/HTTP/http-request.md)、[HTTP 响应头与状态码](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/HTTP/http-response.md)、[HTTP 缓存](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/HTTP/http-cache.md)这四个部分，而对于HTTP相关的扩展与引申，我们还需要了解[HTTPS 理解与实践 ](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/HTTP/HTTPS/HTTPS.md)、[HTTP/2 基础](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/HTTP/HTTP2/HTTP2.md)、[WebSocket 基础](https://github.com/wxyyxc1992/infrastructure-handbook/blob/master/Network/Protocol/WebSocket/WebSocket.md)这些部分。本部分知识点同时也归纳于笔者的[我的校招准备之路:从Web前端到服务端应用架构](https://github.com/wxyyxc1992/Coder-Knowledge-Graph/blob/master/interview/my-frontend-backend-interview.md)这篇综述。

# HTTP Cache

通过网络获取内容既缓慢，成本又高：大的响应需要在客户端和服务器之间进行多次往返通信，这拖延了浏览器可以使用和处理内容的时间，同时也增加了访问者的数据成本。因此，缓存和重用以前获取的资源的能力成为优化性能很关键的一个方面。每个浏览器都实现了 HTTP 缓存！ 我们所要做的就是，确保每个服务器响应都提供正确的 HTTP 头指令，以指导浏览器何时可以缓存响应以及可以缓存多久。服务器在返回响应时，还会发出一组 HTTP 头，用来描述内容类型、长度、缓存指令、验证令牌等。例如，在下图的交互中，服务器返回了一个 1024 字节的响应，指导客户端缓存响应长达 120 秒，并提供验证令牌（x234dff），在响应过期之后，可以用来验证资源是否被修改。

![](http://o6v08w541.bkt.clouddn.com/http-requestfcafds.png)
我们打开百度首页，可以看下百度的HTTP缓存的实现：
![](http://images2015.cnblogs.com/blog/561179/201603/561179-20160331162129410-1428753698.gif)
发现对于静态资源的访问都是返回的200状态码。

| 头部            | 优势和特点                                    | 劣势和问题                                    |
| ------------- | ---------------------------------------- | ---------------------------------------- |
| Expires       | 1、HTTP 1.0 产物，可以在HTTP 1.0和1.1中使用，简单易用。2、以时刻标识失效时间。 | 1、时间是由服务器发送的(UTC)，如果服务器时间和客户端时间存在不一致，可能会出现问题。2、存在版本问题，到期之前的修改客户端是不可知的。 |
| Cache-Control | 1、HTTP 1.1 产物，以时间间隔标识失效时间，解决了Expires服务器和客户端相对时间的问题。2、比Expires多了很多选项设置。 | 1、HTTP 1.1 才有的内容，不适用于HTTP 1.0 。2、存在版本问题，到期之前的修改客户端是不可知的。 |
| Last-Modified | 1、不存在版本问题，每次请求都会去服务器进行校验。服务器对比最后修改时间如果相同则返回304，不同返回200以及资源内容。 | 1、只要资源修改，无论内容是否发生实质性的变化，都会将该资源返回客户端。例如周期性重写，这种情况下该资源包含的数据实际上一样的。2、以时刻作为标识，无法识别一秒内进行多次修改的情况。3、某些服务器不能精确的得到文件的最后修改时间。 |
| ETag          | 1、可以更加精确的判断资源是否被修改，可以识别一秒内多次修改的情况。2、不存在版本问题，每次请求都回去服务器进行校验。 | 1、计算ETag值需要性能损耗。2、分布式服务器存储的情况下，计算ETag的算法如果不一样，会导致浏览器从一台服务器上获得页面内容后到另外一台服务器上进行验证时发现ETag不匹配的情况。 |


## Header
HTTP报文头部中与缓存相关的字段为：
**1. 通用首部字段**（就是请求报文和响应报文都能用上的字段）

![](http://images2015.cnblogs.com/blog/561179/201604/561179-20160401161150504-1030837643.png)

**2. 请求首部字段**

![](http://images2015.cnblogs.com/blog/561179/201604/561179-20160401161240301-2050921595.png)

**3. 响应首部字段**

![](http://images2015.cnblogs.com/blog/561179/201604/561179-20160401161311394-1246877214.png)

**4. 实体首部字段**

![](http://images2015.cnblogs.com/blog/561179/201604/561179-20160401171410441-767100632.png)
## Reference
- [HTTP缓存](https://developers.google.com/web/fundamentals/performance/optimizing-content-efficiency/http-caching?hl=zh-cn#cache-control-)
- [浅谈浏览器http的缓存机制](http://www.cnblogs.com/vajoy/p/5341664.html)
- [HTTP缓存控制小结](http://www.tuicool.com/articles/URJjAb)


# HTTP 1.0:基于Pragma&Expires的缓存实现

在 http1.0 时代，给客户端设定缓存方式可通过两个字段——“Pragma”和“Expires”来规范。虽然这两个字段早可抛弃，但为了做http协议的向下兼容，你还是可以看到很多网站依旧会带上这两个字段。

## Pragma

当该字段值为“no-cache”的时候*（事实上现在RFC中也仅标明该可选值）*，会知会客户端不要对该资源读缓存，即每次都得向服务器发一次请求才行。Pragma属于通用首部字段，在客户端上使用时，常规要求我们往html上加上这段meta元标签（仅对该页面有效，对页面上的资源无效）：
```
<meta http-equiv="Pragma" content="no-cache">
```
它告诉浏览器每次请求页面时都不要读缓存，都得往服务器发一次请求才行。不过这种限制行为在客户端作用有限：
1. 仅有IE才能识别这段meta标签含义，其它主流浏览器仅能识别“Cache-Control: no-store”的meta标签。
2. 在IE中识别到该meta标签含义，并不一定会在请求字段加上Pragma，但的确会让当前页面每次都发新请求（仅限页面，页面上的资源则不受影响）。

另外，需要知道的是，Pragma的优先级是高于Cache-Control 的。譬如在下图这个例子中，我们使用Fiddler为图片资源额外增加以下头部信息:
![](http://img0.tuicool.com/UZFv2mj.png!web)

前者用来设定缓存资源一天，后者禁用缓存，重新访问该页面会发现访问该资源会重新发起一次请求。

## Expire

有了Pragma来禁用缓存，自然也需要有个东西来启用缓存和定义缓存时间，对http1.0而言，Expires就是做这件事的首部字段。Expires的值对应一个GMT（格林尼治时间），比如“Mon, 22 Jul 2002 11:12:01 GMT”来告诉浏览器资源缓存过期时间，如果还没过该时间点则不发请求。在客户端我们同样可以使用meta标签来知会IE（也仅有IE能识别）页面（同样也只对页面有效，对页面上的资源无效）缓存时间：
```
<meta http-equiv="expires" content="mon, 18 apr 2016 14:30:00 GMT">
```
如果希望在IE下页面不走缓存，希望每次刷新页面都能发新请求，那么可以把“content”里的值写为“-1”或“0”。注意的是该方式仅仅作为知会IE缓存时间的标记，你并不能在请求或响应报文中找到Expires字段。如果是在服务端报头返回Expires字段，则在任何浏览器中都能正确设置资源缓存的时间。
![](http://img1.tuicool.com/FjYjYvv.png!web)

需要注意的是，响应报文中Expires所定义的缓存时间是相对服务器上的时间而言的，其定义的是资源“失效时刻”，如果客户端上的时间跟服务器上的时间不一致（特别是用户修改了自己电脑的系统时间），那缓存时间可能就没啥意义了。


# HTTP 1.1 Cache-Control:相对过期时间

针对上述的“Expires时间是相对服务器而言，无法保证和客户端时间统一”的问题，http1.1新增了 Cache-Control 来定义缓存过期时间，若报文中同时出现了Expires 和 Cache-Control，会以 Cache-Control 为准。换言之，这三者的优先级顺序为:Pragma -> Cache-Control -> Expires。Cache-Control也是一个通用首部字段，这意味着它能分别在请求报文和响应报文中使用。在RFC中规范了 Cache-Control 的格式为：
```
"Cache-Control" ":" cache-directive
```
作为请求首部时，cache-directive 的可选值有：

![](http://images2015.cnblogs.com/blog/561179/201604/561179-20160403173213113-100043029.png)

作为响应首部时，cache-directive 的可选值有：

![](http://images2015.cnblogs.com/blog/561179/201604/561179-20160403181549941-1360231582.png)

另外 Cache-Control 允许自由组合可选值，例如：
```
Cache-Control: max-age=3600, must-revalidate
```
它意味着该资源是从原服务器上取得的，且其缓存（新鲜度）的有效时间为一小时，在后续一小时内，用户重新访问该资源则无须发送请求。当然这种组合的方式也会有些限制，比如 no-cache 就不能和 max-age、min-fresh、max-stale 一起搭配使用。组合的形式还能做一些浏览器行为不一致的兼容处理。例如在IE我们可以使用 no-cache 来防止点击“后退”按钮时页面资源从缓存加载，但在 Firefox 中，需要使用 no-store 才能防止历史回退时浏览器不从缓存中去读取数据，故我们在响应报头加上如下组合值即可做兼容处理：

```
Cache-Control: no-cache, no-store
```

# HTTP 1.1 缓存校验

上述的首部字段均能让客户端决定是否向服务器发送请求，比如设置的缓存时间未过期，那么自然直接从本地缓存取数据即可（在chrome下表现为200 from cache），若缓存时间过期了或资源不该直接走缓存，则会发请求到服务器去。我们现在要说的问题是，如果客户端向服务器发了请求，那么是否意味着一定要读取回该资源的整个实体内容呢？我们试着这么想——客户端上某个资源保存的缓存时间过期了，但这时候其实服务器并没有更新过这个资源，如果这个资源数据量很大，客户端要求服务器再把这个东西重新发一遍过来，是否非常浪费带宽和时间呢？答案是肯定的，那么是否有办法让服务器知道客户端现在存有的缓存文件，其实跟自己所有的文件是一致的，然后直接告诉客户端说“这东西你直接用缓存里的就可以了，我这边没更新过呢，就不再传一次过去了”。为了让客户端与服务器之间能实现缓存文件是否更新的验证、提升缓存的复用率，Http1.1新增了几个首部字段来做这件事情。

## Last-Modified

服务器将资源传递给客户端时，会将资源最后更改的时间以“Last-Modified: GMT”的形式加在实体首部上一起返回给客户端。客户端会为资源标记上该信息，下次再次请求时，会把该信息附带在请求报文中一并带给服务器去做检查，若传递的时间值与服务器上该资源最终修改时间是一致的，则说明该资源没有被修改过，直接返回304状态码即可。至于传递标记起来的最终修改时间的请求报文首部字段一共有两个：

**⑴ If-Modified-Since: Last-Modified-value**

```
示例为  If-Modified-Since: Thu, 31 Mar 2016 07:07:52 GMT
```

该请求首部告诉服务器如果客户端传来的最后修改时间与服务器上的一致，则直接回送304 和响应报头即可。当前各浏览器均是使用的该请求首部来向服务器传递保存的 Last-Modified 值。

**⑵ If-Unmodified-Since: Last-Modified-value**

告诉服务器，若Last-Modified没有匹配上*（资源在服务端的最后更新时间改变了）*，则应当返回412(Precondition Failed) 状态码给客户端。

当遇到下面情况时，If-Unmodified-Since 字段会被忽略：

```
1. Last-Modified值对上了（资源在服务端没有新的修改）；
2. 服务端需返回2XX和412之外的状态码；
3. 传来的指定日期不合法
```

Last-Modified 说好却也不是特别好，因为如果在服务器上，一个资源被修改了，但其实际内容根本没发送改变，会因为Last-Modified时间匹配不上而返回了整个实体给客户端*（即使客户端缓存里有个一模一样的资源）*。

![](http://images.cnblogs.com/cnblogs_com/vajoy/558869/o_div.jpg)

## ETag

为了解决上述Last-Modified可能存在的不准确的问题，Http1.1还推出了 ETag 实体首部字段。服务器会通过某种算法，给资源计算得出一个唯一标志符*（比如md5标志）*，在把资源响应给客户端的时候，会在实体首部加上“ETag: 唯一标识符”一起返回给客户端。客户端会保留该 ETag 字段，并在下一次请求时将其一并带过去给服务器。服务器只需要比较客户端传来的ETag跟自己服务器上该资源的ETag是否一致，就能很好地判断资源相对客户端而言是否被修改过了。如果服务器发现ETag匹配不上，那么直接以常规GET 200回包形式将新的资源*（当然也包括了新的ETag）*发给客户端；如果ETag是一致的，则直接返回304知会客户端直接使用本地缓存即可。

那么客户端是如何把标记在资源上的 ETag 传去给服务器的呢？请求报文中有两个首部字段可以带上 ETag 值：

**⑴ If-None-Match: ETag-value**

```
示例为  If-None-Match: "56fcccc8-1699"
```

告诉服务端如果 ETag 没匹配上需要重发资源数据，否则直接回送304和响应报头即可。当前各浏览器均是使用的该请求首部来向服务器传递保存的 ETag 值。

**⑵ If-Match: ETag-value**

告诉服务器如果没有匹配到ETag，或者收到了“*”值而当前并没有该资源实体，则应当返回412(Precondition Failed) 状态码给客户端。否则服务器直接忽略该字段。If-Match 的一个应用场景是，客户端走PUT方法向服务端请求上传/更替资源，这时候可以通过 If-Match 传递资源的ETag。

 

需要注意的是，如果资源是走分布式服务器（比如CDN）存储的情况，需要这些服务器上计算ETag唯一值的算法保持一致，才不会导致明明同一个文件，在服务器A和服务器B上生成的ETag却不一样。

![](http://images.cnblogs.com/cnblogs_com/vajoy/558869/o_div.jpg)

如果 Last-Modified 和 ETag 同时被使用，则要求它们的验证都必须通过才会返回304，若其中某个验证没通过，则服务器会按常规返回资源实体及200状态码。

在较新的 nginx 上默认是同时开启了这两个功能的：

![](http://images2015.cnblogs.com/blog/561179/201604/561179-20160404021556078-697982021.gif)

上图的前三条请求是原始请求，接着的三条请求是刷新页面后的新请求，在发新请求之前我们修改了 reset.css 文件，所以它的 Last-Modified 和 ETag 均发生了改变，服务器因此返回了新的文件给客户端*（状态值为200）*。

而 dog.jpg 我们没有做修改，其Last-Modified 和 ETag在服务端是保持不变的，故服务器直接返回了304状态码让客户端直接使用缓存的 dog.jpg 即可，没有把实体内容返回给客户端*（因为没必要）*。

# 缓存策略
![](http://7xlgth.com1.z0.glb.clouddn.com/http-cache-decision-tree.png)

按照上面的决策树来确定您的应用使用的特定资源或一组资源的最优缓存策略。理想情况下，目标应该是在客户端上缓存尽可能多的响应、缓存尽可能长的时间，并且为每个响应提供验证令牌，以便进行高效的重新验证。

| Cache-Control 指令     | 说明                                       |
| -------------------- | ---------------------------------------- |
| max-age=86400        | 浏览器和任何中继缓存均可以将响应（如果是`public`的）缓存长达一天（60 秒 x 60 分 x 24 小时） |
| private, max-age=600 | 客户端浏览器只能将响应缓存最长 10 分钟（60 秒 x 10 分）       |
| no-store             | 不允许缓存响应，每个请求必须获取完整的响应。                   |

根据 HTTP Archive，在排名最高的 300,000 个网站中（Alexa 排名），[所有下载的响应中，几乎有半数可以由浏览器进行缓存](http://httparchive.org/trends.php#maxage0)，对于重复性网页浏览和访问来说，这是一个巨大的节省！ 当然，这并不意味着特定的应用会有 50% 的资源可以被缓存：有些网站可以缓存 90% 以上的资源， 而有些网站有许多私密的或者时间要求苛刻的数据，根本无法被缓存。
当我们在一个项目上做http缓存的应用时，我们还是会把上述提及的大多数首部字段均使用上，例如使用 Expires 来兼容旧的浏览器，使用 Cache-Control 来更精准地利用缓存，然后开启 ETag 跟 Last-Modified 功能进一步复用缓存减少流量。

那么这里会有一个小问题——Expires 和 Cache-Control 的值应设置为多少合适呢？

答案是不会有过于精准的值，均需要进行按需评估。

例如页面链接的请求常规是无须做长时间缓存的，从而保证回退到页面时能重新发出请求，百度首页是用的 Cache-Control:private，腾讯首页则是设定了60秒的缓存，即 Cache-Control:max-age=60。

而静态资源部分，特别是图片资源，通常会设定一个较长的缓存时间，而且这个时间最好是可以在客户端灵活修改的。以腾讯的某张图片为例：

```
http://i.gtimg.cn/vipstyle/vipportal/v4/img/common/logo.png?max_age=2592000
```

客户端可以通过给图片加上“max_age”的参数来定义服务器返回的缓存时间：

![](http://images2015.cnblogs.com/blog/561179/201604/561179-20160404115556000-838597877.png)

## 废弃和更新已缓存的响应
浏览器发出的所有 HTTP 请求会首先被路由到浏览器的缓存，以查看是否缓存了可以用于实现请求的有效响应。如果有匹配的响应，会直接从缓存中读取响应，这样就避免了网络延迟以及传输产生的数据成本。**然而，如果我们希望更新或废弃已缓存的响应，该怎么办？**

例如，假设我们已经告诉访问者某个 CSS 样式表缓存长达 24 小时 (max-age=86400)，但是设计人员刚刚提交了一个更新，我们希望所有用户都能使用。我们该如何通知所有访问者缓存的 CSS 副本已过时，需要更新缓存？ 这是一个欺骗性的问题 - 实际上，至少在不更改资源网址的情况下，我们做不到。

一旦浏览器缓存了响应，在过期以前，将一直使用缓存的版本，这是由 max-age 或者 expires 指定的，或者直到因为某些原因从缓存中删除，例如用户清除了浏览器缓存。因此，在构建网页时，不同的用户可能使用的是文件的不同版本；刚获取该资源的用户将使用新版本，而缓存过之前副本（但是依然有效）的用户将继续使用旧版本的响应。

**所以，我们如何才能鱼和熊掌兼得：客户端缓存和快速更新？** 很简单，在资源内容更改时，我们可以更改资源的网址，强制用户下载新响应。通常情况下，可以通过在文件名中嵌入文件的指纹码（或版本号）来实现 - 例如 style.**x234dff**.css。

当然这需要有一个前提——静态资源能确保长时间不做改动。如果一个脚本文件响应给客户端并做了长时间的缓存，而服务端在近期修改了该文件的话，缓存了此脚本的客户端将无法及时获得新的数据。

解决该困扰的办法也简单——把服务侧ETag的那一套也搬到前端来用——页面的静态资源以版本形式发布，常用的方法是在文件名或参数带上一串md5或时间标记符：

```
https://hm.baidu.com/hm.js?e23800c454aa573c0ccb16b52665ac26
http://tb1.bdstatic.com/tb/_/tbean_safe_ajax_94e7ca2.js
http://img1.gtimg.com/ninja/2/2016/04/ninja145972803357449.jpg
```

如果文件被修改了，才更改其标记符内容，这样能确保客户端能及时从服务器收取到新修改的文件。

因为能够定义每个资源的缓存策略，所以，我们可以定义’缓存层级’，这样，不但可以控制每个响应的缓存时间，还可以控制访问者看到新版本的速度。例如，我们一起分析一下上面的例子：

- HTML 被标记成`no-cache`，这意味着浏览器在每次请求时都会重新验证文档，如果内容更改，会获取最新版本。同时，在 HTML 标记中，我们在 CSS 和 JavaScript 资源的网址中嵌入指纹码：如果这些文件的内容更改，网页的 HTML 也会随之更改，并将下载 HTML 响应的新副本。
- 允许浏览器和中继缓存（例如 CDN）缓存 CSS，过期时间设置为 1 年。注意，我们可以放心地使用 1 年的’远期过期’，因为我们在文件名中嵌入了文件指纹码：如果 CSS 更新，网址也会随之更改。
- JavaScript 过期时间也设置为 1 年，但是被标记为 private，也许是因为包含了 CDN 不应缓存的一些用户私人数据。
- 缓存图片时不包含版本或唯一指纹码，过期时间设置为 1 天。

## 缓存检查表

不存在最佳的缓存策略。根据您的通信模式、提供的数据类型以及应用特定的数据更新要求，必须定义和配置每个资源最适合的设置以及整体的’缓存层级’。

在定义缓存策略时，要记住下列技巧和方法：

1. **使用一致的网址：**如果您在不同的网址上提供相同的内容，将会多次获取和存储该内容。提示：注意，[网址区分大小写](http://www.w3.org/TR/WD-html40-970708/htmlweb.html)！
2. **确保服务器提供验证令牌 (ETag)：**通过验证令牌，如果服务器上的资源未被更改，就不必传输相同的字节。
3. **确定中继缓存可以缓存哪些资源：**对所有用户的响应完全相同的资源很适合由 CDN 或其他中继缓存进行缓存。
4. **确定每个资源的最优缓存周期：**不同的资源可能有不同的更新要求。审查并确定每个资源适合的 max-age。
5. **确定网站的最佳缓存层级：**对 HTML 文档组合使用包含内容指纹码的资源网址以及短时间或 no-cache 的生命周期，可以控制客户端获取更新的速度。
6. **搅动最小化：**有些资源的更新比其他资源频繁。如果资源的特定部分（例如 JavaScript 函数或一组 CSS 样式）会经常更新，应考虑将其代码作为单独的文件提供。这样，每次获取更新时，剩余内容（例如不会频繁更新的库代码）可以从缓存中获取，确保下载的内容量最少。

![](http://153.3.251.190:11900/HTTP-Cache)


