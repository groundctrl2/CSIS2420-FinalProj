package application.component;

import java.util.*;
import java.util.function.Consumer;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * A menu button that allows the user to select from a limited set of
 * distinct colors. Unlike {@link javafx.scene.control.ColorPicker},
 * this is more limited, less customizable, and does not allow the user
 * to choose their own color.
 * <p>
 * Designed to be instantiated in FXML with its constituent components
 * accessible to a controller.
 *
 * <p>
 * Component structure:
 * <pre>
 * MenuButton (this):
 *   items:
 *     - CustomMenuItem (colorMenu)
 *         style class: color-menu
 *         content:
 *           VBox:
 *             children:
 *               - Pane (primary palette box)
 *                   style class: primary-palette
 *               - Separator
 *               - Pane (extended palette box)
 *                   style class: extended-palette
 * </pre>
 */
public class ConstrainedColorPicker extends MenuButton {
	public final CustomMenuItem colorMenu = new CustomMenuItem();
	public final TilePane primaryPaletteBox = new TilePane();
	public final TilePane extendedPaletteBox = new TilePane();

	private static final double COLOR_TILE_SIZE = 30;

	private Color currentColor = PRIMARY_PALETTE.get("Black");
	private Rectangle icon;

	private Consumer<Color> onPickHandler = c -> { /* no-op */ };

	public ConstrainedColorPicker() {
		initPaletteBoxes();

		var container = new VBox();
		container.getChildren().add(primaryPaletteBox);
		container.getChildren().add(new Separator(Orientation.HORIZONTAL));
		container.getChildren().add(extendedPaletteBox);

		colorMenu.setContent(container);
		colorMenu.setHideOnClick(false);

		this.getItems().add(colorMenu);

		// Add style classes to major subcomponents.
		colorMenu.getStyleClass().add("color-menu");
		primaryPaletteBox.getStyleClass().addAll("primary-palette", "palette-box");
		primaryPaletteBox.getChildren().forEach(c -> c.getStyleClass().add("color-tile"));
		extendedPaletteBox.getStyleClass().addAll("extended-palette", "palette-box");
		extendedPaletteBox.getChildren().forEach(c -> c.getStyleClass().add("color-tile"));
		this.getStyleClass().add("constrained-color-picker");

		// Apply default component styles
		var stylesheet = this.getClass().getResource("component-styles.css");
		assert stylesheet != null;
		this.getStylesheets().add(stylesheet.toString());
	}

	public ConstrainedColorPicker withColorIcon() {
		icon = new Rectangle(16, 16, currentColor);
		return this;
	}

	public Color getColor() {
		return currentColor;
	}

	public void setOnPick(Consumer<Color> onPickHandler) {
		this.onPickHandler = Objects.requireNonNull(onPickHandler);
	}

	private void initPaletteBoxes() {
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

	private Node newColorTile(String descriptor, Color color, double size) {
		var tile = new Rectangle(size, size, color);

		tile.setOnMouseClicked(e -> {
			currentColor = color;
			onPickHandler.accept(color);

			if (icon != null)
				icon.setFill(color);
		});

		addTooltip(tile, descriptor);
		return tile;
	}

	private static void addTooltip(Node node, String text) {
		var tip = new Tooltip(text);
		// make tooltip show up faster (default delay is 1000ms)
		tip.setShowDelay(javafx.util.Duration.millis(200));

		if (node instanceof Control control)
			control.setTooltip(tip);
		else
			Tooltip.install(node, tip);
	}

	// =====================
	// Predefined palettes
	// =====================

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
}
