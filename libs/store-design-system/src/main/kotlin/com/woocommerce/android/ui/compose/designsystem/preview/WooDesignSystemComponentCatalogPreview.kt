@file:Suppress("UnusedPrivateMember")

package com.woocommerce.android.ui.compose.designsystem.preview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooBadgeDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooButtonDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooCell
import com.woocommerce.android.ui.compose.designsystem.component.WooCellDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooCellTrailingAffordance
import com.woocommerce.android.ui.compose.designsystem.component.WooChoiceControlsDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooDividerDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooFilledButton
import com.woocommerce.android.ui.compose.designsystem.component.WooFilledTonalButton
import com.woocommerce.android.ui.compose.designsystem.component.WooIconButtonDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooIconContainerDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBanner
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBannerDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBannerTone
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedButton
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedIconButtonDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooPageHeader
import com.woocommerce.android.ui.compose.designsystem.component.WooPageHeaderDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooProgressIndicatorDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooSearchField
import com.woocommerce.android.ui.compose.designsystem.component.WooSearchFieldDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooSettingsRow
import com.woocommerce.android.ui.compose.designsystem.component.WooSettingsRowDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooSwitchDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooSwitchSettingsRow
import com.woocommerce.android.ui.compose.designsystem.component.WooTab
import com.woocommerce.android.ui.compose.designsystem.component.WooTabRow
import com.woocommerce.android.ui.compose.designsystem.component.WooTabsDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.designsystem.icons.AngleLeft
import com.woocommerce.android.ui.compose.designsystem.icons.ArrowUpRight
import com.woocommerce.android.ui.compose.designsystem.icons.CircleInfo
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

@PreviewLightDark
@Composable
private fun WooDesignSystemComponentCatalogPreview() {
    WooDesignSystemPreviewTheme {
        WooDesignSystemFlatComponentCatalogPreviewContent()
    }
}

@PreviewLightDark
@Composable
private fun WooDesignSystemComponentCatalogLegacyCompatiblePreview() {
    WooDesignSystemThemeWithBackground {
        WooDesignSystemFlatComponentCatalogPreviewContent()
    }
}

@Preview(name = "Large font", fontScale = 1.5f, showBackground = true)
@Composable
private fun WooDesignSystemComponentCatalogLargeFontPreview() {
    WooDesignSystemPreviewTheme {
        WooDesignSystemComponentCatalogScreen(
            initialPath = STRESS_LARGE_FONT_PATH,
            onBackClick = {},
            registerBackHandler = false,
        )
    }
}

@PreviewLightDark
@Composable
private fun WooDesignSystemComponentCatalogLongTextPreview() {
    WooDesignSystemPreviewTheme {
        Box(modifier = Modifier.width(320.dp)) {
            WooDesignSystemComponentCatalogScreen(
                initialPath = STRESS_LONG_TEXT_PATH,
                onBackClick = {},
                registerBackHandler = false,
            )
        }
    }
}

@Preview(name = "RTL", locale = "ar", showBackground = true)
@Composable
private fun WooDesignSystemComponentCatalogRtlPreview() {
    WooDesignSystemPreviewTheme {
        WooDesignSystemComponentCatalogScreen(
            initialPath = STRESS_RTL_PATH,
            onBackClick = {},
            registerBackHandler = false,
        )
    }
}

@Composable
fun WooDesignSystemComponentCatalogScreen(
    initialPath: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    registerBackHandler: Boolean = true,
) {
    val initialCatalogPath = initialPath.takeIf { CatalogRoot.find(it) != null } ?: ROOT_PATH
    var selectedPath by rememberSaveable { mutableStateOf(initialCatalogPath) }
    val selectedNode = CatalogRoot.find(selectedPath) ?: CatalogRoot

    fun navigateBack() {
        if (selectedNode.path == ROOT_PATH) {
            onBackClick()
        } else {
            selectedPath = selectedNode.parentPath
        }
    }

    if (registerBackHandler) {
        BackHandler(onBack = ::navigateBack)
    }

    val layoutDirection = if (selectedNode.path == STRESS_RTL_PATH) {
        LayoutDirection.Rtl
    } else {
        LocalLayoutDirection.current
    }
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        PreviewScreenScaffold(
            modifier = modifier,
            topBar = {
                WooTopAppBar(
                    title = selectedNode.title,
                    navigationIcon = WooIcons.Regular.AngleLeft,
                    navigationIconContentDescription = "Back",
                    onNavigationClick = ::navigateBack,
                    windowInsets = WindowInsets(0),
                )
            },
            contentSpacing = WooTheme.spacing.space6,
        ) {
            when (selectedNode) {
                is CatalogNode.Group -> CatalogGroupContent(
                    group = selectedNode,
                    onNodeClick = { selectedPath = it.path },
                )

                is CatalogNode.Leaf -> selectedNode.content(this)
            }
        }
    }
}

