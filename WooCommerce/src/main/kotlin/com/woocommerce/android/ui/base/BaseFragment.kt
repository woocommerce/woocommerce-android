package com.woocommerce.android.ui.base

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.woocommerce.android.ui.main.MainActivity
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

/**
 * All top level fragments and child fragments should extend this class to provide a consistent method
 * of setting the activity title
 */
abstract class BaseFragment : Fragment(), BaseFragmentView, HasAndroidInjector {
    @Inject internal lateinit var androidInjector: DispatchingAndroidInjector<Any>

    companion object {
        private const val KEY_TITLE = "title"
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.let {
            activity?.title = it.getString(KEY_TITLE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_TITLE, getFragmentTitle())
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            updateActivityTitle()
        }
    }

    override fun onResume() {
        super.onResume()
        updateActivityTitle()
    }

    fun updateActivityTitle() {
        // if this is a top level fragment, skip setting the title when it's not active in the bottom nav
        (this as? TopLevelFragment)?.let {
            if (!it.isActive) {
                return
            }
        }
        if (isAdded && !isHidden) {
            activity?.title = getFragmentTitle()
        }
    }

    override fun getFragmentTitle(): String {
        return activity?.title?.toString() ?: ""
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }
}
