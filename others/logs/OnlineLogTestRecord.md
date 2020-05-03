## V1.0.0
#### 持久化链路
1. 客户端使用内存队列异步化批量上报服务器
2. 服务器接收到请求后无脑写H2数据库
3. 任务结束后，流式同步到MongoDB持久化存储，维护一个包含Array的MongoDB对象
4. 同步结束后删除本地所有数据
#### 查询链路
* 如果本地存在数据，则直接从本地数据库返回
* 如果本地不存在数据，则直连MongoDB，获取数据再返回

***
问题主要在于前台展示：测试100W条数据，本地H2占用82M，MongoDB未知（因为mongo shell不知道为啥用不了...），不过应该也小不到哪里去。这种情况下数据都没办法回传回来...需要更新方案。

```text
org.apache.catalina.connector.ClientAbortException: java.io.IOException: Broken pipe
	at org.apache.catalina.connector.OutputBuffer.realWriteBytes(OutputBuffer.java:351)
	at org.apache.catalina.connector.OutputBuffer.flushByteBuffer(OutputBuffer.java:776)
	at org.apache.catalina.connector.OutputBuffer.append(OutputBuffer.java:681)
	at org.apache.catalina.connector.OutputBuffer.writeBytes(OutputBuffer.java:386)
	at org.apache.catalina.connector.OutputBuffer.write(OutputBuffer.java:364)
	at org.apache.catalina.connector.CoyoteOutputStream.write(CoyoteOutputStream.java:96)
	at com.fasterxml.jackson.core.json.UTF8JsonGenerator._flushBuffer(UTF8JsonGenerator.java:2137)
	at com.fasterxml.jackson.core.json.UTF8JsonGenerator.flush(UTF8JsonGenerator.java:1150)
	at com.fasterxml.jackson.databind.ObjectWriter.writeValue(ObjectWriter.java:923)
	at org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter.writeInternal(AbstractJackson2HttpMessageConverter.java:287)
	at org.springframework.http.converter.AbstractGenericHttpMessageConverter.write(AbstractGenericHttpMessageConverter.java:104)
	at org.springframework.web.servlet.mvc.method.annotation.AbstractMessageConverterMethodProcessor.writeWithMessageConverters(AbstractMessageConverterMethodProcessor.java:287)
	at org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor.handleReturnValue(RequestResponseBodyMethodProcessor.java:181)
	at org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite.handleReturnValue(HandlerMethodReturnValueHandlerComposite.java:82)
	at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:123)
	at org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver.doResolveHandlerMethodException(ExceptionHandlerExceptionResolver.java:403)
	at org.springframework.web.servlet.handler.AbstractHandlerMethodExceptionResolver.doResolveException(AbstractHandlerMethodExceptionResolver.java:61)
	at org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver.resolveException(AbstractHandlerExceptionResolver.java:141)
	at org.springframework.web.servlet.handler.HandlerExceptionResolverComposite.resolveException(HandlerExceptionResolverComposite.java:80)
	at org.springframework.web.servlet.DispatcherServlet.processHandlerException(DispatcherServlet.java:1300)
	at org.springframework.web.servlet.DispatcherServlet.processDispatchResult(DispatcherServlet.java:1111)
	at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1057)
	at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:943)
	at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1006)
	at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:898)
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:634)
	at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:883)
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:741)
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:231)
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
	at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53)
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
	at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100)
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
	at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93)
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
	at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201)
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
	at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:202)
	at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:96)
	at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:541)
	at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:139)
	at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92)
	at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:74)
	at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:343)
	at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:373)
	at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65)
	at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:868)
	at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1594)
	at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)
	at java.lang.Thread.run(Thread.java:748)
Caused by: java.io.IOException: Broken pipe
	at sun.nio.ch.FileDispatcherImpl.write0(Native Method)
	at sun.nio.ch.SocketDispatcher.write(SocketDispatcher.java:47)
	at sun.nio.ch.IOUtil.writeFromNativeBuffer(IOUtil.java:93)
	at sun.nio.ch.IOUtil.write(IOUtil.java:65)
	at sun.nio.ch.SocketChannelImpl.write(SocketChannelImpl.java:471)
	at org.apache.tomcat.util.net.NioChannel.write(NioChannel.java:138)
	at org.apache.tomcat.util.net.NioBlockingSelector.write(NioBlockingSelector.java:101)
	at org.apache.tomcat.util.net.NioSelectorPool.write(NioSelectorPool.java:152)
	at org.apache.tomcat.util.net.NioEndpoint$NioSocketWrapper.doWrite(NioEndpoint.java:1253)
	at org.apache.tomcat.util.net.SocketWrapperBase.doWrite(SocketWrapperBase.java:740)
	at org.apache.tomcat.util.net.SocketWrapperBase.writeBlocking(SocketWrapperBase.java:560)
	at org.apache.tomcat.util.net.SocketWrapperBase.write(SocketWrapperBase.java:504)
	at org.apache.coyote.http11.Http11OutputBuffer$SocketOutputBuffer.doWrite(Http11OutputBuffer.java:538)
	at org.apache.coyote.http11.filters.ChunkedOutputFilter.doWrite(ChunkedOutputFilter.java:110)
	at org.apache.coyote.http11.Http11OutputBuffer.doWrite(Http11OutputBuffer.java:190)
	at org.apache.coyote.Response.doWrite(Response.java:601)
	at org.apache.catalina.connector.OutputBuffer.realWriteBytes(OutputBuffer.java:339)
	... 60 common frames omitted

```

## V2.0.0
>经过小小的调查，mongoDB似乎允许用户直接使用它的文件系统:GridFS，完成文件的存储。那么要不要改成文件对文件的形式呢？同步开始时，先在本地生成日志文件，然后同步到MongoDB。查询时则先下载文件。一旦拥有了完整的文件，分页什么的也就容易实现了，前端展示一次1000行之类的～
