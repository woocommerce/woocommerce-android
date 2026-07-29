package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ProductNavigatorTest {
    @Test
    fun `given exit product at detail root, when exit is handled, then back navigation continues`() {
        // GIVEN
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        var parentBackPressCalls = 0
        activity.onBackPressedDispatcher.addCallback(
            activity,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    parentBackPressCalls++
                }
            }
        )
        val fragment = TestProductFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
        val navController: NavController = mock {
            on { navigateUp() } doReturn false
        }
        Navigation.setViewNavController(fragment.requireView(), navController)
        activity.onBackPressedDispatcher.onBackPressed()

        // WHEN
        ProductNavigator().navigate(fragment, ProductNavigationTarget.ExitProduct)

        // THEN
        verify(navController).navigateUp()
        assertThat(fragment.backPressCalls).isEqualTo(1)
        assertThat(parentBackPressCalls).isEqualTo(1)
    }

    class TestProductFragment : BaseFragment(), BackPressListener {
        var backPressCalls = 0
            private set

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View = View(requireContext())

        override fun onRequestAllowBackPress(): Boolean {
            backPressCalls++
            return false
        }
    }
}
