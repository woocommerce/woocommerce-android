package org.wordpress.android.fluxc.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

import org.wordpress.android.util.StringUtils;

import java.io.Serializable;

@Table
public class ThemeModel implements Identifiable, Serializable {
    private static final long serialVersionUID = 5966516212440517166L;

    @PrimaryKey @Column private int mId;

    @Column private int mLocalSiteId;
    @NonNull @Column private String mThemeId;
    @NonNull @Column private String mName;
    @Nullable @Column private String mDemoUrl;
    @Column private boolean mActive;
    @Column private boolean mIsWpComTheme;

    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    public ThemeModel() {
        this.mId = 0;
        this.mLocalSiteId = 0;
        this.mThemeId = "";
        this.mName = "";
        this.mDemoUrl = null;
        this.mActive = false;
        this.mIsWpComTheme = false;
    }

    /**
     * Use when creating a WP.com theme.
     */
    public ThemeModel(
            @NonNull String themeId,
            @NonNull String name,
            @Nullable String demoUrl) {
        this.mThemeId = themeId;
        this.mName = name;
        this.mDemoUrl = demoUrl;
    }

    /**
     * Use when creating a Jetpack theme.
     */
    public ThemeModel(
            @NonNull String themeId,
            @NonNull String name,
            boolean active
            ) {
        this.mThemeId = themeId;
        this.mName = name;
        this.mActive = active;
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    @Override
    @SuppressWarnings("ConditionCoveredByFurtherCondition")
    public boolean equals(@Nullable Object other) {
        if (other == null || !(other instanceof ThemeModel)) {
            return false;
        }
        ThemeModel otherTheme = (ThemeModel) other;
        return getId() == otherTheme.getId()
                && getLocalSiteId() == otherTheme.getLocalSiteId()
                && StringUtils.equals(getThemeId(), otherTheme.getThemeId())
                && StringUtils.equals(getName(), otherTheme.getName())
                && StringUtils.equals(getDemoUrl(), otherTheme.getDemoUrl())
                && getActive() == otherTheme.getActive()
                && isWpComTheme() == otherTheme.isWpComTheme();
    }

    public int getLocalSiteId() {
        return mLocalSiteId;
    }

    public void setLocalSiteId(int localSiteId) {
        this.mLocalSiteId = localSiteId;
    }

    @NonNull
    public String getThemeId() {
        return mThemeId;
    }

    public void setThemeId(@NonNull String themeId) {
        mThemeId = themeId;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    public void setName(@NonNull String name) {
        mName = name;
    }

    @Nullable
    public String getDemoUrl() {
        return mDemoUrl;
    }

    public void setDemoUrl(@Nullable String demoUrl) {
        mDemoUrl = demoUrl;
    }

    public boolean getActive() {
        return mActive;
    }

    public void setActive(boolean active) {
        mActive = active;
    }

    public boolean isWpComTheme() {
        return mIsWpComTheme;
    }

    public void setIsWpComTheme(boolean isWpComTheme) {
        mIsWpComTheme = isWpComTheme;
    }
}
