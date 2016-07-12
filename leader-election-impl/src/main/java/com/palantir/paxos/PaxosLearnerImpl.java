/**
 * Copyright 2015 Palantir Technologies
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
package com.palantir.paxos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaxosLearnerImpl implements PaxosLearner {

    private static final Logger logger = LoggerFactory.getLogger(PaxosLearnerImpl.class);

    /**
     * @param logDir string path for directory to place durable logs
     * @param type the type of the objects to be learned by the learner
     * @return a new learner
     */
    public static PaxosLearner newLearner(String logDir) {
        PaxosStateLogImpl<PaxosValue> log = new PaxosStateLogImpl<>(logDir);
        ConcurrentSkipListMap<PaxosInstanceId, PaxosValue> state = new ConcurrentSkipListMap<>();

        byte[] greatestValidValue = PaxosStateLogs.getGreatestValidLogEntry(log);
        if (greatestValidValue != null) {
            PaxosValue value = PaxosValue.BYTES_HYDRATOR.hydrateFromBytes(greatestValidValue);
            state.put(value.getRound(), value);
        }

        return new PaxosLearnerImpl(state, log);
    }

    final SortedMap<PaxosInstanceId, PaxosValue> state;
    final PaxosStateLog<PaxosValue> log;

    private PaxosLearnerImpl(SortedMap<PaxosInstanceId, PaxosValue> stateWithGreatestValueFromLog,
                             PaxosStateLog<PaxosValue> log) {
        this.state = stateWithGreatestValueFromLog;
        this.log = log;
    }

    @Override
    public void learn(PaxosValue val) {
        state.put(val.getRound(), val);
        log.writeRound(val.getRound().seq(), val);
    }

    @Override
    public PaxosValue getLearnedValue(PaxosInstanceId instance) {
        long seq = instance.seq();
        try {
            if (!state.containsKey(instance)) {
                byte[] bytes = log.readRound(seq);
                if (bytes != null) {
                    PaxosValue value = PaxosValue.BYTES_HYDRATOR.hydrateFromBytes(log.readRound(seq));
                    state.put(instance, value);
                }
            }
            return state.get(instance);
        } catch (IOException e) {
            logger.error("unable to get corrupt learned value", e);
            return null;
        }
    }

    @Override
    public Collection<PaxosValue> getLearnedValuesSince(PaxosInstanceId instance) {
        long seq = instance.seq();
        PaxosValue greatestLearnedValue = getGreatestLearnedValue();
        long greatestSeq = -1L;
        if (greatestLearnedValue != null) {
            greatestSeq = greatestLearnedValue.getRound().seq();
        }

        Collection<PaxosValue> values = new ArrayList<PaxosValue>();
        for (long i = seq; i <= greatestSeq; i++) {
            PaxosValue value;
            value = getLearnedValue(PaxosInstanceId.fromSeq(i));
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    @Override
    public PaxosValue getGreatestLearnedValue() {
        if (!state.isEmpty()) {
            return state.get(state.lastKey());
        }
        return null;
    }

    @Override
    public Collection<PaxosValue> getAllLearnedValues() {
        Collection<PaxosValue> values = new HashSet<>();
        values.addAll(state.values());
        long greatestSeq = log.getGreatestLogEntry();
        for(long i = 0; i < greatestSeq; i++) {
            values.add(getLearnedValue(PaxosInstanceId.fromSeq(i)));
        }
        return values;
    }
}