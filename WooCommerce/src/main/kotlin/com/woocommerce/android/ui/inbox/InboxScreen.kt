package com.woocommerce.android.ui.inbox

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.core.text.HtmlCompat
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.toAnnotatedString
import com.woocommerce.android.ui.inbox.InboxViewModel.InboxState

@Composable
fun InboxScreen(viewModel: InboxViewModel) {
    val inboxState by viewModel.inboxState.observeAsState(InboxState())
    InboxScreen(state = inboxState)
}

@Composable
fun InboxScreen(state: InboxState) {
    when {
        state.isLoading -> InboxSkeletons()
        else -> InboxNotes(
            notes = state.notes,
            onRefresh = state.onRefresh,
            isRefreshing = state.isRefreshing
        )
    }
}

@Composable
fun InboxEmptyCase() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_200))
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.empty_inbox_title),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_150),
                end = dimensionResource(id = R.dimen.major_150)
            )
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.major_325)))
        Image(
            painter = painterResource(id = R.drawable.img_empty_inbox),
            contentDescription = null,
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.major_300)))
        Text(
            text = stringResource(id = R.string.empty_inbox_description),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_150),
                end = dimensionResource(id = R.dimen.major_150)
            )
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun InboxNotes(
    notes: List<InboxNoteUi>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(isRefreshing, { onRefresh() })
    Box(Modifier.pullRefresh(pullRefreshState)) {
        if (notes.isEmpty()) {
            InboxEmptyCase()
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(notes) { index, note ->
                    InboxNoteRow(note = note)
                    if (index < notes.lastIndex) {
                        Divider(
                            color = colorResource(id = R.color.divider_color),
                            thickness = dimensionResource(id = R.dimen.minor_10)
                        )
                    }
                }
            }
        }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colors.primary,
        )
    }
}

@Composable
fun InboxNoteRow(note: InboxNoteUi, limitDescription: Boolean = false) {
    val displayShowMoreButton = remember { mutableStateOf(limitDescription && note.description.length > 100) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_75))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_75)),
        ) {
            Text(
                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100)),
                text = note.dateCreated,
                style = MaterialTheme.typography.subtitle2,
                color = colorResource(id = R.color.color_surface_variant)
            )
            Text(
                text = note.title,
                fontWeight = if (note.isActioned) FontWeight.Normal else FontWeight.Bold,
                style = MaterialTheme.typography.subtitle1
            )
            Text(
                modifier = Modifier.animateContentSize(),
                text = HtmlCompat.fromHtml(note.description, HtmlCompat.FROM_HTML_MODE_LEGACY).toAnnotatedString(),
                style = MaterialTheme.typography.body2,
                maxLines = if (displayShowMoreButton.value) 2 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis
            )
        }
        AnimatedContent(displayShowMoreButton.value, label = "Animated note action bar") { isMoreButtonVisible ->
            if (isMoreButtonVisible) {
                WCTextButton(
                    modifier = Modifier.padding(start = dimensionResource(id = R.dimen.minor_100)),
                    onClick = { displayShowMoreButton.value = false },
                    text = stringResource(id = R.string.read_more)
                )
            } else {
                when {
                    note.isSurvey -> InboxNoteSurveyActionsRow(note.actions)
                    else -> InboxNoteActionsRow(note.actions)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InboxNoteActionsRow(actions: List<InboxNoteActionUi>) {
    FlowRow(
        modifier = Modifier
            .padding(
                start = dimensionResource(id = R.dimen.minor_100),
                end = dimensionResource(id = R.dimen.minor_100)
            )
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        actions.forEach {
            InboxNoteTextAction(inboxAction = it)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InboxNoteSurveyActionsRow(actions: List<InboxNoteActionUi>) {
    FlowRow(
        modifier = Modifier.padding(
            start = dimensionResource(id = R.dimen.major_100),
            end = dimensionResource(id = R.dimen.major_100),
            bottom = dimensionResource(id = R.dimen.minor_100)
        ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
    ) {
        if (actions.isEmpty()) {
            Text(
                modifier = Modifier.padding(
                    top = dimensionResource(id = R.dimen.major_100),
                    bottom = dimensionResource(id = R.dimen.major_100)
                ),
                text = stringResource(id = R.string.inbox_note_survey_actioned),
                style = MaterialTheme.typography.body2
            )
        } else {
            actions.forEachIndexed { index, inboxNoteActionUi ->
                when {
                    index < 2 -> InboxNoteSurveyAction(inboxNoteActionUi)
                    else -> InboxNoteTextAction(inboxNoteActionUi)
                }
            }
        }
    }
}

@Composable
fun InboxNoteTextAction(inboxAction: InboxNoteActionUi) {
    TextButton(
        onClick = { inboxAction.onClick(inboxAction.id, inboxAction.parentNoteId) },
        enabled = !inboxAction.isDismissing
    ) {
        if (inboxAction.isDismissing) {
            CircularProgressIndicator(
                modifier = Modifier.size(dimensionResource(id = R.dimen.major_150)),
                color = colorResource(id = inboxAction.textColor)
            )
        } else {
            Text(
                text = inboxAction.label.uppercase(),
                color = colorResource(id = inboxAction.textColor)
            )
        }
    }
}

@Composable
fun InboxNoteSurveyAction(inboxAction: InboxNoteActionUi) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = { inboxAction.onClick(inboxAction.id, inboxAction.parentNoteId) },
            border = BorderStroke(
                dimensionResource(id = R.dimen.minor_10),
                colorResource(id = R.color.color_on_surface_disabled)
            ),
            shape = RoundedCornerShape(20),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.background,
            )
        ) {
            Text(
                text = inboxAction.label,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.subtitle1
            )
        }
    }
}

@Composable
fun InboxSkeletons() {
    val numberOfInboxSkeletonRows = 4
    LazyColumn {
        repeat(numberOfInboxSkeletonRows) {
            item {
                InboxNoteItemSkeleton()
                Divider(
                    color = colorResource(id = R.color.divider_color),
                    thickness = dimensionResource(id = R.dimen.minor_10)
                )
            }
        }
    }
}

@Composable
fun InboxNoteItemSkeleton() {
    Column(modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))) {
        InboxNoteHeaderSkeleton()
        Spacer(modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100)))
        InboxNoteContentRowsSkeleton()
        Spacer(modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_85)))
        InboxNoteButtonsSkeleton()
    }
}

