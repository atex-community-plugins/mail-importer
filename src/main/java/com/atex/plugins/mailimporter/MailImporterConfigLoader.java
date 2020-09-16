package com.atex.plugins.mailimporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.atex.plugins.baseline.policy.BaselinePolicy;
import com.atex.plugins.mailimporter.MailImporterConfig.MailRouteConfig;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.polopoly.cm.ExternalContentId;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.util.StringUtil;

/**
 * This is the mail importer configuration loader.
 *
 * We did not have created a policy for the configuration because we only
 * need this plugin to live in the server-integration webapp, if the configuration
 * depended on a policy defined in this plugin we would had to make it available
 * everywhere which is not strictly necessary.
 *
 * @author mnova
 */
public class MailImporterConfigLoader {

    public static final String CONFIG_EXT_ID = "plugins.com.atex.plugins.mail-importer.Config";

    private static final String ARTICLE_ASPECT = "article_aspect";
    private static final String IMAGE_ASPECT = "image_aspect";
    private static final String ACCEPTED_IMAGE_EXTENSIONS = "accepted_image_extensions";
    private static final String ATTACHMENT_NAME_PATTERN = "attachment_name_pattern";
    private static final String ARTICLE_NAME_PATTERN = "article_name_pattern";
    private static final String MAIL_URI = "mail_uri";
    private static final String MAILIMPORTER_ENABLED = "mailimporter_enabled";
    private static final String IMAGE_PARTITION = "image_partition";
    private static final String ARTICLE_PARTITION = "article_partition";
    private static final String TAXONOMYID = "taxonomyId";
    private static final String JSON = "json";

    private final BaselinePolicy baselinePolicy;

    private MailImporterConfigLoader(final BaselinePolicy baselinePolicy) {
        this.baselinePolicy = baselinePolicy;
    }

    public String getArticleAspect() {
        return Strings.nullToEmpty(getChildValue(ARTICLE_ASPECT, "atex.onecms.article"));
    }

