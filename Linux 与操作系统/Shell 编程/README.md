# Shell 编程

初接触 Linux 时即需要通过 Shell 进行交互控制，而所谓的 Shell 即是用户和 Linux 内核之间的接口程序，其可以被看做命名语言解释器（Command-Language Interpreter）。Shell 也可以被系统中其他有效的 Linux 应用程序所调用。Shell 首先判断是否为内部命令，然后在搜索路径（`$PATH`）里寻找这些应用程序；搜索路径是一个能找到可执行程序的目录列表，如果你键入的命令不是一个内部命令并且在路径里没有找到这个可执行文件，将会显示一条错误信息。而如果命令被成功的找到的话，Shell 的内部命令或应用程序将被分解为系统调用并传给 Linux 内核。

而 Bash(Bourne Again Shell) 则是 Bourne Shell(Sh) 的扩展，其优化了原本用户输入处理的不足，提供了多种便捷用户输入的方式。

Shell 并不拘泥于 在 Windows 10 之后其内置了 Linux 子系统，不过在老版本的 Windows 中我们还可以使用 [Git Bash]()、[Babun]()、[Cash (JavaScript)]() 这些工具来模拟执行 Shell 命令。

## 初体验

如果希望复现好莱坞大片中的酷炫效果，可以使用如下命令：

```sh
$ sudo apt-get install hollywood cmatrix
```

![](http://7xkt0f.com1.z0.glb.clouddn.com/65DCC0D6-CDE4-4199-9669-2CA32259FB15.png)
