package model;

import java.util.ArrayList;
import java.util.Arrays;

import edu.princeton.cs.algs4.BreadthFirstPaths;
import edu.princeton.cs.algs4.Graph;
import edu.princeton.cs.algs4.Queue;

public class AmoebaLife implements ILife {
	private Graph world;
	private CellState[] cells; // row-col indexed
	private int[][] amoebaInfo; // row-col indexed
	private int nrows;
	private int ncols;
	private Queue<Cell> queue = new Queue<>(); // Stores cell updates
	private ArrayList<Integer> alreadyMoved; // Stores already moved cells
	private static final int GROWTH_STAGE_1 = 10; // 5 wide stage
	private static final int GROWTH_STAGE_2 = 20; // 7 wide stage
	private static final int GROWTH_STAGE_3 = 30; // Cell splitting stage

	@Override
	public void resize(int nrows, int ncols) {
		this.world = new Graph(nrows * ncols);
		this.cells = new CellState[nrows * ncols];
		this.amoebaInfo = new int[nrows * ncols][2];
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

		// Initialize amoeba growth count and hunger
		for (int i = 0; i < amoebaInfo.length; i++) {
			amoebaInfo[i][0] = 1; // Growth count
			amoebaInfo[i][1] = 1; // Hunger/steps without food
		}
	}

	@Override
	public void randomize() {
		clear();

		// 3 Nuclei
		for (int i = 0; i < 5; i++) {
			int randomInt = RANDOM.nextInt(nrows * ncols);
			cells[randomInt] = CellState.VAMPIRE;
			setGrowthStage(convertToRow(randomInt), convertToCol(randomInt));
		}
		// 1 Food
		cells[RANDOM.nextInt(nrows * ncols)] = CellState.ALIVE;
	}

	/**
	 * Moves cell's growth and hunger count to next cell.
	 * 
	 * @param current Current cell position
	 * @param next    Next cell position
	 */
	private void moveAmoebaInfo(int current, int next) {
		amoebaInfo[next][0] = amoebaInfo[current][0];
		amoebaInfo[next][1] = amoebaInfo[current][1];
	}

	/**
	 * Sets the body cells in the current world and in the update based on the
	 * current growth stage.
	 * 
	 * @param row Nucleus next position row
	 * @param col Nucleus next position col
	 */
	private void setGrowthStage(int row, int col) {
		int nucleus = convertToIndex(row, col);

		// Default growth stage:
		if (amoebaInfo[nucleus][0] >= 0) {
			int[] rowOffsets = { (row - 1 + nrows) % nrows, row, (row + 1 + nrows) % nrows };
			int[] colOffsets = { (col - 1 + ncols) % ncols, col, (col + 1 + ncols) % ncols };

			// Fill neighbors
			for (int r : rowOffsets)
				for (int c : colOffsets) {
					fillBody(r, c);
				}
		}
		// 1st growth stage:
		if (amoebaInfo[nucleus][0] > GROWTH_STAGE_1) {
			int[] rowOffsets = { (row - 2 + nrows) % nrows, (row - 1 + nrows) % nrows, row,
			    (row + 1 + nrows) % nrows, (row + 2 + nrows) % nrows };
			int[] colOffsets = { (col - 2 + ncols) % ncols, (col - 1 + ncols) % ncols, col,
			    (col + 1 + ncols) % ncols, (col + 2 + ncols) % ncols };

			// Fill outer sides
			for (int i = 1; i < 4; i++) {
				fillBody(rowOffsets[0], colOffsets[i]);
				fillBody(rowOffsets[i], colOffsets[0]);
				fillBody(rowOffsets[4], colOffsets[i]);
				fillBody(rowOffsets[i], colOffsets[4]);
			}
		}
		// 2nd growth stage:
		if (amoebaInfo[nucleus][0] > GROWTH_STAGE_2) {
			int[] rowOffsets = { (row - 3 + nrows) % nrows, (row - 2 + nrows) % nrows,
			    (row - 1 + nrows) % nrows, row, (row + 1 + nrows) % nrows,
			    (row + 2 + nrows) % nrows, (row + 3 + nrows) % nrows };
			int[] colOffsets = { (col - 3 + ncols) % ncols, (col - 2 + ncols) % ncols,
			    (col - 1 + ncols) % ncols, col, (col + 1 + ncols) % ncols,
			    (col + 2 + ncols) % ncols, (col + 3 + ncols) % ncols };

			// Fill outer sides
			for (int i = 1; i < 6; i++) {
				fillBody(rowOffsets[0], colOffsets[i]);
				fillBody(rowOffsets[i], colOffsets[0]);
				fillBody(rowOffsets[6], colOffsets[i]);
				fillBody(rowOffsets[i], colOffsets[6]);
			}
			// Fill middle corners
			fillBody(rowOffsets[1], colOffsets[1]);
			fillBody(rowOffsets[1], colOffsets[5]);
			fillBody(rowOffsets[5], colOffsets[1]);
			fillBody(rowOffsets[5], colOffsets[5]);
		}
	}

