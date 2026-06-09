package com.woocommerce.android.ui.compose.designsystem.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooBadge
import com.woocommerce.android.ui.compose.designsystem.component.WooBadgeTone
import com.woocommerce.android.ui.compose.designsystem.component.WooBodyText
import com.woocommerce.android.ui.compose.designsystem.component.WooButtonSize
import com.woocommerce.android.ui.compose.designsystem.component.WooCell
import com.woocommerce.android.ui.compose.designsystem.component.WooCheckbox
import com.woocommerce.android.ui.compose.designsystem.component.WooCircularProgressIndicator
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.component.WooFilterChip
import com.woocommerce.android.ui.compose.designsystem.component.WooIconButton
import com.woocommerce.android.ui.compose.designsystem.component.WooIconButtonEmphasis
import com.woocommerce.android.ui.compose.designsystem.component.WooIconContainer
import com.woocommerce.android.ui.compose.designsystem.component.WooIconContainerTone
import com.woocommerce.android.ui.compose.designsystem.component.WooLinearProgressIndicator
import com.woocommerce.android.ui.compose.designsystem.component.WooLinkText
import com.woocommerce.android.ui.compose.designsystem.component.WooLinkedBodyText
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBanner
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBannerTone
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedIconButton
import com.woocommerce.android.ui.compose.designsystem.component.WooPageHeader
import com.woocommerce.android.ui.compose.designsystem.component.WooPageTitle
import com.woocommerce.android.ui.compose.designsystem.component.WooPrimaryButton
import com.woocommerce.android.ui.compose.designsystem.component.WooRadioButton
import com.woocommerce.android.ui.compose.designsystem.component.WooSearchField
import com.woocommerce.android.ui.compose.designsystem.component.WooSecondaryButton
import com.woocommerce.android.ui.compose.designsystem.component.WooSectionHeader
import com.woocommerce.android.ui.compose.designsystem.component.WooSettingsRow
import com.woocommerce.android.ui.compose.designsystem.component.WooSwitch
import com.woocommerce.android.ui.compose.designsystem.component.WooSwitchSettingsRow
import com.woocommerce.android.ui.compose.designsystem.component.WooTab
import com.woocommerce.android.ui.compose.designsystem.component.WooTabRow
import com.woocommerce.android.ui.compose.designsystem.component.WooTertiaryButton
import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar
import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBarAction
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.preview.PreviewOnlyCatalogScreenshotSection

private const val PHONE_WIDTH_DP = 360

@PreviewTest
@Preview(name = "Production showcase - light", widthDp = PHONE_WIDTH_DP)
@Composable
private fun ProductionShowcaseLight() {
    ScreenshotSurface(dark = false) { ProductionShowcase() }
}

@PreviewTest
@Preview(name = "Production showcase - dark", widthDp = PHONE_WIDTH_DP)
@Composable
private fun ProductionShowcaseDark() {
    ScreenshotSurface(dark = true) { ProductionShowcase() }
}

@PreviewTest
@Preview(name = "Switch and settings rows - light", widthDp = PHONE_WIDTH_DP)
@Composable
private fun RowsEnabledDisabledLight() {
    ScreenshotSurface(dark = false) { RowsEnabledDisabled() }
}

@PreviewTest
@Preview(name = "Switch and settings rows - dark", widthDp = PHONE_WIDTH_DP)
@Composable
private fun RowsEnabledDisabledDark() {
    ScreenshotSurface(dark = true) { RowsEnabledDisabled() }
}

@PreviewTest
@Preview(name = "Section header and divider - dark", widthDp = PHONE_WIDTH_DP)
@Composable
private fun SectionHeaderAndDividerDark() {
    ScreenshotSurface(dark = true) { SectionHeaderAndDivider() }
}

