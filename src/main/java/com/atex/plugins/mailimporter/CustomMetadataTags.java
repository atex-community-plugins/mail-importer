package com.atex.plugins.mailimporter;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class CustomMetadataTags {

    private Map<String, Map<String, ?>> tags;

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
