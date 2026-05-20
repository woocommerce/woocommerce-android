package com.woocommerce.android.ui.aisupportchat

import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckCardData

sealed interface AiSupportChatLaunchMode {
    data object Help : AiSupportChatLaunchMode

    data object PreLogin : AiSupportChatLaunchMode

    data class ConnectivityTool(
        val checks: List<ConnectivityCheckCardData>
    ) : AiSupportChatLaunchMode

    data class Resume(
        val chatId: Long,
        val botSlug: String,
        val sessionId: String?,
        val hasCreatedTicket: Boolean = false,
        val isResolved: Boolean = false
    ) : AiSupportChatLaunchMode
}