    public String getImageAspect() {
        return Strings.nullToEmpty(getChildValue(IMAGE_ASPECT, "atex.onecms.image"));
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

    private List<String> getChildValueSplit(final String name,
                                            final String sep,
                                            final String defaultValue) {
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

    public String getImagePartition() {
        return Strings.nullToEmpty(getChildValue(IMAGE_PARTITION, ""));
    }

    public String getArticlePartition() {
        return Strings.nullToEmpty(getChildValue(ARTICLE_PARTITION, ""));
    }

    public String getTaxonomyId() {
        return getChildValue(TAXONOMYID, "PolopolyPost.d");
    }

    public boolean isEnabled() {
        return Boolean.parseBoolean(getChildValue(MAILIMPORTER_ENABLED, "false"));
    }

    public String getJson() {
        return getChildValue(JSON, "{}");
    }

    private MailRouteConfig createRouteConfig(final String uri) {
        final MailRouteConfig route = new MailRouteConfig();
        route.setEnabled(isEnabled());
        route.setArticlePartition(getArticlePartition());
        route.setImagePartition(getImagePartition());
        route.setUri(uri);
        route.setTaxonomyId(getTaxonomyId());
        route.setPrincipalId("98");
        route.setArticleAspect(getArticleAspect());
        route.setImageAspect(getImageAspect());
        return route;
    }

    private MailRouteConfig createRouteConfig(final JsonObject json) {
        final MailRouteConfig route = new MailRouteConfig();
        route.setEnabled(isEnabled());
        route.setArticlePartition(getArticlePartition());
        route.setImagePartition(getImagePartition());
        route.setTaxonomyId(getTaxonomyId());
        getPrimitive(json, "enabled", JsonElement::getAsBoolean, route::setEnabled);
        getPrimitive(json, "uri", JsonElement::getAsString, route::setUri);
        getPrimitive(json, "articlePartition", JsonElement::getAsString,
                StringUtils::notEmpty, route::setArticlePartition);
        getPrimitive(json, "imagePartition", JsonElement::getAsString,
                StringUtils::notEmpty, route::setImagePartition);
        getPrimitive(json, "articleAspect", JsonElement::getAsString,
                StringUtils::notEmpty, route::setArticleAspect);
        getPrimitive(json, "imageAspect", JsonElement::getAsString,
                StringUtils::notEmpty, route::setImageAspect);
        return route.isEnabled() ? route : null;
    }

    private static <T> void getPrimitive(final JsonObject json,
                                         final String name,
                                         final Function<JsonElement, T> map,
                                         final Consumer<T> consumer) {
        getPrimitive(json, name, map, x -> true, consumer);
    }

    private static <T> void getPrimitive(final JsonObject json,
                                         final String name,
                                         final Function<JsonElement, T> map,
                                         final Predicate<T> validator,
                                         final Consumer<T> consumer) {
        if (json.has(name)) {
            final JsonElement element = json.get(name);
            if (element.isJsonPrimitive() && !element.isJsonNull()) {
                Stream.of(element)
                      .map(map)
                      .filter(validator)
                      .forEach(consumer);
            }
        }
    }

    public static MailImporterConfig createConfig(final MailImporterConfigLoader loader) {
        final MailImporterConfig config = new MailImporterConfig();
        config.setEnabled(loader.isEnabled());
        config.setArticleAspect(loader.getArticleAspect());
        config.setImageAspect(loader.getImageAspect());
        config.setArticleNamePattern(loader.getArticleNamePattern());
        config.setAttachmentNamePattern(loader.getAttachmentNamePattern());
        config.setAcceptedImageExtensions(Optional.ofNullable(loader.getAcceptedImageExtensions())
                                                  .orElse(new ArrayList<>()));
        config.setImagePartition(loader.getImagePartition());
        config.setArticlePartition(loader.getArticlePartition());
        config.setTaxonomyId(loader.getTaxonomyId());
        if (StringUtils.notEmpty(loader.getMailUri())) {
            config.getMailUris().add(loader.createRouteConfig(loader.getMailUri()));
        }
        final String jsonString = loader.getJson();
        if (StringUtils.notEmpty(jsonString) && loader.isEnabled()) {
            final JsonElement jsonElement = new JsonParser().parse(jsonString);
            final AtomicReference<String> defWebPage = new AtomicReference<>(null);
            final AtomicReference<String> defDeskLevel = new AtomicReference<>(null);
            final AtomicReference<String> defSection = new AtomicReference<>(null);
            final AtomicReference<String> defSource = new AtomicReference<>(null);
            final AtomicReference<String> defPrincipalId = new AtomicReference<>(null);
            final Map<String, Map<String, String>> defFieldDefaults = new HashMap<>();
            jsonSection(jsonElement, "defaults", JsonElement::isJsonObject, JsonElement::getAsJsonObject)
                    .ifPresent(defaults -> {
                        getPrimitive(defaults, "webPage", JsonElement::getAsString, defWebPage::set);
                        getPrimitive(defaults, "deskLevel", JsonElement::getAsString, defDeskLevel::set);
                        getPrimitive(defaults, "section", JsonElement::getAsString, defSection::set);
                        getPrimitive(defaults, "source", JsonElement::getAsString, defSource::set);
                        getPrimitive(defaults, "taxonomyId", JsonElement::getAsString, config::setTaxonomyId);
                        getPrimitive(defaults, "principalId", JsonElement::getAsString, defPrincipalId::set);
                        defFieldDefaults.putAll(readContentTypesDefaults(defaults));
                    });
            final String defaultPrincipalId = Optional.ofNullable(defPrincipalId.get())
                                                      .filter(StringUtils::notEmpty)
                                                      .orElse("98");
            final Map<String, Map<String, String>> defFieldMappings = readContentTypesMappings(jsonElement);
            if (StringUtils.notEmpty(loader.getMailUri()) && config.getMailUris().size() > 0) {
                final MailRouteConfig mainRouteConfig = config.getMailUris().get(0);
                mainRouteConfig.setWebPage(defWebPage.get());
                mainRouteConfig.setDeskLevel(defDeskLevel.get());
                mainRouteConfig.setSection(defSection.get());
                mainRouteConfig.setSource(defSource.get());
                mainRouteConfig.setTaxonomyId(loader.getTaxonomyId());
                mainRouteConfig.setFieldsDefaults(defFieldDefaults);
                mainRouteConfig.setFieldsMappings(defFieldMappings);
                mainRouteConfig.setPrincipalId(defaultPrincipalId);
            }
            jsonSection(jsonElement, "mailUri", JsonElement::isJsonArray, JsonElement::getAsJsonArray)
                    .ifPresent(mailUri -> {
                        for (int idx = 0; idx < mailUri.size(); idx++) {
                            final JsonElement mailElement = mailUri.get(idx);
                            if (mailElement.isJsonObject()) {
                                final JsonObject mailJson = mailElement.getAsJsonObject();
                                final MailRouteConfig routeConfig = loader.createRouteConfig(mailJson);
                                if (routeConfig != null) {
                                    routeConfig.setWebPage(defWebPage.get());
                                    routeConfig.setDeskLevel(defDeskLevel.get());
                                    routeConfig.setSection(defSection.get());
                                    routeConfig.setSource(defSource.get());
                                    routeConfig.setTaxonomyId(config.getTaxonomyId());
                                    routeConfig.setPrincipalId(defaultPrincipalId);
                                    getPrimitive(mailJson, "webPage", JsonElement::getAsString, routeConfig::setWebPage);
                                    getPrimitive(mailJson, "deskLevel", JsonElement::getAsString, routeConfig::setDeskLevel);
                                    getPrimitive(mailJson, "section", JsonElement::getAsString, routeConfig::setSection);
                                    getPrimitive(mailJson, "source", JsonElement::getAsString, routeConfig::setSource);
                                    getPrimitive(mailJson, "taxonomyId", JsonElement::getAsString, routeConfig::setTaxonomyId);
                                    getPrimitive(mailJson, "principalId", JsonElement::getAsString, routeConfig::setPrincipalId);
                                    final Map<String, Map<String, String>> fieldDefaults = new HashMap<>();
                                    fieldDefaults.putAll(defFieldDefaults);
                                    fieldDefaults.putAll(readContentTypesDefaults(mailJson));
                                    routeConfig.setFieldsDefaults(fieldDefaults);
                                    routeConfig.setFieldsMappings(defFieldMappings);
                                    config.getMailUris().add(routeConfig);
                                }
                            }
                        }
                    });
        }
        return config;
    }

    private static Map<String, Map<String, String>> readContentTypesDefaults(final JsonElement jsonElement) {
        final Map<String, Map<String, String>> defaults = new HashMap<>();
        jsonSection(jsonElement, "contentTypes", JsonElement::isJsonObject, JsonElement::getAsJsonObject)
                .ifPresent(contentTypes -> {
                    for (final Map.Entry<String, JsonElement> entry : contentTypes.entrySet()) {
                        final String contentType = entry.getKey();
                        if (entry.getValue().isJsonObject()) {
                            defaults.putIfAbsent(contentType, new HashMap<>());
                            final Map<String, String> map = defaults.get(contentType);
                            final JsonObject json = entry.getValue().getAsJsonObject();
                            for (final Map.Entry<String, JsonElement> contentEntry : json.entrySet()) {
                                final String fieldName = contentEntry.getKey();
                                getPrimitive(json, fieldName, JsonElement::getAsString, v ->
                                    map.put(fieldName, v)
                                );
                            }
                        }
                    }
                });
        return defaults;
    }

    private static Map<String, Map<String, String>> readContentTypesMappings(final JsonElement jsonElement) {
        final Map<String, Map<String, String>> mappings = new HashMap<>();
        jsonSection(jsonElement, "mappings", JsonElement::isJsonObject, JsonElement::getAsJsonObject)
                .ifPresent(contentTypes -> {
                    for (final Map.Entry<String, JsonElement> entry : contentTypes.entrySet()) {
                        final String contentType = entry.getKey();
                        if (entry.getValue().isJsonObject()) {
                            mappings.putIfAbsent(contentType, new HashMap<>());
                            final Map<String, String> map = mappings.get(contentType);
                            final JsonObject json = entry.getValue().getAsJsonObject();
                            for (final Map.Entry<String, JsonElement> contentEntry : json.entrySet()) {
                                final String fieldName = contentEntry.getKey();
                                getPrimitive(json, fieldName, JsonElement::getAsString, v ->
                                        map.put(fieldName, v)
                                );
                            }
                        }
                    }
                });
        return mappings;
    }

    private static <T> Optional<T> jsonSection(final JsonElement jsonElement,
                                               final String name,
                                               final Predicate<JsonElement> test,
                                               final Function<JsonElement, T> map) {
        if (jsonElement != null && jsonElement.isJsonObject()) {
            final JsonObject json = jsonElement.getAsJsonObject();
            if (json.has(name)) {
                final JsonElement sectionElement = json.get(name);
                if (test.test(sectionElement)) {
                    return Optional.of(map.apply(sectionElement));
                }
            }
        }
        return Optional.empty();
    }

    public static MailImporterConfig createConfig(final BaselinePolicy baselinePolicy) {
        return createConfig(new MailImporterConfigLoader(baselinePolicy));
    }

    public static MailImporterConfig createConfig(final PolicyCMServer cmServer) throws CMException {
        final BaselinePolicy policy = (BaselinePolicy) cmServer.getPolicy(new ExternalContentId(MailImporterConfigLoader.CONFIG_EXT_ID));
        return createConfig(policy);
    }
}
