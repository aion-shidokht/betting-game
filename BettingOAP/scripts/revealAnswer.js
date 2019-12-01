require('./config.js');

let answer = "Random answer";
let salt = "Random salt";

const account = web3.eth.accounts.privateKeyToAccount(ownerPrivateKey);

let abiObj = web3.avm.contract.Interface(abi);
web3.avm.contract.initBinding(contractAddress, abiObj, ownerPrivateKey, web3);

async function contractCallSign(statementId, answer, salt) {
    let answerBytes = toBytes(answer);
    let saltBytes = toBytes(salt);

    let data = web3.avm.contract.method('revealAnswer').inputs(['int', 'byte[]', 'byte[]'], [statementId, answerBytes, saltBytes]).encode();

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

async function contractCallSignRandom(txCount, startingIndex) {
    let statementId = startingIndex;
    let statementBytes = toBytes(answer);
    let saltBytes = toBytes(salt);

    let data = web3.avm.contract.method('revealAnswer').inputs(['int', 'byte[]', 'byte[]'], [statementId, statementBytes, saltBytes]).encode();

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
    ).then((res) => signedCall = res);

    const re = await web3.eth.sendSignedTransaction(signed.rawTransaction
    ).on('receipt', receipt => {
        console.log("Receipt received!\ntxHash =", receipt.transactionHash)
    });

    console.log(re);
    console.log(re.logs[0].topics)
}

function toBytes(statement) {
    let bytes = [];
    for (let i = 0; i < statement.length; i++) {
        bytes.push(statement.charCodeAt(i));
    }
    return bytes;
}

const runRandom = (sleepTime, count, startingIndex, endingIndex) => {
    let txCount = count;
    let index = startingIndex;
    let timerId = setInterval(
        () => {
            contractCallSignRandom(txCount, index);
            txCount++;
            index++;
            if (index == endingIndex) {
                clearInterval(timerId);
            }
        },
        sleepTime
    );
};

let mode = process.argv[2];
if (mode === "single") {
    let statementId = process.argv[3];
    let answer = process.argv[4];
    let salt = process.argv[5];
    contractCallSign(statementId, answer, salt)
} else if (mode === "random") {
    let sleepTime = process.argv[3];
    let startingIndex = process.argv[4];
    let endingIndex = process.argv[5];
    web3.eth.getTransactionCount(account.address).then(count => {
        runRandom(sleepTime, count, startingIndex, endingIndex);
    });
} else {
    console.log("options:");
    console.log("single [statementId] [answer] [salt]");
    console.log("random [sleepTime] [startingIndex] [endingIndex]");
}
