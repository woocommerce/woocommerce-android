package com.woocommerce.android.ui.login.unexpectedresponse

import androidx.fragment.app.Fragment
import com.woocommerce.android.support.requests.SupportRequestFormActivity
import com.woocommerce.android.ui.aisupportchat.AiSupportChatActivity

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
