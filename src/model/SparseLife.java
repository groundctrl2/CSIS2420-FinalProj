package model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * An implementation of Conway's Game of Life (B3/S23)
 * using an sparse representation of the world.
 */
public class SparseLife implements ILife {
    private int nrows;
    private int ncols;

    /** Stores only the (row, col) locations of live cells */
    private Set<Loc> liveSet;

    /**
     * Use this method to create an index pair instead of {@link Loc#Loc new Loc(...)}
     * to normalize the indices / adjust for grid wrap-around.
     */
    private Loc loc(int row, int col) {
        return new Loc(Math.floorMod(row, nrows), Math.floorMod(col, ncols));
    }

    /**
     * A pair of ints representing the grid location of a live cell.
     * Use {@link SparseLife#loc loc(...)} instead of {@code new Loc(...)}
     */
    private record Loc(int row, int col) {
        /**
         * Returns an array of the locations of the 3x3 neighborhood
         * centered at this location.
         * */
        Loc[] neighborhood(SparseLife context) {
            return new Loc[] {
                context.loc(row - 1, col - 1),  // NW
                context.loc(row - 1, col + 0),  // N
                context.loc(row - 1, col + 1),  // NE
                context.loc(row + 0, col - 1),  // W
                this,  // assume this location is already normalized
                context.loc(row + 0, col + 1),  // E
                context.loc(row + 1, col - 1),  // SW
                context.loc(row + 1, col + 0),  // S
                context.loc(row + 1, col + 1),  // SE
            };
        }
    }

    @Override
    public void resize(int nrows, int ncols) {
        assert nrows >= 0 && ncols >= 0;

        this.nrows = nrows;
        this.ncols = ncols;
        this.liveSet = new HashSet<>();
    }

    @Override
    public void clear() {
        liveSet.clear();
    }

    @Override
    public void randomize() {
        for (int r = 0; r < nrows; r++)
            for (int c = 0; c < ncols; c++)
                if (RANDOM.nextBoolean())
                    liveSet.add(loc(r, c));
    }

    @Override
    public CellState get(int row, int col) {
        if (liveSet.contains(loc(row, col)))
            return CellState.ALIVE;
        else
            return CellState.DEAD;
    }

    @Override
    public void set(int row, int col, CellState state) {
        if (state == CellState.ALIVE)
            liveSet.add(loc(row, col));
        else // (state == CellState.DEAD)
            liveSet.remove(loc(row, col));
    }

    @Override
    public boolean step(Callback action) {
        /*
         * New life can only spawn next to current life. Thus, the only cells that we
         * need to consider are the current live cells and their immediate neighbors.
         * All other cells will remain dead.
         *
         * First, for each live cell, we create a counter mapping the (row, col)
         * of every cell that has potential for change to the population count
         * of the 3x3 neighborhood centered at that cell's location.
         *
         * The population count differ from neighbor count in that it includes
         * the central cell.
         */
        var populationCounts = new HashMap<Loc, Integer>();

        // For each live cell, propagate +1 to all 9 cells in its neighborhood.
        for (var loc : liveSet)
            for (var neighbor : loc.neighborhood(this))
                populationCounts.merge(neighbor, 1, Integer::sum);

        // Compute the new live set.
        var nextGeneration = new HashSet<Loc>();
        boolean worldChanged = false;

        for (var entry : populationCounts.entrySet()) {
            var loc = entry.getKey();
            int popCount = entry.getValue();
            boolean presentLife = liveSet.contains(loc);

            /*
             * This is the condition for life in the standard Conway ruleset (B3/S23)
             * translated to population counts rather than neighbor-only counts.
             */
            boolean futureLife = (popCount == 3) || (presentLife && popCount == 4);

            if (futureLife)
                nextGeneration.add(loc);

            // Notify caller if there is a state change for this cell.
            if (futureLife != presentLife) {
                var state = futureLife ? CellState.ALIVE : CellState.DEAD;
                action.invoke(loc.row(), loc.col(), state);
                worldChanged = true;
            }
        }

        liveSet = nextGeneration;
        return worldChanged;
    }

    @Override
    public void forAllLife(Callback action) {
        for (var loc : liveSet)
            action.invoke(loc.row(), loc.col(), CellState.ALIVE);
    }

    @Override
    public long populationCount() {
        return liveSet.size();
    }
}
