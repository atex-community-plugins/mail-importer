package com.atex.plugins.mailimporter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atex.onecms.content.ContentFileInfo;
import com.atex.onecms.content.ContentId;
import com.atex.onecms.content.ContentManager;
import com.atex.onecms.content.ContentVersionId;
import com.atex.onecms.content.FilesAspectBean;
import com.atex.onecms.content.InsertionInfoAspectBean;
import com.atex.onecms.content.Subject;
import com.atex.onecms.content.files.FileInfo;
import com.atex.onecms.content.metadata.MetadataInfo;
import com.atex.onecms.image.ImageInfoAspectBean;
import com.atex.onecms.image.exif.MetadataTagsAspectBean;
import com.atex.plugins.mailimporter.MailImporterConfig.MailRouteConfig;
import com.atex.plugins.structured.text.StructuredText;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import com.drew.metadata.icc.IccDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import com.polopoly.metadata.Metadata;
import com.polopoly.model.ModelDomain;
import com.polopoly.model.ModelType;
import com.polopoly.model.ModelTypeBean;

public class MailProcessorUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MailProcessorUtils.class);

    private static final Subject SYSTEM_SUBJECT = new Subject("98", "");

    private final ContentManager contentManager;
    private final ModelDomain modelDomain;

    public MailProcessorUtils(final ContentManager contentManager,
                              final ModelDomain modelDomain) {
        this.contentManager = contentManager;
        this.modelDomain = modelDomain;
    }

    static class BeanStrLookup extends StrLookup {

        private Object bean;
        private Map<String,String> map;

        BeanStrLookup(Object bean, Map<String,String> map) {
            this.bean = bean;
            this.map = map;
        }
        @Override
        public String lookup(String name) {
            try {
                return BeanUtils.getProperty(bean, name);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                // ignore missing property
            }
            if (map != null)
                return map.get(name);
            LOG.warn("failed to find property "+name+" in bean");
            return null;
        }
    };

    public String expandBean(Object bean, Map<String, String> map, String property) {
        StrLookup resolver = new BeanStrLookup(bean, map);
        StrSubstitutor strSubstitutor = new StrSubstitutor(resolver);
        return strSubstitutor.replace(property);
    }

    public static class MetadataTagsHolder {
        MetadataTagsAspectBean tags;
        CustomMetadataTags customTags;
    }

    public MetadataTagsHolder getMetadataTags(final InputStream is) throws ImageProcessingException, IOException {
        final MetadataTagsHolder result = new MetadataTagsHolder();

        // resetting the stream is not an option when the stream is quite large, so we have to copy
        // the stream to a temporary file, we are not using an array since it means we will have a
        // very large array sitting in memory, but copying to a file in the fileSystem we will relay
        // on the os files cache.

        final Path tempFile = Files.createTempFile("image.", ".jpg");
        try {
            try (final OutputStream os = Files.newOutputStream(tempFile, StandardOpenOption.APPEND)) {
                IOUtils.copy(is, os);
            }
            try (final InputStream fis = Files.newInputStream(tempFile, StandardOpenOption.READ)) {
                com.drew.metadata.Metadata metadata = ImageMetadataReader.readMetadata(fis);
                result.customTags = extract(metadata);
            }
            try (final InputStream fis = Files.newInputStream(tempFile, StandardOpenOption.READ)) {
                try (final BufferedInputStream bfis = new BufferedInputStream(fis)) {
                    final Optional<MetadataTagsAspectBean> metadataTags = new ImageMetadataExtraction().extract(bfis);
                    metadataTags.ifPresent(metadataTagsAspectBean -> result.tags = metadataTagsAspectBean);
                }
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
        return result;
    }

    public CustomMetadataTags extract(com.drew.metadata.Metadata metadata) {

        Map<String, Map<String, ?>> tags = new HashMap<>();
        CustomMetadataTags customMetadataTags = new CustomMetadataTags();

        for (Directory directory : metadata.getDirectories()) {
            if (!(directory instanceof IccDirectory)) {
                Map<String, Object> tagsInDirectory = new HashMap<>();
                for (Tag tag : directory.getTags()) {
                    if (directory instanceof IptcDirectory) {
                        readIptcDirectoryTag(directory, tag, customMetadataTags);
                    }
                }
                tags.put(directory.getName(), tagsInDirectory);
            }
        }
        customMetadataTags.setTags(tags);
        return customMetadataTags;
    }

    public void readIptcDirectoryTag(final Directory directory,
                                     final Tag tag,
                                     final CustomMetadataTags metadataTags) {
        int type = tag.getTagType();

        switch (type) {
            case IptcDirectory.TAG_BY_LINE:
                metadataTags.setByline(trim(directory.getDescription(type)));
                break;
            case IptcDirectory.TAG_SOURCE:
                metadataTags.setSource(trim(directory.getDescription(type)));
                break;
            case IptcDirectory.TAG_CAPTION:
                metadataTags.setDescription(trim(directory.getDescription(type)));
                metadataTags.setCaption(trim(directory.getDescription(type)));
                break;
            case IptcDirectory.TAG_HEADLINE:
                metadataTags.setHeadline(trim(directory.getDescription(type)));
                break;
            case IptcDirectory.TAG_COPYRIGHT_NOTICE:
                metadataTags.setCopyright(trim(directory.getDescription(type)));
                break;
            case IptcDirectory.TAG_CREDIT:
                metadataTags.setCredit(trim(directory.getDescription(type)));
                break;
            case IptcDirectory.TAG_KEYWORDS:
                metadataTags.setKeywords(trim(directory.getDescription(type)));
                break;
            case IptcDirectory.TAG_CATEGORY:
                metadataTags.setSubject(trim(directory.getDescription(type)));
                break;
            case IptcDirectory.TAG_DATE_CREATED:
                String dateString = directory.getDescription(type);
                metadataTags.setDateCreated(StringUtils.getNormalizedDateString(dateString));
                break;
            case IptcDirectory.TAG_COUNTRY_OR_PRIMARY_LOCATION_NAME:
            case IptcDirectory.TAG_SUB_LOCATION:
                final String desc = trim(directory.getDescription(type));
                if (metadataTags.getLocation() == null) {
                    metadataTags.setLocation(desc);
                } else {
                    metadataTags.setLocation(metadataTags.getLocation() + ";" + desc);
                }
                break;
            default:
                break;
        }
    }

    private String trim(final String value) {
        if (value != null) {
            return value.trim();
        }
        return null;
    }

    public FilesAspectBean getFilesAspectBean(FileInfo fInfo) {
        // atex.Files
        FilesAspectBean filesAspectBean = new FilesAspectBean();
        ContentFileInfo contentFileInfo = new ContentFileInfo(fInfo.getOriginalPath(), fInfo.getURI());
        Map<String, ContentFileInfo> files = new HashMap<>();
        files.put(fInfo.getOriginalPath(), contentFileInfo);
        filesAspectBean.setFiles(files);
        return filesAspectBean;
    }

    public ImageInfoAspectBean getImageInfoAspectBean(MetadataTagsAspectBean metadataTags, FileInfo fInfo) {
        // atex.Image
        ImageInfoAspectBean imageInfoAspectBean = new ImageInfoAspectBean();
        if (metadataTags != null) {
            Optional.ofNullable(metadataTags.getImageHeight())
                    .ifPresent(imageInfoAspectBean::setHeight);
            Optional.ofNullable(metadataTags.getImageWidth())
                    .ifPresent(imageInfoAspectBean::setWidth);
        }
        imageInfoAspectBean.setFilePath(fInfo.getOriginalPath());
        return imageInfoAspectBean;
    }

    public InsertionInfoAspectBean getInsertionInfoAspectBean(final MailRouteConfig config) {
        final InsertionInfoAspectBean bean = new InsertionInfoAspectBean();
        if (StringUtils.notEmpty(config.getDeskLevel())) {
            resolve(config.getDeskLevel()).ifPresent(bean::setSecurityParentId);
        }
        if (StringUtils.notEmpty(config.getWebPage())) {
            resolve(config.getWebPage()).ifPresent(bean::setInsertParentId);
        }
        return bean;
    }

    private Optional<ContentId> resolve(final String externalId) {
        return Optional.ofNullable(contentManager.resolve(externalId, SYSTEM_SUBJECT))
                .map(ContentVersionId::getContentId);
    }

    public MetadataInfo getMetadataInfo(final String taxonomyId) {
        MetadataInfo metadataInfo = new MetadataInfo();
        Set<String> set = new HashSet<>();
        set.add(taxonomyId);
        metadataInfo.setTaxonomyIds(set);
        metadataInfo.setMetadata(new Metadata());
        return metadataInfo;
    }

    public Object getPopulatedImageBean(final MailImporterConfig config,
                                        final MailRouteConfig routeConfig,
                                        final MailBean mailBean,
                                        final MailProcessorUtils.MetadataTagsHolder metadataTags,
                                        final String filename,
                                        final int imageNumber,
                                        final int imageCount) {
        try {
            LOG.debug("Populate image from {}", routeConfig.getImageAspect());

            final Object bean = createBean(routeConfig.getImageAspect());

            final Map<String, Object> values = new HashMap<>();
            if (metadataTags.customTags != null) {
                values.put("byline", metadataTags.customTags.getByline());
                values.put("section", metadataTags.customTags.getSubject());
                values.put("description", metadataTags.customTags.getDescription());
            }
            if (metadataTags.tags != null) {
                values.put("width", metadataTags.tags.getImageWidth());
                values.put("height", metadataTags.tags.getImageHeight());
            }
            if (StringUtils.notEmpty(routeConfig.getSection())) {
                values.put("section", routeConfig.getSection());
            }
            if (StringUtils.notEmpty(routeConfig.getSource())) {
                values.put("source", routeConfig.getSource());
            }
            if (StringUtils.notEmpty(mailBean.getLead())) {
                values.put("byline", mailBean.getLead());
            }

            final List<String> names = Arrays.asList(
                    "byline",
                    "section",
                    "width",
                    "height",
                    "description"
            );
            final Map<String,String> map = new HashMap<>();
            for (final String name : names) {
                map.put(name, toString(values.get(name)));
            }
            map.put("filename", filename);
            if (imageCount > 1) {
                map.put("number", Integer.toString(imageNumber));
                map.put("count", Integer.toString(imageCount));
            }
            values.put("name", expandBean(mailBean, map, config.getAttachmentNamePattern()));

            setProperties(bean, values, routeConfig, routeConfig.getImageAspect());
            return bean;
        } catch (IllegalAccessException | InstantiationException e) {
            LOG.error("Could not create image bean", e);
        }
        return null;
    }

    private String toString(final Object value) {
        if (value != null) {
            return Objects.toString(value);
        }
        return null;
    }

    private void setProperty(final Object bean,
                             final String field,
                             final Object value) {
        try {
            final Class<?> propertyType = PropertyUtils.getPropertyType(bean, field);
            if (propertyType == null) {
                LOG.error("Field " + field + " does not exists in class " + bean.getClass());
                return;
            }
            LOG.debug("setProperty on field " + field + " (type is " + propertyType.getName() + ", value type is " + ((value != null) ? value.getClass().getName() : "null") + ")");
            if (propertyType.getName().equals("com.atex.plugins.structured.text.StructuredText") ) {
                StructuredText structuredText = (StructuredText) PropertyUtils.getProperty(bean, field);
                if (structuredText == null) {
                    structuredText = new StructuredText();
                }
                structuredText.setText((String) value);
                PropertyUtils.setProperty(bean, field, structuredText);
            } else {
                BeanUtils.setProperty(bean, field, value);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException e) {
            LOG.error("unable to use type to set property " + field + " to value " + value, e);
        }
    }

    public Object getPopulatedArticleBean(final MailImporterConfig config,
                                          final MailRouteConfig routeConfig,
                                          final MailBean mail) {
        try {
            LOG.debug("Populate article from {}", routeConfig.getArticleAspect());

            final Object articleBean = createBean(routeConfig.getArticleAspect());

            final String articlePattern = config.getArticleNamePattern();
            final String name = expandBean(mail, null, articlePattern);

            final Map<String, Object> values = new HashMap<>();
            values.put("name", name);
            values.put("body", mail.getBody());
            values.put("headline", mail.getSubject());
            values.put("lead", mail.getLead());
            if (StringUtils.notEmpty(routeConfig.getSection())) {
                values.put("section", routeConfig.getSection());
            }
            if (StringUtils.notEmpty(routeConfig.getSource())) {
                values.put("source", routeConfig.getSource());
            }
            setProperties(articleBean, values, routeConfig, routeConfig.getArticleAspect());
            return articleBean;
        } catch (IllegalAccessException | InstantiationException e) {
            LOG.error("Failed to create article bean", e);
            return null;
        }
    }

    public Map<String, String> getContentBean(final Object bean,
                                              final List<String> fields,
                                              final MailRouteConfig routeConfig,
                                              final String aspectName) {

        return getProperties(
                bean,
                fields,
                routeConfig,
                aspectName
        );
    }

    private Object createBean(final String aspectName) throws IllegalAccessException, InstantiationException {
        LOG.debug("Create bean for {}", aspectName);

        final ModelType typeFromAspectName = modelDomain.getModelType(aspectName);
        assert typeFromAspectName != null;

        final Class<?> fromAspectNameClass = ((ModelTypeBean) typeFromAspectName).getBeanClass();
        assert fromAspectNameClass != null;

        final Object bean = fromAspectNameClass.newInstance();

        LOG.debug("Created classes is {}", bean.getClass().getName());

        return bean;
    }

    private void setProperties(final Object bean,
                               final Map<String, Object> values,
                               final MailRouteConfig routeConfig,
                               final String aspectName) {
        final Map<String, String> fieldsMappings = Optional.ofNullable(routeConfig.getFieldsMappings())
                                                           .map(m -> m.get(aspectName))
                                                           .orElse(new HashMap<>());

        final Map<String, String> fieldsDefaults = Optional.ofNullable(routeConfig.getFieldsDefaults())
                                                           .map(m -> m.get(aspectName))
                                                           .orElse(new HashMap<>());
        fieldsDefaults.entrySet()
                      .stream()
                      .filter(e -> StringUtils.notEmpty(e.getValue()))
                      .forEach(e -> values.put(e.getKey(), e.getValue()));
        for (final Map.Entry<String, Object> entry : values.entrySet()) {
            final String fieldName = fieldsMappings.getOrDefault(entry.getKey(), entry.getKey());
            setProperty(bean, fieldName, entry.getValue());
        }
    }

    private Map<String, String> getProperties(final Object bean,
                                              final List<String> fields,
                                              final MailRouteConfig routeConfig,
                                              final String aspectName) {
        final Map<String, String> fieldsMappings = Optional.ofNullable(routeConfig.getFieldsMappings())
                                                           .map(m -> m.get(aspectName))
                                                           .orElse(new HashMap<>());
        final Map<String, String> results = new HashMap<>();
        for (final String field : fields) {
            final String fieldName = fieldsMappings.getOrDefault(field, field);
            results.put(field, getProperty(bean, fieldName));
        }
        return results;
    }

    private String getProperty(final Object bean,
                               final String field) {
        try {
            final Class<?> propertyType = PropertyUtils.getPropertyType(bean, field);
            if (propertyType == null) {
                LOG.error("Field " + field + " does not exists in class " + bean.getClass());
                return field;
            }
            LOG.debug("setProperty on field " + field + " (type is " + propertyType.getName() + ")");
            if (propertyType.getName().equals("com.atex.plugins.structured.text.StructuredText") ) {
                final StructuredText structuredText = (StructuredText) PropertyUtils.getProperty(bean, field);
                if (structuredText != null) {
                    return structuredText.getText();
                }
            } else {
                return BeanUtils.getProperty(bean, field);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException e) {
            LOG.error("unable to use type to get property " + field, e);
        }
        return null;
    }

}
