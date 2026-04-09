package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosToolbar(
    modifier: Modifier = Modifier,
    titleText: String,
    onBackClicked: (() -> Unit)? = null,
    titleStyle: WooPosTypography = WooPosTypography.Heading,
    titleFontWeight: FontWeight = FontWeight.Bold,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp),
    ) {
        val (backButton, title, trailing) = createRefs()

        AnimatedVisibility(
            visible = onBackClicked != null,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.constrainAs(backButton) {
                start.linkTo(parent.start)
                bottom.linkTo(parent.bottom)
                top.linkTo(parent.top)
            }
        ) {
            WooPosBackButton(
                modifier = Modifier.padding(start = WooPosSpacing.Small.value)
            ) { onBackClicked?.invoke() }
        }

        val startPadding = WooPosSpacing.Small.value
        WooPosText(
            text = titleText,
            style = titleStyle,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = titleFontWeight,
            maxLines = 1,
            modifier = Modifier
                .constrainAs(title) {
                    if (onBackClicked != null) {
                        start.linkTo(backButton.end, margin = startPadding)
                    } else {
                        start.linkTo(parent.start, margin = startPadding)
                    }
                    if (trailingContent != null) {
                        end.linkTo(trailing.start, margin = startPadding)
                        width = Dimension.fillToConstraints
                    }
                    bottom.linkTo(parent.bottom)
                    top.linkTo(parent.top)
                }
        )

        if (trailingContent != null) {
            Box(
                modifier = Modifier.constrainAs(trailing) {
                    end.linkTo(parent.end, margin = startPadding)
                    bottom.linkTo(parent.bottom)
                    top.linkTo(parent.top)
                }
            ) {
                trailingContent()
            }
        }
    }
}

@WooPosPreview
@Composable
fun WooPosToolbarPreview() {
    WooPosTheme {
        Column {
            WooPosToolbar(
                titleText = "Title",
                onBackClicked = { }
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
