# MultiMedia3: Image Compression Using Wavelets

This program will help you gain an understanding of issues that relate to image
compression using wavelets. You will read an RGB file and convert the image pixels to a DWT
representation (as used in the JPEG2000 implementation) for each channel. Depending on the second
parameter n you will decode both the representations using only n levels of the low pass coefficients and
display the output. 

Typical invocations to the program would look like
MyExe Image.rgb 9
This the level 9 or the highest level and as such corresponds to your entire image. Here you are making
use of 29 = 512 coefficients in rows and 512 coefficients in columns, which essentially is the input image
itself and so the output should look just like the input.
MyExe Image.rgb 8
This is level 8 and the first level of the wavelet encoded hierarchy in rows and columns. Here you are
making use of 28 = 256 low pass coefficients in rows and 256 low pass coefficients in columns,
MyExe Image.rgb 1
This is level 1 and the eight level in the wavelet encoded hierarchy in rows and columns . Here you are
making use of 21 = 2 low pass coefficients in rows and 2 low pass coefficients in columns

-----------------------------------

**Encoding Implementation**
For the DWT encoding process, convert each row (for each channel) into low pass and high pass
coefficients followed by the same for each column applied to the output of the row processing. Recurse
through the process as explained in class through rows first then the columns next at each recursive
iteration, each time operating on the low pass section until you reach the appropriate level
**Decoding Implementation**
Once you reach the appropriate level, zero out all the high pass coefficients. Then perform a recursive
IDWT from the encoded level upto level 9 which is the image level. You need to appropriately decode by
zeroing out the unrequested coefficients (just setting the coefficients to zero) and then perform an IDWT.
**Progressive Encoding-Decoding Implementation**
This is when n = -1. In this case you will go through the creation of the entire DWT representation till
level 0. Then decode each level recursively and display the output. The first display will be at level 0,
then level 1 and so on till you reach level 9. You should see the image progressively improving with
details

-----------------------------------

How to run this program:

run from the command file, first compile:

>> javac ImageDisplay.java

and then run the program with desired inputs, for example:

>> java ImageCompression roses_image_512x512.rgb 2

or

>> java ImageCompression roses_image_512x512.rgb -1

the first command line argument is the input image and the seconnd command line argument defines the low pass level to be
used in your decoding

