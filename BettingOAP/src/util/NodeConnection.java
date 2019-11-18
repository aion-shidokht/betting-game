package util;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.aion.avm.core.util.LogSizeUtils;
import org.aion.harness.kernel.Address;
import org.aion.harness.kernel.SignedTransaction;
import org.aion.harness.main.RPC;
import org.aion.harness.main.tools.InternalRpcResult;
import org.aion.harness.main.tools.JsonStringParser;
import org.aion.harness.main.tools.RpcCaller;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.main.types.TransactionReceipt;
import org.aion.harness.result.RpcResult;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NodeConnection {

    private RPC rpc;
    private Address contractAddress;

    public NodeConnection(RPC rpc, Address contractAddress) {
        this.rpc = rpc;
        this.contractAddress = contractAddress;
    }

    public RpcResult<Long> blockNumber() throws InterruptedException {
        return rpc.blockNumber();
    }

    public RpcResult<ReceiptHash> sendSignedTransaction(SignedTransaction transaction) throws InterruptedException {
        return rpc.sendSignedTransaction(transaction);
    }

    public RpcResult<TransactionReceipt> getTransactionReceipt(ReceiptHash receiptHash) throws InterruptedException {
        return rpc.getTransactionReceipt(receiptHash);
    }

    public List<Log> getLogs(BigInteger fromBlock, String toBlock, Set<byte[]> filterTopics) throws InterruptedException, DecoderException {
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

    private String getLogsPayload(BigInteger fromBlock, String toBlock, Set<byte[]> topics) {
        String payloadStart = "{\"jsonrpc\":\"2.0\",\"method\":\"";
        String methodName = "eth_getLogs";
        String paramsStart = "\",\"params\":[{";
        if (null != topics) {
            paramsStart += "\"topics\":[";
            int i = 1;
            for (byte[] topic : topics) {
                paramsStart += "\"0x" + Hex.encodeHexString(LogSizeUtils.truncatePadTopic(topic));
                if (i < topics.size()) {
                    i++;
                    paramsStart += "\", ";
                }
            }
            paramsStart += "], ";
        }
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
}
