package com.woocommerce.android.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.woocommerce.android.ui.products.ProductDetailViewModel
import com.woocommerce.android.ui.products.ProductImagesViewModel
import com.woocommerce.android.ui.products.ProductListViewModel
import com.woocommerce.android.ui.products.ProductVariantsViewModel
import com.woocommerce.android.ui.refunds.IssueRefundViewModel
import com.woocommerce.android.ui.refunds.RefundDetailViewModel
import com.woocommerce.android.ui.reviews.ReviewDetailViewModel
import com.woocommerce.android.ui.reviews.ReviewListViewModel
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
internal abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(ProductDetailViewModel::class)
    internal abstract fun pluginProductDetailViewModel(viewModel: ProductDetailViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProductImagesViewModel::class)
    internal abstract fun pluginProductImagesViewModel(viewModel: ProductImagesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProductListViewModel::class)
    internal abstract fun pluginProductListViewModel(viewModel: ProductListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProductVariantsViewModel::class)
    internal abstract fun pluginProductVariantsViewModel(viewModel: ProductVariantsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(IssueRefundViewModel::class)
    internal abstract fun issueRefundViewModel(viewModel: IssueRefundViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RefundDetailViewModel::class)
    internal abstract fun refundDetailViewModel(viewModel: RefundDetailViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ReviewListViewModel::class)
    internal abstract fun pluginReviewListViewModel(viewModel: ReviewListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ReviewDetailViewModel::class)
    internal abstract fun pluginReviewDetailViewModel(viewModel: ReviewDetailViewModel): ViewModel

    @Binds
    internal abstract fun provideViewModelFactory(viewModelFactory: ViewModelFactory): ViewModelProvider.Factory
}
