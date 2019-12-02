package util;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.aion.harness.main.tools.*;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.main.types.TransactionReceipt;
import org.aion.harness.main.types.internal.TransactionReceiptBuilder;
import org.aion.harness.result.RpcResult;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.aion.harness.kernel.Address;
import org.apache.commons.codec.DecoderException;

public class NodeConnection {

    private final RpcCaller rpc;

    public NodeConnection(String ip, String port) {
        this.rpc = new RpcCaller(ip, port);
    }

    // taken from node-test-harness
    public RpcResult<Long> blockNumber() throws InterruptedException {
        String params = "";
        String payload = RpcPayload.generatePayload(RpcMethod.BLOCK_NUMBER, params);
        InternalRpcResult internalResult = this.rpc.call(payload, false);
        if (internalResult.success) {
            JsonStringParser outputParser = new JsonStringParser(internalResult.output);
            String result = outputParser.attributeToString("result");
            if (result == null) {
                throw new IllegalStateException(internalResult.output);
            } else {
                String rpcResult = (new JsonParser()).parse(internalResult.output).getAsJsonObject().get("result").getAsString();
                return RpcResult.successful(Long.parseLong(rpcResult, 10), internalResult.getTimeOfCall(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
            }
        } else {
            return RpcResult.unsuccessful(internalResult.error);
        }
    }

    public RpcResult<ReceiptHash> sendSignedTransaction(byte[] signedTransactionBytes) throws InterruptedException {
        String params = Helper.bytesToHexString(signedTransactionBytes);
        String payload = RpcPayload.generatePayload(RpcMethod.SEND_RAW_TRANSACTION, params);
        InternalRpcResult internalResult = rpc.call(payload, false);
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

    // taken from node-test-harness
    public RpcResult<TransactionReceipt> getTransactionReceipt(ReceiptHash receiptHash) throws InterruptedException {
        if (receiptHash == null) {
            throw new NullPointerException("Cannot get a receipt from a null receipt hash.");
        } else {
            String params = Helper.bytesToHexString(receiptHash.getHash());
            String payload = RpcPayload.generatePayload(RpcMethod.GET_TRANSACTION_RECEIPT, params);
            InternalRpcResult internalResult = this.rpc.call(payload, false);
            if (internalResult.success) {
                JsonStringParser outputParser = new JsonStringParser(internalResult.output);
                String result = outputParser.attributeToString("result");
                if (result == null) {
                    return RpcResult.unsuccessful("No transaction receipt was returned, the transaction may still be processing.");
                } else {
                    try {
                        TransactionReceipt receipt = (new TransactionReceiptBuilder()).buildFromJsonString(result);
                        return RpcResult.successful(receipt, internalResult.getTimeOfCall(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
                    } catch (DecoderException e) {
                        return RpcResult.unsuccessful(e.toString());
                    }
                }
            } else {
                return RpcResult.unsuccessful(internalResult.error);
            }
        }
    }

    public List<Log> getLogs(long fromBlock, String toBlock, Set<byte[]> filterTopics, Address contractAddress) throws InterruptedException {
        String payload = getLogsPayload(fromBlock, toBlock, filterTopics);
        InternalRpcResult internalResult = rpc.call(payload, false);
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

    // taken from node-test-harness
    public RpcResult<BigInteger> getNonce(Address address) throws InterruptedException {
        if (address == null) {
            throw new IllegalArgumentException("Cannot get nonce of a null address.");
        } else {
            String params = Helper.bytesToHexString(address.getAddressBytes());
            String payload = RpcPayload.generatePayload(RpcMethod.GET_NONCE, params);
            InternalRpcResult internalResult = this.rpc.call(payload, false);
            if (internalResult.success) {
                JsonStringParser outputParser = new JsonStringParser(internalResult.output);
                String result = outputParser.attributeToString("result");
                if (result == null) {
                    throw new IllegalStateException(internalResult.output);
                } else {
                    return RpcResult.successful(new BigInteger(result, 16), internalResult.getTimeOfCall(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
                }
            } else {
                return RpcResult.unsuccessful(internalResult.error);
            }
        }
    }

    private String getLogsPayload(long fromBlock, String toBlock, Set<byte[]> topics) {
        String payloadStart = "{\"jsonrpc\":\"2.0\",\"method\":\"";
        String methodName = "eth_getLogs";
        String paramsStart = "\",\"params\":[{";
        paramsStart += "\"topics\":[[";
        int i = 1;
        for (byte[] topic : topics) {
            paramsStart += "\"0x" + Helper.bytesToHexString(truncatePadTopic(topic));
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
