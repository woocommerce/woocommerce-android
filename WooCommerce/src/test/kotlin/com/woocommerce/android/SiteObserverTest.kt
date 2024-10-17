
import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.woocommerce.android.SiteObserver
import com.woocommerce.android.config.WPComRemoteFeatureFlagRepository
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.environment.EnvironmentRepository
import com.woocommerce.android.wear.WearableConnectionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class SiteObserverTest {
    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val environmentRepository: EnvironmentRepository = mock()
    private val wearableConnectionRepository: WearableConnectionRepository = mock()
    private val featureFlagRepository: WPComRemoteFeatureFlagRepository = mock()
    private val application: Application = mock()
    private val dispatcher: Dispatcher = mock()

    private val siteObserver = SiteObserver(
        selectedSite = selectedSite,
        wooCommerceStore = wooCommerceStore,
        environmentRepository = environmentRepository,
        wearableConnectionRepository = wearableConnectionRepository,
        featureFlagRepository = featureFlagRepository,
        application = application,
        dispatcher = dispatcher
    )

    @Test
    fun `when observeAndUpdateSelectedSiteData is called, fetchRemoteFeatureFlags is called`() = runTest {
        // GIVEN
        val versionName = "1.0.0"
        val packageName = "com.woocommerce.android"
        val packageManager: PackageManager = mock()
        val packageInfo = PackageInfo().apply { this.versionName = versionName }

        whenever(application.packageName).thenReturn(packageName)
        whenever(application.packageManager).thenReturn(packageManager)
        whenever(packageManager.getPackageInfo(packageName, 0)).thenReturn(packageInfo)

        val siteModel = mock<SiteModel> {
            on { id }.thenReturn(1)
        }

        val wooResult: WooResult<String?> = mock()
        whenever(wooResult.isError).thenReturn(false)

        whenever(environmentRepository.fetchOrGetStoreID(siteModel)).thenReturn(wooResult)
        whenever(selectedSite.observe()).thenReturn(MutableStateFlow(siteModel))

        // WHEN
        val job = launch {
            siteObserver.observeAndUpdateSelectedSiteData()
        }
        advanceUntilIdle()

        // THEN
        verify(featureFlagRepository).fetchAndCacheFeatureFlags(versionName)

        job.cancel()
    }
}
