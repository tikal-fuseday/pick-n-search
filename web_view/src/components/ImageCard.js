import React from "react";
import {useHistory} from "react-router-dom";
import {makeStyles} from "@material-ui/core/styles";
import Card from "@material-ui/core/Card";
import CardMedia from "@material-ui/core/CardMedia";
import Fade from "react-reveal/Fade";

const useStyles = makeStyles({
  root: {
    maxWidth: "100%",
    width: 300
  },
  media: {
    height: 150,
    margin: 10
  }
});

const Image = props => {
  const classes = useStyles();
  const {data, index = 0, id} = props;
  console.log(data);
  const dataF = JSON.parse(data.slice(2).slice(0, -1));
  const image = dataF.image;
  const history = useHistory();

  const onImageClicked = () => {
    history.push({
      pathname: `image/${id}`,
      state: props
    });
  };

  return (
    <Fade top distance="20px">
      <Card className={classes.root} onClick={onImageClicked}>
        <CardMedia image={image} title={id} className={classes.media} />
      </Card>
    </Fade>
  );
};
export default Image;
