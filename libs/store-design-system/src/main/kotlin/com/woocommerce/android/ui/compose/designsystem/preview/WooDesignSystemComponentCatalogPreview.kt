@file:Suppress("UnusedPrivateMember")

package com.woocommerce.android.ui.compose.designsystem.preview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooBadgeDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooButtonDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooCellDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooChoiceControlsDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooDividerDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooIconButton
import com.woocommerce.android.ui.compose.designsystem.component.WooIconButtonDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooIconContainerDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBannerDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedIconButtonDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooProgressIndicatorDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooSearchFieldDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooSectionHeader
import com.woocommerce.android.ui.compose.designsystem.component.WooSettingsRowDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooSwitchDemo
import com.woocommerce.android.ui.compose.designsystem.component.WooTabsDemo
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.designsystem.icons.AngleLeft
import com.woocommerce.android.ui.compose.designsystem.icons.ArrowUpRight
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

@Preview(name = "Catalog navigation", showBackground = true)
@Composable
private fun WooDesignSystemComponentCatalogNavigationPreview() {
    WooDesignSystemPreviewTheme {
        Box(modifier = Modifier.width(360.dp)) {
            WooDesignSystemComponentCatalogScreen(
                initialPath = ROOT_PATH,
                onBackClick = {},
            )
        }
    }
}

