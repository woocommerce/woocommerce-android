package com.woocommerce.android.ui.login.qrlogin

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.network.qrlogin.QrLoginExchangeException
import com.woocommerce.android.network.qrlogin.QrLoginScanException
import com.woocommerce.android.network.qrlogin.QrLoginSessionStatusException
import com.woocommerce.android.network.qrlogin.WpComQrLoginExchangeException
import com.woocommerce.android.network.qrlogin.WpComQrLoginScanException
import com.woocommerce.android.network.qrlogin.WpComQrLoginSessionStatusException
import com.woocommerce.android.ui.login.WPApiSiteRepository.CookieNonceAuthenticationException
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.ErrorReason
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CancellationException
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import java.io.IOException
import javax.inject.Inject

class QrLoginErrorMapper @Inject constructor() {
    fun isRetryEligible(reason: ErrorReason): Boolean = when (reason) {
        ErrorReason.Network,
        ErrorReason.ServerError,
        ErrorReason.RateLimited -> true
        else -> false
    }

    fun toScanReason(throwable: Throwable): ErrorReason = when (throwable) {
        QrLoginScanException.TokenRejected -> ErrorReason.TokenRejected
        QrLoginScanException.AlreadyScanned -> ErrorReason.MatchAlreadyScanned
        QrLoginScanException.EndpointMissing -> ErrorReason.EndpointMissing
        QrLoginScanException.RateLimited -> ErrorReason.RateLimited
        QrLoginScanException.UpgradeRequired -> ErrorReason.EndpointMissing
        QrLoginScanException.Network -> ErrorReason.Network
        QrLoginScanException.MalformedRequest,
        QrLoginScanException.MalformedResponse -> ErrorReason.ServerError
        is QrLoginScanException.HttpError -> ErrorReason.ServerError
        is QrLoginScanException.Unknown -> ErrorReason.Unknown
        is IOException -> ErrorReason.Network
        else -> ErrorReason.Unknown
    }

    fun toPollReason(throwable: Throwable): ErrorReason = when (throwable) {
        QrLoginSessionStatusException.EndpointMissing -> ErrorReason.EndpointMissing
        QrLoginSessionStatusException.RateLimited -> ErrorReason.RateLimited
        QrLoginSessionStatusException.Network -> ErrorReason.Network
        QrLoginSessionStatusException.MalformedResponse -> ErrorReason.ServerError
        is QrLoginSessionStatusException.HttpError -> ErrorReason.ServerError
        is QrLoginSessionStatusException.Unknown -> ErrorReason.Unknown
        else -> {
            WooLog.w(WooLog.T.LOGIN, "QR login poll: unmapped failure type ${throwable.javaClass.simpleName}")
            ErrorReason.Unknown
        }
    }

    fun toExchangeReason(throwable: Throwable): ErrorReason = when (throwable) {
        QrLoginExchangeException.TokenRejected -> ErrorReason.TokenRejected
        QrLoginExchangeException.EndpointMissing -> ErrorReason.EndpointMissing
        QrLoginExchangeException.RateLimited -> ErrorReason.RateLimited
        QrLoginExchangeException.Network -> ErrorReason.Network
        QrLoginExchangeException.MalformedResponse -> ErrorReason.ServerError
        QrLoginExchangeException.NotApproved -> ErrorReason.MatchTimedOut
        QrLoginExchangeException.InvalidExchangeGrant -> ErrorReason.MatchInvalidGrant
        is QrLoginExchangeException.HttpError -> ErrorReason.ServerError
        is QrLoginExchangeException.Unknown -> ErrorReason.Unknown
        else -> ErrorReason.Unknown
    }

