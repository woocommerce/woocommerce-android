package org.wordpress.android.fluxc.network.rest.wpcom.wc.onboarding

data class TaskGroupDto(
    val id: String,
    val tasks: List<TaskDto>
)

data class TaskDto(
    val id: String,
    val isComplete: Boolean = false,
    val isVisited: Boolean = false,
    val isActioned: Boolean = false,
    val canView: Boolean = false,
    val actionUrl: String?
)

