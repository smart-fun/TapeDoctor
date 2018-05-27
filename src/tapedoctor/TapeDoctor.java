/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tapedoctor;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 *
 * @author aguyon
 */
public class TapeDoctor extends Application implements Menus.OnMenuListener {
    
    public static final String version = "0.0.1";
    
    private VBox root;
    
    @Override
    public void start(Stage primaryStage) {
        
        //StackPane root = new StackPane();
        root = new VBox();
        root.getChildren().add(new Menus(primaryStage, this));
        
        Scene scene = new Scene(root, 640, 512);
        
        primaryStage.setTitle("Tape Doctor v" + version);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void onWavLoaded(WavFile wavFile) {
        if (wavFile.isSupported()) {
            showWavLoaded(wavFile);
            addWavImage(wavFile);
        } else {
            showWavNotSupported(wavFile);
        }
    }
    
    private void showWavNotSupported(WavFile wavFile) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("File Error");
        if (wavFile.isValid()) {
            alert.setHeaderText("This format is not supported");
            String text = "Only WAV mono is supported\n\n";
            alert.setContentText(text + wavFile.getDisplayInfo());
        } else {
            alert.setHeaderText("This format is not supported");
            alert.setContentText("This is not a WAV file");
        }
        alert.showAndWait();
    }
    
    private void showWavLoaded(WavFile wavFile) {
        String message = "WAV loaded successfully\n\n" + wavFile.getDisplayInfo();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("WAV Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void addWavImage(WavFile wavFile) {
        WavImage wavImage = new WavImage(640, 256, wavFile);
        root.getChildren().add(wavImage);
        wavImage.draw();
    }
    
}
