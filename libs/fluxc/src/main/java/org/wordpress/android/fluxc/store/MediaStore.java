package org.wordpress.android.fluxc.store;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpapi.media.WooMediaNetwork;
import org.wordpress.android.fluxc.store.media.MediaErrorSubType;
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType;
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType.Type;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.fluxc.utils.MimeType;
import org.wordpress.android.util.AppLog;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MediaStore extends Store {
    public static class MediaPayload extends Payload<MediaError> {
        @NonNull public SiteModel site;
        @Nullable public MediaModel media;

        public MediaPayload(@NonNull SiteModel site, @NonNull MediaModel media) {
            this(site, media, null);
        }

        public MediaPayload(@NonNull SiteModel site, @Nullable MediaModel media, @Nullable MediaError error) {
            this.site = site;
            this.media = media;
            this.error = error;
        }
    }

    public static class UploadMediaPayload extends MediaPayload {
        public final boolean stripLocation;

        public UploadMediaPayload(
                @NonNull SiteModel site,
                @Nullable MediaModel media,
                boolean stripLocation) {
            super(site, media, null);
            this.stripLocation = stripLocation;
        }
    }

    public static class FetchMediaListPayload extends Payload<BaseNetworkError> {
        @NonNull public SiteModel site;
        public boolean loadMore;
        @Nullable public MimeType.Type mimeType;
        public int number;

        public FetchMediaListPayload(
                @NonNull SiteModel site,
                int number,
                boolean loadMore,
                @NonNull MimeType.Type mimeType) {
            this.site = site;
            this.loadMore = loadMore;
            this.mimeType = mimeType;
            this.number = number;
        }
    }

    public static class FetchMediaListResponsePayload extends Payload<MediaError> {
        @NonNull public SiteModel site;
        @NonNull public List<MediaModel> mediaList;
        public boolean loadedMore;
        public boolean canLoadMore;
        @Nullable public MimeType.Type mimeType;

        public FetchMediaListResponsePayload(
                @NonNull SiteModel site,
                @NonNull List<MediaModel> mediaList,
                boolean loadedMore,
                boolean canLoadMore,
                @Nullable MimeType.Type mimeType) {
            this.site = site;
            this.mediaList = mediaList;
            this.loadedMore = loadedMore;
            this.canLoadMore = canLoadMore;
            this.mimeType = mimeType;
        }

        public FetchMediaListResponsePayload(
                @NonNull SiteModel site,
                @NonNull MediaError error,
                @Nullable MimeType.Type mimeType) {
            this.mediaList = new ArrayList<>();
            this.site = site;
            this.error = error;
            this.mimeType = mimeType;
        }
    }

    public static class ProgressPayload extends Payload<MediaError> {
        @Nullable public MediaModel media;
        public float progress;
        public boolean completed;
        public boolean canceled;

        public ProgressPayload(
                @NonNull MediaModel media,
                float progress,
                boolean completed,
                boolean canceled) {
            this(media, progress, completed, null);
            this.canceled = canceled;
        }

        public ProgressPayload(
                @Nullable MediaModel media,
                float progress,
                boolean completed,
                @Nullable MediaError error) {
            this.media = media;
            this.progress = progress;
            this.completed = completed;
            this.error = error;
        }
    }

    public static class CancelMediaPayload extends Payload<BaseNetworkError> {
        @NonNull public SiteModel site;
        @NonNull public MediaModel media;
        public boolean delete;

        public CancelMediaPayload(@NonNull SiteModel site, @NonNull MediaModel media, boolean delete) {
            this.site = site;
            this.media = media;
            this.delete = delete;
        }
    }

    //
    // OnChanged events
    //

    public static class MediaError implements OnChangedError {
        @NonNull public MediaErrorType type;
        @Nullable public MediaErrorSubType mErrorSubType;
        @Nullable public String message;
        @Nullable public String apiErrorCode;
        public int statusCode;
        @Nullable public String logMessage;

        public MediaError(@NonNull MediaErrorType type) {
            this.type = type;
        }

        public MediaError(@NonNull MediaErrorType type, @Nullable String message) {
            this.type = type;
            this.message = message;
        }

        public MediaError(
                @NonNull MediaErrorType type,
                @Nullable String message,
                @NonNull MediaErrorSubType errorSubType) {
            this.type = type;
            this.message = message;
            this.mErrorSubType = errorSubType;
        }

        @NonNull
        public static MediaError fromIOException(@NonNull IOException e) {
            MediaError mediaError = new MediaError(MediaErrorType.GENERIC_ERROR);
            mediaError.message = e.getLocalizedMessage();
            mediaError.logMessage = e.getMessage();

            if (e instanceof SocketTimeoutException) {
                mediaError.type = MediaErrorType.TIMEOUT;
            }

            if (e instanceof ConnectException || e instanceof UnknownHostException) {
                mediaError.type = MediaErrorType.CONNECTION_ERROR;
            }

            String errorMessage = e.getMessage();
            if (TextUtils.isEmpty(errorMessage)) {
                return mediaError;
            }

            errorMessage = errorMessage.toLowerCase(Locale.US);
            if (errorMessage.contains("broken pipe") || errorMessage.contains("epipe")) {
                // do not use the real error message.
                mediaError.message = "";
            }

            return mediaError;
        }

        @NonNull
        public String getApiUserMessageIfAvailable() {
            if (TextUtils.isEmpty(message)) {
                return "";
            }

            if (type == MediaErrorType.BAD_REQUEST) {
                String[] splitMsg = message.split("\\|", 2);

                if (splitMsg.length > 1) {
                    String userMessage = splitMsg[1];

                    if (TextUtils.isEmpty(userMessage)) {
                        return message;
                    }

                    // NOTE: It seems the backend is sending a final " Back" string in the message
                    // Note that the real string depends on current locale; this is not optimal and we thought to
                    // try to filter it out in the client app but at the end it can be not reliable so we are
                    // keeping it. We can try to get it filtered on the backend side.

                    return userMessage;
                } else {
                    return message;
                }
            } else {
                return message;
            }
        }
    }

    public static class OnMediaChanged extends OnChanged<MediaError> {
        @NonNull public MediaAction cause;
        @NonNull public List<MediaModel> mediaList;

        public OnMediaChanged(@NonNull MediaAction cause) {
            this(cause, new ArrayList<>(), null);
        }

        public OnMediaChanged(
                @NonNull MediaAction cause,
                @NonNull List<MediaModel> mediaList) {
            this(cause, mediaList, null);
        }

        public OnMediaChanged(
                @NonNull MediaAction cause,
                @Nullable MediaError error) {
            this(cause, new ArrayList<>(), error);
        }

        public OnMediaChanged(
                @NonNull MediaAction cause,
                @NonNull List<MediaModel> mediaList,
                @Nullable MediaError error) {
            this.cause = cause;
            this.mediaList = mediaList;
            this.error = error;
        }
    }

    public static class OnMediaListFetched extends OnChanged<MediaError> {
        @NonNull public SiteModel site;
        public boolean canLoadMore;
        @Nullable public MimeType.Type mimeType;

        public OnMediaListFetched(
                @NonNull SiteModel site,
                boolean canLoadMore,
                @Nullable MimeType.Type mimeType) {
            this.site = site;
            this.canLoadMore = canLoadMore;
            this.mimeType = mimeType;
        }

        public OnMediaListFetched(
                @NonNull SiteModel site,
                @Nullable MediaError error,
                @Nullable MimeType.Type mimeType) {
            this.site = site;
            this.error = error;
            this.mimeType = mimeType;
        }
    }

    public static class OnMediaUploaded extends OnChanged<MediaError> {
        @Nullable public MediaModel media;
        public float progress;
        public boolean completed;
        public boolean canceled;

        public OnMediaUploaded(
                @Nullable MediaModel media,
                float progress,
                boolean completed,
                boolean canceled) {
            this.media = media;
            this.progress = progress;
            this.completed = completed;
            this.canceled = canceled;
        }
    }

    //
    // Errors
    //

    public enum MediaErrorType {
        // local errors, occur before sending network requests
        FS_READ_PERMISSION_DENIED,
        NULL_MEDIA_ARG,
        MALFORMED_MEDIA_ARG,
        DB_QUERY_FAILURE,
        EXCEEDS_FILESIZE_LIMIT,
        EXCEEDS_MEMORY_LIMIT,
        EXCEEDS_SITE_SPACE_QUOTA_LIMIT,

        // network errors, occur in response to network requests
        AUTHORIZATION_REQUIRED,
        CONNECTION_ERROR,
        NOT_AUTHENTICATED,
        NOT_FOUND,
        PARSE_ERROR,
        REQUEST_TOO_LARGE,
        SERVER_ERROR, // this is also returned when PHP max_execution_time or memory_limit is reached
        TIMEOUT,
        BAD_REQUEST,

        // logic constraints errors
        INVALID_ID,

        // unknown/unspecified
        GENERIC_ERROR;

        @NonNull
        public static MediaErrorType fromBaseNetworkError(@NonNull BaseNetworkError baseError) {
            switch (baseError.type) {
                case NOT_FOUND:
                    return MediaErrorType.NOT_FOUND;
                case NOT_AUTHENTICATED:
                    return MediaErrorType.NOT_AUTHENTICATED;
                case AUTHORIZATION_REQUIRED:
                    return MediaErrorType.AUTHORIZATION_REQUIRED;
                case PARSE_ERROR:
                    return MediaErrorType.PARSE_ERROR;
                case SERVER_ERROR:
                    return MediaErrorType.SERVER_ERROR;
                case TIMEOUT:
                    return MediaErrorType.TIMEOUT;
                case NO_CONNECTION:
                case NETWORK_ERROR:
                case CENSORED:
                case INVALID_SSL_CERTIFICATE:
                case HTTP_AUTH_ERROR:
                case INVALID_RESPONSE:
                case UNKNOWN:
                default:
                    return MediaErrorType.GENERIC_ERROR;
            }
        }

        @NonNull
        public static MediaErrorType fromHttpStatusCode(int code) {
            switch (code) {
                case 400:
                    return MediaErrorType.BAD_REQUEST;
                case 404:
                    return MediaErrorType.NOT_FOUND;
                case 403:
                    return MediaErrorType.NOT_AUTHENTICATED;
                case 413:
                    return MediaErrorType.REQUEST_TOO_LARGE;
                case 500:
                    return MediaErrorType.SERVER_ERROR;
                default:
                    return MediaErrorType.GENERIC_ERROR;
            }
        }

        @NonNull
        public static MediaErrorType fromString(@Nullable String string) {
            if (string != null) {
                for (MediaErrorType v : MediaErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    private final WooMediaNetwork mWooMediaNetwork;
    @NonNull private final RemoteMediaCache mRemoteMediaCache;
    @NonNull private final MediaCacheOperations mMediaCacheOperations;
    @NonNull private final MediaIdGenerator mMediaIdGenerator;

    @Inject public MediaStore(
            Dispatcher dispatcher,
            WooMediaNetwork wooMediaNetwork,
            @NonNull RemoteMediaCache remoteMediaCache,
            @NonNull MediaCacheOperations mediaCacheOperations,
            @NonNull MediaIdGenerator mediaIdGenerator) {
        super(dispatcher);
        mWooMediaNetwork = wooMediaNetwork;
        mRemoteMediaCache = remoteMediaCache;
        mMediaCacheOperations = mediaCacheOperations;
        mMediaIdGenerator = mediaIdGenerator;
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Override
    @SuppressWarnings("rawtypes")
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (!(actionType instanceof MediaAction)) {
            return;
        }

        switch ((MediaAction) actionType) {
            case UPLOAD_MEDIA:
                performUploadMedia((UploadMediaPayload) action.getPayload());
                break;
            case FETCH_MEDIA_LIST:
                performFetchMediaList((FetchMediaListPayload) action.getPayload());
                break;
            case FETCH_MEDIA:
                break;
            case CANCEL_MEDIA_UPLOAD:
                performCancelUpload((CancelMediaPayload) action.getPayload());
                break;
            case UPLOADED_MEDIA:
                handleMediaUploaded((ProgressPayload) action.getPayload());
                break;
            case FETCHED_MEDIA_LIST:
                handleMediaListFetched((FetchMediaListResponsePayload) action.getPayload());
                break;
            case FETCHED_MEDIA:
                handleMediaFetched((MediaPayload) action.getPayload());
                break;
            case CANCELED_MEDIA_UPLOAD:
                handleMediaCanceled((ProgressPayload) action.getPayload());
                break;
            case UPDATE_MEDIA:
                updateMedia(((MediaModel) action.getPayload()), true);
                break;
        }
    }

    @Override
    public void onRegister() {
        AppLog.d(AppLog.T.MEDIA, "MediaStore onRegister");
    }

    //
    // Getters
    //

    @NonNull
    public MediaModel instantiateMediaModel(@NonNull MediaModel media) {
        media.setId(mMediaIdGenerator.generate(media.getFilePath()).getValue());
        return media;
    }

    @NonNull
    public List<MediaModel> getSiteImages(@NonNull SiteModel siteModel) {
        return mMediaCacheOperations.getSiteImages(siteModel.getId());
    }

    @NonNull
    public List<MediaModel> getSiteVideos(@NonNull SiteModel siteModel) {
        return mMediaCacheOperations.getSiteVideos(siteModel.getId());
    }

    @NonNull
    public List<MediaModel> getSiteAudio(@NonNull SiteModel siteModel) {
        return mMediaCacheOperations.getSiteAudio(siteModel.getId());
    }

    @NonNull
    public List<MediaModel> getSiteDocuments(@NonNull SiteModel siteModel) {
        return mMediaCacheOperations.getSiteDocuments(siteModel.getId());
    }

    @NonNull
    public List<MediaModel> searchSiteImages(
            @NonNull SiteModel siteModel,
            @NonNull String searchTerm) {
        return mMediaCacheOperations.searchSiteImages(siteModel.getId(), searchTerm);
    }

    @NonNull
    public List<MediaModel> searchSiteVideos(
            @NonNull SiteModel siteModel,
            @NonNull String searchTerm) {
        return mMediaCacheOperations.searchSiteVideos(siteModel.getId(), searchTerm);
    }

    @NonNull
    public List<MediaModel> searchSiteAudio(
            @NonNull SiteModel siteModel,
            @NonNull String searchTerm) {
        return mMediaCacheOperations.searchSiteAudio(siteModel.getId(), searchTerm);
    }

    @NonNull
    public List<MediaModel> searchSiteDocuments(
            @NonNull SiteModel siteModel,
            @NonNull String searchTerm) {
        return mMediaCacheOperations.searchSiteDocuments(siteModel.getId(), searchTerm);
    }

    //
    // Action implementations
    //

    void updateMedia(@Nullable MediaModel media, boolean emit) {
        OnMediaChanged event = new OnMediaChanged(MediaAction.UPDATE_MEDIA);

        if (media == null) {
            event.error = new MediaError(MediaErrorType.NULL_MEDIA_ARG);
        } else {
            mRemoteMediaCache.addOrUpdate(media.getLocalSiteId(), media);
            event.mediaList.add(media);
        }

        if (emit) {
            emitChange(event);
        }
    }

    //
    // Helper methods that choose the appropriate network client to perform an action
    //

    @SuppressWarnings("SameParameterValue")
    private void notifyMediaUploadError(
            @NonNull MediaErrorType errorType,
            @Nullable String errorMessage,
            @Nullable MediaModel media,
            @NonNull String logMessage,
            @NonNull MalformedMediaArgSubType argErrorType) {
        OnMediaUploaded onMediaUploaded = new OnMediaUploaded(media, 1, false, false);
        MediaError mediaError = new MediaError(errorType, errorMessage, argErrorType);
        mediaError.logMessage = logMessage;
        onMediaUploaded.error = mediaError;
        emitChange(onMediaUploaded);
    }

    private void performUploadMedia(@NonNull UploadMediaPayload payload) {
        if (payload.media == null) {
            // null or empty media list -or- list contains a null value
            notifyMediaError(MediaErrorType.NULL_MEDIA_ARG, MediaAction.UPLOAD_MEDIA, null);
            return;
        }

        MalformedMediaArgSubType argError = MediaUtils.getMediaValidationErrorType(payload.media);

        if (argError.getType() != Type.NO_ERROR) {
            String message = "Media doesn't have required data: " + argError.getType().getErrorLogDescription();
            AppLog.e(AppLog.T.MEDIA, message);
            notifyMediaUploadError(
                    MediaErrorType.MALFORMED_MEDIA_ARG,
                    argError.getType().getErrorLogDescription(),
                    payload.media,
                    message,
                    argError);
            return;
        }

        if (payload.stripLocation) {
            MediaUtils.stripLocation(payload.media.getFilePath());
        }

        mWooMediaNetwork.uploadMedia(payload.site, payload.media);
    }

    private void performFetchMediaList(@NonNull FetchMediaListPayload payload) {
        int offset = 0;
        if (payload.loadMore) {
            String mimeTypeValue = payload.mimeType != null ? payload.mimeType.getValue() : null;
            offset = mMediaCacheOperations.getUploadedMediaCount(payload.site.getId(), mimeTypeValue);
        }
        mWooMediaNetwork.fetchMediaList(payload.site, payload.number, offset, payload.mimeType);
    }

    private void performCancelUpload(@NonNull CancelMediaPayload payload) {
        MediaModel media = payload.media;
        if (payload.delete) {
            mRemoteMediaCache.remove(payload.site.getId(), media.getMediaId());
        }

        mWooMediaNetwork.cancelUpload(payload.site, media);
    }

    private void handleMediaUploaded(@NonNull ProgressPayload payload) {
        if (payload.completed && !payload.isError() && !payload.canceled) {
            updateMedia(payload.media, false);
        }
        OnMediaUploaded onMediaUploaded = new OnMediaUploaded(
                payload.media,
                payload.progress,
                payload.completed,
                payload.canceled
        );
        onMediaUploaded.error = payload.error;
        emitChange(onMediaUploaded);
    }

    private void handleMediaCanceled(@NonNull ProgressPayload payload) {
        OnMediaUploaded onMediaUploaded = new OnMediaUploaded(
                payload.media,
                payload.progress,
                payload.completed,
                payload.canceled
        );
        onMediaUploaded.error = payload.error;

        emitChange(onMediaUploaded);
    }

    private void updateFetchedMediaList(@NonNull FetchMediaListResponsePayload payload) {
        List<MediaModel> currentCache = mRemoteMediaCache.getMediaList(payload.site.getId());

        if (payload.loadedMore) {
            // Append to existing cache
            if (currentCache == null) {
                currentCache = new ArrayList<>();
            }
            List<MediaModel> updatedList = new ArrayList<>(currentCache);
            updatedList.addAll(payload.mediaList);
            mRemoteMediaCache.cacheMediaList(payload.site.getId(), updatedList);
        } else {
            // Replace entire cache with fresh data
            mRemoteMediaCache.cacheMediaList(payload.site.getId(), new ArrayList<>(payload.mediaList));
        }
    }

    private void handleMediaListFetched(@NonNull FetchMediaListResponsePayload payload) {
        OnMediaListFetched onMediaListFetched;

        if (payload.isError()) {
            onMediaListFetched = new OnMediaListFetched(payload.site, payload.error, payload.mimeType);
        } else {
            updateFetchedMediaList(payload);
            onMediaListFetched = new OnMediaListFetched(payload.site, payload.canLoadMore, payload.mimeType);
        }

        emitChange(onMediaListFetched);
    }

    private void handleMediaFetched(@NonNull MediaPayload payload) {
        OnMediaChanged onMediaChanged = new OnMediaChanged(MediaAction.FETCH_MEDIA, payload.error);
        if (payload.media != null) {
            mRemoteMediaCache.addOrUpdate(payload.site.getId(), payload.media);
            onMediaChanged.mediaList = new ArrayList<>();
            onMediaChanged.mediaList.add(payload.media);
        }
        emitChange(onMediaChanged);
    }

    private void notifyMediaError(
            @NonNull MediaErrorType errorType,
            @NonNull MediaAction cause,
            @Nullable MediaModel media) {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        OnMediaChanged mediaChange = new OnMediaChanged(cause, mediaList);
        mediaChange.error = new MediaError(errorType, null);
        emitChange(mediaChange);
    }

}
