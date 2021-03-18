package com.woocommerce.android.ui.products

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.products.ProductsModule.AddAttributeFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.AddAttributeTermsFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.AddProductCategoryFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.AddProductDownloadFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.AttributeListFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.EditVariationAttributesFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.GroupedProductListFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.LinkedProductsFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ParentCategoryListFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductCatalogVisibilityFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductCategoriesFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductDetailBottomSheetFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductDetailFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductDownloadDetailsFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductDownloadsFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductDownloadsSettingsFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductExternalLinkFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductFilterListFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductFilterOptionListFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductImageViewerFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductImagesFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductInventoryFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductListFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductMenuOrderFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductPricingFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductReviewsFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductSelectionListFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductSettingsFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductShippingClassFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductShippingFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductSlugFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductSortingFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductStatusFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductTagsFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductTypesBottomSheetFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.ProductVisibilityFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.VariationDetailFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.VariationListFragmentModule
import com.woocommerce.android.ui.products.ProductsModule.WPMediaPickerFragmentModule
import com.woocommerce.android.ui.products.categories.AddProductCategoryFragment
import com.woocommerce.android.ui.products.categories.AddProductCategoryModule
import com.woocommerce.android.ui.products.categories.ParentCategoryListFragment
import com.woocommerce.android.ui.products.categories.ParentCategoryListModule
import com.woocommerce.android.ui.products.categories.ProductCategoriesFragment
import com.woocommerce.android.ui.products.categories.ProductCategoriesModule
import com.woocommerce.android.ui.products.downloads.AddProductDownloadBottomSheetFragment
import com.woocommerce.android.ui.products.downloads.AddProductDownloadModule
import com.woocommerce.android.ui.products.downloads.ProductDownloadDetailsFragment
import com.woocommerce.android.ui.products.downloads.ProductDownloadDetailsModule
import com.woocommerce.android.ui.products.downloads.ProductDownloadsFragment
import com.woocommerce.android.ui.products.downloads.ProductDownloadsModule
import com.woocommerce.android.ui.products.downloads.ProductDownloadsSettingsFragment
import com.woocommerce.android.ui.products.downloads.ProductDownloadsSettingsModule
import com.woocommerce.android.ui.products.reviews.ProductReviewsFragment
import com.woocommerce.android.ui.products.reviews.ProductReviewsModule
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibilityFragment
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibilityModule
import com.woocommerce.android.ui.products.settings.ProductMenuOrderFragment
import com.woocommerce.android.ui.products.settings.ProductMenuOrderModule
import com.woocommerce.android.ui.products.settings.ProductSettingsFragment
import com.woocommerce.android.ui.products.settings.ProductSettingsModule
import com.woocommerce.android.ui.products.settings.ProductSlugFragment
import com.woocommerce.android.ui.products.settings.ProductSlugModule
import com.woocommerce.android.ui.products.settings.ProductStatusFragment
import com.woocommerce.android.ui.products.settings.ProductStatusModule
import com.woocommerce.android.ui.products.settings.ProductVisibilityFragment
import com.woocommerce.android.ui.products.settings.ProductVisibilityModule
import com.woocommerce.android.ui.products.tags.ProductTagsFragment
import com.woocommerce.android.ui.products.tags.ProductTagsModule
import com.woocommerce.android.ui.products.variations.VariationDetailFragment
import com.woocommerce.android.ui.products.variations.VariationDetailModule
import com.woocommerce.android.ui.products.variations.VariationListFragment
import com.woocommerce.android.ui.products.variations.VariationListModule
import com.woocommerce.android.ui.products.variations.attributes.AddAttributeFragment
import com.woocommerce.android.ui.products.variations.attributes.AddAttributeModule
import com.woocommerce.android.ui.products.variations.attributes.AddAttributeTermsFragment
import com.woocommerce.android.ui.products.variations.attributes.AddAttributeTermsModule
import com.woocommerce.android.ui.products.variations.attributes.AttributeListFragment
import com.woocommerce.android.ui.products.variations.attributes.AttributeListModule
import com.woocommerce.android.ui.products.variations.attributes.EditVariationAttributesFragment
import com.woocommerce.android.ui.products.variations.attributes.EditVariationAttributesModule
import com.woocommerce.android.ui.wpmediapicker.WPMediaPickerFragment
import com.woocommerce.android.ui.wpmediapicker.WPMediaPickerModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [
    ProductDetailFragmentModule::class,
    VariationDetailFragmentModule::class,
    ProductListFragmentModule::class,
    ProductFilterListFragmentModule::class,
    ProductFilterOptionListFragmentModule::class,
    VariationListFragmentModule::class,
    AttributeListFragmentModule::class,
    EditVariationAttributesFragmentModule::class,
    AddAttributeFragmentModule::class,
    AddAttributeTermsFragmentModule::class,
    ProductImagesFragmentModule::class,
    ProductImageViewerFragmentModule::class,
    ProductInventoryFragmentModule::class,
    ProductShippingFragmentModule::class,
    ProductShippingClassFragmentModule::class,
    ProductPricingFragmentModule::class,
    ProductSettingsFragmentModule::class,
    ProductCatalogVisibilityFragmentModule::class,
    ProductStatusFragmentModule::class,
    ProductSlugFragmentModule::class,
    ProductExternalLinkFragmentModule::class,
    ProductMenuOrderFragmentModule::class,
    ProductVisibilityFragmentModule::class,
    WPMediaPickerFragmentModule::class,
    ProductSortingFragmentModule::class,
    ProductCategoriesFragmentModule::class,
    AddProductCategoryFragmentModule::class,
    ParentCategoryListFragmentModule::class,
    ProductTagsFragmentModule::class,
    ProductDetailBottomSheetFragmentModule::class,
    ProductTypesBottomSheetFragmentModule::class,
    ProductReviewsFragmentModule::class,
    GroupedProductListFragmentModule::class,
    ProductSelectionListFragmentModule::class,
    LinkedProductsFragmentModule::class,
    ProductDownloadsFragmentModule::class,
    ProductDownloadDetailsFragmentModule::class,
    ProductDownloadsSettingsFragmentModule::class,
    AddProductDownloadFragmentModule::class
])

