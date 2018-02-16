package com.woocommerce.android.ui.base

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.fragment_parent.*

abstract class ParentFragment : Fragment(), ParentFragmentView {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        childFragmentManager.addOnBackStackChangedListener(this)
    }

    final override fun onCreateView(inflater: LayoutInflater?,
                                    container: ViewGroup?,
                                    savedInstanceState: Bundle?): View? {
        val layout: FrameLayout = inflater?.inflate(R.layout.fragment_parent,
                container, false) as FrameLayout
        val view: View? = onCreateFragmentView(inflater, layout, savedInstanceState)
        view?.let {
            layout.addView(view)
        }
        return layout
    }

    override fun onDestroyView() {
        container.removeAllViews()
        super.onDestroyView()
    }

    override fun loadChildFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commit()
    }

    /**
     * Set the top-level fragment view to [View.GONE] if a child has been added
     * to prevent click events from passing through to the covered view.
     *
     * If all child views have been removed, set the top-level fragment view to
     * [View.VISIBLE] to enable click events.
     */
    override fun onBackStackChanged() {
        val mainActivity: AppCompatActivity? = activity as AppCompatActivity
        if (childFragmentManager.backStackEntryCount == 0) {
            container.getChildAt(0).visibility = View.VISIBLE
            mainActivity?.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            mainActivity?.supportActionBar?.setDisplayShowHomeEnabled(false)
        } else {
            container.getChildAt(0).visibility = View.GONE
            mainActivity?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            mainActivity?.supportActionBar?.setDisplayShowHomeEnabled(true)
        }
    }
}
