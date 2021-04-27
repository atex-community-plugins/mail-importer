package com.atex.plugins.mailimporter;

import java.util.List;

import com.atex.onecms.content.ContentId;
import com.atex.plugins.mailimporter.MailImporterConfig.MailRouteConfig;
import com.polopoly.application.Application;

/**
 * MailPublisher
 *
 * @author mnova
 */
public interface MailPublisher {

    void init(final Application application);

    List<ContentId> publish(final MailBean mail,
                            final MailRouteConfig routeConfig) throws Exception;

}
