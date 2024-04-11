package model;

import java.util.NoSuchElementException;
import java.util.Random;

import edu.princeton.cs.algs4.Graph;
import edu.princeton.cs.algs4.Queue;

public class GraphLife implements ILife {
	private Graph world;
	private Cell[] cells; // Vertex-indexed.
	private int nrows;
	private int ncols;

	private static final Random random = new Random();

	@Override
	public void resize(int nrows, int ncols) {
		this.world = new Graph(nrows * ncols);
		this.cells = new Cell[nrows * ncols];
		this.nrows = nrows;
		this.ncols = ncols;
		
		clear(); // Also creates cells.
		for (int current = 0; current < cells.length; current++)
			initializeNeighbors(current, cells[current].row(), cells[current].col());
	}
	
	private void initializeNeighbors(int vertexIndex, int row, int col) {
		int[] rowOffsets = {(row - 1 + nrows) % nrows, row, (row + 1 + nrows) % nrows};
        int[] colOffsets = {(col - 1 + ncols) % ncols, col, (col + 1 + ncols) % ncols};
        
        for (int r : rowOffsets)
    		for (int c : colOffsets)
    			if (r != row || c != col) { // Disclude current cell
    				int neighbor = findCell(r, c);
    				if (hasEdge(vertexIndex, neighbor) != true)
    					world.addEdge(vertexIndex, neighbor);
    			}		
	}
	
	/**
	 * Checks if vertex already linked to neighbor.
	 * 
	 * @param vertexIndex
	 * @param neighbor
	 * @return boolean true/false linked to neighbor
	 */
	private boolean hasEdge(int vertexIndex, int neighbor) {
		for (int n : world.adj(vertexIndex))
			if (n == neighbor)
				return true;
		return false;
	}

	/**
	 * Finds index of cell based on row and col
	 * 
	 * @param row
	 * @param col
	 * @return int cell index
	 */
	private int findCell(int row, int col) {
		for (int current = 0; current < cells.length; current++)
			if (cells[current].row() == row && cells[current].col() == col)
				return current;
		throw new NoSuchElementException("Cell doesn't exist.");
	}

	@Override
	public void clear() {
		int count = 0; 
		for (int row = 0; row < nrows; row++) {
			for (int col = 0; col < ncols; col++) {
				cells[count] = new Cell(row, col, CellState.DEAD);
				count++;
			}
		}
	}

	@Override
	public void randomize() {
		for (int current = 0; current < cells.length; current++)
			if (random.nextBoolean())
                cells[current] = new Cell(cells[current].row(), cells[current].col(), CellState.ALIVE);
            else
            	cells[current] = new Cell(cells[current].row(), cells[current].col(), CellState.DEAD);
	}

	@Override
	public CellState get(int row, int col) {
		for (int current = 0; current < cells.length; current++)
			if (cells[current].row() == row && cells[current].col() == col)
				return cells[current].state();
		throw new NoSuchElementException("Cell doesn't exist.");
	}

	@Override
	public void set(int row, int col, CellState state) {
		for (int current = 0; current < cells.length; current++)
			if (cells[current].row() == row && cells[current].col() == col)
				cells[current] = new Cell(row, col, state);
	}

	@Override
	public void step(Callback action) {
		Queue<Cell> queue = new Queue<>();
		
		// Calculate needed updates
		for (int current = 0; current < cells.length; current++) {
    		int aliveNeighbors = 0;
			for (int neighbor : world.adj(current)) {
    			if (cells[neighbor].state() == CellState.ALIVE)
    				aliveNeighbors++;
    		}
			
			if (cells[current].state() == CellState.ALIVE) {
				if (aliveNeighbors < 2 || aliveNeighbors > 3) // Alive cells only stay alive if between 2-3 neighbors.
					queue.enqueue(new Cell(cells[current].row(), cells[current].col(), CellState.DEAD));
			}
			else { // if (cells[i].state() == CellState.DEAD)
				if (aliveNeighbors == 3) // Dead cell with 3 neighbors becomes alive.
					queue.enqueue(new Cell(cells[current].row(), cells[current].col(), CellState.ALIVE));
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
		for (Cell cell : cells)
			if (cell.state() == CellState.ALIVE)
            	action.invoke(cell.row(), cell.col(), cell.state());
	}
}










