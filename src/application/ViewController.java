package application;

import static javafx.scene.input.KeyCombination.keyCombination;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;

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
	@FXML private HBox root;
	@FXML private BorderPane mainArea;

	// top stuff
	@FXML private HBox topBox;
	@FXML private Text titleText;

	// center stuff
	@FXML private ScrollPane centerPane;
	@FXML private Canvas canvas;

	// bottom stuff
	@FXML private Button clearButton;
	@FXML private Button randomButton;
	@FXML private Button pausePlayButton;
	@FXML private Button stepButton;

	// sidebar stuff
	@FXML private VBox sidebar;
	@FXML private SpinnerBox tpsControl;
	@FXML private SpinnerBox cellSizeControl;
	@FXML private SpinnerBox nrowsControl;
	@FXML private SpinnerBox ncolsControl;
	@FXML private ComboBox<String> gridDimensionsComboBox;

	@FXML private ToggleGroup gridToggleGroup;
	@FXML private RadioButton classicRadioButton;
	@FXML private RadioButton hexRadioButton;

	@FXML private ComboBox<String> modelCBox;
	@FXML private Text modelInfo;
	@FXML private Text debugText;

	@FXML private ColorPicker colorPicker;
	@FXML private Button styleEditorButton;

	// ==================
	// Grid/Canvas stuff
	// ==================
	private Grid grid;

	// handle for the implementation of the simulation itself
	private ILife model = new model.GraphLife();

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
		initSidebar();

		// By default the sidebar is hidden.
		// `T` or `CTRL+T` to toggle it.
		sidebar.setVisible(false);
		sidebar.setManaged(false);

		// `CTRL+D` to toggle debug text
		debugText.setVisible(false);
		debugText.setManaged(false);

		// The scene isn't set until after initialization, so run later.
		Platform.runLater(this::installHotkeys);
	}

	/**
	 * Initializes the canvas.
	 */
	private void initCanvasAndGrid() {
		// Bind canvas container dimensions to the canvas dimensions.
		centerPane.maxWidthProperty().bind(canvas.widthProperty());
		centerPane.maxHeightProperty().bind(canvas.heightProperty());
		// Not sure if this helps with the edges?
		centerPane.prefViewportWidthProperty().bind(canvas.widthProperty());
		centerPane.prefViewportHeightProperty().bind(canvas.heightProperty());

		// subscribe() will also immediately fire and init the grid
		gridToggleGroup.selectedToggleProperty().subscribe(this::setGrid);

		// For debugging. TODO: delete this
		canvas.setOnMouseMoved(event -> {
			// Displaying step count takes precedence over the mouse position
			// during simulation or at the end of a simulation that stalls.
			if (isPlaying || restart)
				return;

			int x = (int) event.getX();
			int y = (int) event.getY();
			int[] index = grid.toRowColIndex(x, y);
			int r = index[0] + 1; // use 1-based indexing for display
			int c = index[1] + 1; // use 1-based indexing for display
			debugText.setText("pos: (%d, %d), cell: [%d, %d]".formatted(x, y, r, c));
		});
	}

	private void setGrid(Toggle selectedToggle) {
		if (selectedToggle == classicRadioButton) {
			grid = new Grid.Classic(this, canvas, centerPane);
			centerPane.getStyleClass().remove("hex-mode");
		}
		else {
			grid = new Grid.Hex(this, canvas, centerPane);
			centerPane.getStyleClass().add("hex-mode");
		}

		// Set initial values of the grid to the initial values in the controls.
		int nrows = nrowsControl.spinner.getValue();
		int ncols = ncolsControl.spinner.getValue();
		int cellSize = cellSizeControl.spinner.getValue();
		grid.setSize(nrows, ncols, cellSize);
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

	private void initSidebar() {
		initGridSizeControls();
		initTpsControls();
		initModelSelectorBox();
		initColorMenu();
		initLiveStyleEditor();
	}

	private void initTpsControls() {
		tpsControl.subscribe(newValue -> {
			ticksPerSecond = newValue;
		});

		// See FXML for initial value.
		ticksPerSecond = tpsControl.spinner.getValue();
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

		// Note: avoid temptation to use method reference here because `grid` can change
		cellSizeControl.subscribe(newValue -> {
			grid.setCellSize(newValue);
		});

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

	private void initModelSelectorBox() {
		var table = new LinkedHashMap<String, Class<? extends ILife>>();

		table.put("GraphLife", model.GraphLife.class);
		table.put("SimpleLife", model.SimpleLife.class);
		table.put("HexLife", model.HexLife.class);
		table.put("KnightLife", model.KnightLife.class);
		table.put("LifeInColor", model.LifeInColor.class);
		table.put("RockPaperScissorLife", model.RockPaperScissorLife.class);
		table.put("VampireLife", model.VampireLife.class);
		table.put("ZombieLife", model.ZombieLife.class);
		table.put("AmoebaLife", model.AmoebaLife.class);

		var items = modelCBox.getItems();

		for (var name : table.keySet())
			items.add(name);

		// Set the current value to the current model's class.
		String currentSelection = model.getClass().getSimpleName();
		assert table.containsKey(currentSelection): currentSelection;
		modelCBox.setValue(currentSelection);

		// Update the model whenever the combo box value changes.
		modelCBox.setOnAction(event -> {
			var className = modelCBox.getValue();
			var selectedClass = table.get(className);

			if (selectedClass.equals(model.getClass())) {
				debugText.setText("No change");
				return;
			}

			try {
				model = selectedClass.getConstructor().newInstance();

				var desc = model.description();

				if (desc != null)
					modelInfo.setText(desc);

			} catch (Exception e) {
				e.printStackTrace();
			}

			resizeModel();
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

		var tip = new Tooltip("Select color");
		tip.setShowDelay(javafx.util.Duration.millis(200));
		colorPicker.setTooltip(tip);
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

		var tip = new Tooltip(styleEditorButton.getText());
		tip.setShowDelay(javafx.util.Duration.millis(200));
		styleEditorButton.setTooltip(tip);

	}

	private void installHotkeys() {
		var acc = root.getScene().getAccelerators();

		acc.put(keyCombination("Shortcut+P"), pausePlayButton::requestFocus);
		acc.put(keyCombination("P"), pausePlayButton::fire);
		// acc.put(keyCombination("Shift+Comma"), backButton::fire);  // '<'
		// acc.put(keyCombination("B"), backButton::fire);
		acc.put(keyCombination("Shift+Period"), stepButton::fire);  // '>'
		acc.put(keyCombination("F"), stepButton::fire);
		acc.put(keyCombination("Shift+C"), clearButton::fire);
		acc.put(keyCombination("Shift+R"), randomButton::fire);

		acc.put(keyCombination("Ignore Shortcut+Close Bracket"), () -> {  // ']'
			tpsControl.setValue(ticksPerSecond + 1);
		});

		acc.put(keyCombination("Ignore Shortcut+Open Bracket"), () -> {  // '['
			tpsControl.setValue(ticksPerSecond - 1);
		});

		acc.put(keyCombination("Ignore Shortcut+Equals"), () -> {
			cellSizeControl.spinner.increment();
		});

		acc.put(keyCombination("Ignore Shortcut+Minus"), () -> {
			cellSizeControl.spinner.decrement();
		});

		acc.put(keyCombination("Shortcut+O"), () -> {
			debugText.setText("Return to origin");
			recenterCanvas();
		});

		acc.put(keyCombination("Ignore Shortcut+T"), () -> {
			sidebar.setManaged(!sidebar.isManaged());
			sidebar.setVisible(!sidebar.isVisible());
		});

		acc.put(keyCombination("Shortcut+D"), () -> {
			debugText.setManaged(!debugText.isManaged());
			debugText.setVisible(!debugText.isVisible());
		});
	}
}
