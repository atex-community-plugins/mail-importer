package com.atex.plugins.mailimporter.util;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import com.polopoly.common.lang.StringUtil;

/**
 * MailImporterServiceLoaderUtil
 *
 * @author mnova
 */
public abstract class MailImporterServiceLoaderUtil {

    private static final Logger LOGGER = Logger.getLogger(MailImporterServiceLoaderUtil.class.getName());

    public static <T> T loadService(final Class<T> klass,
                                    final Class<?> defaultKlass) {
        LOGGER.info("Loading " + klass.getName() + " implementations");
        final AtomicReference<T> defaultService = new AtomicReference<>();
        final ServiceLoader<T> loader = ServiceLoader.load(klass);
        for (final T service : loader) {
            if (StringUtil.equals(service.getClass().getName(), defaultKlass.getName())) {
                defaultService.set(service);
            } else {
                defaultService.set(service);
                break;
            }
        }
        final T service = defaultService.get();
        if (service != null) {
            LOGGER.info("Created " + service.getClass().getName());
        } else {
            LOGGER.severe("No services has been found for " + klass.getName());
        }
        return service;
    }

}
