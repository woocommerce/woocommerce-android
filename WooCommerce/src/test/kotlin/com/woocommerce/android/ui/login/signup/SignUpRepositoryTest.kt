package com.woocommerce.android.ui.login.signup

import com.woocommerce.android.FakeDispatcher
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.signup.SignUpStore
import org.wordpress.android.fluxc.store.signup.SignUpStore.CreateWpAccountResult

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpRepositoryTest : BaseUnitTest() {
    private companion object {
        const val EMAIL_WITHOUT_DOMAIN = "email"
        const val VALID_USER_EMAIL = "email@gmail.com"
        const val INVALID_USER_EMAIL = "email@gm"
        const val USER_EMAIL_CONTAINING_WORDPRESS = "emailWordPress@gmail.com"
        const val ANY_VALID_PASSWORD = "anypassword"
        const val DEFAULT_USERNAME_SUGGESTION = "usernameOne"
        const val ANY_AUTH_TOKEN = "any_auth_token"
        val USER_NAME_SUGGESTIONS_LIST = listOf(
            DEFAULT_USERNAME_SUGGESTION,
            "usernameTwo",
            "usernameThree"
        )
        val SUCCESSFUL_ACCOUNT_CREATION_RESULT = CreateWpAccountResult(success = true, ANY_AUTH_TOKEN)
    }

    private val signUpStore: SignUpStore = mock()
    private val dispatcher: Dispatcher = FakeDispatcher()
    private val signUpCredentialsValidator: SignUpCredentialsValidator = mock()

    private val sut: SignUpRepository = SignUpRepository(
        signUpStore,
        dispatcher,
        signUpCredentialsValidator
    )

    @Test
    fun `given valid credentials, when creating account, then fetch username suggestions`() =
        testBlocking {
            givenValidateCredentialsReturns(null)
            givenAccountCreationResult(SUCCESSFUL_ACCOUNT_CREATION_RESULT)
            givenFetchUserNameSuggestionsReturns(
                SignUpStore.UsernameSuggestionsResult(
                    USER_NAME_SUGGESTIONS_LIST
                )
            )

            val task = async { sut.createAccount(VALID_USER_EMAIL, ANY_VALID_PASSWORD) }
            // Handle token update
            dispatcher.emitChange(OnAuthenticationChanged())
            // Handle account fetching
            dispatcher.emitChange(OnAccountChanged())

            task.await()

            verify(signUpStore).fetchUserNameSuggestions(VALID_USER_EMAIL)
            verify(signUpStore).createWpAccount(VALID_USER_EMAIL, ANY_VALID_PASSWORD, DEFAULT_USERNAME_SUGGESTION)
        }

    @Test
    fun `given fetching username suggestion fails, when creating account, then use email without domain as username`() =
        testBlocking {
            givenValidateCredentialsReturns(null)
            givenAccountCreationResult(SUCCESSFUL_ACCOUNT_CREATION_RESULT)
            givenFetchUserNameSuggestionsReturns(
                SignUpStore.UsernameSuggestionsResult(SignUpStore.UsernameSuggestionsError("error"))
            )

            val task = async { sut.createAccount(VALID_USER_EMAIL, ANY_VALID_PASSWORD) }
            // Handle token update
            dispatcher.emitChange(OnAuthenticationChanged())
            // Handle account fetching
            dispatcher.emitChange(OnAccountChanged())
            task.await()

            verify(signUpStore).createWpAccount(VALID_USER_EMAIL, ANY_VALID_PASSWORD, EMAIL_WITHOUT_DOMAIN)
        }

    @Test
    fun `given an email with wordpress wording in it, when create account, then fetch usernames without wordpress`() =
        testBlocking {
            givenValidateCredentialsReturns(null)
            givenAccountCreationResult(SUCCESSFUL_ACCOUNT_CREATION_RESULT)
            givenFetchUserNameSuggestionsReturns(
                SignUpStore.UsernameSuggestionsResult(
                    USER_NAME_SUGGESTIONS_LIST
                )
            )

            val task = async { sut.createAccount(USER_EMAIL_CONTAINING_WORDPRESS, ANY_VALID_PASSWORD) }
            // Handle token update
            dispatcher.emitChange(OnAuthenticationChanged())
            // Handle account fetching
            dispatcher.emitChange(OnAccountChanged())
            task.await()

            verify(signUpStore).fetchUserNameSuggestions(VALID_USER_EMAIL)
            verify(signUpStore).createWpAccount(
                USER_EMAIL_CONTAINING_WORDPRESS,
                ANY_VALID_PASSWORD,
                DEFAULT_USERNAME_SUGGESTION
            )
        }

    @Test
    fun `given credentials validation error, when create new account , then no api request is fired`() =
        testBlocking {
            givenValidateCredentialsReturns(SignUpError.EMAIL_INVALID)

            val task = async { sut.createAccount(INVALID_USER_EMAIL, ANY_VALID_PASSWORD) }
            // Handle token update
            dispatcher.emitChange(OnAuthenticationChanged())
            // Handle account fetching
            dispatcher.emitChange(OnAccountChanged())
            task.await()

            verify(signUpStore, never()).fetchUserNameSuggestions(anyString())
            verify(signUpStore, never()).createWpAccount(anyString(), anyString(), anyString())
        }

    private suspend fun givenFetchUserNameSuggestionsReturns(result: SignUpStore.UsernameSuggestionsResult) {
        whenever(signUpStore.fetchUserNameSuggestions(anyString())).thenReturn(result)
    }

    private fun givenValidateCredentialsReturns(error: SignUpError?) {
        whenever(signUpCredentialsValidator.validateCredentials(anyString(), anyString())).thenReturn(error)
    }

    private suspend fun givenAccountCreationResult(result: CreateWpAccountResult) {
        whenever(signUpStore.createWpAccount(anyString(), anyString(), anyString())).thenReturn(result)
    }
}
