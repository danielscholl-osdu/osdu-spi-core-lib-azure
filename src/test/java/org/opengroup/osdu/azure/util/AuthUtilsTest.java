package org.opengroup.osdu.azure.util;

import com.azure.security.keyvault.secrets.SecretClient;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.KeyVaultFacade;
import org.opengroup.osdu.azure.di.AzureActiveDirectoryConfiguration;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthUtilsTest {

    // Mock JWT Token: {"aud": "audience","appid": "application-id","oid": "my-oid","sub": "my-sub","tid": "my-tenant"}
    private static final String JWT_TOKEN = "part1.eyJhdWQiOiJhdWRpZW5jZSIsImFwcGlkIjoiYXBwbGljYXRpb24taWQiLCJvaWQiOiJteS1vaWQiLCJzdWIiOiJteS1zdWIiLCJ0aWQiOiJteS10ZW5hbnQifQ.part3";

    // Mock JWT Token: {"sub": "1234567890",
    //  "name": "John Doe",
    //  "iat": 1516239022,
    //  "iss": "https://login.microsoftonline.com/"}
    private static final String JWT_TOKEN_WITH_AAD_ISSUER = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vIn0.uHJ3CKjG1QrRp3hA1Jn5m_dOV6cema5SorFvq94G1zE";

    // Mock Jwt Token:
    //    {
    //        "sub": "1234567890",
    //            "name": "John Doe",
    //            "iat": 1516239022,
    //            "iss": "https://something.com/"
    //    }
    private static final String JWT_TOKEN_WITH_NON_AAD_ISSUER = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJpc3MiOiJodHRwczovL3NvbWV0aGluZy5jb20vIn0.vjjX6rz4Y2ojoWwBhxcCHbOeJuMLJ3T6J5dAh2vM-kg";

    private static final String INVALID_JWT_TOKEN = "invalidjwttoken";
    private static final String INVALID_JWT_TOKEN_CLAIMS = "part1.invalidclaims.part2";
    private static final String EMPTY_JWT_TOKEN_CLAIMS = "part1.e30.part3";
    private static final String TOKEN_AUDIENCE = "audience";
    private static final String TOKEN_SUBJECT = "my-sub";
    private static final String TOKEN_OID = "my-oid";
    private static final String TOKEN_TENANT = "my-tenant";
    private static final String TOKEN_APPID = "application-id";

    private static final String SECRET_NAME = "app-dev-sp-password";

    @InjectMocks
    private AuthUtils authUtils;

    @Mock
    private AzureActiveDirectoryConfiguration aadConfiguration;

    @Mock
    private SecretClient secretClient;

    @Test
    public void ShouldSuccessfullyGetClaims() throws Exception {
        JWTClaimsSet claims = authUtils.getClaimsFromJwtToken(JWT_TOKEN);

        assertEquals(5, claims.getClaims().size());
        assertEquals(1, claims.getAudience().size());
        assertEquals(TOKEN_AUDIENCE, claims.getAudience().get(0));
        assertEquals(TOKEN_SUBJECT, claims.getSubject());
        assertEquals(TOKEN_APPID, claims.getStringClaim("appid"));
        assertEquals(TOKEN_OID, claims.getStringClaim("oid"));
        assertEquals(TOKEN_TENANT, claims.getStringClaim("tid"));
    }

    @Test
    public void ShouldReturnNullForInvalidToken() throws Exception {
        JWTClaimsSet claims = authUtils.getClaimsFromJwtToken(INVALID_JWT_TOKEN);
        assertEquals(null, claims);
    }

    @Test
    public void ShouldReturnNullForInvalidTokenClaims() throws Exception {
        JWTClaimsSet claims = authUtils.getClaimsFromJwtToken(INVALID_JWT_TOKEN_CLAIMS);
        assertEquals(null, claims);
    }

    @Test
    public void ShouldReturnEmptySetForEmptyTokenClaims() throws Exception {
        JWTClaimsSet claims = authUtils.getClaimsFromJwtToken(EMPTY_JWT_TOKEN_CLAIMS);
        assertEquals(0, claims.getClaims().size());
    }

    @Test
    public void ShouldReturnOidForValidTokenWithOid() throws Exception {
        String oid = authUtils.getOidFromJwtToken(JWT_TOKEN);
        assertEquals(TOKEN_OID, oid);
    }

    @Test
    public void ShouldReturnNullOidForInvalidToken() throws Exception {
        String oid = authUtils.getOidFromJwtToken(INVALID_JWT_TOKEN);
        assertEquals(null, oid);
    }

    @Test
    public void ShouldReturnNullOidForEmptyToken() throws Exception {
        String oid = authUtils.getOidFromJwtToken(EMPTY_JWT_TOKEN_CLAIMS);
        assertEquals(null, oid);
    }

    @Test
    public void ShouldGetClientSecret_ifClientSecretNotNullOrEmpty() {
        String secretValue = "testValue";

        when(aadConfiguration.getClientSecret()).thenReturn(secretValue);

        assertEquals(secretValue, AuthUtils.getClientSecret(aadConfiguration, secretClient));
    }

    @Test
    public void ShouldGetClientSecret_ifClientSecretIsNull() {
        MockedStatic<KeyVaultFacade> mockedSettings = mockStatic(KeyVaultFacade.class);
        testGetClientSecretForNullOrEmptyCases(null);
        mockedSettings.close();
    }

    @Test
    public void ShouldGetClientSecret_ifClientSecretIsEmpty() {
        MockedStatic<KeyVaultFacade> mockedSettings = mockStatic(KeyVaultFacade.class);
        testGetClientSecretForNullOrEmptyCases(EMPTY);
        mockedSettings.close();
    }

    private void testGetClientSecretForNullOrEmptyCases(String aadConfigSecret) {
        String secretValue = "testValue";

        when(aadConfiguration.getClientSecret()).thenReturn(aadConfigSecret);
        when(KeyVaultFacade.getSecretWithValidation(secretClient, SECRET_NAME)).thenReturn(secretValue);

        assertEquals(secretValue, AuthUtils.getClientSecret(aadConfiguration, secretClient));
    }

    @Test
    void isAadTokenReturnsTrue_when_issuerIsAAD() {
        assertTrue(authUtils.isAadToken(JWT_TOKEN_WITH_AAD_ISSUER));
    }

    @Test
    void isAadTokenReturnsTrue_when_issuerIsStsWindows() {
        // JWT payload contains iss=https://sts.windows.net/
        String stsToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8ifQ.signature";
        assertTrue(authUtils.isAadToken(stsToken));
    }

    @Test
    void isAadTokenReturnsTrue_when_issuerIsStsChinaCloud() {
        // JWT payload contains iss=https://sts.chinacloudapi.cn/
        String chinaToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJpc3MiOiJodHRwczovL3N0cy5jaGluYWNsb3VkYXBpLmNuLyJ9.signature";
        assertTrue(authUtils.isAadToken(chinaToken));
    }

    @Test
    void isAadTokenReturnsFalse_when_issuerIsNotAAD() {
        assertFalse(authUtils.isAadToken(JWT_TOKEN_WITH_NON_AAD_ISSUER));
    }

    @Test
    void isAadTokenReturnsFalse_when_noIssuerPresent() {
        assertFalse(authUtils.isAadToken(JWT_TOKEN));
    }
}
