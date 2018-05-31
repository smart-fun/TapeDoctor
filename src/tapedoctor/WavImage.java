/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tapedoctor;

import java.util.ArrayList;
import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 *
 * @author aguyon
 */
public class WavImage extends Canvas {
    
    private static final Color RED_TRANSP = new Color(1,0,0, 0.3);
    private static final Color GREEN_TRANSP = new Color(0,1,0, 0.3);
    
    private WavFile wavFile;
    private double displayOffset = 0;
    private int currentError = -1;
    private OnWavImageListener listener;
    private double dragPreviousX = 0;
    
    public WavImage(int width, int height, WavFile wavFile, OnWavImageListener listener) {
        super(width, height);
        
        this.wavFile = wavFile;
        this.listener = listener;
        
        this.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                boolean click = event.isPrimaryButtonDown();
                if (click) {
                    double x = event.getX();
                    double move = x - dragPreviousX;
                    dragPreviousX = x;
                    displayOffset -= move;
                    if (displayOffset < 0) {
                        displayOffset = 0;
                    } else if (displayOffset >= wavFile.getNumSamples() - width) {
                        displayOffset = wavFile.getNumSamples() - width;                        
                    }
                    double offsetPercent = offsetToPercent(displayOffset);
                    listener.onWavMovedPercent(offsetPercent);
                    draw();
                } else {
                    dragPreviousX = event.getX();
                }
            }
        });
        
    }
    
    private double offsetToPercent(double offset) {
        return (100*offset) / (wavFile.getNumSamples() - getWidth());
    }
    private double percentToOffset(double percent) {
        return percent * (wavFile.getNumSamples() - getWidth()) / 100;
    }
    
    public int getCurrentError() {
        return currentError;
    }
    
    // returns true if changes because out of bounds
    public boolean checkCurrentErrorChange() {
        ArrayList<WavFile.MissingBitInfo> missingBits = wavFile.getMissingBits();
        if (currentError >= missingBits.size()) {
            currentError = missingBits.size() - 1;
            return true;
        }
        return false;
    }
    
    public void jumpToPreviousError() {
        ArrayList<WavFile.MissingBitInfo> missingBits = wavFile.getMissingBits();
        if (!missingBits.isEmpty()) {
            --currentError;
            if (currentError < 0) {
                currentError = missingBits.size() - 1;
            }
            jumpToCurrentError(missingBits);
        }
    }
    public void jumpToNextError() {
        ArrayList<WavFile.MissingBitInfo> missingBits = wavFile.getMissingBits();
        if (!missingBits.isEmpty()) {
            ++currentError;
            if (currentError >= missingBits.size()) {
                currentError =  0;
            }
            jumpToCurrentError(missingBits);
        }
    }
    
    public void jumpToCurrentError(ArrayList<WavFile.MissingBitInfo> missingBits) {
        if ((currentError >= 0) && (currentError < missingBits.size())) {
            WavFile.MissingBitInfo firstError = missingBits.get(currentError);
            displayOffset = firstError.offsetStart - (getWidth() * 0.4);
            draw();
            double percent = offsetToPercent(displayOffset);
            listener.onWavMovedPercent(percent);
        }
    }
    
    public void unselectCurrent() {
        currentError = -1;
    }
    
    public void setOffset(int offsetValue) {
        displayOffset = offsetValue;
    }
    
    // 0-100
    public void setOffsetPercent(double offsetPercent) {
        displayOffset = percentToOffset(offsetPercent);
    }
    
    public double getDisplayOffset() {
        return displayOffset;
    }

    public double getDisplayPercent() {
        return offsetToPercent(displayOffset);
    }
    
    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        
        double width = getWidth();
        double height = getHeight();
        double midHeight = height / 2;
        
        gc.beginPath();        
        
        // Background
        gc.setFill(Color.WHITE);
        gc.fillRect(0,0,width,height);
        
        // Horizontal line
        gc.setStroke(Color.LIGHTGREY);
        gc.setLineWidth(1.0);
        gc.moveTo(0, midHeight);
        gc.lineTo(width, midHeight);
        
        gc.setStroke(Color.BLACK);
        gc.moveTo(0, midHeight);
        
        double position = displayOffset;
        for(int x=0; x<width; ++x) {
            double value = wavFile.getSampleValue((int) position);
            ++position;
            double y = midHeight - (value * height);
            gc.lineTo(x, y);
            
            if (wavFile.isHighPeak((int) position)) {
                gc.moveTo(x, 0);
                gc.lineTo(x, Math.max(midHeight * 0.15, y));
            } else if (wavFile.isLowPeak((int) position)) {
                gc.moveTo(x, height);
                gc.lineTo(x, Math.min(height * 0.9, y));
            }
            gc.moveTo(x, y);
        }
        
        gc.stroke();
        
        gc.closePath();
        
        showErrors(gc);        
    }
    
    private void showErrors(GraphicsContext gc) {
        
        ArrayList<WavFile.MissingBitInfo> missingBits = wavFile.getMissingBits();
        
        double screenWidth = getWidth();
        double HalfHeight = getHeight() / 2;
        double offsetStart = displayOffset;
        double offsetEnd = offsetStart + screenWidth;
        
        if (missingBits.size() > 0) {
            gc.beginPath(); 
            gc.setStroke(Color.RED);
            
            // display missing bits areas
            for(WavFile.MissingBitInfo info : missingBits) {
                int start = info.offsetStart;
                if (start >= offsetEnd) {
                    continue;
                }
                int end = info.offsetEnd;
                if (end <= offsetStart) {
                    continue;
                }
                
                double leftOffset = start-offsetStart;
                double leftPixel = leftOffset;

                double rightOffset = end-offsetStart;
                double rightPixel = rightOffset;
                double pixelWidth = rightPixel - leftPixel;
                
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                gc.fillText(info.offsetStart + " -> " + info.offsetEnd, leftPixel + 20, HalfHeight * 0.1);

                if (info.forcedValues.isEmpty()) {
                    gc.setFill(RED_TRANSP);
                    gc.fillRect(leftPixel-1, 0, pixelWidth+2, HalfHeight*2);    // Red on all height
                } else {
                    gc.setFill(RED_TRANSP);
                    gc.fillRect(leftPixel-1, 0, pixelWidth+2, HalfHeight);      // Red on half height

                    for(Integer bitValue : info.forcedValues) {
                        double bitSize = (bitValue == 0) ? wavFile.get0bitSize() : wavFile.get1bitSize();
                        rightOffset = leftOffset + bitSize;
                        rightPixel = rightOffset;
                        pixelWidth = rightPixel - leftPixel;

                        int value = info.forcedValues.get(0);
                        gc.setFill(Color.BLACK);
                        gc.fillText("Forced to " + value, leftPixel + 20, HalfHeight * 1.2);

                        gc.setFill(GREEN_TRANSP);
                        gc.fillRect(leftPixel-1, HalfHeight, pixelWidth+2, HalfHeight);

                        leftOffset = rightOffset;
                    }
                    
                }                                        

            }
            //gc.stroke();
            gc.closePath();
        }
            
        // display Bytes
        gc.beginPath();

        ArrayList<WavFile.ByteInfo> bytes = wavFile.getBytes();
        for(WavFile.ByteInfo byteInfo : bytes) {
            double leftOffset = byteInfo.offsetStart - offsetStart;
            double leftPixel = leftOffset;

            double rightOffset = byteInfo.offsetEnd - offsetStart;
            double rightPixel = rightOffset;

            double pixelWidth = rightPixel - leftPixel;
            if (leftPixel + pixelWidth <= 0) {
                continue;
            }
            if (leftPixel > screenWidth) {
                continue;
            }
            gc.setFill(new Color(1,1,0, 0.3));
            gc.fillRect(leftPixel, HalfHeight * 1.6, pixelWidth, HalfHeight*2);

            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            int printx = (int) (leftPixel + 20);
            do {
                gc.fillText("" + byteInfo.value, printx, HalfHeight * 1.75);
                printx += 400;
            } while (printx < rightPixel - 50);
        }

        gc.closePath();

        // display bits
        gc.beginPath();

        ArrayList<WavFile.BitInfo> bits = wavFile.getBits();
        for(WavFile.BitInfo bitInfo : bits) {
            double leftOffset = bitInfo.offsetStart - offsetStart;
            double leftPixel = leftOffset;

            double rightOffset = bitInfo.offsetEnd - offsetStart;
            double rightPixel = rightOffset;

            double pixelWidth = rightPixel - leftPixel;
            if (leftPixel + pixelWidth <= 0) {
                continue;
            }
            if (leftPixel > screenWidth) {
                continue;
            }

            gc.setFill(new Color(0,0,1, 0.3));
            gc.fillRect(leftPixel, HalfHeight * 1.8, pixelWidth, HalfHeight*2); 

            // display Bit value
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
            gc.fillText("" + bitInfo.value, leftPixel + 20, HalfHeight * 1.95);

        }                

        gc.closePath();

    }
    
    public interface OnWavImageListener {
        void onWavMovedPercent(double offsetPercent);
    }
    
}
