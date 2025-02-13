package org.wordpress.android.fluxc.network.rest.wpcom.wc.onboarding

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class OnboardingRestClient @Inject constructor(
    private val wooNetwork: WooNetwork
) {
    suspend fun fetchOnboardingTasks(site: SiteModel): WooPayload<Array<TaskGroupDto>> {
        val url = WOOCOMMERCE.onboarding.tasks.pathWcAdmin
        return wooNetwork.executeGetGsonRequest(
            site,
            url,
            Array<TaskGroupDto>::class.java
        ).toWooPayload()
    }
}
