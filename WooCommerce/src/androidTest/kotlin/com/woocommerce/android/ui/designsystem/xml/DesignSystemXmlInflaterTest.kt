package com.woocommerce.android.ui.designsystem.xml

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.annotation.AttrRes
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.ui.designsystem.DesignSystemMode
import com.woocommerce.android.ui.prefs.WCSettingsToggleOptionView
import com.woocommerce.android.util.FeatureFlag
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.google.android.material.R as MaterialR

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DesignSystemXmlInflaterTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        AppPrefs.init(appContext)
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        AppPrefs.removeFeatureFlagOverride(FeatureFlag.NEW_DESIGN_SYSTEM)
    }

    @Test
    fun givenDefaultModeAndFlagDisabledWhenInflatingThenLegacyInflaterAndValuesAreUsed() {
        AppPrefs.setFeatureFlagOverride(FeatureFlag.NEW_DESIGN_SYSTEM, false)

        withThemedFragment { fragment ->
            val baseInflater = LayoutInflater.from(fragment.requireContext())
            val inflater = fragment.designSystemXmlLayoutInflater(baseInflater)

            val root = inflater.inflate(R.layout.fragment_settings_beta, null, false)

            assertThat(inflater).isSameAs(baseInflater)
            assertThat(root.backgroundColor()).isEqualTo(fragment.requireContext().colorSurface())
        }
    }

    @Test
    fun givenDefaultModeAndFlagEnabledWhenInflatingThenOverlayValuesAreUsed() {
        AppPrefs.setFeatureFlagOverride(FeatureFlag.NEW_DESIGN_SYSTEM, true)

        withThemedFragment { fragment ->
            val baseInflater = LayoutInflater.from(fragment.requireContext())
            val inflater = fragment.designSystemXmlLayoutInflater(baseInflater)

            val root = inflater.inflate(R.layout.fragment_settings_beta, null, false)

            assertThat(inflater).isNotSameAs(baseInflater)
            assertThat(root.backgroundColor()).isEqualTo(fragment.requireContext().designSystemXmlColorSurface())
        }
    }

    @Test
    fun givenExplicitLegacyModeWhenFlagEnabledThenOriginalInflaterAndLegacyValuesAreUsed() {
        AppPrefs.setFeatureFlagOverride(FeatureFlag.NEW_DESIGN_SYSTEM, true)

        withThemedFragment { fragment ->
            val baseInflater = LayoutInflater.from(fragment.requireContext())
            val inflater = fragment.designSystemXmlLayoutInflater(
                inflater = baseInflater,
                mode = DesignSystemMode.LEGACY,
            )

            val root = inflater.inflate(R.layout.fragment_settings_beta, null, false)

            assertThat(inflater).isSameAs(baseInflater)
            assertThat(root.backgroundColor()).isEqualTo(fragment.requireContext().colorSurface())
        }
    }

    @Test
    fun givenFlagEnabledWhenNonOptedInSettingsLayoutInflatesThenLegacyValuesAreUsed() {
        AppPrefs.setFeatureFlagOverride(FeatureFlag.NEW_DESIGN_SYSTEM, true)

        withThemedFragment { fragment ->
            val baseContext = fragment.requireContext()
            val root = LayoutInflater.from(baseContext).inflate(R.layout.fragment_settings_beta, null, false)

            assertThat(root.backgroundColor()).isEqualTo(baseContext.colorSurface())
            assertThat(root.backgroundColor()).isNotEqualTo(baseContext.designSystemXmlColorSurface())
        }
    }

    @Test
    fun givenExplicitDesignSystemModeWhenFlagDisabledThenOverlayValuesAreUsed() {
        AppPrefs.setFeatureFlagOverride(FeatureFlag.NEW_DESIGN_SYSTEM, false)

        withThemedFragment { fragment ->
            val baseInflater = LayoutInflater.from(fragment.requireContext())
            val inflater = fragment.designSystemXmlLayoutInflater(
                inflater = baseInflater,
                mode = DesignSystemMode.DESIGN_SYSTEM,
            )

            val root = inflater.inflate(R.layout.fragment_settings_beta, null, false)

            assertThat(inflater).isNotSameAs(baseInflater)
            assertThat(root.backgroundColor()).isEqualTo(fragment.requireContext().designSystemXmlColorSurface())
        }
    }

    @Test
    fun givenDesignSystemInflaterWhenInflatingThenOverlayIsScopedToInflatedRoot() {
        withThemedFragment { fragment ->
            val baseContext = fragment.requireContext()
            val inflater = fragment.designSystemXmlLayoutInflater(
                inflater = LayoutInflater.from(baseContext),
                mode = DesignSystemMode.DESIGN_SYSTEM,
            )

            val root = inflater.inflate(R.layout.fragment_settings_beta, null, false)

            assertThat(root.backgroundColor()).isEqualTo(baseContext.designSystemXmlColorSurface())
            assertThat(baseContext.colorSurface()).isNotEqualTo(baseContext.designSystemXmlColorSurface())
        }
    }

    @Test
    fun givenDesignSystemInflaterWhenInflatingSettingsViewThenConstructorContextInheritsOverlay() {
        withThemedFragment { fragment ->
            val baseContext = fragment.requireContext()
            val inflater = fragment.designSystemXmlLayoutInflater(
                inflater = LayoutInflater.from(baseContext),
                mode = DesignSystemMode.DESIGN_SYSTEM,
            )

            val root = inflater.inflate(R.layout.fragment_settings_beta, null, false) as ViewGroup
            val settingsView = root.findViewById<WCSettingsToggleOptionView>(R.id.switchAddonsToggle)

            assertThat(settingsView.context.colorSurface()).isEqualTo(baseContext.designSystemXmlColorSurface())
        }
    }

    @Test
    fun givenMainSettingsLayoutWhenInflatedThenComposeToolbarIslandPrecedesXmlContent() {
        withThemedFragment { fragment ->
            val inflater = fragment.designSystemXmlLayoutInflater(
                inflater = LayoutInflater.from(fragment.requireContext()),
                mode = DesignSystemMode.DESIGN_SYSTEM,
            )

            val root = inflater.inflate(R.layout.fragment_settings_main, null, false) as ViewGroup
            val toolbarId = appContext.resources.getIdentifier(
                "main_settings_top_app_bar",
                "id",
                appContext.packageName
            )
            val contentId = appContext.resources.getIdentifier(
                "main_settings_scroll_content",
                "id",
                appContext.packageName
            )

            assertThat(toolbarId).isNotEqualTo(0)
            assertThat(contentId).isNotEqualTo(0)
            assertThat(root.getChildAt(0)).isInstanceOf(ComposeView::class.java)
            assertThat(root.getChildAt(1)).isInstanceOf(ScrollView::class.java)
            assertThat(root.findViewById<ComposeView>(toolbarId)).isNotNull
            assertThat(root.findViewById<ScrollView>(contentId)).isNotNull
        }
    }

    private fun withThemedFragment(block: (Fragment) -> Unit) {
        block(ContextFragment(ContextThemeWrapper(appContext, R.style.Theme_Woo_DayNight)))
    }

    private fun View.backgroundColor(): Int = (background as ColorDrawable).color

    private fun Context.colorSurface(): Int = resolveColor(MaterialR.attr.colorSurface)

    private fun Context.designSystemXmlColorSurface(): Int =
        ContextThemeWrapper(this, R.style.ThemeOverlay_Woo_DesignSystem_Xml).colorSurface()

    private fun Context.resolveColor(@AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        check(theme.resolveAttribute(attr, typedValue, true)) { "Attribute $attr was not resolved" }

        return if (typedValue.resourceId != 0) {
            ContextCompat.getColor(this, typedValue.resourceId)
        } else {
            typedValue.data
        }
    }

    private class ContextFragment(
        private val themedContext: Context,
    ) : Fragment() {
        override fun getContext(): Context = themedContext
    }
}
