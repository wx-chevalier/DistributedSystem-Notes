#  OCI

为了防止容器被Docker一家垄断，巨头们（谷歌，Redhat、微软、IBM、Intel、思科）决定要成立一个组织（OCI），大家一起商量指定了一套规范（CRI、CNI），大家一致统一只兼容符合这套规范的工具。

他们主要是由RedHat推动，三者各司其职，配合完成Docker所有的功能和新扩展功能，并且对docker的问题进行了改良：包括不需要守护程序或访问有root权限的组；容器架构基于fork/exec模型创建容器，更加安全可靠；所以是更先进、高效和安全的下一代容器容器工具。

# 链接

- 再见Docker，Podman、Skopeo和Buildah下一代容器新架构强势出击！ https://zhuanlan.zhihu.com/p/77373246