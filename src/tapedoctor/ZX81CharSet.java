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
public class ZX81CharSet {
    
    private static int CHARSET_OFFSET = 11;
    private static String CHARSET = "\"L$:?()><=+-*/;,.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    
    public static char getAsciiChar(int zxchar) {
        zxchar &= 0x7F;             // bit 7 not used
        if (zxchar >= 0x40) {       // same but video inverse
            zxchar -= 0x40;
        }
        if (zxchar < CHARSET_OFFSET) {
            return ' ';
        } else {
            return CHARSET.charAt(zxchar - CHARSET_OFFSET);
        }
    }
    
}
