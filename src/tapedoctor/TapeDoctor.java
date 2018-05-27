/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tapedoctor;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 *
 * @author aguyon
 */
public class TapeDoctor extends Application implements Menus.OnMenuListener {
    
    public static final String version = "0.0.1";
    
    @Override
    public void start(Stage primaryStage) {
        Button btn = new Button();
        btn.setText("Say 'Hello World'");
        btn.setOnAction((ActionEvent event) -> {
            System.out.println("Hello World!");
        });
        
        //StackPane root = new StackPane();
        VBox root = new VBox();
        root.getChildren().add(new Menus(primaryStage, this));
        root.getChildren().add(btn);
        
        Scene scene = new Scene(root, 300, 250);
        
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
        if (wavFile.isValid()) {
            
        } else {
            showWavNotValid();
        }
    }
    
    private void showWavNotValid() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("File Error");
        alert.setHeaderText(null);
        alert.setContentText("Wav File is invalid");
        alert.showAndWait();
    }
    
}
