package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LifeApp extends Application {
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;

	@Override
	public void start(Stage stage) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("LifeView.fxml"));
			Parent root = loader.load();
			ViewController control = (ViewController) loader.getController();
			Scene scene = new Scene(root, WIDTH, HEIGHT);
			stage.setTitle("Version 0.1");
			stage.setScene(scene);
			stage.show();

			control.drawGrid();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}
