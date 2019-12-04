require('./config.js');

async function contractCall(playerAddress) {
    let abiObj = web3.avm.contract.Interface(abi);
    web3.avm.contract.initBinding(contractAddress, abiObj, ownerPrivateKey, web3);

    let res = await web3.avm.contract.readOnly.getScore(playerAddress);
    console.log(res)
}

contractCall(process.argv[2]);
