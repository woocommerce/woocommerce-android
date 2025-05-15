package org.wordpress.android.fluxc.post;

import androidx.annotation.NonNull;

import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.PostModel;

import java.util.List;

public class PostTestUtils {
    public static final double EXAMPLE_LATITUDE = 44.8378;
    public static final double EXAMPLE_LONGITUDE = -0.5792;

    static final int DEFAULT_LOCAL_SITE_ID = 6;

    public static PostModel generateSampleUploadedPost() {
        return generateSampleUploadedPost("text");
    }

    public static PostModel generateSampleUploadedPost(String postFormat) {
        PostModel example = new PostModel();
        example.setLocalSiteId(DEFAULT_LOCAL_SITE_ID);
        example.setRemotePostId(5);
        example.setTitle("A test post");
        example.setContent("Bunch of content here");
        example.setPostFormat(postFormat);
        return example;
    }

    public static PostModel generateSampleLocalDraftPost() {
        return generateSampleLocalDraftPost("A test post");
    }

    static PostModel generateSampleLocalDraftPost(@NonNull String title) {
        PostModel example = new PostModel();
        example.setLocalSiteId(DEFAULT_LOCAL_SITE_ID);
        example.setTitle(title);
        example.setContent("Bunch of content here");
        example.setIsLocalDraft(true);
        return example;
    }

    static PostModel generateSampleLocallyChangedPost() {
        return generateSampleLocallyChangedPost("A test post");
    }

    static PostModel generateSampleLocallyChangedPost(@NonNull String title) {
        PostModel example = new PostModel();
        example.setLocalSiteId(DEFAULT_LOCAL_SITE_ID);
        example.setRemotePostId(7);
        example.setTitle(title);
        example.setContent("Bunch of content here");
        example.setIsLocallyChanged(true);
        return example;
    }

    public static List<PostModel> getPosts() {
        return WellSql.select(PostModel.class).getAsModel();
    }

    public static int getPostsCount() {
        return getPosts().size();
    }
}
