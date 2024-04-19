package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Application launcher. To follow the program structure:
 *
 * <ol>
 *   <li>
 *      Start with <a href="LifeView.fxml">LifeView.fxml</a> for the general
 *      layout (the scene graph) of the GUI.
 *   </li>
 *   <li>
 *      Then see <a href="styles.css">styles.css</a> for the stylesheet.
 *   </li>
 *   <li>
 *      Finally, refer to {@link ViewController} for the behavior of the UI.
 *   </li>
 * </ol>
 *
 * @see #main
 */
public class LifeApp extends Application {
	@Override
	public void start(Stage stage) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("LifeView.fxml"));
			Parent root = loader.load();
			Scene scene = new Scene(root);

			/*
			 * Restrict window size to maximum allowance.
			 * While the user can't manually oversize the window, the window
			 * may overshoot the visual bounds if the scene contains an oversized
			 * node (in this case, the main canvas).
			 */
			Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
			stage.setMaxWidth(bounds.getWidth());
			stage.setMaxHeight(bounds.getHeight());

			stage.setTitle("Version 1.0");
			stage.setScene(scene);
			// Close any other windows if the main stage is closed.
			stage.setOnHidden(e -> Platform.exit());
			stage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}
