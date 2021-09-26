package com.netease.mail.chronos.portal.service.impl;

import com.netease.mail.chronos.portal.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Echo009
 * @since 2021/9/26
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {



    @Override
    public boolean checkPermission(String authStr) {
        return true;
    }



}
