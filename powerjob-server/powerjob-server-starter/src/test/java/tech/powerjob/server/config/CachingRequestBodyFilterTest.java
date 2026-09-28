package tech.powerjob.server.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.StreamUtils;

import javax.servlet.ServletInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CachingRequestBodyFilterTest {

    private final CachingRequestBodyFilter filter = new CachingRequestBodyFilter();

    @ParameterizedTest
    @ValueSource(strings = {"", "/powerjob", "/nested/server"})
    void jarUploadPreservesUnreadBinaryBody(String contextPath) throws Exception {
        ByteArrayOutputStream multipartBody = new ByteArrayOutputStream();
        multipartBody.write(("--test-boundary\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"processor.jar\"\r\n"
                + "Content-Type: application/java-archive\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        multipartBody.write(new byte[]{0x50, 0x4b, 0x03, 0x04, 0, (byte) 0x80, (byte) 0xff});
        multipartBody.write("\r\n--test-boundary--\r\n".getBytes(StandardCharsets.UTF_8));
        byte[] body = multipartBody.toByteArray();
        TrackingRequest request = request(contextPath, "/container/jarUpload",
                "multipart/form-data; boundary=test-boundary", body);
        AtomicInteger chainCalls = new AtomicInteger();

        filter.doFilter(request, new MockHttpServletResponse(), (filteredRequest, response) -> {
            chainCalls.incrementAndGet();
            assertEquals(0, request.inputStreamAccessCount, "The filter must not consume the upload body");
            assertSame(request, filteredRequest);
            assertArrayEquals(body, StreamUtils.copyToByteArray(filteredRequest.getInputStream()));
        });

        assertEquals(1, chainCalls.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/powerjob", "/nested/server"})
    void jsonBodyRemainsRepeatable(String contextPath) throws Exception {
        String json = "{\"jobId\":1,\"name\":\"test\"}";
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        TrackingRequest request = request(contextPath, "/job/save", "application/json", body);
        AtomicInteger chainCalls = new AtomicInteger();

        filter.doFilter(request, new MockHttpServletResponse(), (filteredRequest, response) -> {
            chainCalls.incrementAndGet();
            assertTrue(filteredRequest instanceof CachingRequestBodyFilter.CustomHttpServletRequestWrapper);
            assertArrayEquals(body, StreamUtils.copyToByteArray(filteredRequest.getInputStream()));
            assertArrayEquals(body, StreamUtils.copyToByteArray(filteredRequest.getInputStream()));
            assertEquals(json, filteredRequest.getReader().readLine());
            assertEquals(json, filteredRequest.getReader().readLine());
        });

        assertEquals(1, chainCalls.get());
        assertEquals(1, request.inputStreamAccessCount);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/container/jarUploadOther", "/other/container/jarUpload", "/container/jarUpload/child"})
    void similarPathsStillCacheTheBody(String path) throws Exception {
        byte[] body = "{\"jobId\":1}".getBytes(StandardCharsets.UTF_8);
        TrackingRequest request = request("/powerjob", path, "application/json", body);
        AtomicInteger chainCalls = new AtomicInteger();

        filter.doFilter(request, new MockHttpServletResponse(), (filteredRequest, response) -> {
            chainCalls.incrementAndGet();
            assertTrue(filteredRequest instanceof CachingRequestBodyFilter.CustomHttpServletRequestWrapper);
            assertArrayEquals(body, StreamUtils.copyToByteArray(filteredRequest.getInputStream()));
            assertArrayEquals(body, StreamUtils.copyToByteArray(filteredRequest.getInputStream()));
        });

        assertEquals(1, chainCalls.get());
        assertEquals(1, request.inputStreamAccessCount);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"application/x-www-form-urlencoded", "multipart/form-data"})
    void ignoredContentTypesKeepTheOriginalRequest(String contentType) throws Exception {
        byte[] body = "jobId=1".getBytes(StandardCharsets.UTF_8);
        TrackingRequest request = request("/powerjob", "/job/save", contentType, body);
        AtomicInteger chainCalls = new AtomicInteger();

        filter.doFilter(request, new MockHttpServletResponse(), (filteredRequest, response) -> {
            chainCalls.incrementAndGet();
            assertEquals(0, request.inputStreamAccessCount);
            assertSame(request, filteredRequest);
            assertArrayEquals(body, StreamUtils.copyToByteArray(filteredRequest.getInputStream()));
        });

        assertEquals(1, chainCalls.get());
    }

    private static TrackingRequest request(String contextPath, String path, String contentType, byte[] body) {
        TrackingRequest request = new TrackingRequest();
        request.setMethod("POST");
        request.setContextPath(contextPath);
        request.setRequestURI(contextPath + path);
        request.setContentType(contentType);
        request.setContent(body);
        return request;
    }

    private static class TrackingRequest extends MockHttpServletRequest {

        private int inputStreamAccessCount;

        @Override
        public ServletInputStream getInputStream() {
            inputStreamAccessCount++;
            return super.getInputStream();
        }
    }
}
