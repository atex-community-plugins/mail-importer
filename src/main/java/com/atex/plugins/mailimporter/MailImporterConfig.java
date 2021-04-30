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

    public static class Signature {
        private int before = -1;
        private String regex = "";

        public int getBefore() {
            return before;
        }

        public void setBefore(final int before) {
            this.before = before;
        }

        public String getRegex() {
            return regex;
        }

        public void setRegex(final String regex) {
            this.regex = regex;
        }

        public static Signature of(final String regex,
                                   final int before) {
            final Signature signature = new Signature();
            signature.setRegex(regex);
            signature.setBefore(before);
            return signature;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Signature.class.getSimpleName() + "[", "]")
                    .add("before=" + before)
                    .add("regex=" + regex)
                    .toString();
        }
    }

    public static class MailRouteConfig {
        private boolean enabled;
        private String uri;
        private String articleAspect;
        private String imageAspect;
        private String articlePartition;
        private String imagePartition;
        private String webPage;
        private String deskLevel;
        private String section;
        private String source;
        private String taxonomyId;
        private String principalId;
        private int minWords;
        private long imageMinSize;
        private Map<String, Map<String, String>> fieldsDefaults = new HashMap<>();
        private Map<String, Map<String, String>> fieldsMappings = new HashMap<>();
        private List<Signature> signatures = new ArrayList<>();

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

        public String getPrincipalId() {
            return principalId;
        }

        public void setPrincipalId(final String principalId) {
            this.principalId = principalId;
        }

        public int getMinWords() {
            return minWords;
        }

        public void setMinWords(final int minWords) {
            this.minWords = minWords;
        }

        public long getImageMinSize() {
            return imageMinSize;
        }

        public void setImageMinSize(final long imageMinSize) {
            this.imageMinSize = imageMinSize;
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

        public List<Signature> getSignatures() {
            return signatures;
        }

        public void setSignatures(final List<Signature> signatures) {
            this.signatures = signatures;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", MailRouteConfig.class.getSimpleName() + "[", "]")
                    .add("enabled=" + enabled)
                    .add("uri='" + uri + "'")
                    .add("articleAspect='" + articleAspect + "'")
                    .add("imageAspect='" + imageAspect + "'")
                    .add("articlePartition='" + articlePartition + "'")
                    .add("imagePartition='" + imagePartition + "'")
                    .add("webPage='" + webPage + "'")
                    .add("deskLevel='" + deskLevel + "'")
                    .add("section='" + section + "'")
                    .add("source='" + source + "'")
                    .add("taxonomyId='" + taxonomyId + "'")
                    .add("principalId='" + principalId + "'")
                    .add("minWords='" + minWords + "'")
                    .add("imageMinSize='" + imageMinSize + "'")
                    .add("fieldsDefaults=" + fieldsDefaults)
                    .add("fieldsMappings=" + fieldsMappings)
                    .add("signatures=" + signatures)
                    .toString();
        }
    }

}
