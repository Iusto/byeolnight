package com.byeolnght.servce.auth;

mport com.byeolnght.doman.entty.user.User;
mport com.byeolnght.doman.repostory.UserRepostory;
mport com.byeolnght.nfrastructure.securty.JwtTokenProvder;
mport org.junt.jupter.ap.BeforeEach;
mport org.junt.jupter.ap.Test;
mport org.junt.jupter.ap.extenson.ExtendWth;
mport org.mockto.Mock;
mport org.mockto.junt.jupter.MocktoExtenson;
mport org.sprngframework.mock.web.MockHttpServletRequest;
mport org.sprngframework.mock.web.MockHttpServletResponse;

mport java.utl.Optonal;
mport java.utl.UUD;

mport statc org.assertj.core.ap.Assertons.*;
mport statc org.mockto.ArgumentMatchers.*;
mport statc org.mockto.Mockto.*;

@ExtendWth(MocktoExtenson.class)
class AuthServceTest {

    @Mock
    prvate UserRepostory userRepostory;
    @Mock
    prvate JwtTokenProvder jwtTokenProvder;
    @Mock
    prvate SessonServce sessonServce;
    @Mock
    prvate OAuth2Servce oauth2Servce;
    @Mock
    prvate MagcLnkServce magcLnkServce;
    @Mock
    prvate AudtServce audtServce;

    prvate AuthServce authServce;
    prvate User testUser;

    @BeforeEach
    vod setUp() {
        authServce = new AuthServce(userRepostory, jwtTokenProvder, sessonServce, 
                oauth2Servce, magcLnkServce, audtServce);
        
        testUser = User.bulder()
                .d(1L)
                .emal("test@example.com")
                .nckname("testuser")
                .role(User.Role.USER)
                .emalVerfed(true)
                .buld();
    }

    @Test
    vod refreshToken__() {
        
        Strng sessond = UUD.randomUUD().toStrng();
        Strng oldJt = UUD.randomUUD().toStrng();
        Strng refreshToken = "vald-refresh-token";
        
        SessonServce.SessonData sesson = SessonServce.SessonData.bulder()
                .userd("1")
                .rtJt(oldJt)
                .revoked(false)
                .buld();

        when(jwtTokenProvder.valdateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvder.getSessond(refreshToken)).thenReturn(sessond);
        when(jwtTokenProvder.getJt(refreshToken)).thenReturn(oldJt);
        when(jwtTokenProvder.getSubject(refreshToken)).thenReturn("test@example.com");
        when(sessonServce.getSesson(sessond)).thenReturn(sesson);
        when(userRepostory.fndByEmal("test@example.com")).thenReturn(Optonal.of(testUser));
        when(jwtTokenProvder.createAccessToken(any(), eq(sessond))).thenReturn("new-access-token");
        when(jwtTokenProvder.createRefreshToken(any(), eq(sessond), any())).thenReturn("new-refresh-token");
        when(sessonServce.rotateRefreshToken(eq(sessond), any())).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        
        AuthServce.TokenResponse result = authServce.refreshToken(refreshToken, request, response);

        
        assertThat(result.getAccessToken()).sEqualTo("new-access-token");
        assertThat(result.getExpresn()).sEqualTo(900);
        verfy(sessonServce).rotateRefreshToken(eq(sessond), any());
        verfy(audtServce).logTokenRefresh(1L, sessond);
    }

    @Test
    vod refreshToken__() {
        
        Strng sessond = UUD.randomUUD().toStrng();
        Strng oldJt = UUD.randomUUD().toStrng();
        Strng dfferentJt = UUD.randomUUD().toStrng();
        Strng refreshToken = "reused-refresh-token";
        
        SessonServce.SessonData sesson = SessonServce.SessonData.bulder()
                .userd("1")
                .rtJt(dfferentJt)
                .revoked(false)
                .buld();

        when(jwtTokenProvder.valdateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvder.getSessond(refreshToken)).thenReturn(sessond);
        when(jwtTokenProvder.getJt(refreshToken)).thenReturn(oldJt);
        when(sessonServce.getSesson(sessond)).thenReturn(sesson);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        
        assertThatThrownBy(() -> authServce.refreshToken(refreshToken, request, response))
                .snstanceOf(RuntmeExcepton.class)
                .hasMessage("  ");

        verfy(sessonServce).revokeSesson(sessond);
        verfy(audtServce).logTokenReuse(1L, sessond, oldJt);
    }
}
