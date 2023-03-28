package com.woocommerce.android.ui.jitm

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.payments.banner.Banner
import com.woocommerce.android.ui.payments.banner.BannerState

class JitmBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {
    private val content = mutableStateOf<(@Composable () -> Unit)?>(null)

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    @Composable
    override fun Content() {
        content.value?.invoke()
    }

    override fun getAccessibilityClassName(): String = "JITM_BANNER"

    fun setContent(content: @Composable () -> Unit) {
        shouldCreateCompositionOnAttachedToWindow = true
        this.content.value = content
        if (isAttachedToWindow) {
            createComposition()
        }
    }

    private fun applyBannerComposeUI(state: BannerState) {
        // Show banners only if onboarding list is not displayed
        if (state is BannerState.DisplayBannerState) {
            binding.jitmView.apply {
                binding.jitmView.show()
                // Dispose of the Composition when the view's LifecycleOwner is destroyed
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    WooThemeWithBackground {
                        Banner(bannerState = state)
                    }
                }
            }
        } else {
            binding.jitmView.hide()
        }
    }
}

