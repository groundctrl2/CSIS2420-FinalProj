package application;

import java.time.Duration;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import model.CellState;
import model.ILife;
import model.SimpleLife;

public class ViewController {
    // overall layout
    @FXML private BorderPane appContainer;

    // top stuff
    @FXML private HBox topBox;
    @FXML private Text titleText;

    // center stuff
    @FXML private AnchorPane centerPane;
    @FXML private Pane canvasHolder;
    @FXML private Canvas canvas;

    // bottom stuff
    @FXML private VBox bottomBox;
    @FXML private HBox buttonGroup;
    @FXML private Button clearButton;
    @FXML private Button randomButton;
    @FXML private Button pausePlayButton;
    @FXML private Button stepButton;
    @FXML private HBox debugGroup;
    @FXML private Text debugText;
    @FXML private Slider tpsSlider;
    @FXML private Label tpsSliderValue;

    // Grid/Canvas stuff.
    private static final int CELL_INTERIOR_SIZE = 14;
    // Half the border belongs to one cell, and the other half to neigbor.
    private static final int CELL_BORDER_WIDTH = 2; // preferably a multiple of 2
    // Size including border is: interior + 2*(border / 2)
    private static final int CELL_SIZE = CELL_INTERIOR_SIZE + CELL_BORDER_WIDTH;

    private double canvasWidth;
    private double canvasHeight;
    // TODO: deal with remainders
    private int ncols;
    private int nrows;

    private ILife model = new SimpleLife();

    // animation stuff
    private boolean isPlaying;
    private long timestamp;
    private int ticksPerSecond = 30;

    public void initialize() {
        adjustCanvasDimensionsForBorderJank();
        initButtonHandlers();

        // Update tps and slider value label when slider changes value
        tpsSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            ticksPerSecond = newValue.intValue();
            tpsSliderValue.setText(String.valueOf(ticksPerSecond));
        });

        // Set initial value for value label
        tpsSliderValue.setText(String.valueOf(ticksPerSecond));
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
       canvas.widthProperty().addListener((ov, oldValue, newValue) -> resizeGrid());
       canvas.heightProperty().addListener((ov, oldValue, newValue) -> resizeGrid());

       canvas.setOnMouseMoved(event -> {
           int x = (int) event.getX();
           int y = (int) event.getY();
           debugText.setText("coordinate: (%d, %d)".formatted(x, y));
       });

       canvas.setOnMouseClicked(this::handleCanvasMouseClick);
    }

    private void initButtonHandlers() {
        var timer = new AnimationTimer() {
            @Override
            public void handle(long now){
                var tick = Duration.ofSeconds(1).dividedBy(ticksPerSecond);

                if ((now - timestamp) > tick.toNanos()) {
                    model.step(ViewController.this::setDisplayCell);
                    drawGrid();
                    timestamp = now;
                }
            }
        };

        clearButton.setOnAction(event -> {
            if (isPlaying)
                pausePlayButton.fire();

            debugText.setText("You clicked the CLEAR button");
            model.clear();
            drawGrid();
        });

        randomButton.setOnAction(event -> {
            debugText.setText("You clicked the RANDOM button");
            model.randomize();
            drawGrid();
        });

        pausePlayButton.setOnAction(event -> {
            if (isPlaying) {
                timer.stop();
                pausePlayButton.setText("PLAY");
                debugText.setText("You clicked the PAUSE button");
            }
            else {
                timer.start();
                pausePlayButton.setText("PAUSE");
                debugText.setText("You clicked the PLAY button");
            }

            isPlaying = !isPlaying;
        });

        stepButton.setOnAction(event -> {
            debugText.setText("You clicked the STEP button");

            if (!isPlaying) {
                model.step(this::setDisplayCell);
                drawGrid();
            }
        });
    }

    /** Convert from y-coordinate to row index, rounding down */
    private int toRowIndex(double y) {
        return Math.min((int) (y / CELL_SIZE), nrows - 1);
    }

    /** Convert from x-coordinate to column index, rounding down */
    private int toColIndex(double x) {
        return Math.min((int) (x / CELL_SIZE), ncols - 1);
    }

    // TODO: check edge cases of coordinate-to-index conversions (rounding errors?)

    /** Convert from row index to y-coordinate of the top-left of cell interior */
    private double toYCoord(int row) {
        return (CELL_BORDER_WIDTH / 2) + row*CELL_SIZE;
    }

    /** Convert from column index to x-coordinate of the top-left of cell interior */
    private double toXCoord(int col) {
        return (CELL_BORDER_WIDTH / 2) + col*CELL_SIZE;
    }

    /** Callback for model#step */
    private void setDisplayCell(int row, int col, CellState state) {
        var g = canvas.getGraphicsContext2D();
        double x0 = toXCoord(col);
        double y0 = toYCoord(row);
        g.setFill(state == CellState.ALIVE ? Color.BLACK : Color.WHITE);
        g.fillRect(x0, y0, CELL_INTERIOR_SIZE, CELL_INTERIOR_SIZE);
    };

    private void resizeGrid() {
        canvasWidth = canvas.getWidth();
        canvasHeight = canvas.getHeight();
        ncols = (int) canvasWidth / CELL_SIZE;
        nrows = (int) canvasHeight / CELL_SIZE;
        model.resize(nrows, ncols);

        drawGrid();
    }

    private void drawGrid() {
        var g = canvas.getGraphicsContext2D();
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, canvasWidth, canvasHeight);
        g.setStroke(Color.LIGHTGRAY);
        g.setLineWidth(CELL_BORDER_WIDTH);

        // draw verticals
        for (int x = 0; x < canvasWidth; x += CELL_SIZE)
            g.strokeLine(x, 0, x, canvasHeight);

        // draw horizontals
        for (int y = 0; y < canvasHeight; y += CELL_SIZE)
            g.strokeLine(0, y, canvasWidth, y);

        // fill in cells which are alive according to the model
        g.setFill(Color.BLACK);

        model.forAllLife((row, col, state) -> {
            double x0 = toXCoord(col);
            double y0 = toYCoord(row);
            g.fillRect(x0, y0, CELL_INTERIOR_SIZE, CELL_INTERIOR_SIZE);
        });
    }

    private void handleCanvasMouseClick(MouseEvent event) {
        var g = canvas.getGraphicsContext2D();

        double x = event.getX();
        double y = event.getY();

        int row = toRowIndex(y);
        int col = toColIndex(x);

        // top-left coordinates cell
        double x0 = toXCoord(col);
        double y0 = toYCoord(row);

        if (model.get(row, col) == CellState.DEAD) {
            model.set(row, col, CellState.ALIVE);
            g.setFill(Color.BLACK);
        }
        else { // (model.get(row, col) == CellState.ALIVE)
            model.set(row,  col, CellState.DEAD);
            g.setFill(Color.WHITE);
        }

        g.fillRect(x0, y0, CELL_INTERIOR_SIZE, CELL_INTERIOR_SIZE);

        debugText.setText("You clicked on cell (%d, %d)!".formatted(row, col));
    }
}
