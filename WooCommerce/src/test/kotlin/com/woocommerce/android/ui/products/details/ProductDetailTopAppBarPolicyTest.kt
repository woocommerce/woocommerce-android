package com.woocommerce.android.ui.products.details

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ProductDetailTopAppBarPolicyTest {
    private val policy = ProductDetailTopAppBarPolicy()

    @Test
    fun `given phone product-list flow, when mapped, then Back navigation is shown`() {
        val result = map(isPartOfProductListFlow = true)

        assertThat(result.navigation).isEqualTo(ProductDetailTopAppBarNavigation.BACK)
    }

    @Test
    fun `given phone without product-list flow, when mapped, then Close navigation is shown`() {
        val result = map(isPartOfProductListFlow = false)

        assertThat(result.navigation).isEqualTo(ProductDetailTopAppBarNavigation.CLOSE)
    }

    @Test
    fun `given large screen, when mapped, then navigation follows creation policy`() {
        val creation = map(isWindowLargerThanCompact = true, showBackOnLargeScreen = true)
        val existing = map(isWindowLargerThanCompact = true, showBackOnLargeScreen = false)

        assertThat(creation.navigation).isEqualTo(ProductDetailTopAppBarNavigation.BACK)
        assertThat(existing.navigation).isNull()
    }

    @Test
    fun `given Save and Publish, when mapped, then Save is primary and Publish leads ordered overflow`() {
        val result = map(
            menu = menu(
                saveOption = true,
                saveAsDraftOption = true,
                publishOption = true,
                viewProductOption = true,
                shareOption = true,
                duplicateOption = true,
                trashOption = true,
            )
        )

        assertThat(result.primaryAction).isEqualTo(ProductDetailTopAppBarAction.SAVE)
        assertThat(result.shareAction).isNull()
        assertThat(result.overflowActions).containsExactly(
            ProductDetailTopAppBarAction.PUBLISH,
            ProductDetailTopAppBarAction.SAVE_AS_DRAFT,
            ProductDetailTopAppBarAction.SHARE,
            ProductDetailTopAppBarAction.VIEW_PRODUCT,
            ProductDetailTopAppBarAction.SETTINGS,
            ProductDetailTopAppBarAction.DUPLICATE,
            ProductDetailTopAppBarAction.TRASH,
        )
    }

    @Test
    fun `given Publish without Save, when mapped, then Publish is primary and not duplicated in overflow`() {
        val result = map(menu = menu(publishOption = true))

        assertThat(result.primaryAction).isEqualTo(ProductDetailTopAppBarAction.PUBLISH)
        assertThat(result.overflowActions).containsExactly(ProductDetailTopAppBarAction.SETTINGS)
    }

    @Test
    fun `given allowed direct Share, when mapped, then Share is direct and Settings remains in overflow`() {
        val result = map(
            menu = menu(
                shareOption = true,
                showShareOptionAsAction = true,
            )
        )

        assertThat(result.primaryAction).isNull()
        assertThat(result.shareAction).isEqualTo(ProductDetailTopAppBarAction.SHARE)
        assertThat(result.overflowActions).containsExactly(ProductDetailTopAppBarAction.SETTINGS)
    }

    @Test
    fun `given Trash action, when inspected, then only Trash is destructive`() {
        assertThat(ProductDetailTopAppBarAction.entries.filter { it.isDestructive })
            .containsExactly(ProductDetailTopAppBarAction.TRASH)
    }

    private fun map(
        menu: ProductDetailViewModel.MenuButtonsState? = null,
        isWindowLargerThanCompact: Boolean = false,
        isPartOfProductListFlow: Boolean = false,
        showBackOnLargeScreen: Boolean = false,
    ) = policy.map(
        menu = menu,
        isWindowLargerThanCompact = isWindowLargerThanCompact,
        isPartOfProductListFlow = isPartOfProductListFlow,
        showBackOnLargeScreen = showBackOnLargeScreen,
    )

    @Suppress("LongParameterList")
    private fun menu(
        saveOption: Boolean = false,
        saveAsDraftOption: Boolean = false,
        publishOption: Boolean = false,
        viewProductOption: Boolean = false,
        shareOption: Boolean = false,
        showShareOptionAsAction: Boolean = false,
        duplicateOption: Boolean = false,
        trashOption: Boolean = false,
    ) = ProductDetailViewModel.MenuButtonsState(
        saveOption = saveOption,
        saveAsDraftOption = saveAsDraftOption,
        publishOption = publishOption,
        viewProductOption = viewProductOption,
        shareOption = shareOption,
        showShareOptionAsAction = showShareOptionAsAction,
        duplicateOption = duplicateOption,
        trashOption = trashOption,
    )
}
