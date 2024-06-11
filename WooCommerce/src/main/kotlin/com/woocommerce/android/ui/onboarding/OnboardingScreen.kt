package com.woocommerce.android.ui.onboarding

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCTag
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.onboarding.StoreOnboardingViewModel.Companion.NUMBER_ITEMS_IN_COLLAPSED_MODE
import com.woocommerce.android.ui.onboarding.StoreOnboardingViewModel.OnboardingState

@Composable
fun StoreOnboardingScreen(viewModel: StoreOnboardingViewModel) {
    viewModel.viewState.observeAsState().value?.let { onboardingState ->
        Scaffold(topBar = {
            Toolbar(onNavigationButtonClick = viewModel::onBackPressed)
        }) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colors.surface)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        top = dimensionResource(id = dimen.major_100),
                        bottom = dimensionResource(id = dimen.major_100)
                    )
            ) {
                OnboardingTaskProgressHeader(
                    titleStringRes = onboardingState.title,
                    tasks = onboardingState.tasks
                )
                OnboardingTaskList(
                    tasks = onboardingState.tasks,
                    onTaskClicked = viewModel::onTaskClicked,
                    modifier = Modifier
                        .padding(top = dimensionResource(id = dimen.major_100))
                        .fillMaxWidth()
                )
            }
        }
    }
}

// TODO to be removed once dynamic dashboard is enabled
@Composable
fun StoreOnboardingCollapsed(
    onboardingState: OnboardingState,
    onViewAllClicked: () -> Unit,
    onShareFeedbackClicked: () -> Unit,
    onTaskClicked: (OnboardingTaskUi) -> Unit,
    modifier: Modifier = Modifier,
    onHideOnboardingClicked: () -> Unit,
    numberOfItemsToShowInCollapsedMode: Int = NUMBER_ITEMS_IN_COLLAPSED_MODE,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface)
        ) {
            Text(
                modifier = Modifier.padding(
                    top = dimensionResource(id = dimen.major_100),
                    start = dimensionResource(id = dimen.major_100)
                ),
                text = stringResource(id = onboardingState.title),
                style = MaterialTheme.typography.h6,
            )
            @Suppress("MagicNumber")
            OnboardingTaskCollapsedProgressHeader(
                tasks = onboardingState.tasks,
                modifier = Modifier
                    .padding(
                        top = dimensionResource(id = dimen.major_100),
                        start = dimensionResource(id = dimen.major_100),
                        end = dimensionResource(id = dimen.major_100)
                    )
                    .fillMaxWidth(0.5f)
            )
            val taskToDisplay = if (onboardingState.tasks.filter { !it.isCompleted }.size == 1) {
                onboardingState.tasks.filter { !it.isCompleted }
            } else {
                onboardingState.tasks.take(numberOfItemsToShowInCollapsedMode)
            }
            OnboardingTaskList(
                tasks = taskToDisplay,
                onTaskClicked = onTaskClicked,
                modifier = Modifier
                    .padding(top = dimensionResource(id = dimen.major_100))
                    .fillMaxWidth()
            )
            if (onboardingState.tasks.size > NUMBER_ITEMS_IN_COLLAPSED_MODE || taskToDisplay.size == 1) {
                WCTextButton(
                    contentPadding = PaddingValues(dimensionResource(id = dimen.major_100)),
                    onClick = onViewAllClicked
                ) {
                    Text(
                        text = stringResource(string.store_onboarding_task_view_all, onboardingState.tasks.size),
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = dimensionResource(id = dimen.minor_100))
        ) {
            OnboardingMoreMenu(onShareFeedbackClicked, onHideOnboardingClicked)
        }
    }
}

@Composable
private fun OnboardingMoreMenu(
    onShareFeedbackClicked: () -> Unit,
    onHideOnboardingClicked: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    IconButton(onClick = { showMenu = !showMenu }) {
        Icon(
            imageVector = Outlined.MoreVert,
            contentDescription = stringResource(string.more_menu),
        )
    }
    DropdownMenu(
        offset = DpOffset(
            x = dimensionResource(id = dimen.major_100),
            y = 0.dp
        ),
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            modifier = Modifier
                .height(dimensionResource(id = dimen.major_175)),
            onClick = {
                showMenu = false
                onShareFeedbackClicked()
            }
        ) {
            Text(stringResource(id = string.store_onboarding_menu_share_feedback))
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = dimen.minor_100)))
        DropdownMenuItem(
            modifier = Modifier
                .height(dimensionResource(id = dimen.major_175)),
            onClick = {
                showMenu = false
                onHideOnboardingClicked()
            }
        ) {
            Text(stringResource(id = string.store_onboarding_menu_hide_store_setup))
        }
    }
}

