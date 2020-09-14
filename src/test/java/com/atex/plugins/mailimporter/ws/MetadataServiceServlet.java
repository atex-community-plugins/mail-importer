package com.atex.plugins.mailimporter.ws;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Ignore;

import com.atex.plugins.baseline.ws.BaseServiceServlet;

/**
 * MetadataServiceServlet
 *
 * @author mnova
 */
@Ignore
public class MetadataServiceServlet extends BaseServiceServlet<MetadataServiceServlet> {

    public MetadataServiceServlet(final int status) {
        super(MetadataServiceServlet.class, status);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        this.serveResponse(resp);
    }
}
