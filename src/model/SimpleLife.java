package model;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import edu.princeton.cs.algs4.Queue;

/**
 * A basic implementation of Conway's Game of Life.
 */
public class SimpleLife implements ILife {
    private CellState[][] world = new CellState[0][];
    private int nrows;
    private int ncols;

    private static final Random random = new Random();

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
                if (random.nextBoolean())
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
    public void seed(List<Cell> seed) {
        for (var cell : seed)
            this.set(cell.row(), cell.col(), cell.state());
    }

    @Override
    public void step(Callback action) {
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
    	
    	// Make needed updates (done afterwards to prevent invalid updates)
    	while (!queue.isEmpty()) {
    		Cell cell = queue.dequeue();
    		set(cell.row(), cell.col(), cell.state());
    	}
    }

    @Override
    public void forAllLife(Callback action) {
        for (int r = 0; r < nrows; r++)
            for (int c = 0; c < ncols; c++)
                if (world[r][c] == CellState.ALIVE)
                	action.invoke(r, c, world[r][c]);
    }
    
    /**
     * Returns count how many of 8 neighbors alive (wraps around).
     * @TODO replace with adjacency list alive count. 
     * 
     * @return int count of alive neighbors surrounding cell
     */
    @Override
    public int countNeighbors(int row, int col) {
    	int count = 0;
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