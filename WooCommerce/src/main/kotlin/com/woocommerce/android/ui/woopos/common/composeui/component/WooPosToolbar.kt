package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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
    titleText: String,
    onBackClicked: () -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = WooPosSpacing.XLarge.value.toAdaptivePadding())
            .height(48.dp),
    ) {
        val (backButton, title) = createRefs()
        IconButton(
            onClick = { onBackClicked() },
            modifier = Modifier
                .constrainAs(backButton) {
                    start.linkTo(parent.start)
                    bottom.linkTo(parent.bottom)
                }
                .padding(start = WooPosSpacing.Small.value.toAdaptivePadding())
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_back_24dp),
                contentDescription = stringResource(R.string.woopos_toolbar_icon_content_description),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(28.dp)
                    .offset(y = 2.dp)
            )
        }

        val iconTitlePadding = WooPosSpacing.Small.value.toAdaptivePadding()
        WooPosText(
            text = titleText,
            style = WooPosTypography.Heading,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier
                .constrainAs(title) {
                    start.linkTo(backButton.end, margin = iconTitlePadding)
                    bottom.linkTo(parent.bottom)
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
