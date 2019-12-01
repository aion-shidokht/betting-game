require('./config.js');
const blake = require('blakejs');

const account = web3.eth.accounts.privateKeyToAccount(ownerPrivateKey);

let abiObj = web3.avm.contract.Interface(abi);
web3.avm.contract.initBinding(contractAddress, abiObj, ownerPrivateKey, web3);

async function contractCallSign(statement, answer, salt) {
    let statementBytes = toBytes(statement);
    let res = answer.concat(salt);
    let answerHashBytes = hexToBytes(blake.blake2bHex(res, null, 32));
    let data = web3.avm.contract.method('submitStatement').inputs(['byte[]', "byte[]"], [statementBytes, answerHashBytes]).encode();

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

async function contractCallSignRandom(txCount) {
    let statement = "Random Question Submitted";
    let answer = "Random answer";
    let salt = "Random salt";
    // let statementBytes = toBytes(statement + txCount);
    // let res = (answer + txCount).concat(salt + txCount);
    let statementBytes = toBytes(statement);
    let res = (answer).concat(salt);
    let answerHashBytes = hexToBytes(blake.blake2bHex(res, null, 32));
    let data = web3.avm.contract.method('submitStatement').inputs(['byte[]', "byte[]"], [statementBytes, answerHashBytes]).encode();

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

function hexToBytes(hex) {
    let ret = new Uint8Array(hex.length / 2)
    for (let i = 0; i < ret.length; i++) {
        ret[i] = parseInt(hex.substring(i * 2, i * 2 + 2), 16)
    }
    return ret
}

function toBytes(statement) {
    let bytes = [];
    for (var i = 0; i < statement.length; i++) {
        bytes.push(statement.charCodeAt(i));
    }
    return bytes;
}

const runRandom = (sleepTime, count, iteration) => {
    let txCount = count;
    let i = 0;
    let timerId = setInterval(
        () => {
            contractCallSignRandom(txCount);
            txCount++;
            i++;
            if (i == iteration) {
                clearInterval(timerId);
            }
        },
        sleepTime
    );
};

let mode = process.argv[2];
if (mode === "single") {
    let statement = process.argv[3];
    let answer = process.argv[4];
    let salt = process.argv[5];
    contractCallSign(statement, answer, salt)
} else if (mode === "random") {
    let sleepTime = process.argv[3];
    let iteration = process.argv[4];
    web3.eth.getTransactionCount(account.address).then(count => {
        runRandom(sleepTime, count, iteration);
    });
} else {
    console.log("options:");
    console.log("single [statement] [answer] [salt]");
    console.log("random [sleepTime] [iteration]");
}
