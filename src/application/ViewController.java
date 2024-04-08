package application;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class ViewController {
    // overall layout
    @FXML BorderPane appContainer;

    // top stuff
    @FXML HBox topBox;
    @FXML Text titleText;

    // center stuff
    @FXML AnchorPane centerPane;
    @FXML Pane canvasHolder;
    @FXML Canvas canvasOfLife;

    // bottom stuff
    @FXML VBox bottomGroup;
    @FXML HBox bottomBox1;
    @FXML Button clearButton;
    @FXML Button randomButton;
    @FXML Button pauseButton;
    @FXML Button playButton;
    @FXML Button stepButton;
    @FXML HBox bottomBox2;
    @FXML Text debugInfo;

    public void initialize() {
        adjustCanvasDimensionsForBorderJank();
        initButtonHandlers();
    }

    private void adjustCanvasDimensionsForBorderJank() {
        /*
        See the FXML comments above the declaration of the centerPane.

        We first anchor the inner pane to the outer pane's insets,
        then bind the canvas dimensions to the inner pane.

        This should perfectly align the canvas inside the anchor pane
        so that (0, 0) through (canvas width, canvas height) is
        immediately inside the borders.

        There's probably a better way to accomplish this,
        but this works, so ¯\_(ツ)_/¯
        */
       var insets = centerPane.getInsets();
       AnchorPane.setTopAnchor(canvasHolder, insets.getTop());
       AnchorPane.setRightAnchor(canvasHolder, insets.getRight());
       AnchorPane.setBottomAnchor(canvasHolder, insets.getBottom());
       AnchorPane.setLeftAnchor(canvasHolder, insets.getLeft());

       canvasOfLife.widthProperty().bind(canvasHolder.widthProperty());
       canvasOfLife.heightProperty().bind(canvasHolder.heightProperty());
    }

    private void initButtonHandlers() {
        clearButton.setOnAction(event -> {
            debugInfo.setText("You clicked the CLEAR button");
        });

        randomButton.setOnAction(event -> {
            debugInfo.setText("You clicked the RANDOM button");
        });

        pauseButton.setOnAction(event -> {
            debugInfo.setText("You clicked the PAUSE button");
        });

        playButton.setOnAction(event -> {
            debugInfo.setText("You clicked the PLAY button");
        });

        stepButton.setOnAction(event -> {
            debugInfo.setText("You clicked the CLEAR button");
        });
    }
}
