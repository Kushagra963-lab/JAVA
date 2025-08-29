package com.bfh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootApplication
public class BfhChallengeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BfhChallengeApplication.class, args);
    }

    @Bean
RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(10_000);
    factory.setReadTimeout(10_000);
    return new RestTemplate(factory);
}

    @Bean
    CommandLineRunner run(
        RestTemplate http,
        @Value("${bfh.name}") String name,
        @Value("${bfh.regNo}") String regNo,
        @Value("${bfh.email}") String email,
        @Value("${bfh.finalQuery}") String finalQuery
    ) {
        return args -> {
            System.out.println("== BFH Qualifier starting ==");

            // 1) Generate webhook + token
            String genUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

            Map<String, Object> req = Map.of(
                    "name", name,
                    "regNo", regNo,
                    "email", email
            );

            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> genResp = http.postForEntity(genUrl, new HttpEntity<>(req, h), Map.class);

            if (!genResp.getStatusCode().is2xxSuccessful() || genResp.getBody() == null) {
                throw new RuntimeException("Webhook generation failed: " + genResp.getStatusCode());
            }

            Map body = genResp.getBody();
            String webhook = (String) body.getOrDefault("webhook", "");
            String accessToken = (String) body.getOrDefault("accessToken", "");

            System.out.println("Webhook received: " + webhook);
            System.out.println("Access token received (JWT): " + (accessToken != null && !accessToken.isBlank()));

            // 2) Decide question by last two digits (odd -> Q1, even -> Q2)
            int lastTwo = extractLastTwoDigits(regNo);
            System.out.println("Last two digits of regNo: " + lastTwo + " -> " + ((lastTwo % 2 == 0) ? "Even (Question 2)" : "Odd (Question 1)"));

            // 3) Submit final SQL
            String fallbackTestUrl = "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";
            String submitUrl = (webhook != null && !webhook.isBlank()) ? webhook : fallbackTestUrl;

            HttpHeaders hdrs = new HttpHeaders();
            hdrs.setContentType(MediaType.APPLICATION_JSON);
            // Paper shows Authorization header must contain the accessToken (JWT)
            hdrs.set("Authorization", accessToken);

            Map<String, String> submitBody = new HashMap<>();
            submitBody.put("finalQuery", finalQuery);

            ResponseEntity<String> submitResp =
                    http.postForEntity(submitUrl, new HttpEntity<>(submitBody, hdrs), String.class);

            System.out.println("Submission status: " + submitResp.getStatusCode());
            System.out.println("Submission response: " + submitResp.getBody());
            System.out.println("== Finished ==");
        };
    }

    private static int extractLastTwoDigits(String regNo) {
        Matcher m = Pattern.compile("(\\d{2})(?!.*\\d)").matcher(regNo);
        if (m.find()) return Integer.parseInt(m.group(1));
        m = Pattern.compile("(\\d)(?!.*\\d)").matcher(regNo);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 0;
    }
}
