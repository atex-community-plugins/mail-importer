package com.atex.plugins.mailimporter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderWriterSpi;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MimeTypeUtils
 *
 * @author mnova
 */
public abstract class MimeTypeUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(MimeTypeUtils.class.getName());

    public static Optional<String> getMimeType(final InputStream inputStream) {
        final InputStream is;
        final Callable<Void> reset;
        if (inputStream.markSupported()) {
            is = inputStream;
            // mark the input stream so a call to reset will not
            // throw because of the invalid mark point.
            is.mark(16384);
            reset = () -> {
                is.reset();
                return null;
            };
        } else {
            is = new BufferedInputStream(inputStream);
            reset = () -> {
                is.close();
                return null;
            };
        }
        try (final ImageInputStream iis = ImageIO.createImageInputStream(is)) {
            final Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
            if (!iter.hasNext()) {
                return Optional.empty();
            }
            final ImageReader reader = iter.next();
            return Optional.ofNullable(reader)
                           .map(ImageReader::getOriginatingProvider)
                           .map(ImageReaderWriterSpi::getMIMETypes)
                           .filter(v -> v.length > 0)
                           .map(v -> v[0]);
        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
        } finally {
            try {
                reset.call();
            } catch (Exception e) {
                LOGGER.warn("while resetting stream of class {}: "  + e.getMessage(), is.getClass().getName(), e);
            }
        }
        return Optional.empty();
    }

}
