# 容器测试日志
## ClassNotFound问题
>玩热加载这一套，不来几个ClassNotFound都没那味 [滑稽]～

测试容器化的MapReduce任务时，发现如下错误：
```text
2020-05-19 09:33:18 ERROR - [ProcessorRunnable-142925055284740224] execute failed, please fix this bug @tjq!
com.esotericsoftware.kryo.KryoException: Unable to find class: cn.edu.zju.oms.container.ContainerMRProcessor$TestSubTask
	at com.esotericsoftware.kryo.util.DefaultClassResolver.readName(DefaultClassResolver.java:182)
	at com.esotericsoftware.kryo.util.DefaultClassResolver.readClass(DefaultClassResolver.java:151)
	at com.esotericsoftware.kryo.Kryo.readClass(Kryo.java:684)
	at com.esotericsoftware.kryo.Kryo.readClassAndObject(Kryo.java:795)
	at SerializerUtils.deSerialized(SerializerUtils.java:48)
	at ProcessorRunnable.innerRun(ProcessorRunnable.java:63)
	at ProcessorRunnable.run(ProcessorRunnable.java:179)
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)
	at java.util.concurrent.FutureTask.run$$$capture(FutureTask.java:266)
	at java.util.concurrent.FutureTask.run(FutureTask.java)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:748)
Caused by: java.lang.ClassNotFoundException: cn.edu.zju.oms.container.ContainerMRProcessor$TestSubTask
	at java.net.URLClassLoader.findClass(URLClassLoader.java:382)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:418)
	at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:355)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:351)
	at java.lang.Class.forName0(Native Method)
	at java.lang.Class.forName(Class.java:348)
	at com.esotericsoftware.kryo.util.DefaultClassResolver.readName(DefaultClassResolver.java:176)
	... 12 common frames omitted
```

* 原因分析：经过分析，原有在于序列化与反序列化过程中，框架为了追求性能，采用了**对象池**技术（库存代码： a14f554e0085b6a179375a8ca04665434b73c7bd#SerializerUtils），而Kryo在序列化和反序列化过程中只会使用固定的类加载器（创建kryo的类对象（Kryo.class）的类加载器），因此无法找到由OMS自定义类加载器创建的容器类。
* 解决方案：弃用性能优异的对象池技术，该用ThreadLocal + 手动设置Kryo类加载器。