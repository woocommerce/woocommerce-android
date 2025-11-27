package com.woocommerce.android.ui.bookings.filter.teammember

import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.bookings.filter.BookingFilterListItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import org.wordpress.android.fluxc.persistence.entity.BookingResourceEntity

data class BookingTeamMemberFilterUiState(
    val isLoading: Boolean = true,
    val teamMembers: List<TeamMember?> = emptyList(),
    val selectedMembers: BookingsFilterOption.TeamMembers = BookingsFilterOption.TeamMembers.DEFAULT,
    val onTeamMemberSelected: (TeamMember?) -> Unit = {},
) {
    val items: List<BookingFilterListItem> = teamMembers.map { member ->
        BookingFilterListItem(
            title = member?.name?.let { UiString.UiStringText(it) }
                ?: UiString.UiStringRes(R.string.bookings_filter_default),
            selected = isSelected(member),
            onClick = { onTeamMemberSelected(member) }
        )
    }

    private fun isSelected(teamMember: TeamMember?): Boolean = if (teamMember == TeamMember.any) {
        selectedMembers.values.isEmpty()
    } else {
        selectedMembers.values.contains(teamMember?.id)
    }

    val skeletonVisible: Boolean = isLoading && teamMembers.filterNotNull().isEmpty()
}

val BookingResourceEntity.Companion.any: BookingResourceEntity?
    get() = null

typealias TeamMember = BookingResourceEntity
