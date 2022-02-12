package com.woocommerce.android.ui.orders.creation.fees

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.junit.Before
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.store.WooCommerceStore

class OrderCreationAddFeeViewModelTest : BaseUnitTest() {
    private lateinit var sut: OrderCreationAddFeeViewModel

    @Before
    fun setUp() {
        val siteModelMock: SiteModel = mock()
        val siteMock: SelectedSite = mock {
            on { get() } doReturn siteModelMock
        }
        val settingsMock: WCSettingsModel = mock {
            on { currencyDecimalNumber } doReturn 123
        }
        val storeMock: WooCommerceStore = mock {
            on { getSiteSettings(siteModelMock) } doReturn settingsMock
        }

        sut = OrderCreationAddFeeViewModel(
            SavedStateHandle(),
            siteMock,
            storeMock
        )
    }
}
