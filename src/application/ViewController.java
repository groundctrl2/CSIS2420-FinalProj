package application;

import static javafx.scene.input.KeyCombination.keyCombination;

import java.time.Duration;
import java.util.HashMap;

import application.component.LiveStyleEditor;
import application.component.SpinnerBox;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
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
	@FXML private HBox mainControlBar;
	@FXML private Button clearButton;
	@FXML private Button randomButton;
	@FXML private Button pausePlayButton;
	@FXML private Button stepButton;
	@FXML private Text debugText;

	@FXML private HBox secondaryControls;
	@FXML private SpinnerBox tpsControl;
	@FXML private ComboBox<String> modelCBox;
	@FXML private SpinnerBox nrowsControl;
	@FXML private SpinnerBox ncolsControl;
	@FXML private SpinnerBox cellSizeControl;
	@FXML private ComboBox<String> gridDimensionsComboBox;

	// ==================
	// Toolbar stuff
	// ==================
	@FXML private ToolBar toolbar;
	@FXML private ColorPicker colorPicker;
	@FXML private Button styleEditorButton;

	// ==================
	// Grid/Canvas stuff
	// ==================
	private ClassicGrid grid;

	// handle for the implementation of the simulation itself
	private ILife model = new model.VampireLife();

	// for access from the grid
	public ILife getModel() { return model; }

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
		initCanvasAndGrid();
		initButtonHandlers();
		initTpsControls();
		initModelSelectorBox();
		initGridSizeControls();
		initToolBar();

		// The scene isn't set until after initialization, so run later.
		Platform.runLater(this::installHotkeys);
	}

	private void installHotkeys() {
		var acc = root.getScene().getAccelerators();

		acc.put(keyCombination("Shortcut+P"), pausePlayButton::requestFocus);
		acc.put(keyCombination("P"), pausePlayButton::fire);
		acc.put(keyCombination("PLAY"), pausePlayButton::fire);
		acc.put(keyCombination("PAUSE"), pausePlayButton::fire);
		// acc.put(keyCombination("Shift+Comma"), backButton::fire);  // '<'
		// acc.put(keyCombination("b"), backButton::fire);
		acc.put(keyCombination("Shift+Period"), stepButton::fire);  // '>'
		acc.put(keyCombination("F"), stepButton::fire);
		acc.put(keyCombination("Shift+C"), clearButton::fire);
		acc.put(keyCombination("Shift+R"), randomButton::fire);

		acc.put(keyCombination("Close Bracket"), () -> {  // ']'
			tpsControl.setValue(ticksPerSecond + 1);
		});

		acc.put(keyCombination("Open Bracket"), () -> {  // '['
			tpsControl.setValue(ticksPerSecond - 1);
		});

		acc.put(keyCombination("Ignore Shortcut+Equals"), () -> {
			cellSizeControl.spinner.increment();
		});

		acc.put(keyCombination("Ignore Shortcut+Minus"), () -> {
			cellSizeControl.spinner.decrement();
		});

		acc.put(keyCombination("Shortcut+o"), () -> {
			debugText.setText("Return to origin");
			recenterCanvas();
		});

		acc.put(keyCombination("Shortcut+t"), () -> {
			toolbar.setManaged(!toolbar.isManaged());
			toolbar.setVisible(!toolbar.isVisible());
		});
	}

	private void initToolBar() {
		for (var item : toolbar.getItems()) {
			if (item instanceof Labeled labeled) {
				var tip = new Tooltip(labeled.getText());
				tip.setShowDelay(javafx.util.Duration.millis(200));
				labeled.setTooltip(tip);
			}

			if (item instanceof Button button) {
				button.setOnAction(e -> {
					debugText.setText("'" + button.getId() + "' not implemented yet!");
				});
			}
		}

		initColorMenu();
		initLiveStyleEditor();

		// Hide toolbar while it's unfinished.
		toolbar.setVisible(false);
		toolbar.setManaged(false);
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
		assert grid != null: "must call initCanvasAndGrid() first";

		if (grid.primaryColor == null)
			colorPicker.setValue(Color.BLACK);
		else
			colorPicker.setValue(grid.primaryColor);

		// update canvas color on color selection
		colorPicker.setOnAction(e -> {
			var color = colorPicker.getValue();

			if (grid.primaryColor.equals(color))
				return;  // avoid unnecessary redraw

			grid.primaryColor = color;
			grid.redraw();
		});
	}

	private void initGridSizeControls() {
		assert grid != null: "must call initCanvasAndGrid() first";

		nrowsControl.subscribe(newValue -> {
			grid.setNumRows(newValue);
			gridDimensionsComboBox.setValue(newValue + "x" + ncolsControl.getValue());
		});

		ncolsControl.subscribe(newValue -> {
			grid.setNumCols(newValue);
			gridDimensionsComboBox.setValue(nrowsControl.getValue() + "x" + newValue);
		});

		cellSizeControl.subscribe(grid::setCellSize);

		/* Note the order of initialization here. The value must be set before
		 * the update/change listener is registered on the combo-box in order
		 * to prevent double initialization of the grid.
		 */
		gridDimensionsComboBox.setValue(nrowsControl.getValue() + "x" + ncolsControl.getValue());

		gridDimensionsComboBox.setOnAction(e -> {
			String dimensions = gridDimensionsComboBox.getValue();
			var a = dimensions.split("x");
			//
			int nrows_ = Integer.valueOf(a[0]);
			int ncols_ = Integer.valueOf(a[1]);
			// By updating the individual controls, the grid will also be
			// automatically updated.
			nrowsControl.setValue(nrows_);
			ncolsControl.setValue(ncols_);
		});
	}

	private void initTpsControls() {
		tpsControl.subscribe(newValue -> {
			ticksPerSecond = newValue;
		});

		// See FXML for initial value.
		ticksPerSecond = tpsControl.spinner.getValue();
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
	private void initCanvasAndGrid() {
		// Bind canvas container dimensions to the canvas dimensions.
		centerPane.maxWidthProperty().bind(canvas.widthProperty());
		centerPane.maxHeightProperty().bind(canvas.heightProperty());

		grid = new ClassicGrid(this, canvas);
		// Set initial values of the grid to the initial values in the controls.
		int nrows = nrowsControl.spinner.getValue();
		int ncols = ncolsControl.spinner.getValue();
		int cellSize = cellSizeControl.spinner.getValue();
		grid.setSize(nrows, ncols, cellSize);

		// For debugging. TODO: delete this
		canvas.setOnMouseMoved(event -> {
			// Displaying step count takes precedence over the mouse position
			// during simulation or at the end of a simulation that stalls.
			if (isPlaying || restart)
				return;

			int x = (int) event.getX();
			int y = (int) event.getY();
			int c = grid.toColIndex(x) + 1; // use 1-based indexing for display
			int r = grid.toRowIndex(y) + 1; // use 1-based indexing for display
			debugText.setText("pos: (%d, %d), cell: [%d, %d]".formatted(x, y, r, c));
		});
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
					reactToStep(model.step(grid::setDisplayCell));
					timestamp = now;
				}
			}
		};

		clearButton.setOnAction(event -> {
			resetAnimation();
			model.clear();
			grid.redraw();
			debugText.setText("Cleared");
		});

		randomButton.setOnAction(event -> {
			model.randomize();
			grid.redraw();
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

			isPlaying = !isPlaying;
		});

		stepButton.setOnAction(event -> {
			reactToStep(model.step(grid::setDisplayCell));
		});
	}

	/**
	 * Perform miscellaneous actions on each step. For now, this just examines
	 * the step count and also halts the animation if it stalls (although it
	 * currently does not detect loops/cycles.
	 */
	private void reactToStep(boolean change) {
		if (restart) {
			stepCount = 0;
			restart = false;
		}

		if (change) {
			stepCount++;
			debugText.setText("Step count: " + stepCount);
		}
		else {
			// Stop animating if the simulation stalls (reaches a fixed point).
			if (isPlaying)
				pausePlayButton.fire();

			// Reset the step count next time.
			restart = true;

			// TODO: detect cycles and react accordingly
			if (stepCount > 0)
				debugText.setText("No movement after " + stepCount + " steps");
		}

	}

	void resetAnimation() {
		// reset animation variables
		if (isPlaying)
			pausePlayButton.fire();

		assert !isPlaying;
		timestamp = 0;
		stepCount = 0;
		restart = false;
	}

	void recenterCanvas() {
		centerPane.setHvalue(0.5);
		centerPane.setVvalue(0.5);
	}

	void resizeModel() {
		model.resize(grid.nrows(), grid.ncols());
		resetAnimation();
		grid.redraw();
	}
}
