/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tapedoctor;

import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 *
 * @author aguyon
 */
public class Menus extends MenuBar {
    
    public Menus() {
        super();
        
        // File Menu
        Menu fileMenu = new Menu("File");
        {
            MenuItem openWavItem = new MenuItem("Open WAV");
            MenuItem separator = new SeparatorMenuItem();
            MenuItem quitItem = new MenuItem("Quit");
            fileMenu.getItems().addAll(openWavItem, separator, quitItem);
            
            quitItem.setOnAction((ActionEvent actionEvent) -> {
                System.exit(0);
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
    
    private void showAboutDialog() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText(null);
        alert.setContentText("Tape Doctor v" + TapeDoctor.version + "\n© 2018 Arnaud Guyon");
        alert.showAndWait();
    }
}
