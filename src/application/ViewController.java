package application;

import java.time.Duration;
import java.util.HashMap;

import javafx.animation.AnimationTimer;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import model.CellState;
import model.ILife;

/**
 * Controller for the scene graph defined in <a href="LifeView.fxml">LifeView.fxml</a>.
 * <p>
 * This class manages the interaction and facilitates communication between the
 * model (classes that run the simulation behind the scenes) and the view (the
 * visual display and UI components).
 */
public class ViewController {
	/*
	 * For convenience, we make a handle to each major component in the scene graph,
	 * even though we might currently use some of them directly.
	 *
	 * Each @FXML annotated variable is injected from the FXML file by the
	 * FXMLLoader using the 'fx:id' attributes of each element as the name for the
	 * target instance variable.
	 *
	 * Once the FXML is loaded and the variables are injected, the initialize()
	 * method is called for post-processing, so start there to follow the
	 * program behavior.
	 */

	// ==================
	// Component handles
	// ==================
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
	@FXML private Text flavorText;
	@FXML private HBox buttonGroup;
	@FXML private Button clearButton;
	@FXML private Button randomButton;
	@FXML private Button pausePlayButton;
	@FXML private Button stepButton;
	@FXML private Text debugText;
	@FXML private HBox tpsSliderGroup;
	@FXML private Label tpsSliderLabel;
	@FXML private Slider tpsSlider;
	@FXML private Label tpsSliderValue;
	@FXML private ComboBox<String> modelCBox;

	// ==================
	// Grid/Canvas stuff
	// ==================

	// each cell should be square
	private static final int CELL_INTERIOR_SIZE = 10;
	// each cell will have a visible border
	private static final int CELL_BORDER_WIDTH = 1;
	// complete size including border
	private static final int CELL_SIZE = CELL_INTERIOR_SIZE + 2*CELL_BORDER_WIDTH;

	private double canvasWidth;
	private double canvasHeight;
	// TODO: deal with remainders
	private int ncols;
	private int nrows;

	// handle for the implementation of the simulation itself
	private ILife model = new model.VampireLife();

	// ================
	// Animation stuff
	// ================
	private boolean isPlaying;
	private long timestamp;
	private int ticksPerSecond = 2;
	private int stepCount;
	private boolean restart;

	/**
	 * Performs post-processing of the scene graph after loading it from the FXML.
	 * In general, this mostly just adds event handlers for various components.
	 */
	public void initialize() {
		adjustCanvasDimensionsForBorderJank();
		initButtonHandlers();
		initTpsSliderGroup();
		initModelSelectorBox();
	}

	private void initTpsSliderGroup() {
		// Update tps and slider value label when slider changes value
		tpsSlider.valueProperty().addListener((ov, oldValue, newValue) -> {
			ticksPerSecond = newValue.intValue();
			tpsSliderValue.setText(String.valueOf(ticksPerSecond));

			if (isPlaying && ticksPerSecond > 30)
				flavorText.setText("Chaos!");
		});

		// Set initial value for slider and its value label
		tpsSlider.setValue(ticksPerSecond);
		tpsSliderValue.setText(String.valueOf(ticksPerSecond));
	}

	private void initModelSelectorBox() {
		var items = modelCBox.getItems();
		var lookupTable = new HashMap<String, Class<? extends ILife>>();

		/*
		 * Add the name of each concrete model class to the combo box,
		 * and link the the name to the actual class object in a map.
		 */
		for (var cls : ILife.implementations()) {
			String key = cls.getSimpleName();
			items.add(key);
			lookupTable.put(key, cls);
		}

		// Set the current value to the current model's class.
		String currentSelection = model.getClass().getSimpleName();
		assert lookupTable.containsKey(currentSelection): currentSelection;
		modelCBox.setValue(currentSelection);

		// Update the model whenever the combo box value changes.
		modelCBox.setOnAction(event -> {
			var className = modelCBox.getValue();
			var selectedClass = lookupTable.get(className);

			if (selectedClass.equals(model.getClass())) {
				debugText.setText("No change");
				return;
			}

			try {
				model = selectedClass.getConstructor().newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}

			resizeModel();
		});
	}

