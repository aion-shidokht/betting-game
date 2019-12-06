import React from "react";

import {BrowserRouter as Router} from "react-router-dom";
import {AppProvider} from "./context/AppContext";

import Layout from "./layout/Layout"

function App() {
  return (
    <AppProvider>
      <Router>
        <Layout/>
      </Router>
    </AppProvider>
  )
}

export default App;