@Composable
fun WooDesignSystemComponentCatalogScreen(
    initialPath: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
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

    PreviewScreenScaffold(
        modifier = modifier,
        title = selectedNode.title,
        onBackClick = ::navigateBack,
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

@Composable
private fun CatalogGroupContent(
    group: CatalogNode.Group,
    onNodeClick: (CatalogNode) -> Unit,
) {
    CatalogSection(group.sectionTitle) {
        group.description?.let {
            CatalogBodyText(
                text = it,
                modifier = Modifier.padding(horizontal = WooTheme.padding.padding5),
            )
        }
        group.children.forEach { child ->
            CatalogNavigationRow(
                title = child.title,
                description = child.description,
                onClick = { onNodeClick(child) },
            )
        }
    }
}

@Composable
private fun CatalogNavigationRow(
    title: String,
    description: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = WooTheme.padding.padding5),
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space1),
        ) {
            Text(
                text = title,
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.bodyLarge.emphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            description?.let {
                Text(
                    text = it,
                    color = WooTheme.colors.surface.onVariant,
                    style = WooTheme.text.bodySmall.regular,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = WooIcons.Regular.ArrowUpRight,
            contentDescription = null,
            modifier = Modifier.size(WooTheme.iconSize.size18),
            tint = WooTheme.colors.surface.onVariant,
        )
    }
}

@Composable
private fun WooDesignSystemFlatComponentCatalogPreviewContent() {
    PreviewScreenScaffold(
        title = "Store Design System",
        onBackClick = {},
        showNavigationIcon = false,
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
    description = "Browse production components available in this slice.",
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
                    path = PRODUCTION_ICON_ACTIONS_PATH,
                    title = "Icon actions",
                    description = "Plain and outlined icon actions with neutral and primary emphasis.",
                    content = { ProductionIconActionsCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = PRODUCTION_BADGES_PATH,
                    title = "Badges",
                    description = "Status tones and icon-leading variants.",
                    content = { ProductionBadgesCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = PRODUCTION_NOTICE_BANNERS_PATH,
                    title = "Notice banners",
                    description = "Static status and information banners.",
                    content = { ProductionNoticeBannersCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = PRODUCTION_ICON_CONTAINERS_PATH,
                    title = "Icon containers",
                    description = "Decorative icon tone variants.",
                    content = { ProductionIconContainersCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = PRODUCTION_CHOICE_CONTROLS_PATH,
                    title = "Choice controls",
                    description = "Checkboxes, radio buttons, and filter chips.",
                    content = { ProductionChoiceControlsCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = PRODUCTION_SEARCH_PATH,
                    title = "Search",
                    description = "Search field states with clear and trailing actions.",
                    content = { ProductionSearchCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = PRODUCTION_TABS_PATH,
                    title = "Tabs",
                    description = "Icon and text tab row variants.",
                    content = { ProductionTabsCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = PRODUCTION_SECTION_HEADERS_PATH,
                    title = "Section headers",
                    description = "Semantically marked headers for grouped content.",
                    content = { ProductionSectionHeadersCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = PRODUCTION_ROWS_CELLS_PATH,
                    title = "Rows and cells",
                    description = "Cells, settings rows, switches, and switch rows.",
                    content = { ProductionRowsCellsCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = PRODUCTION_DIVIDERS_PATH,
                    title = "Dividers",
                    description = "Horizontal and vertical separators.",
                    content = { ProductionDividersCatalogLeaf() },
                ),
                CatalogNode.Leaf(
                    path = PRODUCTION_PROGRESS_INDICATORS_PATH,
                    title = "Progress indicators",
                    description = "Determinate and indeterminate loading indicators.",
                    content = { ProductionProgressIndicatorsCatalogLeaf() },
                ),
            ),
        ),
    ),
)

@Composable
private fun ProductionButtonsCatalogLeaf() {
    CatalogSection("Buttons") {
        WooButtonDemo(
            modifier = Modifier.padding(horizontal = WooTheme.padding.padding5),
        )
    }
}

@Composable
private fun ProductionIconActionsCatalogLeaf() {
    CatalogSection("Icon buttons") {
        WooIconButtonDemo(modifier = Modifier.padding(horizontal = WooTheme.padding.padding5))
        WooOutlinedIconButtonDemo(modifier = Modifier.padding(horizontal = WooTheme.padding.padding5))
    }
}

@Composable
private fun ProductionBadgesCatalogLeaf() {
    CatalogSection("Badges") {
        WooBadgeDemo(
            modifier = Modifier.padding(horizontal = WooTheme.padding.padding5),
        )
    }
}

@Composable
private fun ProductionNoticeBannersCatalogLeaf() {
    CatalogSection("Notice banners") {
        WooNoticeBannerDemo(
            modifier = Modifier.padding(horizontal = WooTheme.padding.padding5),
        )
    }
}

@Composable
private fun ProductionIconContainersCatalogLeaf() {
    CatalogSection("Icon containers") {
        WooIconContainerDemo(
            modifier = Modifier.padding(horizontal = WooTheme.padding.padding5),
        )
    }
}

@Composable
private fun ProductionChoiceControlsCatalogLeaf() {
    CatalogSection("Choice controls") {
        WooChoiceControlsDemo(
            modifier = Modifier.padding(horizontal = WooTheme.padding.padding5),
        )
    }
}

@Composable
private fun ProductionSearchCatalogLeaf() {
    CatalogSection("Search") {
        WooSearchFieldDemo()
    }
}

@Composable
private fun ProductionTabsCatalogLeaf() {
    CatalogSection("Tabs") {
        WooTabsDemo()
    }
}

@Composable
private fun ProductionSectionHeadersCatalogLeaf() {
    CatalogSection("Section headers") {
        WooSectionHeader("Tracking")
        WooSectionHeader("Orders")
    }
}

@Composable
private fun ProductionRowsCellsCatalogLeaf() {
    CatalogSection("Rows and cells") {
        WooCellDemo()
        WooSwitchDemo(modifier = Modifier.padding(horizontal = WooTheme.padding.padding5))
        WooSettingsRowDemo()
    }
}

@Composable
private fun ProductionDividersCatalogLeaf() {
    CatalogSection("Dividers") {
        WooDividerDemo(
            modifier = Modifier.padding(horizontal = WooTheme.padding.padding5),
        )
    }
}

@Composable
private fun ProductionProgressIndicatorsCatalogLeaf() {
    CatalogSection("Progress indicators") {
        WooProgressIndicatorDemo(
            modifier = Modifier.padding(horizontal = WooTheme.padding.padding5),
        )
    }
}

@Composable
private fun PreviewScreenScaffold(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    showNavigationIcon: Boolean = true,
    contentSpacing: Dp = WooTheme.spacing.space5,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CatalogTopBar(
                title = title,
                onBackClick = onBackClick,
                showNavigationIcon = showNavigationIcon,
            )
        },
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
private fun CatalogTopBar(
    title: String,
    onBackClick: () -> Unit,
    showNavigationIcon: Boolean,
) {
    Surface(
        color = WooTheme.colors.surface.default,
        contentColor = WooTheme.colors.surface.onDefault,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = WooTheme.padding.padding2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showNavigationIcon) {
                WooIconButton(
                    imageVector = WooIcons.Regular.AngleLeft,
                    contentDescription = "Back",
                    onClick = onBackClick,
                )
            } else {
                Box(modifier = Modifier.size(48.dp))
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.bodyLarge.strong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(modifier = Modifier.size(48.dp))
        }
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
                modifier = Modifier.padding(horizontal = WooTheme.padding.padding5),
            )
            content()
        }
    }
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
        modifier = modifier,
    )
}

private const val ROOT_PATH = ""
private const val PRODUCTION_BUTTONS_PATH = "production/buttons"
private const val PRODUCTION_ICON_ACTIONS_PATH = "production/icon-actions"
private const val PRODUCTION_BADGES_PATH = "production/badges"
private const val PRODUCTION_NOTICE_BANNERS_PATH = "production/notice-banners"
private const val PRODUCTION_ICON_CONTAINERS_PATH = "production/icon-containers"
private const val PRODUCTION_CHOICE_CONTROLS_PATH = "production/choice-controls"
private const val PRODUCTION_SEARCH_PATH = "production/search"
private const val PRODUCTION_TABS_PATH = "production/tabs"
private const val PRODUCTION_SECTION_HEADERS_PATH = "production/section-headers"
private const val PRODUCTION_ROWS_CELLS_PATH = "production/rows-cells"
private const val PRODUCTION_DIVIDERS_PATH = "production/dividers"
private const val PRODUCTION_PROGRESS_INDICATORS_PATH = "production/progress-indicators"