@Composable
private fun InboxNoteHeaderSkeleton() {
    SkeletonView(
        width = dimensionResource(id = R.dimen.skeleton_text_medium_width),
        height = dimensionResource(id = R.dimen.major_100)
    )
    Spacer(modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_125)))
    SkeletonView(
        width = dimensionResource(id = R.dimen.skeleton_text_large_width),
        height = dimensionResource(id = R.dimen.major_100)
    )
}

@Composable
private fun InboxNoteContentRowsSkeleton() {
    SkeletonView(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(id = R.dimen.major_100))
    )
    Spacer(modifier = Modifier.padding(top = dimensionResource(id = R.dimen.minor_75)))
    SkeletonView(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(id = R.dimen.major_100))
    )
    Spacer(modifier = Modifier.padding(top = dimensionResource(id = R.dimen.minor_75)))
    SkeletonView(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(id = R.dimen.major_100))
    )
}

@Composable
private fun InboxNoteButtonsSkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
    ) {
        SkeletonView(
            width = dimensionResource(id = R.dimen.skeleton_text_large_width),
            height = dimensionResource(id = R.dimen.major_100)
        )
        SkeletonView(
            width = dimensionResource(id = R.dimen.major_350),
            height = dimensionResource(id = R.dimen.major_100)
        )
    }
}

@Preview
@Composable
private fun InboxPreview(@PreviewParameter(SampleInboxProvider::class, 1) state: InboxState) {
    InboxScreen(state)
}

@Preview
@Composable
private fun EmptyInboxPreview() {
    InboxEmptyCase()
}

class SampleInboxProvider : PreviewParameterProvider<InboxState> {
    override val values = sequenceOf(
        InboxState(
            isLoading = false,
            notes = listOf(
                InboxNoteUi(
                    id = 1,
                    title = "Install the Facebook free extension",
                    description = "description",
                    dateCreated = "5h ago",
                    actions = listOf(
                        InboxNoteActionUi(
                            id = 3,
                            parentNoteId = 1,
                            label = "Open",
                            textColor = R.color.color_secondary,
                            onClick = { _, _ -> },
                            url = "",
                        ),
                        InboxNoteActionUi(
                            id = 4,
                            parentNoteId = 1,
                            label = "Dismiss",
                            textColor = R.color.color_surface_variant,
                            onClick = { _, _ -> },
                            url = "",
                        )
                    ),
                    isActioned = false,
                    isSurvey = false
                ),
                InboxNoteUi(
                    id = 2,
                    title = "Connect with your audience",
                    description = "Description",
                    dateCreated = "22 minutes ago",
                    actions = listOf(
                        InboxNoteActionUi(
                            id = 3,
                            parentNoteId = 2,
                            label = "Open",
                            textColor = R.color.color_secondary,
                            onClick = { _, _ -> },
                            url = "",
                        ),
                        InboxNoteActionUi(
                            id = 4,
                            parentNoteId = 2,
                            label = "Dismiss",
                            textColor = R.color.color_surface_variant,
                            onClick = { _, _ -> },
                            url = "",
                        )
                    ),
                    isActioned = false,
                    isSurvey = true
                )
            )
        )
    )
}
