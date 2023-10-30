import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.swing.*;

public class ImageCompression {

    private JFrame frame; // main application window
    private JLabel lbImDWT;
    private JLabel lbImDWT_text;
    private static int width = 512; // default image width
    private static int height = 512; // default image height
    BufferedImage imgOg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    BufferedImage imgDWT = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    private static int maxPixelValue = 255;

	String[] channelLabels = {"R", "G", "B"};

	//base channel arrays
	int[][] redChannel = new int[height][width];
    int[][] greenChannel = new int[height][width];
    int[][] blueChannel = new int[height][width];

	//DWT Channels
	double[][] redDWT = new double[height][width];
    double[][] greenDWT = new double[height][width];
    double[][] blueDWT = new double[height][width];

	//IDWTChannels
	int[][] redIDWT = new int[height][width];
    int[][] greenIDWT = new int[height][width];
    int[][] blueIDWT = new int[height][width];

     // Constructor for initializing JFrame and labels
    public ImageCompression() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        lbImDWT_text = new JLabel();
        lbImDWT = new JLabel();

        frame.add(lbImDWT_text);
        frame.add(lbImDWT);

        frame.pack();
        frame.setVisible(true);
    }

    //
    // Read Image RGB
	// Reads the image of given width and height at the given imgPath into the matrix.
    //
	public void prcocessTransform(String[] args) {

        try {
            //read in rgb file 
            File file = new File(args[0]);
            int coefficientNum = Integer.parseInt(args[1]);

            InputStream inputStream = new FileInputStream(file);

            long len = file.length();
            byte[] bytes = new byte[(int) len];

            int offset = 0;
            int readCount = 0;
            while (offset < bytes.length && (readCount = inputStream.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += readCount;
            }

            //process image data into byte array 
            int index = 0;
            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    //extract each chennels bytes (R G B)
                    int red = bytes[index];
                    int green = bytes[index + height * width];
                    int blue = bytes[index + height * width * 2];

                    green = green & 0xFF;
                    red = red & 0xFF;
                    blue = blue & 0xFF;

                    redChannel[row][column] = red;
                    blueChannel[row][column] = blue;
                    greenChannel[row][column] = green;

                    int pix = 0xff000000 | ((red & 0xff) << 16) | ((green & 0xff) << 8) | (blue & 0xff);
                    imgOg.setRGB(column, row, pix); //set the RGB pixels in the original image
                    index++;
                }
            }

            //PROCESS COEFFICIENT
            if (coefficientNum > 0) {
                //calculate n 
                int n = (int) Math.pow(2, coefficientNum);

                //TESTING
                // System.out.println(coefficientNum);
                // System.out.println(n);

                //do DWT and idwt for each color channel
                redDWT = DWTStandardDecomposition(redChannel, n);
                greenDWT = DWTStandardDecomposition(greenChannel, n);
                blueDWT = DWTStandardDecomposition(blueChannel, n);
                redIDWT = IDWTComposition(redDWT, n);
                greenIDWT = IDWTComposition(greenDWT, n);
                blueIDWT = IDWTComposition(blueDWT, n);

                //display image
                displayDWT_Tranformation(0);
                
            } else { 
                //coefficient is -1: progressive decoding
                //increment the coefficent at each step so it mimics streaming
                //iterating throguh the 9 levels
                for (int level = 1; level <= 9; level++) {
                    int n = (int) Math.pow(2, level);

                    //do DWT and idwt for each color channel
                    redDWT = DWTStandardDecomposition(redChannel, n);
                    greenDWT = DWTStandardDecomposition(greenChannel, n);
                    blueDWT = DWTStandardDecomposition(blueChannel, n);
                    redIDWT = IDWTComposition(redDWT, n);
                    greenIDWT = IDWTComposition(greenDWT, n);
                    blueIDWT = IDWTComposition(blueDWT, n);

                    //do i need to sleep between each one? 
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                    }

                    displayDWT_Tranformation(level);

                }
            }

        } catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
    }


	//displays the DWT trasnformed image
    private void displayDWT_Tranformation(int iteration) {
        //iterate over row then column
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                //calculate the pixel value for DWT
                int pixValue = 0xff000000 | (((int) redIDWT[row][column] & 0xff) << 16) | (((int) greenIDWT[row][column] & 0xff) << 8) | ((int) blueIDWT[row][column] & 0xff);
                imgDWT.setRGB(column, row, pixValue); //update pixel value 
            }
        }

        if(iteration == 0){
            frame.setTitle("DWT View");
        }
        else{
            frame.setTitle("DWT View Iteration : " + iteration + "/9");
        }
        frame.setPreferredSize(new Dimension(width + 100, height + 100));
        lbImDWT.setIcon(new ImageIcon(imgDWT));
        lbImDWT.setHorizontalAlignment(SwingConstants.CENTER);

        frame.pack();
        frame.setVisible(true); //DISPLAYS IT
    }

	//transpose matrix. row -> column. column -> row
	private static double[][] performMatrixTranspose (double[][] matrix) {
        double[][] tempMat = new double[height][width]; //swap dimensions
        for (int row = 0; row < height; row++)
            for (int column = 0; column < width; column++)
                tempMat[row][column] = matrix[column][row]; //reassign values

        return tempMat;
    }


    ////////////////////
    // DWT
    // For the DWT encoding process, convert each row (for each channel) into low pass and high pass
    // coefficients followed by the same for each column applied to the output of the row processing. Recurse
    // through the process as explained in class through rows first then the columns next at each recursive
    // iteration, each time operating on the low pass section until you reach the appropriate level
    ////////////////////

	private double[][] DWTStandardDecomposition(int[][] matrix, int n) {
        
        double[][] DWTMatrix = new double[height][width]; //initalize matrix 
        
        for (int row = 0; row < height; row++){
            for (int column = 0; column < width; column++){
                DWTMatrix[row][column] = matrix[row][column];
            }
        }

        //row wise decomposition
        for (int row = 0; row < width; row++){
            DWTMatrix[row] = getDecompositionArray(DWTMatrix[row]);
        }

        //transpose matrix to perform colium wise decomposition
        DWTMatrix = performMatrixTranspose(DWTMatrix);
        for (int col = 0; col < height; col++){
            DWTMatrix[col] = getDecompositionArray(DWTMatrix[col]);
        }

        //transpose back
        DWTMatrix = performMatrixTranspose(DWTMatrix);
        //do the zig zag traversal with n 
        DWTMatrix = ZigZag(DWTMatrix, n);

        return DWTMatrix;
    }

	private double[] getDecompositionArray(double[] array) {
        int height = array.length;
        while (height > 0) {
            array = decompositionStep(array, height);
            height = height / 2; //wavelet decomposition 
        }
        return array;
    }

    
    //decompression step 
    //divide into sub array for low pass and high pass
    private double[] decompositionStep(double[] array, int height) {
        double[] copyArray = Arrays.copyOf(array, array.length); //copy array
        for (int index = 0; index < height / 2; index++) { //iterates 1st half 
            copyArray[index] = (array[2 * index] + array[2 * index + 1]) / 2; //calculate avg of values next to eachother (low pass)
            copyArray[height / 2 + index] = (array[2 * index] - array[2 * index + 1]) / 2; //for second half calculate the values next to eachother (high pass)
        }
        return copyArray;
    }

	
    ////////////////////
    // IDWT
    // Once you reach the appropriate level, zero out all the high pass coefficients. Then perform a recursive
    // IDWT from the encoded level upto level 9 which is the image level. You need to appropriately decode by
    // zeroing out the unrequested coefficients (just setting the coefficients to zero) and then perform an IDWT
    ////////////////////

	private static int[][] IDWTComposition(double[][] matrix, int n) {
        int[][] IDWTMatrix = new int[height][width];

        //row wise composition
        matrix = performMatrixTranspose(matrix);
        for (int row = 0; row < width; row++) {
            matrix[row] = getCompositionArray(matrix[row]);
        }

        //transpose and get column wise composition
        matrix = performMatrixTranspose(matrix);
        for (int col = 0; col < height; col++) {
            matrix[col] = getCompositionArray(matrix[col]);
        }

        //zero out unrequested coefficients
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                //System.out.println(n);
                if(n < 9){ //OR EXQUAL TO?
                    IDWTMatrix[row][column] = 0;
                   // System.out.println(IDWTMatrix[row][column]);
                }
                IDWTMatrix[row][column] = (int) Math.round(matrix[row][column]);
                if (IDWTMatrix[row][column] < 0) {
                    IDWTMatrix[row][column] = 0;
                } else if (IDWTMatrix[row][column] > maxPixelValue) {
                    IDWTMatrix[row][column] = maxPixelValue;
                }
            }
        }
        return IDWTMatrix;
    }

	private static double[] getCompositionArray(double[] array) {
        int height = 1;
        while (height <= array.length) {
            array = compositionStep(array, height);
            height = height * 2; //wavelet composition 
        }
        return array;
    }

    private static double[] compositionStep(double[] array, int height) {
        double[] copyArray = Arrays.copyOf(array, array.length); //copy array
        for (int index = 0; index < height / 2; index++) { //iterates 1st half 
            copyArray[2 * index] = array[index] + array[height / 2 + index]; //calculate avg of values next to eachother (low pass)
            copyArray[2 * index + 1] = array[index] - array[height / 2 + index];  //for second half calculate the values next to eachother (high pass)
        }
        return copyArray;
    }


    //count coefficients
	public double[][] ZigZag(double[][] matrix, int n) {
        //starts at top left of matrix [0,0]
        int row = 0;
        int column = 0;
        int length = matrix.length - 1;
        int count = 1;

        //matrix[row][column] = count > n ? 0 : matrix[row][column]; 
        //zeroing out coefficients beyond n
        if (row >= n || column >= n) {
            matrix[row][column] = 0;
        } else {
            // Only increment the count if the coefficient is not zeroed out
            count++;
        }

        //visit all pixels
        while (true) { 
            column++;

            //zeroing out coefficient beyond n
            if(count > n){
                matrix[row][column] = 0;
            }
            else{
                matrix[row][column] = matrix[row][column];
            }
            count++;

            while (column != 0) {
                row++;
                column--;
                //zeroing out coefficients beyond n
                if (row >= n || column >= n) {
                    matrix[row][column] = 0;
                } else {
                    // Only increment the count if the coefficient is not zeroed out
                    count++;
                }
            }
            row++;
            if (row > length) {
                row--;
                break;
            }

            //zeroing out coefficients beyond n
            if (row >= n || column >= n) {
                matrix[row][column] = 0;
            } else {
                // Only increment the count if the coefficient is not zeroed out
                count++;
            }

            while (row != 0) {
                row--;
                column++;
                //zeroing out coefficients beyond n
                if (row >= n || column >= n) {
                    matrix[row][column] = 0;
                } else {
                    // Only increment the count if the coefficient is not zeroed out
                    count++;
                }
            }
        }

        while (true) {
            column++;
            count++;

            if (count > n) {
                matrix[row][column] = 0;
            }

            while (column != length) {
                column++;
                row--;
                //zeroing out coefficients beyond n
                if (row >= n || column >= n) {
                    matrix[row][column] = 0;
                } else {
                    // Only increment the count if the coefficient is not zeroed out
                    count++;
                }
            }

            row++;
            if (row > length) {
                row--;
                break;
            }
            //zeroing out coefficients beyond n
            if (row >= n || column >= n) {
                matrix[row][column] = 0;
            } else {
                // Only increment the count if the coefficient is not zeroed out
                count++;
            }

            while (row < length) {
                row++;
                column--;
                //zeroing out coefficients beyond n
                if (row >= n || column >= n) {
                    matrix[row][column] = 0;
                } else {
                    // Only increment the count if the coefficient is not zeroed out
                    count++;
                }
                }
        }
        return matrix;
    }


    public static void main(String[] args) {
		ImageCompression ren = new ImageCompression();
		ren.prcocessTransform(args);
	}
	

}

