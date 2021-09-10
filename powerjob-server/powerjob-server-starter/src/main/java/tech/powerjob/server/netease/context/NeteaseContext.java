package tech.powerjob.server.netease.context;

import tech.powerjob.server.persistence.remote.model.UserInfoDO;

/**
 * @author Echo009
 * @since 2021/9/10
 */
public class NeteaseContext {


    private static final ThreadLocal<UserInfoDO> HOLDER = ThreadLocal.withInitial(()-> null);


    private NeteaseContext(){

    }

    public static UserInfoDO getCurrentUser(){
        return HOLDER.get();
    }


    public static void setCurrentUser(UserInfoDO userInfo){
        HOLDER.set(userInfo);
    }


    public static void clearCurrentUser(){
        HOLDER.remove();
    }

}
