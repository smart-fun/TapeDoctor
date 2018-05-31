/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tapedoctor;

import java.io.File;
import java.nio.ByteOrder;
import javafx.application.Application;
//import static javafx.application.Application.launch;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import tapedoctor.WavFile.MissingBitInfo;

/**
 *
 * @author aguyon
 */
public class TapeDoctor extends Application implements Menus.OnMenuListener {
    
    public static final String version = "0.0.1";
    
    private VBox root;
    private Stage stage;
    
    private Menus menus;
    private Slider zoomSlider;
    private Slider offsetSlider;
    private WavImage wavImage;
    
    private HBox errorControlBox;
    
    private double zoomValue = 0;
    private double offsetValue = 0;
    
    private int screenWidth = 640;
    
    private Label currentErrorSize;
    
    @Override
    public void start(Stage primaryStage) {
        
        stage = primaryStage;
        //StackPane root = new StackPane();
        root = new VBox();
        menus = new Menus(primaryStage, this);
        root.getChildren().add(menus);
        
        screenWidth = (int) Screen.getPrimary().getVisualBounds().getWidth();
        
        Scene scene = new Scene(root, screenWidth, 450);
        
        primaryStage.setTitle("Tape Doctor v" + version);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        boolean bigEndian = (ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN));
        if (bigEndian) {
            System.out.println("BIG ENDIAN SYSTEM");
        } else {
            System.out.println("LITTLE ENDIAN SYSTEM"); // win7 intel i5, Mac intel i7
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void onWavLoaded(WavFile wavFile) {
        
        menus.displayApplyFixes(false);
        
        if (zoomSlider != null) {
            root.getChildren().remove(zoomSlider);
            zoomSlider = null;
        }
        if (offsetSlider != null) {
            root.getChildren().remove(offsetSlider);
            offsetSlider = null;
        }
        if (wavImage != null) {
            root.getChildren().remove(wavImage);
            wavImage = null;                
        }
        if (errorControlBox != null) {
            root.getChildren().remove(errorControlBox);
            errorControlBox = null;
        }
        if (wavFile.isSupported()) {
            menus.setCanSave(true);
            showWavLoaded(wavFile);
            addZoomSlider();
            addWavImage(wavFile);
            addOffsetSlider();
            if (wavFile.hasRecoveryErrors()) {
                menus.displayApplyFixes(true);
                errorControlBox = new HBox();
                errorControlBox.setSpacing(10.0);
                errorControlBox.setAlignment(Pos.CENTER);
                root.getChildren().add(errorControlBox);
                updateErrorControlBox(wavFile);
            }            
            stage.setTitle(wavFile.getFileName());
        } else {
            menus.setCanSave(false);
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
        wavImage = new WavImage(screenWidth, 256, wavFile);
        root.getChildren().add(wavImage);
        if (wavFile.getNumErrors() > 0) {
            wavImage.jumpToNextError(); // draw is already called here
        } else {
            wavImage.draw();
        }
        
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
                    wavImage.setZoom(zoomValue);
                    wavImage.draw();
                }
            }
        });
        zoomSlider = slider;
    }
    
    private void addOffsetSlider() {
        Slider slider = new Slider();
        slider.setMin(0);
        slider.setMax(100);
        slider.setValue(offsetValue);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        //slider.setMajorTickUnit(50);
        //slider.setMinorTickCount(5);
        //slider.setBlockIncrement(10);
        root.getChildren().add(slider);
        
        slider.valueProperty().addListener(new ChangeListener() {
        @Override
            public void changed(ObservableValue arg0, Object oldValue, Object newValue) {
                offsetValue = (double) newValue;
                if (wavImage != null) {
                    wavImage.setOffsetPercent(offsetValue);
                    wavImage.draw();
                }
            }
        });
        
        offsetSlider = slider;
    }

    @Override
    public void onSavePressed(WavFile wavFile) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Program");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(".P Programs", "*.p"),
            new FileChooser.ExtensionFilter("All Files", "*.*"));
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            wavFile.save(file);
        }
    }

    @Override
    public void onApplyFixes(WavFile wavFile) {
        wavFile.applyFixes();
        wavImage.draw();
        updateErrorControlBox(wavFile);
    }
    
    private void updateErrorControlBox(WavFile wavFile) {
        
        errorControlBox.getChildren().clear();
        
        Button previous = new Button("<<");
        previous.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                wavImage.jumpToPreviousError();
                updateCurrentErrorData(wavFile);
            }
        });
        errorControlBox.getChildren().add(previous);

        Button next = new Button(">>");
        next.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                wavImage.jumpToNextError();
                updateCurrentErrorData(wavFile);
            }
        });
        errorControlBox.getChildren().add(next);
        
        currentErrorSize = new Label("");
        currentErrorSize.setMinWidth(200);

        //currentErrorSize.setBoundsType(TextBoundsType.VISUAL);
        errorControlBox.getChildren().add(currentErrorSize);
        
        Button set0 = new Button("Set 0 bit");
        set0.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                forceBit(wavFile, 0);
                updateCurrentErrorData(wavFile);
            }
        });
        errorControlBox.getChildren().add(set0);
        
        Button set1 = new Button("Set 1 bit");
        set1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                forceBit(wavFile, 1);
                updateCurrentErrorData(wavFile);
            }
        });
        errorControlBox.getChildren().add(set1);
        
        Button applyForceBit = new Button("Apply Now");
        applyForceBit.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                boolean removed = wavFile.applyForceBit(wavImage.getCurrentError());
                if (removed) {
                    wavImage.unselectCurrent();
                } else {
                    wavImage.jumpToCurrentError(wavFile.getMissingBits());
                }
                updateCurrentErrorData(wavFile);
                wavImage.draw();
            }
        });
        errorControlBox.getChildren().add(applyForceBit);
        
        Button deleteZone = new Button("DELETE!");
        deleteZone.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int currentError = wavImage.getCurrentError();
                wavFile.deleteMissingBitArea(currentError);
                wavImage.unselectCurrent();
                updateCurrentErrorData(wavFile);
                wavImage.draw();
            }
        });
        errorControlBox.getChildren().add(deleteZone);
        
        updateCurrentErrorData(wavFile);
        
    }
    
    private void updateCurrentErrorData(WavFile wavFile) {
        
        int currentError = wavImage.getCurrentError();
        MissingBitInfo missingBitInfo = wavFile.getMissingBit(currentError);
        if (missingBitInfo != null) {
            int displayIndex = currentError + 1;
            int numErrors = wavFile.getNumErrors();
            String text = " ERROR " + displayIndex + " / " + numErrors + " \n";
            text += " " + missingBitInfo.offsetStart + " -> " + missingBitInfo.offsetEnd + " ";
            currentErrorSize.setText(text);
        } else {
            currentErrorSize.setText("");
        }
    }
    
    private void forceBit(WavFile wavFile, int value) {
        int currentError = wavImage.getCurrentError();
        MissingBitInfo missingBitInfo = wavFile.getMissingBit(currentError);
        if (missingBitInfo != null) {
            missingBitInfo.forcedValues.clear();
            missingBitInfo.forcedValues.add(value);
            wavImage.draw();
        }
    }
    
}
