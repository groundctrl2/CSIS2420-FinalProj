package application;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import model.CellState;
import model.ILife;

class ClassicGrid {
	final ViewController masterControl;
	final Canvas canvas;
	final GraphicsContext graphics;

	Color primaryColor = Color.BLACK;

	private int nrows;
	private int ncols;

	private int cellSize;
	private int cellInteriorSize;

	private static final int CELL_BORDER_WIDTH = 1;

	ClassicGrid(ViewController masterControl, Canvas canvas) {
		this.masterControl = masterControl;
		this.canvas = canvas;
		this.graphics = canvas.getGraphicsContext2D();
		// Enable click-to-toggle functionality.
		canvas.setOnMouseClicked(this::toggleDisplayCell);
	}

	/** Convert from y-coordinate to row index, rounding down */
	int toRowIndex(double y) {
		return Math.min((int) (y / cellSize), nrows - 1);
	}

	/** Convert from x-coordinate to column index, rounding down */
	int toColIndex(double x) {
		return Math.min((int) (x / cellSize), ncols - 1);
	}

	// TODO: check edge cases of coordinate-to-index conversions (rounding errors?)

	/** Convert from row index to y-coordinate of the top-left of cell interior */
	double toYCoord(int row) {
		return CELL_BORDER_WIDTH + row*cellSize;
	}

	/** Convert from column index to x-coordinate of the top-left of cell interior */
	double toXCoord(int col) {
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
	void setDisplayCell(int row, int col, CellState state) {
		double x0 = toXCoord(col);
		double y0 = toYCoord(row);

		graphics.setFill(decideColor(state));
		graphics.fillRect(x0, y0, cellInteriorSize, cellInteriorSize);
	};

	int nrows() { return nrows; }  // corresponding setter below
	int ncols() { return ncols; }  // corresponding setter below

	void setNumRows(int nrows) {
		setDimensions(nrows, this.ncols);
	}

	void setNumCols(int ncols) {
		setDimensions(this.nrows, ncols);
	}

	void setDimensions(int nrows, int ncols) {
		setSize(nrows, ncols, this.cellSize);
	}

	void setSize(int nrows, int ncols, int cellSize) {
		this.nrows = nrows;
		this.ncols = ncols;
		this.setCellSize(cellSize);
	}

	void setCellSize(int cellSize) {
		this.cellSize = cellSize;
		this.cellInteriorSize = cellSize - 2*CELL_BORDER_WIDTH;
		resize();
	}

	/**
	 * Resizes and redraws the grid. Currently, this also clears the grid, but it
	 * technically doesn't have to.
	 */
	private void resize() {
		canvas.setWidth(ncols * cellSize);
		canvas.setHeight(nrows * cellSize);
		masterControl.recenterCanvas();
		masterControl.resizeModel();
		redraw();
	}

	/**
	 * Redraws the whole grid by querying the model for the state of each
	 * living cell.
	 */
	void redraw() {
		double width = canvas.getWidth();
		double height = canvas.getHeight();
		/*
		 * We could render each cell by using fillRect() followed by strokeRect() for
		 * the cell borders. Alternatively, we can draw all the borders as grid lines
		 * over the whole canvas, and then fill in the cell interiors. We currently,
		 * take the second approach below.
		 */
		graphics.setFill(Color.WHITE);
		graphics.fillRect(0, 0, width, height);
		graphics.setStroke(Color.LIGHTGRAY);
		graphics.setLineWidth(2*CELL_BORDER_WIDTH);

		// Draw vertical grid lines
		for (int x = 0; x < width; x += cellSize)
			graphics.strokeLine(x, 0, x, height);

		// Draw horizontal grid lines
		for (int y = 0; y < height; y += cellSize)
			graphics.strokeLine(0, y, width, y);

		// Fill in cells which are alive according to the model
		masterControl.getModel().forAllLife((row, col, state) -> {
			graphics.setFill(decideColor(state));

			double x0 = toXCoord(col);
			double y0 = toYCoord(row);
			graphics.fillRect(x0, y0, cellInteriorSize, cellInteriorSize);
		});

		// Draw origin lines a little darker (and maybe thicker?) than normal grid lines
		graphics.setLineWidth(2*CELL_BORDER_WIDTH);
		graphics.setStroke(Color.GRAY);
		int halfX = ncols / 2 * cellSize;
		int halfY = nrows / 2 * cellSize;
		graphics.strokeLine(halfX, 0, halfX, height);
		graphics.strokeLine(0, halfY, width, halfY);
	}

	/**
	 * Toggles the state of the cell that was clicked on.
	 */
	void toggleDisplayCell(MouseEvent event) {
		// Actual mouse click coordinates
		double x = event.getX();
		double y = event.getY();

		// Corresponding grid index
		int row = toRowIndex(y);
		int col = toColIndex(x);

		// Top-left coordinates of the cell
		double x0 = toXCoord(col);
		double y0 = toYCoord(row);

		var model = masterControl.getModel();

		if (model.get(row, col) == CellState.DEAD) {
			model.set(row, col, CellState.ALIVE);
			graphics.setFill(primaryColor);
		}
		else { // (model.get(row, col) != CellState.DEAD)
			model.set(row,  col, CellState.DEAD);
			graphics.setFill(Color.WHITE);
		}

		graphics.fillRect(x0, y0, cellInteriorSize, cellInteriorSize);
	}

	/**
	 * Returns a custom color depending on provided CellState.
	 *
	 * @param state CellState
	 * @return a custom, state-dependent color for drawing a cell
	 */
	private Color decideColor(CellState state) {
		switch(state) {
			case BLUE:
				if (ILife.RANDOM.nextBoolean())
					return Color.rgb(50, 90, 130);
				else
					return Color.rgb(50, 70, 160);
			case RED:
				if (ILife.RANDOM.nextBoolean())
					return Color.rgb(180, 0, 0);
				else
					return Color.rgb(210, 20, 0);
			case GREEN:
				if (ILife.RANDOM.nextBoolean())
					return Color.rgb(80, 130, 0);
				else
					return Color.rgb(100, 160, 0);
			case ALIVE:
				return primaryColor;
			default: // DEAD
				return Color.WHITE;
		}
	}
}
