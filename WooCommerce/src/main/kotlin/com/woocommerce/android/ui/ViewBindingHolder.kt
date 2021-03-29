package com.woocommerce.android.ui

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding

/**
 * Enables implementing ViewBinding without releasing the binding in each fragment's onDestroyView().
 * Adapted from https://dropbox.tech/mobile/detecting-memory-leaks-in-android-applications
 */
interface ViewBindingHolder<B : ViewBinding> {
    // fragments should override this with the exact type of binding
    var binding: B?

    // valid only between onCreateView and onDestroyView, similar to requireActivity()
    fun requireBinding() = checkNotNull(binding)

    fun requireBinding(lambda: (B) -> Unit) {
        binding?.let {
            lambda(it)
        }
    }

    // fragments can override this to do any cleanup needed prior to releasing the binding
    fun onBeforeDestroyViewBinding() {}

    /**
     * Make sure to use this with Fragment.viewLifecycleOwner. Note that onDestroy() here is called before
     * the fragment's onDestroyView() so don't access requireBinding() from that event, instead override
     * onBeforeDestroyViewBinding() above
     */
    fun registerBinding(binding: B, lifecycleOwner: LifecycleOwner) {
        this.binding = binding
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                owner.lifecycle.removeObserver(this)
                onBeforeDestroyViewBinding()
                this@ViewBindingHolder.binding = null
            }
        })
    }
}
