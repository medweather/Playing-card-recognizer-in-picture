package ru.medweather.cardrecognizer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.stream.IntStream;

public class CardsService {

    private static BufferedImage grayscale(BufferedImage image) {
        BufferedImage newSubElementImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                Color c = new Color(image.getRGB(j, i));
                int red = (int) (c.getRed() * 0.299);
                int green = (int) (c.getGreen() * 0.587);
                int blue = (int) (c.getBlue() * 0.114);
                Color newColor = new Color(red + green + blue,
                        red + green + blue, red + green + blue);
                newSubElementImage.setRGB(j, i, newColor.getRGB());
            }
        }
        return newSubElementImage;
    }

    public static BufferedImage binarize(BufferedImage img) {
        img = grayscale(img);
        int threshold = treshold(img);
        BufferedImage binarized = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        for(int i = 0; i < img.getWidth(); i++) {
            for(int j = 0; j < img.getHeight(); j++) {
                int red = new Color(img.getRGB(i, j)).getRed();
                int alpha = new Color(img.getRGB(i, j)).getAlpha();
                int newPixel = red > threshold ? 255 : 0;
                newPixel = colorToRGB(alpha, newPixel, newPixel, newPixel);
                binarized.setRGB(i, j, newPixel);
            }
        }
        return binarized;
    }

    private static int treshold(BufferedImage img) {
        int[] histogram = getHistogram(img);
        int total = img.getHeight() * img.getWidth();
        float sum = 0;
        for(int i = 0; i < 256; i++) sum += i * histogram[i];
        float sumB = 0;
        int wB = 0;
        int wF;
        float varMax = 0;
        int threshold = 0;
        for(int i = 0; i < 256; i++) {
            wB += histogram[i];
            if(wB == 0) continue;
            wF = total - wB;
            if(wF == 0) break;
            sumB += (float) (i * histogram[i]);
            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;
            float varBetween = (float) wB * (float) wF * (mB - mF) * (mB - mF);
            if(varBetween > varMax) {
                varMax = varBetween;
                threshold = i;
            }
        }
        return threshold;
    }

    private static int[] getHistogram(BufferedImage img) {
        int[] hist = IntStream.range(0, 256).map(i->0).toArray();
        for(int i = 0; i < img.getWidth(); i++) {
            for(int j = 0; j < img.getHeight(); j++) {
                int red = new Color(img.getRGB(i, j)).getRed();
                hist[red]++;
            }
        }
        return hist;
    }

    public static int getCompareIndexOfSubImages(BufferedImage img1, BufferedImage img2, int offsetX, int positionY) {
        BufferedImage img = img1.getSubimage(offsetX, positionY,
                img2.getWidth(), img2.getHeight());
        img = binarize(img);
        changeColor(img, Constants.ARRAY_RGB_OF_SHADOW_CARD_SURFACE, Constants.ARRAY_RGB_OF_NORMAL_CARD_SURFACE);
        return Double.valueOf(getDifferencePercent(img, img2)).intValue();
    }

    private static double getDifferencePercent(BufferedImage img1, BufferedImage img2) {
        long diff = 0;
        for (int y = 0; y < img1.getHeight(); y++) {
            for (int x = 0; x < img1.getWidth(); x++) {
                diff += getPixelDifference(img1.getRGB(x, y), img2.getRGB(x, y));
            }
        }
        return 100.0 * diff / (3L * 255 * img1.getWidth() * img1.getHeight());
    }

    private static int getPixelDifference(int rgb1, int rgb2) {
        return Math.abs(((rgb1 >> 16) & 0xff) - ((rgb2 >> 16) & 0xff)) + Math.abs(((rgb1 >> 8) & 0xff) - ((rgb2 >> 8) & 0xff)) + Math.abs((rgb1 & 0xff) - (rgb2 & 0xff));
    }

    private static void changeColor(BufferedImage img, int[] rgb1, int[] rgb2) {
        int MASK = 0x00ffffff;
        int sourceRgb = rgb1[0] << 16 | rgb1[1] << 8 | rgb1[2];
        int destRgb = sourceRgb ^ (rgb1[0] << 16 | rgb1[1] << 8 | rgb1[2]);
        int w = img.getWidth();
        int h = img.getHeight();
        int[] arrRgb = img.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < arrRgb.length; i++) {
            if ((arrRgb[i] & MASK) == sourceRgb) {
                arrRgb[i] ^= destRgb;
            }
        }
        img.setRGB(0, 0, w, h, arrRgb, 0, w);
    }

    private static int colorToRGB(int alpha, int red, int green, int blue) {
        int newPixel = 0;
        newPixel += alpha;
        newPixel = newPixel << 8;
        newPixel += red;
        newPixel = newPixel << 8;
        newPixel += green;
        newPixel = newPixel << 8;
        newPixel += blue;
        return newPixel;
    }
}
