package com.woocommerce.android.ui.moremenu

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooBadge
import com.woocommerce.android.ui.compose.designsystem.component.WooBadgeDefaults
import com.woocommerce.android.ui.compose.designsystem.component.WooCell
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.component.WooIconContainer
import com.woocommerce.android.ui.compose.designsystem.component.WooIconContainerTone
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.designsystem.icons.ArrowUpRight
import com.woocommerce.android.ui.compose.designsystem.icons.BadgePercent
import com.woocommerce.android.ui.compose.designsystem.icons.Bolt
import com.woocommerce.android.ui.compose.designsystem.icons.CaretDown
import com.woocommerce.android.ui.compose.designsystem.icons.CreditCard
import com.woocommerce.android.ui.compose.designsystem.icons.Gauge
import com.woocommerce.android.ui.compose.designsystem.icons.Gear
import com.woocommerce.android.ui.compose.designsystem.icons.Inbox
import com.woocommerce.android.ui.compose.designsystem.icons.Star
import com.woocommerce.android.ui.compose.designsystem.icons.Store
import com.woocommerce.android.ui.compose.designsystem.icons.UserGroup
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun MoreMenuScreen(viewModel: MoreMenuViewModel, scrollToTopTrigger: Flow<Unit>) {
    viewModel.moreMenuViewState.observeAsState().value?.let { moreMenuState ->
        MoreMenuScreen(
            state = moreMenuState,
            onSwitchStore = viewModel::onSwitchStoreClick,
            scrollToTopTrigger = scrollToTopTrigger,
        )
    }
}

@Composable
fun MoreMenuScreen(
    state: MoreMenuViewState,
    onSwitchStore: () -> Unit,
    scrollToTopTrigger: Flow<Unit> = emptyFlow(),
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollToTopTrigger) {
        scrollToTopTrigger.collect {
            scrollState.animateScrollTo(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WooTheme.colors.surface.default),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = MAX_OUTER_CONTENT_WIDTH)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = WooTheme.padding.padding7,
                    vertical = WooTheme.padding.padding7,
                )
                .testTag(MoreMenuTestTags.CONTENT),
            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space7),
        ) {
            MoreMenuHeader(
                state = state,
                onSwitchStore = onSwitchStore,
            )

            state.menuSections.forEach { section ->
                key(section.title) {
                    MoreMenuSection(section)
                }
            }
        }
    }
}

