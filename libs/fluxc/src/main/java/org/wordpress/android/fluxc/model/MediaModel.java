package org.wordpress.android.fluxc.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;

// WARN: This class is used within WordPress-MediaPicker-Android, do not remove!
@Table
public class MediaModel extends Payload<BaseNetworkError> implements Identifiable, Serializable {
    private static final long serialVersionUID = -1396457338496002846L;

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

    @PrimaryKey
    @Column private int mId;

    // Associated IDs
    @Column private int mLocalSiteId;
    @Column private long mMediaId; // The remote ID of the media
    @Column private long mPostId; // The remote post ID ('parent') of the media

    // Upload date, ISO 8601-formatted date in UTC
    @Nullable @Column private String mUploadDate;

    // Remote Url's
    @NonNull @Column private String mUrl;
    @Nullable @Column private String mThumbnailUrl;

    // File descriptors
    @Nullable @Column private String mFileName;
    @Nullable @Column private String mFilePath;
    @Nullable @Column private String mMimeType;

    // Descriptive strings
    @Nullable @Column private String mTitle;
    @NonNull @Column private String mCaption;
    @NonNull @Column private String mDescription;
    @NonNull @Column private String mAlt;

    // Local only
    @Nullable @Column private String mUploadState;
    @Column private boolean mMarkedLocallyAsFeatured;

    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    public MediaModel() {
        this.mId = 0;
        this.mLocalSiteId = 0;
        this.mMediaId = 0;
        this.mPostId = 0;
        this.mUploadDate = null;
        this.mUrl = "";
        this.mThumbnailUrl = null;
        this.mFileName = null;
        this.mFilePath = null;
        this.mMimeType = null;
        this.mTitle = null;
        this.mCaption = "";
        this.mDescription = "";
        this.mAlt = "";
        this.mUploadState = null;
        this.mMarkedLocallyAsFeatured = false;
    }

    /**
     * Use when converting local uri into a media, and then, to upload a new or update an existing media.
     */
    public MediaModel(
            int localSiteId,
            @Nullable String uploadDate,
            @Nullable String fileName,
            @Nullable String filePath,
            @Nullable String mimeType,
            @Nullable String title,
            @Nullable MediaUploadState uploadState) {
        this.mLocalSiteId = localSiteId;
        this.mUploadDate = uploadDate;
        this.mUrl = "";
        this.mFileName = fileName;
        this.mFilePath = filePath;
        this.mMimeType = mimeType;
        this.mTitle = title;
        this.mCaption = "";
        this.mDescription = "";
        this.mAlt = "";
        this.mUploadState = uploadState != null ? uploadState.toString() : null;
    }

    public MediaModel(
            int localSiteId,
            long mediaId,
            long postId,
            @Nullable String uploadDate,
            @NonNull String url,
            @Nullable String thumbnailUrl,
            @Nullable String fileName,
            @Nullable String mimeType,
            @Nullable String title,
            @NonNull String caption,
            @NonNull String description,
            @NonNull String alt,
            @NonNull MediaUploadState uploadState) {
        this.mLocalSiteId = localSiteId;
        this.mMediaId = mediaId;
        this.mPostId = postId;
        this.mUploadDate = uploadDate;
        this.mUrl = url;
        this.mThumbnailUrl = thumbnailUrl;
        this.mFileName = fileName;
        this.mMimeType = mimeType;
        this.mTitle = title;
        this.mCaption = caption;
        this.mDescription = description;
        this.mAlt = alt;
        this.mUploadState = uploadState.toString();
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
                && getMarkedLocallyAsFeatured() == otherMedia.getMarkedLocallyAsFeatured()
                && StringUtils.equals(getUploadDate(), otherMedia.getUploadDate())
                && StringUtils.equals(getUrl(), otherMedia.getUrl())
                && StringUtils.equals(getThumbnailUrl(), otherMedia.getThumbnailUrl())
                && StringUtils.equals(getFileName(), otherMedia.getFileName())
                && StringUtils.equals(getFilePath(), otherMedia.getFilePath())
                && StringUtils.equals(getMimeType(), otherMedia.getMimeType())
                && StringUtils.equals(getTitle(), otherMedia.getTitle())
                && StringUtils.equals(getDescription(), otherMedia.getDescription())
                && StringUtils.equals(getCaption(), otherMedia.getCaption())
                && StringUtils.equals(getAlt(), otherMedia.getAlt())
                && StringUtils.equals(getUploadState(), otherMedia.getUploadState());
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    @Override
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

    public void setUploadDate(@Nullable String uploadDate) {
        mUploadDate = uploadDate;
    }

    @Nullable
    public String getUploadDate() {
        return mUploadDate;
    }

    public void setUrl(@NonNull String url) {
        mUrl = url;
    }

    @NonNull
    public String getUrl() {
        return mUrl;
    }

    public void setThumbnailUrl(@Nullable String thumbnailUrl) {
        mThumbnailUrl = thumbnailUrl;
    }

    @Nullable
    public String getThumbnailUrl() {
        return mThumbnailUrl;
    }

    public void setFileName(@Nullable String fileName) {
        mFileName = fileName;
    }

    @Nullable
    public String getFileName() {
        return mFileName;
    }

    public void setFilePath(@Nullable String filePath) {
        mFilePath = filePath;
    }

    @Nullable
    public String getFilePath() {
        return mFilePath;
    }

    public void setMimeType(@Nullable String mimeType) {
        mMimeType = mimeType;
    }

    @Nullable
    public String getMimeType() {
        return mMimeType;
    }

    public void setTitle(@Nullable String title) {
        mTitle = title;
    }

    @Nullable
    public String getTitle() {
        return mTitle;
    }

    public void setCaption(@NonNull String caption) {
        mCaption = caption;
    }

    @NonNull
    public String getCaption() {
        return mCaption;
    }

    public void setDescription(@NonNull String description) {
        mDescription = description;
    }

    @NonNull
    public String getDescription() {
        return mDescription;
    }

    public void setAlt(@NonNull String alt) {
        mAlt = alt;
    }

    @NonNull
    public String getAlt() {
        return mAlt;
    }

    public void setUploadState(@Nullable String uploadState) {
        mUploadState = uploadState;
    }

    public void setUploadState(@NonNull MediaUploadState uploadState) {
        mUploadState = uploadState.toString();
    }

    @Nullable
    public String getUploadState() {
        return mUploadState;
    }

    public boolean getMarkedLocallyAsFeatured() {
        return mMarkedLocallyAsFeatured;
    }

    public void setMarkedLocallyAsFeatured(boolean markedLocallyAsFeatured) {
        mMarkedLocallyAsFeatured = markedLocallyAsFeatured;
    }
}
