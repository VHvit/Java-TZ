package org.example;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    private final int requestLimit;
    private final ReentrantLock lock = new ReentrantLock();
    private long lastRequestTime;
    private int requestCount;

    public Main(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.lastRequestTime = System.currentTimeMillis();
        this.requestCount = 0;
    }

    public void createDocument(String document, String signature) {
        try {
            lock.lock();
            checkRequestLimit();

            String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";
            URL url = new URL(apiUrl);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.writeBytes(document);
                wr.flush();
            }

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }

                    System.out.println("Response: " + response.toString());
                }
            } else {
                throw new RuntimeException("HTTP error code: " + responseCode);
            }

            requestCount++;
            lastRequestTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private void checkRequestLimit() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastRequestTime;

        if (elapsedTime > TimeUnit.SECONDS.toMillis(1)) {
            requestCount = 0;
            lastRequestTime = currentTime;
        }

        if (requestCount >= requestLimit) {
            throw new RuntimeException("Превышен лимит запросов к API");
        }
    }

    public static void main(String[] args) {
        Main crptApi = new Main(TimeUnit.SECONDS, 10);

        String document = "{\"description\": {\"participantInn\": \"string\"}, \"doc_id\": \"string\", " +
                "\"doc_status\": \"string\", \"doc_type\": \"LP_INTRODUCE_GOODS\", " +
                "\"importRequest\": true, \"owner_inn\": \"string\", \"participant_inn\": \"string\", " +
                "\"producer_inn\": \"string\", \"production_date\": \"2020-01-23\", \"production_type\": \"string\", " +
                "\"products\": [{\"certificate_document\": \"string\", \"certificate_document_date\": \"2020-01-23\", " +
                "\"certificate_document_number\": \"string\", \"owner_inn\": \"string\", \"producer_inn\": \"string\", " +
                "\"production_date\": \"2020-01-23\", \"tnved_code\": \"string\", \"uit_code\": \"string\", " +
                "\"uitu_code\": \"string\" }], \"reg_date\": \"2020-01-23\", \"reg_number\": \"string\"}";

        String signature = "sample_signature";

        crptApi.createDocument(document, signature);
    }
}