object ProductsModule {
    @Module
    abstract class ProductListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductListModule::class])
        abstract fun productListFragment(): ProductListFragment
    }

    @Module
    abstract class ProductFilterListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductFilterListModule::class])
        abstract fun productFilterListFragment(): ProductFilterListFragment
    }

    @Module
    abstract class ProductFilterOptionListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductFilterOptionListModule::class])
        abstract fun productFilterOptionListFragment(): ProductFilterOptionListFragment
    }

    @Module
    abstract class ProductDetailFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductDetailModule::class])
        abstract fun productDetailFragment(): ProductDetailFragment
    }

    @Module
    abstract class VariationDetailFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [VariationDetailModule::class])
        abstract fun variationDetailFragment(): VariationDetailFragment
    }

    @Module
    internal abstract class VariationListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [VariationListModule::class])
        abstract fun variationListFragment(): VariationListFragment
    }

    @Module
    internal abstract class AttributeListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [AttributeListModule::class])
        abstract fun attributeListFragment(): AttributeListFragment
    }

    @Module
    internal abstract class AddAttributeFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [AddAttributeModule::class])
        abstract fun addAttributeFragment(): AddAttributeFragment
    }

    @Module
    internal abstract class AddAttributeTermsFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [AddAttributeTermsModule::class])
        abstract fun addAttributeTermsFragment(): AddAttributeTermsFragment
    }

    @Module
    internal abstract class ProductInventoryFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductInventoryModule::class])
        abstract fun productInventoryFragment(): ProductInventoryFragment
    }

    @Module
    internal abstract class ProductShippingFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductShippingModule::class])
        abstract fun productShippingFragment(): ProductShippingFragment
    }

    @Module
    internal abstract class ProductShippingClassFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductShippingClassModule::class])
        abstract fun productShippingClassFragment(): ProductShippingClassFragment
    }

    @Module
    internal abstract class ProductImagesFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductImagesModule::class])
        abstract fun productImagesFragment(): ProductImagesFragment
    }

    @Module
    internal abstract class ProductImageViewerFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductImageViewerModule::class])
        abstract fun productImageViewerFragment(): ProductImageViewerFragment
    }

    @Module
    internal abstract class ProductPricingFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductPricingModule::class])
        abstract fun productPricingFragment(): ProductPricingFragment
    }

    @Module
    internal abstract class ProductSettingsFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductSettingsModule::class])
        abstract fun productSettingsFragment(): ProductSettingsFragment
    }

    @Module
    internal abstract class ProductStatusFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductStatusModule::class])
        abstract fun productStatusFragment(): ProductStatusFragment
    }

    @Module
    internal abstract class ProductCatalogVisibilityFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductCatalogVisibilityModule::class])
        abstract fun productCatalogVisibilityFragment(): ProductCatalogVisibilityFragment
    }

    @Module
    internal abstract class ProductVisibilityFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductVisibilityModule::class])
        abstract fun productVisibilityFragment(): ProductVisibilityFragment
    }

    @Module
    internal abstract class ProductSlugFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductSlugModule::class])
        abstract fun productSlugFragment(): ProductSlugFragment
    }

    @Module
    internal abstract class ProductExternalLinkFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductExternalLinkModule::class])
        abstract fun productExternalLinkFragment(): ProductExternalLinkFragment
    }

    @Module
    internal abstract class LinkedProductsFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [LinkedProductsModule::class])
        abstract fun linkedProductsFragment(): LinkedProductsFragment
    }

    @Module
    internal abstract class ProductMenuOrderFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductMenuOrderModule::class])
        abstract fun productMenuOrderFragment(): ProductMenuOrderFragment
    }

    @Module
    internal abstract class WPMediaPickerFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [WPMediaPickerModule::class])
        abstract fun wpMediaPickerFragment(): WPMediaPickerFragment
    }

    @Module
    internal abstract class ProductSortingFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductSortingModule::class])
        abstract fun productSortingFragment(): ProductSortingFragment
    }

    @Module
    internal abstract class ProductCategoriesFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductCategoriesModule::class])
        abstract fun productCategoriesFragment(): ProductCategoriesFragment
    }

    @Module
    internal abstract class AddProductCategoryFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [AddProductCategoryModule::class])
        abstract fun addProductCategoryFragment(): AddProductCategoryFragment
    }

    @Module
    internal abstract class ParentCategoryListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ParentCategoryListModule::class])
        abstract fun parentCategoryListFragment(): ParentCategoryListFragment
    }

    @Module
    internal abstract class ProductTagsFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductTagsModule::class])
        abstract fun productTagsFragment(): ProductTagsFragment
    }

    @Module
    internal abstract class ProductDetailBottomSheetFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductDetailBottomSheetModule::class])
        abstract fun productDetailBottomSheetFragment(): ProductDetailBottomSheetFragment
    }

    @Module
    internal abstract class ProductTypesBottomSheetFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductTypesBottomSheetModule::class])
        abstract fun productTypesBottomSheetFragment(): ProductTypesBottomSheetFragment
    }

    @Module
    internal abstract class ProductReviewsFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductReviewsModule::class])
        abstract fun productReviewsFragment(): ProductReviewsFragment
    }

    @Module
    internal abstract class GroupedProductListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [GroupedProductListModule::class])
        abstract fun groupedProductListFragment(): GroupedProductListFragment
    }

    @Module
    internal abstract class ProductSelectionListFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductSelectionListModule::class])
        abstract fun productSelectionListFragment(): ProductSelectionListFragment
    }

    @Module
    internal abstract class ProductDownloadsFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductDownloadsModule::class])
        abstract fun productDownloadsFragment(): ProductDownloadsFragment
    }

    @Module
    internal abstract class ProductDownloadDetailsFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductDownloadDetailsModule::class])
        abstract fun productDownloadDetailsFragment(): ProductDownloadDetailsFragment
    }

    @Module
    internal abstract class ProductDownloadsSettingsFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ProductDownloadsSettingsModule::class])
        abstract fun productDownloadsSettingsFragment(): ProductDownloadsSettingsFragment
    }

    @Module
    internal abstract class AddProductDownloadFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [AddProductDownloadModule::class])
        abstract fun provideAddProductDownloadFragment(): AddProductDownloadBottomSheetFragment
    }

    @Module
    internal abstract class EditVariationAttributesFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [EditVariationAttributesModule::class])
        abstract fun editVariationAttributesFragment(): EditVariationAttributesFragment
    }
}
