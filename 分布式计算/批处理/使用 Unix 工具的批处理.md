# 使用 Unix 工具的批处理

我们从一个简单的例子开始。假设您有一台 Web 服务器，每次处理请求时都会在日志文件中附加一行。例如，使用 nginx 默认访问日志格式，日志的一行可能如下所示：

```sh
216.58.210.78 - - [27/Feb/2015:17:55:11 +0000] "GET /css/typography.css HTTP/1.1"
200 3377 "http://test.com/" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5)
AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.115 Safari/537.36"
```

日志的格式定义如下：

```sh
$remote_addr - $remote_user [$time_local] "$request"
$status $body_bytes_sent "$http_referer" "$http_user_agent"
```

日志的这一行表明在 2015 年 2 月 27 日 17:55:11 UTC，服务器从客户端 IP 地址 216.58.210.78 接收到对文件/css/typography.css 的请求。用户没有被认证，所以\$remote_user 被设置为连字符（-）。响应状态是 200（即请求成功），响应的大小是 3377 字节。网页浏览器是 Chrome 40，URL http://test.com/ 的页面中的引用导致该文件被加载。

## 分析简单日志

很多工具可以从这些日志文件生成关于网站流量的漂亮的报告，但为了练手，让我们使用基本的 Unix 功能创建自己的工具。例如，假设你想在你的网站上找到五个最受欢迎的网页。则可以在 Unix shell 中这样做：

```sh
cat /var/log/nginx/access.log | #1 读取日志文件
	awk '{print $7}' | #2 将每一行按空格分割成不同的字段，每行只输出第七个字段，恰好是请求的URL。在我们的例子中是/css/typography.css
	sort             | #3 按字母顺序排列请求的URL列表。如果某个URL被请求过n次，那么排序后，文件将包含连续重复出现n次的该URL
	uniq -c          | #4 uniq命令通过检查两个相邻的行是否相同来过滤掉输入中的重复行。-c则表示还要输出一个计数器：对于每个不同的URL，它会报告输入中出现该URL的次数
	sort -r -n       | #5 第二种排序按每行起始处的数字（-n）排序，这是URL的请求次数。然后逆序（-r）返回结果，大的数字在前
	head -n 5          #6 最后，只输出前五行（-n 5），并丢弃其余的
```

最后输出的结果如下：

```sh
4189 /favicon.ico
3631 /2013/05/24/improving-security-of-ssh-private-keys.html
2124 /2012/12/05/schema-evolution-in-avro-protocol-buffers-thrift.html
1369 /
915 /css/typography.css
```

Unix 工具非常强大，能在几秒钟内处理几 GB 的日志文件，并且您可以根据需要轻松修改命令。例如，如果要从报告中省略 CSS 文件，可以将 awk 参数更改为 `'$7 !~ /\.css$/ {print \$7}'`,如果想统计最多的客户端 IP 地址,可以把 awk 参数改为 `'{print $1}'` 等等。

## 命令链与自定义程序

除了 Unix 命令链，你还可以写一个简单的程序来做同样的事情。例如在 Ruby 中，它可能看起来像这样：

```sh
counts = Hash.new(0)         # 1 counts是一个存储计数器的哈希表，保存了每个URL被浏览的次数，默认为0。

File.open('/var/log/nginx/access.log') do |file|
    file.each do |line|
        url = line.split[6]  # 2 逐行读取日志，抽取每行第七个被空格分隔的字段为URL（这里的数组索引是6，因为Ruby的数组索引从0开始计数）
        counts[url] += 1     # 3 将日志当前行中URL对应的计数器值加一。
    end
end

top5 = counts.map{|url, count| [count, url] }.sort.reverse[0...5] # 4 按计数器值（降序）对哈希表内容进行排序，并取前五位。
top5.each{|count, url| puts "#{count} #{url}" }                   # 5 打印出前五个条目。

```

## 排序 VS 内存中的聚合

Ruby 脚本在内存中保存了一个 URL 的哈希表，将每个 URL 映射到它出现的次数。Unix 管道没有这样的哈希表，而是依赖于对 URL 列表的排序，在这个 URL 列表中，同一个 URL 的只是简单地重复出现。

哪种方法更好？这取决于你有多少个不同的 URL。对于大多数中小型网站，你可能可以为所有不同网址提供一个计数器（假设我们使用 1GB 内存）。在此例中，作业的工作集（working set）（作业需要随机访问的内存大小）仅取决于不同 URL 的数量：如果日志中只有单个 URL，重复出现一百万次，则散列表所需的空间表就只有一个 URL 加上一个计数器的大小。当工作集足够小时，内存散列表表现良好，甚至在性能较差的笔记本电脑上也可以正常工作。

另一方面，如果作业的工作集大于可用内存，则排序方法的优点是可以高效地使用磁盘。这与我们在“SSTables 和 LSM 树”中讨论过的原理是一样的：数据块可以在内存中排序并作为段文件写入磁盘，然后多个排序好的段可以合并为一个更大的排序文件。归并排序具有在磁盘上运行良好的顺序访问模式。（请记住，针对顺序 I/O 进行优化是第 3 章中反复出现的主题，相同的模式在此重现）

GNU Coreutils（Linux）中的 sort 程序通过溢出至磁盘的方式来自动应对大于内存的数据集，并能同时使用多个 CPU 核进行并行排序。这意味着我们之前看到的简单的 Unix 命令链很容易扩展到大数据集，且不会耗尽内存。瓶颈可能是从磁盘读取输入文件的速度。
