import imagescaling.ResampleOp;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ImageProcessor extends Thread {

    private final ImageToDataProcessor controller;

    private final ConcurrentLinkedDeque<ImageProcess> processes = new ConcurrentLinkedDeque<>();

    private final ResampleOp resizeToLQ1;
    private final ResampleOp resizeToULQ1;

    public ImageProcessor(ImageToDataProcessor controller) {
        this.controller = controller;

        resizeToLQ1 = new ResampleOp(32, 32);
        resizeToULQ1 = new ResampleOp(16, 16);

        resizeToLQ1.setFilter(controller.getResampleFilter());
        resizeToULQ1.setFilter(controller.getResampleFilter());
        start();
    }

    public ImageProcessor(ImageToDataProcessor controller, int lqSize, int ulqSize) {
        this.controller = controller;

        resizeToLQ1 = new ResampleOp(lqSize, lqSize);
        resizeToULQ1 = new ResampleOp(ulqSize, ulqSize);

        resizeToLQ1.setFilter(controller.getResampleFilter());
        resizeToULQ1.setFilter(controller.getResampleFilter());
        start();
    }

    public int getProcessesCount() {
        return processes.size();
    }

    public void addImageProcess(ImageProcess process) {
        synchronized (processes) {
            processes.add(process);
        }
    }

    private ImageProcess next() {
        synchronized (processes) {
            return processes.poll();
        }
    }

    @Override
    public void run() {
        super.run();

        while (!isInterrupted()) {
            ImageProcess process = next();
            if (process != null) {
                BufferedImage image = process.getImage(); //---------------------
                BufferedImage thumbnail = process.getThumbnail();

                if (image == null) {
                    System.out.println("image in null!");
                    continue;
                }

                if (process.getRequireThumbnail() && thumbnail == null) {
                    int maxSide = Math.max(image.getWidth(), image.getHeight());
                    int width = image.getWidth(), height = image.getHeight();

                    if (maxSide > controller.getDefaultThumbnailMapSide() * 1.1) {
                        float del = controller.getDefaultThumbnailMapSide() / (float) maxSide;

                        width = Math.round(width * del);
                        height = Math.round(height * del);

                        ResampleOp scaler = new ResampleOp(width, height);

                        thumbnail = scaler.filter(image, null);
                    } else {
                        thumbnail = image;
                    }
                } // thumbnail generating
                BufferedImage img32 = generate32Image(image, thumbnail, resizeToLQ1); // for horizontal hash + color map(downscale to 8x8)
                BufferedImage img16 = img32;                                              // for vertical hash

                if (controller.getLQHashSize() != controller.getULQHashSize()) {
                    img16 = resizeToULQ1.filter(img32, null);
                }

                ArrayList<Integer> hash =
                        generateHorizontalHash(img32, controller.getColorHashedDepth());
                hash.addAll(
                        generateVerticalHash(img16, controller.getColorHashedDepth())
                );


                int[][][] colorMap = generateColorMap(img32);

                process.finish(colorMap, hash, image.getWidth(), image.getHeight(), image, thumbnail);

            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private int[][][] generateColorMap(BufferedImage img32) {
        int[][][] colorMap = new int[8][8][3];
        int del = img32.getWidth() / 8;

        for (byte x = 0; x < 8; x++) {
            for (byte y = 0; y < 8; y++) {
                colorMap[x][y] = generateMidColor(img32.getSubimage(x * del, y * del, del, del));
            }
        }
        return colorMap;
    }

    private int[] generateMidColor(BufferedImage subimage) {
        int[] color = new int[3];

        for (byte x = 0; x < subimage.getWidth(); x++) {
            for (byte y = 0; y < subimage.getHeight(); y++) {
                Color col = new Color(subimage.getRGB(x, y));

                color[0] += col.getRed();
                color[1] += col.getGreen();
                color[2] += col.getBlue();
            }
        }
        color[0] /= subimage.getWidth() * subimage.getHeight();
        color[1] /= subimage.getWidth() * subimage.getHeight();
        color[2] /= subimage.getWidth() * subimage.getHeight();

        return color;
    }

    static private ArrayList<Integer> generateVerticalHash(BufferedImage img, int depth) {
        ArrayList<Double> list = new ArrayList<>();

        for (byte y = 0; y < img.getHeight(); y++) {
            int b = 0;

            for (byte x = 0; x < img.getWidth(); x++) {
                Color col = new Color(img.getRGB(x, y));
                b += (col.getRed() + col.getGreen() + col.getBlue()) / 3;
            }

            list.add(b / ((double) (img.getHeight() / (depth / 256))));
        }
        return normalizeHashes(list);
    }

    static private ArrayList<Integer> generateHorizontalHash(BufferedImage img, int depth) {
        ArrayList<Double> list = new ArrayList<>();

        for (byte x = 0; x < img.getWidth(); x++) {
            int b = 0;

            for (byte y = 0; y < img.getHeight(); y++) {
                Color col = new Color(img.getRGB(x, y));
                b += (col.getRed() + col.getGreen() + col.getBlue()) / 3;
            }

            list.add(b / ((double) (img.getWidth() / (depth / 256))));
        }
        return normalizeHashes(list);
    }

    static private ArrayList<Integer> normalizeHashes(ArrayList<Double> hash) {
        ArrayList<Integer> result = new ArrayList<>();
        double max = 0;
        double min = 255;
        for (double d : hash) {
            if (d < min) min = d;
            if (d > max) max = d;
        }

        for (double num : hash) {
            int newNum;
            try {
                newNum = (int) Math.round(((float) (num - min) / (max - min)) * 255);
            } catch (ArithmeticException e) {
                newNum = (int) Math.round(num);
            }
            result.add(newNum);
        }
        return result;
    }

    static private BufferedImage generate32Image(BufferedImage image, BufferedImage thumbnail, ResampleOp scaler) {
        BufferedImage orig = thumbnail;
        if (orig == null) orig = image;

        if (orig.getWidth() != orig.getHeight()) {
            int width = orig.getWidth();
            int height = orig.getHeight();

            int x, y, w, h;

            if (width > height) {
                y = 0;
                h = height;

                x = (width - height) / 2;
                w = height;
            } else {
                x = 0;
                w = width;

                y = (height - width) / 2;
                h = width;
            }

            orig = orig.getSubimage(x, y, w, h);
        }

        return scaler.filter(orig, null);
    }
}