    fun toAuthReason(throwable: Throwable): ErrorReason = when (throwable) {
        QrLoginAuthenticationException.NotAWooSite -> ErrorReason.NotAWooSite
        is QrLoginAuthenticationException.UserNotEligible -> ErrorReason.UserNotEligible
        is CookieNonceAuthenticationException -> ErrorReason.SiteAuthFailure
        is OnChangedException -> (throwable.error as? SiteError)?.type.toErrorReason()
        // Catches DNS, socket, SSL handshake, and read failures during the post-exchange site
        // discovery + AP save chain, which throw raw IOException without QrLoginExchangeException.
        is IOException -> ErrorReason.Network
        is CancellationException -> throw throwable
        else -> {
            WooLog.w(WooLog.T.LOGIN, "QR login: unmapped failure type ${throwable.javaClass.simpleName}")
            ErrorReason.Unknown
        }
    }

    private fun SiteErrorType?.toErrorReason(): ErrorReason = when (this) {
        SiteErrorType.UNAUTHORIZED,
        SiteErrorType.NOT_AUTHENTICATED -> ErrorReason.SiteAuthFailure
        SiteErrorType.WORDPRESS_COM_CONNECTIVITY_ERROR -> ErrorReason.Network
        else -> ErrorReason.Unknown
    }

    fun toWpComScanReason(throwable: Throwable): ErrorReason = when (throwable) {
        WpComQrLoginScanException.RestForbidden -> ErrorReason.TokenRejected
        WpComQrLoginScanException.SessionNotFound -> ErrorReason.TokenRejected
        WpComQrLoginScanException.AlreadyScanned -> ErrorReason.MatchAlreadyScanned
        WpComQrLoginScanException.RateLimited -> ErrorReason.RateLimited
        WpComQrLoginScanException.Network -> ErrorReason.Network
        WpComQrLoginScanException.MalformedResponse -> ErrorReason.ServerError
        is WpComQrLoginScanException.HttpError -> ErrorReason.ServerError
        is WpComQrLoginScanException.Unknown -> ErrorReason.Unknown
        // NoNumberMatching is unreachable: the client always sends supports_number_matching=true.
        // Log so we notice if the contract drifts, but surface as a generic failure.
        WpComQrLoginScanException.NoNumberMatching -> {
            WooLog.w(WooLog.T.LOGIN, "QR login wp.com scan: unexpected no_number_matching from server")
            ErrorReason.Unknown
        }
        is IOException -> ErrorReason.Network
        else -> ErrorReason.Unknown
    }

    fun toWpComPollReason(throwable: Throwable): ErrorReason = when (throwable) {
        WpComQrLoginSessionStatusException.RateLimited -> ErrorReason.RateLimited
        WpComQrLoginSessionStatusException.Network -> ErrorReason.Network
        WpComQrLoginSessionStatusException.MalformedResponse -> ErrorReason.ServerError
        is WpComQrLoginSessionStatusException.HttpError -> ErrorReason.ServerError
        is WpComQrLoginSessionStatusException.Unknown -> ErrorReason.Unknown
        else -> {
            WooLog.w(WooLog.T.LOGIN, "QR login wp.com poll: unmapped failure type ${throwable.javaClass.simpleName}")
            ErrorReason.Unknown
        }
    }

    fun toWpComExchangeReason(throwable: Throwable): ErrorReason = when (throwable) {
        WpComQrLoginExchangeException.NotApproved -> ErrorReason.MatchTimedOut
        WpComQrLoginExchangeException.InvalidExchangeGrant -> ErrorReason.MatchInvalidGrant
        WpComQrLoginExchangeException.AlreadyConsumed -> ErrorReason.MatchAlreadyCompleted
        WpComQrLoginExchangeException.SessionNotFound -> ErrorReason.MatchInvalidGrant
        WpComQrLoginExchangeException.RateLimited -> ErrorReason.RateLimited
        WpComQrLoginExchangeException.Network -> ErrorReason.Network
        WpComQrLoginExchangeException.MalformedResponse -> ErrorReason.ServerError
        is WpComQrLoginExchangeException.HttpError -> ErrorReason.ServerError
        is WpComQrLoginExchangeException.Unknown -> ErrorReason.Unknown
        else -> ErrorReason.Unknown
    }
}
