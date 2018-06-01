# TapeDoctor

TapeDoctor is a Java project and a Jar Tool that converts old computers programs for modern emulators.

In the past computer programs where recorded on tapes. Most of them can't be read anymore because the tapes degrade with time. 

Maybe it's not to late to save these programs!

Record your old tapes and save them in WAV format, and then use TapeDoctor to recover the data and save it for the emulators.

So far the only supported machine is the Sinclair ZX81, but maybe I'll add other machines later.


## Menus

### File Menu

- **Open WAV** opens a program previously recorded in WAV format
- **Save ZX81 .P** saves the program into .p format so that it can be read by emulators. The entry is greyed until a WAV is correctly loaded and all errors are resolved.
- **Quit** exits TapeDoctor

### Help Menu

- **About** displays information about TapeDoctor


## Loading a WAV

Select **Open WAV** from the File menu, and select a WAV file. When the file is loaded and processed, a popup appears, displaying some details about the WAV, the name of the program (which is encoded in the WAV) and possibly a number of errors. Click OK to continue.

![alt text](pictures/just_loaded.png?raw=true "Just loaded")

If no error occurred, the WAV is displayed.

![alt text](pictures/no_error.png?raw=true "No Error")

The **Purple Zones** represent the **Bits** recognized (0 or 1). On the tape, 4 consecutive peaks represent a 0, and 9 consecutive peaks represent a 1.

The **Yellow Zones** represent the **Bytes** recognized (8 grouped bits). Values vary from 0 to 255.

## Saving the .p program

Select **Save ZX81 .P** from the File Menu, give it a name, and validate. That's it!


## Handling errors

When errors occur during signal analysis, some information and controllers appear. The **Red Zone** is the **Error Zone**. It is possible to go the previous or next error using the **<<** and **>>** buttons.


![alt text](pictures/errors.png?raw=true "Errors")

When an Error is selected, you have the possiblity to force the value of the Bits by yourself. You have to click on "Set 0 bit" or "Set 1 bit" to change the value. Note that a 0 is encoded with 4 peaks, and a 1 is encoded with 9 peaks. When choosing 0 or 1, the bottom part of the zone gets colored in green.

![alt text](pictures/force_bit.png?raw=true "Force Bit value")

If you are happy with the value, click on "Apply bit set". The bit will be taken into account and you will be able to set the value for the next bit or the next error.

![alt text](pictures/resolved.png?raw=true "Resolved")

When all bits are set for an Error, you can go to the next error using >>. If there is a Red small area remaining, you can delete it using "DELETE". Be careful to not use DELETE if there are still some missing bits in the Red area!


Once all errors are fixed, you can save the program.


## License

Copyright 2018 Arnaud Guyon

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
