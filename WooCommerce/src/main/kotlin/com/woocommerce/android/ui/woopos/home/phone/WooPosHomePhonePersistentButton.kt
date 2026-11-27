package com.woocommerce.android.ui.woopos.home.phone

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

@Composable
internal fun WooPosHomePhonePersistentButton(
    state: WooPosHomePhonePersistentButtonState,
    onOpenCartClick: () -> Unit,
    onCheckoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shownState by remember { mutableStateOf(state) }
    if (state != WooPosHomePhonePersistentButtonState.Hidden) shownState = state

    AnimatedVisibility(
        visible = state != WooPosHomePhonePersistentButtonState.Hidden,
        enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
        exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceBright,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AnimatedContent(
                targetState = shownState,
                contentKey = { it::class },
                transitionSpec = { (fadeIn(tween()) togetherWith fadeOut(tween())).using(null) },
                label = "phone_cart_button",
            ) { shown ->
                val buttonModifier = Modifier
                    .fillMaxWidth()
                    .padding(WooPosSpacing.Medium.value)
                    .padding(bottom = WooPosSpacing.Small.value)
                    .navigationBarsPadding()
                when (shown) {
                    is WooPosHomePhonePersistentButtonState.OpenCart -> WooPosButton(
                        text = "${stringResource(R.string.woopos_cart_title)} (${shown.itemCount})",
                        onClick = onOpenCartClick,
                        modifier = buttonModifier,
                    )
                    is WooPosHomePhonePersistentButtonState.Checkout -> WooPosButton(
                        text = stringResource(R.string.woopos_checkout_button),
                        state = when (shown.isEnabled) {
                            true -> WooPosButtonState.ENABLED
                            false -> WooPosButtonState.DISABLED
                        },
                        onClick = onCheckoutClick,
                        modifier = buttonModifier.testTag(WooPosTestTags.CHECKOUT_BUTTON),
                    )
                    WooPosHomePhonePersistentButtonState.Hidden -> error("Unexpected hidden cart button")
                }
            }
        }
    }
}
