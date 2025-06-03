package org.wordpress.android.fluxc.network.rest.wpcom.plugin;

import com.google.gson.annotations.SerializedName;

public class PluginWPComRestResponse {
    public class ActionLinks {
        @SerializedName("Settings")
        public String settings;
    }

    public boolean active;
    public String author;
    public String author_url;
    public boolean autoupdate;
    public String description;
    public String display_name;
    public String name;
    public String plugin_url;
    public String slug;
    public String version;
    public ActionLinks action_links;
}
