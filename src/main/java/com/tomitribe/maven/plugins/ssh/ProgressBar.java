/*
 * Tomitribe Confidential
 *
 * Copyright(c) Tomitribe Corporation. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.tomitribe.maven.plugins.ssh;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class ProgressBar implements Consumer<Double> {
    private final double factor;
    private final OutputStream logger;
    private final long start;

    private int current;

    public ProgressBar(final OutputStream logger, final String text) {
        this(logger, text, 50);
    }

    public ProgressBar(final OutputStream logger, final String text, final int width) {
        this.factor = 100. / width;
        this.logger = logger;
        try {
            logger.write((text + " [").getBytes());
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        start = System.currentTimeMillis();
    }

    @Override
    public void accept(final Double perCent) {
        int newCurrent = (int) (perCent / factor);
        if (newCurrent > current) {
            IntStream.range(0, newCurrent - current).forEach(i -> {
                try {
                    logger.write('=');
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            current = newCurrent;
        }
        if (perCent == 100) {
            try {
                logger.write(("] " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) + "s\n").getBytes());
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
