package tech.powerjob.client;

import com.alibaba.fastjson.JSON;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.request.http.SaveWorkflowNodeRequest;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.common.response.WorkflowNodeInfoDTO;
import tech.powerjob.common.serialize.JsonUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowNodeResponseTest {

    @Test
    void readsServerNodeNameThroughClient() throws Exception {
        assertEquals("节点 \"A\" 😀", saveNode("\"nodeName\":\"节点 \\\"A\\\" 😀\", ").getNodeAlias());
    }

    @Test
    void stillReadsLegacyNodeAliasThroughClient() throws Exception {
        assertEquals("legacy alias", saveNode("\"nodeAlias\":\"legacy alias\", ").getNodeAlias());
    }

    @Test
    void preservesEmptyNodeName() throws Exception {
        assertEquals("", saveNode("\"nodeName\":\"\", ").getNodeAlias());
    }

    @Test
    void allowsNullOrMissingName() throws Exception {
        assertNull(saveNode("\"nodeName\":null, ").getNodeAlias());
        assertNull(saveNode("").getNodeAlias());
    }

    @Test
    void preservesExistingAccessorsAndSerializedName() {
        WorkflowNodeInfoDTO node = new WorkflowNodeInfoDTO();
        node.setNodeAlias("existing caller");
        assertEquals("existing caller", node.getNodeAlias());
        for (String json : new String[]{JSON.toJSONString(node), JsonUtils.toJSONStringUnsafe(node)}) {
            assertEquals("existing caller", JSON.parseObject(json).getString("nodeAlias"));
            assertFalse(JSON.parseObject(json).containsKey("nodeName"));
        }
    }

    private WorkflowNodeInfoDTO saveNode(String nameField) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger writes = new AtomicInteger();
        server.createContext("/openApi/authApp", exchange -> {
            while (exchange.getRequestBody().read() != -1) { /* Consume without logging credentials. */ }
            byte[] body = "{\"success\":true,\"data\":{\"appId\":7,\"token\":\"test-token\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("X-POWERJOB-AUTH-PASSED", "true");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/openApi/addWorkflowNode", exchange -> {
            while (exchange.getRequestBody().read() != -1) { /* Consume the request before responding. */ }
            writes.incrementAndGet();
            byte[] body = ("{\"success\":true,\"data\":[{" + nameField
                    + "\"id\":9007199254740993,\"appId\":7,\"jobId\":23,\"nodeParams\":\"参数\","
                    + "\"enable\":false,\"skipWhenFailed\":true}]}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("X-POWERJOB-AUTH-PASSED", "true");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try (PowerJobClient client = new PowerJobClient("127.0.0.1:" + server.getAddress().getPort(), "test-app", "test-password")) {
            SaveWorkflowNodeRequest request = new SaveWorkflowNodeRequest();
            request.setJobId(23L);
            ResultDTO<List<WorkflowNodeInfoDTO>> result = client.saveWorkflowNode(Collections.singletonList(request));
            assertTrue(result.isSuccess());
            assertEquals(1, result.getData().size());
            assertEquals(1, writes.get());
            assertEquals(Long.valueOf(7), request.getAppId());
            WorkflowNodeInfoDTO node = result.getData().get(0);
            assertEquals(Long.valueOf(9007199254740993L), node.getId());
            assertEquals(Long.valueOf(7), node.getAppId());
            assertEquals(Long.valueOf(23), node.getJobId());
            assertEquals("参数", node.getNodeParams());
            assertFalse(node.getEnable());
            assertTrue(node.getSkipWhenFailed());
            return node;
        } finally {
            server.stop(0);
        }
    }
}
