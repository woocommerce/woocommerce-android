package com.woocommerce.android.model

/**
 * Represents the various stages of an action. An action is defined as any request to modify store data.
 * And example of an action would be a user moderating a review or a marking a review as read. The options
 * in this enum are used to communicate the status of processing this action to the UI in a
 * meaningful way.
 */
enum class ActionStatus {
    /**
     * Used with "optimistic" action types where the action is delayed and an UNDO snackbar is
     * presented to the user to allow them time to cancel the action before it's submitted.
     */
    PENDING {
        override fun isComplete() = false
    },
    /**
     * Requested action has been submitted to the API.
     */
    SUBMITTED {
        override fun isComplete() = false
    },
    /**
     * Requested action completed successfully.
     */
    SUCCESS {
        override fun isComplete() = true
    },
    /**
     * Requested action did not complete successfully and resulted in an error.
     */
    ERROR {
        override fun isComplete() = true
    };

    /**
     * Helper method. Returns true if the associated status is considered "completed", all work
     * is finished and it either ended in success or error.
     */
    abstract fun isComplete(): Boolean
}