@Composable
fun OnboardingTaskList(
    tasks: List<OnboardingTaskUi>,
    onTaskClicked: (OnboardingTaskUi) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        tasks.forEachIndexed { index, task ->
            TaskItem(task, onTaskClicked)
            if (index < tasks.size - 1) {
                Divider(
                    color = colorResource(id = R.color.divider_color),
                    thickness = dimensionResource(id = dimen.minor_10)
                )
            }
        }
    }
}

@Composable
fun TaskItem(
    task: OnboardingTaskUi,
    onTaskClicked: (OnboardingTaskUi) -> Unit
) {
    Row(
        modifier = when {
            !task.isCompleted -> Modifier.clickable { onTaskClicked(task) }
            else -> Modifier
        }.padding(dimensionResource(id = dimen.major_100)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.major_100))
    ) {
        Image(
            modifier = Modifier.fillMaxHeight(),
            painter = painterResource(
                id = if (task.isCompleted) {
                    R.drawable.ic_onboarding_task_completed
                } else {
                    task.taskUiResources.icon
                }
            ),
            contentDescription = "",
            colorFilter =
            if (!task.isCompleted) {
                ColorFilter.tint(color = colorResource(id = R.color.color_icon))
            } else {
                null
            }
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.minor_75))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = task.taskUiResources.title),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )
                if (!task.isCompleted && task.taskUiResources is LaunchStoreTaskRes) {
                    WCTag(
                        text = stringResource(id = string.store_onboarding_launch_store_task_private_tag).uppercase(),
                        modifier = Modifier.padding(start = dimensionResource(id = dimen.minor_100))
                    )
                }
            }
            Text(
                text = stringResource(id = task.taskUiResources.description),
                style = MaterialTheme.typography.body1,
            )
        }
        if (!task.isCompleted) {
            Image(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = ""
            )
        }
    }
}

@Composable
fun OnboardingTaskCollapsedProgressHeader(
    tasks: List<OnboardingTaskUi>,
    modifier: Modifier = Modifier
) {
    val completedTasks = tasks.count { it.isCompleted }
    val totalTasks = tasks.count()
    val progress by remember { mutableFloatStateOf(if (totalTasks == 0) 0f else completedTasks / totalTasks.toFloat()) }
    val animatedProgress = animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    ).value
    Column(modifier) {
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .height(dimensionResource(id = dimen.minor_100))
                .clip(RoundedCornerShape(dimensionResource(id = dimen.minor_100))),
            backgroundColor = colorResource(id = R.color.divider_color),
        )
        Text(
            modifier = Modifier.padding(top = dimensionResource(id = dimen.minor_100)),
            text = stringResource(string.store_onboarding_completed_tasks_status, completedTasks, totalTasks),
            style = MaterialTheme.typography.body2,
        )
    }
}

@Composable
fun OnboardingTaskProgressHeader(
    titleStringRes: Int,
    tasks: List<OnboardingTaskUi>,
    modifier: Modifier = Modifier
) {
    val completedTasks = tasks.count { it.isCompleted }
    val totalTasks = tasks.count()
    val progress by remember { mutableFloatStateOf(if (totalTasks == 0) 0f else completedTasks / totalTasks.toFloat()) }
    val animatedProgress = animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    ).value
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(id = dimen.major_150)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(bottom = dimensionResource(id = dimen.major_100)),
            text = stringResource(id = titleStringRes),
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
        )
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .height(dimensionResource(id = dimen.minor_100))
                .clip(RoundedCornerShape(dimensionResource(id = dimen.minor_100))),
            backgroundColor = colorResource(id = R.color.divider_color),
        )
        Text(
            modifier = Modifier.padding(top = dimensionResource(id = dimen.minor_100)),
            text = stringResource(
                string.store_onboarding_completed_tasks_full_screen_status,
                completedTasks,
                totalTasks
            ),
            style = MaterialTheme.typography.body1,
        )
    }
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Suppress("unused")
@Composable
private fun OnboardingPreview() {
    StoreOnboardingCollapsed(
        OnboardingState(
            show = true,
            title = string.store_onboarding_title,
            tasks = listOf(
                OnboardingTaskUi(
                    taskUiResources = AboutYourStoreTaskRes,
                    isCompleted = false,
                ),
                OnboardingTaskUi(
                    taskUiResources = AboutYourStoreTaskRes,
                    isCompleted = true,
                ),
                OnboardingTaskUi(
                    taskUiResources = AboutYourStoreTaskRes,
                    isCompleted = false,
                )
            )
        ),
        onViewAllClicked = {},
        onShareFeedbackClicked = {},
        onHideOnboardingClicked = {},
        onTaskClicked = {}
    )
}
