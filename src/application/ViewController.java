package application;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.SequencedMap;

import application.component.SliderBox;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Slider;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
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
	 * For convenience, we hold a handle to each major component in the scene graph,
	 * even though some are currently not used directly.
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
	@FXML private BorderPane root;

	// top stuff
	@FXML private HBox topBox;
	@FXML private Text titleText;

	// center stuff
	@FXML private ScrollPane centerPane;
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
	@FXML private SliderBox tpsControl;
	@FXML private ComboBox<String> modelCBox;

	// tentative
	@FXML private SliderBox nrowsControl;
	@FXML private SliderBox ncolsControl;
	@FXML private SliderBox cellSizeControl;

	// ==================
	// Toolbar stuff
	// ==================
	@FXML private ToolBar toolbar;

	// --- Color Menu stuff ---
	@FXML private TilePane primaryPaletteBox;
	@FXML private TilePane extendedPaletteBox;
	private static final double COLOR_TILE_SIZE = 30;

	// https://stackoverflow.com/a/4382138
	private static final SequencedMap<String, Color> PRIMARY_PALETTE;
	private static final SequencedMap<Color, String> KELLY_COLORS;

	static {
		var map = new LinkedHashMap<String, Color>();

		map.put("Black", Color.BLACK);
		map.put("Blue", Color.BLUE);
		map.put("Red", Color.RED);
		map.put("Green", Color.GREEN);
		// normal yellow is a bit too bright on a white background
		map.put("Yellow", Color.YELLOW.darker());
		map.put("Magenta", Color.MAGENTA);
		map.put("Pink", Color.DEEPPINK);
		map.put("Gray", Color.GRAY);
		map.put("Brown", Color.BROWN);
		map.put("Orange", Color.ORANGE);

		PRIMARY_PALETTE = Collections.unmodifiableSequencedMap(map);
	}

	static {
		var map = new LinkedHashMap<Color, String>();

		map.put(Color.web("#FFB300"), "Vivid Yellow");
		map.put(Color.web("#803E75"), "Strong Purple");
		map.put(Color.web("#FF6800"), "Vivid Orange");
		map.put(Color.web("#A6BDD7"), "Very Light Blue");
		map.put(Color.web("#C10020"), "Vivid Red");
		map.put(Color.web("#CEA262"), "Grayish Yellow");
		map.put(Color.web("#817066"), "Medium Gray");
		map.put(Color.web("#007D34"), "Vivid Green");
		map.put(Color.web("#F6768E"), "Strong Purplish Pink");
		map.put(Color.web("#00538A"), "Strong Blue");
		map.put(Color.web("#FF7A5C"), "Strong Yellowish Pink");
		map.put(Color.web("#53377A"), "Strong Violet");
		map.put(Color.web("#FF8E00"), "Vivid Orange Yellow");
		map.put(Color.web("#B32851"), "Strong Purplish Red");
		map.put(Color.web("#F4C800"), "Vivid Greenish Yellow");
		map.put(Color.web("#7F180D"), "Strong Reddish Brown");
		map.put(Color.web("#93AA00"), "Vivid Yellowish Green");
		map.put(Color.web("#593315"), "Deep Yellowish Brown");
		map.put(Color.web("#F13A13"), "Vivid Reddish Orange");
		map.put(Color.web("#232C16"), "Dark Olive Green");

		KELLY_COLORS = Collections.unmodifiableSequencedMap(map);
	}

	@FXML private Button styleEditorButton;

	// ==================
	// Grid/Canvas stuff
	// ==================
	private Color colorOfLife = Color.BLACK;

	private double canvasWidth;
	private double canvasHeight;

	private int ncols;
	private int nrows;

	private int cellSize;
	private int cellInteriorSize;

	private static final int CELL_BORDER_WIDTH = 1;

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
	 */
	public void initialize() {
		debugText.setText("cell size: " + cellSize);
		initCanvasGrid();
		initButtonHandlers();
		initTpsControls();
		initModelSelectorBox();
		initGridSizeControls();

		Platform.runLater(() -> {
			var scene = root.getScene();

			var originShortcut = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN);
			scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
				if (originShortcut.match(e)) {
					debugText.setText("Return to origin");
					centerPane.setHvalue(0.5);
					centerPane.setVvalue(0.5);
				}
			});
		});

		initToolBar();
	}

	private void initToolBar() {
		for (var item : toolbar.getItems()) {
			if (item instanceof Labeled labeled)
				addTooltip(labeled, labeled.getText());

			if (item instanceof Button button)
				button.setOnAction(e -> {
					debugText.setText("'" + button.getId() + "' not implemented yet!");
				});

		}

		initColorMenu();
		initLiveStyleEditor();
	}

	private void initLiveStyleEditor() {
		var stylesheet = this.getClass().getResource("styles.css");
		var editor = new LiveStyleEditor(root, stylesheet);

		styleEditorButton.setOnAction(e -> {
			boolean success = editor.launch();

			if (!success) {
				styleEditorButton.setDisable(true);
				debugText.setText("Unable to launch style editor");
			}
		});
	}

	private void initColorMenu() {
		for (var entry : PRIMARY_PALETTE.entrySet()) {
			String name = entry.getKey();
			Color color = entry.getValue();
			primaryPaletteBox.getChildren().add(newColorTile(name, color, COLOR_TILE_SIZE));
		}

		for (var entry : KELLY_COLORS.entrySet()) {
			Color color = entry.getKey();
			String desc = entry.getValue();
			extendedPaletteBox.getChildren().add(newColorTile(desc, color, COLOR_TILE_SIZE));
		}
	}

	private void addTooltip(Node node, String text) {
		var tip = new Tooltip(text);
		// make tooltip show up faster (default delay is 1000ms)
		tip.setShowDelay(javafx.util.Duration.millis(200));

		if (node instanceof Control control)
			control.setTooltip(tip);
		else
			Tooltip.install(node, tip);
	}

	private Node newColorTile(String descriptor, Color color, double size) {
		var tile = new Rectangle(size, size, color);
		tile.getStyleClass().add("color-tile");

		tile.setOnMouseClicked(e -> {
			if (colorOfLife.equals(color))
				return;

			debugText.setText("Changed color to: " + descriptor);
			colorOfLife = color;
			redrawGrid();
		});

		addTooltip(tile, descriptor);
		return tile;
	}

	private void initGridSizeControls() {
		ncolsControl.slider.valueProperty().addListener((ov, oldValue, newValue) -> {
			ncols = newValue.intValue();
			resizeGrid();
		});

		nrowsControl.slider.valueProperty().addListener((ov, oldValue, newValue) -> {
			nrows = newValue.intValue();
			resizeGrid();
		});

		cellSizeControl.slider.valueProperty().addListener((ov, oldValue, newValue) -> {
			cellSize = newValue.intValue();
			cellInteriorSize = cellSize - 2*CELL_BORDER_WIDTH;
			resizeGrid();
		});

		ncols = ncolsControl.getValue();
		nrows = nrowsControl.getValue();
		cellSize = cellSizeControl.getValue();
		cellInteriorSize = cellSize - 2*CELL_BORDER_WIDTH;
		resizeGrid();
	}

	private void initTpsControls() {
		tpsControl.slider.valueProperty().addListener((ov, oldValue, newValue) -> {
			ticksPerSecond = newValue.intValue();

			if (isPlaying && ticksPerSecond > 30)
				flavorText.setText("Chaos!");
		});

		// See FXML for initial value.
		ticksPerSecond = tpsControl.getValue();
	}

	private void initModelSelectorBox() {
		var items = modelCBox.getItems();
		var lookupTable = new HashMap<String, Class<? extends ILife>>();

		/*
		 * Add the name of each concrete model class to the combo box,
		 * and link the name to the actual class object in a map.
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
	private void initCanvasGrid() {
		// Bind canvas container dimensions to the canvas dimensions.
		centerPane.maxWidthProperty().bind(canvas.widthProperty());
		centerPane.maxHeightProperty().bind(canvas.heightProperty());
		resizeGrid();

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
		return Math.min((int) (y / cellSize), nrows - 1);
	}

	/** Convert from x-coordinate to column index, rounding down */
	private int toColIndex(double x) {
		return Math.min((int) (x / cellSize), ncols - 1);
	}

	// TODO: check edge cases of coordinate-to-index conversions (rounding errors?)

	/** Convert from row index to y-coordinate of the top-left of cell interior */
	private double toYCoord(int row) {
		return CELL_BORDER_WIDTH + row*cellSize;
	}

	/** Convert from column index to x-coordinate of the top-left of cell interior */
	private double toXCoord(int col) {
		return CELL_BORDER_WIDTH + col*cellSize;
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
		g.fillRect(x0, y0, cellInteriorSize, cellInteriorSize);
	};

	/**
	 * Resizes the grid. Currently, this also clears the grid, but it technically
	 * doesn't have to.
	 */
	private void resizeGrid() {
		canvas.setWidth(canvasWidth = ncols * cellSize);
		canvas.setHeight(canvasHeight = nrows * cellSize);
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
		for (int x = 0; x < canvasWidth; x += cellSize)
			g.strokeLine(x, 0, x, canvasHeight);

		// Draw horizontal grid lines
		for (int y = 0; y < canvasHeight; y += cellSize)
			g.strokeLine(0, y, canvasWidth, y);

		// Fill in cells which are alive according to the model
		model.forAllLife((row, col, state) -> {
			decideColor(g, state);

			double x0 = toXCoord(col);
			double y0 = toYCoord(row);
			g.fillRect(x0, y0, cellInteriorSize, cellInteriorSize);
		});

		// Draw origin lines. `CTRL+O` to re-center scroll pane.
		g.setLineWidth(2*CELL_BORDER_WIDTH);
		int halfX = ncols / 2 * cellSize;
		int halfY = nrows / 2 * cellSize;
		g.setStroke(Color.GRAY);  // a little darker than normal grid lines
		g.strokeLine(halfX, 0, halfX, canvasHeight);
		g.strokeLine(0, halfY, canvasWidth, halfY);
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
			g.setFill(colorOfLife);
			flavorText.setText("New life spontaneously emerges!");
		}
		else { // (model.get(row, col) != CellState.DEAD)
			model.set(row,  col, CellState.DEAD);
			g.setFill(Color.WHITE);
			flavorText.setText("The hand of God is cruel.");
		}

		g.fillRect(x0, y0, cellInteriorSize, cellInteriorSize);

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
				g.setFill(colorOfLife);
				break;
			default: // DEAD
				g.setFill(Color.WHITE);
				break;
		}
	}
}
