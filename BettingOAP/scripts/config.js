const Web3 = require("aion-web3");
module.exports.provider = "http://localhost:8545";
global.web3 = new Web3(new Web3.providers.HttpProvider(this.provider));

// FILL WITH THE CONTRACT ADDRESS
global.contractAddress = "";
// FILL WITH THE CONTRACT OWNER PRIVATE KEY
global.ownerPrivateKey = "";
global.playerPrivateKey = "";
global.abi = `
1
BettingContract
Clinit: ()
public static void register(Address)
public static void vote(int, byte[])
public static void submitStatement(byte[], byte[])
public static void revealAnswer(int, byte[], byte[])
public static void payout()
public static void stopGame()
public static void sefDestruct()
public static int getScore(Address)
public static Address[] getWinnerArray()
`;