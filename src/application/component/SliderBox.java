package application.component;

import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;

/**
 * Reusable custom component that groups a slider, a caption, and a value label
 * into an {@link HBox}. For use in FXML and controller classes.
 */
public class SliderBox extends HBox { // abuse of inheritance
	public final Slider slider = new Slider();
	public final Label caption = new Label();
	public final Label valueLabel = new Label();

	public SliderBox() {
		slider.setBlockIncrement(1.0);
		caption.setLabelFor(slider);
		valueLabel.setLabelFor(slider);
		valueLabel.textProperty().bind(slider.valueProperty().map(v -> String.valueOf(v.intValue())));

		this.getStyleClass().add("slider-box");
		slider.getStyleClass().add("slider");
		caption.getStyleClass().add("caption");
		valueLabel.getStyleClass().add("value-label");

		this.getChildren().addAll(caption, slider, valueLabel);
	}

	// ---------------------------------------------
	// Make certain "properties" available in FXML
	// ---------------------------------------------

	public int getValue() { return (int) slider.getValue(); }
	public void setValue(int value) { slider.setValue(value); }

	public int getMin() { return (int) slider.getMin(); }
	public void setMin(int value) { slider.setMin(value); }

	public int getMax() { return (int) slider.getMax(); }
	public void setMax(int value) { slider.setMax(value); }

	public String getCaptionText() { return caption.getText(); }
	public void setCaptionText(String text) { caption.setText(text); }
}
