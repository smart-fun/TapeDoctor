/*
    Copyright 2018 Arnaud Guyon

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
package tapedoctor;

import java.io.File;
import javafx.event.ActionEvent;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

/**
 *
 * @author aguyon
 */
public class Menus extends MenuBar {
    
    final private OnMenuListener listener;
    
    private MenuItem saveItem;
    private WavFile wavFile;
    
    public Menus(Stage stage, Scene scene, OnMenuListener listener) {
        super();
        
        this.listener = listener;
        
        // File Menu
        Menu fileMenu = new Menu("File");
        {
            MenuItem openWavItem = new MenuItem("Open WAV");
            fileMenu.getItems().add(openWavItem);
            
            saveItem = new MenuItem("Save ZX81 .P");
            saveItem.setDisable(true);
            fileMenu.getItems().add(saveItem);
            MenuItem separator = new SeparatorMenuItem();
            MenuItem quitItem = new MenuItem("Quit");
            fileMenu.getItems().addAll(separator, quitItem);
            
            openWavItem.setOnAction((ActionEvent actionEvent) -> {
                openWavFile(stage, scene);
            });
                        
            quitItem.setOnAction((ActionEvent actionEvent) -> {
                System.exit(0);
            });
            
            saveItem.setOnAction((ActionEvent actionEvent) -> {
                listener.onSavePressed(wavFile);
            });
        }
        
        // Help Menu
        Menu helpMenu = new Menu("Help");
        {
            MenuItem aboutItem = new MenuItem("About");
            helpMenu.getItems().addAll(aboutItem);
            
            aboutItem.setOnAction((ActionEvent actionEvent) -> {
                showAboutDialog();
            });
        }
        
        getMenus().addAll(fileMenu, helpMenu);
    }
    
    public void setCanSave(boolean canSave) {
        saveItem.setDisable(!canSave);
    }
    
    private void showAboutDialog() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText(null);
        String text = "Tape Doctor v" + TapeDoctor.VERSION + "\n© 2018 Arnaud Guyon";
        text += "\n\nReads ZX81 Programs recorded in WAV format, and converts them into .p files for ZX81 emulators.";
        alert.setContentText(text);
        alert.showAndWait();
    }
    
    private void openWavFile(Stage stage, Scene scene) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open ZX81 WAV File");
        fileChooser.getExtensionFilters().addAll(
            new ExtensionFilter("WAV Files", "*.wav"),
            new ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            System.out.println("Wav Selected: " + selectedFile.getAbsolutePath());
            
            scene.setCursor(Cursor.WAIT);
            wavFile = new WavFile(selectedFile);
            scene.setCursor(Cursor.DEFAULT);

            listener.onWavLoaded(wavFile);            
        }
    }
    
    public interface OnMenuListener {
        void onWavLoaded(WavFile wavFile);
        void onSavePressed(WavFile wavFile);
    }
    
}
