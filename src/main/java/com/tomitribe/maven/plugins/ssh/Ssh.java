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

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.maven.plugin.logging.Log;
import org.tomitribe.util.IO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

import static java.util.Optional.ofNullable;

public final class Ssh implements AutoCloseable {
    private final Session session;
    private final Log log;
    private final String connection;

    public Ssh(final Log log, final File key, final String pwd, final String connection) {
        this.log = log;
        final JSch jsch = new JSch();
        if (!key.isFile()) {
            throw new IllegalStateException("No key file provided, can't scp " + key.getName() + ".");
        }
        try {
            jsch.addIdentity(key.getAbsolutePath(), pwd);
        } catch (final JSchException e) {
            throw new IllegalStateException(e);
        }

        final int at = connection.indexOf('@');
        final int portSep = connection.indexOf(':');
        final String user = connection.substring(0, at);
        final String host = connection.substring(at + 1, portSep < 0 ? connection.length() : portSep);
        final int port = portSep > 0 ? Integer.parseInt(connection.substring(portSep + 1, connection.length())) : 22;

        try {
            session = jsch.getSession(user, host, port);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
        } catch (final JSchException e) {
            throw new IllegalStateException(e);
        }

        this.connection = connection;
    }

    public String getConnection() {
        return connection;
    }

    public Ssh exec(final String command) {
        Channel channel = null;
        try {
            channel = redirectStreams(openExecChannel(command));
            channel.connect();
            try {
                ofNullable(IO.slurp(channel.getInputStream()))
                    .filter(s -> !s.isEmpty())
                    .ifPresent(log::info);
            } catch (final IOException e) {
                // no-op
            }
            return this;
        } catch (final JSchException je) {
            throw new IllegalStateException(je);
        } finally {
            ofNullable(channel).ifPresent(Channel::disconnect);
        }
    }

    public Ssh scp(final File file, final String target, final Consumer<Double> progressTracker) {
        final String cmd = "scp -t " + target;
        ChannelExec channel = null;
        try {
            channel = openExecChannel(cmd);

            final OutputStream out = channel.getOutputStream();
            final InputStream in = channel.getInputStream();
            channel.connect();

            waitForAck(in);

            final long filesize = file.length();
            final String command = "C0644 " + filesize + " " + file.getName() + "\n";
            out.write(command.getBytes());
            out.flush();

            waitForAck(in);

            final byte[] buf = new byte[1024];
            long totalLength = 0;

            try (final FileInputStream fis = new FileInputStream(file)) {
                while (true) {
                    int len = fis.read(buf, 0, buf.length);
                    if (len <= 0) {
                        break;
                    }
                    out.write(buf, 0, len);
                    totalLength += len;

                    if (progressTracker != null) {
                        progressTracker.accept(totalLength * 100. / filesize);
                    }
                }
                out.flush();
                sendAck(out);
                waitForAck(in);
            }
            return this;
        } catch (final JSchException | IOException je) {
            throw new IllegalStateException(je);
        } finally {
            ofNullable(channel).ifPresent(Channel::disconnect);
        }
    }

    private ChannelExec openExecChannel(final String command) throws JSchException {
        final ChannelExec channelExec = ChannelExec.class.cast(session.openChannel("exec"));
        channelExec.setCommand(command);
        return channelExec;
    }

    private ChannelExec redirectStreams(final ChannelExec channelExec) {
        channelExec.setOutputStream(new LoggerOutputStream(log::info), true);
        channelExec.setErrStream(new LoggerOutputStream(log::error), true);
        // channel.setInputStream(environment.getInput(), true); // would leak threads and prevent proper shutdown
        return channelExec;
    }

    private static void sendAck(final OutputStream out) throws IOException {
        out.write(new byte[]{0});
        out.flush();
    }

    private static void waitForAck(final InputStream in) throws IOException {
        switch (in.read()) {
            case -1:
                throw new IllegalStateException("Server didnt respond.");
            case 0:
                return;
            default:
                final StringBuilder sb = new StringBuilder();

                int c = in.read();
                while (c > 0 && c != '\n') {
                    sb.append((char) c);
                    c = in.read();
                }
                throw new IllegalStateException("SCP error: " + sb.toString());
        }
    }

    @Override
    public void close() {
        try {
            session.disconnect();
        } catch (final RuntimeException re) {
            // no-op
        }
    }
}
