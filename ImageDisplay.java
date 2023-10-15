
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;

import javax.swing.*;


public class ImageDisplay {

	JFrame frame; // main application window
	JLabel lbIm1; // used to display image
	BufferedImage imgOne; // image buffer used to store the original loaded in image
	BufferedImage imgTwo; // image buffer used to store the scaled image
	int width = 640; // default image width 
	int height = 480; // default image height
	HashMap<Integer, ArrayList<Point>> hueMap = new HashMap<>(); //keep strack of coordinates each hue occurs at 

	String[] channelLabels = {"R", "G", "B"};

	class HistogramData {
		BufferedImage image;
		int[] histogram;
		private String histogramFilePath;
	
		public HistogramData(BufferedImage image, int[] histogram, String histogramFilePath) {
			this.image = image;
			this.histogram = histogram;
			this.histogramFilePath = histogramFilePath;
		}
	
		public String getHistogramFilePath() {
			return histogramFilePath;
		}
	}

	class Point {
		private int x;
		private int y;
	
		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
	
		public int getX() {
			return x;
		}
	
		public int getY() {
			return y;
		}
	}

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img)
	{
		try
		{
			int frameLength = width*height*3;

			File file = new File(imgPath); //opens the image path and reads the binary data in the rgb file
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int ind = 0;
			//populating the Buffered Image with RGB pixel values
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2]; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					img.setRGB(x,y,pix);
					ind++;
				}
			}
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	public void showIms(String[] args){
		
		// Read in the specified image
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0], imgOne);

		if (args.length < 2) {
			displayImage(imgOne, "Input Image");
			System.out.println("No object input for detection");
			return;
		}

		ArrayList<BufferedImage> objectImages = new ArrayList<>();
		ArrayList<String> objectNames = new ArrayList<>();
		int n = args.length;
		for(int i = 0; i < n; i++){
			BufferedImage objImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			readImageRGB(width, height, args[i], objImg);
			objectImages.add(objImg);
			objectNames.add(args[i]);
		}

		// Display the input image and object images
        displayImage(imgOne, "Input Image");
        displayImages(objectImages, objectNames);
		boolean type = true;
		// Create a histogram for the input image
		int[] inputHistogram = createHueHistogram(imgOne, type);
		// Display the histogram
		showHistogram(inputHistogram, "Input Image Histogram");

		String inputFilename = "histogram_input.csv";
		// REMOVE THIS FOR SUBMISSION
		//saveHistogramDataToCSV(inputHistogram, inputFilename); //save data to a csv fiel so i can visualize it in python

        // Create a list to hold the histogram data for each object image
    	ArrayList<HistogramData> objectHistogramDataList = new ArrayList<>();
        for (int i = 1; i < args.length; i++) { //loop through the command line inputs which are the objects
            BufferedImage objImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            readImageRGB(width, height, args[i], objImg);
			// Filter out the RGB (0,255,0) background of the object image 
            BufferedImage filteredObjectImg = filterGreenBackground(objImg);
			// REMOVE THIS FOR SUBMISSION
			//TESTING
			//displayImage(filteredObjectImg, "Filtered Image"); //the image is removing the green background and the fringe is removed 
			type = false;
			// Create a histogram for the object image using the filtered image so there is no background rgb (0,255,0)
			int[] objectHistogram = createHueHistogram(filteredObjectImg, type);
			
			// Save the histogram to a CSV file to pass to python in order to visualize
			String objectFilename = "histogram_object" + i + ".csv";
			String objectFilePath = new File(objectFilename).getAbsolutePath(); 
			String objectName = objectNames.get(i);
			// Create HistogramData object for the object image
			HistogramData objectHistogramData = new HistogramData(filteredObjectImg, objectHistogram, objectFilePath);
			objectHistogramDataList.add(objectHistogramData);
			// Display the histogram
			showHistogram(objectHistogram, "Object Image " + i + " Histogram");

			// REMOVE THIS FOR SUBMISSION
			// Save the histogram data to a CSV file
			//saveHistogramDataToCSV(objectHistogram, objectFilename);
			
			compareHistogramsAndColorPixels(inputHistogram, objectHistogram, imgOne, filteredObjectImg, objectName);

		
        }

		// REMOVE THIS FOR SUBMISSION
		// Run the Python script to visualize histograms
		//runPythonScript(inputFilePath, objectHistogramDataList);
	}

	//Method to display the input image and display the object images
	private void displayImage(BufferedImage image, String title) {
		JFrame frame = new JFrame(title);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	
		ImageIcon icon = new ImageIcon(image);
		JLabel label = new JLabel(icon);
	
		frame.add(label);
		frame.pack();
		frame.setVisible(true);
	}
	private void displayImages(ArrayList<BufferedImage> images, ArrayList<String> titles) {
        for (int i = 1; i < images.size(); i++) {
            displayImage(images.get(i), titles.get(i));
        }
    }
	private void showHistogram(int[] histogram, String title) {
		// Display the histogram 
	
		System.out.println("Histogram for " + title + " (Hue)");
	
		for (int h = 0; h < histogram.length; h++) {
			int count = histogram[h];
			if (count > 0) {
				System.out.println("Hue Value " + h + ": " + count);
			}
		}
	}

	private int[] createHueHistogram(BufferedImage img, boolean type) {
		int width = img.getWidth();
        int height = img.getHeight();
		int[] hueHistogram = new int[360];  // Adjusted for HSV (0-360)

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int rgb = img.getRGB(x, y);
				int hue = getHue(rgb);
				if (hue >= 0 && hue < 360) {  // make sure hue is within valid range
					hueHistogram[hue]++;
					if(type) {
						hueMap.putIfAbsent(hue, new ArrayList<>());
						// Add the coordinates (x, y) to the ArrayList for this hue
                        hueMap.get(hue).add(new Point(x, y));
					}
				} 
			}
		}
	
		return hueHistogram;
	}

	private int getHue(int rgb) {
		float[] hsv = new float[3];
		Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsv);
		return Math.round(hsv[0] * 360);  // Convert hue to the 0-360 range
	}

	// ****** 
	// FILTERING OUT GREEN
	// ******

	//filtering out the green background of the object image 
	private BufferedImage filterGreenBackground(BufferedImage img) {
		BufferedImage filteredImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int count = 0;
	
		int greenRGB = new Color(0, 255, 0).getRGB();
	
		// Define a threshold for green color
		int greenThreshold = 50;
	
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int rgb = img.getRGB(x, y);
				Color color = new Color(rgb);
	
				// Check if the color or its neighbors are close to green
				if (isGreenishOrNeighbor(color, greenThreshold, greenRGB, img, x, y)) {
					// Set the pixel to white to remove the green fringe
					filteredImg.setRGB(x, y, Color.WHITE.getRGB());
				} else {
					filteredImg.setRGB(x, y, rgb);
					count++;
				}
			}
		}
	
		return filteredImg;
	}
	
	//handling the fringe of remaining green pixels around the object image
	private boolean isGreenishOrNeighbor(Color color, int threshold, int targetRGB, BufferedImage img, int x, int y) {
		// Check if the current pixel is close to green
		if (isGreenish(color, threshold, targetRGB)) {
			return true;
		}
	
		int neighborRadius = 1; // Adjust the radius of the neighborhood to check
		for (int dx = -neighborRadius; dx <= neighborRadius; dx++) {
			for (int dy = -neighborRadius; dy <= neighborRadius; dy++) {
				if (dx == 0 && dy == 0) {
					continue; // Skip the current pixel
				}
	
				int nx = x + dx;
				int ny = y + dy;
	
				// Check if the neighbor pixel is within bounds
				if (nx >= 0 && nx < img.getWidth() && ny >= 0 && ny < img.getHeight()) {
					Color neighborColor = new Color(img.getRGB(nx, ny));
					if (isGreenish(neighborColor, threshold, targetRGB)) {
						return true; // Neighbor is greenish
					}
				}
			}
		}
	
		return false;
	}
	
	//handling the fringe of remaining green pixels around the object image
	private boolean isGreenish(Color color, int threshold, int targetRGB) {
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();
	
		Color targetColor = new Color(targetRGB);
		int targetR = targetColor.getRed();
		int targetG = targetColor.getGreen();
		int targetB = targetColor.getBlue();
	
		return Math.abs(r - targetR) < threshold && Math.abs(g - targetG) < threshold && Math.abs(b - targetB) < threshold;
	}

	// ******
	//DETECTING
	// ****** 

	private void compareHistogramsAndColorPixels(int[] inputHistogram, int[] objectHistogram, BufferedImage inputImage, BufferedImage objectImage, String objectName) {
		
		boolean anyPixelShared = false;
	
		//find the colors that overlap between histograms and their pixel locations
		boolean[][] sharedPixels = new boolean[width][height];
			//check if the histogram values in the object are in the input histogram 
			//if a value is detected then add the pixel locations form the hasmap to a sharedPixels
		for (int i = 0; i < objectHistogram.length; i++) {
			int objectHueCount = objectHistogram[i];  // Frequency of this hue in the object histogram
		
			if (objectHueCount > 0) {
				// Check if this hue exists in the input histogram
				if (i < inputHistogram.length) {
					int inputHueCount = inputHistogram[i];  // Frequency of this hue in the input histogram
		
					// Get the pixels for this hue from the hueMap
					ArrayList<Point> pixels = hueMap.get(i);
		
					// Mark the pixels in sharedPixels
					if (pixels != null) {
						for (int j = 0; j < Math.min(inputHueCount, objectHueCount); j++) {
							Point pixel = pixels.get(j);
							sharedPixels[pixel.x][pixel.y] = true;
							anyPixelShared = true;
						}
					}
				}
			}
		}

		// Check if any pixel was shared
		if (!anyPixelShared) {
			System.out.println("Object not detected in the input image");
			return;
		}


		int height = sharedPixels.length;
		int width = sharedPixels[0].length;
		boolean[][] visited = new boolean[height][width];
		ArrayList<Point> largestCluster = new ArrayList<>();
		int[] dx = {-1, 1, 0, 0};
		int[] dy = {0, 0, -1, 1};

		//BFS 
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if(sharedPixels[y][x] && !visited[y][x]){
					ArrayList<Point> currCluster = new ArrayList<>();
					Queue<Point> q = new ArrayDeque<>();

					q.add(new Point(x, y));
					visited[y][x] = true;

					while(!q.isEmpty()){
						Point p = q.poll();
						currCluster.add(p);

						for(int i = 0; i < 4; i++){
							int newX = p.x + dx[i];
							int newY = p.y + dy[i];

							if(newX >= 0 && newX < width && newY >= 0 && newY < height){
								if(sharedPixels[newY][newX] && !visited[newY][newX]){
									q.add(new Point(newX, newY));
									visited[newY][newX] = true;
								}
							}
						}
					}
					if(currCluster.size() > largestCluster.size()){
						largestCluster = currCluster;
					}
					
				}
			}
		}

			//TETSING TO SEE PIXELS DETECTED
			// // Color pixels red for testing
			// for (Point pixel : largestCluster) {
			// 	int x = pixel.x;
			// 	int y = pixel.y;
			// 	//System.out.println("Coord" + x + ", " + y);
			// 	if (x >= 0 && x < width && y >= 0 && y < height) {
			// 		inputImage.setRGB(y, x, Color.RED.getRGB());
			// 	}
			// }

			if (largestCluster.isEmpty()) {
				System.out.println("Object not detected in the input image");
				return;
			}


		// Draw bounding box around the clustered pixels
		drawBox(largestCluster, inputImage, objectName);
	
		displayImage(inputImage, "Comparison Result");
	}

	private void drawBox(ArrayList<Point> largestCluster, BufferedImage inputImage, String objectName) {
		int numTargetPixels = largestCluster.size();
	
		if (numTargetPixels == 0) {
			return;
		}
	
		// min and max x, y coordinates of the target pixels
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
	
		for (Point pixel : largestCluster) {
			int x = pixel.x;
			int y = pixel.y;
	
			// Update min and max 
			minX = Math.min(minX, x);
			minY = Math.min(minY, y);
			maxX = Math.max(maxX, x);
			maxY = Math.max(maxY, y);
		}
	
		// Draw box
		Graphics2D g2d = inputImage.createGraphics();
		g2d.setColor(Color.RED);
		g2d.drawRect(minY, minX, maxY - minY, maxX - minX);
	
		// Calculate the position to display the objectName below the box
		int textWidth = g2d.getFontMetrics().stringWidth(objectName);
		int textHeight = g2d.getFontMetrics().getHeight();
		int textX = minY + (maxY - minY - textWidth) / 2;
		int textY = maxY + textHeight;
	
		// Ensure the objectName is fully visible
		if (textY + textHeight > inputImage.getHeight()) {
			textY = maxY;
		}
	
		g2d.drawString(objectName, textX, textY);
		g2d.dispose();
	}
	
	
	



	///////////////////////////////////////////////////////////////////////
	

	// //USED FOR VISUALIZATION & TESTING
	// private void saveHistogramDataToCSV(int[] histogram, String filename) {
	// 	try (PrintWriter writer = new PrintWriter(filename)) {
	// 		// Write the CSV header
	// 		writer.println("Hue,Count");
	
	// 		// Write the histogram data
	// 		for (int h = 0; h < 360; h++) {
	// 			int count = histogram[h];
	// 			if (count > 0) {
	// 				writer.printf("%d,%d%n", h, count);
	// 			}
	// 		}
	// 	} catch (IOException e) {
	// 		e.printStackTrace();
	// 	}
	// }

	

	// private void runPythonScript(String inputFilePath, ArrayList<HistogramData> objectHistogramDataList) {
	// 	try {
	// 		// Construct the command to run the Python script
	// 		ArrayList<String> command = new ArrayList<>();
	// 		command.add("python");
	// 		command.add("plot_histogram.py");
	// 		command.add(inputFilePath);
	
	// 		// Add the histogram file paths to the command
	// 		for (HistogramData histogramData : objectHistogramDataList) {
	// 			command.add(histogramData.getHistogramFilePath());
	// 		}
	
	// 		// Create a ProcessBuilder with the command and start the process
	// 		ProcessBuilder processBuilder = new ProcessBuilder(command);
	// 		Process process = processBuilder.start();
	
	// 		// Handle the process output and errors if needed
	// 		InputStream inputStream = process.getInputStream();
	// 		Scanner scanner = new Scanner(inputStream);
	// 		while (scanner.hasNextLine()) {
	// 			System.out.println(scanner.nextLine());
	// 		}
	
	// 		// Print errors or exceptions from the process
	// 		InputStream errorStream = process.getErrorStream();
	// 		Scanner errorScanner = new Scanner(errorStream);
	// 		while (errorScanner.hasNextLine()) {
	// 			System.err.println("Python Error: " + errorScanner.nextLine());
	// 		}
	
	// 		process.waitFor();  // Wait for the process to complete
	// 	} catch (IOException | InterruptedException e) {
	// 		e.printStackTrace();
	// 	}
	// }
	
	
	
	

	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}

}
