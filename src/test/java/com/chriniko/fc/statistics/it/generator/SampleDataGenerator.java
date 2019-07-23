package com.chriniko.fc.statistics.it.generator;

import com.chriniko.fc.statistics.common.MathProvider;
import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.dto.FieldStatistics;
import com.chriniko.fc.statistics.dto.VegetationStatistic;
import com.chriniko.fc.statistics.it.core.ConfigIT;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SampleDataGenerator {

    private static final Instant INSTANT_FOR_TESTING = Instant.parse(ConfigIT.TIME_POINT_FOR_TESTING);
    private static final ZonedDateTime NOW = ZonedDateTime.ofInstant(INSTANT_FOR_TESTING, ZoneOffset.UTC);

    public static void main(String[] args) throws Exception {
        generate();
    }

    private static void generate() throws Exception {
        // Note: change based on your machine.
        String pathToCreateFiles = "/home/chriniko/Desktop";

        MathProvider mathProvider = new MathProvider();

        // Note: generate random captures.
        List<FieldConditionCapture> captures = generateRandomCaptures();

        // Note: calculate statistics.
        DoubleSummaryStatistics summaryStatistics = captures
                .stream()
                .sorted(Comparator.comparing(FieldConditionCapture::getOccurrenceAt).reversed())
                .filter(fC -> {
                    LocalDate date = ZonedDateTime.ofInstant(fC.getOccurrenceAt(), ZoneOffset.UTC).toLocalDate();

                    LocalDate nowLocalDate = LocalDate.now(Clock.system(NOW.getZone()));

                    long daysDiff = Duration.between(
                            date.atStartOfDay(),
                            nowLocalDate.atStartOfDay()
                    ).toDays();

                    return daysDiff >= 0 && daysDiff <= 30;
                })
                .mapToDouble(FieldConditionCapture::getVegetation)
                .summaryStatistics();

        double min = mathProvider.scale(summaryStatistics.getMin(), 2);
        double max = mathProvider.scale(summaryStatistics.getMax(), 2);
        double avg = mathProvider.scale(summaryStatistics.getAverage(), 2);

        System.out.println("min is: " + min);
        System.out.println("max is: " + max);
        System.out.println("avg is: " + avg);

        VegetationStatistic vegetationStatistic = new VegetationStatistic(min, max, avg);

        FieldStatistics statistics = new FieldStatistics();
        statistics.setVegetation(vegetationStatistic);

        // Note: time to write the input and output to files.
        createInputAndOutputFile(pathToCreateFiles, captures, statistics);
    }

    private static List<FieldConditionCapture> generateRandomCaptures() {
        int noOfCaptures = 200;

        int noOfCapturesForSameTime = 10;
        int noOfCapturesForDifferentTime = 10;

        int daysDecrease = 1;
        int maxSecondsDiff = 240;

        SecureRandom r = new SecureRandom();

        List<FieldConditionCapture> captures = new ArrayList<>(noOfCaptures);

        for (int i = 1; i <= noOfCaptures; i++) {

            int secondsDecrease = r.nextInt(maxSecondsDiff) + 1;
            ZonedDateTime randomOccurrenceAt = NOW.minusDays(daysDecrease);

            // Note: random vegetation captures with same occurrence time.
            for (int k = 1; k <= noOfCapturesForSameTime; k++) {

                FieldConditionCapture capture = new FieldConditionCapture();

                double v = ThreadLocalRandom.current().nextDouble(0.03, 0.98);

                double randomVegetation = BigDecimal
                        .valueOf(v)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();

                capture.setVegetation(randomVegetation);
                capture.setOccurrenceAt(randomOccurrenceAt.toInstant());

                captures.add(capture);
            }


            // Note: random vegetation captures with different occurrence time.
            for (int k = 1; k <= noOfCapturesForDifferentTime; k++) {

                FieldConditionCapture capture = new FieldConditionCapture();

                double v = ThreadLocalRandom.current().nextDouble(0.03, 0.98);

                double randomVegetation = BigDecimal
                        .valueOf(v)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();

                capture.setVegetation(randomVegetation);

                ZonedDateTime time = NOW.minusDays(daysDecrease).minusSeconds(secondsDecrease);
                capture.setOccurrenceAt(time.toInstant());

                captures.add(capture);
            }

            daysDecrease++;
        }
        return captures;
    }

    private static void createInputAndOutputFile(String pathToCreateFiles, List<FieldConditionCapture> captures, FieldStatistics statistics) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        Path inputPath = Paths.get(pathToCreateFiles + "/input.txt");
        if (Files.exists(inputPath)) {
            Files.delete(inputPath);
            Files.createFile(inputPath);
        }
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(inputPath)) {

            String data = objectMapper.writeValueAsString(captures);
            bufferedWriter.write(data);
            bufferedWriter.newLine();
            bufferedWriter.flush();

        }


        Path outputPath = Paths.get(pathToCreateFiles + "/output.txt");
        if (Files.exists(outputPath)) {
            Files.delete(outputPath);
            Files.createFile(outputPath);
        }
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(outputPath)) {

            String data = objectMapper.writeValueAsString(statistics);
            bufferedWriter.write(data);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        }
    }
}
