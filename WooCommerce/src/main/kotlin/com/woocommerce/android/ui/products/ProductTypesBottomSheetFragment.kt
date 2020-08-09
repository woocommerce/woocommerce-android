package com.woocommerce.android.ui.products

import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

class ProductTypesBottomSheetFragment : BottomSheetDialogFragment(), HasAndroidInjector {
    @Inject internal lateinit var childInjector: DispatchingAndroidInjector<Any>

    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: ProductTypesBottomSheetViewModel by viewModels { viewModelFactory }

    override fun androidInjector(): AndroidInjector<Any> {
        return childInjector
    }
}
