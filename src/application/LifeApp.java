package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;

	@Override
	public void start(Stage stage) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("LifeView.fxml"));
			Parent root = loader.load();
			Scene scene = new Scene(root, WIDTH, HEIGHT);
			stage.setTitle("Version 0.1");
			stage.setScene(scene);
			stage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}
