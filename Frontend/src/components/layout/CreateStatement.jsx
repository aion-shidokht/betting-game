import React, {useContext, useState} from "react";
import Button from "@material-ui/core/Button";
import Dialog from "@material-ui/core/Dialog";
import DialogActions from "@material-ui/core/DialogActions";
import DialogContent from "@material-ui/core/DialogContent";
import DialogTitle from "@material-ui/core/DialogTitle";
import Paper from "@material-ui/core/Paper";
import TextField from "@material-ui/core/TextField";
import Draggable from "react-draggable";
import {NAMELIST} from "../constant/Constants"
import Web3 from 'aion-web3';
import blake from 'blakejs';

import useStyles from "../../Style";
import myConfig from "../../config";
import FormControl from "@material-ui/core/FormControl";
import InputLabel from "@material-ui/core/InputLabel";
import Select from "@material-ui/core/Select";
import MenuItem from "@material-ui/core/MenuItem";
import {AppContext} from "../context/AppContext";

function PaperComponent(props) {
  return (
    <Draggable cancel={'[class*="MuiDialogContent-root"]'}>
      <Paper {...props} />
    </Draggable>
  );
}

export default function CreateStatement({handleDialog, isOpen}) {
  const [selectedStatement, setSelectedStatement, timer, setTimer, messageData, setMessageData, handleMessageDialog] = useContext(AppContext);

  const classes = useStyles();

  const [statementString, setStatementString] = useState("");
  const [answer, setAnswer] = useState("");
  const [salt, setSalt] = useState("");

  const handleSubmit = event => {
    event.preventDefault();
    // console.log({ statementString, answer, salt });
    sendSubmitTransaction(statementString, answer, salt);
    handleDialog(false);
    setStatementString("");
    setAnswer("");
    setSalt("")
  };

  window.onload = () => {
    if (window.aionweb3){
      console.log("✓ AIWA injected successfully");
    }
  };

  const sendSubmitTransaction = async (statementString, answer, salt) => {
    // const accountAddress = await window.aionweb3.enable();
    //set web3
    if(window.aionweb3) {
      let web3 = new Web3(
          new Web3.providers.HttpProvider(window.aionweb3.currentProvider.host)
      );

      let account = window.aionweb3.eth.accounts;
      console.log("detected account " + account);

      let statementBytes = toBytes(statementString);
      let res = answer.concat(salt);
      let answerHashBytes = hexToBytes(blake.blake2bHex(res, null, 32));

      let data = web3.avm.contract.method('submitStatement').inputs(['byte[]', "byte[]"], [statementBytes, answerHashBytes]).encode();

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

  function hexToBytes(hex) {
    let ret = new Uint8Array(hex.length / 2);
    for (let i = 0; i < ret.length; i++) {
      ret[i] = parseInt(hex.substring(i * 2, i * 2 + 2), 16)
    }
    return ret
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
          Submit a new statement
        </DialogTitle>

        <DialogContent>
          <form
            onSubmit={handleSubmit}
            className={classes.container}
            noValidate
            autoComplete="off"
          >
            <div>
              <TextField
                required
                id="standard-statement"
                label="Statement"
                name="statementString"
                value={statementString}
                onChange={e => setStatementString(e.target.value)}
                className={classes.textFieldMedium}
                margin="normal"
              />
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
                name="salt"
                value={salt}
                onChange={e => setSalt(e.target.value)}
                className={classes.textFieldMedium}
                margin="normal"
              />
            </div>
          </form>
        </DialogContent>
        <DialogActions>
          <Button autoFocus onClick={event => handleDialog(false)} style={{ background: '#E1DCCD' }}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            onClick={handleSubmit}
            style={{ background: '#E1DCCD' }}
          >
            Submit Statement
          </Button>
        </DialogActions>
      </Dialog>
    </div>
  );
}
