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

import org.apache.maven.plugin.logging.Log;

import java.io.File;

public enum Type {
    SCP {
        @Override
        public void execute(final Log log, final Ssh ssh, final String value) {
            final int idx = value.indexOf("->");
            final String from = value.substring(0, idx).trim();
            final String to = value.substring(idx + 2, value.length()).trim();
            ssh.scp(new File(from), to, new ProgressBar(new LoggerOutputStream(log::info), "Copying " + from + " to " + to + " on " + ssh.getConnection()));
        }
    },
    SHELL {
        @Override
        public void execute(final Log log, final Ssh ssh, final String value) {
            log.info("Executing "  + value + " on " + ssh.getConnection());
            ssh.exec(value);
        }
    };

    public abstract void execute(Log log, Ssh ssh, String value);
}
