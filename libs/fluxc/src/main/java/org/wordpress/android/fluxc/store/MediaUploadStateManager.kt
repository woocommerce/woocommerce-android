package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.MediaId
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaUploadStateManager @Inject constructor() {
    private val uploadStates = ConcurrentHashMap<MediaId, UploadState>()

    sealed class UploadState {
        data class Uploading(val progress: Float) : UploadState()
        data object Completed : UploadState()
        data class Failed(val error: MediaError) : UploadState()
    }

    fun startUpload(id: MediaId, progress: Float = 0f) {
        uploadStates[id] = UploadState.Uploading(progress)
    }

    fun setProgress(id: MediaId, progress: Float) {
        val current = uploadStates[id]
        if (current is UploadState.Uploading) {
            uploadStates[id] = UploadState.Uploading(progress)
        }
    }

    fun completeUpload(id: MediaId) {
        uploadStates[id] = UploadState.Completed
    }

    fun failUpload(id: MediaId, error: MediaError) {
        uploadStates[id] = UploadState.Failed(error)
    }

    fun getUploadState(id: MediaId): UploadState? {
        return uploadStates[id]
    }

    fun remove(id: MediaId) {
        uploadStates.remove(id)
    }

    fun clear() {
        uploadStates.clear()
    }
}