	/**
	 * Initializes the canvas.
	 */
	private void adjustCanvasDimensionsForBorderJank() {
		/*
		 * See the FXML comments above the declaration of the centerPane.
		 *
		 * We first anchor the inner pane to the outer pane's insets, then bind the
		 * canvas dimensions to the inner pane.
		 *
		 * This should perfectly align the canvas inside the anchor pane so that (0, 0)
		 * through (canvas width, canvas height) is immediately inside the borders.
		 *
		 * There's probably a better way to accomplish this, but this works, so
		 * ¯\_(ツ)_/¯
		 */
		var insets = centerPane.getInsets();
		AnchorPane.setTopAnchor(canvasHolder, insets.getTop());
		AnchorPane.setRightAnchor(canvasHolder, insets.getRight());
		AnchorPane.setBottomAnchor(canvasHolder, insets.getBottom());
		AnchorPane.setLeftAnchor(canvasHolder, insets.getLeft());

		// Bind the canvas dimensions to its parent component, so that it
		// ultimately resizes when the window resizes.
		canvas.widthProperty().bind(canvasHolder.widthProperty());
		canvas.heightProperty().bind(canvasHolder.heightProperty());

		// Clear and redraw whenever the canvas viewport is resized.
		canvas.widthProperty().addListener((ov, oldValue, newValue) -> resizeGrid());
		canvas.heightProperty().addListener((ov, oldValue, newValue) -> resizeGrid());

		// For debugging. TODO: delete this
		canvas.setOnMouseMoved(event -> {
			// Displaying step count takes precedence over the mouse position
			// during simulation or at the end of a simulation that stalls.
			if (isPlaying || restart)
				return;

			int x = (int) event.getX();
			int y = (int) event.getY();
			int c = toColIndex(x);
			int r = toRowIndex(y);
			debugText.setText("pos: (%d, %d), cell: [%d, %d]".formatted(x, y, r, c));
		});

		// Enable click-to-toggle functionality.
		canvas.setOnMouseClicked(this::toggleDisplayCell);
	}

	/**
	 * Sets the actions for the main buttons.
	 */
	private void initButtonHandlers() {
		// The timer just calls the model's step() function to advance the
		// simulation by one step on each tick.
		var timer = new AnimationTimer() {
			@Override
			public void handle(long now){
				var tick = Duration.ofSeconds(1).dividedBy(ticksPerSecond);

				if ((now - timestamp) > tick.toNanos()) {
					reactToStep(model.step(ViewController.this::setDisplayCell));
					timestamp = now;
				}
			}
		};

		clearButton.setOnAction(event -> {
			if (isPlaying)
				pausePlayButton.fire();

			flavorText.setText("The slate has been wiped clean");
			model.clear();
			redrawGrid();
			stepCount = 0;
			restart = false;
		});

		randomButton.setOnAction(event -> {
			flavorText.setText("Chaos!");
			model.randomize();
			redrawGrid();
			stepCount = 0;
			restart = false;
		});

		pausePlayButton.setOnAction(event -> {
			if (isPlaying) {
				timer.stop();
				pausePlayButton.setText("PLAY");
				stepButton.setDisable(false);
			}
			else {
				timer.start();
				pausePlayButton.setText("PAUSE");
				stepButton.setDisable(true);
			}

			flavorText.setText("...");
			isPlaying = !isPlaying;
		});

		stepButton.setOnAction(event -> {
			reactToStep(model.step(this::setDisplayCell));
		});
	}

	/**
	 * Nonsense.
	 */
	private void reactToStep(boolean change) {
		if (restart) {
			stepCount = 0;
			restart = false;
			flavorText.setText("Another round.");
		}

		if (change)
			stepCount++;
		else {
			// Stop animating if the simulation stalls (reaches a fixed point).
			if (isPlaying)
				pausePlayButton.fire();

			// Reset the step count next time.
			restart = true;

			if (stepCount > 0) {
				if (model.populationCount() > 0) {
					flavorText.setText("Life prevails.");

					if (model instanceof model.VampireLife)
						temporarilySetFill(flavorText, Color.DARKRED, flavorText.textProperty());
				}
				else {
					flavorText.setText("In the end, death claims all.");
				}
			}
		}

		// TODO: detect cycles and react accordingly

		debugText.setText("Step count: " + stepCount);
	}

