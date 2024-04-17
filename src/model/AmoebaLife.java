package model;

import java.util.Arrays;

import edu.princeton.cs.algs4.Graph;
import edu.princeton.cs.algs4.Queue;

public class AmoebaLife implements ILife {
	private Graph world;
	private CellState[] cells; // row-col indexed
	private int[][] amoebaInfo; // row-col indexed
	private int nrows;
	private int ncols;

	@Override
	public void resize(int nrows, int ncols) {
		this.world = new Graph(nrows * ncols);
		this.cells = new CellState[nrows * ncols];
		this.amoebaInfo = new int[nrows * ncols][3];
		this.nrows = nrows;
		this.ncols = ncols;

		clear();

		// Initialize edges/neighbors
		for (int current = 0; current < cells.length; current++)
			initializeNeighbors(current);

		// Initialize amoeba targets
		for (int i = 0; i < amoebaInfo.length; i++) {
			amoebaInfo[i][0] = 0; // Size
			amoebaInfo[i][1] = -1; // Target
			amoebaInfo[i][0] = 0; // Amount of steps spent chasing target.
		}
	}

	/**
	 * Returns index of cell based on row and col
	 *
	 * @param row
	 * @param col
	 * @return int cell index
	 */
	private int convertToIndex(int row, int col) {
		return row * ncols + col;
	}

	/**
	 * Returns cell's row based on index
	 *
	 * @param index
	 * @return int cell's row
	 */
	private int convertToRow(int index) {
		return index / ncols;
	}

	/**
	 * Returns cell's col based on index
	 *
	 * @param index
	 * @return int cell's col
	 */
	private int convertToCol(int index) {
		return index % ncols;
	}

	/**
	 * Adds neighbor edges to given cell.
	 *
	 * @param index
	 */
	private void initializeNeighbors(int index) {
		int row = convertToRow(index);
		int col = convertToCol(index);

		int[] rowOffsets = { (row - 1 + nrows) % nrows, row, (row + 1 + nrows) % nrows };
		int[] colOffsets = { (col - 1 + ncols) % ncols, col, (col + 1 + ncols) % ncols };

		for (int r : rowOffsets)
			for (int c : colOffsets)
				if (r != row || c != col) { // Disclude current cell
					int neighbor = convertToIndex(r, c);
					if (hasEdge(index, neighbor) != true)
						world.addEdge(index, neighbor);
				}
	}

	/**
	 * Checks if vertex already linked to neighbor.
	 *
	 * @param index
	 * @param neighbor
	 * @return boolean true/false linked to neighbor
	 */
	private boolean hasEdge(int index, int neighbor) {
		for (int n : world.adj(index))
			if (n == neighbor)
				return true;
		return false;
	}

	@Override
	public void clear() {
		Arrays.fill(cells, CellState.DEAD);
		amoebaInfo = new int[nrows * ncols][3];
	}

	@Override
	public void randomize() {
		clear();
		// 3 Nuclei
		for (int i = 0; i < 5; i++) {
			int randomInt = RANDOM.nextInt(nrows * ncols);
			cells[randomInt] = CellState.VAMPIRE;
			amoebaInfo[randomInt][0] = RANDOM.nextInt(14) + 1;
			setAmoebaSizing(randomInt);
		}
		// 1 Food
		cells[RANDOM.nextInt(nrows * ncols)] = CellState.ALIVE;
	}

