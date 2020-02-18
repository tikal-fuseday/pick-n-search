import React from "react";
import {useLocation, useHistory} from "react-router-dom";
import {makeStyles} from "@material-ui/core/styles";
import Paper from "@material-ui/core/Paper";
import Table from "@material-ui/core/Table";
import TableBody from "@material-ui/core/TableBody";
import TableCell from "@material-ui/core/TableCell";
import TableContainer from "@material-ui/core/TableContainer";
import TableHead from "@material-ui/core/TableHead";
import TableRow from "@material-ui/core/TableRow";
import Button from "@material-ui/core/Button";
import axios from "axios";

const useStyles = makeStyles(theme => ({
  root: {
    margin: 10
  },
  paper: {
    padding: 10
  },
  image: {
    maxWidth: "100%",
    maxHeight: "300px"
  },
  button: {
    margin: 20
  }
}));

const ImageInfo = props => {
  const classes = useStyles();
  const location = useLocation();
  const history = useHistory();
  const {data} = location.state;
  const dataF = JSON.parse(data.slice(2).slice(0, -1));
  const image = dataF.image;
  const labels = dataF.map;
  console.log(dataF);

  const onImageDeleted = () => {
    // axios.get("").then(res => {
    history.push("/");
    // });
  };
  return (
    <div className={classes.root}>
      <Paper className={classes.paper}>
        <img src={image} className={classes.image} />

        <TableContainer component={Paper}>
          <Table className={classes.table} aria-label="simple table">
            <TableHead>
              <TableRow>
                <TableCell>Label</TableCell>
                <TableCell>Confidence</TableCell>
              </TableRow>
            </TableHead>

            <TableBody>
              {Object.keys(labels).map(key => (
                <TableRow key={key}>
                  <TableCell component="th" scope="row">
                    {key}
                  </TableCell>
                  <TableCell>{labels[key]}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>

        {/* <Button variant="contained" color="secondary" onClick={onImageDeleted}>
          Delete Image
        </Button> */}
      </Paper>
    </div>
  );
};
export default ImageInfo;
