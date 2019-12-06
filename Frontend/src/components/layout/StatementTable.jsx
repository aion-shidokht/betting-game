import React, {useContext, useEffect, useState} from "react";
import MaterialTable from "material-table";
import axios from "axios";
import CreateStatement from "./CreateStatement";
import VoteBox from "./VoteBox";
import RevealBox from "./RevealBox";
import {withRouter} from "react-router-dom";
import {AppContext} from "../context/AppContext";
import myConfig from "../../config";

function StatementTable({history}) {

  const [state, setState] = useState({
    columns: [
      {title: "Statement ID", field: "statementId"},
      {title: "Statement", field: "statementString"},
      {
        title: "Vote Count",
        field: "voteEventIds",
        render: rowData => (
          <span>{rowData.voteEventIds.length}</span>
        )
      },
      {title: "Answer", field: "answerString"},
    ],
    data: []
  });

  const [selectedStatement, setSelectedStatement, timer, setTimer, messageData, setMessageData, handleMessageDialog] = useContext(AppContext);

  const handleRowClick = (event, rowData) => {
    history.push("/statement-details");
    setSelectedStatement(rowData);
    clearTimeout(timer);
  };

  const [isCreateOpen, setIsCreateOpen] = useState(false);

  const handleCreateDialog = (isOpen) => {
    setIsCreateOpen(isOpen);
  };

  const [isVoteOpen, setIsVoteOpen] = useState(false);
  const [isRevealOpen, setIsRevealOpen] = useState(false);

  const handleVoteDialog = (isOpen) => {
    setIsVoteOpen(isOpen)
  };

  const handleRevealDialog = (isOpen) => {
    setIsRevealOpen(isOpen)
  };

  async function getStatements() {

    await axios(myConfig.hostname + '/bettingOAP/state/allStatements', {
        cancelToken: source.token
      },
      {
        headers: {"Content-Type": "application/json"}
      }
    ).then(response => {
      const newStatements = response.data;
      setState({...state, data: newStatements});
      // console.log(newStatements);
      console.log("Received statements")
    }).catch(error => {
      handleError(error.message);
    });
    clearInterval(timer)
    setTimer(setTimeout(getStatements, myConfig.pollingTime))
  }

  const source = axios.CancelToken.source();

  useEffect(() => {
    getStatements();
    return () => {
      clearInterval(timer);
      // console.log("cleanUp");
      source.cancel('Request canceled.');
    }
  }, []);

  const handleError = error => {
    handleMessageDialog(true, error);
  };

  return (
    <div>
      <MaterialTable
        title="Submitted Statements"
        columns={state.columns}
        data={state.data}
        actions={[
          {
            icon: "add",
            tooltip: "Add Statement",
            isFreeAction: true,
            onClick: event => {
              setIsCreateOpen(true)
            }
          },
          {
            icon: "how_to_vote",
            tooltip: "Vote",
            onClick: (event, rowData) => {
              setSelectedStatement(rowData)
              setIsVoteOpen(true);
            }
          },
          {
            icon: "announcement",
            tooltip: "Reveal Answer",
            onClick: (event, rowData) => {
              setSelectedStatement(rowData)
              setIsRevealOpen(true);
            }
          }
        ]}
        options={{
          pageSize: 20,
          pageSizeOptions: [20],
          paging: true,
          actionsColumnIndex: -1,
          headerStyle: {
            backgroundColor: '#FF9B00',
            fontsize: 'medium',
            fontWeight: "bold",
          },
        }}


        onRowClick={(event, rowData, togglePanel) => handleRowClick(event, rowData)}
      />
      <CreateStatement handleDialog={handleCreateDialog} isOpen={isCreateOpen}/>
      <VoteBox handleDialog={handleVoteDialog} isOpen={isVoteOpen}/>
      <RevealBox handleDialog={handleRevealDialog} isOpen={isRevealOpen}/>

    </div>
  );
}

export default withRouter(StatementTable)
