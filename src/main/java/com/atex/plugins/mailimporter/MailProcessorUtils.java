package com.atex.plugins.mailimporter;

import com.atex.onecms.content.*;
import com.atex.onecms.content.files.FileInfo;
import com.atex.onecms.content.metadata.MetadataInfo;
import com.atex.onecms.image.ImageInfoAspectBean;
import com.atex.plugins.structured.text.StructuredText;
import com.atex.standard.image.exif.MetadataTags;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import com.drew.metadata.icc.IccDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class MailProcessorUtils {

    private Logger log = LoggerFactory.getLogger(getClass());

    public static final String SECURITY_PARENT 	  = "dam.assets.common.d";
    public static final String TAXONOMY_ID = "PolopolyPost.d";

    private final ContentManager contentManager;
    private final MailImporterConfigPolicy config;

    public MailProcessorUtils(ContentManager contentManager, MailImporterConfigPolicy config) {
        this.contentManager = contentManager;
        this.config = config;
    }

    public String getFormatName(InputStream is) throws Exception {
        try {

            ImageInputStream iis = ImageIO.createImageInputStream(is);
            Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
            if (!iter.hasNext()) {
                return null;
            }
            ImageReader reader = (ImageReader) iter.next();
            iis.close();
            if (reader.getOriginatingProvider().getMIMETypes().length > 0)
                return reader.getOriginatingProvider().getMIMETypes()[0];
        } catch (IOException e) {
        }
        return null;
    }

    class BeanStrLookup extends StrLookup {

        private Object bean;
        private Map<String,String> map;

        BeanStrLookup(Object bean) {
            this.bean = bean;
        }

        BeanStrLookup(Object bean, Map<String,String> map) {
            this.bean = bean;
            this.map = map;
        }
        @Override
        public String lookup(String name) {
            try {
                return BeanUtils.getProperty(bean, name);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.error("failed to find bean field "+name,e);
            }
            if (map != null)
                return map.get(name);
            return null;
        }
    };

    public String expandBean(Object bean, Map<String, String> map, String property) {
        StrLookup resolver = new BeanStrLookup(bean,map);
        StrSubstitutor strSubstitutor = new StrSubstitutor(resolver);
        return strSubstitutor.replace(property);
    }

    class MetadataTagsHolder {
        MetadataTags tags;
        CustomMetadataTags customTags;
    }

    public MetadataTagsHolder getMetadataTags(InputStream is) throws ImageProcessingException, IOException {
        try (BufferedInputStream bis = new BufferedInputStream(is)) {
            MetadataTagsHolder result = new MetadataTagsHolder();
            com.drew.metadata.Metadata metadata = ImageMetadataReader.readMetadata(bis);
            result.tags = MetadataTags.extract(metadata);
            result.customTags = extract(metadata);
            return result;
        }
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
                metadataTags.setByline(directory.getDescription(type));
                break;
            case IptcDirectory.TAG_SOURCE:
                metadataTags.setSource(directory.getDescription(type));
                break;
            case IptcDirectory.TAG_CAPTION:
                metadataTags.setDescription(directory.getDescription(type));
                metadataTags.setCaption(directory.getDescription(type));
                break;
            case IptcDirectory.TAG_HEADLINE:
                metadataTags.setHeadline(directory.getDescription(type));
                break;
            case IptcDirectory.TAG_COPYRIGHT_NOTICE:
                metadataTags.setCopyright(directory.getDescription(type));
                break;
            case IptcDirectory.TAG_CREDIT:
                metadataTags.setCredit(directory.getDescription(type));
                break;
            case IptcDirectory.TAG_KEYWORDS:
                metadataTags.setKeywords(directory.getDescription(type));
                break;
            case IptcDirectory.TAG_CATEGORY:
                metadataTags.setSubject(directory.getDescription(type));
                break;
            case IptcDirectory.TAG_DATE_CREATED:
                String dateString = directory.getDescription(type);
                metadataTags.setDateCreated(StringUtils.getNormalizedDateString(dateString));
                break;
            case IptcDirectory.TAG_COUNTRY_OR_PRIMARY_LOCATION_NAME:
            case IptcDirectory.TAG_SUB_LOCATION:
                if (metadataTags.getLocation() == null) {
                    metadataTags.setLocation(directory.getDescription(type));
                } else
                    metadataTags.setLocation(metadataTags.getLocation() + ";" + directory.getDescription(type));
                break;
            default:
                break;
        }
    }


    public FilesAspectBean getFilesAspectBean(FileInfo fInfo) {
        // atex.Files
        FilesAspectBean filesAspectBean = new FilesAspectBean();
        ContentFileInfo contentFileInfo = new ContentFileInfo(fInfo.getOriginalPath(), fInfo.getURI());
        HashMap<String, ContentFileInfo> files = new HashMap<String, ContentFileInfo>();
        files.put(fInfo.getOriginalPath(), contentFileInfo);
        filesAspectBean.setFiles(files);
        return filesAspectBean;
    }

    public ImageInfoAspectBean getImageInfoAspectBean(MetadataTags metadataTags, FileInfo fInfo) {
        // atex.Image
        ImageInfoAspectBean imageInfoAspectBean = new ImageInfoAspectBean();
        imageInfoAspectBean.setHeight(metadataTags.getImageHeight());
        imageInfoAspectBean.setWidth(metadataTags.getImageWidth());
        imageInfoAspectBean.setFilePath(fInfo.getOriginalPath());
        return imageInfoAspectBean;
    }

    public InsertionInfoAspectBean getInsertionInfoAspectBean() {
        // p.InsertionInfo
        ContentId securityParentContentId = contentManager.resolve(SECURITY_PARENT, Subject.NOBODY_CALLER).getContentId();

        InsertionInfoAspectBean insertionInfoAspectBean = new InsertionInfoAspectBean(securityParentContentId);
        insertionInfoAspectBean.setInsertParentId(securityParentContentId);
        return insertionInfoAspectBean;
    }

    public MetadataInfo getMetadataInfo() {
        MetadataInfo metadataInfo = new MetadataInfo();
        Set<String> set = new HashSet<String>();
        set.add(TAXONOMY_ID);
        metadataInfo.setTaxonomyIds(set);
        return metadataInfo;
    }

    public Object getPopulatedImageBean(String imageBeanName, MailBean mailBean, MailProcessorUtils.MetadataTagsHolder metadataTags, String filename) {
        try {
            Object bean = Class.forName(imageBeanName).newInstance();
            String byline = metadataTags.customTags.getByline();
            setProperty(bean, "byline", byline);
            String subject = metadataTags.customTags.getSubject();
            setProperty(bean, "section", subject);
            Integer imageWidth = metadataTags.tags.getImageWidth();
            setProperty(bean, "width", imageWidth);
            Integer imageHeight = metadataTags.tags.getImageHeight();
            setProperty(bean, "height", imageHeight);
            String description = metadataTags.customTags.getDescription();
            setProperty(bean, "description", description);
            Map<String,String> map = new HashMap<>();
            map.put("filename",filename);
            map.put("width", Integer.toString(imageWidth));
            map.put("height", Integer.toString(imageHeight));
            map.put("description", description);
            map.put("section", subject);
            map.put("byline", byline);
            String name = expandBean(mailBean,map,config.getImageNamePattern());
            setProperty(bean,"name", name);
            return bean;
        } catch (IllegalAccessException | ClassNotFoundException | InstantiationException e) {
            log.error("Could not create image bean",e);
        }
        return null;
    }

    private void setProperty(Object bean, String field, Object value) {
        try {
            Class propertyType = PropertyUtils.getPropertyType(bean, field);
            if (propertyType.getName().equals("com.atex.plugins.structured.text.StructuredText") ) {
                StructuredText structuredText = (StructuredText)PropertyUtils.getProperty(bean,field);
                if (structuredText == null) {
                    structuredText = new StructuredText();
                }
                structuredText.setText((String)value);
                PropertyUtils.setProperty(bean, field, structuredText);
            } else if (propertyType.equals("java.lang.String")){
                BeanUtils.setProperty(bean, field, value);
            } else
                throw new RuntimeException("unknown type");
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException e) {
            log.error("unable to use type to set properties",e);
        }

    }

    public Object getPopulatedArticleBean(String articleBeanName, MailBean mail) {
        try {
            Object articleBean = Class.forName(articleBeanName).newInstance();
            String articlePattern = config.getArticleNamePattern();
            String name = expandBean(mail, null, articlePattern);
            setProperty(articleBean, "name", name);
            StructuredText body = new StructuredText();
            body.setText(mail.getBody());
            BeanUtils.setProperty(articleBean, "body", body);
            StructuredText headline = new StructuredText();
            headline.setText(mail.getSubject());
            BeanUtils.setProperty(articleBean, "headline", headline);
            return articleBean;
        } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException | InstantiationException e) {
            log.error("Failed to create article bean",e);
            return null;
        }
    }



}
