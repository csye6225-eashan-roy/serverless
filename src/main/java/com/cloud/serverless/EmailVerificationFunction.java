package com.cloud.serverless;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.pubsub.v1.PubsubMessage;

import java.io.IOException;
import java.util.Base64;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class EmailVerificationFunction implements BackgroundFunction<PubsubMessage> {
    private static final Gson gson = new Gson();

    @Override
    public void accept(PubsubMessage message, Context context) throws IOException, InterruptedException {
        // Decode the message
        String messageData = new String(Base64.getDecoder().decode(message.getData().toStringUtf8()));
        VerificationInfo info = gson.fromJson(messageData, VerificationInfo.class);

        // Generate the verification URL
        String verificationUrl = "http://eashanroy.me/verify?token=" + info.getEmailVerificationToken();

        // Send the email
        sendVerificationEmail(info.getUsername(), verificationUrl);

        System.out.println("Email sent to: " + info.getUsername());
    }

    private void sendVerificationEmail(String username, String verificationUrl) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://<dc>.api.mailchimp.com/3.0/lists/<AUDIENCE_ID>/members/")) // Replace <dc> with your datacenter, e.g., us5, and <AUDIENCE_ID> with your actual Audience ID
                .header("Authorization", "Bearer YOUR_API_KEY") // Replace YOUR_API_KEY with your actual Mailchimp API key
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildEmailJson(username, verificationUrl)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Check response status code and handle accordingly
        if (response.statusCode() == 200) {
            System.out.println("Email sent successfully to: " + username);
        } else {
            // Handle failure
            System.out.println("Failed to send email to: " + username + ". Status code: " + response.statusCode());
        }
    }

    private String buildEmailJson(String email, String verificationUrl) {
        JsonObject json = new JsonObject();
        // Construct JSON payload as required by Mailchimp API for your specific email template and audience
        return json.toString();
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

