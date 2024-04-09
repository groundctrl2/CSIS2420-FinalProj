package model;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

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
        // TODO Auto-generated method stub
    }

    @Override
    public void forAllLife(Callback action) {
        for (int r = 0; r < nrows; r++)
            for (int c = 0; c < ncols; c++)
                if (world[r][c] == CellState.ALIVE)
                    action.invoke(r, c, world[r][c]);
    }
}
