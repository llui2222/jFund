package com.xm.jfund.utils;

import com.xm.jfund.exposures.IMessageProcessor;
import jxmUtils.Functionals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class ConnectionUtils {

    private static final Logger sLogger = LoggerFactory.getLogger(ConnectionUtils.class);

    public static void connectProcessorsToServers(final List<IMessageProcessor> processors) {
        int attempt = 0;
        while (true) {
            ++attempt;
            sLogger.info("Attempt number {} to connect processors to servers.", attempt);

            final List<Boolean> connectFlags =
                processors.stream()
                    .map(IMessageProcessor::connect)
                    .collect(Functionals.toArrayList(processors.size()));
            final boolean allConnected = connectFlags.stream().allMatch(c -> c);

            if (allConnected) {
                sLogger.info("Connecting to all servers was successful.");
                return;
            }
            else {
                sLogger.warn("Attempt to connect to servers has failed.");

                // Disconnect from the ones that made it -- we will try again in the next iteration
                Functionals.app(connectFlags, processors, (connected, p) -> {
                    if (connected) {
                        p.disconnect();
                    }
                    else {
                        sLogger.info("Was unable to connect to server {}", p.getServerName());
                    }
                });

                final int delay =
                    (attempt > sSecondsOfDelayForRetryingConnection.length)
                        ? sSecondsOfDelayAfterQuickAttempts
                        : sSecondsOfDelayForRetryingConnection[attempt - 1];
                sLogger.info("Backing off for {} seconds.", delay);
                try {
                    Thread.sleep(delay * 1000);
                }
                catch (final InterruptedException ex) {
                    sLogger.error("Parent thread interrupted while backing off after server disconnect.", ex);
                }
            }
        }
    }

    private static final Class<ConnectionUtils> sc = ConnectionUtils.class;

    private static final int[] sSecondsOfDelayForRetryingConnection = {
        10, 10,
        20, 20,
        40, 40,
        60, 60
    };

    private static final int sSecondsOfDelayAfterQuickAttempts = 120;
}
