package org.wordpress.android.fluxc.store;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.Authenticator;
import org.wordpress.android.fluxc.persistence.AccountMapper;
import org.wordpress.android.fluxc.persistence.AccountStorePersistence;
import org.wordpress.android.fluxc.persistence.WPDatabaseTestRule;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateErrorPayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;

import java.lang.reflect.Method;

import androidx.test.core.app.ApplicationProvider;

@RunWith(RobolectricTestRunner.class)
public class AccountStoreTest {
    @Rule
    public WPDatabaseTestRule wpDatabaseRule = new WPDatabaseTestRule(ApplicationProvider.getApplicationContext());

    private AccountStorePersistence mAccountStorePersistence;

    @Before
    public void setUp() {
        mAccountStorePersistence = new AccountStorePersistence(
                wpDatabaseRule.getDb(),
                new AccountMapper()
        );
    }

    @Test
    public void testLoadAccount() {
        AccountModel testAccount = new AccountModel();
        testAccount.setPrimarySiteId(100);
        testAccount.setAboutMe("testAboutMe");
        mAccountStorePersistence.insertOrUpdateDefaultAccount(testAccount);
        AccountStore testStore = new AccountStore(new Dispatcher(), getMockRestClient(),
                getMockSelfHostedEndpointFinder(), getMockAuthenticator(), getMockAccessToken(true),
                mAccountStorePersistence);
        AccountModel loaded = testStore.getAccount();
        assertThat(loaded.getPrimarySiteId()).isEqualTo(testAccount.getPrimarySiteId());
        assertThat(loaded.getAboutMe()).isEqualTo(testAccount.getAboutMe());
    }

    @Test
    public void testHasAccessToken() {
        AccountStore testStore = new AccountStore(new Dispatcher(), getMockRestClient(),
                getMockSelfHostedEndpointFinder(), getMockAuthenticator(), getMockAccessToken(true),
                mAccountStorePersistence);
        assertThat(testStore.hasAccessToken()).isTrue();
        testStore = new AccountStore(new Dispatcher(), getMockRestClient(), getMockSelfHostedEndpointFinder(),
                getMockAuthenticator(), getMockAccessToken(false), mAccountStorePersistence);
        assertThat(testStore.hasAccessToken()).isFalse();
    }

    @Test
    public void testIsSignedIn() {
        AccountModel testAccount = new AccountModel();
        testAccount.setVisibleSiteCount(0);
        mAccountStorePersistence.insertOrUpdateDefaultAccount(testAccount);
        AccountStore testStore = new AccountStore(new Dispatcher(), getMockRestClient(),
                getMockSelfHostedEndpointFinder(), getMockAuthenticator(), getMockAccessToken(false),
                mAccountStorePersistence);
        assertThat(testStore.hasAccessToken()).isFalse();
        testAccount.setVisibleSiteCount(1);
        mAccountStorePersistence.insertOrUpdateDefaultAccount(testAccount);
        testStore = new AccountStore(new Dispatcher(), getMockRestClient(), getMockSelfHostedEndpointFinder(),
                getMockAuthenticator(), getMockAccessToken(true), mAccountStorePersistence);
        assertThat(testStore.hasAccessToken()).isTrue();
    }

    @Test
    public void testSignOut() throws Exception {
        AccountModel testAccount = new AccountModel();
        AccessToken testToken = new AccessToken(ApplicationProvider.getApplicationContext());
        testToken.set("TESTTOKEN");
        testAccount.setUserId(24);
        mAccountStorePersistence.insertOrUpdateDefaultAccount(testAccount);
        AccountStore testStore = new AccountStore(new Dispatcher(), getMockRestClient(),
                getMockSelfHostedEndpointFinder(), getMockAuthenticator(), testToken,
                mAccountStorePersistence);
        assertThat(testStore.hasAccessToken()).isTrue();
        // Sign out is private (and it should remain private)
        Method privateMethod = AccountStore.class.getDeclaredMethod("signOut");
        privateMethod.setAccessible(true);
        privateMethod.invoke(testStore);
        assertThat(testStore.hasAccessToken()).isFalse();
        assertThat(mAccountStorePersistence.getDefaultAccount()).isNull();
    }

    @Test
    public void testPayloadIsError() throws Exception {
        // AuthenticateErrorPayload masks the error field of its superclass (Payload)
        AuthenticateErrorPayload payload1 = new AuthenticateErrorPayload(AuthenticationErrorType.GENERIC_ERROR);
        assertThat(payload1.isError()).isTrue();
        payload1.error = null;
        assertThat(payload1.isError()).isFalse();

        AuthenticatePayload payload2 = new AuthenticatePayload("", "");
        assertThat(payload2.isError()).isFalse();
        payload2.error = new BaseNetworkError(GenericErrorType.NETWORK_ERROR);
        assertThat(payload2.isError()).isTrue();
    }

    private AccountRestClient getMockRestClient() {
        return Mockito.mock(AccountRestClient.class);
    }

    private Authenticator getMockAuthenticator() {
        return Mockito.mock(Authenticator.class);
    }

    private AccessToken getMockAccessToken(boolean exists) {
        AccessToken mock = Mockito.mock(AccessToken.class);
        Mockito.when(mock.exists()).thenReturn(exists);
        return mock;
    }

    private SelfHostedEndpointFinder getMockSelfHostedEndpointFinder() {
        return Mockito.mock(SelfHostedEndpointFinder.class);
    }
}
