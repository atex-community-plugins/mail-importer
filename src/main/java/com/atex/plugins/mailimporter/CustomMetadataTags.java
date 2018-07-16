package com.atex.plugins.mailimporter;

import com.atex.onecms.image.exif.MetadataTagsAspectBean;
import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.icc.IccDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class CustomMetadataTags {

    private Map<String, Map<String, ?>> tags;

    protected static final Logger log = LoggerFactory.getLogger(CustomMetadataTags.class);
    
    private static final int IMAGE_RIGHTS_CUSTOM_FIELD = 730;
    private static final String CUSTOM_FIELD_19 = "CustomField19";

    private String byline;
    private String caption;
    private String credit;
    private String headline;
    private String location;
    private String description;
    private String subject;
    private String keywords;
    private String source;
    private String dateCreated;
    private Date dateCreatedAsDate;
    private String copyright;

    public static CustomMetadataTags extract(Metadata metadata) {

        CustomMetadataTags customMetadataTags = new CustomMetadataTags();

        customMetadataTags.tags = new HashMap<>();

        for (Directory directory : metadata.getDirectories()) {
            if (!(directory instanceof IccDirectory)) {
                Map<String, Object> tagsInDirectory = new HashMap<>();
                for (Tag tag : directory.getTags()) {
                    if(directory instanceof IptcDirectory && tag.getTagType() == IMAGE_RIGHTS_CUSTOM_FIELD){
                        tagsInDirectory.put(CUSTOM_FIELD_19, getTagValue(directory, tag));
                    }else{
                        tagsInDirectory.put(tag.getTagName(), getTagValue(directory, tag));
                    }
                    if (directory instanceof IptcDirectory) {
                        readIptcDirectoryTag(directory, tag, customMetadataTags);
                    }
                }
                customMetadataTags.tags.put(directory.getName(), tagsInDirectory);
            }

        }

        MetadataTagsAspectBean meta = new MetadataTagsAspectBean();

        meta.setTags(customMetadataTags.tags);

        return customMetadataTags;
    }


    private static Object getTagValue(final Directory directory, final Tag tag) {
        Object tagValue = directory.getObject(tag.getTagType());
        if (!(tagValue instanceof Rational) && tagValue instanceof Number) {
            return tagValue;
        }
        return tag.getDescription();
    }

    public static void readIptcDirectoryTag(final Directory directory,
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
            case IptcDirectory.TAG_CREDIT:
                metadataTags.setCredit(directory.getDescription(type));
                break;
            case IptcDirectory.TAG_KEYWORDS:
                metadataTags.setKeywords(directory.getDescription(type));
                break;
            case IptcDirectory.TAG_CATEGORY:
                metadataTags.setSubject(directory.getDescription(type));
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

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getByline() {
        return byline;
    }

    public void setByline(String byline) {
        this.byline = byline;
    }

    public String getCredit() {
        return credit;
    }

    public void setCredit(String credit) {
        this.credit = credit;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Map<String, Map<String, ?>> getTags() {
        return new HashMap<>(tags);
    }

    public void setTags(final Map<String, Map<String, ?>> tags) {
        this.tags = tags;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date date) {
        if (date == null) {
            dateCreated = null;
        } else {
            dateCreated = StringUtils.dateToUTCDateString(date);
        }
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public void setDateCreatedAsDate(Date dateCreatedAsDate) {
        this.dateCreatedAsDate = dateCreatedAsDate;
    }

    public Date getDateCreatedAsDate() {
        return dateCreatedAsDate;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public String getCopyright() {
        return copyright;
    }
}