	/**
	 * Adds body cell so long as it doesn't cover a nucleus cell.
	 * 
	 * @param row
	 * @param col
	 */
	private void fillBody(int row, int col) {
		int cell = convertToIndex(row, col);
		if (cells[cell] != CellState.VAMPIRE) {
			cells[cell] = CellState.ZOMBIE;
			queue.enqueue(new Cell(row, col, CellState.ZOMBIE));
		}
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
		ArrayList<Integer> foodIndexes = new ArrayList<>();
		alreadyMoved = new ArrayList<>();
		int nucleusCount = 0;

		for (int i = 0; i < cells.length; i++) {
			if (cells[i] == CellState.VAMPIRE)
				nucleusCount++;
			if (cells[i] == CellState.ALIVE)
				foodIndexes.add(i);
		}

		// Calculate needed updates:
		for (int current = 0; current < cells.length; current++) {
			// Skip cells that have already been moved/dealt with.
			if (!alreadyMoved.contains(current)) {
				int row = convertToRow(current);
				int col = convertToCol(current);

				// If cell food, stay food and float around.
				if (cells[current] == CellState.ALIVE) {
					// Get all possible positions
					ArrayList<Integer> availablePositions = getPossiblePositions(current);

					// Pick a random position for next position.
					int nextPosition = availablePositions
					    .get(RANDOM.nextInt(availablePositions.size()));

					queue.enqueue(new Cell(convertToRow(nextPosition), convertToCol(nextPosition),
					    CellState.ALIVE));
				}
				// If cell dead/empty or body and alone, chance to become food if all dead.
				else if (cells[current] != CellState.VAMPIRE) {
					// Find whether cell is alone
					boolean alone = true;
					for (Integer neighbor : world.adj(current))
						if (cells[neighbor] != CellState.DEAD)
							alone = false;

					if (RANDOM.nextInt(6000) == 0 && alone) // 1 in 6000 chance of becoming food.
						queue.enqueue(new Cell(row, col, CellState.ALIVE));
				}
				// Else cell is nucleus.
				else {
					// If there's food to get, targer/eat it.
					if (foodIndexes.size() > 0) {
						// Find closest food.
						BreadthFirstPaths bfs = new BreadthFirstPaths(world, current);
						int target = foodIndexes.get(0);
						int targetDistance = bfs.distTo(target);

						for (int food : foodIndexes)
							if (bfs.distTo(food) < targetDistance) {
								target = food;
								targetDistance = bfs.distTo(food);
							}

						// If population too high, kill the babies until low enough.
						if (nucleusCount > 50 && amoebaInfo[current][0] == 1
						    && RANDOM.nextBoolean()) {
							cells[current] = CellState.DEAD;
							queue.enqueue(new Cell(row, col, CellState.DEAD));
							nucleusCount--;
						}
						// If too big, split into 2 nucleus cells.
						else if (amoebaInfo[current][0] > GROWTH_STAGE_3) {
							// Get all possible positions
							ArrayList<Integer> availablePositions = getPossiblePositions(current);

							// Pick a random position for next position.
							int twinPosition = availablePositions
							    .get(RANDOM.nextInt(availablePositions.size()));

							// Keep original cell with the default growth stage and hunger.
							queue.enqueue(new Cell(row, col, CellState.VAMPIRE));
							setGrowthStage(row, col);
							amoebaInfo[current][0] = 1;
							amoebaInfo[current][1] = 1;

							// Add twin cell with the default growth stage and hunger.
							int twinRow = convertToRow(twinPosition);
							int twinCol = convertToCol(twinPosition);

							cells[twinPosition] = CellState.VAMPIRE;
							queue.enqueue(new Cell(twinRow, twinCol, CellState.VAMPIRE));
							setGrowthStage(twinRow, twinCol);
							amoebaInfo[twinPosition][0] = 1;
							amoebaInfo[twinPosition][1] = 1;
						}
						// Growth Stage default eating.
						else if (targetDistance <= 3) {
							eat(current, target);
						}
						// Growth Stage 1 eating.
						else if (targetDistance <= 4 && amoebaInfo[current][0] > GROWTH_STAGE_1) {
							eat(current, target);
						}
						// Growth Stage 2 eating.
						else if (targetDistance <= 5 && amoebaInfo[current][0] > GROWTH_STAGE_2) {
							eat(current, target);
						}
						// Continue targeting.
						else {
							amoebaInfo[current][1]++; // Add 1 to hunger.
							int nextPosition = current;

							// Get all possible positions
							ArrayList<Integer> availablePositions = getPossiblePositions(current);

							// Pick the closest available position to the target.
							int currentDistance = targetDistance;
							for (int neighbor : availablePositions) {
								BreadthFirstPaths neighborPath = new BreadthFirstPaths(world,
								    neighbor);
								if (currentDistance > neighborPath.distTo(target)) {
									currentDistance = neighborPath.distTo(target);
									nextPosition = neighbor;
								}
							}

							move(current, nextPosition);
						}
					}
					// Else no food to eat, move randomly.
					else {
						// Get all possible random positions
						ArrayList<Integer> availablePositions = getPossiblePositions(current);

						// Pick a random position for next position.
						int randomPosition = availablePositions
						    .get(RANDOM.nextInt(availablePositions.size()));
						move(current, randomPosition);
					}
				}
			}
		}

		// Make needed updates (done afterwards to prevent invalid updates)
		Arrays.fill(cells, CellState.DEAD);
		while (!queue.isEmpty()) {
			Cell cell = queue.dequeue();
			// Invoke callback if a new state differs from old state
			if (cell.state() != get(cell.row(), cell.col())) {
				action.invoke(cell.row(), cell.col(), cell.state());
			}
			set(cell.row(), cell.col(), cell.state());
		}
		for (int current = 0; current < cells.length; current++)
			action.invoke(convertToRow(current), convertToCol(current), cells[current]);

		return populationCount() > 0; // Game designed to go on as long as Amoeba still alive.
	}

