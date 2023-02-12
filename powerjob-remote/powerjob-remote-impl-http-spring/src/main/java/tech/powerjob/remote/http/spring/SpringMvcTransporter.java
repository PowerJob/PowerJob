package tech.powerjob.remote.http.spring;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import tech.powerjob.common.PowerSerializable;
import tech.powerjob.remote.framework.base.RemotingException;
import tech.powerjob.remote.framework.base.URL;
import tech.powerjob.remote.framework.transporter.Protocol;
import tech.powerjob.remote.framework.transporter.Transporter;
import tech.powerjob.remote.http.HttpProtocol;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * spring-webmvc 使用 RestTemplate 发送http请求，后续兼容 spring-webflux 后，部分请求可以是非阻塞式的
 *
 * @author songyinyin
 * @since 2023/2/12 11:43
 */
public class SpringMvcTransporter implements Transporter {

  private static final Protocol PROTOCOL = new HttpProtocol();

  private final RestTemplate restTemplate = new RestTemplate();

  @Override
  public Protocol getProtocol() {
    return PROTOCOL;
  }

  @Override
  public void tell(URL url, PowerSerializable request) {
    String fullUrl = getFullUrl(url);
    restTemplate.postForEntity(fullUrl, request, String.class);
  }

  @Override
  public <T> CompletionStage<T> ask(URL url, PowerSerializable request, Class<T> clz) throws RemotingException {
    String fullUrl = getFullUrl(url);
    ResponseEntity<T> responseEntity = restTemplate.postForEntity(fullUrl, request, clz);
    // throw exception
    final int statusCode = responseEntity.getStatusCodeValue();
    if (statusCode != HttpStatus.OK.value()) {
      // CompletableFuture.get() 时会传递抛出该异常
      throw new RemotingException(String.format("request [url:%s] failed, status: %d, msg: %s",
          fullUrl, statusCode, responseEntity.getBody()
      ));
    }
    return CompletableFuture.completedFuture(responseEntity.getBody());
  }

  private String getFullUrl(URL url) {
    return "http://" + url.getAddress().toFullAddress() + url.getLocation().toPath();
  }
}