@PreviewTest
@Preview(name = "Section header and divider - light", widthDp = PHONE_WIDTH_DP)
@Composable
private fun SectionHeaderAndDividerLight() {
    ScreenshotSurface(dark = false) { SectionHeaderAndDivider() }
}

@PreviewTest
@Preview(name = "Top app bar long title - light", widthDp = PHONE_WIDTH_DP)
@Composable
private fun TopBarLongTitleLight() {
    ScreenshotSurface(dark = false, pad = false) { TopBarLongTitle() }
}

@PreviewTest
@Preview(name = "Top app bar long title - dark", widthDp = PHONE_WIDTH_DP)
@Composable
private fun TopBarLongTitleDark() {
    ScreenshotSurface(dark = true, pad = false) { TopBarLongTitle() }
}

@PreviewTest
@Preview(name = "Top app bar long text action 1.5x - light", widthDp = PHONE_WIDTH_DP, fontScale = 1.5f)
@Composable
private fun TopBarLongTextActionLargeFontLight() {
    ScreenshotSurface(dark = false, pad = false) { TopBarLongTextAction() }
}

@PreviewTest
@Preview(name = "Top app bar long text action 1.5x - dark", widthDp = PHONE_WIDTH_DP, fontScale = 1.5f)
@Composable
private fun TopBarLongTextActionLargeFontDark() {
    ScreenshotSurface(dark = true, pad = false) { TopBarLongTextAction() }
}

@PreviewTest
@Preview(name = "Long settings row - light", widthDp = PHONE_WIDTH_DP)
@Composable
private fun LongSettingsRowLight() {
    ScreenshotSurface(dark = false) { LongSettingsRow() }
}

@PreviewTest
@Preview(name = "Large font 1.5x - light", widthDp = PHONE_WIDTH_DP, fontScale = 1.5f)
@Composable
private fun LargeFontLight() {
    ScreenshotSurface(dark = false) { LargeFontContent() }
}

@PreviewTest
@Preview(name = "Large font lower 1.5x - light", widthDp = PHONE_WIDTH_DP, fontScale = 1.5f)
@Composable
private fun LargeFontLowerLight() {
    ScreenshotSurface(dark = false) { LargeFontLowerContent() }
}

@PreviewTest
@Preview(name = "RTL Arabic - light", widthDp = PHONE_WIDTH_DP, locale = "ar")
@Composable
private fun RtlLight() {
    ScreenshotSurface(dark = false) { RtlContent() }
}

@PreviewTest
@Preview(name = "Preview-only catalog - light", widthDp = PHONE_WIDTH_DP)
@Composable
private fun PreviewOnlyCatalogLight() {
    ScreenshotSurface(dark = false, pad = false) { PreviewOnlyCatalogScreenshotSection() }
}

@PreviewTest
@Preview(name = "Preview-only catalog - dark", widthDp = PHONE_WIDTH_DP)
@Composable
private fun PreviewOnlyCatalogDark() {
    ScreenshotSurface(dark = true, pad = false) { PreviewOnlyCatalogScreenshotSection() }
}

@Composable
private fun ScreenshotSurface(
    dark: Boolean,
    pad: Boolean = true,
    content: @Composable () -> Unit,
) {
    WooDesignSystemTheme(useDarkTheme = dark) {
        Surface(color = WooTheme.colors.background.section) {
            if (pad) {
                Box(modifier = Modifier.padding(WooTheme.padding.padding5)) {
                    Card { content() }
                }
            } else {
                content()
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(color = WooTheme.colors.surface.default) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooTheme.padding.padding5),
            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
        ) {
            content()
        }
    }
}

