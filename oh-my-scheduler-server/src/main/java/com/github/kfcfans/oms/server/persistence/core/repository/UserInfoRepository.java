package com.github.kfcfans.oms.server.persistence.core.repository;

import com.github.kfcfans.oms.server.persistence.core.model.UserInfoDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 用户信息表数据库访问层
 *
 * @author tjq
 * @since 2020/4/12
 */
public interface UserInfoRepository extends JpaRepository<UserInfoDO, Long> {

    List<UserInfoDO> findByUsernameLike(String username);

    List<UserInfoDO> findByIdIn(List<Long> userIds);
}
