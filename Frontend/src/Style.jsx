import { makeStyles } from "@material-ui/core/styles";

const useStyles = makeStyles(theme => ({
  container: {
    display: "flex",
    flexWrap: "wrap",
    flex: 100
  },
  button: {
    marginTop: 40,
    marginRight: 40
  },
  textFieldShort: {
    marginLeft: theme.spacing(2),
    marginRight: theme.spacing(2),
    width: 200
  },
  textFieldMedium: {
    marginLeft: theme.spacing(2),
    marginRight: theme.spacing(2),
    width: 400
  },
  textFieldLong: {
    marginLeft: theme.spacing(2),
    marginRight: theme.spacing(2),
    width: 600
  },
  Paper: {
    height: "100%",
    padding: 20,
    marginTop: 10,
    marginBottom: 10
  },
  formControl: {
    margin: theme.spacing(2),
    minWidth: 120,
  },
  selectEmpty: {
    marginTop: theme.spacing(2),
  },
}));

export default useStyles;
