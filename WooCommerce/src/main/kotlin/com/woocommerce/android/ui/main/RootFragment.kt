package com.woocommerce.android.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.woocommerce.android.R.layout

/**
 * This empty fragment is the start destination for the navigation graph. It's basically a hack to
 * work around the fact that the graph requires a start destination, but we don't want the start
 * destination to cover up the top level fragments (which aren't part of the graph)
 */
class RootFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layout.fragment_root, container, false)
    }
}
