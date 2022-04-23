package com.woocommerce.android.ui.moremenu

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.push.UnseenReviewsCountHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore

class MoreMenuViewModelTests : BaseUnitTest() {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val unseenReviewsCountHandler: UnseenReviewsCountHandler = mock {
        on { observeUnseenCount() } doReturn flowOf(0)
    }
    private val selectedSite: SelectedSite = mock {
        on { observe() } doReturn flowOf(
            SiteModel().apply {
                displayName = "Site"
                url = "url"
            }
        )
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val accountStore: AccountStore = mock {
        on { account } doReturn AccountModel().apply {
            avatarUrl = "avatar"
        }
    }

    private lateinit var viewModel: MoreMenuViewModel

    suspend fun setup(setupMocks: suspend () -> Unit) {
        setupMocks()
        viewModel = MoreMenuViewModel(
            savedState = SavedStateHandle(),
            accountStore = accountStore,
            selectedSite = selectedSite,
            unseenReviewsCountHandler = unseenReviewsCountHandler,
            appPrefsWrapper = appPrefsWrapper
        )
    }

    @Test
    fun `when coupons beta feature toggle is updated, then refresh the list of button`() = testBlocking {
        val prefsChanges = MutableSharedFlow<String>()
        setup {
            whenever(appPrefsWrapper.observePrefs()).thenReturn(prefsChanges)
        }
        val states = viewModel.moreMenuViewState.captureValues()

        prefsChanges.emit("IS_COUPONS_ENABLED")

        assertThat(states.size).isEqualTo(2)
    }
}
