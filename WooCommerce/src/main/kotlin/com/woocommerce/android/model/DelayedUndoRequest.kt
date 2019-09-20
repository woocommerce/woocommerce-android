package com.woocommerce.android.model

abstract class DelayedUndoRequest(var requestStatus: RequestStatus) {
    enum class RequestStatus {
        PENDING {
            override fun isComplete() = false
        },
        SUBMITTED {
            override fun isComplete() = false
        },
        SUCCESS {
            override fun isComplete() = true
        },
        ERROR {
            override fun isComplete() = true
        };

        abstract fun isComplete(): Boolean
    }
}
