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

/**
 *
 * @author aguyon
 */
public class ByteHelper {
    
    public static boolean equalsString(byte[] buffer, int offset, String text) {
        int length = text.length();
        int max = buffer.length;
        for(int i=0; i<length; ++i) {
            int pos = i + offset;
            if (pos >= max) {
                return false;
            }
            char car = (char) buffer[pos];
            char other = text.charAt(i);
            if (car != other) {
                return false;
            }
        }
        return true;
    }

    public static int getInt2(byte[] buffer, int offset) {
        return (int) getInt(buffer, offset, 2);
    }
        
    public static long getInt4(byte[] buffer, int offset) {
        return getInt(buffer, offset, 4);
    }
    
    private static long getInt(byte[] buffer, int offset, int numBytes) {
        long result = 0;
        for(int i=0; i<numBytes; ++i) {
            int value = buffer[offset + numBytes - 1 - i];
            if (value < 0) {
                value += 256;
            }
            result = (result << 8) + value;
        }
        return result;
    }
    
}
