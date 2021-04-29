package com.woocommerce.android.ui.base

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

/**
 * All top level fragments and child fragments should extend this class to provide a consistent method
 * of setting the activity title
 */
open class BaseDaggerFragment : BaseFragment, HasAndroidInjector {
    @Inject internal lateinit var androidInjector: DispatchingAndroidInjector<Any>

    constructor() : super()
    constructor(@LayoutRes layoutId: Int) : super(layoutId)

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun getFragmentTitle(): String {
        return activity?.title?.toString() ?: ""
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }
}
