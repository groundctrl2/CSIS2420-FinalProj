package model;

/**
 * Contract for algorithms that implement Life-like cellular automata.
 */
public interface ILife {
    /**
     * A convenience class for holding multiple return values / parameters.
     *
     * @see ILife#seed
     */
    public record Cell(int row, int col, CellState state) {}

    @FunctionalInterface
    public interface Callback {
        void invoke(int row, int col, CellState state);
    }

    /**
     * Re-instantiate the world with new dimensions.
     */
    void resize(int nrows, int ncols);

    /**
     * End all life.
     */
    void clear();

    void randomize();

    /**
     * Queries the state of a cell.
     */
    CellState get(int row, int col);

    /**
     * Set the specified cell with the given state.
     */
    void set(int row, int col, CellState state);

    /**
     * Advance the world by one tick.
     */
    void step(Callback action);

    /**
     * Execute an action for all live cells.
     */
    void forAllLife(Callback action);
}