@Composable
private fun ProductionShowcase() {
    PrivacyIntro()
    WooPageHeader(title = "Products")
    WooLinkText(text = "View policies", onClick = {})
    Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2)) {
        WooBadge(text = "Error", tone = WooBadgeTone.Error, leadingIcon = { BadgeLeadingIcon() })
        WooBadge(text = "Success", tone = WooBadgeTone.Success, leadingIcon = { BadgeLeadingIcon() })
        WooBadge(text = "Info", tone = WooBadgeTone.Info, leadingIcon = { BadgeLeadingIcon() })
        WooBadge(text = "Outlined", tone = WooBadgeTone.NeutralOutlined, leadingIcon = { BadgeLeadingIcon() })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3)) {
        WooPrimaryButton(text = "Primary", onClick = {}, leadingIcon = { ButtonLeadingIcon() })
        WooSecondaryButton(text = "Secondary", onClick = {}, leadingIcon = { ButtonLeadingIcon() })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3)) {
        WooTertiaryButton(text = "Tertiary", onClick = {}, leadingIcon = { ButtonLeadingIcon() })
        WooPrimaryButton(
            text = "Small",
            onClick = {},
            size = WooButtonSize.Small,
            leadingIcon = { ButtonLeadingIcon() },
        )
    }
    WooPrimaryButton(text = "Disabled", onClick = {}, enabled = false)
    WooSectionHeader("Tracking")
    WooCell(
        title = "Store details",
        description = "Manage address and contact information.",
        onClick = {},
        leadingContent = {
            WooIconContainer(
                imageVector = ImageVector.vectorResource(R.drawable.ic_settings_filled_24dp),
                tone = WooIconContainerTone.Purple,
            )
        },
        trailingContent = {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24dp),
                contentDescription = null,
            )
        },
    )
    WooSwitchSettingsRow(
        title = "Crash reports",
        description = "Send crash diagnostics.",
        checked = true,
        onCheckedChange = {},
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WooCheckbox(checked = true, onCheckedChange = {})
        WooRadioButton(selected = true, onClick = {})
        WooFilterChip(selected = true, onClick = {}, label = "Selected")
    }
    WooNoticeBanner(
        title = "Orders synced",
        description = "New order data is available.",
        tone = WooNoticeBannerTone.Success,
    )
    WooNoticeBanner(
        title = "Manual review needed",
        description = "Some settings need another look.",
        tone = WooNoticeBannerTone.NeutralOutlined,
    )
    WooSearchField(
        value = "Search products",
        onValueChange = {},
        onClearClick = {},
        clearContentDescription = "Clear search",
        modifier = Modifier.fillMaxWidth(),
    )
    WooSearchField(
        value = "Disabled search",
        onValueChange = {},
        enabled = false,
        onClearClick = {},
        clearContentDescription = "Clear search",
        modifier = Modifier.fillMaxWidth(),
    )
    WooTabRow(selectedTabIndex = 0) {
        WooTab(selected = true, onClick = {}, text = "Products")
        WooTab(selected = false, onClick = {}, text = "Orders")
        WooTab(selected = false, onClick = {}, text = "More")
    }
    WooDivider()
    Row(
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WooSwitch(checked = true, onCheckedChange = {})
        WooIconButton(
            imageVector = ImageVector.vectorResource(R.drawable.ic_help_24dp),
            contentDescription = "Help",
            onClick = {},
        )
        WooOutlinedIconButton(
            imageVector = ImageVector.vectorResource(R.drawable.ic_help_24dp),
            contentDescription = "Help",
            onClick = {},
        )
        WooOutlinedIconButton(
            imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new_24dp),
            contentDescription = "Open",
            onClick = {},
            emphasis = WooIconButtonEmphasis.Primary,
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
    ) {
        Column(
            modifier = Modifier.size(width = 160.dp, height = 32.dp),
            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
        ) {
            WooLinearProgressIndicator(progress = 0.64f)
            WooLinearProgressIndicator()
        }
        WooCircularProgressIndicator(progress = 0.64f, modifier = Modifier.size(32.dp))
        WooCircularProgressIndicator(modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun RowsEnabledDisabled() {
    WooSectionHeader("Enabled")
    WooSwitchSettingsRow(
        title = "Crash reports",
        description = "Send crash diagnostics.",
        checked = true,
        onCheckedChange = {},
    )
    WooSwitchSettingsRow(
        title = "Usage analytics",
        description = "Send anonymous usage data.",
        checked = false,
        onCheckedChange = {},
    )
    WooSettingsRow(
        title = "Open policies",
        description = "Opens in a browser tab.",
        onClick = {},
        trailingContent = {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24dp),
                contentDescription = null,
            )
        },
    )
    WooDivider()
    WooSectionHeader("Disabled")
    WooSwitchSettingsRow(
        title = "Crash reports (on)",
        description = "Send crash diagnostics.",
        checked = true,
        onCheckedChange = {},
        enabled = false,
    )
    WooSwitchSettingsRow(
        title = "Usage analytics (off)",
        description = "Send anonymous usage data.",
        checked = false,
        onCheckedChange = {},
        enabled = false,
    )
    WooSettingsRow(
        title = "Open policies",
        description = "Disabled row text uses source-backed disabled color.",
        enabled = false,
        onClick = {},
        trailingContent = {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24dp),
                contentDescription = null,
            )
        },
    )
    WooLinkText(text = "Disabled link", onClick = {}, enabled = false)
}

