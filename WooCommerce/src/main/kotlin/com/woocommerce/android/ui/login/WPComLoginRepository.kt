package com.woocommerce.android.ui.login

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.util.dispatchAndAwait
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore.FetchAuthOptionsPayload
import org.wordpress.android.fluxc.store.AccountStore.OnAuthOptionsFetched
import org.wordpress.android.login.AuthOptions
import javax.inject.Inject

class WPComLoginRepository @Inject constructor(
    private val dispatcher: Dispatcher
) {
    suspend fun fetchAuthOptions(emailOrUsername: String): Result<AuthOptions> {
        val action = AccountActionBuilder.newFetchAuthOptionsAction(
            FetchAuthOptionsPayload(emailOrUsername)
        )
        val event: OnAuthOptionsFetched = dispatcher.dispatchAndAwait(action)

        return when {
            event.isError -> Result.failure(OnChangedException(event.error))
            else -> Result.success(
                AuthOptions(
                    isPasswordless = event.isPasswordless,
                    isEmailVerified = event.isEmailVerified
                )
            )
        }
    }
}
