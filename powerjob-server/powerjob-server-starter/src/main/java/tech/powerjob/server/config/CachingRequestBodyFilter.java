package tech.powerjob.server.config;

import com.google.common.collect.Sets;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.util.Set;

/**
 * 解决 HttpServletRequest 只能被读取一次的问题，方便全局日志 & 鉴权，切面提前读取数据
 * 在请求进入Servlet容器之前，先经过Filter的过滤器链。在请求进入Controller之前，先经过 HandlerInterceptor 的拦截器链。Filter 一定先于 HandlerInterceptor 执行
 * <a href="https://juejin.cn/post/6858037733776949262">解决HttpServletRequest 流数据不可重复读</a>
 *
 * @author tjq
 * @since 2024/2/11
 */
@Component
public class CachingRequestBodyFilter implements Filter {


    /**
     * 忽略部分不需要处理的类型：
     * GET 请求的数据一般是 Query String，直接在 url 的后面，不需要特殊处理
     * multipart/form-data：doDispatch() 阶段就会进行处理，此处已经空值了，强行处理会导致结果空
     * application/x-www-form-urlencoded：估计也类似，有特殊逻辑，导致 OpenAPI 部分请求参数无法传递，同样忽略
     */
    private static final Set<String> IGNORE_CONTENT_TYPES = Sets.newHashSet("application/x-www-form-urlencoded", "multipart/form-data");

    private static final Set<String> IGNORE_URIS = Sets.newHashSet("/container/jarUpload");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            String uri = ((HttpServletRequest) request).getRequestURI();
            // 忽略 jar 上传等处理路径
            if (IGNORE_URIS.contains(uri)) {
                chain.doFilter(request, response);
                return;
            }
            String contentType = request.getContentType();
            if (contentType != null && !IGNORE_CONTENT_TYPES.contains(contentType)) {
                CustomHttpServletRequestWrapper wrappedRequest = new CustomHttpServletRequestWrapper((HttpServletRequest) request);
                chain.doFilter(wrappedRequest, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    // Implement other required methods like init() and destroy() if necessary


    public static class CustomHttpServletRequestWrapper extends HttpServletRequestWrapper {

        private final String body;

        public CustomHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = null;
            try {
                InputStream inputStream = request.getInputStream();
                if (inputStream != null) {
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    char[] charBuffer = new char[128];
                    int bytesRead = -1;
                    while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                        stringBuilder.append(charBuffer, 0, bytesRead);
                    }
                }
            } finally {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            }
            body = stringBuilder.toString();
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes());

            return new ServletInputStream() {
                public int read() throws IOException {
                    return byteArrayInputStream.read();
                }

                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException("Not implemented");
                }
            };
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(this.getInputStream()));
        }

        public String getBody() {
            return this.body;
        }
    }

}
