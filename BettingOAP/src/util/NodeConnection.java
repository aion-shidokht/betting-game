package util;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import types.Address;
import org.aion.harness.main.RPC;
import org.aion.harness.main.tools.*;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.main.types.TransactionReceipt;
import org.aion.harness.result.RpcResult;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class NodeConnection {

    private final RPC rpc;
    private final Address contractAddress;

    public NodeConnection(RPC rpc, Address contractAddress) {
        this.rpc = rpc;
        this.contractAddress = contractAddress;
    }

    public RpcResult<Long> blockNumber() throws InterruptedException {
        return rpc.blockNumber();
    }

    public RpcResult<ReceiptHash> sendSignedTransaction(byte[] signedTransactionBytes) throws InterruptedException {
//        return rpc.sendSignedTransaction(transaction);
        String params = Helper.bytesToHexString(signedTransactionBytes);
        String payload = RpcPayload.generatePayload(RpcMethod.SEND_RAW_TRANSACTION, params);
//            InternalRpcResult internalResult = this.rpc.call(payload, false);
        //todo replace later
        RpcCaller rpcCaller = new RpcCaller("127.0.0.1", "8545");
        InternalRpcResult internalResult = rpcCaller.call(payload, false);
        if (internalResult.success) {
            JsonStringParser outputParser = new JsonStringParser(internalResult.output);
            String result = outputParser.attributeToString("result");
            if (result == null) {
                return RpcResult.unsuccessful("No receipt hash was returned, transaction was likely rejected.");
            } else {
                return RpcResult.successful(new ReceiptHash(Helper.hexStringToBytes(result)), internalResult.getTimeOfCall(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
            }
        } else {
            return RpcResult.unsuccessful(internalResult.error);
        }

    }

    public RpcResult<TransactionReceipt> getTransactionReceipt(ReceiptHash receiptHash) throws InterruptedException {
        return rpc.getTransactionReceipt(receiptHash);
    }

    public List<Log> getLogs(BigInteger fromBlock, String toBlock, Set<byte[]> filterTopics) throws InterruptedException {
        String payload = getLogsPayload(fromBlock, toBlock, filterTopics);
        //todo replace with the node_test_harness equivalent
        RpcCaller rpcCaller = new RpcCaller("127.0.0.1", "8545");
        InternalRpcResult internalResult = rpcCaller.call(payload, false);
        ArrayList<Log> logArray = new ArrayList<>();

        if (internalResult.success) {
            JsonStringParser outputParser = new JsonStringParser(internalResult.output);
            String result = outputParser.attributeToString("result");

            if (result != null) {
                JsonArray retrievedLogs = (JsonArray) new JsonParser().parse(result);
                for (int i = 0; i < retrievedLogs.size(); i++) {
                    Log log = new LogBuilder().buildFromJsonString(retrievedLogs.get(i).toString());
                    if (contractAddress.equals(log.address)) {
                        logArray.add(log);
                    }
                }
            }
        }
        return logArray;
    }

    public RpcResult<BigInteger> getNonce(org.aion.harness.kernel.Address address) throws InterruptedException {
        return rpc.getNonce(address);
    }

    private String getLogsPayload(BigInteger fromBlock, String toBlock, Set<byte[]> topics) {
        String payloadStart = "{\"jsonrpc\":\"2.0\",\"method\":\"";
        String methodName = "eth_getLogs";
        String paramsStart = "\",\"params\":[{";
        paramsStart += "\"topics\":[[";
        int i = 1;
        for (byte[] topic : topics) {
            paramsStart += "\0x" + Helper.bytesToHexString(truncatePadTopic(topic));
            if (i < topics.size()) {
                i++;
                paramsStart += "\", ";
            }
        }
        paramsStart += "\"]], ";

        String payloadEnd = "}],\"id\":1}";

        return payloadStart +
                methodName +
                paramsStart +
                "\"fromBlock\": \"" +
                fromBlock +
                "\", \"toBlock\": \"" +
                toBlock + "\"" +
                payloadEnd;
    }

    private static byte[] truncatePadTopic(byte[] topic) {
        int TOPIC_SIZE = 32;
        byte[] result = new byte[TOPIC_SIZE];
        if (null == topic) {
            throw new NullPointerException();
        } else if (topic.length < TOPIC_SIZE) {
            // Too short:  zero-pad.
            System.arraycopy(topic, 0, result, 0, topic.length);
            for (int i = topic.length; i < TOPIC_SIZE; ++i) {
                result[i] = 0;
            }
        } else if (topic.length > TOPIC_SIZE) {
            // Too long:  truncate.
            System.arraycopy(topic, 0, result, 0, TOPIC_SIZE);
        } else {
            // Just the right size.
            System.arraycopy(topic, 0, result, 0, TOPIC_SIZE);
        }
        return result;
    }
}
