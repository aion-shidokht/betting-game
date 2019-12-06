import React, {useContext, useState} from "react";
import Button from "@material-ui/core/Button";
import Dialog from "@material-ui/core/Dialog";
import DialogActions from "@material-ui/core/DialogActions";
import DialogContent from "@material-ui/core/DialogContent";
import DialogContentText from "@material-ui/core/DialogContentText";
import DialogTitle from "@material-ui/core/DialogTitle";
import Paper from "@material-ui/core/Paper";
import TextField from "@material-ui/core/TextField";
import Draggable from "react-draggable";

import useStyles from "../../Style";
import Web3 from "aion-web3";
import {AppContext} from "../context/AppContext";
import myConfig from "../../config";

import {NAMELIST} from "../constant/Constants";
import FormControl from "@material-ui/core/FormControl";
import InputLabel from "@material-ui/core/InputLabel";
import Select from "@material-ui/core/Select";
import MenuItem from "@material-ui/core/MenuItem";

function PaperComponent(props) {
  return (
    <Draggable cancel={'[class*="MuiDialogContent-root"]'}>
      <Paper {...props} />
    </Draggable>
  );
}

export default function RevealBox({handleDialog, isOpen}) {
  const classes = useStyles();

  const [answer, setAnswer] = useState("");
  const [salt, setSalt] = useState("");

  const [selectedStatement, setSelectedStatement, timer, setTimer, messageData, setMessageData, handleMessageDialog] = useContext(AppContext);

  const handleSubmit = event => {
    event.preventDefault();
    // console.log({ answer, salt });
    sendRevealTransaction(answer, salt);
    handleDialog(false);
    setAnswer("");
    setSalt("");
  };

  window.onload = () => {
    if (window.aionweb3){
      console.log("✓ AIWA injected successfully");
    }
  };

  const sendRevealTransaction = async (answer, salt) => {
    if (window.aionweb3) {
      let web3 = new Web3(
          new Web3.providers.HttpProvider(window.aionweb3.currentProvider.host));

      let account = window.aionweb3.eth.accounts;
      console.log("detected account " + account);


      let statementId = selectedStatement.statementId;
      console.log("revealing answer for " + statementId + " " + answer);

      let answerBytes = toBytes(answer);
      let saltBytes = toBytes(salt);

      let data = web3.avm.contract.method('revealAnswer').inputs(['int', 'byte[]', 'byte[]'], [statementId, answerBytes, saltBytes]).encode();

      const tx = {
        from: account.address,
        to: myConfig.contractAddress,
        data: data,
        gasPrice: 10000000000,
        gas: 2000000,
        type: 0x1
      };
      try {
        let txHash = await window.aionweb3.sendTransaction(tx).then(function (txHash) {
          console.log('txHash:', txHash);
        });
      } catch (err) {
        console.log(err);
      }
    } else {
      console.log("Account not Detected - Please download AIWA to play this game");
    }
  };

  const handleError = error => {
    handleMessageDialog(true, error);
  };

  function toBytes(statement) {
    let bytes = [];
    for (let i = 0; i < statement.length; i++) {
      bytes.push(statement.charCodeAt(i));
    }
    return bytes;
  }

  return (
    <div>
      <Dialog
        open={isOpen}
        onClose={event => handleDialog(false)}
        PaperComponent={PaperComponent}
        aria-labelledby="draggable-dialog-title"
      >
        <DialogTitle style={{ cursor: "move" }} id="draggable-dialog-title">
          Reveal Answer
        </DialogTitle>

        <DialogContent>
          <DialogContentText>{selectedStatement.statementString}</DialogContentText>
          <form className={classes.container} noValidate autoComplete="off">
            <FormControl className={classes.formControl}>
              <InputLabel id="answer-label">Answer</InputLabel>
              <Select
                  required
                  labelId="answer-label"
                  id="answer"
                  value={answer}
                  onChange={e => setAnswer(e.target.value)}
              >
                {NAMELIST.map(name =>
                    <MenuItem key={name} value={name}>{name}</MenuItem>
                )}
              </Select>
            </FormControl>
            <TextField
              required
              id="standard-salt"
              label="Salt"
              onChange={e => setSalt(e.target.value)}
              className={classes.textFieldMedium}
              margin="normal"
            />
          </form>
        </DialogContent>
        <DialogActions>
          <Button autoFocus onClick={event => handleDialog(false)} style={{ background: '#E1DCCD' }}>
            Close
          </Button>
          <Button
            type="submit"
            variant="contained"
            onClick={handleSubmit}
            style={{ background: '#E1DCCD' }}
          >
            Reveal
          </Button>
        </DialogActions>
      </Dialog>
    </div>
  );
}
