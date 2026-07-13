package com.woocommerce.android.ui.dashboard.inbox

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedButton
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.dashboard.DashboardSkeleton
import com.woocommerce.android.ui.dashboard.DashboardFragmentDirections
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError
import com.woocommerce.android.ui.dashboard.inbox.DashboardInboxViewModel.NavigateToInbox
import com.woocommerce.android.ui.dashboard.inbox.DashboardInboxViewModel.ViewState
import com.woocommerce.android.ui.inbox.InboxNoteActionUi
import com.woocommerce.android.ui.inbox.InboxNoteUi
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event

@Composable
fun DashboardInboxCard(
    parentViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    viewModel: DashboardInboxViewModel = hiltViewModel { factory: DashboardInboxViewModel.Factory ->
        factory.create(parentViewModel)
    }
) {
    viewModel.viewState.observeAsState().value?.let { state ->
        val button = (state as? ViewState.Content)?.notes?.takeIf { it.isNotEmpty() }?.let { viewModel.button }
        WidgetCard(
            titleResource = state.title,
            menu = viewModel.menu,
            button = button,
            modifier = modifier,
            isError = state is ViewState.Error
        ) {
            when (state) {
                is ViewState.Content -> LatestNotes(state.notes)
                is ViewState.Error -> WidgetError(
                    onContactSupportClicked = parentViewModel::onContactSupportClicked,
                    onRetryClicked = viewModel::onRefresh
                )
                is ViewState.Loading -> Loading()
            }
        }
    }

    HandleEvents(viewModel.event, parentViewModel::onShowSnackbar, parentViewModel::onLaunchUrlInAuthenticatedWebView)
}

@Composable
private fun HandleEvents(
    event: LiveData<Event>,
    onShowSnackbar: (message: Int) -> Unit,
    onLaunchUrlInAuthenticatedWebView: (Event.LaunchUrlInAuthenticatedWebView) -> Unit
) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(event, navController, lifecycleOwner) {
        val observer = Observer { event: Event ->
            when (event) {
                is NavigateToInbox -> {
                    navController.navigateSafely(
                        DashboardFragmentDirections.actionDashboardToInboxFragment()
                    )
                }
                is Event.ShowSnackbar -> onShowSnackbar(event.message)

                is Event.LaunchUrlInAuthenticatedWebView -> onLaunchUrlInAuthenticatedWebView(event)
            }
        }

        event.observe(lifecycleOwner, observer)

        onDispose {
            event.removeObserver(observer)
        }
    }
}

@Composable
fun LatestNotes(
    notes: List<InboxNoteUi>
) {
    Column {
        if (notes.isEmpty()) {
            EmptyView()
        } else {
            notes.forEach { note ->
                DashboardInboxNoteRow(note, limitDescription = true)

                WooDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun Loading() {
    Column {
        repeat(3) {
            DashboardInboxNoteItemSkeleton()
            WooDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
            )
        }
    }
}

@Composable
fun EmptyView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.major_200)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_inbox),
            contentDescription = null,
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.major_100)))
        Text(
            text = stringResource(id = R.string.empty_inbox_title),
            textAlign = TextAlign.Center,
            style = WooTheme.text.titleLarge.strong,
            color = WooTheme.colors.surface.onDefault,
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_150),
                end = dimensionResource(id = R.dimen.major_150)
            )
        )
    }
}

@Composable
@Preview
fun PreviewEmptyView() {
    WooDesignSystemThemeWithBackground { EmptyView() }
}

@Composable
@Preview
fun PreviewLoadingCard() {
    WooDesignSystemThemeWithBackground { Loading() }
}

