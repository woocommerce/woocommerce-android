package com.woocommerce.android.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.blaze.creation.BlazeCampaignCreationDispatcher
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooButtonSize
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.component.WooFilledButton
import com.woocommerce.android.ui.compose.designsystem.component.WooFilledTonalButton
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedButton
import com.woocommerce.android.ui.compose.designsystem.component.WooPageHeaderDefaults
import com.woocommerce.android.ui.compose.designsystem.component.WooPageHeaderScrollBehavior
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.designsystem.icons.Pen
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.OpenRangePicker
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetUiModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetUiModel.ConfigurableWidget
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetUiModel.FeedbackWidget
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetUiModel.NewWidgetsCard
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetUiModel.ShareStoreWidget
import com.woocommerce.android.ui.dashboard.aiassistant.DashboardAIAssistantCard
import com.woocommerce.android.ui.dashboard.blaze.DashboardBlazeCard
import com.woocommerce.android.ui.dashboard.coupons.DashboardCouponsCard
import com.woocommerce.android.ui.dashboard.google.DashboardGoogleAdsCard
import com.woocommerce.android.ui.dashboard.inbox.DashboardInboxCard
import com.woocommerce.android.ui.dashboard.onboarding.DashboardOnboardingCard
import com.woocommerce.android.ui.dashboard.orders.DashboardOrdersCard
import com.woocommerce.android.ui.dashboard.orders.DashboardOrdersViewModel.ViewState.OrderItem
import com.woocommerce.android.ui.dashboard.orders.TopOrders
import com.woocommerce.android.ui.dashboard.pushnotifications.DashboardPushNotificationsCard
import com.woocommerce.android.ui.dashboard.reviews.DashboardReviewsCard
import com.woocommerce.android.ui.dashboard.stats.DashboardStatsCard
import com.woocommerce.android.ui.dashboard.stock.DashboardProductStockCard
import com.woocommerce.android.ui.dashboard.stock.StockEmptyView
import com.woocommerce.android.ui.dashboard.topperformers.DashboardTopPerformersWidgetCard
import com.woocommerce.android.ui.dashboard.topperformers.TopPerformerSkeletonItem
import com.woocommerce.android.ui.main.MainActivityViewModel
import com.woocommerce.android.ui.orders.filters.data.OrderStatusOption
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

private const val SCROLL_INTERACTION_DEBOUNCE_MS = 1000L

@Composable
internal fun DashboardWidgets(
    mainActivityViewModel: MainActivityViewModel,
    dashboardViewModel: DashboardViewModel,
    blazeCampaignCreationDispatcher: BlazeCampaignCreationDispatcher,
    scrollToTopTrigger: Flow<Unit>,
    scrollBehavior: WooPageHeaderScrollBehavior,
    contentBeforeWidgets: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasNewWidgets = dashboardViewModel.hasNewWidgets.observeAsState(false).value
    val state = dashboardViewModel.dashboardCardsState.observeAsState().value
    DashboardLayout(
        widgets = state?.widgets.orEmpty(),
        isRefreshing = state?.isRefreshing == true,
        onPullToRefresh = dashboardViewModel::onPullToRefresh,
        scrollToTopTrigger = scrollToTopTrigger,
        onDashboardInteracted = dashboardViewModel::onDashboardInteracted,
        hasNewWidgets = hasNewWidgets,
        showCustomizeButton = state != null,
        onEditWidgetsClicked = dashboardViewModel::onEditWidgetsClicked,
        scrollBehavior = scrollBehavior,
        contentBeforeWidgets = contentBeforeWidgets,
        modifier = modifier,
    ) { widget, modifier ->
        DashboardWidgetCard(
            widget = widget,
            mainActivityViewModel = mainActivityViewModel,
            dashboardViewModel = dashboardViewModel,
            blazeCampaignCreationDispatcher = blazeCampaignCreationDispatcher,
            modifier = modifier,
        )
    }
}

