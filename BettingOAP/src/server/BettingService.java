package server;

import main.SignedTransactionBuilder;
import state.UserState;
import util.Helper;
import util.QueuePopulator;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/state")
public class BettingService {

    @Inject
    UserState userState;

    @Inject
    QueuePopulator queuePopulator;

    @GET
    @Path("/allStatements")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllStatements() {
        return Response.ok()
                .entity(userState.getStatements())
                .header("Access-Control-Allow-Origin","*")
                .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                .allow("OPTIONS")
                .build();
    }

    @GET
    @Path("/allPlayers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllPlayers() {
        return Response.ok()
                .entity(userState.getPlayers())
                .header("Access-Control-Allow-Origin","*")
                .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                .allow("OPTIONS")
                .build();
    }

    @GET
    @Path("/allAnswers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllAnswers() {
        return Response.ok()
                .entity(userState.getAnswers())
                .header("Access-Control-Allow-Origin","*")
                .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                .allow("OPTIONS")
                .build();
    }

    @GET
    @Path("/allVotes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVotes() {
        return Response.ok()
                .entity(userState.getVotes())
                .header("Access-Control-Allow-Origin","*")
                .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                .allow("OPTIONS")
                .build();
    }

    @GET
    @Path("/answer")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAnswerId(@QueryParam("eventId") final Integer eventId) {
        return Response.ok()
                .entity(userState.getAnswers().get(eventId))
                .header("Access-Control-Allow-Origin","*")
                .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                .allow("OPTIONS")
                .build();
    }

    @GET
    @Path("/gameStatus")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGameStatus() {
        return Response.ok()
                .entity(userState.getGameStatus())
                .header("Access-Control-Allow-Origin","*")
                .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                .allow("OPTIONS")
                .build();
    }

    @GET
    @Path("/getNonce")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNonce(@QueryParam("address") final String address) {
        return Response.ok()
                .entity(userState.getNonce(new org.aion.harness.kernel.Address(Helper.hexStringToBytes(address))))
                .header("Access-Control-Allow-Origin","*")
                .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                .allow("OPTIONS")
                .build();
    }

    @GET
    @Path("/getTransactions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTransactions(@QueryParam("address") final String address) {
        return Response.ok()
                .entity(userState.getTransactions(address))
                .header("Access-Control-Allow-Origin","*")
                .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                .allow("OPTIONS")
                .build();
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
