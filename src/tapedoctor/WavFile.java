/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tapedoctor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author aguyon
 */
public class WavFile {
    
    private static final double PEAK_THRESHOLD = 0.01;
    private static final double WAVE_AMPLITUDE = 0.8;
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
    private double minPeak = 0;
    private double maxPeak = 0;
    private static int peakPeriod = 7;
    
    public static class BitInfo {
        int offsetStart;
        int offsetEnd;
        int value;
        private BitInfo(int offsetStart, int value) {
            this.offsetStart = offsetStart;
            this.value = value;
            if (value == 0) {
                offsetEnd = offsetStart + (peakPeriod * 6); // 4 peaks + blank
            } else {
                offsetEnd = offsetStart + (peakPeriod * 11); // 9 peaks + blank
            }
        }
    }
    private ArrayList<BitInfo> bitsArray = new ArrayList<>(16384 * 8);  // 16K default
    public ArrayList<BitInfo> getBits() {
        return bitsArray;
    }
    
    public static class MissingBitInfo {
        int offsetStart;
        int offsetEnd;
        ArrayList<Integer> forcedValues = new ArrayList<Integer>();
        private MissingBitInfo(int offsetStart, int offsetEnd) {
            this.offsetStart = offsetStart;
            this.offsetEnd = offsetEnd;
        }
    }
    private ArrayList<MissingBitInfo> missingBits = new ArrayList<>();
    public ArrayList<MissingBitInfo> getMissingBits() {
        return missingBits;
    }
    public MissingBitInfo getMissingBit(int errorNumber) {
        if ((errorNumber >= 0) && errorNumber < missingBits.size()) {
            return missingBits.get(errorNumber);
        }
        return null;
    }
    public void deleteMissingBitArea(int index) {
        if ((index >= 0) && index < missingBits.size()) {
            missingBits.remove(index);
        }
    }
    
    public static class ByteInfo {
        int offsetStart;
        int offsetEnd;  // start of last bit
        int value;
        private ByteInfo(int offsetStart, int offsetEnd, int value) {
            this.offsetStart = offsetStart;
            this.offsetEnd = offsetEnd;
            this.value = value;
        }
    }
    private ArrayList<ByteInfo> byteArray = new ArrayList<>(16384);  // 16K default
    public ArrayList<ByteInfo> getBytes() {
        return byteArray;
    }
    public int getFirstByteOffset() {
        if (byteArray.isEmpty()) {
            return 0;
        } else {
            return byteArray.get(0).offsetStart;
        }
    }
    
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
            System.out.println("Peak Amplitude: " + minPeak + " to " + maxPeak);
            resample();
            //resampleDynamic();
            
            computePeakPeriod();
            findBits();
            logMissingBits();
            findBytes();

            guessMissingBits();
            
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
    
    public int getNumErrors() {
        return missingBits.size();
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
        
        String programName = getProgramName();
        if (programName.length() > 0) {
            builder.append("\n\nZX81 Program: ");
            builder.append(programName);
        }
        int numErrors = getNumErrors();
        if (numErrors > 0) {
            builder.append("\n");
            builder.append(numErrors);
            builder.append(" error(s) to fix");
        }
        
        return builder.toString();
    }
    
