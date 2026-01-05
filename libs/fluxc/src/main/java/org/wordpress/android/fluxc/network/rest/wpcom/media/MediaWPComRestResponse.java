package org.wordpress.android.fluxc.network.rest.wpcom.media;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.network.Response;

import java.util.List;

/**
 * Response to GET request for media items
 * <p>
 * @see <a href="https://developer.wordpress.com/docs/api/1.1/get/sites/%24site/media/%24media_ID/">doc</a>
 */
@SuppressWarnings("NotNullFieldNotInitialized")
public class MediaWPComRestResponse implements Response {
    public static final String DELETED_STATUS = "deleted";

    public static class MultipleMediaResponse {
        @NonNull public List<MediaWPComRestResponse> media;
    }

    public static class Thumbnails {
        @Nullable public String thumbnail;
        @Nullable public String fmt_std;
    }

    public long ID;
    @NonNull public String date;
    public long post_ID;
    @NonNull public String URL;
    @NonNull public String file;
    @NonNull public String mime_type;
    @NonNull public String title;
    @NonNull public String caption;
    @NonNull public String description;
    @NonNull public String alt;
    @Nullable public Thumbnails thumbnails;
    @Nullable public String status;
}
