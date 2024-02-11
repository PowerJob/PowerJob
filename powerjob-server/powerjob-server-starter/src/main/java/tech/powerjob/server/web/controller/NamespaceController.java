package tech.powerjob.server.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.interceptor.ApiPermission;
import tech.powerjob.server.auth.interceptor.dp.ModifyOrCreateDynamicPermission;
import tech.powerjob.server.auth.interceptor.gp.SaveNamespaceGrantPermissionPlugin;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.persistence.remote.model.NamespaceDO;
import tech.powerjob.server.persistence.remote.repository.NamespaceRepository;
import tech.powerjob.server.web.converter.NamespaceConverter;
import tech.powerjob.server.web.request.ModifyNamespaceRequest;
import tech.powerjob.server.web.request.QueryNamespaceRequest;
import tech.powerjob.server.web.response.NamespaceBaseVO;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 命名空间 Controller
 *
 * @author tjq
 * @since 2023/9/3
 */
@Slf4j
@RestController
@RequestMapping("/namespace")
public class NamespaceController {

    @Resource
    private NamespaceRepository namespaceRepository;

    @ResponseBody
    @PostMapping("/save")
    @ApiPermission(name = "Namespace-Save", roleScope = RoleScope.NAMESPACE, dynamicPermissionPlugin = ModifyOrCreateDynamicPermission.class, grandPermissionPlugin = SaveNamespaceGrantPermissionPlugin.class)
    public ResultDTO<NamespaceBaseVO> save(@RequestBody ModifyNamespaceRequest req) {

        req.valid();

        Long id = req.getId();
        NamespaceDO namespaceDO;
        if (id == null) {
            namespaceDO = new NamespaceDO();
            namespaceDO.setGmtCreate(new Date());

            // code 单独拷贝
            namespaceDO.setCode(req.getCode());
            // 创建时生成 token
            namespaceDO.setToken(UUID.randomUUID().toString());

        } else {
            namespaceDO = fetchById(id);
        }

        // 拷贝通用变更属性（code 不允许更改）
        namespaceDO.setName(req.getName());
        namespaceDO.setExtra(req.getExtra());
        namespaceDO.setStatus(Optional.ofNullable(req.getStatus()).orElse(SwitchableStatus.ENABLE.getV()));

        namespaceDO.setGmtModified(new Date());
        NamespaceDO savedNamespace = namespaceRepository.save(namespaceDO);
        return ResultDTO.success(NamespaceConverter.do2BaseVo(savedNamespace));
    }

    @PostMapping("/list")
    public ResultDTO<List<NamespaceBaseVO>> listNamespace(@RequestBody QueryNamespaceRequest queryNamespaceRequest) {
        List<NamespaceDO> allDos = namespaceRepository.findAll();
        return ResultDTO.success(allDos.stream().map(NamespaceConverter::do2BaseVo).collect(Collectors.toList()));
    }

    private NamespaceDO fetchById(Long id) {
        Optional<NamespaceDO> namespaceDoOpt = namespaceRepository.findById(id);
        if (!namespaceDoOpt.isPresent()) {
            throw new IllegalArgumentException("can't find namespace by id: " + id);
        }
        return namespaceDoOpt.get();
    }

}
