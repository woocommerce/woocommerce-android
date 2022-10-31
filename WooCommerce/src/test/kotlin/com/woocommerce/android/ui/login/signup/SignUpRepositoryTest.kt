package com.woocommerce.android.ui.login.signup

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.util.dispatchAndAwait
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload
import org.wordpress.android.fluxc.store.signup.SignUpStore
import org.wordpress.android.fluxc.store.signup.SignUpStore.CreateWpAccountResult

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpRepositoryTest : BaseUnitTest() {
    private companion object {
        const val ANY_VALID_USER_EMAIL = "email@gmail.com"
        const val USER_EMAIL_CONTAINING_WORDPRESS = "emailWithWordPress@gmail.com"
        const val ANY_PASSWORD = "anypassword"
        const val DEFAULT_USERNAME_SUGGESTION = "usernameOne"
        const val ANY_AUTH_TOKEN = "any_auth_token"
        val USER_NAME_SUGGESTIONS_LIST = listOf(
            DEFAULT_USERNAME_SUGGESTION,
            "usernameTwo",
            "usernameThree"
        )
        val DEFAULT_SUCCESSFUL_ACCOUNT_CREATION_RESULT = CreateWpAccountResult(success = true, ANY_AUTH_TOKEN)
        val DEFAULT_ON_ACCOUNT_CHANGED_EVENT = OnAccountChanged()
        val DEFAULT_ON_AUTH_TOKEN_CHANGED_EVENT = OnAuthenticationChanged()
    }

    private val signUpStore: SignUpStore = mock()
    private val dispatcher: Dispatcher = mock()
    private val prefsWrapper: AppPrefsWrapper = mock()
    private val signUpCredentialsValidator: SignUpCredentialsValidator = mock()

    private val sut: SignUpRepository = SignUpRepository(
        signUpStore,
        dispatcher,
        prefsWrapper,
        signUpCredentialsValidator
    )

    @Before
    fun setUp() {
        testBlocking {
            givenDispatcherReturnsForTokenUpdates(DEFAULT_ON_AUTH_TOKEN_CHANGED_EVENT)
            givenDispatcherReturnsForAccountChanges(DEFAULT_ON_ACCOUNT_CHANGED_EVENT)
        }
    }

    @Test
    fun `given valid email, when creating account , then fetch username suggestions`() =
        testBlocking {
            givenValidateCredentialsReturns(null)
            givenAccountCreationResult(DEFAULT_SUCCESSFUL_ACCOUNT_CREATION_RESULT)
            givenFetchUserNameSuggestionsReturns(
                SignUpStore.UsernameSuggestionsResult(
                    USER_NAME_SUGGESTIONS_LIST
                )
            )

            sut.createAccount(ANY_VALID_USER_EMAIL, ANY_PASSWORD)

            verify(signUpStore).fetchUserNameSuggestions(ANY_VALID_USER_EMAIL)
            verify(signUpStore).createWpAccount(ANY_VALID_USER_EMAIL, ANY_PASSWORD, DEFAULT_USERNAME_SUGGESTION)
        }

//    @Test
//    fun `given username error, when fetching username suggestions , then use email address to create account `() =
//        testBlocking {
//
//        }
//
//    @Test
//    fun `given an email with wordpress wording in it, when create new account , then strip wordpress from email`() =
//        testBlocking {
//
//        }

    private suspend fun givenFetchUserNameSuggestionsReturns(result: SignUpStore.UsernameSuggestionsResult) {
        whenever(signUpStore.fetchUserNameSuggestions(anyString())).thenReturn(result)
    }

    private fun givenValidateCredentialsReturns(error: SignUpRepository.SignUpError?) {
        whenever(signUpCredentialsValidator.validateCredentials(anyString(), anyString())).thenReturn(error)
    }

    private suspend fun givenAccountCreationResult(result: CreateWpAccountResult) {
        whenever(signUpStore.createWpAccount(anyString(), anyString(), anyString())).thenReturn(result)
    }

    private suspend fun givenDispatcherReturnsForTokenUpdates(result: OnAuthenticationChanged) {
        whenever(dispatcher.dispatchAndAwait<UpdateTokenPayload, OnAuthenticationChanged>(any()))
            .thenReturn(result)
    }

    private suspend fun givenDispatcherReturnsForAccountChanges(result: OnAccountChanged) {
        whenever(dispatcher.dispatchAndAwait<Void, OnAccountChanged>(any()))
            .thenReturn(result)
    }
}