@Composable
private fun LargeFontContent() {
    WooSectionHeader("Large font")
    PrivacyIntro(title = "Large font page title")
    WooPageHeader(title = "Products")
    WooPrimaryButton(
        text = "Primary action",
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
    )
    WooSecondaryButton(
        text = "Secondary action",
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
    )
    WooCell(
        title = "Cell title",
        description = "Cell content wraps before trailing content.",
        trailingContent = {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24dp),
                contentDescription = null,
            )
        },
    )
    WooSwitchSettingsRow(
        title = "Crash reports",
        description = "Description wraps cleanly before the switch.",
        checked = true,
        onCheckedChange = {},
    )
    WooSettingsRow(
        title = "Open policies",
        description = "Trailing content stays aligned while text grows.",
        onClick = {},
        trailingContent = {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24dp),
                contentDescription = null,
            )
        },
    )
    WooNoticeBanner(
        title = "Notice title",
        description = "Static notice content stays readable at large font.",
        tone = WooNoticeBannerTone.Info,
    )
    WooSearchField(
        value = "Search products",
        onValueChange = {},
        onClearClick = {},
        clearContentDescription = "Clear search",
        modifier = Modifier.fillMaxWidth(),
    )
    WooTabRow(selectedTabIndex = 0) {
        WooTab(selected = true, onClick = {}, text = "Products")
        WooTab(selected = false, onClick = {}, text = "Orders")
    }
    WooLinkText(text = "Disabled link", onClick = {}, enabled = false)
}

@Composable
private fun LargeFontLowerContent() {
    WooSectionHeader("Large font lower")
    WooNoticeBanner(
        title = "Outlined notice",
        description = "Outlined neutral content stays readable at large font.",
        tone = WooNoticeBannerTone.NeutralOutlined,
    )
    WooSearchField(
        value = "Search products",
        onValueChange = {},
        onClearClick = {},
        clearContentDescription = "Clear search",
        modifier = Modifier.fillMaxWidth(),
    )
    WooSearchField(
        value = "Disabled search",
        onValueChange = {},
        enabled = false,
        onClearClick = {},
        clearContentDescription = "Clear search",
        modifier = Modifier.fillMaxWidth(),
    )
    WooTabRow(selectedTabIndex = 0) {
        WooTab(selected = true, onClick = {}, text = "Products")
        WooTab(selected = false, onClick = {}, text = "Orders")
    }
    WooLinkText(text = "Disabled link", onClick = {}, enabled = false)
}

@Composable
private fun SectionHeaderAndDivider() {
    WooSectionHeader("Tracking")
    WooBodyText("Body text sits below the section header on the same surface.")
    WooDivider()
    WooBodyText("Content above and below the divider for visibility comparison.")
    WooDivider()
    WooSectionHeader("Diagnostics")
}

