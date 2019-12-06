import React from "react";
import {AppBar, Toolbar, Typography} from "@material-ui/core";

function Header() {
  return (
    <AppBar style={{ background: '#1C1C1C' }} position="static">
      <Toolbar>
        <Typography variant="h5" color="inherit">
          Guessing Game
        </Typography>
      </Toolbar>
    </AppBar>
  );
}

export default Header;
