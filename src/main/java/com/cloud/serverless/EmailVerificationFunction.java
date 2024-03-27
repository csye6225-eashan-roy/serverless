package com.cloud.serverless;

import com.google.cloud.functions.CloudEventsFunction;
import com.google.gson.Gson;
import io.cloudevents.CloudEvent;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class EmailVerificationFunction implements CloudEventsFunction {

    private static final Logger logger = Logger.getLogger(EmailVerificationFunction.class.getName());
    private static final Gson gson = new Gson();

    private DatabaseService databaseService = new DatabaseService();
    private final String mailgunApiKey = System.getenv("MAILGUN_API_KEY"); // Set this environment variable in your Cloud Function configuration
    private final String mailgunDomain = "eashanroy.me"; // Replace with your Mailgun domain

    private static class PubSubBody {
        private Message message;
        static class Message {
            private String data;
            //{"message":{"data":"eyJ1c2VybmFtZSI6ImVhc2hhbnJveTdAZ21haWwuY29tIiwidmVyaWZpY2F0aW9uVG9rZW4iOiJlMGNkY2RhNy0xMzhhLTRjYWEtOTQwMS0yMTE0ZDhhNzY5YzQifQ==","messageId":"10787488454474661","message_id":"10787488454474661","publishTime":"2024-03-26T08:23:09.085Z","publish_time":"2024-03-26T08:23:09.085Z"},"subscription":"projects/csye6225-eashan-roy/subscriptions/eventarc-us-central1-email-verification-function-296418-sub-715"}
            public String getUsernameDecoded() {
                if (data == null) {
                    return null;
                }
                // Decode the data from Base64
                String json = new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
                // Now, parse the JSON to extract the username
                VerificationInfo info = new Gson().fromJson(json, VerificationInfo.class);
                return info.username;
            }
        }
    }
    private static class VerificationInfo {
        String username; // Ensure this matches the JSON property for the username
    }

    @Override
    public void accept(CloudEvent event) {

        if (event.getData() == null) {
            logger.severe("Message data is null");
            return; // Early return to prevent the rest of the code from executing
        }
        // Extract Cloud Event data and convert to PubSubBody
        String cloudEventData = new String(event.getData().toBytes(), StandardCharsets.UTF_8);
//        logger.info("MAILGUN_API_KEY: " + mailgunApiKey);
//        logger.info("Received message data: " + cloudEventData);
//        logger.info("MAILGUN_DOMAIN: " + mailgunDomain);
        PubSubBody body = gson.fromJson(cloudEventData, PubSubBody.class);

        if (body != null && body.message != null && body.message.data != null) {
            // Decode username
            String username = body.message.getUsernameDecoded();
            logger.info("Decoded username: " + username);


            // Generate verification token and expiry time
            String token = UUID.randomUUID().toString();
            Timestamp expiryTime = Timestamp.valueOf(LocalDateTime.now().plusMinutes(2));

            // Update user's verification token and expiry time in the database
            databaseService.updateUserVerificationToken(username, token, expiryTime);

            String verificationUrl = "http://eashanroy.me:8081/v1/verify-email?token=" + token;

            try {
                sendVerificationEmail(username, verificationUrl);
            } catch (IOException | InterruptedException e) {
                logger.severe("Failed to send verification email: " + e.getMessage());
            }
        } else {
            logger.severe("Failed to extract user information from the message");
        }
    }

    private void sendVerificationEmail(String username, String verificationUrl) throws IOException, InterruptedException {
        // Log the values to ensure they are not null
        logger.info("Email id of user is: " + username);
        logger.info("Verification URL is: " + verificationUrl);

        // Check if username or verificationUrl is null
        if (username == null || verificationUrl == null) {
            logger.severe("Username or verification URL is null. Cannot send email.");
            return; // Exit the method early if either is null
        }
        HttpClient client = HttpClient.newHttpClient();

        Map<String, String> data = new HashMap<>();
        data.put("from", "New User <mailgun@" + mailgunDomain + ">");
        data.put("to", username);
        data.put("subject", "Email Verification");
        data.put("text", "Please click on the following link to verify your email: " + verificationUrl);

        // Log the map to debug
//        data.forEach((key, value) -> logger.info("Key: " + key + ", Value: " + value));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mailgun.net/v3/" + mailgunDomain + "/messages"))
                .POST(buildFormDataFromMap(data))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(("api:" + mailgunApiKey).getBytes()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Check response status code and handle accordingly
        if (response.statusCode() == 200) {
            logger.info("Email sent successfully to: " + username);
        } else {
            // Handle failure
            logger.info("Failed to send email to: " + username + ". Status code: " + response.statusCode() + " Response: " + response.body());
        }
    }

    public static HttpRequest.BodyPublisher buildFormDataFromMap(Map<String, String> data) {
        var builder = new StringBuilder();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }
}

