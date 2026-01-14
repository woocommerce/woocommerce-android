package org.wordpress.android.fluxc.store

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.MediaId
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType
import org.wordpress.android.fluxc.store.MediaUploadStateManager.UploadState

class MediaUploadStateManagerTest {
    private lateinit var stateManager: MediaUploadStateManager

    @Before
    fun setup() {
        stateManager = MediaUploadStateManager()
    }

    @Test
    fun `when starting upload, then it stores uploading state`() {
        val mediaId = MediaId(123)
        stateManager.startUpload(mediaId, 0.5f)

        val state = stateManager.getUploadState(mediaId)
        assertThat(state).isEqualTo(UploadState.Uploading(0.5f))
    }

    @Test
    fun `when setting progress, then it updates progress for uploading state`() {
        val mediaId = MediaId(123)
        stateManager.startUpload(mediaId, 0.0f)
        stateManager.setProgress(mediaId, 0.75f)

        val state = stateManager.getUploadState(mediaId)

        assertThat(state).isEqualTo(UploadState.Uploading(0.75f))
    }

    @Test
    fun `when setting progress for non-existent ID, then it does nothing`() {
        val mediaId = MediaId(999)
        stateManager.setProgress(mediaId, 0.5f)

        val state = stateManager.getUploadState(mediaId)

        assertThat(state).isNull()
    }

    @Test
    fun `when setting progress for completed upload, then it does nothing`() {
        val mediaId = MediaId(123)
        stateManager.completeUpload(mediaId)
        stateManager.setProgress(mediaId, 0.5f)

        val state = stateManager.getUploadState(mediaId)

        assertThat(state).isEqualTo(UploadState.Completed)
    }

    @Test
    fun `when failing upload, then it transitions to failed state with error`() {
        val mediaId = MediaId(123)
        stateManager.startUpload(mediaId, 0.5f)
        val error = MediaError(MediaErrorType.GENERIC_ERROR, "Test error")
        stateManager.failUpload(mediaId, error)

        val state = stateManager.getUploadState(mediaId)

        assertThat(state).isEqualTo(UploadState.Failed(error))
    }

    @Test
    fun `when failing upload without prior state, then it stores failed state`() {
        val mediaId = MediaId(123)
        val error = MediaError(MediaErrorType.GENERIC_ERROR, "Test error")
        stateManager.failUpload(mediaId, error)

        val state = stateManager.getUploadState(mediaId)

        assertThat(state).isEqualTo(UploadState.Failed(error))
    }

    @Test
    fun `when completing upload, then it stores completed state`() {
        val mediaId = MediaId(123)
        stateManager.startUpload(mediaId, 0.5f)
        stateManager.completeUpload(mediaId)

        val state = stateManager.getUploadState(mediaId)

        assertThat(state).isEqualTo(UploadState.Completed)
    }

    @Test
    fun `when removing, then it deletes state`() {
        val mediaId = MediaId(123)
        stateManager.startUpload(mediaId, 0.5f)
        stateManager.remove(mediaId)

        val state = stateManager.getUploadState(mediaId)
        assertThat(state).isNull()
    }

    @Test
    fun `when removing non-existent ID, then it does nothing`() {
        val mediaId = MediaId(999)
        stateManager.remove(mediaId)
    }

    @Test
    fun `when clearing, then it removes all states`() {
        stateManager.startUpload(MediaId(1), 0.1f)
        stateManager.startUpload(MediaId(2), 0.2f)
        stateManager.completeUpload(MediaId(3))

        stateManager.clear()

        assertThat(stateManager.getUploadState(MediaId(1))).isNull()
        assertThat(stateManager.getUploadState(MediaId(2))).isNull()
        assertThat(stateManager.getUploadState(MediaId(3))).isNull()
    }

    @Test
    fun `when storing multiple media, then they can have independent states`() {
        val id1 = MediaId(1)
        val id2 = MediaId(2)
        val id3 = MediaId(3)
        val error = MediaError(MediaErrorType.GENERIC_ERROR, "Test error")

        stateManager.startUpload(id1, 0.3f)
        stateManager.completeUpload(id2)
        stateManager.startUpload(id3, 0.5f)
        stateManager.failUpload(id3, error)

        val state1 = stateManager.getUploadState(id1)
        val state2 = stateManager.getUploadState(id2)
        val state3 = stateManager.getUploadState(id3)

        assertThat(state1).isEqualTo(UploadState.Uploading(0.3f))
        assertThat(state2).isEqualTo(UploadState.Completed)
        assertThat(state3).isEqualTo(UploadState.Failed(error))
    }

    @Test
    fun `when completing upload, then it overwrites existing state`() {
        val mediaId = MediaId(123)
        stateManager.startUpload(mediaId, 0.5f)
        stateManager.completeUpload(mediaId)

        val state = stateManager.getUploadState(mediaId)
        assertThat(state).isEqualTo(UploadState.Completed)
    }

    @Test
    fun `when getting non-existent ID, then it returns null`() {
        val mediaId = MediaId(999)
        val state = stateManager.getUploadState(mediaId)
        assertThat(state).isNull()
    }
}
