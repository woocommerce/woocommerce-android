package com.woocommerce.android.network

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.InvalidSignatureListener
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide holder that records when a store request fails with the invalid signature error
 * (Jetpack's `rest_invalid_signature`). This is a server-side problem the app can't fix, so instead of
 * silently retrying we surface it to the merchant in a dialog shown across the app.
 *
 * The detected state is keyed by the remote site id so it only applies to the affected store, and it clears
 * automatically once a request to that store succeeds again (self-healing).
 */
@Singleton
class StoreConnectionErrorMonitor @Inject constructor(
    private val selectedSite: SelectedSite
) : InvalidSignatureListener {
    private val _invalidSignatureDetected = MutableStateFlow<Long?>(null)

    /**
     * Emits the remote site id of the store currently affected by the invalid signature error, or `null`
     * when no store is affected.
     */
    val invalidSignatureDetected: StateFlow<Long?> = _invalidSignatureDetected.asStateFlow()

    /**
     * Returns `true` when the invalid signature error is currently active for the selected store.
     */
    fun isDetectedForSelectedSite(): Boolean =
        _invalidSignatureDetected.value != null && _invalidSignatureDetected.value == selectedSite.getOrNull()?.siteId

    override fun onInvalidSignatureDetected(siteModel: SiteModel) {
        _invalidSignatureDetected.value = siteModel.siteId
    }

    override fun onSuccessfulConnection(siteModel: SiteModel) {
        if (_invalidSignatureDetected.value == siteModel.siteId) {
            _invalidSignatureDetected.value = null
        }
    }
}
