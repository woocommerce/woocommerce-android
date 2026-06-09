@file:Suppress("UnusedPrivateMember")

package com.woocommerce.android.ui.compose.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooBadge
import com.woocommerce.android.ui.compose.designsystem.component.WooBadgeTone
import com.woocommerce.android.ui.compose.designsystem.component.WooBodyText
import com.woocommerce.android.ui.compose.designsystem.component.WooButtonSize
import com.woocommerce.android.ui.compose.designsystem.component.WooCell
import com.woocommerce.android.ui.compose.designsystem.component.WooCellContent
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
import com.woocommerce.android.ui.compose.designsystem.component.WooVerticalDivider
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@PreviewLightDark
@Composable
private fun WooDesignSystemComponentCatalogPreview() {
    WooDesignSystemPreviewTheme {
        CatalogContent()
    }
}

@PreviewLightDark
@Composable
private fun WooDesignSystemComponentCatalogLegacyCompatiblePreview() {
    WooThemeWithBackground {
        CatalogContent()
    }
}

@Preview(name = "Large font", fontScale = 1.5f, showBackground = true)
@Composable
private fun WooDesignSystemComponentCatalogLargeFontPreview() {
    WooDesignSystemPreviewTheme {
        SensitiveComponentPreviewContent()
    }
}

@PreviewLightDark
@Composable
private fun WooDesignSystemComponentCatalogLongTextPreview() {
    WooDesignSystemPreviewTheme {
        Box(modifier = Modifier.width(320.dp)) {
            LongTextPreviewContent()
        }
    }
}

@Preview(name = "RTL", locale = "ar", showBackground = true)
@Composable
private fun WooDesignSystemComponentCatalogRtlPreview() {
    WooDesignSystemPreviewTheme {
        RtlPreviewContent()
    }
}

@Composable
private fun CatalogContent() {
    PreviewScreenScaffold(
        topBar = {
            WooTopAppBar(
                title = "Store settings",
                navigationIcon = ImageVector.vectorResource(R.drawable.ic_back_24dp),
                navigationIconContentDescription = "Back",
                onNavigationClick = {},
                windowInsets = WindowInsets(0),
                actions = listOf(
                    WooTopAppBarAction.Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new_24dp),
                        contentDescription = "Open",
                        onClick = {},
                    ),
                    WooTopAppBarAction.Text(
                        text = "Done",
                        onClick = {},
                    ),
                ),
            )
        },
        contentSpacing = WooTheme.spacing.space6,
    ) {
        ProductionCatalogSection()
        PreviewOnlyCatalogSection()
    }
}

@Composable
private fun ProductionCatalogSection() {
    CatalogSection("Production components") {
        PrivacyIntro()
        WooPageHeader(
            title = "Products",
            actions = {
                WooOutlinedIconButton(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new_24dp),
                    contentDescription = "Open",
                    onClick = {},
                )
            },
        )
        WooPageTitle("Standalone title")
        WooBodyText("Standalone body text remains available for smaller compositions.")
        WooLinkText(text = "Standalone link", onClick = {})

        WooSectionHeader("Badges")
        BadgeToneRows()

        WooSectionHeader("Buttons")
        Column(verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2)) {
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
        }

        WooSectionHeader("Cells and rows")
        WooCell(
            title = "Store details",
            description = "Manage address, currency, and contact information.",
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
        WooCellContent(
            title = "Cell content block",
            description = "Reusable title and description content for custom cell layouts.",
        )
        WooSettingsRow(
            title = "Usage analytics",
            description = "Send anonymous usage data.",
            onClick = {},
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

        WooSectionHeader("Choices")
        ChoiceControlRows()

        WooSectionHeader("Icon containers")
        IconContainerToneRows()

        WooSectionHeader("Notice banners")
        WooNoticeBanner(
            title = "Orders synced",
            description = "New order data is available.",
            tone = WooNoticeBannerTone.Success,
            leadingIcon = {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_check_circle_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            },
        )
        WooNoticeBanner(
            title = "Connection issue",
            description = "Some analytics data may be delayed.",
            tone = WooNoticeBannerTone.Warning,
            leadingIcon = {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_warning_filled_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            },
        )
        WooNoticeBanner(
            title = "Manual review needed",
            description = "Some settings need another look.",
            tone = WooNoticeBannerTone.NeutralOutlined,
            leadingIcon = {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_info_outline_20dp),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            },
        )

        WooSectionHeader("Search and tabs")
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

        WooSectionHeader("Switches, icons, dividers, and progress")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WooSwitch(
                checked = true,
                onCheckedChange = {},
            )
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
        WooDivider()
        Row(
            modifier = Modifier.size(width = 180.dp, height = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
        ) {
            WooVerticalDivider()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            ) {
                WooLinearProgressIndicator(progress = 0.64f)
                WooLinearProgressIndicator()
            }
            WooCircularProgressIndicator(
                progress = 0.64f,
                modifier = Modifier.size(32.dp),
            )
            WooCircularProgressIndicator(modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun BadgeToneRows() {
    Column(verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2)) {
        Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2)) {
            WooBadge(text = "Error", tone = WooBadgeTone.Error, leadingIcon = { BadgeLeadingIcon() })
            WooBadge(text = "Caution", tone = WooBadgeTone.Caution, leadingIcon = { BadgeLeadingIcon() })
            WooBadge(text = "Warning", tone = WooBadgeTone.Warning, leadingIcon = { BadgeLeadingIcon() })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2)) {
            WooBadge(text = "Success", tone = WooBadgeTone.Success, leadingIcon = { BadgeLeadingIcon() })
            WooBadge(text = "Info", tone = WooBadgeTone.Info, leadingIcon = { BadgeLeadingIcon() })
            WooBadge(text = "Neutral", tone = WooBadgeTone.Neutral, leadingIcon = { BadgeLeadingIcon() })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2)) {
            WooBadge(text = "Outlined", tone = WooBadgeTone.NeutralOutlined, leadingIcon = { BadgeLeadingIcon() })
        }
    }
}

