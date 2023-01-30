package com.woocommerce.android.ui.login.storecreation.onboarding

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingViewModel.OnboardingTask

@Composable
@Suppress("MagicNumber")
fun StoreOnboardingScreen(
    onboardingState: StoreOnboardingViewModel.OnboardingState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Text(
            text = stringResource(id = onboardingState.title),
            style = MaterialTheme.typography.h6,
        )
        OnboardingTaskLinearProgress(
            tasks = onboardingState.tasks,
            modifier = Modifier
                .padding(top = dimensionResource(id = R.dimen.major_100))
                .fillMaxWidth(0.5f)
        )
        OnboardingTaskList(
            tasks = onboardingState.tasks,
            modifier = Modifier
                .padding(top = dimensionResource(id = R.dimen.major_100))
                .fillMaxWidth()
        )
    }
}

@Composable
fun OnboardingTaskList(
    tasks: List<OnboardingTask>,
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
                    painter = painterResource(id = task.icon),
                    contentDescription = "",
                    colorFilter = ColorFilter.tint(color = colorResource(id = R.color.color_icon))
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
            if (index < tasks.lastIndex)
                Divider(
                    color = colorResource(id = R.color.divider_color),
                    thickness = dimensionResource(id = R.dimen.minor_10)
                )
        }
    }
}

@Composable
fun OnboardingTaskLinearProgress(
    tasks: List<OnboardingTask>,
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
            modifier = with(Modifier) {
                height(dimensionResource(id = R.dimen.minor_100))
            },
            color = MaterialTheme.colors.primary,
            backgroundColor = colorResource(id = R.color.divider_color)
        )
        Text(
            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.minor_100)),
            text = stringResource(R.string.store_onboarding_completed_tasks_status, completedTasks, totalTasks),
            style = MaterialTheme.typography.body2,
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
    StoreOnboardingScreen(
        StoreOnboardingViewModel.OnboardingState(
            show = true,
            title = R.string.store_onboarding_title,
            tasks = listOf(
                OnboardingTask(
                    icon = R.drawable.ic_product,
                    title = R.string.store_onboarding_task_add_product_title,
                    description = R.string.store_onboarding_task_add_product_description,
                    isCompleted = false
                ),
                OnboardingTask(
                    icon = R.drawable.ic_product,
                    title = R.string.store_onboarding_task_launch_store_title,
                    description = R.string.store_onboarding_task_launch_store_description,
                    isCompleted = false
                ),
                OnboardingTask(
                    icon = R.drawable.ic_product,
                    title = R.string.store_onboarding_task_change_domain_title,
                    description = R.string.store_onboarding_task_change_domain_description,
                    isCompleted = false
                )
            )
        )
    )
}
