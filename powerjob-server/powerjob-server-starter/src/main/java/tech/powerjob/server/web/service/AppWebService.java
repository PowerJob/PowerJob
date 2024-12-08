package tech.powerjob.server.web.service;

import org.springframework.data.domain.Page;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.web.request.ModifyAppInfoRequest;
import tech.powerjob.server.web.request.QueryAppInfoRequest;

import java.util.Optional;

/**
 * AppWebService
 *
 * @author tjq
 * @since 2024/12/8
 */
public interface AppWebService {

    AppInfoDO save(ModifyAppInfoRequest request);

    void delete(Long id);

    Optional<AppInfoDO> findByAppName(String appName);

    Page<AppInfoDO> list(QueryAppInfoRequest queryAppInfoRequest);
}
