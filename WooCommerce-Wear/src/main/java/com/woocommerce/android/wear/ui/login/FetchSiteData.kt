package com.woocommerce.android.wear.ui.login

import com.woocommerce.android.wear.extensions.combineWithTimeout
import com.woocommerce.android.wear.phone.PhoneConnectionRepository
import com.woocommerce.android.wear.ui.login.FetchSiteData.LoginRequestState.Logged
import com.woocommerce.android.wear.ui.login.FetchSiteData.LoginRequestState.Timeout
import com.woocommerce.android.wear.ui.login.FetchSiteData.LoginRequestState.Waiting
import com.woocommerce.commons.MessagePath.REQUEST_SITE
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchSiteData @Inject constructor(
    private val phoneRepository: PhoneConnectionRepository,
    private val loginRepository: LoginRepository
) {
    suspend operator fun invoke(): Flow<LoginRequestState> {
        if (phoneRepository.isPhoneConnectionAvailable()) {
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

    enum class LoginRequestState {
        Logged,
        Waiting,
        Timeout
    }
}
