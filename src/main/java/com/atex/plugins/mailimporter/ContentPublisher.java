package com.atex.plugins.mailimporter;

import com.atex.onecms.content.*;
import com.atex.onecms.content.files.FileInfo;
import com.atex.onecms.content.files.FileService;
import com.atex.onecms.content.metadata.MetadataInfo;
import com.atex.onecms.image.ImageInfoAspectBean;
import com.polopoly.application.Application;
import com.polopoly.cm.client.*;
import com.polopoly.integration.IntegrationServerApplication;
import com.polopoly.metadata.Metadata;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Utility class used to publish a parsed {@link MailBean}
 * as content in Polopoly. The following applies to content
 * created by this Polopoly Mail Publishing integration:
 *
 * <ul>
 *   <li>All articles will be instances of <code>standard.Article</code>.</li>
 *   <li>All images will be instances of <code>standard.Image</code>.</li>
 *   <li>All content will have <code>PolopolyPost.d</code> as security parent.</li>
 * </ul>
 */
public class ContentPublisher
{
    public static final Subject SYSTEM_SUBJECT 	  = new Subject("98", null);
    public static final String SCHEME_TMP = "tmp";

    private final static String[] ACCEPTED_IMAGE_EXTENSIONS = { "jpg", "jpeg", "png", "gif", "zip", "jar" };
    public static final String MAIL_PROCESSOR_ARTICLEBEAN = "mail-processor.articlebean";
    public static final String MAIL_PROCESSOR_IMAGEBEAN = "mail-processor.imagebean";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private FileService fileService = null;
    private ContentManager contentManager = null;
    private MailProcessorUtils mailProcessorUtils = null;
    private String imageBeanName;

    public ContentPublisher()
    {
        imageBeanName = System.getProperty(MAIL_PROCESSOR_IMAGEBEAN, "com.atex.nosql.image.ImageContentDataBean");
    }

    private Application getApplication() {
        return IntegrationServerApplication.getPolopolyApplication();
    }

    public ContentId publish(final MailBean mail)
        throws Exception
    {

        /**
         * Please note that the mapping between data and Polopoly types we have to perform here will be
         * very much simplified in future versions of Polopoly, where the Data-API will have support for
         * write operations. It will then be possible to manage data mapping and conversions centralized
         * for the entire project.
         *
         * http://support.polopoly.com/confluence/display/Polopoly > Data-API.
         */

        Application application = getApplication();
        CmClient cmclient = getCmClient(application);
        if (contentManager == null) {
            contentManager = cmclient.getContentManager();
        }

        if (mailProcessorUtils == null) {
            mailProcessorUtils = new MailProcessorUtils(contentManager);
        }

        if (fileService == null) {
            fileService = getFileService(application);
        }

        try {
            Object articleBean = createArticle(mail);

            ContentResult<Object> cr = writeArticleBean(mailProcessorUtils, articleBean);

            return cr.getContentId().getContentId();
        } catch (CMException e) {
            throw new RuntimeException("Failed to publish contents!", e);
        }
    }

    public Object createArticle(MailBean mail) throws Exception {
        String articleBeanName = System.getProperty(MAIL_PROCESSOR_ARTICLEBEAN, "com.atex.nosql.article.ArticleBean");
        Object articleBean = mailProcessorUtils.getPopulatedArticleBean(articleBeanName, mail);


        List<ContentId> images = new ArrayList<>();
        for (String filename : mail.getAttachments().keySet()) {
            if (isAcceptedImageExtension(filename)) {
                ContentId contentId = createImage(mail, filename, mail.getAttachments().get(filename));
                images.add(0, contentId);
            }
        }

        BeanUtils.setProperty(articleBean,"images", images);
        return articleBean;
    }

    private ContentResult<Object> writeArticleBean(MailProcessorUtils mailProcessorUtils, Object articleBean) {
        ContentWriteBuilder<Object> cwb = new ContentWriteBuilder<>();
        cwb.mainAspectData(articleBean);
        cwb.type(articleBean.getClass().getName());

        InsertionInfoAspectBean insertionInfoAspectBean = mailProcessorUtils.getInsertionInfoAspectBean();
        cwb.aspect("p.InsertionInfo", insertionInfoAspectBean);

        ContentWrite<Object> content = cwb.buildCreate();
        return contentManager.create(content, SYSTEM_SUBJECT);
    }

    private FileService getFileService(Application application) throws com.polopoly.application.IllegalApplicationStateException {
        HttpFileServiceClient httpFileServiceClient = application.getPreferredApplicationComponent(HttpFileServiceClient.class);
        return httpFileServiceClient.getFileService();
    }

    private boolean isAcceptedImageExtension(final String filename)
    {
        for (String suffix : ACCEPTED_IMAGE_EXTENSIONS) {
            if (filename.toLowerCase().endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private ContentId createImage(MailBean mailBean, final String name, final byte[] imageData)  throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(imageData);

        String mimeType = mailProcessorUtils.getFormatName(bis);

        bis.reset();

        MailProcessorUtils.MetadataTagsHolder metadataTags = mailProcessorUtils.getMetadataTags(bis);


        bis.reset();

        FileInfo fInfo = fileService.uploadFile(SCHEME_TMP, null, name, bis, mimeType, SYSTEM_SUBJECT);

        FilesAspectBean filesAspectBean = mailProcessorUtils.getFilesAspectBean(fInfo);
        ImageInfoAspectBean imageInfoAspectBean = mailProcessorUtils.getImageInfoAspectBean(metadataTags.tags, fInfo);
        InsertionInfoAspectBean insertionInfoAspectBean = mailProcessorUtils.getInsertionInfoAspectBean();

        Object bean = mailProcessorUtils.getPopulatedImageBean(imageBeanName, mailBean, metadataTags, name);

        // leave creation date to prestore hook

        MetadataInfo metadataInfo = mailProcessorUtils.getMetadataInfo();
        Metadata metadata = new Metadata();

        metadataInfo.setMetadata(metadata);

        ContentWriteBuilder cwb = new ContentWriteBuilder();
        cwb.type(bean.getClass().getName());
        cwb.mainAspectData(bean);

        cwb.aspect(FilesAspectBean.ASPECT_NAME, filesAspectBean);
        cwb.aspect(ImageInfoAspectBean.ASPECT_NAME, imageInfoAspectBean);
        cwb.aspect(InsertionInfoAspectBean.ASPECT_NAME, insertionInfoAspectBean);
        cwb.aspect(MetadataInfo.ASPECT_NAME, metadataInfo);

        ContentWrite content = cwb.buildCreate();
        ContentResult cr = contentManager.create(content, SYSTEM_SUBJECT);
        if (!cr.getStatus().isOk()) {
            log.error("Error importing image: " + name + "." + cr.getStatus().toString());
        }
        log.info("Inserted image " + name + " with contentid: " + cr.getContentId().getContentId());

        return cr.getContentId().getContentId();
    }

    private CmClient getCmClient(Application application) {
        return (CmClient)application.getApplicationComponent(CmClientBase.DEFAULT_COMPOUND_NAME);
    }

}
