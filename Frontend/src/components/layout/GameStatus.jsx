import React, {useContext, useEffect, useState} from "react";
import TextField from "@material-ui/core/TextField";
import {AppContext} from "../context/AppContext";
import axios from "axios";
import myConfig from "../../config";
import List from "@material-ui/core/List";
import ListItem from "@material-ui/core/ListItem";
import ListItemText from "@material-ui/core/ListItemText";
import useStyles from "../../Style";
import Typography from "@material-ui/core/Typography";
import Container from "@material-ui/core/Container";

function GameStatus() {

  const classes = useStyles();
  const [selectedStatement, setSelectedStatement, timer, setTimer, messageData, setMessageData, handleMessageDialog] = useContext(AppContext);

  const [state, setState] = useState({
    prize: "",
    stopped: "",
    stoppedAge: "",
    stoppedTransactionHash: "",
    winnerAge: "",
    winners: [],
    winnersTransactionHash: ""
  });

  async function getGameStatus() {
    await axios(myConfig.hostname + '/bettingOAP/state/gameStatus',
      {
        headers: {"Content-Type": "application/json"}
      }
    ).then(response => {
      const gameStatus = response.data;
      setState(gameStatus);
      // console.log(gameStatus);
      console.log("Received game status")
    }).catch(error => {
      handleError(error.message)
    });
    clearInterval(timer);
    setTimer(setTimeout(getGameStatus, myConfig.pollingTime))
  }

  useEffect(() => {
    getGameStatus();
    return () => {
      clearInterval(timer)
    }
  }, []);

  const handleError = error => {
    handleMessageDialog(true, error);
  };

  return (
    <div>
      <form className={classes.container} noValidate autoComplete="off">
        <div component="div">
          <div>
            <TextField
                label="Stopped"
                InputProps={{
                  readOnly: true
                }}
                id="stopped"
                value={state.stopped ? "Yes" : "No"}
                className={classes.textFieldShort}
                margin="normal"
            />
          </div>

          {state.stopped && <div>
            <TextField
                label="Stopped Depth"
                InputProps={{
                  readOnly: true
                }}
                id="stoppedAge"
                value={state.stoppedAge}
                className={classes.textFieldLong}
                margin="normal"
            />
          </div>}

          {state.stopped && <div>
            <TextField
                label="Stopped Transaction Hash"
                InputProps={{
                  readOnly: true
                }}
                id="stoppedTransactionHash"
                value={state.stoppedTransactionHash}
                className={classes.textFieldLong}
                margin="normal"
            />
          </div>}
        </div>

        <div>
          <div>
          <TextField
              label="Prize (Aion)"
              InputProps={{
                readOnly: true
              }}
              id="prize"
              value={state.prize}
              className={classes.textFieldShort}
              margin="normal"
          />
          </div>
          {state.winners && state.winners.length > 0 && <div>

             <TextField
            label="Winner Announcement Depth"
            InputProps={{
              readOnly: true
            }}
            id="winnerAge"
            value={state.winnerAge}
            className={classes.textFieldLong}
            margin="normal"
          />
          </div>}
        </div>

        {state.winners && state.winners.length > 0 && <div>
          <TextField
            label="Winner Transaction Hash"
            InputProps={{
              readOnly: true
            }}
            id="winnerTransactionHash"
            value={state.winnersTransactionHash}
            className={classes.textFieldLong}
            margin="normal"
          />
        </div> }
        {state.winners && state.winners.length > 0 && <Container style={{paddingLeft: 16, marginTop: 10}} component="div">
          <Typography variant="h7" className={classes.title}>
            Winner list
          </Typography>
          <List>
            {state.winners.map(winner =>
              <ListItem key={winner}>
                <ListItemText primary={winner}/>
              </ListItem>
            )}
          </List>
        </Container>}

      </form>
    </div>

  );
}

export default GameStatus
