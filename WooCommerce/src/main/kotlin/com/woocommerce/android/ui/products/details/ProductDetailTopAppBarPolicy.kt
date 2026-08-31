package com.woocommerce.android.ui.products.details

internal class ProductDetailTopAppBarPolicy {
    fun map(
        menu: ProductDetailViewModel.MenuButtonsState?,
        isWindowLargerThanCompact: Boolean,
        isPartOfProductListFlow: Boolean,
        showBackOnLargeScreen: Boolean,
    ): ProductDetailTopAppBarUiState {
        return ProductDetailTopAppBarUiState(
            navigation = when {
                isWindowLargerThanCompact && showBackOnLargeScreen -> ProductDetailTopAppBarNavigation.BACK
                isWindowLargerThanCompact -> null
                isPartOfProductListFlow -> ProductDetailTopAppBarNavigation.BACK
                else -> ProductDetailTopAppBarNavigation.CLOSE
            },
            primaryAction = menu.primaryAction(),
            shareAction = menu.shareAction(),
            overflowActions = menu.overflowActions(),
        )
    }

    private fun ProductDetailViewModel.MenuButtonsState?.primaryAction() = when {
        this?.saveOption == true -> ProductDetailTopAppBarAction.SAVE
        this?.publishOption == true -> ProductDetailTopAppBarAction.PUBLISH
        else -> null
    }

    private fun ProductDetailViewModel.MenuButtonsState?.shareAction() = ProductDetailTopAppBarAction.SHARE.takeIf {
        this?.shareOption == true && showShareOptionAsAction
    }

    private fun ProductDetailViewModel.MenuButtonsState?.overflowActions() = buildList {
        if (this@overflowActions?.publishOption == true && saveOption) add(ProductDetailTopAppBarAction.PUBLISH)
        if (this@overflowActions?.saveAsDraftOption == true) add(ProductDetailTopAppBarAction.SAVE_AS_DRAFT)
        if (this@overflowActions?.shareOption == true && !showShareOptionAsAction) {
            add(ProductDetailTopAppBarAction.SHARE)
        }
        if (this@overflowActions?.viewProductOption == true) add(ProductDetailTopAppBarAction.VIEW_PRODUCT)
        add(ProductDetailTopAppBarAction.SETTINGS)
        if (this@overflowActions?.duplicateOption == true) add(ProductDetailTopAppBarAction.DUPLICATE)
        if (this@overflowActions?.trashOption == true) add(ProductDetailTopAppBarAction.TRASH)
    }
}
