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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@Mojo(name = "run")
public class SshMojo extends AbstractMojo {
    @Parameter(property = "ssh.connection", defaultValue = "ssh-plugin")
    private String sshKey; // in settings.xml

    @Parameter
    private List<String> connections;

    @Parameter
    private List<Command> commands;

    @Parameter(defaultValue = "${settings}", readonly = true)
    private Settings settings;

    @Parameter(property = "ssh.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping.");
            return;
        }

        final Server server = ofNullable(settings.getServer(sshKey)).orElseThrow(() -> new IllegalArgumentException("No connection " + sshKey));

        final Collection<Ssh> sshs = ofNullable(connections).orElse(emptyList()).stream()
            .map(c -> new Ssh(getLog(), new File(server.getPrivateKey()), server.getPassphrase(), c))
            .collect(toList());

        if (sshs.isEmpty()) {
            getLog().warn("No connection.");
            return;
        }
        ofNullable(commands).orElse(emptyList()).forEach(c -> sshs.forEach(ssh -> c.getType().execute(getLog(), ssh, c.getValue())));
    }
}
