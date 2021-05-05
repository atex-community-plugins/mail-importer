package com.atex.plugins.mailimporter;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.LoggerFactory;

/**
 * GZipper
 *
 * @author mnova
 */
public class GZipper {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(GZipper.class);

    public File zip(final File src) {
        try {
            final File gzFile = new File(src.getParentFile(), src.getName() + ".gz");
            final FileInputStream in = new FileInputStream(src);
            final GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(gzFile));

            try {
                final byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                closeQuietly(in);
                closeQuietly(out);
            }

            return gzFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File unzip(final File src) {
        final File outFile = new File(src.getParentFile(), src.getName().replace(".gz", ""));
        return unzip(src, outFile);
    }

    public File unzip(final File src,
                      final File dst) {
        try {
            final GZIPInputStream in = new GZIPInputStream(new FileInputStream(src));
            final FileOutputStream out = new FileOutputStream(dst);

            try {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                closeQuietly(in);
                closeQuietly(out);
            }

            return dst;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeQuietly(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

}
