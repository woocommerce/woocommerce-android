package com.woocommerce.android.ui.sitepicker

import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.user.WCUserModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.system.WCApiVersionResponse
import org.wordpress.android.fluxc.store.WooCommerceStore

object SitePickerTestUtils {
    const val loginSiteAddress = "awootestshop.mystagingwebsite.com"

    val account = AccountModel().apply {
        displayName = "Anitaa"
        userName = "anitaa1990"
        avatarUrl = ""
    }

    val userInfo = SitePickerViewModel.UserInfo(
        displayName = "Anitaa",
        username = "anitaa1990",
        userAvatarUrl = ""
    )

    val apiVerificationResponse = WCApiVersionResponse(
        siteModel = SiteModel().apply { id = 4 },
        apiVersion = WooCommerceStore.WOO_API_NAMESPACE_V3
    )

    val errorApiVerificationResponse = WCApiVersionResponse(
        siteModel = SiteModel(),
        apiVersion = WooCommerceStore.WOO_API_NAMESPACE_V1
    )

    val userModel = WCUserModel()

    fun getDefaultLoginViewState(defaultViewState: SitePickerViewModel.SitePickerViewState) = defaultViewState.copy(
        isToolbarVisible = false,
        isHelpBtnVisible = true,
        isSecondaryBtnVisible = true,
        isPrimaryBtnVisible = true
    )

    fun getDefaultSwitchStoreViewState(defaultViewState: SitePickerViewModel.SitePickerViewState) =
        defaultViewState.copy(
            isToolbarVisible = true,
            isHelpBtnVisible = false,
            isSecondaryBtnVisible = false,
            isPrimaryBtnVisible = true
        )

    fun getEmptyViewState(
        defaultViewState: SitePickerViewModel.SitePickerViewState,
        resourceProvider: ResourceProvider
    ) = defaultViewState.copy(
        isNoStoresViewVisible = true,
        isPrimaryBtnVisible = true,
        primaryBtnText = resourceProvider.getString(R.string.login_jetpack_view_instructions_alt),
        noStoresLabelText = resourceProvider.getString(R.string.login_no_stores),
        noStoresBtnText = resourceProvider.getString(R.string.login_jetpack_what_is)
    )

    fun generateStores(totalCount: Int = 5): List<SiteModel> {
        val result = ArrayList<SiteModel>()
        for (i in totalCount downTo 1) {
            result.add(
                SiteModel().apply {
                    id = i
                    siteId = i.toLong()
                    hasWooCommerce = true
                }
            )
        }
        result[1].url = loginSiteAddress
        return result
    }
}
