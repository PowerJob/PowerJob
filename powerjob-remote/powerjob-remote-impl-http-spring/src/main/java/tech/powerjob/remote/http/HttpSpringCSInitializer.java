package tech.powerjob.remote.http;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;
import tech.powerjob.remote.framework.actor.ActorInfo;
import tech.powerjob.remote.framework.actor.HandlerInfo;
import tech.powerjob.remote.framework.cs.CSInitializer;
import tech.powerjob.remote.framework.cs.CSInitializerConfig;
import tech.powerjob.remote.framework.transporter.Transporter;
import tech.powerjob.remote.http.spring.SpringMvcTransporter;
import tech.powerjob.remote.http.spring.SpringUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author songyinyin
 * @since 2023/2/11 19:55
 */
public class HttpSpringCSInitializer implements CSInitializer {

  private RequestMappingHandlerMapping requestMappingHandlerMapping;

  @Override
  public String type() {
    return tech.powerjob.common.enums.Protocol.HTTP.name();
  }

  @Override
  public void init(CSInitializerConfig config) {
    this.requestMappingHandlerMapping = (RequestMappingHandlerMapping) SpringUtils.getBean("requestMappingHandlerMapping");
  }

  @Override
  public Transporter buildTransporter() {
    return new SpringMvcTransporter();
  }

  @Override
  public void bindHandlers(List<ActorInfo> actorInfos) {
    for (ActorInfo actorInfo : actorInfos) {
      for (HandlerInfo handlerInfo : actorInfo.getHandlerInfos()) {

        RequestMappingInfo.BuilderConfiguration options = new RequestMappingInfo.BuilderConfiguration();
        options.setPatternParser(new PathPatternParser());
        RequestMappingInfo mapping = RequestMappingInfo.paths(handlerInfo.getLocation().toPath())
            .methods(RequestMethod.POST)
            // 处理请求的提交内容类型
//            .consumes(MediaType.APPLICATION_JSON_VALUE)
            // 返回的内容类型
            .produces(MediaType.APPLICATION_JSON_VALUE)
            .options(options)
            .build();
        Method method = handlerInfo.getMethod();
        requestMappingHandlerMapping.registerMapping(mapping, actorInfo.getActor(), method);
      }
    }
  }

  @Override
  public void close() throws IOException {

  }


}
