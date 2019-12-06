import React, {useContext, useEffect, useState} from "react";
import MaterialTable from "material-table";
import axios from "axios";
import {AppContext} from "../context/AppContext";
import myConfig from "../../config";

function AnswerTable() {
    const [selectedStatement, setSelectedStatement, timer, setTimer, messageData, setMessageData, handleMessageDialog] = useContext(AppContext);

    const [state, setState] = useState({
        columns: [
            {title: "Statement Id", field: "statementId"},
            {title: "Answer", field: "answer"},
            {title: "Depth", field: "age"},
            {title: "Transaction Hash", field: "transactionHash"}
        ],
        data: []
    });

    async function getAnswers() {

        await axios(myConfig.hostname + '/bettingOAP/state/allAnswers',
            {
                headers: {"Content-Type": "application/json"}
            }
        ).then(response => {
            const newAnswers = response.data;
            setState({...state, data: newAnswers});
            console.log("received answers")
        }).catch(error => {
            handleError(error.message)
        });
        clearInterval(timer);
        setTimer(setTimeout(getAnswers, myConfig.pollingTime))
    }

    useEffect(() => {
        getAnswers();
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
                title="Submitted Answers"
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

export default AnswerTable
