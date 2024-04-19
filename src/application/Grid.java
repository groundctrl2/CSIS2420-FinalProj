package application;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import model.CellState;
import model.ILife;

/**
 * Abstract base class for rectangular grids.
 *
 * @see Grid#Classic
 */
abstract class Grid {
	final ViewController masterControl;
	final Canvas canvas;
	final GraphicsContext graphics;

	Color primaryColor = Color.BLACK;
	static final Color TILE_BORDER_COLOR = Color.LIGHTGRAY;
	static final Color AXIS_COLOR = Color.GRAY;

	protected int nrows;
	protected int ncols;

	protected int cellSize;
	protected int cellInteriorSize;

	protected static final int CELL_BORDER_WIDTH = 1;

	protected Grid(ViewController masterControl, Canvas canvas) {
		this.masterControl = masterControl;
		this.canvas = canvas;
		this.graphics = canvas.getGraphicsContext2D();
		// Enable click-to-toggle functionality.
		canvas.setOnMouseClicked(this::toggleDisplayCell);
	}

	abstract int[] toRowColIndex(double x, double y);

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

	abstract protected void resizeCanvas();

	final void resize() {
		resizeCanvas();
		masterControl.recenterCanvas();
		masterControl.resizeModel();
		redraw();
	}

	abstract void redraw();

	abstract void toggleDisplayCell(MouseEvent event);

	abstract void setDisplayCell(int row, int col, CellState state);

