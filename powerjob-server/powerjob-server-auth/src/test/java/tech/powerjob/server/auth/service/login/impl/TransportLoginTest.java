package tech.powerjob.server.auth.service.login.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import tech.powerjob.common.enums.SwitchableStatus;
import tech.powerjob.server.auth.LoginUserHolder;
import tech.powerjob.server.auth.common.PowerJobAuthException;
import tech.powerjob.server.auth.jwt.JwtService;
import tech.powerjob.server.auth.jwt.ParseResult;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransportLoginTest {
    private final JwtService jwt = mock(JwtService.class);
    private final UserInfoRepository users = mock(UserInfoRepository.class);
    private final PowerJobLoginServiceImpl login = new PowerJobLoginServiceImpl(jwt, users, Collections.emptyList());

    @AfterEach
    void cleanUserContext() { LoginUserHolder.clean(); }

    @Test
    void directTokenAndBothHttpHeaderNamesUseCurrentUserRecord() {
        acceptedToken();
        UserInfoDO user = user();
        when(users.findByUsername("synthetic-user")).thenReturn(Optional.of(user));
        assertEquals(7L, login.ifLogin("synthetic-token").get().getId().longValue());
        for (String header : new String[]{"PowerJwt", "Power_jwt"}) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(header, "synthetic-token");
            assertEquals(7L, login.ifLogin(request).get().getId().longValue());
        }
        verify(users, times(3)).findByUsername("synthetic-user");
    }

    @Test
    void disabledUserCannotAuthenticateThroughNonHttpTransport() {
        acceptedToken();
        UserInfoDO user = user();
        user.setStatus(SwitchableStatus.DISABLE.getV());
        when(users.findByUsername("synthetic-user")).thenReturn(Optional.of(user));
        assertThrows(PowerJobAuthException.class, () -> login.ifLogin("synthetic-token"));
        assertNull(LoginUserHolder.get());
    }

    @Test
    void revocationMarkerCannotBeBypassedByDirectToken() {
        acceptedToken();
        UserInfoDO user = user();
        user.setTokenLoginVerifyInfo("{\"encryptedToken\":\"new-password-marker\"}");
        when(users.findByUsername("synthetic-user")).thenReturn(Optional.of(user));
        assertThrows(PowerJobAuthException.class, () -> login.ifLogin("synthetic-token"));
        assertNull(LoginUserHolder.get());
    }

    @Test
    void absentAndRejectedTokensDoNotReadUserRecords() {
        assertFalse(login.ifLogin((String) null).isPresent());
        assertFalse(login.ifLogin("").isPresent());
        when(jwt.parse("rejected", null)).thenReturn(new ParseResult().setStatus(ParseResult.Status.FAILED));
        assertFalse(login.ifLogin("rejected").isPresent());
        verifyNoInteractions(users);
        assertNull(LoginUserHolder.get());
    }

    private void acceptedToken() {
        when(jwt.parse("synthetic-token", null)).thenReturn(new ParseResult().setStatus(ParseResult.Status.SUCCESS)
                .setResult(Collections.singletonMap("username", "synthetic-user")));
    }

    private UserInfoDO user() {
        UserInfoDO user = new UserInfoDO();
        user.setId(7L);
        user.setUsername("synthetic-user");
        user.setStatus(SwitchableStatus.ENABLE.getV());
        return user;
    }
}
