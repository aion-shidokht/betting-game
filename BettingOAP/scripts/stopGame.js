require('./config.js');

const account = web3.eth.accounts.privateKeyToAccount(ownerPrivateKey);

let abiObj = web3.avm.contract.Interface(abi);
web3.avm.contract.initBinding(contractAddress, abiObj, ownerPrivateKey, web3);

async function contractCallSign() {

    let data = web3.avm.contract.method('stopGame').encode();

    const Tx = {
        from: account.address,
        to: contractAddress,
        data: data,
        gasPrice: 10000000000,
        gas: 2000000,
        type: 0x1
    };

    const signed = await web3.eth.accounts.signTransaction(
        Tx, account.privateKey
    ).then((res) => signedCall = res);

    const re = await web3.eth.sendSignedTransaction(signed.rawTransaction
    ).on('receipt', receipt => {
        console.log("Receipt received!\ntxHash =", receipt.transactionHash)
    });

    console.log(re);
    console.log(re.logs[0].topics)
}

contractCallSign();
