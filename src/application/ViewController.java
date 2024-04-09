package application;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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
    @FXML Canvas canvas;

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

    // temporary
    @FXML Button debug;

    // Canvas stuff.
    private static final int CELL_INTERIOR_SIZE = 14;
    private static final int CELL_BORDER_WIDTH = 2; // preferably a multiple of 2

    private double width;
    private double height;
    // TODO: deal with remainders
    private int cols;
    private int rows;

    private boolean[][] grid;

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

       canvas.widthProperty().bind(canvasHolder.widthProperty());
       canvas.heightProperty().bind(canvasHolder.heightProperty());

       // clear and redraw on resizes
       canvas.widthProperty().addListener(observable -> drawGrid());
       canvas.heightProperty().addListener(observable -> drawGrid());

       canvas.setOnMouseMoved(event -> {
           int x = (int) event.getX();
           int y = (int) event.getY();
           debugInfo.setText("coordinate: (%d, %d)".formatted(x, y));
       });

       canvas.setOnMouseClicked(this::handleCanvasMouseClick);
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

    // call in LifeApp#main() after stage.show()
    void drawGrid() {
        width = canvas.getWidth();
        height = canvas.getHeight();

        var g = canvas.getGraphicsContext2D();
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, width, height);

        g.setStroke(Color.LIGHTGRAY);
        g.setLineWidth(CELL_BORDER_WIDTH);
        int stride = CELL_INTERIOR_SIZE + CELL_BORDER_WIDTH;
        cols = (int) width / stride;
        rows = (int) height / stride;
        grid = new boolean[rows][cols];

        // draw verticals
        for (int x = 0; x < width; x += stride)
            g.strokeLine(x, 0, x, height);

        // draw horizontals
        for (int y = 0; y < height; y += stride)
            g.strokeLine(0, y, width, y);
    }

    void handleCanvasMouseClick(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();

        // convert from canvas coordinates to grid indices
        // TODO: write helper methods to convert to and from coordinates
        // to indices for later use.
        int cellSize = CELL_INTERIOR_SIZE + CELL_BORDER_WIDTH;
        int r = (int) (y / height * rows);
        int c = (int) (x / width * cols);

        // top-left coordinates cell
        double x0 = (CELL_BORDER_WIDTH / 2) + c*cellSize;
        double y0 = (CELL_BORDER_WIDTH / 2) + r*cellSize;

        // toggle cell
        // TODO: handle orphan cells on the right and bottom edges.
        var g = canvas.getGraphicsContext2D();
        boolean alive = (grid[r][c] = !grid[r][c]);
        g.setFill(alive ? Color.BLACK : Color.WHITE);
        g.fillRect(x0, y0, CELL_INTERIOR_SIZE, CELL_INTERIOR_SIZE);

        debugInfo.setText("You clicked on cell (%d, %d)!".formatted(r, c));
    }
}
