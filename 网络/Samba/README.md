# Samba

Samba 是一个能让 Linux 系统应用 Microsoft 网络通讯协议的软件，而 SMB 是 Server Message Block 的缩写，即为服务器消息块 ，SMB 主要是作为 Microsoft 的网络通讯协议，后来 Samba 将 SMB 通信协议应用到了 Linux 系统上，就形成了现在的 Samba 软件。后来微软又把 SMB 改名为 CIFS（Common Internet File System），即公共 Internet 文件系统，并且加入了许多新的功能，这样一来，使得 Samba 具有了更强大的功能。

Samba 最大的功能就是可以用于 Linux 与 windows 系统直接的文件共享和打印共享，Samba 既可以用于 windows 与 Linux 之间的文件共享，也可以用于 Linux 与 Linux 之间的资源共享，由于 NFS(网络文件系统）可以很好的完成 Linux 与 Linux 之间的数据共享，因而 Samba 较多的用在了 Linux 与 windows 之间的数据共享上面。

组成 Samba 运行的有两个服务，一个是 SMB，另一个是 NMB；SMB 是 Samba 的核心启动服务，主要负责建立 Linux Samba 服务器与 Samba 客户机之间的对话， 验证用户身份并提供对文件和打印系统的访问，只有 SMB 服务启动，才能实现文件的共享，监听 139 TCP 端口；而 NMB 服务是负责解析用的，类似与 DNS 实现的功能，NMB 可以把 Linux 系统共享的工作组名称与其 IP 对应起来，如果 NMB 服务没有启动，就只能通过 IP 来访问共享文件，监听 137 和 138 UDP 端口。

例如，某台 Samba 服务器的 IP 地址为 10.0.0.163，对应的工作组名称为 davidsamba，那么在 Windows 的 IE 浏览器输入下面两条指令都可以访问共享文件。其实这就是 Windows 下查看 Linux Samba 服务器共享文件的方法。

# SMB

SMB 是基于客户机/服务器型的协议，因而一台 Samba 服务器既可以充当文件共享服务器，也可以充当一个 Samba 的客户端，例如，一台在 Linux 下已经架设好的 Samba 服务器，windows 客户端就可以通过 SMB 协议共享 Samba 服务器上的资源文件，同时，Samba 服务器也可以访问网络中其它 windows 系统或者 Linux 系统共享出来的文件。Samba 在 windows 下使用的是 NetBIOS 协议，如果你要使用 Linux 下共享出来的文件，请确认你的 windows 系统下是否安装了 NetBIOS 协议。
