package com.woocommerce.android.ui.login.qrlogin.flow

import com.woocommerce.android.network.qrlogin.QrLoginRestClient
import com.woocommerce.android.ui.login.qrlogin.QrLoginAuthenticator
import com.woocommerce.android.ui.login.qrlogin.QrLoginPayload
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Picks the right [QrLoginFlow] for a parsed QR payload.
 *
 * Returns `null` for payloads that don't drive a scan/poll/exchange protocol — the ViewModel
 * routes those (site-URL prefill, legacy app-login, wp.com magic-link) directly via its own
 * dispatch events.
 *
 * Each call returns a fresh flow instance because QR tokens are single-use; reusing a flow across
 * scans would mean reusing a consumed token.
 */
@Singleton
class QrLoginFlowFactory @Inject constructor(
    private val restClient: QrLoginRestClient,
    private val authenticator: QrLoginAuthenticator,
) {
    fun create(payload: QrLoginPayload, scope: CoroutineScope): QrLoginFlow? = when (payload) {
        is QrLoginPayload.Ticket -> SiteQrLoginFlow(
            ticket = payload,
            scope = scope,
            restClient = restClient,
            authenticator = authenticator,
        )
        is QrLoginPayload.SiteUrl,
        is QrLoginPayload.WpComMagicLinkUrl,
        is QrLoginPayload.AppLogin.Credentials,
        is QrLoginPayload.AppLogin.WpComEmail,
        QrLoginPayload.InstallQrCode,
        QrLoginPayload.Invalid -> null
    }
}
