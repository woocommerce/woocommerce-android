package com.woocommerce.android.ui.main

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.fragment.FragmentNavigator

@Navigator.Name("main_fragment")  // Use as custom tag at navigation.xml
class MainNavigator(
    private val context: Context,
    private val manager: FragmentManager,
    private val containerId: Int
) : FragmentNavigator(context, manager, containerId) {
    override fun navigate(
        destination: Destination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: androidx.navigation.Navigator.Extras?
    ): NavDestination? {
        return super.navigate(destination, args, navOptions, navigatorExtras)
    }
}
