package application;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;

/**
 * An input dialog with text area for editing the stylesheet during runtime.
 * <p>
 * This is accomplished through the use of temp files, so beware of all the
 * issues that this may entail. In particular, resources references in the
 * stylesheet by relative URLs will most likely fail, so it may be necessary
 * to separate resource-based styles into a stylesheet separate from the
 * one being edited.
 */
class LiveStyleEditor {
	private final Parent target;
	private final URL stylesheet;
	private Path tempfile;

	private final Dialog<Void> dialog;
	private final TextArea editor;
	private final Button applier;

	/**
	 * @param target - the UI component whose style is to be edited
	 * @param stylesheet - the location of the the stylesheet
	 */
	LiveStyleEditor(Parent target, URL stylesheet) {
		this.target = Objects.requireNonNull(target, "null target");
		this.stylesheet = Objects.requireNonNull(stylesheet, "null stylesheet");

		// Dialog window containing the editor
		dialog = new Dialog<>();
		dialog.setTitle("Style Editor");
		dialog.setResizable(true);
		// Don't block main window
		dialog.initModality(Modality.NONE);

		// File content editor
		editor = new TextArea();
		editor.setWrapText(true);
		editor.setPrefRowCount(24);

		// Intermediate container, for alignment
		var wrapper = new StackPane();
		wrapper.getChildren().add(editor);
		StackPane.setAlignment(editor, Pos.CENTER);

		// Set dialog content to (wrapped) editor
		var pane = dialog.getDialogPane();
		pane.setContent(wrapper);

		// Add 'apply' button and set handler
		pane.getButtonTypes().add(ButtonType.APPLY);
		applier = (Button) pane.lookupButton(ButtonType.APPLY);

		/*
		 * #addEventFilter is used instead of #setOnAction in order to
		 * prevent the dialog from auto-closing (the handler must consume
		 * the event in order to do so).
		 */
		applier.addEventFilter(ActionEvent.ACTION, this::handleApplyAction);

		/*
		 * Button will initially be disabled when editor is first launched
		 * (see later). Only enable if the content of the text area changes.
		 */
		editor.setTextFormatter(new TextFormatter<Void>(change -> {
			if (change.isContentChange())
				applier.setDisable(false);

			return change;
		}));

	}

	/**
	 * Launches the editor dialog.
	 *
	 * @return {@code true} if successful or already launched,
	 *         {@code false} if stylesheet could not be loaded.
	 */
	boolean launch() {
		if (dialog.isShowing()) {
			Platform.runLater(() -> editor.requestFocus());
			return true;
		}

		String content = getStylesheetContent();

		if (content == null)
			return false;

		editor.setText(content);
		applier.setDisable(true);
		dialog.show();
		return true;
	}

	/**
	 * Applies the new stylesheet to the target if possible.
	 */
	private void handleApplyAction(ActionEvent e) {
		writeToTempFile(editor.getText());

		if (tempfile == null)
			return;

		var stylesheets = target.getStylesheets();

		if (stylesheet != null)
			stylesheets.remove(stylesheet.toString());

		var pathstring = tempfile.toUri().toString();
		stylesheets.remove(pathstring);
		stylesheets.add(pathstring);

		// Prevent dialog from auto-closing.
		e.consume();
	}

	private String getStylesheetContent() {
		/*
		 * Temp file might already exist if editor was launched, closed, and
		 * re-launched.
		 */
		var content = maybeReadFile(tempfile);

		// Otherwise, load from initial stylesheet.
		if (content == null)
			content = maybeReadFile(stylesheet);

		return content;
	}

	private String maybeReadFile(URL file) {
		if (file == null)
			return null;

		try {
			return maybeReadFile(Path.of(file.toURI()));
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	private String maybeReadFile(Path file) {
		if (file == null)
			return null;

		try {
			return Files.readString(file);
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void writeToTempFile(String content) {
		try {
			if (tempfile == null) {
				tempfile = Files.createTempFile("life-app-style-edit", null);
				tempfile.toFile().deleteOnExit();
			}

			Files.writeString(tempfile, content);
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
