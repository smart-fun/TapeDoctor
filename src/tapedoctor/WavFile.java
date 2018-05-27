/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tapedoctor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author aguyon
 */
public class WavFile {
    
    private byte[] fileBuffer;
    private String fileName;
    
    // Header information
    private long fileSize;
    private int numChannels;    // 1 mono, 2 stereo
    private int sampleRate;     // 22050, 44100, 48000
    private int byteRate;       // 176400 = (Sample Rate * BitsPerSample * Channels) / 8
    private int bitType;        // (BitsPerSample * Channels) / 8 => 1:8 bit mono, 2: 8 bit stereo or 16 bit mono, 4: 16 bit stereo
    private int bitsPerSample;  // 8, 16
    private long dataSize;      // NumSamples * NumChannels * BytesPerSample(1 or 2)
    
    // other information
    private boolean isWavFile;
    private int bytesPerSample; // 1 or 2
    private int numSamples;     // a sample can be mono or stereo
    
    public WavFile(File file) {
        
        fileName = file.getAbsolutePath();
        try {
            fileBuffer = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        } catch (IOException ex) {
            Logger.getLogger(WavFile.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        isWavFile = isWavFile();
        if (isWavFile) {
            // Read WAV Header
            fileSize = 8 + ByteHelper.getInt4(fileBuffer, 4);
            numChannels = ByteHelper.getInt2(fileBuffer, 22);
            sampleRate = (int) ByteHelper.getInt4(fileBuffer, 24);
            byteRate = (int) ByteHelper.getInt4(fileBuffer, 28);
            bitType = (int) ByteHelper.getInt2(fileBuffer, 32);
            bitsPerSample = (int) ByteHelper.getInt2(fileBuffer, 34);
            dataSize = ByteHelper.getInt4(fileBuffer, 40);
            
            bytesPerSample = bitsPerSample / 8;
            numSamples = (int) (dataSize / (numChannels * bytesPerSample));
            
            System.out.println("WavFile init done");
        }
        
    }
    
    private boolean isWavFile() {
        boolean isRiff = ByteHelper.equalsString(fileBuffer, 0, "RIFF");
        boolean isWave = ByteHelper.equalsString(fileBuffer, 8, "WAVE");
        boolean hasData = ByteHelper.equalsString(fileBuffer, 36, "data");
        return isRiff && isWave && hasData;        
    }
    
    final public boolean isSupported() {
        return isWavFile && (numChannels == 1);
    }
    
    final public boolean isValid() {
        return isWavFile;
    }
    
    public int getNumSamples() {
        return numSamples;
    }
    
    public String getDisplayInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append(fileName);
        builder.append("\n");
        if (numChannels == 1) {
            builder.append("Mono\n");
        } else if (numChannels == 2) {
            builder.append("Stereo\n");
        }
        builder.append(sampleRate);
        builder.append(" Hz\n");
        builder.append(bitsPerSample);
        builder.append(" bits");
        return builder.toString();
    }
    
    // TODO: this is 8 bits only !!
    public double getSampleValue(int sampleNumber) {
        if ((sampleNumber < 0) || (sampleNumber >= numSamples)) {
            return 0;
        }
        byte rough = fileBuffer[sampleNumber + 44];
        double value = (rough >= 0) ? rough : rough + 256;
        value = value / 256;
        return value - 0.5;
    }
    
}
