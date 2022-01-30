package nz.co.jammehcow.jenkinsdiscord;

import jenkins.model.Jenkins;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Proxy;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import nz.co.jammehcow.jenkinsdiscord.exception.WebhookException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;

/**
 * Author: jammehcow.
 * Date: 22/04/17.
 */
class DiscordWebhook {
    private String webhookUrl;
    private JSONObject obj;
    private JSONObject embed;
    private JSONArray fields;
    private InputStream file;
    private String filename;

    static final int TITLE_LIMIT = 256;
    static final int DESCRIPTION_LIMIT = 2048;
    static final int FOOTER_LIMIT = 2048;

    enum StatusColor {
        /**
         * Green "you're sweet as" color.
         */
        GREEN(1681177),
        /**
         * Yellow "go, but I'm watching you" color.
         */
        YELLOW(16776970),
        /**
         * Red "something ain't right" color.
         */
        RED(11278871),
        /**
         * Grey. Just grey.
         */
        GREY(13487565);
        private long code;

        StatusColor(int code) {
            this.code = code;
        }
    }

    /**
     * Instantiates a new Discord webhook.
     *
     * @param url the webhook URL
     */
    DiscordWebhook(String url) {
        this.webhookUrl = url;
        this.obj = new JSONObject();
        this.obj.put("username", "Jenkins");
        this.obj.put("avatar_url", "https://get.jenkins.io/art/jenkins-logo/1024x1024/headshot.png");
        this.embed = new JSONObject();
        this.fields = new JSONArray();
    }

    /**
     * Sets the embed title.
     *
     * @param title the title text
     * @return this
     */
    public DiscordWebhook setTitle(String title) {
        this.embed.put("title", title);
        return this;
    }

    public DiscordWebhook setCustomUsername(String username) {
        if (username != null && !username.equals(""))
            this.obj.put("username", username);
        else
        {
            // unset will allow default discord username to be used (as specified in discord's integration settings)
            this.obj.remove("username"); 
        }
        return this;
    }

    public DiscordWebhook setCustomAvatarUrl(String url) {
        if (url != null && !url.equals(""))
            this.obj.put("avatar_url", url);
        else
        {
            // unset will allow default avatar to be used (as specified in discord's integration settings)
            this.obj.remove("avatar_url"); 
        }
        return this;
    }

    /**
     * Sets the embed title url.
     *
     * @param buildUrl the build url
     * @return this
     */
    public DiscordWebhook setURL(String buildUrl) {
        this.embed.put("url", buildUrl);
        return this;
    }

    /**
     * Sets the build status (for the embed's color).
     *
     * @param isSuccess if the build is successful
     * @return this
     */
    public DiscordWebhook setStatus(StatusColor isSuccess) {
        this.embed.put("color", isSuccess.code);
        return this;
    }

    /**
     * Sets the embed description.
     *
     * @param content the content
     * @return this
     */
    public DiscordWebhook setDescription(String content) {
        this.embed.put("description", content);
        return this;
    }

    public DiscordWebhook setContent(String content) {
        this.obj.put("content", content);
        return this;
    }

    /**
     * Sets the URL of image at the bottom of embed.
     * @param url URL of image
     * @return this
     */
    public DiscordWebhook setImage(String url) {
        JSONObject image = new JSONObject();
        image.put("url", url);
        this.embed.put("image", image);
        return this;
    }

    /**
     * Sets the URL of image on the right side.
     * @param url URL of image
     * @return this
     */
    public DiscordWebhook setThumbnail(String url) {
        JSONObject thumbnail = new JSONObject();
        thumbnail.put("url", url);
        this.embed.put("thumbnail", thumbnail);
        return this;
    }

    public DiscordWebhook addField(String name, String value) {
        JSONObject field = new JSONObject();
        field.put("name", name);
        field.put("value", value);
        this.fields.put(field);
        return this;
    }

    /**
     * Sets the embed's footer text.
     *
     * @param text the footer text
     * @return this
     */
    public DiscordWebhook setFooter(String text) {
        this.embed.put("footer", new JSONObject().put("text", text));
        return this;
    }

    DiscordWebhook setFile(InputStream is, String filename) {
        this.file = is;
        this.filename = filename;
        return this;
    }

    /**
     * Send the payload to Discord.
     *
     * @throws WebhookException the webhook exception
     */
    public void send() throws WebhookException {
    	
        this.embed.put("fields", fields);
        if (this.embed.toString().length() > 6000)
            throw new WebhookException("Embed object larger than the limit (" + this.embed.toString().length() + ">6000).");

        this.obj.put("embeds", new JSONArray().put(this.embed));

        try {
            final Jenkins instance = Jenkins.getInstanceOrNull();
            if (instance != null && instance.proxy != null) {
                String proxyIP = instance.proxy.name;
                int proxyPort = instance.proxy.port;
                if (!proxyIP.equals("")) {
                    Unirest.config().proxy(new Proxy(proxyIP, proxyPort));
                }
            }
            HttpResponse<JsonNode> response;
            if (file != null) {
                response = Unirest.post(this.webhookUrl)
                        .field("payload_json", obj.toString())
                        .field("file", file, filename)
                        .asJson();
            } else {
                response = Unirest.post(this.webhookUrl)
                        .field("payload_json", obj.toString())
                        .asJson();
            }

            if (response.getStatus() < 200 || response.getStatus() >= 300) {
                throw new WebhookException(response.getBody().getObject().toString(2));
            }
        } catch (UnirestException e) { e.printStackTrace(); }
    }
}
