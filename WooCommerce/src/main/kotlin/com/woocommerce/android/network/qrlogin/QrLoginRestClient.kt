package com.woocommerce.android.network.qrlogin

import com.woocommerce.android.ui.login.qrlogin.Secret

data class QrLoginCredentials(
    val userLogin: String,
    val applicationPassword: Secret,
    val uuid: String?
)
