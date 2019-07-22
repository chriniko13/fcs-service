package com.chriniko.fc.statistics.it.generator;

import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.dto.FieldStatistics;
import com.chriniko.fc.statistics.dto.MergedFieldConditionCapture;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.validation.constraints.NotNull;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

public class SampleDataGenerator {

    public static void main(String[] args) throws Exception {
        generate();
    }

    private static void generate() throws Exception {
        // Note: change based on your machine.
        String pathToCreateFiles = "/home/chriniko/Desktop";

        // Note: generate random captures.
        List<FieldConditionCapture> captures = generateRandomCaptures();


        // Note: group captures by date
        Map<LocalDate, List<FieldConditionCapture>> capturesGroupByDate = groupCapturesByDate(captures);

        // Note: merge captures by date
        List<MergedFieldConditionCapture> mergedCapturesByDate = mergeCapturesByDate(capturesGroupByDate);


        // Note: calculate vegetation statistics (min, max, avg) of last N days.
        FieldStatistics statistics = calculateStatistics(mergedCapturesByDate);

        // Note: time to write the input and output to files.
        createInputAndOutputFile(pathToCreateFiles, captures, statistics);
    }

    private static List<FieldConditionCapture> generateRandomCaptures() {
        int noOfCaptures = 100;

        int noOfCapturesForSameTime = 10;
        int noOfCapturesForDifferentTime = 10;


        int dayIncrement = 1;
        int maxSecondsDiff = 240;


        SecureRandom r = new SecureRandom();
        ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());

        List<FieldConditionCapture> captures = new ArrayList<>(noOfCaptures);

        for (int i = 1; i <= noOfCaptures; i++) {

            int plusSeconds = r.nextInt(maxSecondsDiff) + 1;
            ZonedDateTime randomOccurrenceAt = now.plusDays(dayIncrement);

            // Note: random vegetation captures with same occurrence time.
            for (int k = 1; k <= noOfCapturesForSameTime; k++) {

                FieldConditionCapture capture = new FieldConditionCapture();

                double randomVegetation = BigDecimal
                        .valueOf(r.nextDouble())
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();

                capture.setVegetation(randomVegetation);
                capture.setOccurrenceAt(randomOccurrenceAt.toInstant());

                captures.add(capture);
            }


            // Note: random vegetation captures with different occurrence time.
            for (int k = 1; k <= noOfCapturesForDifferentTime; k++) {

                FieldConditionCapture capture = new FieldConditionCapture();

                double randomVegetation = BigDecimal
                        .valueOf(r.nextDouble())
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();

                capture.setVegetation(randomVegetation);

                ZonedDateTime time = now.plusDays(dayIncrement).plusSeconds(plusSeconds);
                capture.setOccurrenceAt(time.toInstant());


                captures.add(capture);
            }

            dayIncrement++;
        }
        return captures;
    }

    private static Map<LocalDate, List<FieldConditionCapture>> groupCapturesByDate(List<FieldConditionCapture> captures) {
        Map<LocalDate, List<FieldConditionCapture>> capturesGroupByDate = new LinkedHashMap<>();

        for (FieldConditionCapture capture : captures) {

            @NotNull Instant occurrenceAt = capture.getOccurrenceAt();
            LocalDate localDate = ZonedDateTime.ofInstant(occurrenceAt, ZoneId.of("UTC")).toLocalDate();

            List<FieldConditionCapture> fieldConditionCaptures = capturesGroupByDate.get(localDate);
            if (fieldConditionCaptures == null) {

                List<FieldConditionCapture> l = new LinkedList<>();
                l.add(capture);
                capturesGroupByDate.put(localDate, l);

            } else {
                fieldConditionCaptures.add(capture);
            }
        }
        return capturesGroupByDate;
    }

    private static List<MergedFieldConditionCapture> mergeCapturesByDate(Map<LocalDate, List<FieldConditionCapture>> capturesGroupByDate) {
        List<MergedFieldConditionCapture> mergedCapturesByDate = new LinkedList<>();

        for (Map.Entry<LocalDate, List<FieldConditionCapture>> entry : capturesGroupByDate.entrySet()) {

            LocalDate key = entry.getKey();

            List<FieldConditionCapture> cS = entry.getValue();

            double sum = 0.0D;
            for (FieldConditionCapture c : cS) {
                sum += c.getVegetation();
            }

            double avg = BigDecimal
                    .valueOf(sum)
                    .divide(BigDecimal.valueOf(cS.size()), 2, RoundingMode.HALF_UP)
                    .doubleValue();

            MergedFieldConditionCapture mergedFieldConditionCapture = new MergedFieldConditionCapture(key, avg);
            mergedCapturesByDate.add(mergedFieldConditionCapture);
        }
        return mergedCapturesByDate;
    }

    private static FieldStatistics calculateStatistics(List<MergedFieldConditionCapture> mergedCapturesByDate) {
        int lastDays = 30;

        mergedCapturesByDate.sort(Comparator.comparing(MergedFieldConditionCapture::getDate).reversed());

        List<MergedFieldConditionCapture> latestCaptures = mergedCapturesByDate.size() < lastDays
                ? mergedCapturesByDate
                : mergedCapturesByDate.subList(0, lastDays);

        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        double sum = 0.0D;

        for (MergedFieldConditionCapture capture : latestCaptures) {

            @NotNull Double vegetation = capture.getVegetation();
            if (vegetation < min) {
                min = vegetation;
            }
            if (vegetation > max) {
                max = vegetation;
            }
            sum += vegetation;
        }

        double avg = BigDecimal
                .valueOf(sum)
                .divide(BigDecimal.valueOf(lastDays), 2, RoundingMode.HALF_UP)
                .doubleValue();

        System.out.println("min is: " + min);
        System.out.println("max is: " + max);
        System.out.println("avg is: " + avg);


        FieldStatistics statistics = new FieldStatistics();
        statistics.getVegetation().setMin(min);
        statistics.getVegetation().setMax(max);
        statistics.getVegetation().setAvg(avg);
        return statistics;
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
