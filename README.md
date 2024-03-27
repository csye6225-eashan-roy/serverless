# serverless
Code for Google Cloud Function  

-- The Cloud Function will do the following:

1. Receive the base64 encoded message (containing email id of the user) from the Pub/Sub topic
2. Decode the message, deserialize it, insert a verification token in the cloudsql db associated with the email id of the user, and also add a timer of 2 minutes
3. Send mail to the user using mailgun's api that contains the verification url. When user clicks the link, it redirects them to the '/verify' endpoint of the web application   
