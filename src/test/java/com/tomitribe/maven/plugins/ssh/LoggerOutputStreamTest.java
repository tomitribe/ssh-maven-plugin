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

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class LoggerOutputStreamTest {
    @Test
    public void run() throws IOException {
        final StringBuilder builder = new StringBuilder();
        final Log log = new DefaultLog(null) {
            @Override
            public void info(final CharSequence charSequence) {
                builder.append(charSequence).append("\n");
            }
        };

        final LoggerOutputStream stream = new LoggerOutputStream(log::info);
        stream.write("test line 1\n".getBytes());
        stream.write("test line 2\nline 3 at the same time\nand sone o line4".getBytes());
        stream.write("but end is here\n".getBytes());
        stream.write("last is flushed".getBytes());
        stream.close();

        assertEquals(
            "test line 1\n" +
            "test line 2\n" +
            "line 3 at the same time\n" +
            "and sone o line4but end is here\n" +
            "last is flushed\n",
            builder.toString());
    }
}
