/*
 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.atlasdb.cli.command.timestamp;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.LoggerFactory;

import com.palantir.atlasdb.cleaner.KeyValueServicePuncherStore;
import com.palantir.atlasdb.cli.output.OutputPrinter;
import com.palantir.atlasdb.services.AtlasDbServices;

import io.airlift.airline.Command;
import io.airlift.airline.Option;
import io.airlift.airline.OptionType;

@Command(name = "fetch", description = "Fetches a timestamp. By default"
        + " this will be a fresh timestamp unless otherwise specified.")
public class FetchTimestamp extends AbstractTimestampCommand {
    private static final OutputPrinter printer = new OutputPrinter(LoggerFactory.getLogger(FetchTimestamp.class));

    @Option(name = {"-i", "--immutable"},
            type = OptionType.COMMAND,
            description = "Get the current immutable timestamp, instead of a fresh one.")
    private boolean immutable;

    @Option(name = {"-d", "--date-time"},
            type = OptionType.COMMAND,
            description = "Return the earliest approximate wall clock datetime at which the chosen timestamp"
                    + " could have been used in a transaction.")
    private boolean dateTime;

    private static final String IMMUTABLE_STRING = "Immutable";
    private static final String FRESH_STRING = "Fresh";

    @Override
    public boolean isOnlineRunSupported() {
        return true;
    }

    @Override
    protected boolean requireTimestamp() {
        return false;
    }

    @Override
    protected int executeTimestampCommand(AtlasDbServices services) {
        printer.warn(
                "This CLI has been deprecated. Please use the timestamp/fresh-timestamp endpoint instead.");

        String name;
        if (immutable) {
            timestamp = services.getTransactionManager().getImmutableTimestamp();
            name = IMMUTABLE_STRING;
        } else {
            timestamp = services.getTimestampService().getFreshTimestamp();
            name = FRESH_STRING;
        }
        printer.info("The {} timestamp is: {}", name, timestamp);
        writeTimestampToFileIfSpecified();

        if (dateTime) {
            long timeMillis = KeyValueServicePuncherStore.getMillisForTimestamp(
                    services.getKeyValueService(), timestamp);
            DateTime dt = new DateTime(timeMillis);
            String stringTime = ISODateTimeFormat.dateTime().print(dt);
            printer.info("Wall clock datetime of {} timestamp is: {}", name, stringTime);
        }

        printer.info("Timestamp command completed succesfully.");
        return 0;
    }
}
