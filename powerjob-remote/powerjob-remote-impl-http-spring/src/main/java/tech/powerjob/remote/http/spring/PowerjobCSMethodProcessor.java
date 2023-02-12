package tech.powerjob.remote.http.spring;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.remote.framework.actor.Handler;

import java.util.List;

/**
 * 带有 @Handler 注解的，接收的请求参数 使用 json 解析，等同于Spring中，在请求参数前使用注解：@RequestBody
 *
 * @author songyinyin
 * @see RequestResponseBodyMethodProcessor
 * @since 2023/2/12 18:02
 */
public class PowerjobCSMethodProcessor implements HandlerMethodArgumentResolver {

  private HandlerMethodArgumentResolver requestResponseBodyMethodProcessor;

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasMethodAnnotation(Handler.class);
  }

  @Override
  public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
    if (requestResponseBodyMethodProcessor == null) {
      RequestMappingHandlerAdapter requestMappingHandlerAdapter = SpringUtils.getBean(RequestMappingHandlerAdapter.class);
      this.requestResponseBodyMethodProcessor = getRequestResponseBodyMethodProcessor(requestMappingHandlerAdapter.getArgumentResolvers());
    }
    if (requestResponseBodyMethodProcessor == null) {
      throw new PowerJobException("requestResponseBodyMethodProcessor is null");
    }
    return requestResponseBodyMethodProcessor.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
  }

  private HandlerMethodArgumentResolver getRequestResponseBodyMethodProcessor(List<HandlerMethodArgumentResolver> resolvers) {
    if (resolvers == null) {
      return null;
    }
    for (HandlerMethodArgumentResolver resolver : resolvers) {
      if (resolver instanceof RequestResponseBodyMethodProcessor) {
        return resolver;
      }
    }
    return null;
  }
}
