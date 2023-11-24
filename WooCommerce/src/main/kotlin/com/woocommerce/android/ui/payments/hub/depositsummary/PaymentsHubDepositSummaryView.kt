@file:OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)

package com.woocommerce.android.ui.payments.hub.depositsummary

import android.content.res.Configuration
import android.icu.text.MessageFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.StringUtils
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PaymentsHubDepositSummaryView(
    viewModel: PaymentsHubDepositSummaryViewModel = viewModel()
) {
    viewModel.viewState.observeAsState().let {
        WooThemeWithBackground {
            when (val value = it.value) {
                is PaymentsHubDepositSummaryState.Success -> PaymentsHubDepositSummaryView(
                    value.overview,
                    value.fromCache,
                    value.onLearnMoreClicked,
                    value.onExpandCollapseClicked,
                    viewModel::onSummaryDepositShown,
                    value.onCurrencySelected,
                )

                null,
                PaymentsHubDepositSummaryState.Loading,
                is PaymentsHubDepositSummaryState.Error -> {
                    // show nothing
                }
            }
        }
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel
            .openBrowserEvents
            .collect { url ->
                ChromeCustomTabUtils.launchUrl(
                    context,
                    url,
                    ChromeCustomTabUtils.Height.Partial.ThreeQuarters,
                )
            }
    }
}

@Composable
fun PaymentsHubDepositSummaryView(
    overview: PaymentsHubDepositSummaryState.Overview,
    fromCache: Boolean,
    onLearnMoreClicked: () -> Unit,
    onExpandCollapseClicked: (Boolean) -> Unit,
    onSummaryDepositShown: () -> Unit,
    onCurrencySelected: (String) -> Unit,
    isPreview: Boolean = LocalInspectionMode.current,
    selectedPage: Int = 0,
) {
    LaunchedEffect(key1 = overview) { onSummaryDepositShown() }
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val pageCount = overview.infoPerCurrency.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.color_surface))
    ) {
        val pagerState = rememberPagerState(initialPage = selectedPage)
        val isInitialLoad = remember { mutableStateOf(true) }

        val currencies = overview.infoPerCurrency.keys.toList()
        val selectedCurrency = currencies[pagerState.currentPage]

        LaunchedEffect(pagerState.currentPage) {
            if (isInitialLoad.value) {
                isInitialLoad.value = false
            } else {
                onCurrencySelected(selectedCurrency)
            }
        }
        val selectedCurrencyInfo = overview.infoPerCurrency[selectedCurrency] ?: return@Column

        AnimatedVisibility(
            visible = (isExpanded || isPreview) && pageCount > 1,
            modifier = Modifier.fillMaxWidth(),
        ) {
            CurrenciesTabs(
                currencies = currencies.map { it.uppercase() }.toList(),
                pagerState = pagerState
            )
        }

        HorizontalPager(
            pageCount = pageCount,
            state = pagerState
        ) { pageIndex ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorResource(id = R.color.color_surface))
            ) {
                FundsOverview(selectedCurrencyInfo, isExpanded, fromCache, pageIndex) {
                    isExpanded = !isExpanded
                    onExpandCollapseClicked(isExpanded)
                }

                AnimatedVisibility(
                    visible = isExpanded || isPreview,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    DepositsInfo(
                        selectedCurrencyInfo,
                        onLearnMoreClicked
                    )
                }
            }
        }
    }
}

