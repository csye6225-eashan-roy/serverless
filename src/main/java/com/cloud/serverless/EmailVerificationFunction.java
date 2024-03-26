package com.cloud.serverless;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.pubsub.v1.PubsubMessage;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class EmailVerificationFunction implements BackgroundFunction<PubsubMessage> {
    private static final Gson gson = new Gson();
    private final String mailgunApiKey = System.getenv("MAILGUN_API_KEY"); // Set this environment variable in your Cloud Function configuration
    private final String mailgunDomain = "your_domain"; // Replace with your Mailgun domain

    @Override
    public void accept(PubsubMessage message, Context context) {
        // Decode the message
        String messageData = new String(Base64.getDecoder().decode(message.getData().toStringUtf8()));
        VerificationInfo info = gson.fromJson(messageData, VerificationInfo.class);

        // Generate the verification URL
        String verificationUrl = "http://eashanroy.me/verify?token=" + info.getEmailVerificationToken();

        /// Send the email
        try {
            sendVerificationEmail(info.getUsername(), verificationUrl);
            System.out.println("Email sent to: " + info.getUsername());
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to send verification email: " + e.getMessage());
        }
    }

    private void sendVerificationEmail(String username, String verificationUrl) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        Map<Object, Object> data = new HashMap<>();
        data.put("from", "Excited User <mailgun@" + mailgunDomain + ">");
        data.put("to", username);
        data.put("subject", "Email Verification");
        data.put("text", "Please click on the following link to verify your email: " + verificationUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mailgun.net/v3/" + mailgunDomain + "/messages"))
                .POST(buildFormDataFromMap(data))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(("api:" + mailgunApiKey).getBytes()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Check response status code and handle accordingly
        if (response.statusCode() == 200) {
            System.out.println("Email sent successfully to: " + username);
        } else {
            // Handle failure
            System.out.println("Failed to send email to: " + username + ". Status code: " + response.statusCode() + " Response: " + response.body());
        }
    }

    public static HttpRequest.BodyPublisher buildFormDataFromMap(Map<Object, Object> data) {
        var builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }

    private static class VerificationInfo {
        private String username;
        private String emailVerificationToken;

        // Getters and Setters
        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }
        public String getEmailVerificationToken() {
            return emailVerificationToken;
        }
        public void setEmailVerificationToken(String emailVerificationToken) {
            this.emailVerificationToken = emailVerificationToken;
        }

    }
}

