package org.wordpress.android.fluxc.store

enum class MediaUploadState {
    QUEUED,
    UPLOADING,
    DELETING,
    DELETED,
    FAILED,
    UPLOADED;

    companion object {
        @JvmStatic
        fun fromString(stringState: String?): MediaUploadState {
            if (stringState != null) {
                entries.forEach { state ->
                    if (stringState.equals(state.toString(), ignoreCase = true)) {
                        return state
                    }
                }
            }
            return UPLOADED
        }
    }
}
