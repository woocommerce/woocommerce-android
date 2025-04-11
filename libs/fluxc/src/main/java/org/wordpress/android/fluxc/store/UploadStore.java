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
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.persistence.UploadSqlUtils;
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload;
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType;
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType.Type;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UploadStore extends Store {

    @Inject public UploadStore(Dispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void onRegister() {
        AppLog.d(T.API, "UploadStore onRegister");
    }

    // Ensure that events reach the UploadStore before their main stores (MediaStore, PostStore)
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

    public float getUploadProgressForMedia(MediaModel mediaModel) {
        MediaUploadModel mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(mediaModel.getId());
        if (mediaUploadModel != null) {
            return mediaUploadModel.getProgress();
        }
        return 0;
    }

    private void handleUploadMedia(@NonNull MediaPayload payload) {
        if (payload.media == null) {
            return;
        }
        MediaUploadModel mediaUploadModel = new MediaUploadModel(payload.media.getId());
        MalformedMediaArgSubType argError = MediaUtils.getMediaValidationErrorType(payload.media);

        if (argError.getType() != Type.NO_ERROR) {
            mediaUploadModel.setUploadState(MediaUploadModel.FAILED);
            mediaUploadModel.setMediaError(
                    new MediaError(
                            MediaErrorType.MALFORMED_MEDIA_ARG,
                            argError.getType().getErrorLogDescription(),
                            argError
                    )
            );
        }
        UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);
    }

    private void handleMediaUploaded(@NonNull ProgressPayload payload) {
        if (payload.media == null) {
            return;
        }

        MediaUploadModel mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(payload.media.getId());
        if (mediaUploadModel == null) {
            if (!payload.isError() && !payload.canceled && !payload.completed) {
                // This is a progress event, and the upload seems to have already been cancelled
                // We don't want to store a new MediaUploadModel in this case, just move on
                return;
            }
            mediaUploadModel = new MediaUploadModel(payload.media.getId());
        }

        if (payload.isError() || payload.canceled) {
            mediaUploadModel.setUploadState(MediaUploadModel.FAILED);
            if (payload.isError()) {
                mediaUploadModel.setMediaError(payload.error);
            }
            UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);
            return;
        }

        if (payload.completed) {
            mediaUploadModel.setUploadState(MediaUploadModel.COMPLETED);
            mediaUploadModel.setProgress(1F);
            UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);
        } else {
            if (mediaUploadModel.getProgress() < payload.progress) {
                mediaUploadModel.setProgress(payload.progress);
                // To avoid conflicts with another action handler updating the state of the MediaUploadModel,
                // update the progress value only, since that's all the new information this event gives us
                UploadSqlUtils.updateMediaProgressOnly(mediaUploadModel);
            }
        }
    }

    private void handleCancelMedia(@NonNull CancelMediaPayload payload) {
        // If the cancel action has the delete flag, the corresponding MediaModel will be deleted once this action
        // reaches the MediaStore, along with the MediaUploadModel (because of the FOREIGN KEY association)
        // Otherwise, we should mark the MediaUploadModel as FAILED
        if (!payload.delete) {
            MediaUploadModel mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(payload.media.getId());
            if (mediaUploadModel == null) {
                mediaUploadModel = new MediaUploadModel(payload.media.getId());
            }

            mediaUploadModel.setUploadState(MediaUploadModel.FAILED);
            UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);
        }
    }

    private void handleUpdateMedia(@NonNull MediaModel payload) {
        MediaUploadModel mediaUploadModel = UploadSqlUtils.getMediaUploadModelForLocalId(payload.getId());
        if (mediaUploadModel == null) {
            return;
        }

        // If the new MediaModel state is different from ours, update the MediaUploadModel to reflect it
        MediaUploadState newUploadState = MediaUploadState.fromString(payload.getUploadState());
        switch (mediaUploadModel.getUploadState()) {
            case MediaUploadModel.UPLOADING:
                if (newUploadState == MediaUploadState.FAILED) {
                    mediaUploadModel.setUploadState(MediaUploadModel.FAILED);
                    mediaUploadModel.setMediaError(new MediaError(MediaErrorType.GENERIC_ERROR));
                    mediaUploadModel.setProgress(0);
                    UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);
                }
                break;
            case MediaUploadModel.COMPLETED:
                // We never care about changes to MediaModels that are already COMPLETED
                break;
            case MediaUploadModel.FAILED:
                if (newUploadState == MediaUploadState.UPLOADING || newUploadState == MediaUploadState.QUEUED) {
                    mediaUploadModel.setUploadState(MediaUploadModel.UPLOADING);
                    mediaUploadModel.setMediaError(null); // clear any previous errors
                    UploadSqlUtils.insertOrUpdateMedia(mediaUploadModel);
                }
                break;
        }
    }
}
