package com.woocommerce.android.ui

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding

/**
 * Enables implementing ViewBinding without releasing the binding in each fragment's onDestroyView()
 */
interface ViewBindingHolder<B : ViewBinding> {
    // fragments should override this with the exact type of binding
    var binding: B?

    // valid only between onCreateView and onDestroyView, similar to requireActivity()
    fun requireBinding() = checkNotNull(binding)

    /**
     * Make sure to use this with Fragment.viewLifecycleOwner
     */
    fun registerBinding(binding: B, lifecycleOwner: LifecycleOwner) {
        this.binding = binding
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                owner.lifecycle.removeObserver(this)
                // Fragment.viewLifecycleOwner will call onDestroy() before the Fragment calls onDestroyView(), so we
                // postpone the release of the viewBinding. Otherwise calling requireBinding() in onDestroyView() will
                // result in a NullPointerException
                Handler(Looper.getMainLooper()).post {
                    this@ViewBindingHolder.binding = null
                }
            }
        })
    }
}
