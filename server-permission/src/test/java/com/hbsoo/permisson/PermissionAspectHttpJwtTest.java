package com.hbsoo.permisson;

import com.hbsoo.permisson.utils.AESUtil;
import com.hbsoo.permisson.utils.JwtUtils;
import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.annotation.Permission;
import com.hbsoo.server.annotation.Protocol;
import com.hbsoo.server.message.entity.HttpPacket;
import com.hbsoo.server.message.server.HttpServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionAspectHttpJwtTest {

    @Mock
    private ChannelHandlerContext ctx;
    @Mock
    private OutsideUserSessionManager outsideUserSessionManager;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private MethodSignature methodSignature;

    @Spy
    private TestProtectedHandler handler = new TestProtectedHandler();

    private JwtUtils jwtUtils;
    private PermissionAspect aspect;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        AESUtil aesUtil = new AESUtil();
        jwtUtils = new JwtUtils();
        jwtUtils.setSecret("aspect-test-secret-key-32bytes-long!");
        ReflectionTestUtils.setField(jwtUtils, "aesUtil", aesUtil);

        aspect = new PermissionAspect();
        ReflectionTestUtils.setField(aspect, "jwtUtils", jwtUtils);
        ReflectionTestUtils.setField(aspect, "outsideUserSessionManager", outsideUserSessionManager);
        ReflectionTestUtils.setField(handler, "outsideUserSessionManager", outsideUserSessionManager);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("handle");
        when(methodSignature.getDeclaringType()).thenReturn((Class) TestProtectedHandler.class);
        when(methodSignature.getMethod()).thenReturn(
                TestProtectedHandler.class.getMethod("handle", ChannelHandlerContext.class, HttpPacket.class));
        when(joinPoint.getTarget()).thenReturn(handler);
    }

    @Test
    void http_withoutToken_deniesAndDoesNotInvokeHandler() throws Throwable {
        HttpPacket packet = httpPacket(null);
        when(joinPoint.getArgs()).thenReturn(new Object[]{ctx, packet});

        Object result = aspect.around(joinPoint);

        assertNull(result);
        verify(handler, never()).handle(any(ChannelHandlerContext.class), any(HttpPacket.class));
    }

    @Test
    void http_withValidAdminToken_allowsHandler() throws Throwable {
        String token = jwtUtils.generateToken("u1", Map.of("permission", "ADMIN"));
        HttpPacket packet = httpPacket(token);
        when(joinPoint.getArgs()).thenReturn(new Object[]{ctx, packet});
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            handler.handle(ctx, packet);
            return null;
        });

        aspect.around(joinPoint);

        verify(handler).handle(ctx, packet);
    }

    private HttpPacket httpPacket(String authentication) {
        HttpPacket packet = new HttpPacket();
        HttpHeaders headers = new DefaultHttpHeaders();
        if (authentication != null) {
            headers.set("Authentication", authentication);
        }
        packet.setHeaders(headers);
        return packet;
    }

    @PermissionAuth(permission = {Permission.ADMIN})
    @OutsideMessageHandler(value = 0, uri = "/test/protected", protocol = Protocol.HTTP)
    static class TestProtectedHandler extends HttpServerMessageDispatcher {
        @Override
        public void handle(ChannelHandlerContext ctx, HttpPacket httpPacket) {
        }
    }
}