@Composable
private fun MoreMenuHeader(
    state: MoreMenuViewState,
    onSwitchStore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val switchStoreLabel = stringResource(R.string.more_menu_switch_store)
    val headerModifier = if (state.isStoreSwitcherEnabled) {
        Modifier.clickable(
            role = Role.Button,
            onClickLabel = switchStoreLabel,
            onClick = onSwitchStore,
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WooTheme.radius.extraLarge),
        color = WooTheme.colors.surface.bright,
        contentColor = WooTheme.colors.surface.onDefault,
    ) {
        Row(
            modifier = headerModifier
                .fillMaxWidth()
                .padding(WooTheme.padding.padding7)
                .semantics(mergeDescendants = true) {}
                .testTag(MoreMenuTestTags.STORE_HEADER),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderAvatar(avatarUrl = state.userAvatarUrl)
            Spacer(modifier = Modifier.width(WooTheme.spacing.space4))
            HeaderContent(
                siteName = state.siteName,
                planName = state.sitePlan,
                siteUrl = state.siteUrl,
                modifier = Modifier.weight(1f),
            )
            if (state.isStoreSwitcherEnabled) {
                Spacer(modifier = Modifier.width(WooTheme.spacing.space3))
                Icon(
                    imageVector = WooIcons.Regular.CaretDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(WooTheme.iconSize.size24)
                        .clearAndSetSemantics {}
                        .testTag(MoreMenuTestTags.HEADER_SWITCH_AFFORDANCE),
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun HeaderContent(
    siteName: String,
    planName: String,
    siteUrl: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space1),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2),
            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space1),
        ) {
            Text(
                text = siteName,
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.titleLarge.strong,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (planName.isNotEmpty()) {
                WooBadge(
                    text = planName.uppercase(),
                    colors = WooBadgeDefaults.colors(
                        containerColor = WooTheme.colors.container.primaryContainer,
                        contentColor = WooTheme.colors.container.onPrimaryContainer,
                    ),
                )
            }
        }
        Text(
            text = siteUrl,
            color = WooTheme.colors.surface.onVariant,
            style = WooTheme.text.bodySmall.regular,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HeaderAvatar(
    avatarUrl: String,
    modifier: Modifier = Modifier,
) {
    val contentDescription = stringResource(R.string.more_menu_avatar)
    val avatarModifier = modifier
        .size(AVATAR_SIZE)
        .clip(RoundedCornerShape(WooTheme.radius.medium))

    if (avatarUrl.isBlank()) {
        Image(
            painter = painterResource(R.drawable.img_gravatar_placeholder),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = avatarModifier,
        )
        return
    }

    val context = LocalContext.current
    val imageRequest = remember(context, avatarUrl) {
        ImageRequest.Builder(context)
            .data(avatarUrl)
            .crossfade(true)
            .placeholder(R.drawable.img_gravatar_placeholder)
            .fallback(R.drawable.img_gravatar_placeholder)
            .error(R.drawable.img_gravatar_placeholder)
            .build()
    }

    key(avatarUrl) {
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = avatarModifier,
        )
    }
}

@Composable
private fun MoreMenuSection(section: MoreMenuItemSection) {
    val renderedItems = section.items.filter { it.state != MoreMenuItemButton.State.Hidden }
    if (!section.isVisible || renderedItems.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
    ) {
        section.title?.let { title ->
            Text(
                text = stringResource(title),
                color = WooTheme.colors.surface.onVariant,
                style = WooTheme.text.bodyLarge.emphasized,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() }
                    .testTag(MoreMenuTestTags.section(title)),
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    collectionInfo = CollectionInfo(rowCount = renderedItems.size, columnCount = 1)
                    isTraversalGroup = true
                }
                .testTag(MoreMenuTestTags.collection(section.title)),
            shape = RoundedCornerShape(WooTheme.radius.extraLarge),
            color = WooTheme.colors.surface.bright,
            contentColor = WooTheme.colors.surface.onDefault,
        ) {
            Column {
                renderedItems.forEachIndexed { index, item ->
                    key(item.title) {
                        when (item.state) {
                            MoreMenuItemButton.State.Loading -> MoreMenuLoadingCell(
                                item = item,
                                modifier = Modifier.collectionItem(index),
                            )
                            MoreMenuItemButton.State.Visible -> MoreMenuCell(
                                item = item,
                                modifier = Modifier.collectionItem(index),
                            )
                            MoreMenuItemButton.State.Hidden -> Unit
                        }
                        if (index < renderedItems.lastIndex) {
                            WooDivider(
                                modifier = Modifier
                                    .padding(horizontal = WooTheme.padding.padding7)
                                    .testTag(MoreMenuTestTags.divider(section.title, index)),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.collectionItem(index: Int) = semantics {
    collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
    traversalIndex = index.toFloat()
}

@Composable
private fun MoreMenuCell(
    item: MoreMenuItemButton,
    modifier: Modifier = Modifier,
) {
    val badgeStateDescription = item.badgeStateDescription()
    WooCell(
        title = stringResource(item.title),
        description = stringResource(item.description),
        onClick = item.onClick,
        modifier = modifier
            .testTag(MoreMenuTestTags.item(item.title))
            .semantics {
                badgeStateDescription?.let { stateDescription = it }
            },
        leadingContent = { MoreMenuIcon(item.icon) },
        trailingContent = if (item.badgeState != null || item.extraIcon != null) {
            { MoreMenuTrailingContent(item) }
        } else {
            null
        },
    )
}

@Composable
private fun MoreMenuTrailingContent(item: MoreMenuItemButton) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoreMenuBadge(item.badgeState)
        if (item.extraIcon == R.drawable.ic_external) {
            Icon(
                imageVector = WooIcons.Regular.ArrowUpRight,
                contentDescription = null,
                modifier = Modifier
                    .size(WooTheme.iconSize.size18)
                    .clearAndSetSemantics {},
            )
        }
    }
}

@Composable
private fun MoreMenuLoadingCell(
    item: MoreMenuItemButton,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .testTag(MoreMenuTestTags.loadingItem(item.title))
            .fillMaxWidth()
            .heightIn(min = MINIMUM_TOUCH_TARGET_SIZE)
            .background(WooTheme.colors.surface.bright)
            .padding(WooTheme.padding.padding7),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonView(
            modifier = Modifier
                .testTag(MoreMenuTestTags.loadingIcon(item.title))
                .size(ICON_CONTAINER_SIZE)
                .clip(RoundedCornerShape(WooTheme.radius.medium)),
        )
        Spacer(modifier = Modifier.width(WooTheme.spacing.space5))
        Column(
            modifier = Modifier
                .weight(1f)
                .testTag(MoreMenuTestTags.loadingContent(item.title)),
            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2),
        ) {
            SkeletonView(
                modifier = Modifier
                    .fillMaxWidth(LOADING_TITLE_PLACEHOLDER_FRACTION)
                    .height(WooTheme.spacing.space5)
                    .clip(RoundedCornerShape(WooTheme.radius.small)),
            )
            SkeletonView(
                modifier = Modifier
                    .fillMaxWidth(LOADING_DESCRIPTION_PLACEHOLDER_FRACTION)
                    .height(WooTheme.spacing.space4)
                    .clip(RoundedCornerShape(WooTheme.radius.small)),
            )
        }
    }
}

@Composable
private fun MoreMenuIcon(
    icon: Int,
    modifier: Modifier = Modifier,
) {
    when (icon) {
        R.drawable.ic_more_screen_settings -> DesignSystemIcon(
            icon = WooIcons.Regular.Gear,
            tone = WooIconContainerTone.Green,
            modifier = modifier,
        )
        R.drawable.ic_more_menu_upgrades -> DesignSystemIcon(WooIcons.Regular.Bolt, modifier = modifier)
        R.drawable.ic_more_menu_payments -> DesignSystemIcon(WooIcons.Regular.CreditCard, modifier = modifier)
        R.drawable.ic_more_menu_wp_admin -> DesignSystemIcon(
            icon = WooIcons.Regular.Gauge,
            tone = WooIconContainerTone.Sandstone,
            modifier = modifier,
        )
        R.drawable.ic_more_menu_store -> DesignSystemIcon(
            icon = WooIcons.Regular.Store,
            tone = WooIconContainerTone.Sandstone,
            modifier = modifier,
        )
        R.drawable.ic_more_menu_coupons -> DesignSystemIcon(WooIcons.Regular.BadgePercent, modifier = modifier)
        R.drawable.ic_more_menu_reviews -> DesignSystemIcon(WooIcons.Regular.Star, modifier = modifier)
        R.drawable.icon_multiple_users -> DesignSystemIcon(WooIcons.Regular.UserGroup, modifier = modifier)
        R.drawable.ic_more_menu_inbox -> DesignSystemIcon(WooIcons.Regular.Inbox, modifier = modifier)
        else -> BrandIconContainer(drawable = icon, modifier = modifier)
    }
}

@Composable
private fun DesignSystemIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    tone: WooIconContainerTone = WooIconContainerTone.Purple,
) {
    WooIconContainer(
        imageVector = icon,
        tone = tone,
        contentDescription = null,
        modifier = modifier.clearAndSetSemantics {},
    )
}

@Composable
private fun BrandIconContainer(
    drawable: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .size(ICON_CONTAINER_SIZE)
            .clearAndSetSemantics {},
        shape = RoundedCornerShape(WooTheme.radius.medium),
        color = WooTheme.colors.background.sectionVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(drawable),
                contentDescription = null,
                modifier = Modifier.size(WooTheme.iconSize.size24),
            )
        }
    }
}

@Composable
private fun MoreMenuBadge(badgeState: BadgeState?) {
    when {
        badgeState == null -> Unit
        badgeState.animateAppearance -> PaymentNewFeatureDot()
        badgeState.textState.text.isNotEmpty() -> WooBadge(
            text = badgeState.textState.text,
            colors = WooBadgeDefaults.colors(
                containerColor = WooTheme.colors.primary,
                contentColor = WooTheme.colors.onPrimary,
            ),
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

@Composable
private fun PaymentNewFeatureDot() {
    Box(
        modifier = Modifier
            .size(WooTheme.iconSize.size24)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        val visible = remember {
            MutableTransitionState(false).apply { targetState = true }
        }
        AnimatedVisibility(
            visibleState = visible,
            enter = createBadgeEnterAnimation(),
        ) {
            Box(
                modifier = Modifier
                    .size(WooTheme.spacing.space3)
                    .background(WooTheme.colors.secondary, CircleShape),
            )
        }
    }
}

@Composable
private fun MoreMenuItemButton.badgeStateDescription(): String? = when {
    badgeState == null -> null
    badgeState.animateAppearance -> stringResource(R.string.more_menu_payments_new_feature_state_description)
    badgeState.textState.text.toIntOrNull() != null -> {
        val count = badgeState.textState.text.toInt()
        pluralStringResource(
            R.plurals.more_menu_unseen_reviews_state_description,
            count,
            count,
        )
    }
    else -> null
}

private fun createBadgeEnterAnimation(): EnterTransition {
    val animationSpec = TweenSpec<Float>(
        durationMillis = BADGE_ANIMATION_DURATION_MILLIS,
        delay = BADGE_ANIMATION_DELAY,
    )
    return scaleIn(animationSpec = animationSpec) + fadeIn(animationSpec = animationSpec)
}

object MoreMenuTestTags {
    const val CONTENT = "more_menu_content"
    const val STORE_HEADER = "more_menu_store_header"
    const val HEADER_SWITCH_AFFORDANCE = "more_menu_header_switch_affordance"
    const val SETTINGS_SECTION = "more_menu_section_settings"
    const val GENERAL_SECTION = "more_menu_section_general"
    const val SETTINGS_COLLECTION = "more_menu_collection_settings"
    const val GENERAL_COLLECTION = "more_menu_collection_general"

    const val PAYMENTS_ITEM = "more_menu_item_payments"
    const val COUPONS_ITEM = "more_menu_item_coupons"
    const val REVIEWS_ITEM = "more_menu_item_reviews"
    const val CUSTOMERS_ITEM = "more_menu_item_customers"
    const val SETTINGS_ITEM = "more_menu_item_settings"
    const val WC_ADMIN_ITEM = "more_menu_item_wc_admin"
    const val VIEW_STORE_ITEM = "more_menu_item_view_store"
    const val SUBSCRIPTIONS_ITEM = "more_menu_item_subscriptions"
    const val GOOGLE_ITEM = "more_menu_item_google"
    const val BLAZE_ITEM = "more_menu_item_blaze"
    const val INBOX_ITEM = "more_menu_item_inbox"

    fun section(title: Int) = when (title) {
        R.string.more_menu_settings_section_title -> SETTINGS_SECTION
        R.string.more_menu_general_section_title -> GENERAL_SECTION
        else -> "more_menu_section_$title"
    }

    fun collection(title: Int?) = when (title) {
        R.string.more_menu_settings_section_title -> SETTINGS_COLLECTION
        R.string.more_menu_general_section_title -> GENERAL_COLLECTION
        else -> "more_menu_collection_$title"
    }

    fun divider(sectionTitle: Int?, itemIndex: Int) = "${collection(sectionTitle)}_divider_$itemIndex"

    fun item(title: Int) = when (title) {
        R.string.more_menu_button_payments -> PAYMENTS_ITEM
        R.string.more_menu_button_coupons -> COUPONS_ITEM
        R.string.more_menu_button_reviews -> REVIEWS_ITEM
        R.string.more_menu_button_customers -> CUSTOMERS_ITEM
        R.string.more_menu_button_settings -> SETTINGS_ITEM
        R.string.more_menu_button_wс_admin -> WC_ADMIN_ITEM
        R.string.more_menu_button_store -> VIEW_STORE_ITEM
        R.string.more_menu_button_subscriptions -> SUBSCRIPTIONS_ITEM
        R.string.more_menu_button_google -> GOOGLE_ITEM
        R.string.more_menu_button_blaze -> BLAZE_ITEM
        R.string.more_menu_button_inbox -> INBOX_ITEM
        else -> "more_menu_item_$title"
    }

    fun loadingItem(title: Int) = "${item(title)}_loading"

    fun loadingIcon(title: Int) = "${loadingItem(title)}_icon"

    fun loadingContent(title: Int) = "${loadingItem(title)}_content"
}

@Preview(name = "430dp light", widthDp = 430, heightDp = 932, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "430dp dark", widthDp = 430, heightDp = 932, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun MoreMenuCompactPreview() {
    MoreMenuPreviewRoot(previewMoreMenuState())
}

@Preview(name = "320dp 2x font", widthDp = 320, heightDp = 932, fontScale = 2f)
@Composable
private fun MoreMenuNarrowLargeFontPreview() {
    MoreMenuPreviewRoot(previewMoreMenuState())
}

@Preview(name = "Arabic RTL", widthDp = 430, heightDp = 932, locale = "ar")
@Composable
private fun MoreMenuRtlPreview() {
    MoreMenuPreviewRoot(
        previewMoreMenuState(
            siteName = "متجر القهوة والكتب",
            siteUrl = "example.com",
        ),
    )
}

@Preview(name = "Expanded tablet", widthDp = 840, heightDp = 900)
@Composable
private fun MoreMenuExpandedPreview() {
    MoreMenuPreviewRoot(previewMoreMenuState())
}

@Preview(name = "Loading hidden disabled", widthDp = 430, heightDp = 932)
@Composable
private fun MoreMenuLoadingAndHiddenPreview() {
    val state = previewMoreMenuState(isStoreSwitcherEnabled = false)
    MoreMenuPreviewRoot(
        state.copy(
            menuSections = state.menuSections.map { section ->
                section.copy(
                    items = section.items.map { item ->
                        when (item.title) {
                            R.string.more_menu_button_subscriptions,
                            R.string.more_menu_button_google -> item.copy(state = MoreMenuItemButton.State.Loading)
                            R.string.more_menu_button_blaze,
                            R.string.more_menu_button_inbox -> item.copy(state = MoreMenuItemButton.State.Hidden)
                            else -> item
                        }
                    },
                )
            },
        ),
    )
}

@Composable
private fun MoreMenuPreviewRoot(state: MoreMenuViewState) {
    WooDesignSystemThemeWithBackground {
        MoreMenuScreen(state = state, onSwitchStore = {})
    }
}

private fun previewMoreMenuState(
    siteName: String = "Mug & Hug",
    siteUrl: String = "mugandhugstore.com",
    sitePlan: String = "Free Trial",
    isStoreSwitcherEnabled: Boolean = true,
) = MoreMenuViewState(
    menuSections = previewMenuSections(),
    siteName = siteName,
    siteUrl = siteUrl,
    sitePlan = sitePlan,
    userAvatarUrl = "",
    isStoreSwitcherEnabled = isStoreSwitcherEnabled,
)

@Suppress("LongMethod")
private fun previewMenuSections() = listOf(
    MoreMenuItemSection(
        title = R.string.more_menu_settings_section_title,
        items = listOf(
            previewItem(
                title = R.string.more_menu_button_settings,
                description = R.string.more_menu_button_settings_description,
                icon = R.drawable.ic_more_screen_settings,
            ),
            previewItem(
                title = R.string.more_menu_button_subscriptions,
                description = R.string.more_menu_button_subscriptions_description,
                icon = R.drawable.ic_more_menu_upgrades,
            ),
        ),
    ),
    MoreMenuItemSection(
        title = R.string.more_menu_general_section_title,
        items = listOf(
            previewItem(
                title = R.string.more_menu_button_payments,
                description = R.string.more_menu_button_payments_description,
                icon = R.drawable.ic_more_menu_payments,
                badgeState = previewPaymentBadge(),
            ),
            previewItem(
                title = R.string.more_menu_button_google,
                description = R.string.more_menu_button_google_description,
                icon = R.drawable.google_logo,
            ),
            previewItem(
                title = R.string.more_menu_button_blaze,
                description = R.string.more_menu_button_blaze_description,
                icon = R.drawable.ic_blaze,
            ),
            previewItem(
                title = R.string.more_menu_button_wс_admin,
                description = R.string.more_menu_button_wc_admin_description,
                icon = R.drawable.ic_more_menu_wp_admin,
                extraIcon = R.drawable.ic_external,
            ),
            previewItem(
                title = R.string.more_menu_button_store,
                description = R.string.more_menu_button_store_description,
                icon = R.drawable.ic_more_menu_store,
                extraIcon = R.drawable.ic_external,
            ),
            previewItem(
                title = R.string.more_menu_button_coupons,
                description = R.string.more_menu_button_coupons_description,
                icon = R.drawable.ic_more_menu_coupons,
            ),
            previewItem(
                title = R.string.more_menu_button_reviews,
                description = R.string.more_menu_button_reviews_description,
                icon = R.drawable.ic_more_menu_reviews,
                badgeState = previewReviewBadge(PREVIEW_REVIEW_COUNT),
            ),
            previewItem(
                title = R.string.more_menu_button_customers,
                description = R.string.more_menu_button_customers_description,
                icon = R.drawable.icon_multiple_users,
            ),
            previewItem(
                title = R.string.more_menu_button_inbox,
                description = R.string.more_menu_button_inbox_description,
                icon = R.drawable.ic_more_menu_inbox,
            ),
        ),
    ),
)

private fun previewItem(
    title: Int,
    description: Int,
    icon: Int,
    extraIcon: Int? = null,
    badgeState: BadgeState? = null,
) = MoreMenuItemButton(
    title = title,
    description = description,
    icon = icon,
    extraIcon = extraIcon,
    badgeState = badgeState,
)

private fun previewPaymentBadge() = BadgeState(
    badgeSize = R.dimen.major_110,
    backgroundColor = R.color.color_secondary,
    textColor = R.color.color_on_surface,
    textState = TextState("", R.dimen.text_minor_80),
    animateAppearance = true,
)

private fun previewReviewBadge(count: Int) = BadgeState(
    badgeSize = R.dimen.major_150,
    backgroundColor = R.color.color_primary,
    textColor = R.color.color_on_primary,
    textState = TextState(count.toString(), R.dimen.text_minor_80),
)

private val MAX_OUTER_CONTENT_WIDTH = 648.dp
private val AVATAR_SIZE = 44.dp
private val ICON_CONTAINER_SIZE = 44.dp
private val MINIMUM_TOUCH_TARGET_SIZE = 48.dp
private const val LOADING_TITLE_PLACEHOLDER_FRACTION = 0.4f
private const val LOADING_DESCRIPTION_PLACEHOLDER_FRACTION = 0.7f
private const val BADGE_ANIMATION_DURATION_MILLIS = 400
private const val BADGE_ANIMATION_DELAY = 200
private const val PREVIEW_REVIEW_COUNT = 128
