package com.woocommerce.android.ui.login

import com.woocommerce.android.extensions.combineWithTimeout
import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.ui.login.FetchSiteData.LoginRequestState.Logged
import com.woocommerce.android.ui.login.FetchSiteData.LoginRequestState.Timeout
import com.woocommerce.android.ui.login.FetchSiteData.LoginRequestState.Waiting
import com.woocommerce.commons.wear.MessagePath.REQUEST_SITE
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
