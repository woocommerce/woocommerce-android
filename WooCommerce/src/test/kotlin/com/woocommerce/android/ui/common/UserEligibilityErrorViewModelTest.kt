package com.woocommerce.android.ui.common

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.UserEligibilityErrorViewModel.ViewState
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.user.WCUserModel
import org.wordpress.android.fluxc.store.WCUserStore

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class UserEligibilityErrorViewModelTest : BaseUnitTest() {
    private val appPrefsWrapper: AppPrefs = mock()

    private val selectedSite: SelectedSite = mock()
    private val userStore: WCUserStore = mock()
    private lateinit var viewModel: UserEligibilityErrorViewModel

    private val testUser = WCUserModel().apply {
        remoteUserId = 1L
        firstName = "Anitaa"
        lastName = "Murthy"
        username = "murthyanitaa"
        roles = "[author, editor]"
        email = "reallychumma1@gmail.com"
    }

    private val viewState = ViewState()

    @Before
    fun setup() {
        viewModel = spy(
            UserEligibilityErrorViewModel(
                SavedStateHandle(),
                appPrefsWrapper,
                userStore,
                selectedSite
            ))

        clearInvocations(
            viewModel,
            selectedSite,
            appPrefsWrapper
        )
    }

    @Test
    fun `Displays the user eligibility error screen correctly`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        doReturn(SiteModel()).whenever(selectedSite).get()
        doReturn(testUser).whenever(userStore).getUserByEmail(any(), any())
        whenever(appPrefsWrapper.getUserEmail()).thenReturn(testUser.email)

        val expectedViewState = viewState.copy(user = testUser.toAppModel())

        var userData: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> userData = new }

        viewModel.start()
        assertThat(userData).isEqualTo(expectedViewState)
    }
}
