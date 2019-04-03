[![返回目录](https://i.postimg.cc/JzFTMvjF/image.png)](https://github.com/wx-chevalier/Awesome-CheatSheets)

> 本文参考了许多经典的文章描述/示例，统一声明在了 [Java Concurrent Programming Links](https://parg.co/UDS)。

# Java 并发编程概论：内存模型，并发单元，并发控制与异步模式

参考[并发编程导论]()中的介绍，并发编程主要会考虑并发单元、并发控制与异步模式等方面；本文即是着眼于 Java，具体地讨论 Java 中并发编程相关的知识要点。Java 是典型的共享内存的并发模型，线程之间的通信往往是隐式进行。

# Java Memory Model | Java 内存模型

Prior to Java 5, the Java Memory Model (JMM) was ill defined. It was possible to get all kinds of strange results when shared memory was accessed by multiple threads, such as:

a thread not seeing values written by other threads: a visibility problem
a thread observing ‘impossible’ behavior of other threads, caused by instructions not being executed in the order expected: an instruction reordering problem.
With the implementation of JSR 133 in Java 5, a lot of these issues have been resolved. The JMM is a set of rules based on the “happens-before” relation, which constrain when one memory access must happen before another, and conversely, when they are allowed to happen out of order. Two examples of these rules are:

The monitor lock rule: a release of a lock happens before every subsequent acquire of the same lock.
The volatile variable rule: a write of a volatile variable happens before every subsequent read of the same volatile variable
Although the JMM can seem complicated, the specification tries to find a balance between ease of use and the ability to write performant and scalable concurrent data structures.

```java
public class MySharedObject {

    // static variable pointing to instance of MySharedObject
    public static final MySharedObject sharedInstance =
        new MySharedObject();

    // member variables pointing to two objects on the heap
    public Integer object2 = new Integer(22);
    public Integer object4 = new Integer(44);

    public long member1 = 12345;
    public long member1 = 67890;
}
```

```java
public class MyRunnable implements Runnable() {

    public void run() {
        methodOne();
    }

    public void methodOne() {
        int localVariable1 = 45;

        MySharedObject localVariable2 =
            MySharedObject.sharedInstance;

        //... do more with local variables.

        methodTwo();
    }

    public void methodTwo() {
        Integer localVariable1 = new Integer(99);

        //... do more with local variable.
    }
}
```

Java 内存模型和硬件内存架构不一样。硬件内存架构不区分线程栈和堆内存。在硬件中，线程栈和堆内存都分配在主内存中。部分线程栈和堆数据有时可能出现在 CPU 缓存中，有时可能出现在寄存器中。

![](http://tutorials.jenkov.com/images/java-concurrency/java-memory-model-3.png)

如果多个线程共享一个对象，如果没有合理的使用 volatile 声明和线程同步，一个线程更新共享对象后，另一个线程可能无法取到对象的最新值。

如图，共享变量存储在主内存。运行在某个 CPU 中的线程将共享变量读取到自己的 CPU 缓存。在 CPU 缓存中，修改了共享对象的值，由于 CPU 并未将缓存中的数据刷回主内存，导致对共享变量的修改对于在另一个 CPU 中运行的线程而言是不可见的。这样每个线程都会拥有一份属于自己的共享变量的拷贝，分别存于各自对应的 CPU 缓存中。

如果多个线程共享一个对象，当多个线程更新这个变量时，会引发竞争条件。
想象下，如果有两个线程分别运行在两个 CPU 中，两个线程分别将同一个共享变量读取到各自的 CPU 缓存。现在线程一将变量加一，线程二也将变量加一，当两个 CPU 缓存的数据刷回主内存时，变量的值只加了一，并没有加二。同步锁可以确保一段代码块同时只有一个线程可以进入。同步锁可以确保被保护代码块内所有的变量都是从主内存获取的，当被保护代码块执行完毕时，所有变量的更新都会刷回主内存，无论这些变量是否用 volatile 修饰。

# Concurrent Primitive | 并发单元

常见的 Runnable、Callable、Future、FutureTask 这几个与线程相关的类或者接口：

- Runnable 应该是我们最熟悉的接口，它只有一个 run()函数，用于将耗时操作写在其中，该函数没有返回值。然后使用某个线程去执行该 runnable 即可实现多线程，Thread 类在调用 start()函数后就是执行的是 Runnable 的 run()函数。

- Callable 与 Runnable 的功能大致相似，Callable 中有一个 call()函数，但是 call()函数有返回值，而 Runnable 的 run()函数不能将结果返回给客户程序。

- Executor 就是 Runnable 和 Callable 的调度容器，Future 就是对于具体的 Runnable 或者 Callable 任务的执行结果进行取消、查询是否完成、获取结果、设置结果操作。get 方法会阻塞，直到任务返回结果。

- FutureTask 则是一个 RunnableFuture<V>，而 RunnableFuture 实现了 Runnbale 又实现了 Futrue<V> 这两个接口。

## Threads & Runnables

Timer 计时器具备使任务延迟执行以及周期性执行的功能，但是 Timer 天生存在一些缺陷，所以从 JDK 1.5 开始就推荐使用 ScheduledThreadPoolExecutor（ScheduledExecutorService 实现类）作为其替代工具。

首先 Timer 对提交的任务调度是基于绝对时间而不是相对时间的，所以通过其提交的任务对系统时钟的改变是敏感的（譬如提交延迟任务后修改了系统时间会影响其执行）；而 ScheduledThreadExecutor 只支持相对时间，对系统时间不敏感。

接着 Timer 的另一个问题是如果 TimerTask 抛出未检查异常则 Timer 将会产生无法预料的行为，因为 Timer 线程并不捕获异常，所以 TimerTask 抛出的未检查异常会使 Timer 线程终止，所以后续提交的任务得不到执行；而 ScheduledThreadPoolExecutor 不存在此问题。

所有的现代操作系统都通过进程和线程来支持并发。进程是通常彼此独立运行的程序的实例，比如，如果你启动了一个 Java 程序，操作系统产生一个新的进程，与其他程序一起并行执行。在这些进程的内部，我们使用线程并发执行代码，因此，我们可以最大限度的利用 CPU 可用的核心(core)。 Java 从 JDK1.0 开始执行线程。在开始一个新的线程之前，你必须指定由这个线程执行的代码，通常称为 task。这可以通过实现 Runnable——一个定义了一个无返回值无参数的 run()方法的函数接口，如下面的代码所示：

```java
Runnable task = () -> {
    String threadName = Thread.currentThread().getName();
    System.out.println("Hello " + threadName);
};

task.run();

Thread thread = new Thread(task);
thread.start();

System.out.println("Done!");
```

因为 Runnable 是一个函数接口，所以我们利用 lambda 表达式将当前的线程名打印到控制台。首先，在开始一个线程前我们在主线程中直接运行 runnable。 控制台输出的结果可能像下面这样：

```
Hello main
Hello Thread-0
Done!
```

或者这样：

```
Hello main
Done!
Hello Thread-0
```

## Executors

## Fork/Join

# Concurrency Control | 并发控制

## Atomic Variables | 原子性与原子变量

## volatile | 可见性保障

## 锁与同步

# Async Programming | 异步编程

## Callable & Future

## CompletableFuture

## RxJava

# Thread Communication | 线程通信

# Built-in ThreadSafe DataStructure | 内置的线程安全模型

# Todos

- [concurrency-torture-testing-your-code-within-the-java-memory-model](http://zeroturnaround.com/rebellabs/concurrency-torture-testing-your-code-within-the-java-memory-model/)

- https://www.baeldung.com/java-fork-join
