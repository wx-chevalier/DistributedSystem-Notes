# Systemd

[init 进程](./Linux-CheatSheet.md)是 Linux 系统 Booting 之后的首个进程，其作为守护进程运行直至系统关闭；传统的 Linux 中的服务控制方式也主要依赖于 sysvinit 机制:

```sh
$ sudo /etc/init.d/apache2 start
# 或者
$ service apache2 start
```

当 sysvinit 系统初始化的时候，它是串行启动，并且会将所有可能用到的后台服务进程全部启动运行；系统必须等待所有的服务都启动就绪之后，才允许用户登录，导致启动时间过长与系统资源浪费。并且 init 进程只是执行启动脚本，不管其他事情，脚本需要自己处理各种情况，使得脚本复杂度增加很多。Systemd 就是为了解决这些问题而诞生的。它的设计目标是，为系统的启动和管理提供一套完整的解决方案；Systemd 并不是一个命令，而是一组命令，涉及到系统管理的方方面面。

```sh
# 查看 Systemd 的版本
$ systemctl --version

# 重启系统
$ sudo systemctl reboot

# 关闭系统，切断电源
$ sudo systemctl poweroff

# CPU 停止工作
$ sudo systemctl halt

# 查看启动耗时
$ systemd-analyze

# 查看每个服务的启动耗时
$ systemd-analyze blame

# 显示瀑布状的启动过程流
$ systemd-analyze critical-chain

# 显示指定服务的启动流
$ systemd-analyze critical-chain atd.service
```

Systemd 可以管理所有系统资源。不同的资源统称为 Unit(单位)，Unit 一共分成 12 种。

- Service unit：系统服务
- Target unit：多个 Unit 构成的一个组
- Device Unit：硬件设备
- Mount Unit：文件系统的挂载点
- Automount Unit：自动挂载点
- Path Unit：文件或路径
- Scope Unit：不是由 Systemd 启动的外部进程
- Slice Unit：进程组
- Snapshot Unit：Systemd 快照，可以切回某个快照
- Socket Unit：进程间通信的 socket
- Swap Unit：swap 文件
- Timer Unit：定时器

systemctl status 命令用于查看系统状态和单个 Unit 的状态。

```sh
# 显示系统状态
$ systemctl status

# 显示单个 Unit 的状态
$ sysystemctl status bluetooth.service

# 显示远程主机的某个 Unit 的状态
$ systemctl -H root@rhel7.example.com status httpd.service
```

我们最常用的就是 Unit 管理命令：

```sh
# 立即启动一个服务
$ sudo systemctl start apache.service

# 立即停止一个服务
$ sudo systemctl stop apache.service

# 重启一个服务
$ sudo systemctl restart apache.service

# 杀死一个服务的所有子进程
$ sudo systemctl kill apache.service

# 重新加载一个服务的配置文件
$ sudo systemctl reload apache.service

# 重载所有修改过的配置文件
$ sudo systemctl daemon-reload

# 显示某个 Unit 的所有底层参数
$ systemctl show httpd.service

# 显示某个 Unit 的指定属性的值
$ systemctl show -p CPUShares httpd.service

# 设置某个 Unit 的指定属性
$ sudo systemctl set-property httpd.service CPUShares=500
```

每一个 Unit 都有一个配置文件，告诉 Systemd 怎么启动这个 Unit。Systemd 默认从目录 `/etc/systemd/system/` 读取配置文件。但是，里面存放的大部分文件都是符号链接，指向目录 `/usr/lib/systemd/system/`，真正的配置文件存放在那个目录。systemctl enable 命令用于在上面两个目录之间，建立符号链接关系。配置文件的基础格式如下：

```sh
[Unit]
Description=ATD daemon

[Service]
Type=forking
ExecStart=/usr/bin/atd

[Install]
WantedBy=multi-user.target
```

