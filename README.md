# Office Guessing Game
This is a simple game designed to test the knowledge of employees about each other. It is designed to run on the Open Application Network and requires aiwa chrome extension. 

**BettingOAP**

This module contains the server logic for the application. It polls the blockchain for events and responds to user queries.
BettingOAP requires a config file path as an input. You can use the following commands to build and run this module:
```$xslt
mvn package
java -cp lib/node-test-harness.jar:target/BettingOAP-1.0-SNAPSHOT.jar Main config.properties 
```
**Frontend**

This module contains the front end logic. Frontend module reads the config from `src/config.js`. You can use the following commands to build and run this module:
```
npm install
npm run build
```
or
```
npm install
npm start
```

NOTE: Make sure to fill in both config files with the required information before running the packaged files. 

**Contract**

This module contains the AVM smart contract for the game. The address of deployed `bettingContract.jar` has to be used for frontend and server 
config files.

**Here is how to play:**
1) Register by providing your account address to the contract owner.
2) Submit statements about yourself or other employees.
3) Make guesses about other people's submitted statements.
4) When the game closes, reveal the answer to your statement.

Whoever has the most correct guesses when the game closes, wins!!!