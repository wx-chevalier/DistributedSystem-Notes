# Shell 编程

Shell 是用户和 Linux（或者更准确的说，是用户和 Linux 内核）之间的接口程序，在提示符下输入的每个命令都由 Shell 先解释然后传给 Linux 内核。Shell 是一个命令语言解释器（command-language interpreter），拥有自己内建的 Shell 命令集；此外，Shell 也能被系统中其他有效的 Linux 实用程序和应用程序（utilities and application programs）所调用。

而 Bash(Bourne Again Shell) 则是 Bourne Shell(Sh) 的扩展，其优化了原本用户输入处理的不足，提供了多种便捷用户输入的方式。bash 也包含了很多 C 和 Korn Shell 里的优点。bash 有很灵活和强大的编程接口，同时又有很友好的用户界面。

- /bin/sh (已经被 /bin/bash 所取代)
- /bin/bash (就是 Linux 默认的 Shell)
- /bin/ksh (KornShell 由 AT&T Bell lab. 发展出来的，兼容于 bash)
- /bin/tcsh (整合 C Shell ，提供更多的功能)
- /bin/csh (已经被 /bin/tcsh 所取代)
- /bin/zsh (基于 ksh 发展出来的，功能更强大的 Shell)

Shell 并不拘泥于 Linux，在 Windows 10 之后其内置了 Linux 子系统，不过在老版本的 Windows 中我们还可以使用 [Git Bash]()、[Babun]()、[Cash (JavaScript)]() 这些工具来模拟执行 Shell 命令。

# 初体验

如果希望复现好莱坞大片中的酷炫效果，可以使用如下命令：

```sh
$ sudo apt-get install hollywood cmatrix
```

![](http://7xkt0f.com1.z0.glb.clouddn.com/65DCC0D6-CDE4-4199-9669-2CA32259FB15.png)

# CheatSheet

```sh

```

# 链接

- https://devhints.io/bash 提取其中的命令语句为 CheatSheet
