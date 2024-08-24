package tech.powerjob.server.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.interceptor.ApiPermission;
import tech.powerjob.server.common.options.WebOption;
import tech.powerjob.server.infrastructure.config.Config;
import tech.powerjob.server.infrastructure.config.ConfigItem;
import tech.powerjob.server.infrastructure.config.DynamicServerConfigCrudService;

import java.util.List;

/**
 * 系统设置 controller
 *
 * @author tjq
 * @since 2024/8/24
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/config")
public class SystemConfigController {

    private final DynamicServerConfigCrudService dynamicServerConfigCrudService;

    @PostMapping("/save")
    @ApiPermission(name = "Config-Save", roleScope = RoleScope.GLOBAL, requiredPermission = Permission.SU)
    public ResultDTO<Void> saveConfig(@RequestBody Config config) {
        dynamicServerConfigCrudService.save(config);
        return ResultDTO.success(null);
    }

    @GetMapping("/list")
    @ApiPermission(name = "Config-List", roleScope = RoleScope.GLOBAL, requiredPermission = Permission.SU)
    public ResultDTO<List<Config>> listConfig() {
        return ResultDTO.success(dynamicServerConfigCrudService.list());
    }

    @GetMapping("/configItemOptions")
    @ApiPermission(name = "Config-ConfigItemOptions", roleScope = RoleScope.GLOBAL, requiredPermission = Permission.SU)
    public ResultDTO<List<WebOption>> configItemOptions() {
        return ResultDTO.success(WebOption.build(ConfigItem.class));
    }

    @DeleteMapping("/delete")
    @ApiPermission(name = "Config-Delete", roleScope = RoleScope.GLOBAL, requiredPermission = Permission.SU)
    public ResultDTO<Void> deleteNamespace(String key) {
        dynamicServerConfigCrudService.delete(key);
        return ResultDTO.success(null);
    }
}
