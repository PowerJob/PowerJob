package tech.powerjob.server.netease.service;

import java.util.List;
import java.util.Set;

/**
 * @author Echo009
 * @since 2021/10/14
 */
public interface UserPermissionService {


    boolean hasPermission(String appId,String permissionType);


    Set<Long> listAppId();


}
