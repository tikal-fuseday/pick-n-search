import React, {useEffect, useState} from "react";
import {BrowserRouter as Router, Switch, Route, Link} from "react-router-dom";
import ImageCard from "./components/ImageCard";
import ImageInfo from "./components/ImageInfo";
import Loading from "./components/Loading";
import {mock} from "./mock";
import axios from "axios";
import "./App.css";

const App = () => {
  const [isLoading, toggleLoading] = useState(true);
  const [images, setImages] = useState([]);

  useEffect(() => {
    axios.get("https://us-central1-pick-n-search.cloudfunctions.net/read").then(res => {
      console.log(res);
      setImages(res.data);
      toggleLoading(false);
    });
    // fetch data from pulsar
  }, []);

  return (
    <Router>
      <div className="App">
        <Switch>
          <Route path="/image/:id" component={ImageInfo} />
          <Route exact path="/">
            {isLoading ? (
              <Loading />
            ) : (
              <div className="images">
                {images.map((img, index) => (
                  <ImageCard key={img.id} {...img} index={index} />
                ))}
              </div>
            )}
          </Route>
        </Switch>
      </div>
    </Router>
  );
};

export default App;
