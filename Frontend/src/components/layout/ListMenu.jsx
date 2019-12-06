import React from "react";
import {makeStyles} from "@material-ui/core/styles";
import {List} from "@material-ui/core";
import ListItem from "@material-ui/core/ListItem";
import ListItemIcon from "@material-ui/core/ListItemIcon";
import ListItemText from "@material-ui/core/ListItemText";
import Divider from "@material-ui/core/Divider";
import {withRouter} from "react-router-dom"
import {ChevronRight} from "@material-ui/icons";


const useStyles = makeStyles(theme => ({
  root: {
    width: "100%",
    height: "100%",
    maxWidth: 360,
    backgroundColor: "#1c1c1c",
    color: '#E1DCCD'
  }
}));

function ListMenu({handleMenuChange, selectedMenu}) {
  const classes = useStyles();

  return (
    <div className={classes.root}>
      <List component="nav" aria-label="main mailbox folders">
        <ListItem
          button
          selected={selectedMenu === 0}
          onClick={event => handleMenuChange(event, 0)}
        >
          <ListItemIcon>
            <ChevronRight style={{color:'#E1DCCD'}}/>
          </ListItemIcon>
          <ListItemText primary="Statements" />
        </ListItem>

        {<Divider/>}

        <ListItem
          button
          selected={selectedMenu === 1}
          onClick={event => handleMenuChange(event, 1)}
        >
          <ListItemIcon>
            <ChevronRight style={{color:'#E1DCCD'}}/>
          </ListItemIcon>
          <ListItemText primary="Players"/>
        </ListItem>

        {<Divider/>}

        <ListItem
            button
            selected={selectedMenu === 2}
            onClick={event => handleMenuChange(event, 2)}
        >
          <ListItemIcon>
            <ChevronRight style={{color:'#E1DCCD'}}/>
          </ListItemIcon>
          <ListItemText primary="Votes"/>
        </ListItem>

        {<Divider/>}

        <ListItem
            button
            selected={selectedMenu === 3}
            onClick={event => handleMenuChange(event, 3)}
        >
          <ListItemIcon>
            <ChevronRight style={{color:'#E1DCCD'}}/>
          </ListItemIcon>
          <ListItemText primary="Answers"/>
        </ListItem>

        {<Divider/>}

        <ListItem
            button
            selected={selectedMenu === 4}
            onClick={event => handleMenuChange(event, 4)}
        >
          <ListItemIcon>
            <ChevronRight style={{color:'#E1DCCD'}}/>
          </ListItemIcon>
          <ListItemText primary="Game Status"/>
        </ListItem>
      </List>
    </div>
  )
}

export default withRouter(ListMenu);
