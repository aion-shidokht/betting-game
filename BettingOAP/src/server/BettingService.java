package server;

import main.SignedTransactionBuilder;
import state.UserState;
import types.*;
import util.Helper;
import util.QueuePopulator;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/state")
public class BettingService {

    @Inject
    UserState userState;

    @Inject
    QueuePopulator queuePopulator;

    @GET
    @Path("/allStatements")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AggregatedStatement> getAllStatements() {
        return userState.getStatements();
    }

    @GET
    @Path("/allPlayers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, Player> getAllPlayers() {
        return userState.getPlayers();
    }

    @GET
    @Path("/allAnswers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, Answer> getAllAnswers() {
        return userState.getAnswers();
    }

    @GET
    @Path("/votes")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, Vote> getVoteIds(@QueryParam("eventIds") final List<Integer> eventIds) {
        Map<Integer, Vote> voteMap = userState.getVotes();
        return eventIds.stream()
                .filter(voteMap::containsKey)
                .collect(Collectors.toMap(Function.identity(), voteMap::get));
    }

    @GET
    @Path("/answer")
    @Produces(MediaType.APPLICATION_JSON)
    public Answer getAnswerId(@QueryParam("eventId") final Integer eventId) {
        return userState.getAnswers().get(eventId);
    }

    @GET
    @Path("/gameStatus")
    @Produces(MediaType.APPLICATION_JSON)
    public Game getGameStatus() {
        return userState.getGameStatus();
    }

    @GET
    @Path("/getNonce")
    @Produces(MediaType.APPLICATION_JSON)
    public BigInteger getGameStatus(@QueryParam("address") final String address) {
        return userState.getNonce(new org.aion.harness.kernel.Address(Helper.hexStringToBytes(address)));
    }

    @GET
    @Path("/getTransactions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TransactionDetails> getTransactions(@QueryParam("address") final String address) {
        return userState.getTransactions(address);
    }

    @POST
    @Path("/sendTransaction")
    public String send(String signedTransaction) {
        byte[] txBytes = Helper.hexStringToBytes(signedTransaction);
        byte[] transactionHash = SignedTransactionBuilder.getTransactionHashOfSignedTransaction(txBytes);
        queuePopulator.putRawTransaction(txBytes);
        return Helper.bytesToHexString(transactionHash);
    }

}
