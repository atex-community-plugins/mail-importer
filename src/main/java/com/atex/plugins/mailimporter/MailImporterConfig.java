package com.atex.plugins.mailimporter;

import com.atex.plugins.baseline.policy.BaselinePolicy;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.List;

/**
 * Policy for the plugin configuration.
 *
 * @author mnova
 */
public class MailImporterConfig {

    public static final String CONFIG_EXT_ID = "plugins.com.atex.plugins.mail-importer.Config";

    private static final String ARTICLEBEAN = "article_bean";
    private static final String IMAGEBEAN = "image_bean";
    private static final String ACCEPTED_IMAGE_EXTENSIONS = "accepted_image_extensions";
    private static final String ATTACHMENT_NAME_PATTERN = "attachment_name_pattern";
    private static final String ARTICLE_NAME_PATTERN = "article_name_pattern";
    private static final String MAIL_URI = "mail_uri";
    private static final String MAILIMPORTER_ENABLED = "mailimporter_enabled";

    private final BaselinePolicy baselinePolicy;

    MailImporterConfig(BaselinePolicy baselinePolicy) {
        this.baselinePolicy = baselinePolicy;
    }

    public String getArticleBean() {
        return Strings.nullToEmpty(getChildValue(ARTICLEBEAN, "com.atex.nosql.article.ArticleBean"));
    }

    public String getImageBean() {
        return Strings.nullToEmpty(getChildValue(IMAGEBEAN, "com.atex.nosql.image.ImageContentDataBean"));
    }

    public String getAttachmentNamePattern() {
        return Strings.nullToEmpty(getChildValue(ATTACHMENT_NAME_PATTERN, "Attachment_${from}_${filename}"));
    }

    public String getArticleNamePattern() {
        return Strings.nullToEmpty(getChildValue(ARTICLE_NAME_PATTERN, "Email_${from}_${subject}"));
    }

    private String getChildValue(String property, String defaultValue) {
        return baselinePolicy.getChildValue(property, defaultValue);
    }

    public List<String> getAcceptedImageExtensions() {
        return getChildValueSplit(ACCEPTED_IMAGE_EXTENSIONS, ",", "jpg,jpeg,png,gif,zip,jar");
    }

    private List<String> getChildValueSplit(final String name, final String sep, String defaultValue) {
        final String value = Strings.nullToEmpty(getChildValue(name, defaultValue));
        return Lists.newArrayList(Splitter
                .on(sep)
                .omitEmptyStrings()
                .trimResults()
                .split(value)
        );
    }

    public String getMailUri() {
        return Strings.nullToEmpty(getChildValue(MAIL_URI, "pop3://localhost:110?username=admin@localhost&password=admin&delete=true"));
    }

    public boolean isEnabled() {
        return Boolean.parseBoolean(getChildValue(MAILIMPORTER_ENABLED, "false"));
    }
}
