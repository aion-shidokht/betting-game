require('./config.js');

let abiObj = web3.avm.contract.Interface(abi);
web3.avm.contract.initBinding(contractAddress, abiObj, ownerPrivateKey, web3);

const account = web3.eth.accounts.privateKeyToAccount(ownerPrivateKey);

async function transfer(value) {
    const Tx = {
        from: account.address,
        to: contractAddress,
        gasPrice: 10000000000,
        gas: 2000000,
        value: value,
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

let value = process.argv[2];
transfer(value);