@Composable
private fun DashboardInboxNoteRow(note: InboxNoteUi, limitDescription: Boolean) {
    val showMore = remember { mutableStateOf(limitDescription && note.description.length > 100) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = WooTheme.padding.padding5),
            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
        ) {
            Text(
                modifier = Modifier.padding(top = WooTheme.padding.padding5),
                text = note.dateCreated,
                style = WooTheme.text.titleSmall.regular,
                color = WooTheme.colors.surface.onDefault,
            )
            Text(
                text = note.title,
                style = if (note.isActioned) {
                    WooTheme.text.titleMedium.regular
                } else {
                    WooTheme.text.titleMedium.strong
                },
                color = WooTheme.colors.surface.onDefault,
            )
            Text(
                modifier = Modifier.animateContentSize(),
                text = AnnotatedString.fromHtml(note.description),
                style = WooTheme.text.bodyMedium.regular,
                color = WooTheme.colors.surface.onDefault,
                maxLines = if (showMore.value) 2 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AnimatedContent(showMore.value, label = "Animated note action bar") { isMoreVisible ->
            if (isMoreVisible) {
                DashboardInboxTextAction(
                    label = stringResource(id = R.string.read_more),
                    onClick = { showMore.value = false },
                    modifier = Modifier.padding(start = WooTheme.padding.padding3),
                )
            } else if (note.isSurvey) {
                DashboardInboxSurveyActions(note.actions)
            } else {
                DashboardInboxActions(note.actions)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardInboxActions(actions: List<InboxNoteActionUi>) {
    FlowRow(
        modifier = Modifier
            .padding(horizontal = WooTheme.padding.padding3)
            .fillMaxWidth(),
    ) {
        actions.forEach { action -> DashboardInboxAction(action) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardInboxSurveyActions(actions: List<InboxNoteActionUi>) {
    FlowRow(
        modifier = Modifier.padding(
            start = WooTheme.padding.padding5,
            end = WooTheme.padding.padding5,
            bottom = WooTheme.padding.padding3,
        ),
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space5),
    ) {
        if (actions.isEmpty()) {
            Text(
                modifier = Modifier.padding(vertical = WooTheme.padding.padding5),
                text = stringResource(id = R.string.inbox_note_survey_actioned),
                style = WooTheme.text.bodyMedium.regular,
                color = WooTheme.colors.surface.onDefault,
            )
        } else {
            actions.forEachIndexed { index, action ->
                if (index < 2) {
                    WooOutlinedButton(
                        text = action.label,
                        onClick = { action.onClick(action.id, action.parentNoteId) },
                        enabled = !action.isDismissing,
                    )
                } else {
                    DashboardInboxAction(action)
                }
            }
        }
    }
}

@Composable
private fun DashboardInboxAction(action: InboxNoteActionUi) {
    val actionColor = colorResource(id = action.textColor)
    TextButton(
        onClick = { action.onClick(action.id, action.parentNoteId) },
        enabled = !action.isDismissing,
        colors = ButtonDefaults.textButtonColors(contentColor = actionColor),
    ) {
        if (action.isDismissing) {
            CircularProgressIndicator(
                modifier = Modifier.size(WooTheme.iconSize.size24),
                color = actionColor,
                strokeWidth = WooTheme.stroke.extraThick,
            )
        } else {
            Text(
                text = action.label.uppercase(),
                style = WooTheme.text.labelLarge.emphasized,
            )
        }
    }
}

@Composable
private fun DashboardInboxTextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(
            contentColor = WooTheme.colors.container.onSecondaryContainer
        ),
    ) {
        Text(text = label, style = WooTheme.text.labelLarge.emphasized)
    }
}

@Composable
private fun DashboardInboxNoteItemSkeleton() {
    Column(modifier = Modifier.padding(WooTheme.padding.padding5)) {
        DashboardSkeleton(width = 120.dp, height = 16.dp)
        Spacer(modifier = Modifier.padding(top = WooTheme.padding.padding6))
        DashboardSkeleton(width = 240.dp, height = 16.dp)
        Spacer(modifier = Modifier.padding(top = WooTheme.padding.padding5))
        repeat(3) {
            DashboardSkeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
            )
            Spacer(modifier = Modifier.padding(top = WooTheme.padding.padding2))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space5)) {
            DashboardSkeleton(width = 160.dp, height = 16.dp)
            DashboardSkeleton(width = 56.dp, height = 16.dp)
        }
    }
}
