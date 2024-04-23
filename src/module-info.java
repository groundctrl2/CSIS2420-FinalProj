open module lifeapp {
	exports application;
	exports application.component;
	exports model;

	requires transitive javafx.controls;
	requires transitive javafx.fxml;

	requires transitive algs4_modular;
}