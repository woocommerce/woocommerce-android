package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class JetpackStatus(
    val isJetpackInstalled: Boolean,
    val isJetpackConnected: Boolean,
    val wpComEmail: String?
) : Parcelable
