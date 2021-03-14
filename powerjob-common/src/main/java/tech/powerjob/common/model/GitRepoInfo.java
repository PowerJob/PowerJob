package tech.powerjob.common.model;

import lombok.Data;

/**
 * The class for Git Repository info.
 *
 * @author tjq
 * @since 2020/5/17
 */
@Data
public class GitRepoInfo {
    /**
     * Address of Git repository.
     */
    private String repo;
    /**
     * Name of the branch.
     */
    private String branch;
    /**
     * username of Git.
     */
    private String username;
    /**
     * Password of Git.
     */
    private String password;
}
