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
 *
 * @see https://stackoverflow.com/q/31248983
 */
public class SpinnerBox extends HBox {
	public final Spinner<Integer> spinner;
	public final Label label;

	// Annotations needed to expose constructor to FXML
	public SpinnerBox(@NamedArg("min") int min,
	                  @NamedArg("max") int max,
	                  @NamedArg("value") int value,
	                  @NamedArg("label") String label) {
		this.spinner = new Spinner<>(min, max, value);
		this.spinner.setEditable(true);
		this.label = new Label(label);

		addInputValidator(min, max, value);

		this.getChildren().addAll(this.label, this.spinner);
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
				return change;

			try {
				Integer.parseInt(text);
			} catch (NumberFormatException e) {
				return null; // reject change
			}

			return change;
		}));
	}

	/* Convenience methods */

	public int getValue() { return spinner.getValue(); }
	public void setValue(int value) { spinner.getValueFactory().setValue(value); }

	public void setOnAction(IntConsumer subscriber) {
		spinner.valueProperty().subscribe(newValue -> {
			if (newValue == null)
				return;

			var vf = (IntegerSpinnerValueFactory) spinner.getValueFactory();
			int clamped = Math.max(vf.getMin(), Math.min(newValue, vf.getMax()));
			subscriber.accept(clamped);
		});
	}
}
