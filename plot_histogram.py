import matplotlib.pyplot as plt
import numpy as np

def plot_hue_histogram(csv_files):
    for csv_file in csv_files:
        # Load data from CSV
        data = np.loadtxt(csv_file, delimiter=',', skiprows=1)
        hue_values = data[:, 0]
        counts = data[:, 1]

        # # Scale the Hue values to 0-360 range
        # hue_values_scaled = hue_values * 2

        # Create a 1D histogram plot
        plt.bar(hue_values, counts, color='blue', alpha=0.7)

        plt.xlabel('Hue (0-360)')
        plt.ylabel('Count')
        plt.title('Hue Histogram (0-360)')

        # Set x-axis limits to cover the 0-360 range
        plt.xlim(0, 360)

        plt.show()

if __name__ == "__main__":
    import sys

    if len(sys.argv) < 2:
        print("Usage: python plot_histogram.py <csv_file1> <csv_file2> ... <csv_filen>")
        sys.exit(1)

    # Get CSV files from command-line arguments
    csv_files = sys.argv[1:]
    plot_hue_histogram(csv_files)
