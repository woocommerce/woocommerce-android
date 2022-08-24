package com.woocommerce.android.ui.prefs

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.banner.BannerDisplayEligibilityChecker
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class MainPresenterTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val store: WooCommerceStore = mock()
    private val mainPresenterSettingsContractView: MainSettingsContract.View = mock()
    private val accountStore: AccountStore = mock()
    private val bannerDisplayEligibilityChecker: BannerDisplayEligibilityChecker = mock()

    private lateinit var mainSettingsPresenter: MainSettingsPresenter

    @Before
    fun setup() {
        mainSettingsPresenter = MainSettingsPresenter(
            selectedSite,
            accountStore,
            store,
            mock(),
            mock(),
            bannerDisplayEligibilityChecker,
        )
        mainSettingsPresenter.takeView(mainPresenterSettingsContractView)
    }
}
