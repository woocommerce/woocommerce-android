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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding

@Composable
fun WooPosToolbar(
    modifier: Modifier = Modifier,
    titleText: String,
    onBackClicked: (() -> Unit)? = null,
    titleStyle: WooPosTypography = WooPosTypography.Heading,
    titleFontWeight: FontWeight = FontWeight.Bold
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp),
    ) {
        val (backButton, title) = createRefs()

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
            IconButton(
                onClick = { onBackClicked?.invoke() },
                modifier = Modifier
                    .size(48.dp)
                    .padding(start = WooPosSpacing.Small.value.toAdaptivePadding())
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.woopos_toolbar_icon_content_description),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(28.dp)
                )
            }
        }

        val startPadding = WooPosSpacing.Small.value.toAdaptivePadding()
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
                    bottom.linkTo(parent.bottom)
                    top.linkTo(parent.top)
                }
        )
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