    public double getSampleValue(int sampleNumber) {
        if ((sampleNumber >= 0) && (sampleNumber < numSamples)) {
            return convertedSamples[sampleNumber];
        } else {
            return 0;
        }
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
    
    // Peak detection
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
                    if (currentValue > maxPeak) {
                        maxPeak = currentValue;
                    }
                } else {
                    if (currentValue < peakValue - PEAK_THRESHOLD) {
                        hiPeaks.add(peakOffset);
                        increasing = false;
                        peakOffset = pos;
                        peakValue = currentValue;
                    }
                }
            } else {    // decreasing
                if (currentValue < peakValue) {
                    peakValue = currentValue;
                    peakOffset = pos;
                    if (currentValue < minPeak) {
                        minPeak = currentValue;
                    }

                } else {
                    if (currentValue > peakValue + PEAK_THRESHOLD) {
                        loPeaks.add(peakOffset);
                        increasing = true;
                        peakOffset = pos;
                        peakValue = currentValue;                        
                    }
                }
            }
        }
    }
    
    private void logMissingBits() {
        
        BitInfo previous = null;
        int numErrors = 0;
        for(BitInfo bitInfo: bitsArray) {
            if (previous == null) {
                previous = bitInfo;
            } else {
                
                double previousSize = (previous.value == 0) ? get0bitSize() : get1bitSize();
                int endOfPrevious = (int) (previous.offsetStart + previousSize);
                int startOfNext = bitInfo.offsetStart - peakPeriod;                
                int space = startOfNext - endOfPrevious;
                if (space >= peakPeriod * 3) {
                    ++numErrors;
                    System.out.println("Missing 1 bit after position: " + endOfPrevious);
                    MissingBitInfo missingBit = new MissingBitInfo(endOfPrevious, startOfNext);
                    missingBits.add(missingBit);
                }
                previous = bitInfo;
            }
        }
        System.out.println(numErrors + " errors");
        
        
    }
    
    // resample with peaks information
    private void resample() {
        double amplitude = (maxPeak - minPeak); // hiPeakAvg - loPeakAvg;
        double multiplier = WAVE_AMPLITUDE / amplitude;
        for(int pos=0; pos<numSamples; ++pos) {
            double currentValue = convertedSamples[pos];
            currentValue = currentValue * multiplier;
            convertedSamples[pos] = currentValue;
        }
    }
    
    private void computePeakPeriod() {
        // put in hashmap the number of samples between peaks (period), number of times we have this period
        // then find the mose used period
        
        // first put all peaks in a sorted array
        ArrayList<Integer> peaksArray = new ArrayList<>();
        peaksArray.addAll(hiPeaks);
        Collections.sort(peaksArray);
        
        // Then read periods
        HashMap<Integer, Integer> periods = new HashMap<>();
        int previousPeak = peaksArray.get(0);
        for(int i=1; i<peaksArray.size(); ++i) {
            int peak = peaksArray.get(i);
            int period = peak - previousPeak;
            int num = periods.containsKey(period) ? periods.get(period) + 1 : 1;
            periods.put(period, num);
            previousPeak = peak;
        }
        
        // Now finds the most used period
        int periodScore = 0;
        Set<Integer> keys = periods.keySet();
        for(Integer key : keys) {   // keys are periods
            int score = periods.get(key);
            if (score > periodScore) {
                periodScore = score;
                peakPeriod = key;
            }
        }
        
        System.out.println("Peak Period is " + peakPeriod);
        
    }
    
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
            //double numSamples0 = 7 * 6 * sampleRate / 22050;    // 7 values per sample in 22Khz, 4 peaks + blank for 0
            double numSamples0 = peakPeriod * 6;    // 4 peaks + blank for 0
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
                if (numHigh0 < 4) { // optim: don't try to find 1 bit (9 peaks) if already less than 4
                    return findNextLowPeak(pos);
                }
            }

            //double numSamples1 = 7 * 11 * sampleRate / 22050;   // 7 values per sample in 22Khz, 9 peaks + blank for 1
            double numSamples1 = peakPeriod * 11;   // 9 peaks + blank for 1
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
        byteArray.clear();
        while(bitArrayPos < bitsArray.size() - 7) {

            int byteValue = 0;
            for(int i=0; i<8; ++i) {
                BitInfo bitInfo = bitsArray.get(bitArrayPos + i);
                byteValue = (byteValue << 1) + bitInfo.value;
            }
            
            // TODO: check start and end offset respects good timing !
            BitInfo bitInfo0 = bitsArray.get(bitArrayPos);
            BitInfo bitInfo7 = bitsArray.get(bitArrayPos + 7);
            ByteInfo byteInfo = new ByteInfo(bitInfo0.offsetStart, bitInfo7.offsetEnd, byteValue);
            byteArray.add(byteInfo);
            
            bitArrayPos += 8;
        }
    
    }
    
    public boolean save(File file) {
        
        if (byteArray.size() == 0) {
            return false;
        }
        
        // Skip NAME
        int offset = 0;
        int value;
        do {
            value = byteArray.get(offset).value;
            ++offset;
        } while((value < 128) && (offset < byteArray.size()));

        // Copy DATA
        byte[] buffer = new byte[byteArray.size() - offset];
        for(int pos=offset; pos<byteArray.size(); ++pos) {
            ByteInfo byteInfo = byteArray.get(pos);
            buffer[pos - offset] = (byte) byteInfo.value;
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(buffer, 0, buffer.length);
            fos.flush();
            fos.close();
            return true;
        } catch (IOException ex) {
            Logger.getLogger(WavFile.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } finally {
        }
    }
    
    public double get0bitSize() {
        return peakPeriod * (53 / 7.0);
    }
    public double get1bitSize() {
        return peakPeriod * (88 / 7.0);
    }

    private void guessMissingBits() {
        double bit0size = get0bitSize();
        double bit1size = get1bitSize();
        for(MissingBitInfo info : missingBits) {
            double numberOf0 = (info.offsetEnd - info.offsetStart) / bit0size;
            if ((numberOf0 > 0.9) && (numberOf0 < 1.1)) {
                // This is 0 that was not detected
                info.forcedValues.add(0);
            } else {
                double numberOf1 = (info.offsetEnd - info.offsetStart) / bit1size;
                if ((numberOf1 > 0.9) && (numberOf1 < 1.1)) {
                    // This is 1 that was not detected
                    info.forcedValues.add(1);
                }
            }
        }
    }

    public void applyAutoFixes() {
        int numFixes = 0;
        for(int i=0; i<missingBits.size(); ++i) {
            MissingBitInfo info = missingBits.get(i);
            if (info.forcedValues.size() == 1) {
                ++numFixes;
                int value = info.forcedValues.get(0);
                BitInfo bitInfo = new BitInfo(info.offsetStart, value);
                bitsArray.add(bitInfo);
                missingBits.remove(i);
                --i;
            }
        }
        sortBitsArray();
        System.out.println(numFixes + " fixes applied");
    }
    
    // returns true if removed all Missing bit info
    public boolean applyForceBit(int missingBitIndex) {
        boolean removed = false;
        if ((missingBitIndex >= 0) && (missingBitIndex < missingBits.size())) {
            MissingBitInfo missingBit = missingBits.get(missingBitIndex);
            double startOffset = missingBit.offsetStart;
            if (!missingBit.forcedValues.isEmpty()) {
                int bitValue = missingBit.forcedValues.get(0);
                if ((bitValue >= 0) && (bitValue <= 1)) {
                    double bitWidth = (bitValue == 0) ? get0bitSize() : get1bitSize();
                    BitInfo bitInfo = new BitInfo((int) startOffset, bitValue);
                    
                    // Search for other bit that could be useless (like detected 0 inside the forced bit 1)
                    double endOffset = startOffset + bitWidth;
                    for(int index=0; index<bitsArray.size(); ++index) {
                        BitInfo other = bitsArray.get(index);
                        if ((other.offsetStart >= startOffset) && (other.offsetEnd <= endOffset)) {
                            bitsArray.remove(index);
                            --index;
                        }
                    }

                    // add the bitInfo
                    bitsArray.add(bitInfo);
                    sortBitsArray();
                    
                    // keep the remaining missing bits if applies, or remove it
                    missingBit.forcedValues.clear();
                    missingBit.offsetStart = (int) endOffset;   // seems a bit too far
                    
                    double sizeLimit = peakPeriod * 2.5;
                    if (missingBit.offsetEnd - missingBit.offsetStart < sizeLimit) {
                        missingBits.remove(missingBitIndex);
                        removed = true;
                    }
                    
                }
                findBytes();    // Re-calculate bytes
            }
        }
        return removed;
    }
    
    private void sortBitsArray() {
                Collections.sort(bitsArray, new Comparator<BitInfo>() {
            @Override
            public int compare(BitInfo b1, BitInfo b2) {
                return b1.offsetStart - b2.offsetStart;
            }            
        });
    }
    
    public String getProgramName() {
        String name = "";
        for(ByteInfo byteInfo : byteArray) {
            int zxchar = byteInfo.value;
            name += ZX81CharSet.getAsciiChar(zxchar);
            if (zxchar >= 128) {
                break;
            }
            if (name.length() >= 100) {
                break;
            }
        }
        return name;
    }
    
}
