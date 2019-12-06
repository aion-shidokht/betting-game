import React, {useContext} from "react";
import TextField from "@material-ui/core/TextField";
import {withRouter} from "react-router-dom"
import {Button, Container} from "@material-ui/core";
import {AppContext} from "../context/AppContext";

import useStyles from "../../Style";

function StatementDetails({history}) {
  const classes = useStyles();

    const [selectedStatement, setSelectedStatement, timer, setTimer, messageData, setMessageData, handleMessageDialog] = useContext(AppContext);

    return (
    <form className={classes.container} noValidate autoComplete="off">
      <div>
        <TextField
          label="Statement ID"
          InputProps={{
            readOnly: true
          }}
          id="standard-required"
          value={
            selectedStatement.statementId}
          className={classes.textFieldShort}
          margin="normal"
        />

          <TextField
              label="Statement"
              InputProps={{
                  readOnly: true
              }}
              id="standard-required"
              value={selectedStatement.statementString}
              className={classes.textFieldLong}
              margin="normal"
          />
      </div>
        <div>
            <TextField
                label="Player Address"
                InputProps={{
                    readOnly: true
                }}
                id="standard-required"
                value={selectedStatement.playerAddress ? selectedStatement.playerAddress.addressString : ""}
                className={classes.textFieldLong}
                margin="normal"
            />
        </div>

        <div>
            <TextField
                label="Answer Hash"
                InputProps={{
                    readOnly: true
                }}
                id="standard-required"
                value={selectedStatement.answerHash }
                className={classes.textFieldLong}
                margin="normal"
            />
        </div>
        <TextField
            label="Vote Count"
            InputProps={{
                readOnly: true
            }}
            id="standard-required"
            value={selectedStatement.voteEventIds ? selectedStatement.voteEventIds.length : "0"}
            className={classes.textFieldShort}
            margin="normal"
        />

          <div>
              <TextField
                  label="Statement Depth"
                  InputProps={{
                      readOnly: true
                  }}
                  id="standard-required"
                  value={selectedStatement.statementAge }
                  className={classes.textFieldLong}
                  margin="normal"
              />
          </div>
        <div>
            <TextField
                label="Statement Transaction Hash"
                InputProps={{
                    readOnly: true
                }}
                id="standard-required"
                value={selectedStatement.statementTransactionHash}
                className={classes.textFieldLong}
                margin="normal"
            />
        </div>
          <div>
              <TextField
                  label="Answer"
                  InputProps={{
                      readOnly: true
                  }}
                  id="standard-required"
                  value={selectedStatement.answerString}
                  className={classes.textFieldLong}
                  margin="normal"
              />
          </div>

          <div>
              <TextField
                  label="Answer Depth"
                  InputProps={{
                      readOnly: true
                  }}
                  id="standard-required"
                  value={selectedStatement.answerAge ? selectedStatement.answerAge : ""}
                  className={classes.textFieldLong}
                  margin="normal"
              />
          </div>

          <div>
              <TextField
                  label="Answer Transaction Hash"
                  InputProps={{
                      readOnly: true
                  }}
                  id="standard-required"
                  value={selectedStatement.answerTransactionHash}
                  className={classes.textFieldLong}
                  margin="normal"
              />
          </div>
      <Container>
        <Button
         // style={{marginTop: 40, marginRight: 40}}
          variant="contained"
          style={{ background: '#E1DCCD'}}
          onClick={event => history.push("/statements")}
        >
          Back
        </Button>
      </Container>
    </form>
  );
}

export default withRouter(StatementDetails)
