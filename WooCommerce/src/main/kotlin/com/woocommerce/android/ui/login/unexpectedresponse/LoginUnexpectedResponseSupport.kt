package com.woocommerce.android.ui.login.unexpectedresponse

import androidx.fragment.app.Fragment
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.requests.SupportRequestFormActivity
import com.woocommerce.android.ui.aisupportchat.AiSupportChatActivity
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

fun Fragment.openAiSupportChat(event: LoginUnexpectedResponseSupport.OpenAiSupportChat) {
    startActivity(
        AiSupportChatActivity.createLoginUnexpectedResponseIntent(
            context = requireContext(),
            failure = event.failure,
            siteAddress = event.siteAddress
        )
    )
}

fun Fragment.openSupportForm(event: LoginUnexpectedResponseSupport.OpenSupportForm) {
    startActivity(
        SupportRequestFormActivity.createIntent(
            context = requireContext(),
            origin = event.origin,
            extraTags = arrayListOf(LoginUnexpectedResponseFailure.SUPPORT_TAG),
            diagnosticLog = event.failure.supportDetails,
            prefilledSiteAddress = event.siteAddress
        )
    )
}
