/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tapedoctor;

import java.io.File;
import java.nio.ByteOrder;
import javafx.application.Application;
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
import javafx.scene.control.Tooltip;
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
public class TapeDoctor extends Application implements Menus.OnMenuListener, WavImage.OnWavImageListener {
    
    public static final String VERSION = "0.8.0";
    
    private VBox root;
    private Stage stage;
    private Scene scene;
    
    private Menus menus;
    private Slider offsetSlider;
    private boolean justForcedOffsetSlider = false;
    private WavImage wavImage;
    
    private HBox errorControlBox;
        
    private int screenWidth = 640;
    
    private Label currentErrorSize;
    
    @Override
    public void start(Stage primaryStage) {
        
        screenWidth = (int) Screen.getPrimary().getVisualBounds().getWidth();

        stage = primaryStage;
        root = new VBox();
        scene = new Scene(root, screenWidth, 400);

        menus = new Menus(primaryStage, scene, this);
        root.getChildren().add(menus);        
        
        primaryStage.setTitle("Tape Doctor v" + VERSION);
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
        
        menus.setCanSave(false);
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
            addWavImage(wavFile);
            addOffsetSlider();
            wavFile.applyAutoFixes();
            wavImage.draw();
            if (wavFile.getNumErrors() > 0) {                
                errorControlBox = new HBox();
                errorControlBox.setSpacing(10.0);
                errorControlBox.setAlignment(Pos.CENTER);
                root.getChildren().add(errorControlBox);
                updateErrorControlBox(wavFile);
            } else {
                menus.setCanSave(true);
            }
            String title = wavFile.getFileName();
            String programName = wavFile.getProgramName();
            if (programName.length() > 0) {
                title += " (" + programName + ")";
            }
            stage.setTitle(title);
            showWavLoaded(wavFile);

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
        wavImage = new WavImage(screenWidth, 256, wavFile, this);
        root.getChildren().add(wavImage);
        if (wavFile.getNumErrors() > 0) {
            wavImage.jumpToNextError(); // draw is already called here
        } else {
            wavImage.setOffset(wavFile.getFirstByteOffset());
            wavImage.draw();
        }
    }
    
    private void addOffsetSlider() {
        Slider slider = new Slider();
        slider.setMin(0);
        slider.setMax(100);
        slider.setValue(wavImage.getDisplayPercent());
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        //slider.setMajorTickUnit(50);
        //slider.setMinorTickCount(5);
        //slider.setBlockIncrement(10);
        root.getChildren().add(slider);
        
        slider.valueProperty().addListener(new ChangeListener() {
        @Override
            public void changed(ObservableValue arg0, Object oldValue, Object newValue) {
                double offsetValue = (double) newValue;
                if (wavImage != null && !justForcedOffsetSlider) {
                    wavImage.setOffsetPercent(offsetValue);
                    wavImage.draw();
                }
                justForcedOffsetSlider = false;
            }
        });
        
        offsetSlider = slider;
    }

    @Override
    public void onSavePressed(WavFile wavFile) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Program");
        String programName = wavFile.getProgramName();
        if (programName.length() > 0) {
            fileChooser.setInitialFileName(programName + ".p");
        }
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(".P Programs", "*.p"),
            new FileChooser.ExtensionFilter("All Files", "*.*"));
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            boolean success = wavFile.save(file);
            displayFileSaved(success, wavFile);
        }
    }
    
    private void displayFileSaved(boolean success, WavFile wavFile) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        if (success) {
            alert.setTitle("File Saved");
            alert.setHeaderText(null);
            alert.setContentText("The file has been correctly saved");
        } else {
            alert.setTitle("File Error");
            alert.setHeaderText("The file has not been saved correctly");
            alert.setContentText("A problem occurred, please try again later");
        }
        alert.showAndWait();
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
        previous.setTooltip(new Tooltip("Goes to previous Error"));

        Button next = new Button(">>");
        next.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                wavImage.jumpToNextError();
                updateCurrentErrorData(wavFile);
            }
        });
        errorControlBox.getChildren().add(next);
        next.setTooltip(new Tooltip("Goes to next Error"));
        
        currentErrorSize = new Label("");
        currentErrorSize.setMinWidth(150);

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
        set0.setTooltip(new Tooltip("Forces the first bit of the Error to 0"));
        
        Button set1 = new Button("Set 1 bit");
        set1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                forceBit(wavFile, 1);
                updateCurrentErrorData(wavFile);
            }
        });
        errorControlBox.getChildren().add(set1);
        set1.setTooltip(new Tooltip("Forces the first bit of the Error to 1"));
        
        Button applyForceBit = new Button("Apply bit set");
        applyForceBit.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                boolean removed = wavFile.applyForceBit(wavImage.getCurrentError());
                int numErrors = wavFile.getNumErrors();
                if (numErrors > 0) {
                    wavImage.jumpToCurrentError(wavFile.getMissingBits());
                } else {
                    wavImage.unselectCurrent();
                }
                /*if (removed) {
                    wavImage.unselectCurrent();
                } else {
                    wavImage.jumpToCurrentError(wavFile.getMissingBits());
                }*/
                updateCurrentErrorData(wavFile);
                wavImage.draw();
                if (wavFile.getNumErrors() == 0) {
                    menus.setCanSave(true);
                }
            }
        });
        errorControlBox.getChildren().add(applyForceBit);
        applyForceBit.setTooltip(new Tooltip("Applies the bit set (0 or 1) to the curve"));
        
        Button deleteZone = new Button("DELETE");
        deleteZone.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int currentError = wavImage.getCurrentError();
                wavFile.deleteMissingBitArea(currentError);
                //wavImage.unselectCurrent();
                int numErrors = wavFile.getNumErrors();
                if (numErrors > 0) {
                    wavImage.jumpToCurrentError(wavFile.getMissingBits());
                } else {
                    wavImage.unselectCurrent();
                }
                updateCurrentErrorData(wavFile);
                wavImage.draw();
                if (wavFile.getNumErrors() == 0) {
                    menus.setCanSave(true);
                }
            }
        });
        errorControlBox.getChildren().add(deleteZone);
        deleteZone.setTooltip(new Tooltip("Deletes the error (some bits may be missing)"));
        
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

    @Override
    public void onWavMovedPercent(double offsetPercent) {
        if (offsetSlider != null) {
            justForcedOffsetSlider = true;
            offsetSlider.setValue(offsetPercent);
        }
    }

}
