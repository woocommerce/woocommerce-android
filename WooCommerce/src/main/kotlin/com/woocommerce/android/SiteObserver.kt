package com.woocommerce.android

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ApplicationPasswordsTester
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@Suppress("ForbiddenComment")
/**
 * A utility class that can be used to force fetching data specific to current site,
 * the fetching will occur on app launch, and on each site switching
 *
 * TODO: check and move other relevant pieces to this class, currently it's used only for fetching plugins
 */
class SiteObserver @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val applicationPasswordsTester: ApplicationPasswordsTester
) {
    suspend fun observeAndUpdateSelectedSiteData() {
        selectedSite.observe()
            .filterNotNull()
            .distinctUntilChanged { old, new -> new.id == old.id }
            .onEach {
                // TODO: Temporary test, remove after migrating actual endpoints
                applicationPasswordsTester.testApi()
            }
            .collect { site ->
                WooLog.d(WooLog.T.UTILS, "Fetch plugins for site ${site.name}")
                wooCommerceStore.fetchSitePlugins(site)
            }
    }
}
