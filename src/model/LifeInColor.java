package model;

import java.util.Arrays;

import edu.princeton.cs.algs4.Graph;
import edu.princeton.cs.algs4.Queue;

public class LifeInColor implements ILife {
	private Graph world;
	private CellState[] cells; // row-col indexed
	private int nrows;
	private int ncols;

	@Override
	public void resize(int nrows, int ncols) {
		this.world = new Graph(nrows * ncols);
		this.cells = new CellState[nrows * ncols];
		this.nrows = nrows;
		this.ncols = ncols;

		clear();

		// Initialize edges/neighbors
		for (int current = 0; current < cells.length; current++)
			initializeNeighbors(current);
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

		int[] rowOffsets = {(row - 1 + nrows) % nrows, row, (row + 1 + nrows) % nrows};
		int[] colOffsets = {(col - 1 + ncols) % ncols, col, (col + 1 + ncols) % ncols};

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
	}

	@Override
	public void randomize() {
		for (int current = 0; current < cells.length; current++)
			if (RANDOM.nextBoolean() && RANDOM.nextBoolean())
				cells[current] = CellState.DEAD;
			else
				cells[current] = randomSpecies();
	}

	/**
	 * @return random species CellState (between RED, BLUE, and GREEN).
	 */
	private CellState randomSpecies() {
		int randomInt = RANDOM.nextInt(3);
		if (randomInt == 0)
			return CellState.RED;
		else if (randomInt == 1)
			return CellState.GREEN;
		else
			return CellState.BLUE;
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
			CellState species = cells[current];

			// If dead cell, set species
			if (species == CellState.DEAD) {
				for (int neighbor : world.adj(current))
					if (cells[neighbor] != CellState.DEAD)
						species = cells[neighbor];
				if (species == CellState.DEAD) // If still dead, set as random species.
					species = randomSpecies();
			}

			// Count amount of alive neighbors
			int aliveNeighbors = 0;
			for (int neighbor : world.adj(current))
				if (cells[neighbor] == species)
					aliveNeighbors++;

			// Record needed updates
			int row = convertToRow(current);
			int col = convertToCol(current);

			if (cells[current] == species) {
				if (aliveNeighbors < 2 || aliveNeighbors > 3) // Alive cells only stay alive if between 2-3 neighbors.
					queue.enqueue(new Cell(row, col, CellState.DEAD));
			}
			else { // if (cells[i].state() == CellState.DEAD)
				if (aliveNeighbors == 3) // Dead cell with 3 neighbors becomes alive.
					queue.enqueue(new Cell(row, col, species));
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