centos 7 以上是用 Systemd 进行系统初始化的，Systemd 是 Linux 系统中最新的初始化系统(init)，它主要的设计目标是克服 sysvinit 固有的缺点，提高系统的启动速度。关于 Systemd 的详情介绍在[这里](http://www.ibm.com/developerworks/cn/linux/1407_liuming_init3/)。

Systemd 服务文件以.service 结尾，比如现在要建立 nginx 为开机启动，如果用 yum install 命令安装的，yum 命令会自动创建 nginx.service 文件，直接用命令

| 1   | systemcel enable nginx.service |
| --- | ------------------------------ |
|     |                                |

设置开机启动即可。

设置开机启动即可。在这里我是用源码编译安装的，所以要手动创建 nginx.service 服务文件。

设置开机启动即可。在这里我是用源码编译安装的，所以要手动创建 nginx.service 服务文件。开机没有登陆情况下就能运行的程序，存在系统服务(system)里，即：

| 1   | /lib/systemd/system/ |
| --- | -------------------- |
|     |                      |

1.在系统服务目录里创建 nginx.service 文件

| 1   | vim /lib/systemd/system/nginx.service |
| --- | ------------------------------------- |
|     |                                       |

内容如下

| 12345678910111213 | [Unit] Description=nginx After=network.target [Service] Type=forking ExecStart=/usr/local/nginx/sbin/nginx ExecReload=/usr/local/nginx/sbin/nginx -s reload ExecStop=/usr/local/nginx/sbin/nginx -s quitPrivateTmp=true [Install] WantedBy=multi-user.target |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
|                   |                                                                                                                                                                                                                                                              |

[]: Unit "服务的说明"
[]: Unit "服务的说明"

Description:描述服务

[]: Unit "服务的说明"

Description:描述服务
After:描述服务类别

[]: Unit "服务的说明"

Description:描述服务
After:描述服务类别
[Service]服务运行参数的设置

[]: Unit "服务的说明"

Description:描述服务
After:描述服务类别
[Service]服务运行参数的设置
Type=forking 是后台运行的形式

[]: Unit "服务的说明"

Description:描述服务
After:描述服务类别
[Service]服务运行参数的设置
Type=forking 是后台运行的形式
ExecStart 为服务的具体运行命令

[]: Unit "服务的说明"

Description:描述服务
After:描述服务类别
[Service]服务运行参数的设置
Type=forking 是后台运行的形式
ExecStart 为服务的具体运行命令
ExecReload 为重启命令

[]: Unit "服务的说明"

Description:描述服务
After:描述服务类别
[Service]服务运行参数的设置
Type=forking 是后台运行的形式
ExecStart 为服务的具体运行命令
ExecReload 为重启命令
ExecStop 为停止命令

[]: Unit "服务的说明"

Description:描述服务
After:描述服务类别
[Service]服务运行参数的设置
Type=forking 是后台运行的形式
ExecStart 为服务的具体运行命令
ExecReload 为重启命令
ExecStop 为停止命令
PrivateTmp=True 表示给服务分配独立的临时空间

[]: Unit "服务的说明"

Description:描述服务
After:描述服务类别
[Service]服务运行参数的设置
Type=forking 是后台运行的形式
ExecStart 为服务的具体运行命令
ExecReload 为重启命令
ExecStop 为停止命令
PrivateTmp=True 表示给服务分配独立的临时空间注意：[Service]的启动、重启、停止命令全部要求使用绝对路径

[]: Unit "服务的说明"

Description:描述服务
After:描述服务类别
[Service]服务运行参数的设置
Type=forking 是后台运行的形式
ExecStart 为服务的具体运行命令
ExecReload 为重启命令
ExecStop 为停止命令
PrivateTmp=True 表示给服务分配独立的临时空间注意：[Service]的启动、重启、停止命令全部要求使用绝对路径
[Install]运行级别下服务安装的相关设置，可设置为多用户，即系统运行级别为 3

保存退出。

**2.设置开机启动**

| 1   | systemctl enable nginx.service |
| --- | ------------------------------ |
|     |                                |

**3.其它命令**

**3.其它命令**
启动 nginx 服务

| 1   | systemctl start nginx.service |
| --- | ----------------------------- |
|     |                               |

设置开机自启动

| 1   | systemctl enable nginx.service |
| --- | ------------------------------ |
|     |                                |

停止开机自启动

| 1   | systemctl disable nginx.service |
| --- | ------------------------------- |
|     |                                 |

查看服务当前状态

| 1   | systemctl status nginx.service |
| --- | ------------------------------ |
|     |                                |

重新启动服务

| 1   | systemctl restart nginx.service |
| --- | ------------------------------- |
|     |                                 |

查看所有已启动的服务

| 1   | systemctl list-units --type=service |
| --- | ----------------------------------- |
|     |                                     |

**4.Systemd 命令和 sysvinit 命令的对照表**

| Sysvinit 命令           | Systemd 命令                                                                             | 备注                                               |
| ----------------------- | ---------------------------------------------------------------------------------------- | -------------------------------------------------- |
| service foo start       | systemctl start foo.service                                                              | 用来启动一个服务 (并不会重启现有的)                |
| service foo stop        | systemctl stop foo.service                                                               | 用来停止一个服务 (并不会重启现有的)。              |
| service foo restart     | systemctl restart foo.service                                                            | 用来停止并启动一个服务。                           |
| service foo reload      | systemctl reload foo.service                                                             | 当支持时，重新装载配置文件而不中断等待操作。       |
| service foo condrestart | systemctl condrestart foo.service                                                        | 如果服务正在运行那么重启它。                       |
| service foo status      | systemctl status foo.service                                                             | 汇报服务是否正在运行。                             |
| ls /etc/rc.d/init.d/    | systemctl list-unit-files –type=service                                                  | 用来列出可以启动或停止的服务列表。                 |
| chkconfig foo on        | systemctl enable foo.service                                                             | 在下次启动时或满足其他触发条件时设置服务为启用     |
| chkconfig foo off       | systemctl disable foo.service                                                            | 在下次启动时或满足其他触发条件时设置服务为禁用     |
| chkconfig foo           | systemctl is-enabled foo.service                                                         | 用来检查一个服务在当前环境下被配置为启用还是禁用。 |
| chkconfig –list         | systemctl list-unit-files –type=service                                                  | 输出在各个运行级别下服务的启用和禁用情况           |
| chkconfig foo –list     | ls /etc/systemd/system/\*.wants/foo.service                                              | 用来列出该服务在哪些运行级别下启用和禁用。         |
| chkconfig foo –add      | systemctl daemon-reload                                                                  | 当您创建新服务文件或者变更设置时使用。             |
| telinit 3               | systemctl isolate multi-user.target (OR systemctl isolate runlevel3.target OR telinit 3) | 改变至多用户运行级别。                             |

**5.Sysvinit 运行级别和 systemd 目标的对应表**

| Sysvinit 运行级别 | Systemd 目标                                          | 备注                                                        |
| ----------------- | ----------------------------------------------------- | ----------------------------------------------------------- |
| 0                 | runlevel0.target, poweroff.target                     | 关闭系统。                                                  |
| 1, s, single      | runlevel1.target, rescue.target                       | 单用户模式。                                                |
| 2, 4              | runlevel2.target, runlevel4.target, multi-user.target | 用户定义/域特定运行级别。默认等同于 3。                     |
| 3                 | runlevel3.target, multi-user.target                   | 多用户，非图形化。用户可以通过多个控制台或网络登录。        |
| 5                 | runlevel5.target, graphical.target                    | 多用户，图形化。通常为所有运行级别 3 的服务外加图形化登录。 |
| 6                 | runlevel6.target, reboot.target                       | 重启                                                        |
| emergency         | emergency.target                                      | 紧急 Shell                                                  |

# Todos

- [Systemd 详解](https://blog.linuxeye.com/400.html)