@Composable
private fun ChoiceControlRows() {
    Column(verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WooCheckbox(checked = true, onCheckedChange = {})
            WooCheckbox(checked = false, onCheckedChange = {}, enabled = false)
            WooRadioButton(selected = true, onClick = {})
            WooRadioButton(selected = false, onClick = {}, enabled = false)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WooFilterChip(
                selected = true,
                onClick = {},
                label = "Selected",
                leadingIcon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_check_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            WooFilterChip(selected = false, onClick = {}, label = "Filter")
            WooFilterChip(selected = false, onClick = {}, label = "Disabled", enabled = false)
        }
    }
}

@Composable
private fun IconContainerToneRows() {
    Column(verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3)) {
        Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3)) {
            WooIconContainer(ImageVector.vectorResource(R.drawable.ic_star_24dp))
            WooIconContainer(
                imageVector = ImageVector.vectorResource(R.drawable.ic_star_24dp),
                tone = WooIconContainerTone.Sandstone,
            )
            WooIconContainer(
                imageVector = ImageVector.vectorResource(R.drawable.ic_star_24dp),
                tone = WooIconContainerTone.Blue,
            )
            WooIconContainer(
                imageVector = ImageVector.vectorResource(R.drawable.ic_star_24dp),
                tone = WooIconContainerTone.Green,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3)) {
            WooIconContainer(
                imageVector = ImageVector.vectorResource(R.drawable.ic_star_24dp),
                tone = WooIconContainerTone.Orange,
            )
            WooIconContainer(
                imageVector = ImageVector.vectorResource(R.drawable.ic_star_24dp),
                tone = WooIconContainerTone.Pink,
            )
            WooIconContainer(
                imageVector = ImageVector.vectorResource(R.drawable.ic_star_24dp),
                tone = WooIconContainerTone.DarkPurple,
            )
        }
    }
}

@Composable
private fun SensitiveComponentPreviewContent() {
    PreviewScreenScaffold(
        topBar = {
            WooTopAppBar(
                title = "Large font app bar",
                navigationIcon = ImageVector.vectorResource(R.drawable.ic_back_24dp),
                navigationIconContentDescription = "Back",
                onNavigationClick = {},
                windowInsets = WindowInsets(0),
            )
        },
    ) {
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
        WooTertiaryButton(
            text = "Tertiary action",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        WooCell(
            title = "Cell title wraps",
            description = "Cell body copy also wraps before the trailing affordance.",
            trailingContent = {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24dp),
                    contentDescription = null,
                )
            },
        )
        WooSettingsRow(
            title = "Settings row title",
            description = "Description wraps under the title and leaves space for trailing content.",
            trailingContent = {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24dp),
                    contentDescription = null,
                )
            },
        )
        WooSwitchSettingsRow(
            title = "Switch row title",
            description = "The row owns one toggle action.",
            checked = true,
            onCheckedChange = {},
        )
        WooNoticeBanner(
            title = "Large font notice",
            description = "The static banner keeps title and description readable.",
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
    }
}

@Composable
private fun LongTextPreviewContent() {
    PreviewScreenScaffold(
        topBar = {
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
        },
    ) {
        WooPageTitle("A long page title that wraps across multiple lines without clipping")
        WooBodyText(
            "Long body copy stays readable, wraps naturally, and avoids relying on a fixed one-line height.",
        )
        WooLinkText(
            text = "A long standalone link that wraps like body text",
            onClick = {},
        )
        WooPrimaryButton(
            text = "Long primary button label wraps",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        WooSearchField(
            value = "Long search query that should remain usable",
            onValueChange = {},
            onClearClick = {},
            clearContentDescription = "Clear search",
            modifier = Modifier.fillMaxWidth(),
        )
        WooSettingsRow(
            title = "A long settings title that wraps before the trailing affordance",
            description = "A long settings description wraps underneath and keeps the trailing icon aligned.",
            trailingContent = {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new_24dp),
                    contentDescription = null,
                )
            },
        )
    }
}

@Composable
private fun RtlPreviewContent() {
    PreviewScreenScaffold(
        topBar = {
            WooTopAppBar(
                title = "RTL title",
                navigationIcon = ImageVector.vectorResource(R.drawable.ic_back_24dp),
                navigationIconContentDescription = "Back",
                onNavigationClick = {},
                windowInsets = WindowInsets(0),
            )
        },
    ) {
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
private fun PreviewScreenScaffold(
    topBar: @Composable () -> Unit,
    contentSpacing: Dp = WooTheme.spacing.space5,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = topBar,
        containerColor = WooTheme.colors.background.section,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(WooTheme.padding.padding5),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
            content = content,
        )
    }
}

@Composable
private fun CatalogSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = WooTheme.colors.surface.default,
        contentColor = WooTheme.colors.surface.onDefault,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooTheme.padding.padding5),
            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
        ) {
            Text(
                text = title,
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.titleMedium.strong,
            )
            content()
        }
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