@Composable
private fun DashboardLayout(
    widgets: List<DashboardWidgetUiModel>,
    isRefreshing: Boolean,
    onPullToRefresh: () -> Unit,
    scrollToTopTrigger: Flow<Unit>,
    onDashboardInteracted: () -> Unit,
    hasNewWidgets: Boolean,
    showCustomizeButton: Boolean,
    onEditWidgetsClicked: () -> Unit,
    scrollBehavior: WooPageHeaderScrollBehavior,
    contentBeforeWidgets: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    widgetContent: @Composable (DashboardWidgetUiModel, Modifier) -> Unit,
) {
    val pullRefreshState = rememberPullToRefreshState()
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val numberOfColumns = calculateColumnNumber(
            availableWidthInDp = maxWidth - WooTheme.padding.padding7 * 2,
            visibleWidgetsCount = widgets.count { widget -> widget.isVisible },
        )
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onPullToRefresh,
            state = pullRefreshState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = WooTheme.colors.surface.default,
                    color = WooTheme.colors.primary,
                )
            },
        ) {
            DashboardWidgetLayout(
                widgetUiModels = widgets,
                scrollToTopTrigger = scrollToTopTrigger,
                onDashboardInteracted = onDashboardInteracted,
                hasNewWidgets = hasNewWidgets,
                showCustomizeButton = showCustomizeButton,
                onEditWidgetsClicked = onEditWidgetsClicked,
                widgetContent = widgetContent,
                modifier = Modifier.fillMaxSize(),
                numberOfColumns = numberOfColumns,
                scrollBehavior = scrollBehavior,
                contentBeforeWidgets = contentBeforeWidgets,
            )
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun DashboardWidgetLayout(
    widgetUiModels: List<DashboardWidgetUiModel>,
    scrollToTopTrigger: Flow<Unit>,
    onDashboardInteracted: () -> Unit,
    hasNewWidgets: Boolean,
    showCustomizeButton: Boolean,
    onEditWidgetsClicked: () -> Unit,
    widgetContent: @Composable (DashboardWidgetUiModel, Modifier) -> Unit,
    scrollBehavior: WooPageHeaderScrollBehavior,
    contentBeforeWidgets: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    numberOfColumns: Int = 1,
) {
    val scrollState = rememberScrollState()
    val scrollModifier = modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .verticalScroll(scrollState)

    LaunchedEffect(scrollToTopTrigger, scrollBehavior, scrollState) {
        scrollToTopTrigger.collectLatest {
            coroutineScope {
                launch { scrollState.animateScrollTo(0) }
                launch { scrollBehavior.expand() }
            }
        }
    }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }
            .drop(1) // Ignore the initial value emitted on composition
            .debounce(SCROLL_INTERACTION_DEBOUNCE_MS)
            .collect { onDashboardInteracted() }
    }

    Column(modifier = scrollModifier) {
        contentBeforeWidgets()
        Column(
            modifier = Modifier.padding(WooTheme.padding.padding7),
            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space7),
        ) {
            if (numberOfColumns == 1) {
                widgetUiModels.forEach { widget ->
                    key(widget.stableKey()) {
                        AnimatedVisibility(widget.isVisible) {
                            widgetContent(widget, Modifier.fillMaxWidth())
                        }
                    }
                }
            } else {
                val widgetColumns = splitWidgetsIntoColumns(
                    numberOfColumns = numberOfColumns,
                    visibleUiWidgets = widgetUiModels.filter { widget -> widget.isVisible },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space5),
                ) {
                    widgetColumns.forEach { columnWidgets ->
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space7),
                        ) {
                            columnWidgets.forEach { widget ->
                                widgetContent(widget, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
            if (showCustomizeButton) {
                DashboardCustomizeButton(
                    hasNewWidgets = hasNewWidgets,
                    onClick = onEditWidgetsClicked,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
private fun DashboardCustomizeButton(
    hasNewWidgets: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val newWidgetsStateDescription = stringResource(R.string.dashboard_customize_new_sections_state_description)
    Box(modifier = modifier) {
        WooFilledTonalButton(
            text = stringResource(R.string.my_store_edit_screen_widgets),
            onClick = onClick,
            size = WooButtonSize.Small,
            modifier = Modifier.semantics {
                if (hasNewWidgets) {
                    stateDescription = newWidgetsStateDescription
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = WooIcons.Regular.Pen,
                    contentDescription = null,
                )
            },
        )
        if (hasNewWidgets) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(WooTheme.spacing.space3)
                    .background(WooTheme.colors.primary, CircleShape)
                    .clearAndSetSemantics {},
            )
        }
    }
}

@Suppress("MagicNumber")
private fun calculateColumnNumber(
    availableWidthInDp: Dp,
    visibleWidgetsCount: Int,
): Int {
    val columns = when {
        availableWidthInDp < 600.dp -> 1 // 600dp covers 99.96% of phones in portrait
        availableWidthInDp < 1000.dp -> 2 // 1000dp should be enough to avoid 3 columns on big phones in landscape
        else -> 3 // 3 columns should only display on tablets in landscape
    }

    return columns.coerceAtMost(maximumValue = maxOf(visibleWidgetsCount, 1))
}

private fun splitWidgetsIntoColumns(
    numberOfColumns: Int,
    visibleUiWidgets: List<DashboardWidgetUiModel>
): List<List<DashboardWidgetUiModel>> {
    val widgetColumns = MutableList<MutableList<DashboardWidgetUiModel>>(numberOfColumns) { mutableListOf() }
    for ((index, widget) in visibleUiWidgets.withIndex()) {
        widgetColumns[index % numberOfColumns].add(widget)
    }
    return widgetColumns
}

private fun DashboardWidgetUiModel.stableKey(): Any = when (this) {
    is ConfigurableWidget -> widget.type
    is ShareStoreWidget -> "share_store"
    is FeedbackWidget -> "feedback"
    is NewWidgetsCard -> "new_widgets"
}

@Composable
private fun DashboardWidgetCard(
    widget: DashboardWidgetUiModel,
    mainActivityViewModel: MainActivityViewModel,
    dashboardViewModel: DashboardViewModel,
    blazeCampaignCreationDispatcher: BlazeCampaignCreationDispatcher,
    modifier: Modifier
) {
    when (widget) {
        is ConfigurableWidget -> {
            ConfigurableWidgetCard(
                widgetUiModel = widget,
                mainActivityViewModel = mainActivityViewModel,
                dashboardViewModel = dashboardViewModel,
                blazeCampaignCreationDispatcher = blazeCampaignCreationDispatcher,
                modifier = modifier
            )
        }

        is ShareStoreWidget -> {
            ShareStoreCard(
                onShareClicked = widget.onShareClicked,
                modifier = modifier
            )
        }

        is FeedbackWidget -> {
            FeedbackCard(
                widget = widget,
                modifier = modifier
            )
        }

        is NewWidgetsCard -> {
            NewWidgetsCard(
                state = widget,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun ConfigurableWidgetCard(
    widgetUiModel: ConfigurableWidget,
    mainActivityViewModel: MainActivityViewModel,
    dashboardViewModel: DashboardViewModel,
    blazeCampaignCreationDispatcher: BlazeCampaignCreationDispatcher,
    modifier: Modifier
) {
    when (widgetUiModel.widget.type) {
        DashboardWidget.Type.AI_ASSISTANT -> DashboardAIAssistantCard(
            onClick = dashboardViewModel::onAiAssistantCardClicked,
            modifier = modifier
        )

        DashboardWidget.Type.PUSH_NOTIFICATIONS -> DashboardPushNotificationsCard(
            onClick = {
                dashboardViewModel.trackCardInteracted(DashboardWidget.Type.PUSH_NOTIFICATIONS.trackingIdentifier)
                dashboardViewModel.onDashboardWidgetEvent(
                    DashboardViewModel.DashboardEvent.OpenWooPushNotificationsIntroduction
                )
            },
            onShown = { dashboardViewModel.trackPushNotificationsCardView() },
            onHideClicked = { dashboardViewModel.onHideWidgetClicked(DashboardWidget.Type.PUSH_NOTIFICATIONS) },
            modifier = modifier
        )

        DashboardWidget.Type.STATS -> {
            DashboardStatsCard(
                openDatePicker = { start, end, callback ->
                    dashboardViewModel.onDashboardWidgetEvent(OpenRangePicker(start, end, callback))
                },
                parentViewModel = dashboardViewModel,
                modifier = modifier
            )
        }

        DashboardWidget.Type.POPULAR_PRODUCTS -> DashboardTopPerformersWidgetCard(
            parentViewModel = dashboardViewModel,
            modifier = modifier
        )

        DashboardWidget.Type.BLAZE -> DashboardBlazeCard(
            blazeCampaignCreationDispatcher = blazeCampaignCreationDispatcher,
            activityViewModel = mainActivityViewModel,
            parentViewModel = dashboardViewModel,
            modifier = modifier
        )

        DashboardWidget.Type.ONBOARDING -> DashboardOnboardingCard(
            parentViewModel = dashboardViewModel,
            modifier = modifier
        )

        DashboardWidget.Type.ORDERS -> DashboardOrdersCard(
            parentViewModel = dashboardViewModel,
            modifier = modifier
        )

        DashboardWidget.Type.REVIEWS -> DashboardReviewsCard(
            parentViewModel = dashboardViewModel,
            modifier = modifier
        )

        DashboardWidget.Type.COUPONS -> DashboardCouponsCard(
            parentViewModel = dashboardViewModel,
            modifier = modifier
        )

        DashboardWidget.Type.STOCK -> DashboardProductStockCard(
            parentViewModel = dashboardViewModel,
            modifier = modifier
        )

        DashboardWidget.Type.INBOX -> DashboardInboxCard(
            parentViewModel = dashboardViewModel,
            modifier = modifier
        )

        DashboardWidget.Type.GOOGLE_ADS -> {
            DashboardGoogleAdsCard(
                parentViewModel = dashboardViewModel,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun ShareStoreCard(
    onShareClicked: () -> Unit,
    modifier: Modifier
) {
    DashboardCardSurface(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(WooTheme.padding.padding5),
        ) {
            Image(
                painter = painterResource(id = R.drawable.blaze_campaign_created_success),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.height(WooTheme.spacing.space7))
            Text(
                text = stringResource(id = R.string.get_the_word_out),
                style = WooTheme.text.titleLarge.strong,
                color = WooTheme.colors.surface.onDefault,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(WooTheme.spacing.space3))
            Text(
                text = stringResource(id = R.string.share_your_store_message),
                style = WooTheme.text.bodyLarge.regular,
                color = WooTheme.colors.surface.onDefault,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(WooTheme.spacing.space5))
            WooFilledButton(
                onClick = onShareClicked,
                text = stringResource(id = R.string.share_store_button),
            )
        }
    }
}

@Composable
private fun FeedbackCard(
    widget: FeedbackWidget,
    modifier: Modifier
) {
    LaunchedEffect(Unit) {
        widget.onShown()
    }

    DashboardCardSurface(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(WooTheme.padding.padding5),
        ) {
            Text(
                text = stringResource(id = R.string.feedback_request_title),
                style = WooTheme.text.bodyLarge.emphasized,
                color = WooTheme.colors.surface.onDefault,
                modifier = Modifier.padding(top = WooTheme.padding.padding3),
            )
            Spacer(modifier = Modifier.height(WooTheme.spacing.space5))
            Row(
                horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space5),
                modifier = Modifier.fillMaxWidth(),
            ) {
                WooOutlinedButton(
                    onClick = widget.onNegativeClick,
                    text = stringResource(id = R.string.feedback_request_make_better),
                    modifier = Modifier.weight(1f),
                )
                WooFilledButton(
                    onClick = widget.onPositiveClick,
                    text = stringResource(id = R.string.feedback_request_like_it),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NewWidgetsCard(
    state: NewWidgetsCard,
    modifier: Modifier
) {
    DashboardCardSurface(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(WooTheme.padding.padding5),
        ) {
            Text(
                text = stringResource(R.string.dashboard_new_widgets_card_title),
                style = WooTheme.text.titleLarge.strong,
                color = WooTheme.colors.surface.onDefault,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(WooTheme.spacing.space3))
            Text(
                text = stringResource(R.string.dashboard_new_widgets_card_description),
                style = WooTheme.text.bodyLarge.regular,
                color = WooTheme.colors.surface.onDefault,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(WooTheme.spacing.space5))
            WooFilledButton(
                onClick = state.onShowCardsClick,
                text = stringResource(id = R.string.dashboard_new_widgets_card_button),
            )
        }
    }
}

@PreviewLightDark
@Preview(name = "Dashboard large font", fontScale = 2f)
@Preview(name = "Dashboard RTL", locale = "ar")
@Composable
private fun DashboardLayoutPreview() {
    DashboardPreviewContent(widgets = previewWidgets.take(1))
}

@Preview(name = "Dashboard wide widgets and Customize", widthDp = 840, heightDp = 700)
@Composable
private fun DashboardWideLayoutPreview() {
    DashboardPreviewContent(widgets = previewWidgets.take(2))
}

@Composable
private fun DashboardPreviewContent(widgets: List<DashboardWidgetUiModel>) {
    WooDesignSystemThemeWithBackground {
        val scrollBehavior = WooPageHeaderDefaults.exitUntilCollapsedScrollBehavior()
        Column {
            DashboardHeader(
                storeName = "Example Store",
                showShareStoreButton = true,
                onShareStoreClicked = {},
                scrollBehavior = scrollBehavior,
            )
            DashboardLayout(
                widgets = widgets,
                isRefreshing = false,
                onPullToRefresh = {},
                scrollToTopTrigger = emptyFlow(),
                onDashboardInteracted = {},
                hasNewWidgets = true,
                showCustomizeButton = true,
                onEditWidgetsClicked = {},
                scrollBehavior = scrollBehavior,
                contentBeforeWidgets = {},
                modifier = Modifier.weight(1f),
            ) { widget, modifier ->
                DashboardPreviewCardContent(widget = widget, modifier = modifier)
            }
        }
    }
}

@Composable
private fun DashboardPreviewCardContent(
    widget: DashboardWidgetUiModel,
    modifier: Modifier,
) {
    val menu = DashboardWidgetMenu(emptyList())
    val widgetType = (widget as ConfigurableWidget).widget.type
    when (widgetType) {
        DashboardWidget.Type.ORDERS -> WidgetCard(
            titleResource = DashboardWidget.Type.ORDERS.titleResource,
            menu = menu,
            isError = false,
            modifier = modifier,
        ) {
            TopOrders(
                selectedFilter = previewOrderFilter,
                filterOptions = listOf(previewOrderFilter),
                onFilterSelected = {},
                orders = previewOrders,
                onOrderClicked = {},
            )
        }

        DashboardWidget.Type.POPULAR_PRODUCTS -> WidgetCard(
            titleResource = DashboardWidget.Type.POPULAR_PRODUCTS.titleResource,
            menu = menu,
            isError = false,
            modifier = modifier,
        ) {
            Column(modifier = Modifier.padding(horizontal = WooTheme.padding.padding5)) {
                repeat(3) {
                    TopPerformerSkeletonItem()
                    if (it < 2) {
                        WooDivider()
                    }
                }
            }
        }

        DashboardWidget.Type.STOCK -> WidgetCard(
            titleResource = DashboardWidget.Type.STOCK.titleResource,
            menu = menu,
            isError = false,
            modifier = modifier,
        ) {
            StockEmptyView()
        }

        DashboardWidget.Type.REVIEWS -> WidgetCard(
            titleResource = DashboardWidget.Type.REVIEWS.titleResource,
            menu = menu,
            isError = true,
            modifier = modifier,
        ) {
            WidgetError(onContactSupportClicked = {}, onRetryClicked = {})
        }

        else -> error("Unsupported preview widget type: $widgetType")
    }
}

private val previewWidgets = listOf(
    DashboardWidget.Type.ORDERS,
    DashboardWidget.Type.POPULAR_PRODUCTS,
    DashboardWidget.Type.STOCK,
    DashboardWidget.Type.REVIEWS,
).map { type ->
    ConfigurableWidget(DashboardWidget(type, true, DashboardWidget.Status.Available))
}

private val previewOrderFilter = OrderStatusOption(
    key = "processing",
    label = "Processing",
    statusCount = 1,
    isSelected = true,
)

private val previewOrders = listOf(
    OrderItem(
        id = 2L,
        number = "#1041",
        date = "Yesterday",
        customerName = "A deliberately long customer name for font scaling",
        status = "Completed",
        statusColor = R.color.tag_bg_completed,
        totalPrice = "$86.00",
        isPosOrder = true,
    ),
)