@Composable
private fun TopBarLongTitle() {
    WooTopAppBar(
        title = "A very long top app bar title that should truncate before actions",
        navigationIcon = ImageVector.vectorResource(R.drawable.ic_back_24dp),
        navigationIconContentDescription = "Back",
        onNavigationClick = {},
        windowInsets = WindowInsets(0),
        actions = listOf(
            WooTopAppBarAction.Text(
                text = "Save",
                onClick = {},
            ),
            WooTopAppBarAction.Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new_24dp),
                contentDescription = "Open",
                onClick = {},
            ),
        ),
    )
}

@Composable
private fun TopBarLongTextAction() {
    WooTopAppBar(
        title = "Products",
        navigationIcon = ImageVector.vectorResource(R.drawable.ic_back_24dp),
        navigationIconContentDescription = "Back",
        onNavigationClick = {},
        windowInsets = WindowInsets(0),
        actions = listOf(
            WooTopAppBarAction.Text(
                text = "Complete setup changes",
                onClick = {},
            ),
            WooTopAppBarAction.Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new_24dp),
                contentDescription = "Open",
                onClick = {},
            ),
        ),
    )
}

@Composable
private fun LongSettingsRow() {
    WooSettingsRow(
        title = "A long settings title that wraps before the trailing affordance",
        description = "A long settings description wraps underneath and keeps the trailing icon aligned.",
        onClick = {},
        trailingContent = {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new_24dp),
                contentDescription = null,
            )
        },
    )
    WooDivider()
    WooSwitchSettingsRow(
        title = "A long switch row title that wraps before the trailing switch control",
        description = "A long description that also wraps under the title without overlapping the switch.",
        checked = true,
        onCheckedChange = {},
    )
}

@Composable
private fun RtlContent() {
    WooTopAppBar(
        title = "RTL title",
        navigationIcon = ImageVector.vectorResource(R.drawable.ic_back_24dp),
        navigationIconContentDescription = "Back",
        onNavigationClick = {},
        windowInsets = WindowInsets(0),
    )
    WooSettingsRow(
        title = "Row title",
        description = "Leading and trailing content follow layout direction.",
        leadingContent = {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_info_outline_20dp),
                contentDescription = null,
            )
        },
        trailingContent = {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24dp),
                contentDescription = null,
            )
        },
    )
    WooSearchField(
        value = "بحث",
        onValueChange = {},
        onClearClick = {},
        clearContentDescription = "Clear search",
        modifier = Modifier.fillMaxWidth(),
    )
    WooTabRow(selectedTabIndex = 0) {
        WooTab(selected = true, onClick = {}, text = "المنتجات")
        WooTab(selected = false, onClick = {}, text = "الطلبات")
    }
}

@Composable
private fun PrivacyIntro(
    title: String = "Privacy",
) {
    Column(verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2)) {
        WooPageTitle(title)
        WooBodyText("Control how diagnostics and usage information help improve the app.")
        WooLinkedBodyText(text = rememberLinkedBodyText(), onLinkClick = {})
    }
}

@Composable
private fun BadgeLeadingIcon() {
    Icon(
        imageVector = ImageVector.vectorResource(R.drawable.ic_star_24dp),
        contentDescription = null,
    )
}

@Composable
private fun ButtonLeadingIcon() {
    Icon(
        imageVector = ImageVector.vectorResource(R.drawable.ic_star_24dp),
        contentDescription = null,
    )
}

@Composable
private fun rememberLinkedBodyText() = remember {
    buildAnnotatedString {
        append("Read the ")
        withLink(
            LinkAnnotation.Clickable(
                tag = "privacy_policy",
                linkInteractionListener = {},
            ),
        ) {
            append("privacy policy")
        }
        append(" before changing settings.")
    }
}
