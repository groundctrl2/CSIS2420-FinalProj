package model;

import java.util.Arrays;

import edu.princeton.cs.algs4.Queue;

/**
 * A basic implementation of Conway's Game of Life with the classic B3/S23 rules.
 */
public class SimpleLife implements ILife {
	private CellState[][] world;  // will be instantiated whenever resize() is called
	private int nrows;
	private int ncols;

	@Override
	public void resize(int nrows, int ncols) {
		this.world = new CellState[nrows][ncols];
		this.nrows = nrows;
		this.ncols = ncols;
		clear();
	}

	@Override
	public void clear() {
		for (var row : world)
			Arrays.fill(row, CellState.DEAD);
	}

	@Override
	public void randomize() {
		for (int r = 0; r < nrows; r++)
			for (int c = 0; c < ncols; c++)
				if (RANDOM.nextBoolean())
					world[r][c] = CellState.ALIVE;
				else
					world[r][c] = CellState.DEAD;
	}

	@Override
	public CellState get(int row, int col) {
		return world[row][col];
	}

	@Override
	public void set(int row, int col, CellState state) {
		world[row][col] = state;
	}

	@Override
	public boolean step(Callback action) {
		Queue<Cell> queue = new Queue<>();

		// Calculate needed updates
		for (int r = 0; r < nrows; r++) {
			for (int c = 0; c < ncols; c++) {
				int aliveNeighbors = countNeighbors(r, c);

				if (get(r, c) == CellState.ALIVE) {
					if (aliveNeighbors < 2 || aliveNeighbors > 3) // Alive cells only stay alive if between 2-3 neighbors.
						queue.enqueue(new Cell(r, c, CellState.DEAD));
				}
				else { // if (get(r, c) == CellState.DEAD)
					if (aliveNeighbors == 3) // Dead cell with 3 neighbors becomes alive.
						queue.enqueue(new Cell(r, c, CellState.ALIVE));
				}
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
		for (int r = 0; r < nrows; r++)
			for (int c = 0; c < ncols; c++)
				if (world[r][c] == CellState.ALIVE)
					action.invoke(r, c, world[r][c]);
	}

	@Override
	public long populationCount() {
		long count = 0;

		for (var row : world)
			for (var state : row)
				if (state == CellState.ALIVE)
					count++;

		return count;
	}

	/**
	 * Returns count how many of 8 neighbors alive (wraps around).
	 *
	 * @return int count of alive neighbors surrounding cell
	 */
	private int countNeighbors(int row, int col) {
		int count = 0;
		/*
		 * The +M factor before reducing (mod M) accounts for the fact that Java's (%)
		 * operator uses truncated division as opposed to floor division--the latter
		 * being more convenient for cyclic array-indexing.
		 *
		 * As an example, -1 % 5 == -1 in Java instead of 4.
		 *
		 * See: https://en.wikipedia.org/wiki/Modulo#In_programming_languages
		 */
		int[] rowOffsets = {(row - 1 + nrows) % nrows, row, (row + 1 + nrows) % nrows};
		int[] colOffsets = {(col - 1 + ncols) % ncols, col, (col + 1 + ncols) % ncols};

		for (int r : rowOffsets)
			for (int c : colOffsets)
				if (r != row || c != col) // Disclude current cell
					if (get(r, c) == CellState.ALIVE)
						count++;

		return count;
	}
}