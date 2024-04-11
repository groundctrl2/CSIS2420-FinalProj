package model;

import java.util.Arrays;
import java.util.Random;

import edu.princeton.cs.algs4.Graph;
import edu.princeton.cs.algs4.Queue;

public class KnightLife implements ILife {
	private Graph world;
	private CellState[] cells; // row-col indexed
	private int nrows;
	private int ncols;

	private static final Random random = new Random();

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
		return (int) index / ncols;
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

		// int[] rowOffsets = {(row - 1 + nrows) % nrows, row, (row + 1 + nrows) % nrows};
		// int[] colOffsets = {(col - 1 + ncols) % ncols, col, (col + 1 + ncols) % ncols};
		int[] rowOffsets = { (row - 2 + nrows) % nrows, (row - 1 + nrows) % nrows, (row + 1 + nrows) % nrows,
				(row + 2 + nrows) % nrows };
		int[] colOffsets = { (col - 2 + ncols) % ncols, (col - 1 + ncols) % ncols, (col + 1 + ncols) % ncols,
				(col + 2 + ncols) % ncols };
		int[][] neighbors = { { rowOffsets[0], colOffsets[1] }, { rowOffsets[0], colOffsets[2] },
				{ rowOffsets[1], colOffsets[0] }, { rowOffsets[1], colOffsets[3] }, { rowOffsets[2], colOffsets[0] },
				{ rowOffsets[2], colOffsets[3] }, { rowOffsets[3], colOffsets[1] }, { rowOffsets[3], colOffsets[2] } };

		for (int n = 0; n < neighbors.length; n++) {
			int neighbor = convertToIndex(neighbors[n][0], neighbors[n][1]);
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
			if (random.nextBoolean())
				cells[current] = CellState.ALIVE;
			else
				cells[current] = CellState.DEAD;
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
	public void step(Callback action) {
		Queue<Cell> queue = new Queue<>();

		// Calculate needed updates
		for (int current = 0; current < cells.length; current++) {
			// Count amount of alive neighbors
			int aliveNeighbors = 0;
			for (int neighbor : world.adj(current)) {
				if (cells[neighbor] == CellState.ALIVE)
					aliveNeighbors++;
			}

			// Record needed updates
			int row = convertToRow(current);
			int col = convertToCol(current);

			if (cells[current] == CellState.ALIVE) {
				if (aliveNeighbors < 2 || aliveNeighbors > 3) // Alive cells only stay alive if between 2-3 neighbors.
					queue.enqueue(new Cell(row, col, CellState.DEAD));
			} else { // if (cells[i].state() == CellState.DEAD)
				if (aliveNeighbors == 3) // Dead cell with 3 neighbors becomes alive.
					queue.enqueue(new Cell(row, col, CellState.ALIVE));
			}
		}

		// Make needed updates (done afterwards to prevent invalid updates)
		while (!queue.isEmpty()) {
			Cell cell = queue.dequeue();
			set(cell.row(), cell.col(), cell.state());
		}
	}

	@Override
	public void forAllLife(Callback action) {
		for (int current = 0; current < cells.length; current++)
			if (cells[current] == CellState.ALIVE)
				action.invoke(convertToRow(current), convertToCol(current), cells[current]);
	}
}