@Composable
private fun FundsOverview(
    currencyInfo: PaymentsHubDepositSummaryState.Info,
    isExpanded: Boolean,
    fromCache: Boolean,
    pageIndex: Int,
    onExpandCollapseClick: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        if (isExpanded) 180f else 0f, label = "chevronRotation"
    )
    val topRowIS = remember { MutableInteractionSource() }
    val topRowCoroutineScope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = topRowIS,
                indication = null
            ) {
                val press = PressInteraction.Press(Offset.Zero)
                topRowCoroutineScope.launch {
                    topRowIS.emit(press)
                    topRowIS.emit(PressInteraction.Release(press))
                }
                onExpandCollapseClick()
            }
            .padding(
                start = dimensionResource(id = R.dimen.major_100),
                end = dimensionResource(id = R.dimen.major_100),
                top = dimensionResource(id = R.dimen.major_150),
                bottom = dimensionResource(id = R.dimen.major_100)
            )
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                style = MaterialTheme.typography.body2,
                text = stringResource(id = R.string.card_reader_hub_deposit_summary_available_funds),
                color = colorResource(id = R.color.color_on_surface)
            )
            FundsNumber(
                currencyInfo.availableFundsFormatted,
                currencyInfo.availableFundsAmount,
                fromCache,
                pageIndex
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                style = MaterialTheme.typography.body2,
                text = stringResource(id = R.string.card_reader_hub_deposit_summary_pending_funds),
                color = colorResource(id = R.color.color_on_surface)
            )

            FundsNumber(
                currencyInfo.pendingFundsFormatted,
                currencyInfo.pendingFundsAmount,
                fromCache,
                pageIndex
            )

            Text(
                style = MaterialTheme.typography.caption,
                text = StringUtils.getQuantityString(
                    context = LocalContext.current,
                    quantity = currencyInfo.pendingBalanceDepositsCount,
                    default = R.string.card_reader_hub_deposit_summary_pending_deposits_plural,
                    one = R.string.card_reader_hub_deposit_summary_pending_deposits_one,
                ),
                color = colorResource(id = R.color.color_on_surface_medium)
            )
        }

        Column(
            modifier = Modifier.weight(.3f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center,
        ) {
            IconButton(
                onClick = { onExpandCollapseClick() },
                interactionSource = topRowIS,
            ) {
                Icon(
                    modifier = Modifier.rotate(chevronRotation),
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription =
                    stringResource(R.string.card_reader_hub_deposit_summary_collapse_expand_content_description),
                    tint = MaterialTheme.colors.primary,
                )
            }
        }
    }
    val dividerPaddingAnimation by animateDpAsState(
        targetValue = if (isExpanded) {
            dimensionResource(id = R.dimen.major_100)
        } else {
            dimensionResource(id = R.dimen.minor_00)
        },
        label = "dividerPaddingAnimation"
    )

    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dividerPaddingAnimation)
    )
}

@Composable
private fun DepositsInfo(
    currencyInfo: PaymentsHubDepositSummaryState.Info,
    onLearnMoreClicked: () -> Unit
) {
    Column {
        Column(
            modifier = Modifier
                .padding(
                    start = dimensionResource(id = R.dimen.major_100),
                    end = dimensionResource(id = R.dimen.major_100),
                    top = 10.dp,
                    bottom = dimensionResource(id = R.dimen.major_125)
                )
        ) {
            currencyInfo.fundsAvailableInDays?.let { fundsAvailableInDays ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_calendar_gray_16), contentDescription = null)
                    Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.minor_100)))
                    Text(
                        style = MaterialTheme.typography.caption,
                        text = StringUtils.getQuantityString(
                            context = LocalContext.current,
                            quantity = fundsAvailableInDays,
                            default = R.string.card_reader_hub_deposit_summary_funds_available_after_plural,
                            one = R.string.card_reader_hub_deposit_summary_funds_available_after_one,
                        ),
                        color = colorResource(id = R.color.color_on_surface_medium),
                    )
                }
            }

            if (currencyInfo.nextDeposit != null || currencyInfo.lastDeposit != null) {
                NextAndLastDeposit(
                    currencyInfo.nextDeposit,
                    currencyInfo.lastDeposit
                )
                Divider(modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.minor_100)))

            currencyInfo.fundsDepositInterval?.let { interval ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_acropolis_gray_15), contentDescription = null)
                    Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.minor_100)))
                    Text(
                        style = MaterialTheme.typography.caption,
                        text = interval.buildText(),
                        color = colorResource(id = R.color.color_on_surface_medium),
                    )
                }
            }

            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.major_75)))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLearnMoreClicked)
                    .padding(vertical = dimensionResource(id = R.dimen.minor_50)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier.size(15.dp),
                    painter = painterResource(
                        id = R.drawable.ic_info_outline_20dp
                    ),
                    contentDescription = null,
                    tint = colorResource(id = R.color.color_primary)
                )
                Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.minor_100)))
                Text(
                    style = MaterialTheme.typography.caption,
                    text = stringResource(id = R.string.card_reader_hub_deposit_summary_learn_more),
                    color = colorResource(id = R.color.color_primary),
                )
            }
        }

        Divider(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun NextAndLastDeposit(
    nextDeposit: PaymentsHubDepositSummaryState.Deposit?,
    lastDeposit: PaymentsHubDepositSummaryState.Deposit?,
) {
    Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.major_150)))

    Text(
        style = MaterialTheme.typography.body2,
        text = stringResource(id = R.string.card_reader_hub_deposit_funds_deposits_title).uppercase(),
        color = colorResource(id = R.color.color_on_surface_medium),
    )

    Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.minor_100)))

    nextDeposit?.let {
        Deposit(
            depositType = R.string.card_reader_hub_deposit_summary_next,
            deposit = it,
            textColor = R.color.color_on_surface
        )
        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.major_100)))
    }

    lastDeposit?.let {
        Deposit(
            depositType = R.string.card_reader_hub_deposit_summary_last,
            deposit = it,
            textColor = R.color.color_on_surface_medium
        )
        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.major_100)))
    }
}

