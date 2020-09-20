package com.woocommerce.android.ui.orders

import com.woocommerce.android.viewmodel.MultiLiveEvent.Event

sealed class OrderNavigationTarget : Event() {
    data class ViewOrderStatusSelector(
        val currentStatus: String,
        val orderStatusList: Array<String>
    ) : OrderNavigationTarget() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ViewOrderStatusSelector

            if (currentStatus != other.currentStatus) return false
            if (!orderStatusList.contentEquals(other.orderStatusList)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = currentStatus.hashCode()
            result = 31 * result + orderStatusList.contentHashCode()
            return result
        }
    }

    data class IssueOrderRefund(val remoteOrderId: Long) : OrderNavigationTarget()
}
