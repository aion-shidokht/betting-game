import React from "react";
import Button from "@material-ui/core/Button";
import Dialog from "@material-ui/core/Dialog";
import DialogActions from "@material-ui/core/DialogActions";
import DialogContent from "@material-ui/core/DialogContent";
import DialogContentText from "@material-ui/core/DialogContentText";
import DialogTitle from "@material-ui/core/DialogTitle";

export default function MessageBox({handleDialog, isOpen, message}) {

  return (
    <div>
      <Dialog
        fullWidth={true}
        maxWidth="sm"
        open={isOpen}
        onClose={event => handleDialog(false, "")}
        aria-labelledby="alert-dialog-title"
        aria-describedby="alert-dialog-description"
      >
        <DialogTitle id="alert-dialog-title">{"Error"}</DialogTitle>
        <DialogContent>
          <DialogContentText id="alert-dialog-description">
            {message}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={event => handleDialog(false, "")} style={{ background: '#E1DCCD' }}>
            Ok
          </Button>
        </DialogActions>
      </Dialog>
    </div>
  );
}