	/**
	 * Allows nucleus to eat the target and stay in the world.
	 * 
	 * @param current Current nucleus cell
	 * @param target  Target food cell
	 */
	private void eat(int current, int target) {
		int row = convertToRow(current);
		int col = convertToCol(current);

		amoebaInfo[current][0]++; // Add to size.
		// Kill the food.
		cells[target] = CellState.DEAD;
		queue.enqueue(new Cell(convertToRow(target), convertToCol(target), CellState.DEAD));
		// Keep the nucleus.
		queue.enqueue(new Cell(row, col, CellState.VAMPIRE));
		setGrowthStage(row, col);
	}

	/**
	 * Returns a list of all neighbor positions that aren't already taken by a
	 * nucleus. If no positions are available, current position is put in the list.
	 * 
	 * @param current Current cell
	 * @return availablePositions List of possible positions
	 */
	private ArrayList<Integer> getPossiblePositions(int current) {
		ArrayList<Integer> availablePositions = new ArrayList<Integer>();

		var neighbors = world.adj(current);
		for (int neighbor : neighbors) {
			if (cells[neighbor] != CellState.VAMPIRE)
				availablePositions.add(neighbor);
		}
		if (availablePositions.size() == 0)
			availablePositions.add(current);

		return availablePositions;
	}

	/**
	 * Moves the nucleus to next position. Works if staying in place as well.
	 * 
	 * @param current      Current nucleus cell
	 * @param nextPosition Index to move current to
	 */
	private void move(int current, int nextPosition) {
		int newRow = convertToRow(nextPosition);
		int newCol = convertToCol(nextPosition);

		// Move nucleus cell.
		cells[current] = CellState.ZOMBIE;
		queue.enqueue(new Cell(newRow, newCol, CellState.ZOMBIE));
		cells[nextPosition] = CellState.VAMPIRE;
		queue.enqueue(new Cell(newRow, newCol, CellState.VAMPIRE));
		// Set body cells.
		setGrowthStage(newRow, newCol);
		// Transfer info and mark that cell has already been moved.
		moveAmoebaInfo(current, nextPosition);
		alreadyMoved.add(nextPosition);
	}

	@Override
	public void forAllLife(Callback action) {
		for (int current = 0; current < cells.length; current++)
			action.invoke(convertToRow(current), convertToCol(current), cells[current]);
	}

	@Override
	public long populationCount() {
		long count = 0;
		for (var state : cells)
			if (state == CellState.VAMPIRE)
				count++;
		return count;
	}
}