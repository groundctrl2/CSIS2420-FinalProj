package application.component;

import java.util.function.IntConsumer;

import javafx.beans.NamedArg;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.util.converter.IntegerStringConverter;

/**
 * Simplifies the construction of a labeled integer spinner in FXML.
 */
public class SpinnerBox extends HBox {
	public final Spinner<Integer> spinner;
	public final Label label;

	// Annotations needed to expose constructor to FXML
	public SpinnerBox(@NamedArg("min") int min,
	                  @NamedArg("max") int max,
	                  @NamedArg("value") int value,
	                  @NamedArg("label") String label,
	                  @NamedArg(value="labelOnLeft", defaultValue="true") boolean labelOnLeft) {
		this.spinner = new Spinner<>(min, max, value);
		this.spinner.setEditable(true);
		this.label = new Label(label);

		addInputValidator(min, max, value);

		if (labelOnLeft)
			this.getChildren().addAll(this.label, this.spinner);
		else
			this.getChildren().addAll(this.spinner, this.label);

		this.setSpacing(10);
		this.getStyleClass().add("spinner-box");
	}

	/**
	 * Add input validation through text formatter.
	 */
	private void addInputValidator(int min, int max, int value) {
		spinner.getValueFactory().setConverter(new IntegerStringConverter() {
			@Override
			public Integer fromString(String s) {
				// allow empty input to equal 0,..which clamps to `min`.
				if (s == null || s.isBlank())
					return min;

				return super.fromString(s);
			}
		});

		var editor = spinner.getEditor();

		editor.setTextFormatter(new TextFormatter<Integer>(change -> {
			if (!change.isContentChange())
				return change;

			var text = change.getControlNewText().strip();

			if (text.isEmpty())
				return change;  // accept empty input

			try {
				Integer.parseInt(text);
			} catch (NumberFormatException e) {
				return null;  // reject change
			}

			return change;
		}));
	}

	/* Convenience methods */

	public int getValue() { return spinner.getValue(); }
	public void setValue(int value) { spinner.getValueFactory().setValue(value); }

	/**
	 * Attaches a change listener to the spinner's value property.
	 *
	 * @param subscriber a callback that receives the new value of the spinner
	 */
	public void subscribe(IntConsumer subscriber) {
		spinner.valueProperty().subscribe(newValue -> {
			if (newValue == null)
				return;

			var vf = (IntegerSpinnerValueFactory) spinner.getValueFactory();
			int clamped = Math.clamp(newValue, vf.getMin(), vf.getMax());
			subscriber.accept(clamped);
		});
	}
}
