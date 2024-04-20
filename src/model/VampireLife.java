package model;

import java.util.ArrayList;
import java.util.Arrays;

import edu.princeton.cs.algs4.BreadthFirstPaths;
import edu.princeton.cs.algs4.Graph;
import edu.princeton.cs.algs4.Queue;

public class VampireLife implements ILife {
	private Graph world;
	private CellState[] cells; // row-col indexed
	private int nrows;
	private int ncols;
	private int[][] vampireTargets; // row-col indexed
	private int vampireCount = 0;
	private static final CellState COLOR_1 = CellState.RED;

	@Override
	public void resize(int nrows, int ncols) {
		this.world = new Graph(nrows * ncols);
		this.cells = new CellState[nrows * ncols];
		this.vampireTargets = new int[nrows * ncols][2];
		this.nrows = nrows;
		this.ncols = ncols;

		clear();

		// Initialize edges/neighbors
		for (int current = 0; current < cells.length; current++)
			initializeNeighbors(current);

		// Initialize vampire targets
		for (int i = 0; i < vampireTargets.length; i++) {
			vampireTargets[i][0] = -1; // Target
			vampireTargets[i][1] = 0; // Amount of steps spent chasing target.
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
		vampireTargets = new int[nrows * ncols][2];
	}

	@Override
	public void randomize() {
		for (int current = 0; current < cells.length; current++)
			if (RANDOM.nextBoolean())
				cells[current] = CellState.ALIVE;
			else
				cells[current] = CellState.DEAD;

		// Generate 1 vampire.
		cells[RANDOM.nextInt(cells.length)] = COLOR_1;
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
			// If cell is vampire
			if ((cells[current] == COLOR_1)) {
				// Find alive cells
				ArrayList<Integer> aliveCells = new ArrayList<>();
				for (int cell = 0; cell < cells.length; cell++)
					if (cells[cell] == CellState.ALIVE)
						aliveCells.add(cell);

				int oldRow = convertToRow(current);
				int oldCol = convertToCol(current);

				// if vampire count goes over 4, some die. Viago, Vladislav, Deacon and Petyr forever.
				if (vampireCount > 4) {
					queue.enqueue(new Cell(oldRow, oldCol, CellState.DEAD));
					vampireTargets[current][0] = -1; // Reset target
					vampireTargets[current][1] = 0; // Reset target step count
					vampireCount--;
				}
				// Else if there's alive cells to infect, get em.
				else if (aliveCells.size() > 0) {
					BreadthFirstPaths bfs = new BreadthFirstPaths(world, current);

					// If current target has been chased too long or it hasn't been found yet, get new one.
					if (vampireTargets[current][1] > 5 || vampireTargets[current][0] == -1) {
						vampireTargets[current][1] = 0; // Reset target step count
						vampireTargets[current][0] = aliveCells.get(0);
						int closestDistance = bfs.distTo(vampireTargets[current][0]);

						for (int i = 1; i < aliveCells.size(); i++) {
							int currentCell = aliveCells.get(i);
							int currentDistance = bfs.distTo(aliveCells.get(i));
							if (currentDistance < closestDistance)
								vampireTargets[current][0] = currentCell;
						}
					}
					// Else keep current target and record that its been pursued while dead again.
					else {
						if (cells[vampireTargets[current][0]] == CellState.DEAD)
							vampireTargets[current][1]++;
					}

					// Get the next position.
					var path = bfs.pathTo(vampireTargets[current][0]).iterator();
					path.next();
					int nextPosition = current;
					if (path.hasNext())
						nextPosition = path.next();

					// Ensure vampire only moves into empty/dead space.
					// if next position not empty, pick a random empty position or stay in place.
					if (cells[nextPosition] != CellState.DEAD) {
						// Get all possible positions
						ArrayList<Integer> availablePositions = new ArrayList<Integer>();
						availablePositions.add(current);

						var neighbors = world.adj(current);
						for (int neighbor : neighbors) {
							if (cells[neighbor] == CellState.DEAD)
								availablePositions.add(neighbor);
						}

						// Pick a random position for next position
						nextPosition = availablePositions.get(RANDOM.nextInt(availablePositions.size()));
					}

					// If moving, move.
					if (current != nextPosition) {
						int newRow = convertToRow(nextPosition);
						int newCol = convertToCol(nextPosition);

						queue.enqueue(new Cell(oldRow, oldCol, CellState.DEAD));
						queue.enqueue(new Cell(newRow, newCol, COLOR_1));

						// Set new vampire target values
						vampireTargets[convertToIndex(newRow, newCol)][0] = vampireTargets[current][0];
						vampireTargets[convertToIndex(newRow, newCol)][1] = vampireTargets[current][1];
						vampireTargets[current][0] = -1;
						vampireTargets[current][1] = 0;
					}
					// If not moving, stay in place
					else {
						queue.enqueue(new Cell(oldRow, oldCol, COLOR_1));
					}
				}
				// Else vampire lives in immortal peace.
				else {
					queue.enqueue(new Cell(oldRow, oldCol, COLOR_1));
				}
			}
			// else cell is ALIVE or DEAD
			else {
				// Count amount of alive neighbors
				int aliveNeighbors = 0;
				for (int neighbor : world.adj(current)) {
					if (cells[neighbor] == CellState.ALIVE)
						aliveNeighbors++;

					if (cells[neighbor] == COLOR_1);
				}

				// Check if there's a vampire neighbor
				boolean vampireNeighbor = false;
				for (int neighbor : world.adj(current)) {
					if (cells[neighbor] == COLOR_1)
						vampireNeighbor = true;
				}

				// Record needed updates
				int row = convertToRow(current);
				int col = convertToCol(current);

				if (cells[current] == CellState.ALIVE) {
					if (vampireNeighbor) {// If cell has a vampire neighbor, cell becomes a vampire
						queue.enqueue(new Cell(row, col, COLOR_1));
						vampireCount++;
						vampireTargets[current][0] = -1; // Reset target
						vampireTargets[current][1] = 0; // Reset target step count
					}
					else if (aliveNeighbors < 2 || aliveNeighbors > 3) // Alive cells only stay alive if between 2-3 neighbors.
						queue.enqueue(new Cell(row, col, CellState.DEAD));
				}
				else { // if (cells[i].state() == CellState.DEAD)
					if (aliveNeighbors == 3) // Dead cell with 3 neighbors becomes alive.
						queue.enqueue(new Cell(row, col, CellState.ALIVE));
				}

			}
		}

		boolean worldChanged = false;

		// Make needed updates (done afterwards to prevent invalid updates)
		int newvampireCount = 0;
		while (!queue.isEmpty()) {
			Cell cell = queue.dequeue();

			// Invoke callback if a new state differs from old state
			if (cell.state() != get(cell.row(), cell.col())) {
				action.invoke(cell.row(), cell.col(), cell.state());
				worldChanged = true;
			}
			set(cell.row(), cell.col(), cell.state());

			if (cell.state() == COLOR_1)
				newvampireCount++;
		}
		vampireCount = newvampireCount;

		return worldChanged;
	}

	@Override
	public void forAllLife(Callback action) {
		for (int current = 0; current < cells.length; current++)
			if (cells[current] == CellState.ALIVE || cells[current] == COLOR_1)
				action.invoke(convertToRow(current), convertToCol(current), cells[current]);
	}

	@Override
	public long populationCount() {
		long count = 0;

		for (var state : cells)
			if (state == CellState.ALIVE || state == COLOR_1)
				count++;

		return count;
	}
	
	/**
	 * @return Description of this model
	 */
	public String description() {
		return "Vampires vs The Game of Life.\nVampires use BFS to attack all life."; 
	}
}