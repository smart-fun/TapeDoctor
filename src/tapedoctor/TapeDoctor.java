/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tapedoctor;

import javafx.application.Application;
//import static javafx.application.Application.launch;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 *
 * @author aguyon
 */
public class TapeDoctor extends Application implements Menus.OnMenuListener {
    
    public static final String version = "0.0.1";
    
    private VBox root;
    
    private WavImage wavImage;
    private double zoomValue = 0;
    
    @Override
    public void start(Stage primaryStage) {
        
        //StackPane root = new StackPane();
        root = new VBox();
        root.getChildren().add(new Menus(primaryStage, this));
        
        addZoomSlider();
        
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
        wavImage = new WavImage(640, 256, wavFile);
        root.getChildren().add(wavImage);
        wavImage.draw(zoomValue);
    }
    
    private void addZoomSlider() {
        Slider slider = new Slider();
        slider.setMin(0);
        slider.setMax(1);
        slider.setValue(zoomValue);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(0.5);
        slider.setMinorTickCount(5);
        slider.setBlockIncrement(0.1);
        root.getChildren().add(slider);
        
        slider.valueProperty().addListener(new ChangeListener() {
        @Override
            public void changed(ObservableValue arg0, Object oldValue, Object newValue) {
                zoomValue = (double) newValue;
                if (wavImage != null) {
                    wavImage.draw(zoomValue);
                }
            }
        });

    }
    
}
