package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
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

            drawSomething(control.canvasOfLife);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}

	private void drawSomething(Canvas canvas) {
	    double w = canvas.getWidth();
	    double h = canvas.getHeight();
	    System.out.println(w + ", " + h);

	    var g = canvas.getGraphicsContext2D();
	    g.setFill(Color.SPRINGGREEN);
	    g.fillRect(10, 10, w - 20, h - 20);
	}
}
