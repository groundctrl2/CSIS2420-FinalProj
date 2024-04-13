package model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Contract for algorithms that implement Life-like cellular automata.
 * <p>
 * This defines the primary way in which the {@link application.ViewController}
 * will interact with model classes (which handle the simulation data and
 * implement the actual Game of Life algorithm) in order to update the view.
 *
 */
public interface ILife {
	/**
	 * A convenience class for holding multiple return values / parameters.
	 */
	record Cell(int row, int col, CellState state) {}

	@FunctionalInterface
	interface Callback {
		void invoke(int row, int col, CellState state);
	}

	/**
	 * For use by implementing classes.
	 *
	 * @see #randomize()
	 */
	static final Random RANDOM = new Random();

	/**
	 * Re-instantiate the world with new dimensions.
	 */
	void resize(int nrows, int ncols);

	/**
	 * End all life.
	 */
	void clear();

	/**
	 * Chaos!
	 */
	void randomize();

	/**
	 * Queries the state of a cell.
	 */
	CellState get(int row, int col);

	/**
	 * Set the specified cell to the given state.
	 */
	void set(int row, int col, CellState state);

	/**
	 * Advance the world by one tick. The callback should be invoked for each cell
	 * whose state was changed from the last tick.
	 *
	 * @param action Used to notify the caller (i.e., the controller) that a state
	 *               change occurred for a given cell in order to provide incremental
	 *               updates.
	 * @return {@code true} if the world was changed at all as a result of this step,
	 *         {@code false} otherwise.
	 */
	boolean step(Callback action);

	/**
	 * Execute an action for all live cells.
	 *
	 * @param action Used to provide the caller with the data for each living cell.
	 */
	void forAllLife(Callback action);

	/**
	 * @return the number of currently living cells.
	 */
	long populationCount();

	/**
	 * @return a set of all concrete classes that implement the {@link ILife} interface.
	 */
	static List<Class<? extends ILife>> implementations() {
		var packageName = ILife.class.getPackageName();
		var directory = packageName.replaceAll("\\.", "/");

		try (
			var stream = ILife.class.getClassLoader().getResourceAsStream(directory);
			var reader = new BufferedReader(new InputStreamReader(stream))) {
			var fileExt = ".class";
			return reader.lines()
			             .filter(filename -> filename.endsWith(fileExt))
			             .map(f -> getClass(packageName, f.substring(0, f.length() - fileExt.length())))
			             .filter(Objects::nonNull)
			             .collect(Collectors.toList());
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Helper method for {@link #implementations()}
	 */
	private static Class<? extends ILife> getClass(String packageName, String className) {
		try {
			var cls = Class.forName(packageName + "." + className);
			var mods = cls.getModifiers();

			// Filter for concrete subclasses of ILife only

			if (Modifier.isInterface(mods) || Modifier.isAbstract(mods))
				return null;

			if (!ILife.class.isAssignableFrom(cls))
				return null;

			return cls.asSubclass(ILife.class);
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
