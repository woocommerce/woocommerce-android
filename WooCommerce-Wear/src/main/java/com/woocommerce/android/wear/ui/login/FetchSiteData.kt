package com.woocommerce.android.wear.ui.login

import com.woocommerce.android.wear.extensions.combineWithTimeout
import com.woocommerce.android.wear.phone.PhoneConnectionRepository
import com.woocommerce.android.wear.ui.login.LoginViewModel.LoginState
import com.woocommerce.android.wear.ui.login.LoginViewModel.LoginState.Logged
import com.woocommerce.android.wear.ui.login.LoginViewModel.LoginState.Timeout
import com.woocommerce.android.wear.ui.login.LoginViewModel.LoginState.Waiting
import com.woocommerce.commons.MessagePath.REQUEST_APP_SETTINGS
import com.woocommerce.commons.MessagePath.REQUEST_SITE
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchSiteData @Inject constructor(
    private val phoneRepository: PhoneConnectionRepository,
    private val loginRepository: LoginRepository
) {
    suspend operator fun invoke(): Flow<LoginState> {
        if (phoneRepository.isPhoneConnectionAvailable()) {
            phoneRepository.sendMessage(REQUEST_APP_SETTINGS)
            phoneRepository.sendMessage(REQUEST_SITE)
        }

        return loginRepository.isSiteAvailable
            .combineWithTimeout { isSiteAvailable, isTimeout ->
                when {
                    isSiteAvailable -> Logged
                    isTimeout.not() -> Waiting
                    else -> Timeout
                }
            }
    }
}
