package com.woocommerce.android.ui.dashboard.inbox

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.compose.viewModelWithFactory
import com.woocommerce.android.ui.dashboard.DashboardFragmentDirections
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError
import com.woocommerce.android.ui.dashboard.inbox.DashboardInboxViewModel.NavigateToInbox
import com.woocommerce.android.ui.dashboard.inbox.DashboardInboxViewModel.ViewState
import com.woocommerce.android.ui.inbox.InboxNoteActionEvent
import com.woocommerce.android.ui.inbox.InboxNoteItemSkeleton
import com.woocommerce.android.ui.inbox.InboxNoteRow
import com.woocommerce.android.ui.inbox.InboxNoteUi
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.ChromeCustomTabUtils.Height.Partial.ThreeQuarters
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event

@Composable
fun DashboardInboxCard(
    parentViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    viewModel: DashboardInboxViewModel = viewModelWithFactory { factory: DashboardInboxViewModel.Factory ->
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

    HandleEvents(viewModel.event, parentViewModel::onShowSnackbar)
}

@Composable
private fun HandleEvents(
    event: LiveData<Event>,
    onShowSnackbar: (message: Int) -> Unit
) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    DisposableEffect(event, navController, lifecycleOwner) {
        val observer = Observer { event: Event ->
            when (event) {
                is NavigateToInbox -> {
                    navController.navigateSafely(
                        DashboardFragmentDirections.actionDashboardToInboxFragment()
                    )
                }
                is Event.ShowSnackbar -> onShowSnackbar(event.message)

                is InboxNoteActionEvent.OpenUrlEvent -> {
                    ChromeCustomTabUtils.launchUrl(context, event.url, ThreeQuarters)
                }
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
                InboxNoteRow(note, limitDescription = true)

                Divider(
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
            InboxNoteItemSkeleton()
            Divider(
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
            style = MaterialTheme.typography.h6,
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
    EmptyView()
}

@Composable
@Preview
fun PreviewLoadingCard() {
    Loading()
}