	/**
	 * Temporarily changes the color of a component.
	 *
	 * @param shape the colorable component
	 * @param fill the temporary color
	 * @param property the property whose change of value should cause the
	 *                 old color to be restored.
	 * @param <T> the type of the value wrapped by {@code property}
	 */
	private <T> void temporarilySetFill(Shape shape, Paint fill, Property<T> property) {
		var savedFill = shape.getFill();
		shape.setFill(fill);

		/*
		 * Add a one-shot change listener that restores the old fill. An anonymous class
		 * is used instead of lambda so that we can self-reference the listener.
		 */
		property.addListener(new ChangeListener<T>() {
			@Override
			public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
				shape.setFill(savedFill);
				property.removeListener(this);
			}
		});
	}

	/* ===============================
	 * Canvas/Grid drawing functions.
	 * ===============================
	 *
	 * TODO: move grid data and functions into a separate class so that we can
	 * switch grid types (rectangular <-> hex).
	 */

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
		return CELL_BORDER_WIDTH + row*CELL_SIZE;
	}

	/** Convert from column index to x-coordinate of the top-left of cell interior */
	private double toXCoord(int col) {
		return CELL_BORDER_WIDTH + col*CELL_SIZE;
	}

	/**
	 * Callback for {@link model.ILife#step}.
	 * <p>
	 * This is passed (as a lambda) to the model so that it can notify the
	 * controller whenever a cell changes state, allowing the canvas/grid to be
	 * incrementally updated.
	 * <p>
	 * In theory, this should be more efficient than redrawing the whole grid on
	 * each step, but with GPUs and buffering and caches, maybe not.
	 */
	private void setDisplayCell(int row, int col, CellState state) {
		var g = canvas.getGraphicsContext2D();
		double x0 = toXCoord(col);
		double y0 = toYCoord(row);

		decideColor(g, state);
		g.fillRect(x0, y0, CELL_INTERIOR_SIZE, CELL_INTERIOR_SIZE);
	};

	/**
	 * Resizes the grid. Currently, this also clears the grid, but it technically
	 * doesn't have to.
	 */
	private void resizeGrid() {
		canvasWidth = canvas.getWidth();
		canvasHeight = canvas.getHeight();
		ncols = (int) canvasWidth / CELL_SIZE;
		nrows = (int) canvasHeight / CELL_SIZE;
		resizeModel();
	}

	private void resizeModel() {
		model.resize(nrows, ncols);

		// reset animation variables
		if (isPlaying)
			pausePlayButton.fire();

		assert !isPlaying;
		timestamp = 0;
		stepCount = 0;
		flavorText.setText("In the beginning, there was nothing...");

		redrawGrid();
	}

	/**
	 * Redraws the whole grid by querying the model for the state of each
	 * living cell.
	 */
	private void redrawGrid() {
		var g = canvas.getGraphicsContext2D();
		/*
		 * We could render each cell by using fillRect() followed by strokeRect() for
		 * the cell borders. Alternatively, we can draw all the borders as grid lines
		 * over the whole canvas, and then fill in the cell interiors. We currently,
		 * take the second approach below.
		 */
		g.setFill(Color.WHITE);
		g.fillRect(0, 0, canvasWidth, canvasHeight);
		g.setStroke(Color.LIGHTGRAY);
		g.setLineWidth(2*CELL_BORDER_WIDTH);

		// Draw vertical grid lines
		for (int x = 0; x < canvasWidth; x += CELL_SIZE)
			g.strokeLine(x, 0, x, canvasHeight);

		// Draw horizontal grid lines
		for (int y = 0; y < canvasHeight; y += CELL_SIZE)
			g.strokeLine(0, y, canvasWidth, y);

		// Fill in cells which are alive according to the model
		g.setFill(Color.BLACK);
		model.forAllLife((row, col, state) -> {
			decideColor(g, state);

			double x0 = toXCoord(col);
			double y0 = toYCoord(row);
			g.fillRect(x0, y0, CELL_INTERIOR_SIZE, CELL_INTERIOR_SIZE);
		});
	}

	/**
	 * Toggles the state of the cell that was clicked on.
	 */
	private void toggleDisplayCell(MouseEvent event) {
		var g = canvas.getGraphicsContext2D();

		// Actual mouse click coordinates
		double x = event.getX();
		double y = event.getY();

		// Corresponding grid index
		int row = toRowIndex(y);
		int col = toColIndex(x);

		// Top-left coordinates of the cell
		double x0 = toXCoord(col);
		double y0 = toYCoord(row);

		if (model.get(row, col) == CellState.DEAD) {
			model.set(row, col, CellState.ALIVE);
			g.setFill(Color.BLACK);
			flavorText.setText("New life spontaneously emerges!");
		}
		else { // (model.get(row, col) != CellState.DEAD)
			model.set(row,  col, CellState.DEAD);
			g.setFill(Color.WHITE);
			flavorText.setText("The hand of God is cruel.");
		}

		g.fillRect(x0, y0, CELL_INTERIOR_SIZE, CELL_INTERIOR_SIZE);

	}

	/**
	 * Changes GraphicsContext objects fill color depending on provided CellState.
	 *
	 * @param g GraphicsContext
	 * @param state CellState
	 */
	private void decideColor(GraphicsContext g, CellState state) {
		switch(state) {
			case VAMPIRE:
				if (ILife.RANDOM.nextBoolean())
					g.setFill(Color.rgb(97, 22, 24));
				else
					g.setFill(Color.rgb(130, 20, 21));
				break;
			case ZOMBIE:
				if (ILife.RANDOM.nextBoolean())
					g.setFill(Color.rgb(49, 87, 44));
				else
					g.setFill(Color.rgb(79, 119, 45));
				break;
			case ALIVE:
				g.setFill(Color.BLACK);
				break;
			default: // DEAD
				g.setFill(Color.WHITE);
				break;
		}
	}
}
