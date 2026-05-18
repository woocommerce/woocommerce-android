package com.woocommerce.android.ui.aisupportchat

import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckCardData

sealed interface AiSupportChatLaunchMode {
    data object Help : AiSupportChatLaunchMode

    data object PreLogin : AiSupportChatLaunchMode

    data class ConnectivityTool(
        val checks: List<ConnectivityCheckCardData>
    ) : AiSupportChatLaunchMode
}
