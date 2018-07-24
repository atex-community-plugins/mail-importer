package com.atex.plugins.mailimporter;

import com.atex.onecms.content.ContentResult;
import com.atex.onecms.content.ContentResultBuilder;
import com.atex.onecms.content.ContentWrite;
import com.atex.onecms.content.LegacyContentAdapter;
import com.atex.plugins.baseline.policy.BaselinePolicy;
import com.atex.plugins.sitemap.SitemapConfigBean;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.policymvc.PolicyModelDomain;

import java.util.List;

/**
 * Policy for the plugin configuration.
 *
 * @author mnova
 */
public class MailImporterConfigPolicy extends BaselinePolicy {

    public static final String CONFIG_EXT_ID = "plugins.com.atex.plugins.mail-importer.Config";

    private static final String ARTICLEBEAN = "article_bean";
    private static final String IMAGEBEAN = "image_bean";
    private static final String ACCEPTED_IMAGE_EXTENSIONS = "accepted_image_extensions";
    private static final String ATTACHMENT_NAME_PATTERN = "attachment_name_pattern";
    private static final String ARTICLE_NAME_PATTERN = "article_name_pattern";

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

    public String getImageNamePattern() {
        return null;
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
}
