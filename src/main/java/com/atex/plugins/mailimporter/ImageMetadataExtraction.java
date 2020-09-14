package com.atex.plugins.mailimporter;

import java.io.InputStream;
import java.util.Optional;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atex.onecms.image.exif.MetadataTagsAspectBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.polopoly.common.lang.StringUtil;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * ImageMetadataExtraction
 *
 * @author mnova
 */
public class ImageMetadataExtraction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageMetadataExtraction.class.getName());

    private static final String SYSTEM_PARAMETER = "image.metadata.service.url";
    private static final String DEFAULT_URL =  "http://localhost:8080/image-metadata-extractor-service/image";

    private static final Gson GSON = new GsonBuilder().create();

    private final String serviceUrl;
    private final Client client = Client.create();

    public ImageMetadataExtraction(final String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public ImageMetadataExtraction() {
        this(Optional
                .ofNullable(System.getProperty(SYSTEM_PARAMETER))
                .filter(StringUtil::notEmpty)
                .orElse(DEFAULT_URL));
    }

    public Optional<MetadataTagsAspectBean> extract(final InputStream inputStream) {
        final String mimeType = MimeTypeUtils.getMimeType(inputStream)
                .orElse("image/jpeg");
        return extract(inputStream, mimeType);
    }

    public Optional<MetadataTagsAspectBean> extract(final InputStream inputStream,
                                                    final String mimeType) {
        try {
            final String threadId = getThreadId();
            LOGGER.debug(threadId + "Calling service at {} with mimeType {}", serviceUrl, mimeType);

            final long startTime = System.currentTimeMillis();

            final String json = client
                    .resource(serviceUrl)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .entity(inputStream)
                    .type(MediaType.valueOf(mimeType))
                    .post(String.class);

            final long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.debug(threadId + "Result (took {}ms) is: {}", elapsed, json);

            if (StringUtil.notEmpty(json)) {
                return Optional.of(GSON.fromJson(json, MetadataTagsAspectBean.class));
            }
        } catch (UniformInterfaceException e) {
            logError(e, e.getResponse().getEntity(String.class));
        } catch (Exception e) {
            logError(e, null);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return Optional.empty();
    }

    private void logError(final Exception e, final String errorMsg) {
        if (StringUtil.isEmpty(errorMsg)) {
            LOGGER.error(getThreadId() + "Error calling image metadata extraction service", e);
        } else {
            LOGGER.error(getThreadId() + "Error calling image metadata extraction service: {}", errorMsg);
        }
    }

    private String getThreadId() {
        return String.format("[%d] ", Thread.currentThread().getId());
    }
}