@Composable
private fun CatalogGroupContent(
    group: CatalogNode.Group,
    onNodeClick: (CatalogNode) -> Unit,
) {
    CatalogSection(group.sectionTitle) {
        group.description?.let {
            CatalogBodyText(
                text = it,
                modifier = Modifier.padding(horizontal = WooTheme.padding.padding5)
            )
        }
        group.children.forEach { child ->
            WooSettingsRow(
                title = child.title,
                description = child.description,
                onClick = { onNodeClick(child) },
                trailingContent = { WooCellTrailingAffordance() },
            )
        }
    }
}

@Composable
private fun WooDesignSystemFlatComponentCatalogPreviewContent() {
    PreviewScreenScaffold(
        topBar = {},
    ) {
        CatalogRoot.leaves().forEach { leaf ->
            leaf.content(this)
        }
    }
}

private sealed class CatalogNode {
    abstract val path: String
    abstract val title: String
    abstract val description: String?

    val parentPath: String
        get() = path.substringBeforeLast("/", missingDelimiterValue = ROOT_PATH)

    data class Group(
        override val path: String,
        override val title: String,
        override val description: String? = null,
        val sectionTitle: String = title,
        val children: List<CatalogNode>,
    ) : CatalogNode()

    data class Leaf(
        override val path: String,
        override val title: String,
        override val description: String? = null,
        val content: @Composable ColumnScope.() -> Unit,
    ) : CatalogNode()
}

private fun CatalogNode.find(path: String): CatalogNode? {
    if (this.path == path) return this
    return when (this) {
        is CatalogNode.Group -> children.firstNotNullOfOrNull { it.find(path) }
        is CatalogNode.Leaf -> null
    }
}

private fun CatalogNode.leaves(): List<CatalogNode.Leaf> = when (this) {
    is CatalogNode.Group -> children.flatMap { it.leaves() }
    is CatalogNode.Leaf -> listOf(this)
}

