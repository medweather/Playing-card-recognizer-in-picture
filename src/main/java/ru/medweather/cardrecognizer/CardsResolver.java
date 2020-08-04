package ru.medweather.cardrecognizer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CardsResolver {

    private static Map<String, BufferedImage> mapRankTempls = new HashMap<>();
    private static Map<String, BufferedImage> mapSuitsTempls = new HashMap<>();
    private static int totalFoundCards = 0;
    private static int totalNotFoundCards = 0;

    private static Map<String, BufferedImage> loadTemplates(String[] templs)  {
        Map<String, BufferedImage> loadTemplates = new HashMap<>();
        Arrays.stream(templs).forEach(t -> {
            try {
                loadTemplates.put(t.substring(0, t.lastIndexOf(".")), ImageIO.read(Objects.requireNonNull(CardsResolver.class.getClassLoader().getResource(t))));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return loadTemplates;
    }

    public static void recognize(String[] args) throws IOException {
        mapRankTempls = loadTemplates(Constants.RANK_TEMPLS);
        if (mapRankTempls == null || mapRankTempls.isEmpty()) {
            System.out.println("Отсутствуют шаблоны рангов карт ");
            System.exit(0);
        };
        mapSuitsTempls = loadTemplates(Constants.SUITS_TEMPLS);
        if (mapRankTempls == null || mapRankTempls.isEmpty()) {
            System.out.println("Отсутствуют шаблоны мастей карт");
            System.exit(0);
        }
        String dirScan = args.length == 0 ? Constants.DEFAULT_INPUT_DIR : Optional.ofNullable(args[0]).orElse(Constants.DEFAULT_INPUT_DIR);
        File[] arrlf = new File(dirScan).listFiles((d, n) -> n.endsWith(".png"));
        if (arrlf.length == 0) {
            System.out.println("В папке " + dirScan + " файлов с картинками не найдено");
            System.exit(0);
        }
        for (File inFile : arrlf) {
            BufferedImage img1 = ImageIO.read(inFile);
            if (img1.getWidth()!= Constants.IMAGE_WIDTH || img1.getHeight() != Constants.IMAGE_HEIGHT){
                System.out.println("Неверный размер исходного изображения в файле "+inFile.getName());
                continue;
            }
            String[] foundRank = {"?","?","?","?","?"};
            String[] foundSuits = {"?","?","?","?","?"};
            for(int cardNum = 0; cardNum < Constants.ARRAY_OF_CARD_POSITION_X.length; cardNum++) {
                for(Map.Entry<String, BufferedImage> entry : mapRankTempls.entrySet()) {
                    BufferedImage img2 = entry.getValue();
                    int offset = Constants.OFFSET_OF_CARD_RANK_X;
                    String nameRank = entry.getKey();
                    String[] arrSplitRank = entry.getKey().split("_");
                    if (arrSplitRank.length == 2) {
                        nameRank = arrSplitRank[0];
                        offset = Integer.valueOf(arrSplitRank[1]);
                    }
                    int pd = CardsService.getCompareIndexOfSubImages(img1, img2, Constants.ARRAY_OF_CARD_POSITION_X[cardNum] + offset, Constants.CARD_POSITION_RANK_Y);
                    if (pd <= Constants.ALLOW_RANK_SCALE){
                        foundRank[cardNum] = nameRank;
                    }
                }
                for(Map.Entry<String, BufferedImage> entry : mapSuitsTempls.entrySet()) {
                    BufferedImage img2 = entry.getValue();
                    int pd = CardsService.getCompareIndexOfSubImages(img1, img2,
                            Constants.ARRAY_OF_CARD_POSITION_X[cardNum] + Constants.OFFSET_OF_CARD_SUITS_X,
                            Constants.CARD_POSITION_SUITS_Y);
                    if (pd <= Constants.ALLOW_SUITS_SCALE){
                        foundSuits[cardNum] = entry.getKey();
                        totalFoundCards++;
                    }
                }
            }
            String res = IntStream.range(0, foundRank.length).mapToObj(i -> foundRank[i] + foundSuits[i]).collect(Collectors.joining()).replace("??", "");
            totalNotFoundCards += res.chars().filter(ch -> ch == '?').count();
            System.out.println(inFile.getName() + " - " + res);
        }
        System.out.printf("\n\nОбщее количество распознанных карт - %d \n", totalFoundCards);
        System.out.printf("Общее количество нераспознанных карт - %d \n", totalNotFoundCards);
    }
}
