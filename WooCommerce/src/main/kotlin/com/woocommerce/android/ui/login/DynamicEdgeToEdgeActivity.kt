package com.woocommerce.android.ui.login

// The prologue screen requires an edge-to-edge layout, whereas the login screens do not. This interface allows
// LoginActivity to enable edge-edge layout needed and disable it when the login screens are displayed.
interface DynamicEdgeToEdgeActivity {
    fun enableDynamicEdgeToEdge(forceDarkStatusBar: Boolean = false)
    fun disableDynamicEdgeToEdge()
}
