package server;

import main.SignedTransactionBuilder;
import org.aion.util.conversions.Hex;
import state.ProjectedState;
import types.*;
import util.QueuePopulator;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/state")
public class BettingService {

    @Inject
    ProjectedState projectedState;

    @Inject
    QueuePopulator queuePopulator;

    @GET
    @Path("/allStatements")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, Statement> getAllStatements() {
        return projectedState.getStatements();
    }

    @GET
    @Path("/allPlayers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, Player> getAllPlayers() {
        return projectedState.getPlayers();
    }

    @GET
    @Path("/allAnswers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, Answer> getAllAnswers() {
        return projectedState.getAnswers();
    }

    @GET
    @Path("/votes")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, Vote> getVoteIds(@QueryParam("eventIds") final List<Integer> eventIds) {
        Map<Integer, Vote> voteMap = projectedState.getVotes();
        return eventIds.stream()
                .filter(voteMap::containsKey)
                .collect(Collectors.toMap(Function.identity(), voteMap::get));
    }

    @GET
    @Path("/answer")
    @Produces(MediaType.APPLICATION_JSON)
    public Answer getAnswerId(@QueryParam("eventId") final Integer eventId) {
        return projectedState.getAnswers().get(eventId);
    }

    @GET
    @Path("/gameStatus")
    @Produces(MediaType.APPLICATION_JSON)
    public Game getGameStatus() {
        return projectedState.getGameStatus();
    }

    @POST
    @Path("/sendTransaction")
    public String send(String signedTransaction) {
        byte[] txBytes = Hex.decode(signedTransaction);
        byte[] transactionHash = SignedTransactionBuilder.getTransactionHashOfSignedTransaction(txBytes);
        queuePopulator.putRawTransaction(txBytes);
        return Hex.toHexString(transactionHash);
    }

}
