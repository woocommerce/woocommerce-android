package com.woocommerce.android.ui.login.storecreation.onboarding

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingViewModel.Companion.NUMBER_ITEMS_IN_COLLAPSED_MODE
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingViewModel.OnboardingTaskUi

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
                    .padding(dimensionResource(id = R.dimen.major_100))
            ) {
                OnboardingTaskProgressHeader(
                    titleStringRes = onboardingState.title,
                    tasks = onboardingState.tasks
                )
                OnboardingTaskList(
                    tasks = onboardingState.tasks,
                    modifier = Modifier
                        .padding(top = dimensionResource(id = R.dimen.major_100))
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun StoreOnboardingCollapsed(
    onboardingState: StoreOnboardingViewModel.OnboardingState,
    onViewAllClicked: () -> Unit,
    modifier: Modifier = Modifier,
    numberOfItemsToShowInCollapsedMode: Int = NUMBER_ITEMS_IN_COLLAPSED_MODE,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Text(
            text = stringResource(id = onboardingState.title),
            style = MaterialTheme.typography.h6,
        )
        @Suppress("MagicNumber")
        OnboardingTaskCollapsedProgressHeader(
            tasks = onboardingState.tasks,
            modifier = Modifier
                .padding(top = dimensionResource(id = R.dimen.major_100))
                .fillMaxWidth(0.5f)
        )
        OnboardingTaskList(
            tasks = onboardingState.tasks.take(numberOfItemsToShowInCollapsedMode),
            modifier = Modifier
                .padding(top = dimensionResource(id = R.dimen.major_100))
                .fillMaxWidth()
        )
        Text(
            modifier = Modifier.clickable { onViewAllClicked() },
            text = stringResource(R.string.store_onboarding_task_view_all, onboardingState.tasks.size),
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary,
        )
    }
}

@Composable
fun OnboardingTaskList(
    tasks: List<OnboardingTaskUi>,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        tasks.forEachIndexed { index, task ->
            Row(
                modifier = modifier.padding(bottom = dimensionResource(id = R.dimen.major_100)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
            ) {
                Image(
                    modifier = Modifier.fillMaxHeight(),
                    painter = painterResource(
                        id = if (task.isCompleted)
                            R.drawable.ic_onboarding_task_completed
                        else task.icon
                    ),
                    contentDescription = "",
                    colorFilter =
                    if (!task.isCompleted)
                        ColorFilter.tint(color = colorResource(id = R.color.color_icon))
                    else null
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = task.title),
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        modifier = Modifier.padding(top = dimensionResource(id = R.dimen.minor_75)),
                        text = stringResource(id = task.description),
                        style = MaterialTheme.typography.body1,
                    )
                }
                Image(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = ""
                )
            }
            if (index < tasks.size - 1)
                Divider(
                    color = colorResource(id = R.color.divider_color),
                    thickness = dimensionResource(id = R.dimen.minor_10)
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
    val progress by remember { mutableStateOf(completedTasks / totalTasks.toFloat()) }
    val animatedProgress = animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    ).value
    Column(modifier) {
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .height(dimensionResource(id = R.dimen.minor_100))
                .clip(RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))),
            color = MaterialTheme.colors.primary,
            backgroundColor = colorResource(id = R.color.divider_color),
        )
        Text(
            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.minor_100)),
            text = stringResource(R.string.store_onboarding_completed_tasks_status, completedTasks, totalTasks),
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
    val progress by remember { mutableStateOf(completedTasks / totalTasks.toFloat()) }
    val animatedProgress = animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    ).value
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(id = R.dimen.major_150)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.major_100)),
            text = stringResource(id = titleStringRes),
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
        )
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .height(dimensionResource(id = R.dimen.minor_100))
                .clip(RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))),
            color = MaterialTheme.colors.primary,
            backgroundColor = colorResource(id = R.color.divider_color),
        )
        Text(
            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.minor_100)),
            text = stringResource(
                R.string.store_onboarding_completed_tasks_full_screen_status,
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
        StoreOnboardingViewModel.OnboardingState(
            show = true,
            title = R.string.store_onboarding_title,
            tasks = listOf(
                OnboardingTaskUi(
                    icon = R.drawable.ic_product,
                    title = R.string.store_onboarding_task_add_product_title,
                    description = R.string.store_onboarding_task_add_product_description,
                    isCompleted = false,
                ),
                OnboardingTaskUi(
                    icon = R.drawable.ic_product,
                    title = R.string.store_onboarding_task_launch_store_title,
                    description = R.string.store_onboarding_task_launch_store_description,
                    isCompleted = true,
                ),
                OnboardingTaskUi(
                    icon = R.drawable.ic_product,
                    title = R.string.store_onboarding_task_change_domain_title,
                    description = R.string.store_onboarding_task_change_domain_description,
                    isCompleted = false,
                )
            )
        ),
        onViewAllClicked = {}
    )
}
