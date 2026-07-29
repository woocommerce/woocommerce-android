package com.woocommerce.android.ui.main

import androidx.activity.addCallback
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment

internal fun FragmentActivity.addBackNavigationCallbackAfterNavHost(
    navHostFragment: NavHostFragment,
    beforeBackDispatch: () -> Unit
) {
    check(
        navHostFragment.isAdded &&
            navHostFragment.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED) &&
            navHostFragment.activity === this
    ) {
        "The NavHostFragment must be created before registering the activity back callback"
    }
    onBackPressedDispatcher.addCallback(this) {
        beforeBackDispatch()
        isEnabled = false
        try {
            onBackPressedDispatcher.onBackPressed()
        } finally {
            isEnabled = true
        }
    }
}
