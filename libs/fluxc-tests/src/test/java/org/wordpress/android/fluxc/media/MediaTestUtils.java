package org.wordpress.android.fluxc.media;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.utils.MediaUtils;

public class MediaTestUtils {

    public static MediaModel generateMediaFromPath(String filePath) {
        return generateMediaFromPath(0, 0L, filePath);
    }

    public static MediaModel generateMediaFromPath(int localSiteId, long mediaId, String filePath) {
        return new MediaModel(
                0, // id
                localSiteId,
                0, // localPostId
                mediaId,
                0L, // postId
                null, // uploadDate
                "", // url
                null, // thumbnailUrl
                MediaUtils.getFileName(filePath),
                filePath,
                MediaUtils.getMimeTypeForExtension(MediaUtils.getExtension(filePath)),
                MediaUtils.getFileName(filePath), // title
                "", // caption
                "", // description
                "", // alt
                null, // uploadState
                false // markedLocallyAsFeatured
        );
    }
}
