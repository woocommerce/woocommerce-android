package com.woocommerce.android.ui.login.wpcom

import android.os.Parcelable
import com.woocommerce.android.model.JetpackStatus
import kotlinx.parcelize.Parcelize

sealed interface WPComLoginMode : Parcelable {
    @Parcelize
    data class JetpackSetup(val jetpackStatus: JetpackStatus) : WPComLoginMode
}
