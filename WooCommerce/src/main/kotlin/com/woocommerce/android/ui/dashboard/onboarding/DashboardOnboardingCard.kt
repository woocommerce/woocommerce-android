package com.woocommerce.android.ui.dashboard.onboarding

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.compose.viewModelWithFactory
import com.woocommerce.android.ui.dashboard.DashboardFragmentDirections
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.onboarding.DashboardOnboardingViewModel.Factory
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.onboarding.AboutYourStoreTaskRes
import com.woocommerce.android.ui.onboarding.NavigateToAboutYourStore
import com.woocommerce.android.ui.onboarding.NavigateToAddProduct
import com.woocommerce.android.ui.onboarding.NavigateToDomains
import com.woocommerce.android.ui.onboarding.NavigateToLaunchStore
import com.woocommerce.android.ui.onboarding.NavigateToOnboardingFullScreen
import com.woocommerce.android.ui.onboarding.NavigateToSetupPayments
import com.woocommerce.android.ui.onboarding.NavigateToSetupWooPayments
import com.woocommerce.android.ui.onboarding.NavigateToSurvey
import com.woocommerce.android.ui.onboarding.OnboardingState
import com.woocommerce.android.ui.onboarding.OnboardingTaskList
import com.woocommerce.android.ui.onboarding.OnboardingTaskUi
import com.woocommerce.android.ui.onboarding.ShowNameYourStoreDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent

@Composable
fun DashboardOnboardingCard(
    parentViewModel: DashboardViewModel,
    openOnboardingInFullScreen: () -> Unit,
    navigateToAddProduct: () -> Unit,
    onboardingViewModel: DashboardOnboardingViewModel =
        viewModelWithFactory<DashboardOnboardingViewModel, Factory>(
            creationCallback = {
                it.create(parentViewModel)
            }
        )
) {
    onboardingViewModel.viewState.observeAsState().value?.let { onboardingState ->
        StoreOnboardingCardContent(
            onboardingState = onboardingState,
            onViewAllClicked = onboardingViewModel::viewAllClicked,
            onShareFeedbackClicked = onboardingViewModel::onShareFeedbackClicked,
            onTaskClicked = onboardingViewModel::onTaskClicked,
            onHideOnboardingClicked = onboardingViewModel::onHideOnboardingClicked
        )
    }
    HandleEvents(
        onboardingViewModel.event,
        openOnboardingInFullScreen = openOnboardingInFullScreen,
        navigateToAddProduct = navigateToAddProduct
    )
}

@Composable
private fun HandleEvents(
    event: LiveData<MultiLiveEvent.Event>,
    openOnboardingInFullScreen: () -> Unit,
    navigateToAddProduct: () -> Unit
) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(event, navController, lifecycleOwner) {
        val observer = Observer { event: MultiLiveEvent.Event ->
            when (event) {

                is NavigateToOnboardingFullScreen -> openOnboardingInFullScreen()
                is NavigateToSurvey ->
                    NavGraphMainDirections.actionGlobalFeedbackSurveyFragment(SurveyType.STORE_ONBOARDING).apply {
                        navController.navigateSafely(this)
                    }

                is NavigateToLaunchStore ->
                    navController.navigateSafely(
                        directions = DashboardFragmentDirections.actionDashboardToLaunchStoreFragment()
                    )

                is NavigateToDomains ->
                    navController.navigateSafely(
                        directions = DashboardFragmentDirections.actionDashboardToNavGraphDomainChange()
                    )

                is NavigateToAddProduct -> navigateToAddProduct()

                is NavigateToSetupPayments ->
                    navController.navigateSafely(
                        directions = DashboardFragmentDirections.actionDashboardToPaymentsPreSetupFragment(
                            taskId = NavigateToSetupPayments.taskId
                        )
                    )

                is NavigateToSetupWooPayments ->
                    navController.navigateSafely(
                        directions = DashboardFragmentDirections.actionDashboardToWooPaymentsSetupInstructionsFragment()
                    )

                is NavigateToAboutYourStore ->
                    navController.navigateSafely(
                        DashboardFragmentDirections.actionDashboardToAboutYourStoreFragment()
                    )

                is ShowNameYourStoreDialog -> {
                    navController.navigateSafely(
                        DashboardFragmentDirections.actionDashboardToNameYourStoreDialogFragment(fromOnboarding = true)
                    )
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
fun StoreOnboardingCardContent(
    onboardingState: OnboardingState,
    onViewAllClicked: () -> Unit,
    onShareFeedbackClicked: () -> Unit,
    onTaskClicked: (OnboardingTaskUi) -> Unit,
    modifier: Modifier = Modifier,
    onHideOnboardingClicked: () -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface)
        ) {
            Text(
                modifier = Modifier.padding(
                    top = dimensionResource(id = R.dimen.major_100),
                    start = dimensionResource(id = R.dimen.major_100)
                ),
                text = stringResource(id = onboardingState.title),
                style = MaterialTheme.typography.h6,
            )
            @Suppress("MagicNumber")
            (OnboardingTaskCollapsedProgressHeader(
                tasks = onboardingState.tasks,
                modifier = Modifier
                    .padding(
                        top = dimensionResource(id = R.dimen.major_100),
                        start = dimensionResource(id = R.dimen.major_100),
                        end = dimensionResource(id = R.dimen.major_100)
                    )
                    .fillMaxWidth(0.5f)
            ))
            val taskToDisplay = if (onboardingState.tasks.filter { !it.isCompleted }.size == 1) {
                onboardingState.tasks.filter { !it.isCompleted }
            } else {
                onboardingState.tasks.take(DashboardOnboardingViewModel.NUMBER_ITEMS_IN_COLLAPSED_MODE)
            }
            OnboardingTaskList(
                tasks = taskToDisplay,
                onTaskClicked = onTaskClicked,
                modifier = Modifier
                    .padding(top = dimensionResource(id = R.dimen.major_100))
                    .fillMaxWidth()
            )
            if (onboardingState.tasks.size > DashboardOnboardingViewModel.NUMBER_ITEMS_IN_COLLAPSED_MODE || taskToDisplay.size == 1) {
                WCTextButton(
                    contentPadding = PaddingValues(dimensionResource(id = R.dimen.major_100)),
                    onClick = onViewAllClicked
                ) {
                    Text(
                        text = stringResource(R.string.store_onboarding_task_view_all, onboardingState.tasks.size),
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = dimensionResource(id = R.dimen.minor_100))
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
            contentDescription = stringResource(R.string.more_menu),
        )
    }
    DropdownMenu(
        offset = DpOffset(
            x = dimensionResource(id = R.dimen.major_100),
            y = 0.dp
        ),
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            modifier = Modifier
                .height(dimensionResource(id = R.dimen.major_175)),
            onClick = {
                showMenu = false
                onShareFeedbackClicked()
            }
        ) {
            Text(stringResource(id = R.string.store_onboarding_menu_share_feedback))
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        DropdownMenuItem(
            modifier = Modifier
                .height(dimensionResource(id = R.dimen.major_175)),
            onClick = {
                showMenu = false
                onHideOnboardingClicked()
            }
        ) {
            Text(stringResource(id = R.string.store_onboarding_menu_hide_store_setup))
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
            backgroundColor = colorResource(id = R.color.divider_color),
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
    StoreOnboardingCardContent(
        OnboardingState(
            show = true,
            title = R.string.store_onboarding_title,
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
