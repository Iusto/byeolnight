package com.byeolnight.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Spring Security의 OAuth2 사용자 조회 흐름과 애플리케이션 계정 처리를 연결한다.
 *
 * <p>외부 제공자 호출은 이 클래스에서 끝내고, DB를 사용하는 회원 판별은
 * {@link OAuthAccountService}에 위임한다. 외부 HTTP 호출 중에는 불필요한 DB
 * 트랜잭션을 유지하지 않기 위한 구조다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OAuthAccountService oauthAccountService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        String provider = userRequest.getClientRegistration().getRegistrationId();

        try {
            // Spring Security가 제공자 UserInfo API를 호출해 원본 속성을 가져온다.
            OAuth2User oAuth2User = super.loadUser(userRequest);
            OAuth2UserInfoFactory.OAuth2UserInfo userInfo =
                    OAuth2UserInfoFactory.getOAuth2UserInfo(provider, oAuth2User);

            // 제공자별 속성 해석 이후의 회원 판별과 저장은 트랜잭션 서비스에 맡긴다.
            return new CustomOAuth2User(
                    oauthAccountService.authenticate(provider, userInfo),
                    oAuth2User.getAttributes()
            );
        } catch (OAuth2AuthenticationException e) {
            log.warn("소셜 로그인 실패: 오류코드={}, 제공자={}", e.getError().getErrorCode(), provider);
            throw e;
        }
    }
}
