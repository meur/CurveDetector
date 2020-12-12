import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.opencv.core.CvType.CV_8UC1;

public class Main {
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        final Mat input = readImage("curve4.png");
        showImage(input, "original");

        System.out.printf("col: %d row: %d\n", input.cols(), input.rows());

        final Mat bw = toBlackAndWhite(input);
        showImage(bw, "b&w");

        /*final Mat filtered = filterImage(bw);
        showImage(filtered, "Filtered");*/

        final Mat reduced = findRelevantPoints(bw);
        showImage(reduced, "reduced");

        final Mat lineFree = removeLines(reduced);
        showImage(lineFree, "Line-free");

        exportPoints(lineFree, "export.txt");
    }

    private static Mat toBlackAndWhite(final Mat input) {
        final Mat output = new Mat(input.size(), CV_8UC1);
        for (int x = 0; x < input.rows(); x++) {
            for (int y = 0; y < input.cols(); y++) {
                if (isBlackPoint(input, x, y)) {
                    output.put(x, y, 0, 0, 0);
                }
                else {
                    output.put(x, y, 255, 255, 255);
                }
            }
        }
        return output;
    }

    private static Mat filterImage(final Mat input) {
        Mat output = new Mat(input.size(), input.type());
        input.copyTo(output);
        System.out.printf("col: %d row: %d\n", input.cols(), input.rows());
        for (int x = 0; x < input.rows(); x++) {
            for (int y = 0; y < input.cols(); y++) {
                if (alonePoint(input, x, y)) {
                    output.put(x, y, 255, 255, 255);
                }
            }
        }
        return output;
    }

    private static boolean alonePoint(final Mat image, final int x, final int y) {
        int neighbours = 0;
        if (hasNeighbourRight(image, x, y)) neighbours++;
        if (hasNeighbourLeft(image, x, y)) neighbours++;
        if (hasNeighbourUnder(image, x, y)) neighbours++;
        if (hasNeighbourUpper(image, x, y)) neighbours++;
        return neighbours < 2;
    }

    private static Mat findRelevantPoints(final Mat input) {
        final Mat output = new Mat(input.size(), input.type());
        for (int x = 0; x < input.rows(); x++) {
            for (int y = 0; y < input.cols(); y++) {
                if (isBlackPoint(input, x, y) && isRelevantPoint(input, x, y)) {
                    output.put(x, y, 0, 0, 0);
                }
                else {
                    output.put(x, y, 255, 255, 255);
                }
            }
        }
        return output;
    }

    private static boolean isBlackPoint(final Mat image, final int x, final int y) {
        double[] colors = image.get(x, y);
        int sum = 0;
        for (double color : colors) {
            sum += color;
        }
        return sum < colors.length * 255 / 2;
    }

    private static boolean isRelevantPoint(final Mat image, final int x, final int y) {
        return hasNeighbourUnder(image, x, y) && !hasNeighbourUpper(image, x, y)
                && (hasNeighbourLeft(image, x, y) && !hasNeighbourRight(image, x, y)
                || !hasNeighbourLeft(image, x, y) && hasNeighbourRight(image, x, y));
    }

    private static Mat removeLines(final Mat input) {
        Mat output = new Mat(input.size(), input.type());
        input.copyTo(output);
        final int length = 4;
        for (int x = 0; x < input.rows() - length; x++) {
            for (int y = length; y < input.cols() - length; y++) {
                if (isBlackPoint(output, x, y)) {
                    int d = 1;
                    while (d <= length && isBlackPoint(output, x + d, y + d)) {
                        output.put(x + d, y + d, 255, 255, 255);
                        d++;
                    }
                    d = 1;
                    while (d <= length && isBlackPoint(output, x + d, y - d)) {
                        output.put(x + d, y - d, 255, 255, 255);
                        d++;
                    }
                }
            }
        }
        return output;
    }

    private static boolean hasNeighbourLeft(final Mat image, final int x, final int y) {
        return y > 0
                && isBlackPoint(image, x, y - 1);
    }

    private static boolean hasNeighbourRight(final Mat image, final int x, final int y) {
        return y < image.cols() - 1
                && isBlackPoint(image, x, y + 1);
    }

    private static boolean hasNeighbourUnder(final Mat image, final int x, final int y) {
        return x < image.rows() - 1
                && isBlackPoint(image, x + 1, y);
    }

    private static boolean hasNeighbourUpper(final Mat image, final int x, final int y) {
        return x > 0
                && isBlackPoint(image, x - 1, y);
    }

    private static Mat readImage(final String path) {
        return Imgcodecs.imread(Main.class.getResource(path).getPath());
    }

    private static void showImage(final Mat image, final String title) {
        HighGui.imshow(title ,image);
        HighGui.waitKey();
    }

    private static void showImage(final Mat image) {
        showImage(image, "Image");
    }

    private static void exportPoints(final Mat input, final String outputFileName) {
        try {
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFileName));
            for (int x = 0; x < input.cols(); x++) {
                for (int y = 0; y < input.rows(); y++) {
                    if (isBlackPoint(input, y, x)) {
                        writer.write(x + " " + (input.rows() - y) + "\n");
                    }
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

