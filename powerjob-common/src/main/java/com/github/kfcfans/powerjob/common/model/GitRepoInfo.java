package com.github.kfcfans.powerjob.common.model;

import lombok.Data;

/**
 * Git代码库信息
 *
 * @author tjq
 * @since 2020/5/17
 */
@Data
public class GitRepoInfo {
    // 仓库地址
    private String repo;
    // 分支名称
    private String branch;
    // 用户名
    private String username;
    // 密码
    private String password;
}
