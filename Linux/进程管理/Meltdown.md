![](https://inews.co.uk/wp-content/uploads/2018/01/meltdownspectre-1376x1032.jpg)

# Meltdown 简述

近日现代 CPU 的 Meltdown & Spectre 漏洞沸沸扬扬，最早是 Google 研究员发现[可以通过内存侧信道时序攻击来获取隐私数据](https://googleprojectzero.blogspot.de/2018/01/reading-privileged-memory-with-side.html)，后续 [Chromium](https://www.chromium.org/Home/chromium-security/ssca), [Apple](https://support.apple.com/en-us/HT208394) 以及 [Mozilla](https://blog.mozilla.org/security/2018/01/03/mitigations-landing-new-class-timing-attack/) 都发文讨论了其对各个平台的影响与应对方案。网上讨论该漏洞的文章也很多，笔者个人感觉 [This is how Meltdown works](https://dev.to/isaacandsuch/how-meltdown-works-28j2), [Why Raspberry Pi Isn't Vulnerable to Spectre or Meltdown](http://t.cn/RH3DVKj) 讲解的不错，而本文则翻译自 [Meltdown in a nutshell](https://hackernoon.com/meltdown-in-a-nutshell-bda0b79f84a2) 这个简述；本文归纳于 [Linux 配置使用、内部原理与 Shell 编程](https://parg.co/UMI)系列。

对于如下伪代码：

```c
x = read(memory_location_of_os_where_secret_lies) // 抛出异常
y = arr[x * 4096] // 基于 x 读取本地内存
```

应用程序会被编译或者解释为 CPU 指令，第一行对于敏感内存的读取最终会抛出异常，不过由于 CPU 的推测执行(Speculative Execution)优化，第二行代码会在 CPU 进行权限检测前执行，即 y 的值实际上已经被读取到了 CPU 缓存中。如果 x 的值是 `s`，那么内存 arr[`s` * 4096] 即会被 CPU 读取。CPU 发现进程无权读取后，即会清除 x 与 y 的值，不过 CPU 为了优化读取速度，会将本次的读取值缓存到 CPU 本地。攻击者即利用这一特性发起了旁路攻击，攻击者使用类似暴力破解的方式，遍历所有可能的 arr[x *4096] 地址；根据计时器来判断哪个地址的读取速度最快，即可以获取到 x 的值 `s`。依次类推可以获得 ‘e’, ‘c’,’r’,’e’,’t’ 等值。
