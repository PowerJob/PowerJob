package tech.powerjob.server.web.websocket;

import tech.powerjob.server.config.OmsEndpointConfigure;
import tech.powerjob.server.core.container.ContainerService;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.auth.LoginUserHolder;
import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.service.login.PowerJobLoginService;
import tech.powerjob.server.auth.service.permission.PowerJobPermissionService;
import tech.powerjob.server.persistence.remote.model.ContainerInfoDO;
import tech.powerjob.server.persistence.remote.repository.ContainerInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * 容器部署 WebSocket 服务
 * 记录一个不错的 WebSocket 测试网站：<a>http://www.easyswoole.com/wstool.html</a>
 *
 * @author tjq
 * @since 2020/5/17
 */
@Slf4j
@Component
@ServerEndpoint(value = "/container/deploy/{id}", configurator = OmsEndpointConfigure.class)
public class ContainerDeployServerEndpoint {

    @Resource
    private ContainerService containerService;

    @Resource
    private PowerJobLoginService powerJobLoginService;
    @Resource
    private PowerJobPermissionService permissionService;
    @Resource
    private ContainerInfoRepository containerInfoRepository;

    private static final String CONTAINER_ID = "containerId";
    private static final String AUTHENTICATED = "authenticated";

    @OnOpen
    public void onOpen(@PathParam("id") Long id, Session session) {
        session.getUserProperties().put(CONTAINER_ID, id);
        session.setMaxTextMessageBufferSize(16 * 1024);
        session.setMaxIdleTimeout(30000);
        session.getAsyncRemote().sendText("SYSTEM: authentication required before deployment");
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        synchronized (session) {
            if (session.getUserProperties().putIfAbsent(AUTHENTICATED, Boolean.TRUE) != null) {
                return;
            }
        }

        RemoteEndpoint.Async remote = session.getAsyncRemote();
        try {
            Map<String, Object> authentication = JsonUtils.parseMap(message);
            Object token = authentication.get("jwtToken");
            Optional<PowerJobUser> user = token instanceof String
                    ? powerJobLoginService.ifLogin((String) token) : Optional.empty();
            if (!user.isPresent()) {
                remote.sendText("SYSTEM: authentication denied");
                return;
            }
            Long id = (Long) session.getUserProperties().get(CONTAINER_ID);
            ContainerInfoDO container = containerInfoRepository.findById(id).orElse(null);
            if (container == null || !permissionService.hasPermission(user.get().getId(), RoleScope.APP, container.getAppId(), Permission.OPS)) {
                remote.sendText("SYSTEM: permission denied");
                return;
            }
            session.setMaxIdleTimeout(0);
            remote.sendText("SYSTEM: authenticated successfully, start to deploy container: " + id);
            containerService.deploy(id, session);
        }catch (Exception e) {
            // Parsing/authentication exceptions may contain the first-frame credential.
            log.warn("[ContainerDeployServerEndpoint] deployment request failed ({})", e.getClass().getSimpleName());
            remote.sendText("SYSTEM: authentication or deployment failed");
        } finally {
            LoginUserHolder.clean();
            try {
                session.close();
            } catch (IOException e) {
                log.warn("[ContainerDeployServerEndpoint] failed to close session");
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        try {
            session.close();
        } catch (IOException e) {
            log.error("[ContainerDeployServerEndpoint] close session failed.", e);
        }
        log.warn("[ContainerDeployServerEndpoint] session onError!", throwable);
    }
}
