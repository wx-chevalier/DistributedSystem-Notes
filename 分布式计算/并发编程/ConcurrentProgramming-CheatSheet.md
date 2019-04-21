[![返回目录](https://i.postimg.cc/JzFTMvjF/image.png)](https://github.com/wx-chevalier/Awesome-CheatSheets)

# Concurrent Programming CheatSheet | 并发编程导论

并发编程是非常广泛的概念，其向下依赖于操作系统、存储等，与分布式系统、微服务等，而又会具体落地于 Java 并发编程、Go 并发编程、JavaScript 异步编程等领域。

并发控制中主要考虑线程之间的通信(线程之间以何种机制来交换信息)与同步(读写等待，竞态条件等)模型，在命令式编程中，线程之间的通信机制有两种：共享内存和消息传递。

Java 就是典型的共享内存模式的通信机制，而 Go 则是提倡以消息传递方式实现内存共享，而非通过共享来实现通信。

在共享内存的并发模型里，线程之间共享程序的公共状态，线程之间通过写-读内存中的公共状态来隐式进行通信。在消息传递的并发模型里，线程之间没有公共状态，线程之间必须通过明确的发送消息来显式进行通信。同步是指程序用于控制不同线程之间操作发生相对顺序的机制。在共享内存并发模型里，同步是显式进行的。程序员必须显式指定某个方法或某段代码需要在线程之间互斥执行。在消息传递的并发模型里，由于消息的发送必须在消息的接收之前，因此同步是隐式进行的。

# 并发，并行，异步

Parallelism consists of performing multiple operations at the same time. Multiprocessing is a means to effect parallelism, and it entails spreading tasks over a computer’s central processing units (CPUs, or cores). Multiprocessing is well-suited for CPU-bound tasks: tightly bound for loops and mathematical computations usually fall into this category.

Concurrency is a slightly broader term than parallelism. It suggests that multiple tasks have the ability to run in an overlapping manner. (There’s a saying that concurrency does not imply parallelism.)

Threading is a concurrent execution model whereby multiple threads take turns executing tasks. One process can contain multiple threads. What’s important to know about threading is that it’s better for IO-bound tasks. While a CPU-bound task is characterized by the computer’s cores continually working hard from start to finish, an IO-bound job is dominated by a lot of waiting on input/output to complete.

To recap the above, concurrency encompasses both multiprocessing (ideal for CPU-bound tasks) and threading (suited for IO-bound tasks). Multiprocessing is a form of parallelism, with parallelism being a specific type (subset) of concurrency.

In fact, async IO is a single-threaded, single-process design: it uses cooperative multitasking, In fact, async IO is a single-threaded, single-process design: it uses cooperative multitasking, It has been said in other words that async IO gives a feeling of concurrency despite using a single thread in a single process. Coroutines (a central feature of async IO) can be scheduled concurrently, but they are not inherently concurrent. cooperative multitasking is a fancy way of saying that a program’s event loop (more on that later) communicates with multiple tasks to let each take turns running at the optimal time.

Async IO takes long waiting periods in which functions would otherwise be blocking and allows other functions to run during that downtime. (A function that blocks effectively forbids others from running from the time that it starts until the time that it returns.)

Asynchronous routines are able to “pause” while waiting on their ultimate result and let other routines run in the meantime.
Asynchronous code, through the mechanism above, facilitates concurrent execution. To put it differently, asynchronous code gives the look and feel of concurrency.

在多核的前提下，性能和线程是紧密联系在一起的。线程间的跳转对高频 IO 操作的性能有决定性作用: 一次跳转意味着至少 3-20 微秒的延时，由于每个核心的 L1 cache 独立（我们的 cpu L2 cache 也是独立的），随之而来是大量的 cache miss，一些变量的读取、写入延时会从纳秒级上升几百倍至微秒级: 等待 cpu 把对应的 cacheline 同步过来。有时这带来了一个出乎意料的结果，当每次的处理都很简短时，一个多线程程序未必比一个单线程程序更快。因为前者可能在每次付出了大的切换代价后只做了一点点“正事”，而后者在不停地做“正事”。不过单线程也是有代价的，它工作良好的前提是“正事”都很快，否则一旦某次变慢就使后续的所有“正事”都被延迟了。在一些处理时间普遍较短的程序中，使用（多个不相交的）单线程能最大程度地”做正事“，由于每个请求的处理时间确定，延时表现也很稳定，各种 http server 正是这样。但我们的检索服务要做的事情可就复杂多了，有大量的后端服务需要访问，广泛存在的长尾请求使每次处理的时间无法确定，排序策略也越来越复杂。如果还是使用（多个不相交的）单线程的话，一次难以预计的性能抖动，或是一个大请求可能导致后续一堆请求被延迟。

并发就是可同时发起执行的程序，并行就是可以在支持并行的硬件上执行的并发程序；换句话说，并发程序代表了所有可以实现并发行为的程序，这是一个比较宽泛的概念，并行程序也只是他的一个子集。

## 线程级并发

从 20 世纪 60 年代初期出现时间共享以来，计算机系统中就开始有了对并发执行的支持；传统意义上，这种并发执行只是模拟出来的，是通过使一台计算机在它正在执行的进程间快速切换的方式实现的，这种配置称为单处理器系统。从 20 世纪 80 年代开始，多处理器系统，即由单操作系统内核控制的多处理器组成的系统采用了多核处理器与超线程（HyperThreading）等技术允许我们实现真正的并行。多核处理器是将多个 CPU 集成到一个集成电路芯片上：

![image](https://user-images.githubusercontent.com/5803001/52341286-21d58300-2a4d-11e9-85fe-5fe5f3894d66.png)

超线程，有时称为同时多线程（simultaneous multi-threading），是一项允许一个 CPU 执行多个控制流的技术。它涉及 CPU 某些硬件有多个备份，比如程序计数器和寄存器文件；而其他的硬件部分只有一份，比如执行浮点算术运算的单元。常规的处理器需要大约 20 000 个时钟周期做不同线程间的转换，而超线程的处理器可以在单个周期的基础上决定要执行哪一个线程。这使得 CPU 能够更好地利用它的处理资源。例如，假设一个线程必须等到某些数据被装载到高速缓存中，那 CPU 就可以继续去执行另一个线程。

## 指令级并发

在较低的抽象层次上，现代处理器可以同时执行多条指令的属性称为指令级并行。实每条指令从开始到结束需要长得多的时间，大约 20 个或者更多的周期，但是处理器使用了非常多的聪明技巧来同时处理多达 100 条的指令。在流水线中，将执行一条指令所需要的活动划分成不同的步骤，将处理器的硬件组织成一系列的阶段，每个阶段执行一个步骤。这些阶段可以并行地操作，用来处理不同指令的不同部分。我们会看到一个相当简单的硬件设计，它能够达到接近于一个时钟周期一条指令的执行速率。如果处理器可以达到比一个周期一条指令更快的执行速率，就称之为超标量（Super Scalar）处理器。

## 单指令、多数据

在最低层次上，许多现代处理器拥有特殊的硬件，允许一条指令产生多个可以并行执行的操作，这种方式称为单指令、多数据，即 SIMD 并行。例如，较新的 Intel 和 AMD 处理器都具有并行地对 4 对单精度浮点数（C 数据类型 float）做加法的指令。

# 并发单元

进程是资源分配的基本单位，线程是调度的基本单位。

## 进程

进程，是计算机中的程序关于某数据集合上的一次运行活动，是系统进行资源分配和调度的基本单位，是操作系统结构的基础。它的执行需要系统分配资源创建实体之后，才能进行。

## 线程

术语“线程”可以用来描述很多不同的事情。在本文中，我会使用它来代指一个逻辑线程。也就是：按照线性顺序的一系列操作；一个执行的逻辑路径。CPU 的每个核心只能真正并发同时执行一个逻辑线程。

CPU：独立的中央处理单元，体现在主板上是有多个 CPU 的槽位。  
CPU cores：在每一个 CPU 上，都可能有多个核（core），每一个核中都有独立的一套 ALU、FPU、Cache 等组件，所以这个概念也被称作物理核。  
processor：这个主要得益于超线程技术，可以让一个物理核模拟出多个逻辑核，即 processor。
简单来说就是，当有多个计算任务时，可以让其中一个计算任务使用 ALU 的时候，另一个则去使用 FPU。
这样就可以充分利用物理核中的各个部件，使得同一个物理核中，也可以并行处理多个计算任务。
理论上来说，对于计算密集型的任务，线程数应该和 CPU 所能提供的并行数一致。那这里的“并行数”应该采取物理核数还是 processor 数呢？“超线程”技术，并没有像理论中的那样加大并行度，从而提高吞吐量。在我的程序（以及大部分程序）中，对各个计算部件（FPU\ALU）的使用并不是均匀的，一般 ALU 的使用占大头，FPU 的使用只占小部分，所以超线程技术并不能带来很大的并行度提升；而这一点点提升，也被线程切换带来的消耗所抵消了。

算术逻辑单元 ALU 与浮点运算单元 FPU

如果线程的数量多于内核的数量，那么有的线程必须要暂停以便于其他的线程来运行工作，当再次轮到自己的执行的时候，会将任务恢复。为了支持暂停和恢复，线程至少需要如下两件事情：
某种类型的指令指针。也就是，当我暂停的时候，我正在执行哪行代码？
一个栈。

此处的线程，即指操作系统线程

系统在将线程调度到 CPU 上时就有了足够的信息，能够暂停某个线程、允许其他的线程运行，随后再次恢复原来的线程。这种操作通常对线程来说是完全透明的。从线程的角度来说，它是连续运行的。

操作系统实现的线程有两个属性，这两个属性极大地限制了它们可以存在的数量；任何将语言线程和操作系统线程进行 1:1 映射的解决方案都无法支持大规模的并发。

使用操作系统线程将会导致每个线程都有固定的、较大的内存成本
采用操作系统线程的另一个主要问题是每个 OS 线程都有大小固定的栈。尽管这个大小是可以配置的，但是在 64 位的环境中，JVM 会为每个线程分配 1M 的栈。你可以将默认的栈空间设置地更小一些，但是你需要权衡内存的使用，因为这会增加栈溢出的风险。代码中的递归越多，就越有可能出现栈溢出。如果你保持默认值的话，那么 1000 个线程就将使用 1GB 的 RAM。虽然现在 RAM 便宜了很多，但是几乎没有人会为了运行上百万个线程而准备 TB 级别的 RAM。

操作系统线程的另一个瓶颈就是上下文切换的代价较大，从该角度来说，使用操作系统线程只能有数万个线程。操作系统有一个所有正在运行的进程和线程的列表，并试图为它们分配“公平”的 CPU 运行时间 [5]。当内核从一个线程切换至另一个线程时，有很多的工作要做。操作系统有一个所有正在运行的进程和线程的列表，并试图为它们分配“公平”的 CPU 运行时间 [5]。当内核从一个线程切换至另一个线程时，有很多的工作要做。

## Coroutine | 协程

“用户空间线程（user space thread）”来代指由语言进行调度的线程，而不是内核 /OS 所调度的线程。

Go 的栈是动态分配大小的，随着存储数据的数量而增长和收缩。

每个新建的 Goroutine 只有大约 4KB 的栈。每个栈只有 4KB，那么在一个 1GB 的 RAM 上，我们就可以有 256 万个 Goroutine 了，相对于 Java 中每个线程的 1MB，这是巨大的提升。Golang 实现了自己的调度器，允许众多的 Goroutines 运行在相同的 OS 线程上。就算 Go 会运行与内核相同的上下文切换，但是它能够避免切换至 ring-0 以运行内核，然后再切换回来，这样就会节省大量的时间。但是，这只是纸面上的分析。为了支持上百万的 Goroutines，Go 需要完成更复杂的事情。

要支持真正的大并发需要另外一项优化：当你知道线程能够做有用的工作时，才去调度它。如果你运行大量线程的话，其实只有少量的线程会执行有用的工作。Go 通过集成通道（channel）和调度器（scheduler）来实现这一点。如果某个 Goroutine 在一个空的通道上等待，那么调度器会看到这一点并且不会运行该 Goroutine。Go 更近一步，将大多数空闲的线程都放到它的操作系统线程上。通过这种方式，活跃的 Goroutine（预期数量会少得多）会在同一个线程上调度执行，而数以百万计的大多数休眠的 Goroutine 会单独处理。这样有助于降低延迟。

除非 Java 增加语言特性，允许调度器进行观察，否则的话，是不可能支持智能调度的。但是，你可以在“用户空间”中构建运行时调度器，它能够感知线程何时能够执行工作。这构成了像 Akka 这种类型的框架的基础，它能够支持上百万的 Actor。

# 并发控制

涉及多线程程序涉及的时候经常会出现一些令人难以思议的事情，用堆和栈分配一个变量可能在以后的执行中产生意想不到的结果，而这个结果的表现就是内存的非法被访问，导致内存的内容被更改。

理解这个现象的两个基本概念是：在一个进程的线程共享堆区，而进程中的线程各自维持自己堆栈。 在 windows 等平台上，不同线程缺省使用同一个堆，所以用 C 的 malloc (或者 windows 的 GlobalAlloc)分配内存的时候是使用了同步保护的。如果没有同步保护，在两个线程同时执行内存操作的时候会产生竞争条件，可能导致堆内内存管理混乱。比如两个线程分配了统一块内存地址，空闲链表指针错误等。

Symbian 的线程一般使用独立的堆空间。这样每个线程可以直接在自己的堆里分配和释放，可以减少同步所引入的开销。当线程退出的时候，系统直接回收线程的堆空间，线程内没有释放的内存空间也不会造成进程内的内存泄漏。

但是两个线程使用共用堆的时候，就必须用 critical section 或者 mutex 进行同步保护。否则程序崩溃时早晚的事。如果你的线程需要在共用堆上无规则的分配和释放任何数量和类型的对象，可以定制一个自己的 allcator，在 allocator 内部使用同步保护。线程直接使用这个 allocator 分配内存就可以了。这相当于实现自己的 malloc，free 。但是更建议你重新审查一下自己的系统，因为这种情况大多数是不必要的。经过良好的设计，线程的本地堆应该能够满足大多数对象的需求。如果有某一类对象需要在共享堆上创建和共享，这种需求是比较合理的，可以在这个类的 new 和 delete 上实现共享保护。

# 锁

# 悲观锁与乐观锁

# CAS

为了实现可串行化，同时避免锁机制存在的各种问题，我们可以采用基于多版本并发控制（Multiversion concurrency control，MVCC）思想的无锁事务机制。人们一般把基于锁的并发控制机制称成为悲观机制，而把 MVCC 机制称为乐观机制。这是因为锁机制是一种预防性的，读会阻塞写，写也会阻塞读，当锁定粒度较大，时间较长时并发性能就不会太好；而 MVCC 是一种后验性的，读不阻塞写，写也不阻塞读，等到提交的时候才检验是否有冲突，由于没有锁，所以读写不会相互阻塞，从而大大提升了并发性能。我们可以借用源代码版本控制来理解 MVCC，每个人都可以自由地阅读和修改本地的代码，相互之间不会阻塞，只在提交的时候版本控制器会检查冲突，并提示 merge。目前，Oracle、PostgreSQL 和 MySQL 都已支持基于 MVCC 的并发机制，但具体实现各有不同。

MVCC 的一种简单实现是基于 CAS（Compare-and-swap）思想的有条件更新（Conditional Update）。普通的 update 参数只包含了一个 keyValueSet’，Conditional Update 在此基础上加上了一组更新条件 conditionSet { … data[keyx]=valuex, … }，即只有在 D 满足更新条件的情况下才将数据更新为 keyValueSet’；否则，返回错误信息。这样，L 就形成了如下图所示的 Try/Conditional Update/(Try again)的处理模式：

## MVCC

MVCC 的全称是 Multi-Version Concurrency Control，通常用于数据库等场景中，实现多版本的并发控制。MVCC 是通过保存数据的多个版本来实现并发控制，当需要更新某条数据时，实现了 MVCC 的存储系统不会立即用新数据覆盖原始数据，而是创建该条记录的一个新的版本。对于多数数据库系统，存储会分为 Data Part 和 Undo Log，Data Part 用来存储事务已提交的数据，而 Undo Log 用来存储旧版本的数据。多版本的存在允许了读和写的分离，读操作是需要读取某个版本之前的数据即可，和写操作不冲突，大大提高了性能。

在没有事务支持的情况下，多个 L 进行并发处理可能会导致数据一致性问题。比如，考虑 L1 和 L2 的如下执行顺序：

L1 从 D 读取 key:123 对应的值 100
L2 从 D 读取 key:123 对应的 100
L1 将 key:123 更新为 100 + 1
L2 将 key:123 更新为 100 + 2
如果 L1 和 L2 串行执行，key:123 对应的值将为 103，但上面并发执行中 L1 的执行效果完全被 L2 所覆盖，实际 key:123 所对应的值变成了 102。

为了让 L 的处理具有可串行化特性(Serializability)，一种最直接的解决方案就是考虑为 D 加上基于锁的简单事务。让 L 在进行业务处理前先锁定 D，完成以后释放锁。另外，为了防止持有锁的 L 由于某种原因长时间未提交事务，D 还需要具有超时机制，当 L 尝试提交一个已超时的事务时会得到一个错误响应。本方案的优点是实现简单，缺点是锁定了整个数据集，粒度太大；时间上包含了 L 的整个处理时间，跨度太长。虽然我们可以考虑把锁定粒度降低到数据项级别，按 key 进行锁定，但这又会带来其他的问题。由于更新的 keySet’可能是事先不确定的，所以可能无法在开始事务时锁定所有的 key；如果分阶段来锁定需要的 key，又可能出现死锁(Deadlock)问题。另外，按 key 锁定在有锁争用的情况下并不能解决锁定时间太长的问题。所以，按 key 锁定仍然存在重要的不足之处。

# 并发模式

典型的后端服务可以分为跟业务无关的通信层，以及业务逻辑层；通信层负责 socket 连接的创建和管理，业务逻辑层则是负责被动响应请求，或主动推送业务消息。通信层以 IO 操作为主，并发连接数非常高，但是不太消耗 CPU；对 tcp 用 epoll/iocp 等新模式，让 tcp 连接和线程进程不再强制产生绑定，操作系统只是把所有注册到 epoll 里面的 tcp 连接状态变化返回给程序，让 tcp 处理回归本质。以 Nginx 为代表的反向代理服务器，即用事件驱动的形式取代多线程。

## Event-Driven Architecture | 事件驱动架构

基于事件驱动的服务器，无需为每一个请求创建额外的对应线程，虽然可以省去创建线程与销毁线程的开销；但它在处理网络请求时，会把侦听到的请求放在事件队列中交给观察者，事件循环会不停的处理这些网络请求。最为经典的进程模型与事件驱动模型的对比当属 Apache 与 Nginx, Apache 是标准的为新的请求创建阻塞型线程来处理，每个线程负责处理单个请求。而 Nginx 则是基于事件的 Web 服务器，它使用 libevent 与 Reactor Pattern 来处理请求。Nginx 在接收到请求之后，会将其交托给系统去处理(读取数据)，而不会阻塞等待；在当数据准备完毕之后，服务器会将结果返回给客户端。Redis 的事件模型实现基于 linux 的 epoll，sun 的 export,FreeBSD 和 Mac osx 的 queue。

下图以 Netty 为例

![](http://ayedo.github.io/img/reactor.png)

We could implement a mechanism which takes the events from the selector and distributes or “dispatches” them to interested methods. If you would like react to an event you can thus subscribe a method to it. Once the event occurs the dispatcher then calls the subscribed methods (often called “handlers”) one by one and one after another. Not only does Netty implement this reactor pattern for you but it further extends it with the concepts of pipelines: Instead of having a single handler attached to an event it allows you to attach a whole sequence of handlers which can pass their output as input to the next handler. How is this useful? Usually, networking applications work in layers: You have raw byte streams which represent HTTP which might me used to transport compressed and encrypted data and so on. With Netty you can now chain a HTTP decoder which passes its output to a decompression handler which passes its output to an decrypt handler and so on… In short: Event handlers in Netty are composable and composition is at the very core of most maintainable and extendable code.

而对于业务逻辑层而言，不但要跟内网的网络服务通信，还要跟外网的服务通信，在业务逻辑层也产生了大量的外网网络 IO，导致一个请求不能在 100ms 内完成，可能增加到了 500ms 甚至几秒钟，其中大部分时间是在等待网络，白白的占用了一个线程等 IO。第二代事件驱动模型应运而生，把业务逻辑也变成事件驱动，彻底消除浪费线程等待 IO 这个现象。事件驱动有两件常见的外衣，一件是异步回调，另一件是 coroutine，近几年有了很多应用：

- Go 的 goroutine
- Python 3 的 coroutine
- Kotlin 的 coroutine
- nodejs 的异步回调
- swoole 1 的异步回调和 swoole 2 的 coroutine
- erlang/elixir 的 process 也算是 coroutine
- VertX 的异步回调
