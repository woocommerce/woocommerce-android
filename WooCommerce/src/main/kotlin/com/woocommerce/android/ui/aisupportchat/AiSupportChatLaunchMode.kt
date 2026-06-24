package com.woocommerce.android.ui.aisupportchat

import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckCardData

sealed interface AiSupportChatLaunchMode {
    val siteAddress: String?

    data class Help(
        override val siteAddress: String? = null
    ) : AiSupportChatLaunchMode

    data class PreLogin(
        override val siteAddress: String? = null
    ) : AiSupportChatLaunchMode

    data class ConnectivityTool(
        val checks: List<ConnectivityCheckCardData>,
        override val siteAddress: String? = null
    ) : AiSupportChatLaunchMode

    data class StoreConnectionError(
        override val siteAddress: String? = null
    ) : AiSupportChatLaunchMode

    data class Resume(
        val chatId: Long,
        val botSlug: String,
        val sessionId: String?,
        val hasCreatedTicket: Boolean = false,
        val isResolved: Boolean = false,
        override val siteAddress: String? = null
    ) : AiSupportChatLaunchMode
}
