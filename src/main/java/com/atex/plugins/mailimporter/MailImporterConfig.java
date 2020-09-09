package com.atex.plugins.mailimporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * MailImporterConfig
 *
 * @author mnova
 */
public class MailImporterConfig {

    private boolean enabled;
    private String articleAspect;
    private String imageAspect;
    private String attachmentNamePattern;
    private String articleNamePattern;
    private List<String> acceptedImageExtensions = new ArrayList<>();
    private List<MailRouteConfig> mailUris = new ArrayList<>();
    private String imagePartition;
    private String articlePartition;
    private String taxonomyId;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getArticleAspect() {
        return articleAspect;
    }

    public void setArticleAspect(final String articleAspect) {
        this.articleAspect = articleAspect;
    }

    public String getImageAspect() {
        return imageAspect;
    }

    public void setImageAspect(final String imageAspect) {
        this.imageAspect = imageAspect;
    }

    public String getAttachmentNamePattern() {
        return attachmentNamePattern;
    }

    public void setAttachmentNamePattern(final String attachmentNamePattern) {
        this.attachmentNamePattern = attachmentNamePattern;
    }

    public String getArticleNamePattern() {
        return articleNamePattern;
    }

    public void setArticleNamePattern(final String articleNamePattern) {
        this.articleNamePattern = articleNamePattern;
    }

    public List<String> getAcceptedImageExtensions() {
        return acceptedImageExtensions;
    }

    public void setAcceptedImageExtensions(final List<String> acceptedImageExtensions) {
        this.acceptedImageExtensions = acceptedImageExtensions;
    }

    public List<MailRouteConfig> getMailUris() {
        return mailUris;
    }

    public void setMailUris(final List<MailRouteConfig> mailUris) {
        this.mailUris = mailUris;
    }

    public String getImagePartition() {
        return imagePartition;
    }

    public void setImagePartition(final String imagePartition) {
        this.imagePartition = imagePartition;
    }

    public String getArticlePartition() {
        return articlePartition;
    }

    public void setArticlePartition(final String articlePartition) {
        this.articlePartition = articlePartition;
    }

    public String getTaxonomyId() {
        return taxonomyId;
    }

    public void setTaxonomyId(final String taxonomyId) {
        this.taxonomyId = taxonomyId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MailImporterConfig.class.getSimpleName() + "[", "]")
                .add("enabled=" + enabled)
                .add("articleAspect='" + articleAspect + "'")
                .add("imageAspect='" + imageAspect + "'")
                .add("attachmentNamePattern='" + attachmentNamePattern + "'")
                .add("articleNamePattern='" + articleNamePattern + "'")
                .add("acceptedImageExtensions=" + acceptedImageExtensions)
                .add("mailUris=" + mailUris)
                .add("imagePartition='" + imagePartition + "'")
                .add("articlePartition='" + articlePartition + "'")
                .add("taxonomyId='" + taxonomyId + "'")
                .toString();
    }

    public static class MailRouteConfig {
        private boolean enabled;
        private String uri;
        private String articlePartition;
        private String imagePartition;
        private String webPage;
        private String deskLevel;
        private String section;
        private String source;
        private String taxonomyId;
        private Map<String, Map<String, String>> fieldsDefaults = new HashMap<>();
        private Map<String, Map<String, String>> fieldsMappings = new HashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(final String uri) {
            this.uri = uri;
        }

        public String getArticlePartition() {
            return articlePartition;
        }

        public void setArticlePartition(final String articlePartition) {
            this.articlePartition = articlePartition;
        }

        public String getImagePartition() {
            return imagePartition;
        }

        public void setImagePartition(final String imagePartition) {
            this.imagePartition = imagePartition;
        }

        public String getWebPage() {
            return webPage;
        }

        public void setWebPage(final String webPage) {
            this.webPage = webPage;
        }

        public String getDeskLevel() {
            return deskLevel;
        }

        public void setDeskLevel(final String deskLevel) {
            this.deskLevel = deskLevel;
        }

        public String getSection() {
            return section;
        }

        public void setSection(final String section) {
            this.section = section;
        }

        public String getSource() {
            return source;
        }

        public void setSource(final String source) {
            this.source = source;
        }

        public String getTaxonomyId() {
            return taxonomyId;
        }

        public void setTaxonomyId(final String taxonomyId) {
            this.taxonomyId = taxonomyId;
        }

        public Map<String, Map<String, String>> getFieldsDefaults() {
            return fieldsDefaults;
        }

        public void setFieldsDefaults(final Map<String, Map<String, String>> fieldsDefaults) {
            this.fieldsDefaults = fieldsDefaults;
        }

        public Map<String, Map<String, String>> getFieldsMappings() {
            return fieldsMappings;
        }

        public void setFieldsMappings(final Map<String, Map<String, String>> fieldsMappings) {
            this.fieldsMappings = fieldsMappings;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", MailRouteConfig.class.getSimpleName() + "[", "]")
                    .add("enabled=" + enabled)
                    .add("uri='" + uri + "'")
                    .add("articlePartition='" + articlePartition + "'")
                    .add("imagePartition='" + imagePartition + "'")
                    .add("webPage='" + webPage + "'")
                    .add("deskLevel='" + deskLevel + "'")
                    .add("section='" + section + "'")
                    .add("source='" + source + "'")
                    .add("taxonomyId='" + taxonomyId + "'")
                    .add("fieldsDefaults=" + fieldsDefaults)
                    .add("fieldsMappings=" + fieldsMappings)
                    .toString();
        }
    }

}
