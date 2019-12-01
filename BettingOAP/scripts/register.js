require('./config.js');

const account = web3.eth.accounts.privateKeyToAccount(ownerPrivateKey);
let abiObj = web3.avm.contract.Interface(abi);

web3.avm.contract.initBinding(contractAddress, abiObj, ownerPrivateKey, web3);

async function contractCallSign(playerAddress) {
    let data = web3.avm.contract.method('register').inputs(['address'], [playerAddress]).encode();

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
    );

    const re = await web3.eth.sendSignedTransaction(signed.rawTransaction
    ).on('receipt', receipt => {
        console.log("Receipt received!\ntxHash =", receipt.transactionHash)
    });

    console.log(re);
    console.log(re.logs[0].topics)
}

async function contractCallSignRandom(txCount) {
    let toAddress = '0xa0' + randHex(62);
    let data = web3.avm.contract.method('register').inputs(['address'], [toAddress]).encode();

    const Tx = {
        from: account.address,
        to: contractAddress,
        nonce: web3.utils.toHex(txCount),
        data: data,
        gasPrice: 10000000000,
        gas: 2000000,
        type: 0x1
    };

    const signed = await web3.eth.accounts.signTransaction(
        Tx, account.privateKey
    );

    const re = await web3.eth.sendSignedTransaction(signed.rawTransaction
    ).on('receipt', receipt => {
        console.log("Receipt received!\ntxHash =", receipt.transactionHash)
    });

    console.log(re);
    console.log(re.logs[0].topics)
}

let randHex = function (len) {
    let maxlen = 8,
        min = Math.pow(16, Math.min(len, maxlen) - 1);
    let max = Math.pow(16, Math.min(len, maxlen)) - 1,
        n = Math.floor(Math.random() * (max - min + 1)) + min,
        r = n.toString(16);
    while (r.length < len) {
        r = r + randHex(len - maxlen);
    }
    return r;
};

const runRandom = (sleepTime, count, iteration) => {
    let txCount = count;
    let i = 0;
    let interval = setInterval(
        () => {
            contractCallSignRandom(txCount);
            txCount++;
            i++;
            if (i == iteration) {
                clearInterval(interval);
            }
        },
        sleepTime
    );
};

let mode = process.argv[2];
if (mode === "single") {
    let playerAddress = process.argv[3];
    contractCallSign(playerAddress)
} else if (mode === "random") {
    let sleepTime = process.argv[3];
    let iteration = process.argv[4];
    web3.eth.getTransactionCount(account.address).then(count => {
        runRandom(sleepTime, count, iteration);
    });
} else {
    console.log("options:");
    console.log("single [playerAddress]");
    console.log("random [sleepTime] [iteration]");
}

/*
async function contractCall() {
    let res = await web3.avm.contract.transaction.register(playerAddress);
    console.log("Counter: " + res);
    return res;
};
 */
