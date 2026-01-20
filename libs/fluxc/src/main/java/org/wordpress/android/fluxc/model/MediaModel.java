package org.wordpress.android.fluxc.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;

public class MediaModel extends Payload<BaseNetworkError> implements Serializable {
    public enum MediaUploadState {
        QUEUED, UPLOADING, DELETING, DELETED, FAILED, UPLOADED;

        @NonNull
        public static MediaUploadState fromString(@Nullable String stringState) {
            if (stringState != null) {
                for (MediaUploadState state : MediaUploadState.values()) {
                    if (stringState.equalsIgnoreCase(state.toString())) {
                        return state;
                    }
                }
            }
            return UPLOADED;
        }
    }

    private int mId;

    // Associated IDs
    private int mLocalSiteId;
    private long mMediaId; // The remote ID of the media
    private long mPostId; // The remote post ID ('parent') of the media

    // Upload date, ISO 8601-formatted date in UTC
    @Nullable private final String mUploadDate;

    // Remote Url's
    @NonNull private final String mUrl;

    // File descriptors
    @Nullable private final String mFileName;
    @Nullable private String mFilePath;
    @Nullable private final String mMimeType;

    // Descriptive strings
    @Nullable private final String mTitle;
    @NonNull private final String mCaption;
    @NonNull private final String mDescription;

    /**
     * Use when converting local uri into a media, and then, to upload a new or update an existing media.
     */
    public MediaModel(
            int localSiteId,
            @Nullable String uploadDate,
            @Nullable String fileName,
            @Nullable String filePath,
            @Nullable String mimeType,
            @Nullable String title) {
        this.mLocalSiteId = localSiteId;
        this.mUploadDate = uploadDate;
        this.mUrl = "";
        this.mFileName = fileName;
        this.mFilePath = filePath;
        this.mMimeType = mimeType;
        this.mTitle = title;
        this.mCaption = "";
        this.mDescription = "";
    }

    /**
     * Used for receiving media from the remote
     */
    public MediaModel(
            int localSiteId,
            long mediaId,
            long postId,
            @Nullable String uploadDate,
            @NonNull String url,
            @Nullable String fileName,
            @Nullable String mimeType,
            @Nullable String title,
            @NonNull String caption,
            @NonNull String description) {
        this.mLocalSiteId = localSiteId;
        this.mMediaId = mediaId;
        this.mPostId = postId;
        this.mUploadDate = uploadDate;
        this.mUrl = url;
        this.mFileName = fileName;
        this.mMimeType = mimeType;
        this.mTitle = title;
        this.mCaption = caption;
        this.mDescription = description;
    }

    @Override
    @SuppressWarnings("ConditionCoveredByFurtherCondition")
    public boolean equals(@Nullable Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof MediaModel)) return false;

        MediaModel otherMedia = (MediaModel) other;

        return getId() == otherMedia.getId()
                && getLocalSiteId() == otherMedia.getLocalSiteId()
                && getMediaId() == otherMedia.getMediaId()
                && getPostId() == otherMedia.getPostId()
                && StringUtils.equals(getUploadDate(), otherMedia.getUploadDate())
                && StringUtils.equals(getUrl(), otherMedia.getUrl())
                && StringUtils.equals(getFileName(), otherMedia.getFileName())
                && StringUtils.equals(getFilePath(), otherMedia.getFilePath())
                && StringUtils.equals(getMimeType(), otherMedia.getMimeType())
                && StringUtils.equals(getTitle(), otherMedia.getTitle())
                && StringUtils.equals(getDescription(), otherMedia.getDescription())
                && StringUtils.equals(getCaption(), otherMedia.getCaption());
    }

    public void setId(int id) {
        mId = id;
    }

    public int getId() {
        return mId;
    }

    public void setLocalSiteId(int localSiteId) {
        mLocalSiteId = localSiteId;
    }

    public int getLocalSiteId() {
        return mLocalSiteId;
    }

    public void setMediaId(long mediaId) {
        mMediaId = mediaId;
    }

    public long getMediaId() {
        return mMediaId;
    }

    public void setPostId(long postId) {
        mPostId = postId;
    }

    public long getPostId() {
        return mPostId;
    }

    @Nullable
    public String getUploadDate() {
        return mUploadDate;
    }

    @NonNull
    public String getUrl() {
        return mUrl;
    }

    @Nullable
    public String getFileName() {
        return mFileName;
    }

    @Nullable
    public String getFilePath() {
        return mFilePath;
    }

    @Nullable
    public String getMimeType() {
        return mMimeType;
    }

    @Nullable
    public String getTitle() {
        return mTitle;
    }

    @NonNull
    public String getCaption() {
        return mCaption;
    }

    @NonNull
    public String getDescription() {
        return mDescription;
    }
}
