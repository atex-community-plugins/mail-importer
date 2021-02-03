package com.atex.plugins.mailimporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.atex.plugins.mailimporter.MailImporterConfig.MailRouteConfig;
import com.polopoly.application.Application;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.CmClient;
import com.polopoly.cm.client.CmClientBase;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.integration.IntegrationServerApplication;
import com.polopoly.util.StringUtil;

@Service
public class MailImporterEnabledChecker implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(MailImporterEnabledChecker.class);

    private static final String MAIL_IMPORTER_ROUTE = "mailImporterRoute";

    @Autowired
    private MailParseProcessor mailProcessor;

    @Autowired
    private MailPublishProcessor publishProcessor;

    @Autowired
    protected CamelContext camelContext;

    private Application application;

    public MailImporterEnabledChecker() {
    }

    public MailImporterEnabledChecker(final Application application) {
        this.application = application;
    }

    private static String lastConfiguration;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        final MailImporterConfig config = getConfig();

        final List<Route> existingRoutes = getExistingRoutes(camelContext);

        // if the configuration has been changed or if we should disable all the routes
        // then we will stop and remove them.

        if (config == null || !config.isEnabled() || !StringUtil.equals(lastConfiguration, getConfigurationDescription(config))) {
            for (final Route existingRoute : existingRoutes) {
                try {
                    if (existingRoute != null) {
                        // do not use getDescription since in camel 2.13.2 (used in polopoly 10.18.0)
                        // does not exists.
                        //LOG.info("Stopping route " + existingRoute.getDescription());
                        LOG.info("Stopping route " + existingRoute.getId());
                        camelContext.stopRoute(existingRoute.getId());
                        camelContext.removeRoute(existingRoute.getId());
                    }
                } catch (Exception e) {
                    LOG.error("failed to stop mail importer route " + existingRoute.getId(), e);
                }
            }
            existingRoutes.clear();
        }

        // if the configuration is enabled but we did not have any valid route
        // yet then we will proceed in creating them.

        if (config != null && config.isEnabled() && existingRoutes.size() == 0) {
            final Map<String, RouteBuilder> routes = new HashMap<>();
            final AtomicInteger idx = new AtomicInteger(0);
            for (final MailRouteConfig routeConfig : config.getMailUris()) {
                if (routeConfig.isEnabled()) {
                    final String routeId = createRouteId(idx.getAndIncrement());
                    final String description = hidePwd(routeConfig.getUri());

                    LOG.info("Creating route " + routeId + " for " + description);

                    final RouteBuilder routeBuilder = new RouteBuilder() {
                        @Override
                        public void configure() throws Exception {
                            from(routeConfig.getUri())
                                    .autoStartup(false)
                                    .id(routeId)
                                    // do not use description since in camel 2.13.2 (used in polopoly 10.18.0)
                                    // does not exists and we cannot use the setHeader too but we can fake it
                                    // with a anonymous processor.
                                    //.description(description)
                                    //.setHeader("X-ROUTE-CONFIG", () -> routeConfig)
                                    .process((m) -> m.getIn().setHeader("X-ROUTE-CONFIG", routeConfig))
                                    .process(mailProcessor)
                                    .process(publishProcessor);
                        }
                    };
                    routes.put(routeId, routeBuilder);
                }
            }
            if (routes.size() > 0) {
                try {
                    if (shouldBeStarted(camelContext.getStatus())) {
                        camelContext.start();
                    }
                    final List<String> ids = new ArrayList<>();
                    for (final Map.Entry<String, RouteBuilder> route : routes.entrySet()) {
                        try {
                            camelContext.addRoutes(route.getValue());
                        } catch (Exception e) {
                            LOG.error("cannot add route " + route.getKey() + ": " + e.getMessage(), e);
                            continue;
                        }
                        ids.add(route.getKey());
                    }
                    for (final String id : ids) {
                        try {
                            camelContext.startRoute(id);
                        } catch (Exception e) {
                            LOG.error("Failed to start route " + id + ": " + e.getMessage(), e);
                        }
                    }
                    lastConfiguration = getConfigurationDescription(config);
                } catch (Exception e) {
                    LOG.error("Failed to start camel context", e);
                }
            }
        }
    }

    private MailImporterConfig getConfig() {
        try {
            final Application app = getApplication();
            final CmClient cmClient = (CmClient) app.getApplicationComponent(CmClientBase.DEFAULT_COMPOUND_NAME);
            final PolicyCMServer cmServer = cmClient.getPolicyCMServer();
            return MailImporterConfigLoader.createConfig(cmServer);
        } catch (CMException e) {
            LOG.error("unable to load mail importer config", e);
            return null;
        }
    }

    private Application getApplication() {
        if (application == null) {
            application = IntegrationServerApplication.getPolopolyApplication();
        }
        return application;
    }

    String hidePwd(final String uri) {
        final int i = uri.toLowerCase().indexOf("password=");
        if (i < 0) {
            return uri;
        }
        final int e = uri.indexOf('&', i);
        if (e > 0) {
            return uri.substring(0, i) + "password=xxx" + uri.substring(e);
        } else {
            return uri.substring(0, i) + "password=xxx";
        }
    }

    private boolean shouldBeStarted(final ServiceStatus status) {
        if (status != null) {
            if (status.isStopping()) {
                return false;
            }
            if (status.isSuspended() || status.isSuspending()) {
                return false;
            }
            return status.isStartable() && !status.isStarting();
        }
        return true;
    }

    private String getConfigurationDescription(final MailImporterConfig config) {
        if (config == null) {
            return "null";
        }
        return Objects.toString(config);
    }

    public List<Route> getExistingRoutes(final CamelContext camelContext) {
        final List<Route> routes = new ArrayList<>();
        int idx = 0;
        while (idx < 100) {
            final Route route = camelContext.getRoute(createRouteId(idx++));

            // since when a route change we will remove all the routes
            // before re-adding the changed ones, routes will always be
            // ordered, so whenever we get a null route we know we have
            // reached the end of the routes.

            if (route == null) {
                break;
            }

            routes.add(route);
        }
        return routes;
    }

    String createRouteId(final int idx) {
        return String.format("%s%02d", MAIL_IMPORTER_ROUTE, idx);
    }
}
