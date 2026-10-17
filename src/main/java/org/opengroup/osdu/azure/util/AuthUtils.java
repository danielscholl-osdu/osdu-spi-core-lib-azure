// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.azure.util;

import com.azure.security.keyvault.secrets.SecretClient;
import com.nimbusds.jwt.JWTClaimsSet;
import org.opengroup.osdu.azure.KeyVaultFacade;
import org.opengroup.osdu.azure.di.AzureActiveDirectoryConfiguration;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Base64;

/**
 * Utils for auth.
 */
@Component
public final class AuthUtils {

    private static final String SECRET_NAME = "app-dev-sp-password";
    private static final String LOGIN_MICROSOFT_ONLINE_ISSUER = "https://login.microsoftonline.com/";
    private static final String STS_WINDOWS_ISSUER = "https://sts.windows.net/";
    private static final String STS_CHINA_CLOUD_API_ISSUER = "https://sts.chinacloudapi.cn/";

    /**
     * Get claims set from JWT token.
     *
     * @param jwtToken the JWT token string
     * @return the JWTClaimsSet object
     */
    public JWTClaimsSet getClaimsFromJwtToken(final String jwtToken) {
        try {
            String[] jwtTokenParts = jwtToken.split("\\.");
            Base64.Decoder decoder = Base64.getDecoder();
            String infoString = new String(decoder.decode(jwtTokenParts[1]));
            return JWTClaimsSet.parse(infoString);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get OID from JWT token.
     *
     * @param jwtToken the JWT token string
     * @return the OID
     */
    public String getOidFromJwtToken(final String jwtToken) {
        try {
            JWTClaimsSet claimsSet = getClaimsFromJwtToken(jwtToken);
            return claimsSet == null ? null : claimsSet.getStringClaim("oid");
        } catch (ParseException e) {
            return null;
        }
    }


    /**
     * Get client secret.
     *
     * @param aadConfiguration Azure Active Directory configuration bean.
     * @param sc               KeyVault client
     * @return client secrete
     */
    public static String getClientSecret(final AzureActiveDirectoryConfiguration aadConfiguration, final SecretClient sc) {
        if (aadConfiguration.getClientSecret() != null && !aadConfiguration.getClientSecret().isEmpty()) {
            return aadConfiguration.getClientSecret();
        }
        return KeyVaultFacade.getSecretWithValidation(sc, SECRET_NAME);
    }

    /**
     * Verifies if the token is issued by AAD.
     *
     * @param token token that needs to be validated
     * @return boolean
     */
    public Boolean isAadToken(final String token) {
        JWTClaimsSet claimsSet = getClaimsFromJwtToken(token);
        return (claimsSet != null && isAadIssuer(claimsSet.getIssuer()));
    }

    /***
     * Checks if issuer is Azure.
     * @param issuer Token Issuer
     * @return boolean
     */
    private static boolean isAadIssuer(final String issuer) {
        return (issuer != null
                && (issuer.startsWith(LOGIN_MICROSOFT_ONLINE_ISSUER)
                    || issuer.startsWith(STS_WINDOWS_ISSUER)
                    || issuer.startsWith(STS_CHINA_CLOUD_API_ISSUER))
        );
    }
}
