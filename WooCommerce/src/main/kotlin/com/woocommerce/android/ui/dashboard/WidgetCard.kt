package com.woocommerce.android.ui.dashboard

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.getText
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu

@Composable
fun WidgetCard(
    @StringRes titleResource: Int,
    modifier: Modifier = Modifier,
    menu: DashboardWidgetMenu,
    @DrawableRes iconResource: Int? = null,
    button: DashboardWidgetAction? = null,
    isError: Boolean,
    content: @Composable () -> Unit
) {
    DashboardCardSurface(modifier = modifier) {
        val hasLeadingIcon = isError || iconResource != null
        Row(
            modifier = Modifier.padding(start = WooTheme.padding.padding5),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isError) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_tintable_info_outline_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(WooTheme.iconSize.size24),
                    tint = WooTheme.colors.surface.onVariant,
                )
            } else if (iconResource != null) {
                Image(
                    painter = painterResource(id = iconResource),
                    contentDescription = null,
                    modifier = Modifier.size(WooTheme.iconSize.size24),
                )
            }
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (hasLeadingIcon) WooTheme.padding.padding5 else WooTheme.padding.padding0,
                        top = WooTheme.padding.padding7,
                        end = WooTheme.padding.padding5,
                        bottom = WooTheme.padding.padding7,
                    ),
                text = stringResource(id = titleResource),
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.titleSmall.emphasized,
            )

            DashboardOverflowMenu(
                menu = menu,
            )
        }

        content()

        if (button != null && !isError) {
            TextButton(
                onClick = button.action,
                modifier = Modifier
                    .padding(
                        start = WooTheme.padding.padding3,
                        bottom = WooTheme.padding.padding1,
                    ),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = WooTheme.colors.container.onSecondaryContainer
                ),
                shape = RoundedCornerShape(WooTheme.radius.medium),
            ) {
                Text(
                    text = button.title.getText(),
                    style = WooTheme.text.labelLarge.emphasized,
                )
            }
        }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Preview(name = "RTL", locale = "ar")
@Composable
fun PreviewWidgetCard() {
    WooDesignSystemThemeWithBackground {
        WidgetCard(
            titleResource = R.string.blaze_campaign_title,
            iconResource = R.drawable.ic_blaze,
            menu = DashboardWidgetMenu(
                items = listOf(
                    DashboardWidgetAction(
                        titleResource = R.string.blaze_campaign_title,
                        action = {}
                    ),
                    DashboardWidgetAction(
                        titleResource = R.string.theme_preview_title,
                        action = {}
                    )
                )
            ),
            button = DashboardWidgetAction(
                titleResource = R.string.blaze_campaign_show_all_button,
                action = {}
            ),
            isError = false
        ) {
            Text(
                modifier = Modifier.padding(WooTheme.padding.padding5),
                text = "Content",
                style = WooTheme.text.bodyLarge.regular,
                color = WooTheme.colors.surface.onDefault,
            )
        }
    }
}
