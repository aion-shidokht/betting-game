import React, {useContext, useState} from "react"
import {Route, Switch, withRouter} from "react-router-dom"
import {Grid, Paper} from "@material-ui/core";
import Header from "./Header";
import ListMenu from "./ListMenu";
import StatementTable from "./StatementTable";
import StatementDetails from "./StatementDetails";
import PlayerTable from "./PlayerTable";
import VoteTable from "./VoteTable";
import AnswerTable from "./AnswerTable";
import useStyles from "../../Style";
import {AppContext} from "../context/AppContext";
import MessageBox from "./MessageBox";
import GameStatus from "./GameStatus";


function Layout({history}) {
  const [selectedStatement, setSelectedStatement, timer, setTimer, messageData, setMessageData, handleMessageDialog] = useContext(AppContext);

  const classes = useStyles();
  const [selectedMenu, setSelectedMenu] = useState(0)

  const handleMenuChange = (event, selectedIndex) => {
    setSelectedMenu(selectedIndex)
    clearInterval(timer);
    if (selectedIndex === 0) {
      history.push('/statements')
    } else if (selectedIndex === 1){
      history.push('/players')
    } else if (selectedIndex === 2){
      history.push('/votes')
    } else if (selectedIndex === 3){
      history.push('/answers')
    } else {
      history.push('/game')
    }
  };

  return (
    <Grid container>
      <Header/>
      <Grid container>
        <Grid item sm={2} zeroMinWidth>
          <Paper className={classes.Paper}>
            <ListMenu handleMenuChange={handleMenuChange} selectedMenu={selectedMenu}/>
          </Paper>
        </Grid>
        <Grid item sm={10} zeroMinWidth>
          <Paper className={classes.Paper}>
            <Switch>
              <Route exact path="/" component={StatementTable}/>
              <Route exact path="/statements" component={StatementTable}/>
              <Route exact path="/statement-details" component={StatementDetails}/>
              <Route exact path="/players" component={PlayerTable}/>
              <Route exact path="/votes" component={VoteTable}/>
              <Route exact path="/answers" component={AnswerTable}/>
              <Route exact path="/game" component={GameStatus}/>
            </Switch>
          </Paper>
        </Grid>
      </Grid>
      <MessageBox handleDialog={handleMessageDialog} isOpen={messageData.isOpen} message={messageData.message}/>
    </Grid>
  );
}

export default withRouter(Layout)
