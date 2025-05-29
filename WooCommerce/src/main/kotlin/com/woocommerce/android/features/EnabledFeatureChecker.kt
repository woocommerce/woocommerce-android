package com.woocommerce.android.features

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.store.WooCommerceStore

/**
 * Responsible for fetching and storing SSR data for the selected site,
 * and allowing consumers to check features based on that data.
 *
 * You must call [prepare] before using [checkFeature], or a [NotReadyException] will be thrown.
 */
class SSRFeatureEvaluator(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) {

    private var ssr: WCSSRModel? = null

    class PrepareFailedException(message: String) : Exception(message)
    class NotReadyException(message: String) : Exception(message)

    /**
     * Fetches the SSR model from the remote store and caches it.
     * Throws [PrepareFailedException] if the model is null.
     */
    suspend fun prepare() {
        val result = wooCommerceStore.fetchSSR(selectedSite.get()).model
        if (result == null) {
            throw PrepareFailedException("SSR could not be fetched")
        }
        ssr = result
    }

    /**
     * Invalidates the current SSR data so that a new [prepare] call is needed.
     */
    fun invalidate() {
        ssr = null
    }

    /**
     * Returns whether the evaluator is ready to check features (i.e., [prepare] was successfully called).
     */
    fun isReady(): Boolean = ssr != null

    /**
     * Checks a feature using the provided [SSRFeatureChecker]. Requires [prepare] to be called first.
     *
     * @throws NotReadyException if SSR is not loaded.
     */
    suspend fun checkFeature(checker: SSRFeatureChecker): Boolean {
        val nonNullSSR = ssr ?: throw NotReadyException(
            "Class is not ready to check features. `prepare()` failed or wasn't called."
        )
        return checker.check(nonNullSSR)
    }
}

/**
 * Contract for feature checkers that evaluate feature availability based on SSR settings.
 */
interface SSRFeatureChecker {
    suspend fun check(settings: WCSSRModel): Boolean
}
