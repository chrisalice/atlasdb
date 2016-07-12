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

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/acceptor")
public interface PaxosAcceptor {
    long NO_LOG_ENTRY = -1L;

    /**
     * The acceptor prepares for a given proposal by either promising not to accept future proposals
     * or rejecting the proposal.
     *
     * @return a paxos promise not to accept lower numbered proposals
     */
    @POST
    @Path("prepare")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    PaxosPromise prepare(PrepareRequest prepareRequest);

    /**
     * The acceptor decides whether to accept or reject a given proposal.
     *
     * @return a paxos message indicating if the proposal was accepted or rejected
     */
    @POST
    @Path("accept")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    BooleanPaxosResponse accept(AcceptRequest acceptRequest);

    /**
     * Gets the sequence number of the acceptor's most recent known round.
     *
     * @return the sequence number of the most recent round or {@value NO_LOG_ENTRY} if this
     *         acceptor has not prepared or accepted any rounds
     */
    @POST // This is marked as a POST because we cannot accept stale or cached results for this method.
    @Path("latest-sequence-prepared-or-accepted")
    @Produces(MediaType.APPLICATION_JSON)
    long getLatestSequencePreparedOrAccepted();
}