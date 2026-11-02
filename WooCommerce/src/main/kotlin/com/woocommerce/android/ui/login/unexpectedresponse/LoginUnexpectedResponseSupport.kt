package com.woocommerce.android.ui.login.unexpectedresponse

import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import javax.inject.Inject

class LoginUnexpectedResponseSupport @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository
) {
    fun contactSupportEvent(
        failure: LoginUnexpectedResponseFailure,
        siteAddress: String?,
        origin: HelpOrigin
    ): MultiLiveEvent.Event = if (featureFlagRepository.isEnabled(FeatureFlag.AI_SUPPORT_CHAT)) {
        OpenAiSupportChat(failure, siteAddress)
    } else {
        OpenSupportForm(failure, siteAddress, origin)
    }

    data class OpenAiSupportChat(
        val failure: LoginUnexpectedResponseFailure,
        val siteAddress: String?
    ) : MultiLiveEvent.Event()

    data class OpenSupportForm(
        val failure: LoginUnexpectedResponseFailure,
        val siteAddress: String?,
        val origin: HelpOrigin
    ) : MultiLiveEvent.Event()
}