@Composable
private fun Deposit(
    depositType: Int,
    deposit: PaymentsHubDepositSummaryState.Deposit,
    textColor: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            modifier = Modifier.weight(.5f),
            style = MaterialTheme.typography.body1,
            text = stringResource(id = depositType),
            color = colorResource(id = textColor),
        )

        Text(
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.body1,
            text = deposit.date,
            color = colorResource(id = textColor),
        )

        Box(modifier = Modifier.weight(1f)) {
            when (deposit.status) {
                PaymentsHubDepositSummaryState.Deposit.Status.ESTIMATED ->
                    DepositStatus(
                        text = R.string.card_reader_hub_deposit_summary_status_estimated,
                        backgroundColor = R.color.deposit_summary_status_estimated_background,
                        textColor = R.color.deposit_summary_status_estimated_text
                    )

                PaymentsHubDepositSummaryState.Deposit.Status.PENDING ->
                    DepositStatus(
                        text = R.string.card_reader_hub_deposit_summary_status_pending,
                        backgroundColor = R.color.deposit_summary_status_pending_background,
                        textColor = R.color.deposit_summary_status_pending_text
                    )

                PaymentsHubDepositSummaryState.Deposit.Status.IN_TRANSIT ->
                    DepositStatus(
                        text = R.string.card_reader_hub_deposit_summary_status_in_transit,
                        backgroundColor = R.color.deposit_summary_status_in_transit_background,
                        textColor = R.color.deposit_summary_status_in_transit_text
                    )

                PaymentsHubDepositSummaryState.Deposit.Status.PAID ->
                    DepositStatus(
                        text = R.string.card_reader_hub_deposit_summary_status_paid,
                        backgroundColor = R.color.deposit_summary_status_paid_background,
                        textColor = R.color.deposit_summary_status_paid_text
                    )

                PaymentsHubDepositSummaryState.Deposit.Status.CANCELED ->
                    DepositStatus(
                        text = R.string.card_reader_hub_deposit_summary_status_canceled,
                        backgroundColor = R.color.deposit_summary_status_canceled_background,
                        textColor = R.color.deposit_summary_status_canceled_text
                    )

                PaymentsHubDepositSummaryState.Deposit.Status.FAILED ->
                    DepositStatus(
                        text = R.string.card_reader_hub_deposit_summary_status_failed,
                        backgroundColor = R.color.deposit_summary_status_failed_background,
                        textColor = R.color.deposit_summary_status_failed_text
                    )

                PaymentsHubDepositSummaryState.Deposit.Status.UNKNOWN -> DepositStatus(
                    text = R.string.card_reader_hub_deposit_summary_status_unknown,
                    backgroundColor = R.color.deposit_summary_status_unknown_background,
                    textColor = R.color.deposit_summary_status_unknown_text
                )
            }
        }

        Box(
            modifier = Modifier.weight(.8f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                style = MaterialTheme.typography.body1,
                text = deposit.amount,
                color = colorResource(id = textColor),
                fontWeight = FontWeight(600),
            )
        }
    }
}

@Composable
private fun CurrenciesTabs(
    currencies: List<String>,
    pagerState: PagerState,
) {
    val scope = rememberCoroutineScope()
    TabRow(
        modifier = Modifier.fillMaxWidth(),
        selectedTabIndex = pagerState.currentPage,
        backgroundColor = colorResource(id = R.color.color_surface),
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier
                    .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                    .padding(horizontal = 16.dp),
                height = 4.dp,
                color = colorResource(id = R.color.color_primary)
            )
        }
    ) {
        currencies.forEachIndexed { index, title ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                text = {
                    val isSelected = pagerState.currentPage == index
                    Text(
                        style = MaterialTheme.typography.body1,
                        text = title,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) {
                            colorResource(id = R.color.color_primary)
                        } else {
                            colorResource(id = R.color.color_on_surface_disabled)
                        }
                    )
                },
            )
        }
    }
}

