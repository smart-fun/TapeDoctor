/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

// TODO: check if 4 peaks is 0 and 9 peaks is 1, or the contrary
package tapedoctor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author aguyon
 */
public class WavFile {
    
    private static final double PEAK_THRESHOLD = 0.01;
    private static final double WAVE_AMPLITUDE = 0.6;
    private static final double BAUDS = 400;
    
    private byte[] fileBuffer;
    private File file;
    
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
    private double[] convertedSamples;
    private HashSet<Integer> hiPeaks = new HashSet<>();
    private HashSet<Integer> loPeaks = new HashSet<>();
    double hiPeakAvg = 0;
    double loPeakAvg = 0;
    
    private static class BitInfo {
        int offset;
        int value;
        private BitInfo(int offset, int value) {
            this.offset = offset;
            this.value = value;
        }
    }
    private ArrayList<BitInfo> bitsArray = new ArrayList<>(16384 * 8);  // 16K default
    
    private static class ByteInfo {
        int offset;
        int value;
        private ByteInfo(int offset, int value) {
            this.offset = offset;
            this.value = value;
        }
    }
    private ArrayList<ByteInfo> byteArray = new ArrayList<>(16384);  // 16K default
        
    public WavFile(File file) {
        
        this.file = file;
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
            
            convertSamples();
            searchPeaks();
            System.out.println("First Pass Peaks, Low Average: " + loPeakAvg + ", High Average: " + hiPeakAvg);
            resample();
            //resampleDynamic();
            System.out.println("Second Pass Peaks, Low Average: " + loPeakAvg + ", High Average: " + hiPeakAvg);
            
            
            findBits();
            findBytes();
            
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
    
    public String getFileName() {
        return file.getName();
    }
    
    public boolean isHighPeak(int position) {
        return hiPeaks.contains(position);
    }
    public boolean isLowPeak(int position) {
        return loPeaks.contains(position);
    }
    
    public String getDisplayInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append(file.getAbsolutePath());
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
    
    public double getSampleValue(int sampleNumber) {
        return convertedSamples[sampleNumber];
    }
    
    private double getSample8bitsValue(int sampleNumber) {
        if ((sampleNumber < 0) || (sampleNumber >= numSamples)) {
            return 0;
        }
        byte rough = fileBuffer[sampleNumber + 44];
        double value = (rough >= 0) ? rough : rough + 256;
        value = value / 256;
        return value - 0.5;
    }

    private double getSample16bitsValue(int sampleNumber) {
        if ((sampleNumber < 0) || (sampleNumber >= numSamples)) {
            return 0;
        }
        int offset = 44 + (sampleNumber * 2);
        int rough = ByteHelper.getInt2(fileBuffer, offset);
        if (rough > 32768) {
            rough -= 65536;
        }
        double value = rough;        
        value = value / 65536.0;
        return value;
    }
    
    private void convertSamples() {
        convertedSamples = new double[numSamples];
        if (numChannels == 1) {
            for(int pos=0; pos<numSamples; ++pos) {
               if (bitsPerSample == 8) {
                   convertedSamples[pos] = getSample8bitsValue(pos);
               } else if (bitsPerSample == 16) {
                    convertedSamples[pos] = getSample16bitsValue(pos);
                }
            }
        }
    }
    
    private void searchPeaks() {
        hiPeaks.clear();
        loPeaks.clear();
        int peakOffset = 0;
        double peakValue = convertedSamples[0];
        boolean increasing = convertedSamples[1] > convertedSamples[0];
        for(int pos=1; pos<numSamples; ++pos) {
            double currentValue = convertedSamples[pos];
            if (increasing) {
                if (currentValue > peakValue) {
                    peakValue = currentValue;
                    peakOffset = pos;
                } else {
                    if (currentValue < peakValue - PEAK_THRESHOLD) {
                        hiPeaks.add(peakOffset);
                        if (hiPeakAvg == 0) {
                            hiPeakAvg = peakValue;
                        } else {
                            int numPeaks = hiPeaks.size();
                            hiPeakAvg = ((hiPeakAvg * (numPeaks-1)) + peakValue) / numPeaks;
                        }
                        increasing = false;
                        peakOffset = pos;
                        peakValue = currentValue;
                    }
                }
            } else {    // decreasing
                if (currentValue < peakValue) {
                    peakValue = currentValue;
                    peakOffset = pos;
                } else {
                    if (currentValue > peakValue + PEAK_THRESHOLD) {
                        loPeaks.add(peakOffset);
                        if (loPeakAvg == 0) {
                            loPeakAvg = peakValue;
                        } else {
                            int numPeaks = loPeaks.size();
                            loPeakAvg = ((loPeakAvg * (numPeaks-1)) + peakValue) / numPeaks;
                        }
                        increasing = true;
                        peakOffset = pos;
                        peakValue = currentValue;                        
                    }
                }
            }
        }
    }
    
    // resample with peaks information
    private void resample() {
        double amplitude = hiPeakAvg - loPeakAvg;
        double multiplier = WAVE_AMPLITUDE / amplitude;
        for(int pos=0; pos<numSamples; ++pos) {
            double currentValue = convertedSamples[pos];
            currentValue = currentValue * multiplier;
            convertedSamples[pos] = currentValue;
        }
    }
    
    // resample with peaks information
    /*
    private void resampleDynamic() {
        double hiPeakSmooth = hiPeakAvg;
        double loPeakSmooth = loPeakAvg;
        for(int pos=0; pos<numSamples; ++pos) {

            double currentValue = convertedSamples[pos];

            if (hiPeaks.contains(pos)) {
                hiPeakSmooth = ((hiPeakSmooth * 3) + currentValue) / 4;   // Smooth peak amplitude
            }
            //if (loPeaks.contains(pos)) {
            //    loPeakSmooth = ((loPeakSmooth * 3) + currentValue) / 4;   // Smooth peak amplitude
            //}

            double amplitude = hiPeakSmooth - loPeakSmooth;
            double multiplier = WAVE_AMPLITUDE / amplitude;
            currentValue = currentValue * multiplier;
            convertedSamples[pos] = currentValue;
        }
    }
*/

// TODO: tries to find zeroes (4 peaks) and ones (9 peaks)
    
    private int getNumHighPeaks(int startPos, int endPos) {
        return getNumPeaks(hiPeaks, startPos, endPos);
    }

    private int getNumLowPeaks(int startPos, int endPos) {
        return getNumPeaks(loPeaks, startPos, endPos);
    }

    private int getNumPeaks(HashSet<Integer> peaks, int startPos, int endPos) {
        int count = 0;
        for(int pos=startPos; pos<endPos; ++pos) {
            if (peaks.contains(pos)) {
                ++count;
            }
        }
        return count;
    }

    private int findNextHighPeak(int startPos) {
        return findNextPeak(hiPeaks, startPos);
    }

    private int findNextLowPeak(int startPos) {
        return findNextPeak(loPeaks, startPos);
    }

    private int findNextPeak(HashSet<Integer> peaks, int startPos) {
        for(int pos=startPos+1; pos<numSamples; ++pos) {
            if (peaks.contains(pos)) {
                return pos;
            }
        }
        return -1;
    }
    
    private void findBits() {
        int pos = 0;
        while (pos < numSamples) {
            pos = findNextBit(pos);
            if (pos < 0) {
                break;
            }
        }
    }
    
    private int findNextBit(int startPos) {
        int pos = findNextHighPeak(startPos);
        if (pos >= 0) {
            int startBit = pos;
            double numSamples0 = 7 * 6 * sampleRate / 22050;    // 7 values per sample in 22Khz, 4 peaks + blank for 0
            {
                int endBit0 = startBit + (int)numSamples0;
                int numHigh0 = getNumHighPeaks(startBit, endBit0);
                int numLow0 = getNumLowPeaks(startBit, endBit0);
                if ((numHigh0 == 4) && (numLow0 == 4)) {
                    // this is a 0 bit
                    BitInfo bitInfo = new BitInfo(startBit, 0);
                    bitsArray.add(bitInfo);
                    return endBit0;
                }
            }

            double numSamples1 = 7 * 11 * sampleRate / 22050;   // 7 values per sample in 22Khz, 9 peaks + blank for 1
            {
                int endBit1 = startBit + (int)numSamples1;
                int numHigh1 = getNumHighPeaks(startBit, endBit1);
                int numLow1 = getNumLowPeaks(startBit, endBit1);
                if ((numHigh1 == 9) && (numLow1 == 9)) {
                    // this is a 1 bit
                    BitInfo bitInfo = new BitInfo(startBit, 1);
                    bitsArray.add(bitInfo);
                    return endBit1;
                }
            }
            return findNextLowPeak(pos);
        }
        return -1;
    }
    
    private void findBytes() {
        int pos = 0;
        int bitArrayPos = 0;
        while(bitArrayPos < bitsArray.size() - 7) {

            int byteValue = 0;
            for(int i=0; i<8; ++i) {
                BitInfo bitInfo = bitsArray.get(bitArrayPos + i);
                byteValue = (byteValue << 1) + bitInfo.value;
            }
            
            // TODO: check start and end offset respects good timing !
            BitInfo bitInfo = bitsArray.get(bitArrayPos);
            ByteInfo byteInfo = new ByteInfo(bitInfo.offset, byteValue);
            byteArray.add(byteInfo);
            
            bitArrayPos += 8;
        }
    
    }
    
}
