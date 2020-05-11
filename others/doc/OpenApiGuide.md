# STEP4: OpenAPI
>OpenAPI允许开发者通过接口来完成手工的操作，让系统整体变得更加灵活，启用OpenAPI需要依赖`oh-my-scheduler-client`库。

* 最新依赖版本请参考Maven中央仓库：[推荐地址](https://search.maven.org/search?q=com.github.kfcfans)&[备用地址](https://mvnrepository.com/search?q=com.github.kfcfans)。
```xml
<dependency>
  <groupId>com.github.kfcfans</groupId>
  <artifactId>oh-my-scheduler-client</artifactId>
  <version>${oms.client.latest.version}</version>
</dependency>
```

### 简单示例
```java
// 初始化 client，需要server地址和应用名称作为参数
OhMyClient ohMyClient = new OhMyClient("127.0.0.1:7700", "oms-test");
// 调用相关的API
ohMyClient.stopInstance(1586855173043L)
```