@Composable
private fun DepositStatus(
    text: Int,
    backgroundColor: Int,
    textColor: Int
) {
    Box(
        modifier = Modifier
            .background(
                color = colorResource(backgroundColor),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(
                horizontal = dimensionResource(id = R.dimen.minor_100),
                vertical = dimensionResource(id = R.dimen.minor_50)
            )
    ) {
        Text(
            text = stringResource(id = text),
            style = MaterialTheme.typography.caption,
            color = colorResource(id = textColor),
        )
    }
}

@Composable
private fun FundsNumber(
    valueToDisplay: String,
    valueAmount: Long,
    fromCache: Boolean,
    pageIndex: Int,
) {
    var animationPlayed by remember { mutableStateOf(false) }
    if (pageIndex == 0) {
        AnimatedContent(
            targetState = valueToDisplay to valueAmount,
            transitionSpec = {
                if (animationPlayed) {
                    EnterTransition.None with ExitTransition.None
                } else if (targetState.second > initialState.second) {
                    slideInVertically { -it } with slideOutVertically { it }
                } else {
                    slideInVertically { it } with slideOutVertically { -it }
                }
            },
            label = "AnimatedFundsNumber"
        ) { value ->
            Text(
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                text = value.first,
                color = colorResource(id = R.color.color_on_surface)
            )
            if (!fromCache && value.second == valueAmount) {
                animationPlayed = true
            }
        }
    } else {
        Text(
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            text = valueToDisplay,
            color = colorResource(id = R.color.color_on_surface)
        )
    }
}

@Composable
private fun PaymentsHubDepositSummaryState.Info.Interval.buildText() =
    when (this) {
        PaymentsHubDepositSummaryState.Info.Interval.Daily -> stringResource(
            id = R.string.card_reader_hub_deposit_summary_available_deposit_time_daily
        )

        is PaymentsHubDepositSummaryState.Info.Interval.Weekly -> {
            val dayOfWeek = DayOfWeek.valueOf(weekDay.uppercase(Locale.getDefault()))
            stringResource(
                id = R.string.card_reader_hub_deposit_summary_available_deposit_time_weekly,
                dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            )
        }

        is PaymentsHubDepositSummaryState.Info.Interval.Monthly -> {
            val formatter = MessageFormat("{0,ordinal}", Locale.getDefault())
            stringResource(
                id = R.string.card_reader_hub_deposit_summary_available_deposit_time_monthly,
                formatter.format(arrayOf(day))
            )
        }
    }

private val previewState = mapOf(
    "USD" to PaymentsHubDepositSummaryState.Info(
        availableFundsFormatted = "100$",
        pendingFundsFormatted = "200$",
        availableFundsAmount = 10000,
        pendingFundsAmount = 20000,
        pendingBalanceDepositsCount = 1,
        fundsAvailableInDays = 5,
        fundsDepositInterval = PaymentsHubDepositSummaryState.Info.Interval.Daily,
        nextDeposit = PaymentsHubDepositSummaryState.Deposit(
            amount = "100$",
            status = PaymentsHubDepositSummaryState.Deposit.Status.ESTIMATED,
            date = "13 Oct 2023"
        ),
        lastDeposit = PaymentsHubDepositSummaryState.Deposit(
            amount = "100$",
            status = PaymentsHubDepositSummaryState.Deposit.Status.FAILED,
            date = "13 Oct 2023"
        )
    ),
    "EUR" to PaymentsHubDepositSummaryState.Info(
        availableFundsFormatted = "100$",
        pendingFundsFormatted = "200$",
        availableFundsAmount = 10000,
        pendingFundsAmount = 20000,
        pendingBalanceDepositsCount = 1,
        fundsAvailableInDays = 2,
        fundsDepositInterval = PaymentsHubDepositSummaryState.Info.Interval.Weekly("Friday"),
        nextDeposit = PaymentsHubDepositSummaryState.Deposit(
            amount = "100$",
            status = PaymentsHubDepositSummaryState.Deposit.Status.PAID,
            date = "13 Oct 2023"
        ),
        lastDeposit = PaymentsHubDepositSummaryState.Deposit(
            amount = "100$",
            status = PaymentsHubDepositSummaryState.Deposit.Status.PENDING,
            date = "13 Oct 2023"
        )
    ),
    "RUB" to PaymentsHubDepositSummaryState.Info(
        availableFundsFormatted = "100$",
        pendingFundsFormatted = "200$",
        availableFundsAmount = 10000,
        pendingFundsAmount = 20000,
        pendingBalanceDepositsCount = 1,
        fundsAvailableInDays = 4,
        fundsDepositInterval = PaymentsHubDepositSummaryState.Info.Interval.Weekly("Monday"),
        nextDeposit = PaymentsHubDepositSummaryState.Deposit(
            amount = "100$",
            status = PaymentsHubDepositSummaryState.Deposit.Status.IN_TRANSIT,
            date = "13 Oct 2023"
        ),
        lastDeposit = PaymentsHubDepositSummaryState.Deposit(
            amount = "100$",
            status = PaymentsHubDepositSummaryState.Deposit.Status.CANCELED,
            date = "13 Oct 2023"
        )
    ),
    "GBP" to PaymentsHubDepositSummaryState.Info(
        availableFundsFormatted = "100$",
        pendingFundsFormatted = "200$",
        availableFundsAmount = 10000,
        pendingFundsAmount = 20000,
        pendingBalanceDepositsCount = 1,
        fundsAvailableInDays = 3,
        fundsDepositInterval = PaymentsHubDepositSummaryState.Info.Interval.Weekly("Tuesday"),
        nextDeposit = PaymentsHubDepositSummaryState.Deposit(
            amount = "100$",
            status = PaymentsHubDepositSummaryState.Deposit.Status.UNKNOWN,
            date = "13 Oct 2023"
        ),
        lastDeposit = PaymentsHubDepositSummaryState.Deposit(
            amount = "100$",
            status = PaymentsHubDepositSummaryState.Deposit.Status.ESTIMATED,
            date = "13 Oct 2023"
        )
    ),
)

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PaymentsHubDepositSummaryViewUsdPreview() {
    WooThemeWithBackground {
        PaymentsHubDepositSummaryView(
            PaymentsHubDepositSummaryState.Overview(
                defaultCurrency = "USD",
                infoPerCurrency = previewState,
            ),
            fromCache = false,
            onLearnMoreClicked = {},
            onExpandCollapseClicked = {},
            onSummaryDepositShown = {},
            onCurrencySelected = {},
            selectedPage = 0
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PaymentsHubDepositSummaryViewEurPreview() {
    WooThemeWithBackground {
        PaymentsHubDepositSummaryView(
            PaymentsHubDepositSummaryState.Overview(
                defaultCurrency = "USD",
                infoPerCurrency = previewState
            ),
            fromCache = false,
            onLearnMoreClicked = {},
            onExpandCollapseClicked = {},
            onSummaryDepositShown = {},
            onCurrencySelected = {},
            selectedPage = 1
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PaymentsHubDepositSummaryViewRubPreview() {
    WooThemeWithBackground {
        PaymentsHubDepositSummaryView(
            PaymentsHubDepositSummaryState.Overview(
                defaultCurrency = "USD",
                infoPerCurrency = previewState
            ),
            fromCache = false,
            onLearnMoreClicked = {},
            onExpandCollapseClicked = {},
            onSummaryDepositShown = {},
            onCurrencySelected = {},
            selectedPage = 2
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PaymentsHubDepositSummaryViewGbpPreview() {
    WooThemeWithBackground {
        PaymentsHubDepositSummaryView(
            PaymentsHubDepositSummaryState.Overview(
                defaultCurrency = "USD",
                infoPerCurrency = previewState
            ),
            fromCache = false,
            onLearnMoreClicked = {},
            onExpandCollapseClicked = {},
            onSummaryDepositShown = {},
            onCurrencySelected = {},
            selectedPage = 3
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PaymentsHubDepositSummaryViewNoDepositsPreview() {
    WooThemeWithBackground {
        PaymentsHubDepositSummaryView(
            PaymentsHubDepositSummaryState.Overview(
                defaultCurrency = "USD",
                infoPerCurrency = mapOf(
                    "USD" to PaymentsHubDepositSummaryState.Info(
                        availableFundsFormatted = "100$",
                        pendingFundsFormatted = "200$",
                        availableFundsAmount = 10000,
                        pendingFundsAmount = 20000,
                        pendingBalanceDepositsCount = 1,
                        fundsAvailableInDays = 5,
                        fundsDepositInterval = PaymentsHubDepositSummaryState.Info.Interval.Daily,
                        nextDeposit = null,
                        lastDeposit = null,
                    )
                )
            ),
            fromCache = false,
            onLearnMoreClicked = {},
            onExpandCollapseClicked = {},
            onSummaryDepositShown = {},
            onCurrencySelected = {},
            selectedPage = 0
        )
    }
}