private val CatalogRoot = CatalogNode.Group(
    path = ROOT_PATH,
    title = "Store Design System",
    sectionTitle = "Catalog groups",
    description = "Browse production components, preview-only work, and stress cases.",
    children = listOf(
        CatalogNode.Group(
            path = "production",
            title = "Production components",
            description = "Components available for migrated Store Management screens.",
            children = listOf(
                CatalogNode.Leaf(
                    path = PRODUCTION_BUTTONS_PATH,
                    title = "Buttons",
                    description = "Filled, tonal, outlined, small, and disabled actions.",
                    content = { ProductionButtonsCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = "production/badges",
                    title = "Badges",
                    description = "Status tones and icon-leading variants.",
                    content = { ProductionBadgesCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = "production/choices",
                    title = "Choices",
                    description = "Checkboxes, radios, and filter chips.",
                    content = { ProductionChoicesCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = "production/rows-cells",
                    title = "Rows and cells",
                    description = "Headers, cells, settings rows, and switch rows.",
                    content = { ProductionRowsCellsCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = "production/notices",
                    title = "Notices",
                    description = "Success, warning, and neutral outlined banners.",
                    content = { ProductionNoticesCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = "production/search-tabs",
                    title = "Search and tabs",
                    description = "Search field states and top-level tabs.",
                    content = { ProductionSearchTabsCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = "production/switches-progress",
                    title = "Switches and progress",
                    description = "Switches, icon buttons, dividers, and indicators.",
                    content = { ProductionSwitchesProgressCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = "production/icon-containers",
                    title = "Icon containers",
                    description = "Decorative icon tone variants.",
                    content = { ProductionIconContainersCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = "production/xml-toolbar",
                    title = "XML toolbar",
                    description = "View toolbar bridge for XML-hosted screens.",
                    content = { ProductionXmlToolbarCatalogLeaf() },
                ),
            ),
        ),
        CatalogNode.Group(
            path = "preview",
            title = "Preview-only components",
            description = "Exploratory components that are not runtime-ready primitives.",
            children = listOf(
                CatalogNode.Leaf(
                    path = PREVIEW_SEGMENT_CONTROL_PATH,
                    title = "Segment control",
                    description = "Source in progress.",
                    content = { PreviewOnlySegmentControlCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = "preview/sheets",
                    title = "Sheets",
                    description = "Modal and navigation ownership.",
                    content = { PreviewOnlySheetsCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = "preview/bottom-tab-bar",
                    title = "Bottom tab bar",
                    description = "App shell and back stack ownership.",
                    content = { PreviewOnlyBottomTabBarCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = "preview/table",
                    title = "Table",
                    description = "Data, sizing, scrolling, selection, and semantics.",
                    content = { PreviewOnlyTableCatalogLeaf() },
                ),
            ),
        ),
        CatalogNode.Group(
            path = "stress",
            title = "Stress cases",
            description = "Layout and content stress checks for catalog components.",
            children = listOf(
                CatalogNode.Leaf(
                    path = STRESS_LARGE_FONT_PATH,
                    title = "Large font",
                    description = "Common controls at larger font scale.",
                    content = { SensitiveComponentPreviewContent() },
                ),
                CatalogNode.Leaf(
                    path = STRESS_LONG_TEXT_PATH,
                    title = "Long text",
                    description = "Wrapping and truncation behavior.",
                    content = { LongTextPreviewContent() },
                ),
                CatalogNode.Leaf(
                    path = STRESS_RTL_PATH,
                    title = "RTL",
                    description = "Layout direction behavior.",
                    content = { RtlPreviewContent() },
                ),
            ),
        ),
    ),
)

@Composable
private fun ProductionButtonsCatalogLeaf() {
    CatalogSection("Buttons") {
        WooButtonDemo(
            modifier = Modifier.padding(horizontal = WooTheme.padding.padding5)
        )
    }
}

@Composable
private fun ProductionBadgesCatalogLeaf() {
    CatalogSection("Badges") {
        WooBadgeDemo(
            modifier = Modifier.padding(horizontal = WooTheme.padding.padding5)
        )
    }
}

@Composable
private fun ProductionChoicesCatalogLeaf() {
    CatalogSection("Choices") {
        WooChoiceControlsDemo(
            modifier = Modifier.padding(horizontal = WooTheme.padding.padding5)
        )
    }
}

@Composable
private fun ProductionRowsCellsCatalogLeaf() {
    CatalogSection("Rows and cells") {
        PrivacyIntro(
            modifier = Modifier.padding(horizontal = WooTheme.padding.padding5)
        )
        WooPageHeaderDemo()
        WooCellDemo()
        WooSettingsRowDemo()
    }
}

@Composable
private fun ProductionNoticesCatalogLeaf() {
    CatalogSection("Notice banners") {
        WooNoticeBannerDemo(
            modifier = Modifier.padding(horizontal = WooTheme.padding.padding5)
        )
    }
}

@Composable
private fun ProductionSearchTabsCatalogLeaf() {
    CatalogSection("Search and tabs") {
        WooSearchFieldDemo()
        WooTabsDemo()
    }
}

@Composable
private fun ProductionSwitchesProgressCatalogLeaf() {
    CatalogSection("Switches, icons, dividers, and progress") {
        WooSwitchDemo(modifier = Modifier.padding(horizontal = WooTheme.padding.padding5))
        WooIconButtonDemo(modifier = Modifier.padding(horizontal = WooTheme.padding.padding5))
        WooOutlinedIconButtonDemo(modifier = Modifier.padding(horizontal = WooTheme.padding.padding5))
        WooDividerDemo(modifier = Modifier.padding(horizontal = WooTheme.padding.padding5))
        WooProgressIndicatorDemo(modifier = Modifier.padding(horizontal = WooTheme.padding.padding5))
    }
}

@Composable
private fun ProductionIconContainersCatalogLeaf() {
    CatalogSection("Icon containers") {
        WooIconContainerDemo(modifier = Modifier.padding(horizontal = WooTheme.padding.padding5))
    }
}

@Composable
private fun ProductionXmlToolbarCatalogLeaf() {
    CatalogSection("XML toolbar") {
        WooDesignSystemToolbarDemo(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PreviewOnlySegmentControlCatalogLeaf() {
    CatalogSection("Segment control") {
        PreviewOnlySegmentControlSample(modifier = Modifier.padding(horizontal = WooTheme.padding.padding5))
    }
}

@Composable
private fun PreviewOnlySheetsCatalogLeaf() {
    CatalogSection("Sheets") {
        PreviewOnlySheetSample(modifier = Modifier.padding(horizontal = WooTheme.padding.padding5))
    }
}

@Composable
private fun PreviewOnlyBottomTabBarCatalogLeaf() {
    CatalogSection("Bottom tab bar") {
        PreviewOnlyTabBarSample(modifier = Modifier.padding(horizontal = WooTheme.padding.padding5))
    }
}

@Composable
private fun PreviewOnlyTableCatalogLeaf() {
    CatalogSection("Table") {
        PreviewOnlyTableSample(modifier = Modifier.padding(horizontal = WooTheme.padding.padding5))
    }
}

@Composable
private fun SensitiveComponentPreviewContent() {
    PrivacyIntro(title = "Large font page title")
    WooPageHeader(title = "Products")
    WooFilledButton(
        text = "Filled action",
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
    )
    WooFilledTonalButton(
        text = "Filled tonal action",
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
    )
    WooOutlinedButton(
        text = "Outlined action",
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
    )
    WooCell(
        title = "Cell title wraps",
        description = "Cell body copy also wraps before the trailing affordance.",
        trailingContent = { WooCellTrailingAffordance() },
    )
    WooSettingsRow(
        title = "Settings row title",
        description = "Description wraps under the title and leaves space for trailing content.",
        trailingContent = { WooCellTrailingAffordance() },
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
        value = "",
        onValueChange = {},
        placeholder = "Search products",
        trailingActionText = "Cancel",
        onTrailingActionClick = {},
        modifier = Modifier.fillMaxWidth(),
    )
    WooTabRow(selectedTabIndex = 0) {
        WooTab(selected = true, onClick = {}, text = "Products")
        WooTab(selected = false, onClick = {}, text = "Orders")
    }
}

@Composable
private fun LongTextPreviewContent() {
    CatalogPageTitle("A long page title that wraps across multiple lines without clipping")
    CatalogBodyText(
        "Long body copy stays readable, wraps naturally, and avoids relying on a fixed one-line height.",
    )
    CatalogLinkText(
        text = "A long standalone link that wraps like body text",
    )
    WooFilledButton(
        text = "Long filled button label wraps",
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
                imageVector = WooIcons.Regular.ArrowUpRight,
                contentDescription = null,
                modifier = Modifier.size(WooTheme.iconSize.size18),
            )
        },
    )
}

@Composable
private fun RtlPreviewContent() {
    WooSettingsRow(
        title = "Row title",
        description = "Leading and trailing content follow layout direction.",
        leadingContent = {
            Icon(
                imageVector = WooIcons.Regular.CircleInfo,
                contentDescription = null,
            )
        },
        trailingContent = { WooCellTrailingAffordance() },
    )
    WooSearchField(
        value = "بحث",
        onValueChange = {},
        onClearClick = {},
        clearContentDescription = "Clear search",
        trailingActionText = "إلغاء",
        onTrailingActionClick = {},
        modifier = Modifier.fillMaxWidth(),
    )
    WooTabRow(selectedTabIndex = 0) {
        WooTab(selected = true, onClick = {}, text = "المنتجات")
        WooTab(selected = false, onClick = {}, text = "الطلبات")
    }
}

@Composable
private fun PrivacyIntro(
    modifier: Modifier = Modifier,
    title: String = "Privacy",
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2)
    ) {
        CatalogPageTitle(title)
        CatalogBodyText("Control how diagnostics and usage information help improve the app.")
        CatalogBodyText("Read the privacy policy before changing settings.")
    }
}

@Composable
private fun PreviewScreenScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    contentSpacing: Dp = WooTheme.spacing.space5,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
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
        shape = RoundedCornerShape(WooTheme.radius.medium),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = WooTheme.padding.padding5),
            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
        ) {
            Text(
                text = title,
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.titleMedium.strong,
                modifier = Modifier.padding(horizontal = WooTheme.padding.padding5)
            )
            content()
        }
    }
}

@Composable
private fun CatalogPageTitle(text: String) {
    Text(
        text = text,
        color = WooTheme.colors.background.onSection,
        style = WooTheme.text.headlineSmall.strong,
    )
}

@Composable
private fun CatalogBodyText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = WooTheme.colors.surface.onVariant,
        style = WooTheme.text.bodyMedium.regular,
        modifier = modifier
    )
}

@Composable
private fun CatalogLinkText(text: String) {
    Text(
        text = text,
        color = WooTheme.colors.primary,
        style = WooTheme.text.bodyMedium.emphasized,
        textDecoration = TextDecoration.Underline,
    )
}

private const val ROOT_PATH = ""
private const val PRODUCTION_BUTTONS_PATH = "production/buttons"
private const val PREVIEW_SEGMENT_CONTROL_PATH = "preview/segment-control"
private const val STRESS_LARGE_FONT_PATH = "stress/large-font"
private const val STRESS_LONG_TEXT_PATH = "stress/long-text"
private const val STRESS_RTL_PATH = "stress/rtl"
