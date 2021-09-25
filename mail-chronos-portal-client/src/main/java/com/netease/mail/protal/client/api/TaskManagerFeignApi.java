package com.netease.mail.protal.client.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author Echo009
 * @since 2021/9/26
 */
@FeignClient(value = "mail-chronos-portal")
public interface TaskManagerFeignApi {







}
