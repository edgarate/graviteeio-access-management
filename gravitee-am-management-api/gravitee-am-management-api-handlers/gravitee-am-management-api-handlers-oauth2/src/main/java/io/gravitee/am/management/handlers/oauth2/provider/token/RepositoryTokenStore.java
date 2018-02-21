/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.am.management.handlers.oauth2.provider.token;

import io.gravitee.am.management.handlers.oauth2.provider.RepositoryProviderUtils;
import io.gravitee.am.repository.oauth2.api.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RepositoryTokenStore implements TokenStore {

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private AuthenticationKeyGenerator authenticationKeyGenerator;

    @Override
    public OAuth2Authentication readAuthentication(OAuth2AccessToken token) {
        // TODO move to async call
        Optional<io.gravitee.am.repository.oauth2.model.OAuth2Authentication> oAuth2Authentication
                = Optional.ofNullable(tokenRepository.readAuthentication(convert(token)).blockingGet());

        if (oAuth2Authentication.isPresent()) {
            return RepositoryProviderUtils.convert(oAuth2Authentication.get());
        } else {
            return null;
        }
    }

    @Override
    public OAuth2Authentication readAuthentication(String token) {
        // TODO move to async call
        Optional<io.gravitee.am.repository.oauth2.model.OAuth2Authentication> oAuth2Authentication
                = Optional.ofNullable(tokenRepository.readAuthentication(token).blockingGet());

        if (oAuth2Authentication.isPresent()) {
            return RepositoryProviderUtils.convert(oAuth2Authentication.get());
        } else {
            return null;
        }
    }

    @Override
    public void storeAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
        io.gravitee.am.repository.oauth2.model.OAuth2AccessToken accessToken = convert(token);

        // extract authentication key
        io.gravitee.am.repository.oauth2.model.OAuth2Authentication oAuth2Authentication = RepositoryProviderUtils.convert(authentication);
        String authenticationKey = authenticationKeyGenerator.extractKey(oAuth2Authentication);

        // store date information
        accessToken.setCreatedAt(new Date());
        accessToken.setUpdatedAt(accessToken.getCreatedAt());

        // TODO move to async call
        tokenRepository.storeAccessToken(accessToken, oAuth2Authentication, authenticationKey).subscribe();
    }

    @Override
    public OAuth2AccessToken readAccessToken(String tokenValue) {
        // TODO move to async call
        Optional<io.gravitee.am.repository.oauth2.model.OAuth2AccessToken> oAuth2AccessToken = Optional.ofNullable(tokenRepository.readAccessToken(tokenValue).blockingGet());

        if (oAuth2AccessToken.isPresent()) {
            return convert(oAuth2AccessToken.get());
        } else {
            return null;
        }
    }

    @Override
    public void removeAccessToken(OAuth2AccessToken token) {
        // TODO move to async call
        tokenRepository.removeAccessToken(convert(token)).subscribe();
    }

    @Override
    public void storeRefreshToken(OAuth2RefreshToken _refreshToken, OAuth2Authentication authentication) {
        io.gravitee.am.repository.oauth2.model.OAuth2RefreshToken refreshToken = convert(_refreshToken);

        // store date information
        refreshToken.setCreatedAt(new Date());
        refreshToken.setUpdatedAt(refreshToken.getCreatedAt());

        // TODO move to async call
        tokenRepository.storeRefreshToken(refreshToken, RepositoryProviderUtils.convert(authentication)).subscribe();
    }

    @Override
    public OAuth2RefreshToken readRefreshToken(String tokenValue) {
        // TODO move to async call
        Optional<io.gravitee.am.repository.oauth2.model.OAuth2RefreshToken> oAuth2RefreshToken
                = Optional.ofNullable(tokenRepository.readRefreshToken(tokenValue).blockingGet());

        if(oAuth2RefreshToken.isPresent()) {
            return convert(oAuth2RefreshToken.get());
        } else {
            return null;
        }
    }

    @Override
    public OAuth2Authentication readAuthenticationForRefreshToken(OAuth2RefreshToken token) {
        // TODO move to async call
        Optional<io.gravitee.am.repository.oauth2.model.OAuth2Authentication> oAuth2Authentication
                = Optional.ofNullable(tokenRepository.readAuthenticationForRefreshToken(convert(token)).blockingGet());

        if (oAuth2Authentication.isPresent()) {
            return RepositoryProviderUtils.convert(oAuth2Authentication.get());
        } else {
            return null;
        }
    }

    @Override
    public void removeRefreshToken(OAuth2RefreshToken token) {
        // TODO move to async call
        tokenRepository.removeRefreshToken(convert(token)).subscribe();
    }

    @Override
    public void removeAccessTokenUsingRefreshToken(OAuth2RefreshToken refreshToken) {
        tokenRepository.removeAccessTokenUsingRefreshToken(convert(refreshToken));
    }

    @Override
    public OAuth2AccessToken getAccessToken(OAuth2Authentication authentication) {
        // extract authentication key
        io.gravitee.am.repository.oauth2.model.OAuth2Authentication oAuth2Authentication = RepositoryProviderUtils.convert(authentication);
        String authenticationKey = authenticationKeyGenerator.extractKey(oAuth2Authentication);

        // get access token
        // TODO move to async call
        Optional<io.gravitee.am.repository.oauth2.model.OAuth2AccessToken> oAuth2AccessToken = Optional.ofNullable(tokenRepository.getAccessToken(authenticationKey).blockingGet());

        if (oAuth2AccessToken.isPresent()) {
            io.gravitee.am.repository.oauth2.model.OAuth2AccessToken accessToken = oAuth2AccessToken.get();
            Optional<io.gravitee.am.repository.oauth2.model.OAuth2Authentication> optExtractedAuthentication = Optional.ofNullable(tokenRepository.readAuthentication(accessToken.getValue()).blockingGet());
            if ((!optExtractedAuthentication.isPresent() || !authenticationKey.equals(authenticationKeyGenerator.extractKey(optExtractedAuthentication.get())))) {
                // TODO move to async call
                tokenRepository.removeAccessToken(accessToken.getValue()).subscribe();
                // Keep the store consistent (maybe the same user is represented by this authentication but the details have
                // changed)
                // TODO move to async call
                tokenRepository.storeAccessToken(accessToken, oAuth2Authentication, authenticationKey).subscribe();

                // something happens with authentication (different serialization object)
                // Keep the refresh token consistent
                if (!optExtractedAuthentication.isPresent() && accessToken.getRefreshToken() != null) {
                    // TODO move to async call
                    tokenRepository.storeRefreshToken(accessToken.getRefreshToken(), oAuth2Authentication).subscribe();
                }
            }
            return convert(accessToken);
        } else {
            return null;
        }
    }

    @Override
    public Collection<OAuth2AccessToken> findTokensByClientIdAndUserName(String clientId, String userName) {
        // TODO move to async call
        Collection<io.gravitee.am.repository.oauth2.model.OAuth2AccessToken> accessTokens =
                tokenRepository.findTokensByClientIdAndUserName(clientId, userName).blockingGet();

        if (accessTokens != null) {
            return accessTokens.stream().map(this::convert).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public Collection<OAuth2AccessToken> findTokensByClientId(String clientId) {
        // TODO move to async call
        Collection<io.gravitee.am.repository.oauth2.model.OAuth2AccessToken> accessTokens =
                tokenRepository.findTokensByClientId(clientId).blockingGet();

        if (accessTokens != null) {
            return accessTokens.stream().map(this::convert).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private io.gravitee.am.repository.oauth2.model.OAuth2AccessToken convert(OAuth2AccessToken _oAuth2AccessToken) {
        io.gravitee.am.repository.oauth2.model.OAuth2AccessToken oAuth2AccessToken =
                new io.gravitee.am.repository.oauth2.model.OAuth2AccessToken(_oAuth2AccessToken.getValue());
        oAuth2AccessToken.setAdditionalInformation(_oAuth2AccessToken.getAdditionalInformation());
        oAuth2AccessToken.setExpiration(_oAuth2AccessToken.getExpiration());
        oAuth2AccessToken.setScope(_oAuth2AccessToken.getScope());
        oAuth2AccessToken.setTokenType(_oAuth2AccessToken.getTokenType());

        // refresh token
        OAuth2RefreshToken _oAuth2RefreshToken = _oAuth2AccessToken.getRefreshToken();
        if (_oAuth2RefreshToken != null) {
            io.gravitee.am.repository.oauth2.model.OAuth2RefreshToken oAuth2RefreshToken =
                    new io.gravitee.am.repository.oauth2.model.OAuth2RefreshToken(_oAuth2AccessToken.getRefreshToken().getValue());
            if (_oAuth2RefreshToken instanceof DefaultExpiringOAuth2RefreshToken) {
                Date expiration = ((DefaultExpiringOAuth2RefreshToken) _oAuth2RefreshToken).getExpiration();
                oAuth2RefreshToken.setExpiration(expiration);
            }
            oAuth2AccessToken.setRefreshToken(oAuth2RefreshToken);
        }

        return oAuth2AccessToken;
    }

    private OAuth2AccessToken convert(io.gravitee.am.repository.oauth2.model.OAuth2AccessToken _oAuth2AccessToken) {
        DefaultOAuth2AccessToken oAuth2AccessToken = new DefaultOAuth2AccessToken(_oAuth2AccessToken.getValue());
        oAuth2AccessToken.setAdditionalInformation(_oAuth2AccessToken.getAdditionalInformation());
        oAuth2AccessToken.setExpiration(_oAuth2AccessToken.getExpiration());
        oAuth2AccessToken.setScope(_oAuth2AccessToken.getScope());
        oAuth2AccessToken.setTokenType(_oAuth2AccessToken.getTokenType());

        // refresh token
        io.gravitee.am.repository.oauth2.model.OAuth2RefreshToken _oAuth2RefreshToken = _oAuth2AccessToken.getRefreshToken();
        if (_oAuth2RefreshToken != null) {
            DefaultExpiringOAuth2RefreshToken oAuth2RefreshToken =
                    new DefaultExpiringOAuth2RefreshToken(_oAuth2AccessToken.getRefreshToken().getValue(), _oAuth2AccessToken.getRefreshToken().getExpiration());
            oAuth2AccessToken.setRefreshToken(oAuth2RefreshToken);
        }

        return oAuth2AccessToken;
    }

    private io.gravitee.am.repository.oauth2.model.OAuth2RefreshToken convert(OAuth2RefreshToken _oAuth2RefreshToken) {
        io.gravitee.am.repository.oauth2.model.OAuth2RefreshToken oAuth2RefreshToken = new io.gravitee.am.repository.oauth2.model.OAuth2RefreshToken(_oAuth2RefreshToken.getValue());
        if (_oAuth2RefreshToken instanceof DefaultExpiringOAuth2RefreshToken) {
            oAuth2RefreshToken.setExpiration(((DefaultExpiringOAuth2RefreshToken) _oAuth2RefreshToken).getExpiration());
        }
        return oAuth2RefreshToken;
    }

    private OAuth2RefreshToken convert(io.gravitee.am.repository.oauth2.model.OAuth2RefreshToken _oAuth2RefreshToken) {
        return new DefaultExpiringOAuth2RefreshToken(_oAuth2RefreshToken.getValue(), _oAuth2RefreshToken.getExpiration());
    }
}
