package util;

import org.aion.harness.kernel.SignedTransaction;
import org.aion.harness.main.RPC;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.main.types.TransactionLog;
import org.aion.harness.main.types.TransactionReceipt;
import org.aion.harness.result.RpcResult;

import java.util.List;

public class NodeConnection {

    private RPC rpc;

    public NodeConnection(RPC rpc) {
        this.rpc = rpc;
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

    public List<TransactionLog> getLogs(long fromBlock, long toBlock){
        // todo
        return null;
    }
}
