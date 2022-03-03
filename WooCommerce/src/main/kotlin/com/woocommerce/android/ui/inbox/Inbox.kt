package com.woocommerce.android.ui.inbox

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.inbox.InboxViewModel.InboxNoteUi
import com.woocommerce.android.ui.inbox.InboxViewModel.InboxState

@Composable
fun Inbox(viewModel: InboxViewModel) {
    val inboxState by viewModel.inboxState.observeAsState(InboxState())
    Inbox(state = inboxState)
}

@Composable
fun Inbox(state: InboxState) {
    when {
        state.isLoading -> InboxSkeleton()
        state.notes.isEmpty() -> InboxEmptyCase()
        state.notes.isNotEmpty() -> InboxSkeleton()
    }
}

@Composable
fun InboxEmptyCase() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.empty_inbox_title),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6
        )
        Spacer(Modifier.size(54.dp))
        Image(
            painter = painterResource(id = R.drawable.img_empty_inbox),
            contentDescription = null,
        )
        Spacer(Modifier.size(48.dp))
        Text(
            text = stringResource(id = R.string.empty_inbox_description),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
fun InboxSkeleton() {
    LazyColumn {

        /*
          Lay down the Shimmer Animated item 5 time
          [repeat] is like a loop which executes the body
          according to the number specified
        */
        repeat(4) {
            item {
                ShimmerAnimation()
                Divider(
                    color = colorResource(id = R.color.divider_color),
                    thickness = 1.dp
                )
            }
        }
    }
}

@Composable
fun InboxNotes(notes: List<InboxNoteUi>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(notes) { index, note ->
            InboxNoteRow(note = note)
            if (index < notes.lastIndex)
                Divider(
                    color = colorResource(id = R.color.divider_color),
                    thickness = 1.dp
                )
        }
    }
}

@Composable
fun InboxNoteRow(note: InboxNoteUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = note.updatedTime,
            style = MaterialTheme.typography.subtitle2,
            color = colorResource(id = R.color.color_surface_variant)
        )
        Text(
            text = note.title,
            style = MaterialTheme.typography.subtitle1
        )
        Text(
            text = note.description,
            style = MaterialTheme.typography.body2
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = { note.onCallToActionClick(note.id) }
            ) {
                Text(
                    text = note.callToActionText.uppercase(),
                    color = colorResource(id = R.color.color_secondary)
                )
            }

            TextButton(
                onClick = { note.onDismissNote(note.id) },
                Modifier.padding(start = 16.dp)
            ) {
                Text(
                    text = note.dismissText.uppercase(),
                    color = colorResource(id = R.color.color_surface_variant)
                )
            }
        }
    }
}


@Composable
fun InboxNoteSkeletonItem(
    brush: Brush
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Spacer(
            modifier = Modifier
                .width(96.dp)
                .height(16.dp)
                .background(brush = brush)
        )
        Spacer(modifier = Modifier.padding(top = 20.dp))
        Spacer(
            modifier = Modifier
                .width(190.dp)
                .height(16.dp)
                .background(brush = brush)
        )
        Spacer(modifier = Modifier.padding(top = 16.dp))
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .background(brush = brush)
        )
        Spacer(modifier = Modifier.padding(top = 6.dp))
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .background(brush = brush)
        )
        Spacer(modifier = Modifier.padding(top = 6.dp))
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .background(brush = brush)
        )
        Spacer(modifier = Modifier.padding(top = 14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(
                modifier = Modifier
                    .width(150.dp)
                    .height(16.dp)
                    .background(brush = brush)
            )
            Spacer(
                modifier = Modifier
                    .width(60.dp)
                    .height(16.dp)
                    .background(brush = brush)
            )
        }
    }
}

@Composable
fun ShimmerAnimation() {

    /*
     Create InfiniteTransition
     which holds child animation like [Transition]
     animations start running as soon as they enter
     the composition and do not stop unless they are removed
    */
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        /*
         Specify animation positions,
         initial Values 0F means it
         starts from 0 position
        */
        initialValue = 0f,
        targetValue = 1600f,
        animationSpec = infiniteRepeatable(
            // Tween Animates between values over specified [durationMillis]
            tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            RepeatMode.Restart
        )
    )

    val ShimmerColorShades = listOf(
        colorResource(id = R.color.skeleton_color),
        colorResource(id = R.color.skeleton_color).copy(0.15f),
        colorResource(id = R.color.skeleton_color)
    )
    /*
      Create a gradient using the list of colors
      Use Linear Gradient for animating in any direction according to requirement
      start=specifies the position to start with in cartesian like system Offset(10f,10f) means x(10,0) , y(0,10)
      end = Animate the end position to give the shimmer effect using the transition created above
    */
    val brush = Brush.linearGradient(
        colors = ShimmerColorShades,
        start = Offset(10f, 10f),
        end = Offset(translateAnim, translateAnim)
    )

    InboxNoteSkeletonItem(brush = brush)
}
