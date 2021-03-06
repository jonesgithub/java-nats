/*
 *  Copyright (c) 2015-2016 Apcera Inc. All rights reserved. This program and the accompanying
 *  materials are made available under the terms of the MIT License (MIT) which accompanies this
 *  distribution, and is available at http://opensource.org/licenses/MIT
 */

package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Requestor {
    String url = Nats.DEFAULT_URL;
    String subject;
    String payload;

    static final String usageString =
            "\nUsage: java Requestor [-s <server>] <subject> <message>\n\nOptions:\n"
                    + "    -s <url>        the NATS server URLs, separated by commas\n";

    public Requestor(String[] args) {
        parseArgs(args);
    }

    public static void usage() {
        System.err.println(usageString);
    }

    public void run() throws Exception {
        try (Connection nc = Nats.connect(url)) {
            Message msg = nc.request(subject, payload.getBytes(), 100, TimeUnit.MILLISECONDS);
            System.out.printf("Published [%s] : '%s'\n", subject, payload);
            if (msg == null) {
                Exception err = nc.getLastException();
                if (err != null) {
                    System.err.println("Error in request");
                    throw err;
                }
                throw new TimeoutException("Request timed out");
            }
            System.out.printf("Received [%s] : '%s'\n", msg.getSubject(),
                    msg.getData() == null ? "" : new String(msg.getData()));
        }
    }

    public void parseArgs(String[] args) {
        if (args == null || args.length < 2) {
            throw new IllegalArgumentException("must supply at least subject and msg");
        }

        List<String> argList = new ArrayList<String>(Arrays.asList(args));

        // The last arg should be subject and payload
        // get the payload and remove it from args
        payload = argList.remove(argList.size() - 1);

        // get the subject and remove it from args
        subject = argList.remove(argList.size() - 1);

        // Anything left is flags + args
        Iterator<String> it = argList.iterator();
        while (it.hasNext()) {
            String arg = it.next();
            switch (arg) {
                case "-s":
                case "--server":
                    if (!it.hasNext()) {
                        throw new IllegalArgumentException(arg + " requires an argument");
                    }
                    it.remove();
                    url = it.next();
                    it.remove();
                    continue;
                default:
                    throw new IllegalArgumentException(
                            String.format("Unexpected token: '%s'", arg));
            }
        }
    }

    /**
     * Publishes a message to a subject.
     *
     * @param args the subject, message payload, and other arguments
     */
    public static void main(String[] args) throws Exception {
        try {
            new Requestor(args).run();
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            Requestor.usage();
            throw e;
        }
    }
}
