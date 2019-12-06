import React, {useContext, useEffect, useState} from "react";
import MaterialTable from "material-table";
import axios from "axios";
import {AppContext} from "../context/AppContext";
import myConfig from "../../config";

function VoteTable() {
    const [selectedStatement, setSelectedStatement, timer, setTimer, messageData, setMessageData, handleMessageDialog] = useContext(AppContext);

    const [state, setState] = useState({
        columns: [
            {title: "Player Address", field: "playerAddress.addressString"},
            {title: "Statement Id", field: "statementId"},
            {title: "Guess", field: "guessedAnswer"},
            {title: "Transaction Hash", field: "transactionHash"},
            {title: "Depth", field: "age"},
            {title: "Correct", field: "correct", render: rowData => (
                    <span>{rowData.correct ? "Yes" : "No"}</span>
                )}
        ],
        data: []
    });

    async function getVotes() {

        await axios(myConfig.hostname + '/bettingOAP/state/allVotes',
            {
                headers: {"Content-Type": "application/json"}
            }
        ).then(response => {
            const newVotes = response.data;
            setState({...state, data: newVotes});
            // console.log(newVotes);
            console.log("Received votes")
        }).catch(error => {
            handleError(error.message)
        });
        clearInterval(timer);
        setTimer(setTimeout(getVotes, myConfig.pollingTime))
    }

    useEffect(() => {
        getVotes();
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
                title="Submitted Votes"
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

export default VoteTable