	/**
	 * Returns a custom color depending on provided CellState.
	 *
	 * @param state CellState
	 * @return a custom, state-dependent color for drawing a cell
	 */
	protected Color decideColor(CellState state) {
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

	/**
	 * The classic rectangular grid, with <em>square</em> tiles.
	 */
	static class Classic extends Grid {
		Classic(ViewController masterControl, Canvas canvas) {
			super(masterControl, canvas);
		}

		// For use by controller
		@Override
		int[] toRowColIndex(double x, double y) {
			return new int[] { toRowIndex(y), toColIndex(x) };
		}

		/** Convert from y-coordinate to row index, rounding down */
		private int toRowIndex(double y) {
			return Math.min((int) (y / cellSize), nrows - 1);
		}

		/** Convert from x-coordinate to column index, rounding down */
		private int toColIndex(double x) {
			return Math.min((int) (x / cellSize), ncols - 1);
		}

		/** Convert from row index to y-coordinate of the top-left of cell interior */
		private double toYCoord(int row) {
			return CELL_BORDER_WIDTH + row*cellSize;
		}

		/** Convert from column index to x-coordinate of the top-left of cell interior */
		private double toXCoord(int col) {
			return CELL_BORDER_WIDTH + col*cellSize;
		}

		/**
		 * Resizes and redraws the grid. Currently, this also clears the grid, but it
		 * technically doesn't have to.
		 */
		@Override
		protected void resizeCanvas() {
			canvas.setWidth(ncols * cellSize);
			canvas.setHeight(nrows * cellSize);
		}

		/**
		 * Redraws the whole grid by querying the model for the state of each
		 * living cell.
		 */
		@Override
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
			graphics.setStroke(TILE_BORDER_COLOR);
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

			// Draw axis lines a little darker (and maybe thicker?) than normal grid lines
			graphics.setLineWidth(2*CELL_BORDER_WIDTH);
			graphics.setStroke(AXIS_COLOR);
			int halfX = ncols / 2 * cellSize;
			int halfY = nrows / 2 * cellSize;
			graphics.strokeLine(halfX, 0, halfX, height);
			graphics.strokeLine(0, halfY, width, halfY);
		}

		/**
		 * Toggles the state of the cell that was clicked on.
		 */
		@Override
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
		 * Callback for {@link model.ILife#step}.
		 * <p>
		 * This is passed (as a lambda) to the model so that it can notify the
		 * controller whenever a cell changes state, allowing the canvas/grid to be
		 * incrementally updated.
		 * <p>
		 * In theory, this should be more efficient than redrawing the whole grid on
		 * each step, but with GPUs and buffering and caches, maybe not.
		 */
		@Override
		void setDisplayCell(int row, int col, CellState state) {
			double x0 = toXCoord(col);
			double y0 = toYCoord(row);

			graphics.setFill(decideColor(state));
			graphics.fillRect(x0, y0, cellInteriorSize, cellInteriorSize);
		};
	}

	/**
	 * A rectangular grid with <em>hexagonal</em> tiles.
	 */
	static class Hex extends Grid {
		Hex(ViewController masterControl, Canvas canvas) {
			super(masterControl, canvas);

			// Disable click-to-toggle for now;
			// Need to figure out conversion from pixels to (row, col)
			canvas.setOnMouseClicked(null);
		}

		@Override
		int[] toRowColIndex(double x, double y) {
			// This doesn't work.
			int row = (int) (y / .75 / hexHeight());
			int col = (int) ((x / hexWidth()) - ((row & 1) + 1) * 0.5);
			return new int[] { row, col };
		}

		/** Convert from row index to y-coordinate of the top point of hex interior */
		private double toYCoord(int row, int col) {
			// If the rows weren't offset, then the tiles would overlap.
			// That's why the factor of 3/4 is included for the delta-y.
			return CELL_BORDER_WIDTH + row*(0.75)*hexHeight();
		}

		/** Convert from column index to x-coordinate of the top point of hex interior */
		private double toXCoord(int row, int col) {
			return CELL_BORDER_WIDTH + col*hexWidth() + rowOffset(row);
		}

		/**
		 * For even rows, the offset is half the width.
		 * For odd rows, the offset is the width.
		 */
		private double rowOffset(int row) {
			return ((row & 1) + 1) * 0.5 * hexWidth();
		}

		private double hexHeight() {
			return cellSize;
		}

		private double hexInteriorHeight() {
			return cellInteriorSize;
		}

		private double hexWidth() {
			return Math.sqrt(3) / 2 * cellSize;
		}

		private double hexInteriorWidth() {
			return Math.sqrt(3) / 2 * cellInteriorSize;
		}

		private void drawHexTile(int row, int col, Paint interiorFill) {
			// Assume we're drawing the hexagon in "pointy-top" orientation.
			// (x0, y0) is the coordinate of the top point.
			double x0 = toXCoord(row, col);
			double y0 = toYCoord(row, col);

			double dx = hexInteriorWidth() / 2;
			double dy = hexInteriorHeight() / 4;

			double[] xs = {x0, x0 + dx, x0 + dx, x0, x0 - dx, x0 - dx};
			double[] ys = {y0, y0 + dy, y0 + 3*dy, y0 + 4*dy, y0 + 3*dy, y0 + dy};

			graphics.setStroke(TILE_BORDER_COLOR);
			graphics.setLineWidth(3*CELL_BORDER_WIDTH);
			graphics.strokePolygon(xs, ys, 6);

			graphics.setFill(interiorFill);
			graphics.fillPolygon(xs, ys, 6);
		}

		/**
		 * Resizes and redraws the grid. Currently, this also clears the grid, but it
		 * technically doesn't have to.
		 */
		@Override
		protected void resizeCanvas() {
			canvas.setWidth((ncols + 0.5) * hexWidth());
			canvas.setHeight((0.75 * nrows + 1) * hexHeight());
		}

		/**
		 * Redraws the whole grid by querying the model for the state of each
		 * living cell.
		 */
		@Override
		void redraw() {
			double width = canvas.getWidth();
			double height = canvas.getHeight();

			graphics.setFill(masterControl.getRootBackgroundColor());
			graphics.fillRect(0, 0, width, height);

			var model = masterControl.getModel();

			for (int row = 0; row < nrows; row++)
				for (int col = 0; col < ncols; col++)
					drawHexTile(row, col, decideColor(model.get(row, col)));
		}

		/**
		 * Toggles the state of the cell that was clicked on.
		 */
		@Override
		void toggleDisplayCell(MouseEvent event) {
			// Actual mouse click coordinates
			double x = event.getX();
			double y = event.getY();

			// Corresponding grid index
			int[] index = toRowColIndex(x, y);
			int row = index[0];
			int col = index[1];

			var model = masterControl.getModel();

			if (model.get(row, col) == CellState.DEAD) {
				model.set(row, col, CellState.ALIVE);
				drawHexTile(row, col, primaryColor);
			}
			else { // (model.get(row, col) != CellState.DEAD)
				model.set(row,  col, CellState.DEAD);
				drawHexTile(row, col, Color.WHITE);
			}
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
		@Override
		void setDisplayCell(int row, int col, CellState state) {
			drawHexTile(row, col, decideColor(state));
		};
	}
}
