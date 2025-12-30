package org.wordpress.android.fluxc.network.rest.wpcom.media;

import android.content.Context;
import android.text.TextUtils;

import com.android.volley.RequestQueue;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.UploadActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.MediaFields;
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody.ProgressListener;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaWPComRestResponse.MultipleMediaResponse;
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListResponsePayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.fluxc.utils.MimeType;
import org.wordpress.android.fluxc.utils.WPComRestClientUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * MediaRestClient provides an interface for manipulating a WP.com site's media. It provides
 * methods to:
 *
 * <ul>
 *     <li>Fetch existing media from a WP.com site
 *     (via {@link #fetchMediaList(SiteModel, int, int, MimeType.Type)} and
 *     {@link #fetchMedia(SiteModel, MediaModel)}</li>
 *     <li>Push new media to a WP.com site
 *     (via {@link #uploadMedia(SiteModel, MediaModel)})</li>
 *     <li>Push updates to existing media to a WP.com site
 *     (via {@link #pushMedia(SiteModel, MediaModel)})</li>
 *     <li>Delete existing media from a WP.com site
 *     (via {@link #deleteMedia(SiteModel, MediaModel)})</li>
 * </ul>
 */
@Singleton
public class MediaRestClient extends BaseWPComRestClient implements ProgressListener {
    @NonNull private final OkHttpClient mOkHttpClient;
    @NonNull private final MediaResponseUtils mMediaResponseUtils;
    // this will hold which media is being uploaded by which call, in order to be able
    // to monitor multiple uploads
    @NonNull private final ConcurrentHashMap<Integer, Call> mCurrentUploadCalls = new ConcurrentHashMap<>();

    @Inject public MediaRestClient(
            Context appContext,
            Dispatcher dispatcher,
            @Named("regular") RequestQueue requestQueue,
            @NonNull @Named("regular") OkHttpClient okHttpClient,
            AccessToken accessToken,
            UserAgent userAgent,
            @NonNull MediaResponseUtils mediaResponseUtils) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
        mOkHttpClient = okHttpClient;
        mMediaResponseUtils = mediaResponseUtils;
    }

    @Override
    public void onProgress(@NonNull MediaModel media, float progress) {
        if (mCurrentUploadCalls.containsKey(media.getId())) {
            notifyMediaProgress(media, Math.min(progress, 0.99f));
        }
    }

    private void removeCallFromCurrentUploadsMap(int id) {
        mCurrentUploadCalls.remove(id);
        AppLog.d(T.MEDIA, "mediaRestClient: removed id: " + id + " from current uploads, remaining: "
                          + mCurrentUploadCalls.size());
    }

    private void notifyMediaProgress(@NonNull MediaModel media, float progress) {
        ProgressPayload payload = new ProgressPayload(media, progress, false, null);
        mDispatcher.dispatch(UploadActionBuilder.newUploadedMediaAction(payload));
    }

    private void notifyMediaUploaded(@Nullable MediaModel media, @Nullable MediaError error) {
        if (media != null) {
            media.setUploadState(error == null ? MediaUploadState.UPLOADED : MediaUploadState.FAILED);
            removeCallFromCurrentUploadsMap(media.getId());
        }

        ProgressPayload payload = new ProgressPayload(media, 1.f, error == null, error);
        mDispatcher.dispatch(UploadActionBuilder.newUploadedMediaAction(payload));
    }

    //
    // Utility methods
    //

    /**
     * The current REST API call (v1.1) accepts 'title', 'description', 'caption', 'alt',
     * and 'parent_id' for all media. Audio media also accepts 'artist' and 'album' attributes.
     * <p>
     *
     * @see <a href="https://developer.wordpress.com/docs/api/1.1/post/sites/%24site/media/">documentation</a>
     */
    @NonNull
    private Map<String, Object> getEditRequestParams(@NonNull final MediaModel media) {
        MediaFields[] fieldsToUpdate = media.getFieldsToUpdate();

        final Map<String, Object> params = new HashMap<>();
        for (MediaFields field : fieldsToUpdate) {
            switch (field) {
                case PARENT_ID:
                    if (media.getPostId() > 0) {
                        params.put(MediaFields.PARENT_ID.getFieldName(), String.valueOf(media.getPostId()));
                    }
                    break;
                case TITLE:
                    if (!TextUtils.isEmpty(media.getTitle())) {
                        params.put(MediaFields.TITLE.getFieldName(), media.getTitle());
                    }
                    break;
                case DESCRIPTION:
                    if (!TextUtils.isEmpty(media.getDescription())) {
                        params.put(MediaFields.DESCRIPTION.getFieldName(), media.getDescription());
                    }
                    break;
                case CAPTION:
                    if (!TextUtils.isEmpty(media.getCaption())) {
                        params.put(MediaFields.CAPTION.getFieldName(), media.getCaption());
                    }
                    break;
                case ALT:
                    if (!TextUtils.isEmpty(media.getAlt())) {
                        params.put(MediaFields.ALT.getFieldName(), media.getAlt());
                    }
                    break;
            }
        }
        return params;
    }
}
