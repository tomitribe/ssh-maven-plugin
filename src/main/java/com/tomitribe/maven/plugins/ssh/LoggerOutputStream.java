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
import java.util.function.Consumer;

import static java.util.Optional.of;

public class LoggerOutputStream extends OutputStream {
    private static final int BUFFER_SIZE = 1024;

    private byte[] buffer;
    private int count;
    private int bufferLen;

    private final Consumer<String> log;

    public LoggerOutputStream(final Consumer<String> log) {
        this.log = log;
        this.bufferLen = BUFFER_SIZE;
        this.buffer = new byte[bufferLen];
        this.count = 0;
    }

    @Override
    public void write(final int b) throws IOException {
        if (b == 0 || b == '\n') {
            flush();
            return;
        }

        if (count == bufferLen) {
            final byte[] newBuf = new byte[bufferLen + BUFFER_SIZE];
            System.arraycopy(buffer, 0, newBuf, 0, bufferLen);
            buffer = newBuf;
            bufferLen = newBuf.length;
        }

        buffer[count] = (byte) b;
        count++;
    }



    @Override
    public void flush() {
        of(count).filter(c -> c > 0).ifPresent(c -> {
            log.accept(new String(buffer, 0, c));
            count = 0;
        });
    }

    @Override
    public void close() {
        flush();
    }
}

