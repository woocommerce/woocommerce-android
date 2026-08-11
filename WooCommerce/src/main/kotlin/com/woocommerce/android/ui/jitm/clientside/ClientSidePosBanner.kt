package com.woocommerce.android.ui.jitm.clientside

import android.content.Context
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.jitm.JitmMessagePathsProvider
import com.woocommerce.android.ui.woopos.WooPosIsScreenSizeAllowed
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMContent
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMCta
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@Reusable
class ClientSidePosBanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val selectedSite: SelectedSite,
    private val wooStore: WooCommerceStore,
    private val featureFlagRepository: FeatureFlagRepository,
    private val wooPosIsScreenSizeAllowed: WooPosIsScreenSizeAllowed,
    private val dismissalStorage: ClientSideBannerDismissalStorage,
) {
    val messagePath: String = JitmMessagePathsProvider.MY_STORE
    val bannerId: String = BANNER_ID

    @Suppress("ReturnCount")
    suspend fun shouldShow(): Boolean {
        if (!featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_TABLET_PROMO_BANNER)) return false

        val site = selectedSite.getIfExists() ?: return false

        if (wooPosIsScreenSizeAllowed()) return false

        val countryCode = wooStore.getSiteSettingsAsync(site)?.countryCode
        if (countryCode !in ELIGIBLE_COUNTRIES) return false

        if (dismissalStorage.isBannerHidden(bannerId)) return false

        return true
    }

    fun toJitmResponse(): JITMApiResponse {
        return JITMApiResponse(
            template = "banner",
            content = JITMContent(
                message = context.getString(R.string.pos_client_side_banner_title),
                description = context.getString(R.string.pos_client_side_banner_description),
                icon = "",
                iconPath = null,
                title = ""
            ),
            cta = JITMCta(
                message = context.getString(R.string.pos_client_side_banner_cta),
                link = BANNER_URL
            ),
            timeToLive = 0,
            id = bannerId,
            featureClass = FEATURE_CLASS,
            expires = 0L,
            maxDismissal = 1,
            isDismissible = true,
            url = "",
            jitmStatsUrl = "",
            assets = mapOf(
                "background_image_url" to
                    "https://woocommerce.com/wp-content/uploads/2026/01/pos-on-phones-promotion@3x.png",
                "badge_image_url" to ""
            )
        )
    }

    fun onDismiss() {
        dismissalStorage.hideBanner(bannerId)
    }

    fun onCtaClick() {
        dismissalStorage.hideBanner(bannerId)
    }

    companion object {
        private const val BANNER_ID = "woo_pos_client_banner"
        private const val FEATURE_CLASS = "woo_pos_promotion"
        private const val BANNER_URL = "https://woocommerce.com/mobile/pos/learn-more"
        private val ELIGIBLE_COUNTRIES = listOf("US", "GB")
    }
}
