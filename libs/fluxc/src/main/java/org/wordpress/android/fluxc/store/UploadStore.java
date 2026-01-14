package org.wordpress.android.fluxc.store;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.action.UploadAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaId;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload;
import org.wordpress.android.fluxc.store.MediaUploadStateManager.UploadState;
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType;
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType.Type;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UploadStore extends Store {
    private final MediaUploadStateManager mUploadStateManager;

    @Inject public UploadStore(Dispatcher dispatcher, MediaUploadStateManager uploadStateManager) {
        super(dispatcher);
        mUploadStateManager = uploadStateManager;
    }

    @Override
    public void onRegister() {
        AppLog.d(T.API, "UploadStore onRegister");
    }

    // Ensure that events reach the UploadStore before their main stores (MediaStore)
    @Subscribe(threadMode = ThreadMode.ASYNC, priority = 1)
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (actionType instanceof UploadAction) {
            onUploadAction((UploadAction) actionType, action.getPayload());
        } else if (actionType instanceof MediaAction) {
            onMediaAction((MediaAction) actionType, action.getPayload());
        }
    }

    private void onUploadAction(UploadAction actionType, Object payload) {
        switch (actionType) {
            case UPLOADED_MEDIA:
                handleMediaUploaded((ProgressPayload) payload);
                mDispatcher.dispatch(MediaActionBuilder.newUploadedMediaAction((ProgressPayload) payload));
                break;
        }
    }

    @SuppressWarnings("EnumSwitchStatementWhichMissesCases")
    private void onMediaAction(MediaAction actionType, @NonNull Object payload) {
        switch (actionType) {
            case UPLOAD_MEDIA:
                handleUploadMedia((MediaPayload) payload);
                break;
            case CANCEL_MEDIA_UPLOAD:
                handleCancelMedia((CancelMediaPayload) payload);
                break;
            case UPDATE_MEDIA:
                handleUpdateMedia((MediaModel) payload);
                break;
        }
    }

    private void handleUploadMedia(@NonNull MediaPayload payload) {
        if (payload.media == null) {
            return;
        }
        MediaId mediaId = new MediaId(payload.media.getId());
        MalformedMediaArgSubType argError = MediaUtils.getMediaValidationErrorType(payload.media);

        if (argError.getType() != Type.NO_ERROR) {
            MediaError error = new MediaError(
                    MediaErrorType.MALFORMED_MEDIA_ARG,
                    argError.getType().getErrorLogDescription(),
                    argError
            );
            mUploadStateManager.failUpload(mediaId, error);
        } else {
            mUploadStateManager.startUpload(mediaId);
        }
    }

    private void handleMediaUploaded(@NonNull ProgressPayload payload) {
        if (payload.media == null) {
            return;
        }

        MediaId mediaId = new MediaId(payload.media.getId());
        UploadState currentState = mUploadStateManager.getUploadState(mediaId);

        if (currentState == null) {
            if (!payload.isError() && !payload.canceled && !payload.completed) {
                // This is a progress event, and the upload seems to have already been cancelled
                // We don't want to store a new state in this case, just move on
                return;
            }
        }

        if (payload.isError() || payload.canceled) {
            MediaError error = payload.isError() ? payload.error :
                    new MediaError(MediaErrorType.GENERIC_ERROR, "Upload cancelled");
            mUploadStateManager.failUpload(mediaId, error);
            return;
        }

        if (payload.completed) {
            mUploadStateManager.completeUpload(mediaId);
        } else {
            // Only update progress if it's higher than current
            if (currentState instanceof UploadState.Uploading) {
                float currentProgress = ((UploadState.Uploading) currentState).getProgress();
                if (currentProgress < payload.progress) {
                    mUploadStateManager.setProgress(mediaId, payload.progress);
                }
            } else {
                // No current state, start upload with this progress
                mUploadStateManager.startUpload(mediaId, payload.progress);
            }
        }
    }

    private void handleCancelMedia(@NonNull CancelMediaPayload payload) {
        // If the cancel action has the delete flag, we should remove the upload state
        // Otherwise, mark it as FAILED
        MediaId mediaId = new MediaId(payload.media.getId());
        if (payload.delete) {
            mUploadStateManager.remove(mediaId);
        } else {
            MediaError error = new MediaError(MediaErrorType.GENERIC_ERROR, "Upload cancelled");
            mUploadStateManager.failUpload(mediaId, error);
        }
    }

    private void handleUpdateMedia(@NonNull MediaModel payload) {
        MediaId mediaId = new MediaId(payload.getId());
        UploadState currentState = mUploadStateManager.getUploadState(mediaId);
        if (currentState == null) {
            return;
        }

        // If the new MediaModel state is different from ours, update our state to reflect it
        MediaUploadState newUploadState = MediaUploadState.fromString(payload.getUploadState());

        if (currentState instanceof UploadState.Uploading) {
            if (newUploadState == MediaUploadState.FAILED) {
                MediaError error = new MediaError(MediaErrorType.GENERIC_ERROR);
                mUploadStateManager.failUpload(mediaId, error);
            }
        } else if (currentState instanceof UploadState.Completed) {
            // We never care about changes to MediaModels that are already COMPLETED
        } else if (currentState instanceof UploadState.Failed) {
            if (newUploadState == MediaUploadState.UPLOADING || newUploadState == MediaUploadState.QUEUED) {
                mUploadStateManager.startUpload(mediaId);
            }
        }
    }
}
