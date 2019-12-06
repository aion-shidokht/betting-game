import React, {useContext, useEffect, useState} from "react";
import MaterialTable from "material-table";
import axios from "axios";
import {AppContext} from "../context/AppContext";
import myConfig from "../../config";

function PlayerTable() {
  const [selectedStatement, setSelectedStatement, timer, setTimer, messageData, setMessageData, handleMessageDialog] = useContext(AppContext);

  const [state, setState] = useState({
    columns: [
      {title: "Player Address", field: "playerAddress.addressString"},
      {title: "Score", field: "score"},
      {title: "Depth", field: "age"},
      {title: "Transaction Hash", field: "transactionHash"}
    ],
    data: []
  });

  async function getPlayers() {

    await axios(myConfig.hostname + '/bettingOAP/state/allPlayers',
      {
        headers: {"Content-Type": "application/json"}
      }
    ).then(response => {
      const newPlayers = response.data;
      setState({...state, data: newPlayers});
      // console.log(newPlayers);
      console.log("Received players")
    }).catch(error => {
      handleError(error.message);
    });
    clearInterval(timer);
    setTimer(setTimeout(getPlayers, myConfig.pollingTime))
  }

  useEffect(() => {
    getPlayers();
    return () => {
      clearInterval(timer)
    }
  }, []);

  const handleError = error => {
    handleMessageDialog(true, error);
  };

  return (
    <div>
      <MaterialTable
        title="Registered Players"
        columns={state.columns}
        data={state.data}
        options={{
          pageSize: 20,
          pageSizeOptions: [20],
          paging: true,
          headerStyle: {
            backgroundColor: '#FF9B00',
            fontsize: 'medium',
            fontWeight: "bold",
          },
        }}
      />
    </div>
  );
}

export default PlayerTable
