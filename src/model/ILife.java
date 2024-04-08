package model;

import java.util.List;
import java.util.function.Consumer;

/**
 * Contract for algorithms that implement Life-like cellular automata.
 */
public interface ILife {
    /**
     * A convenience class for holding multiple return values / parameters.
     *
     * @see ILife#seed
     */
    public record Cell(int r, int c, CellState state) {}

    /**
     * End all life.
     */
    void clear();

    /**
     * Set the specified cell with the given state.
     */
    void set(int r, int c, CellState state);

    /**
     * Set the initial state of the world
     */
    void seed(List<Cell> seed);

    /**
     * Advance the world by one tick.
     */
    void step(Consumer<Cell> callback);
}