	/**
	 * Sets the body cells depending on given nucleus's size. Designed to have cells
	 * overlap body cells, but not nucleus cells.
	 * 
	 * @param nucleus
	 */
	private void setAmoebaSizing(int nucleus) {
		int row = convertToRow(nucleus);
		int col = convertToCol(nucleus);

		// 1st size:
		if (amoebaInfo[nucleus][0] > 0) {
			int[] rowOffsets = { (row - 1 + nrows) % nrows, row, (row + 1 + nrows) % nrows };
			int[] colOffsets = { (col - 1 + ncols) % ncols, col, (col + 1 + ncols) % ncols };

			//Fill neighbors
			for (int r : rowOffsets)
				for (int c : colOffsets) {
					fillIfNotNucleus(r, c);
				}
		}
		// 2nd size:
		if (amoebaInfo[nucleus][0] > 6) {
			int[] rowOffsets = { (row - 2 + nrows) % nrows, (row - 1 + nrows) % nrows, row,
			    (row + 1 + nrows) % nrows, (row + 2 + nrows) % nrows };
			int[] colOffsets = { (col - 2 + ncols) % ncols, (col - 1 + ncols) % ncols, col,
			    (col + 1 + ncols) % ncols, (col + 2 + ncols) % ncols };

			// Fill outer sides
			for (int i = 1; i < 4; i++) {
				fillIfNotNucleus(rowOffsets[0], colOffsets[i]);
				fillIfNotNucleus(rowOffsets[i], colOffsets[0]);
				fillIfNotNucleus(rowOffsets[4], colOffsets[i]);
				fillIfNotNucleus(rowOffsets[i], colOffsets[4]);
			}
		}
		// 3rd size:
		if (amoebaInfo[nucleus][0] > 12) {
			int[] rowOffsets = { (row - 3 + nrows) % nrows, (row - 2 + nrows) % nrows,
			    (row - 1 + nrows) % nrows, row, (row + 1 + nrows) % nrows,
			    (row + 2 + nrows) % nrows, (row + 3 + nrows) % nrows };
			int[] colOffsets = { (col - 3 + ncols) % ncols, (col - 2 + ncols) % ncols,
			    (col - 1 + ncols) % ncols, col, (col + 1 + ncols) % ncols,
			    (col + 2 + ncols) % ncols, (col + 3 + ncols) % ncols };
			
			// Fill outer sides
			for (int i = 1; i < 6; i++) {
				fillIfNotNucleus(rowOffsets[0], colOffsets[i]);
				fillIfNotNucleus(rowOffsets[i], colOffsets[0]);
				fillIfNotNucleus(rowOffsets[6], colOffsets[i]);
				fillIfNotNucleus(rowOffsets[i], colOffsets[6]);
			}
			// Fill middle corners
			fillIfNotNucleus(rowOffsets[1], colOffsets[1]);
			fillIfNotNucleus(rowOffsets[1], colOffsets[5]);
			fillIfNotNucleus(rowOffsets[5], colOffsets[1]);
			fillIfNotNucleus(rowOffsets[5], colOffsets[5]);
		}
	}
	
	/**
	 * Helper method for setAmoebaSizing. Fills cell as body if not already a nucleus.
	 */
	private void fillIfNotNucleus(int row, int col) {
		int cell = convertToIndex(row, col);
		if (cells[cell] != CellState.VAMPIRE)
			cells[cell] = CellState.ZOMBIE;
	}

	@Override
	public CellState get(int row, int col) {
		return cells[convertToIndex(row, col)];
	}

	@Override
	public void set(int row, int col, CellState state) {
		cells[convertToIndex(row, col)] = state;
	}

	@Override
	public boolean step(Callback action) {
		Queue<Cell> queue = new Queue<>();

		// Calculate needed updates
		for (int current = 0; current < cells.length; current++) {
			int row = convertToRow(current);
			int col = convertToCol(current);

			// If dead cell with no species/food neighbors, chance of becoming food (ALIVE).
			if (cells[current] == CellState.DEAD) {
				boolean alone = true;
				for (int neighbor : world.adj(current))
					if (cells[neighbor] != CellState.DEAD)
						alone = false;

				if (alone && RANDOM.nextInt(3000) == 0)
					queue.enqueue(new Cell(row, col, CellState.ALIVE));
			}
			// Else If cell is the nucleus.
			else if (cells[current] == CellState.VAMPIRE) {
				queue.enqueue(new Cell(row, col, CellState.VAMPIRE));
			}
		}

		boolean worldChanged = false;

		// Make needed updates (done afterwards to prevent invalid updates)
		while (!queue.isEmpty()) {
			Cell cell = queue.dequeue();

			// Invoke callback if a new state differs from old state
			if (cell.state() != get(cell.row(), cell.col())) {
				action.invoke(cell.row(), cell.col(), cell.state());
				worldChanged = true;
			}

			set(cell.row(), cell.col(), cell.state());
		}

		return worldChanged;
	}

	@Override
	public void forAllLife(Callback action) {
		for (int current = 0; current < cells.length; current++)
			if (cells[current] != CellState.DEAD)
				action.invoke(convertToRow(current), convertToCol(current), cells[current]);
	}

	@Override
	public long populationCount() {
		long count = 0;

		for (var state : cells)
			if (state != CellState.DEAD)
				count++;

		return count;
	}
}