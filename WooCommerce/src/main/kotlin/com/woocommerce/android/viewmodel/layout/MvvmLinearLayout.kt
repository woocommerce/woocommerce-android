package com.woocommerce.android.viewmodel.layout

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.lifecycle.LifecycleOwner
import com.woocommerce.android.viewmodel.view.IMvvmCustomView
import com.woocommerce.android.viewmodel.view.IMvvmCustomViewModel
import com.woocommerce.android.viewmodel.view.IMvvmViewState
import com.woocommerce.android.viewmodel.view.LifecycleOwnerNotFoundException
import com.woocommerce.android.viewmodel.view.MvvmCustomViewStateWrapper

abstract class MvvmLinearLayout<V: IMvvmViewState, T: IMvvmCustomViewModel<V>>(
    context: Context,
    attributeSet: AttributeSet?
): LinearLayout(context, attributeSet), IMvvmCustomView<V, T> {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val lifecycleOwner = context as? LifecycleOwner ?: throw LifecycleOwnerNotFoundException()

        onLifecycleOwnerAttached(lifecycleOwner)
    }

    override fun onSaveInstanceState() =
            MvvmCustomViewStateWrapper(
                    super.onSaveInstanceState(),
                    viewModel.state
            )

    @Suppress("UNCHECKED_CAST")
    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is MvvmCustomViewStateWrapper) {
            viewModel.state = state.state as V
            super.onRestoreInstanceState(state.superState)
        }
    }
}
