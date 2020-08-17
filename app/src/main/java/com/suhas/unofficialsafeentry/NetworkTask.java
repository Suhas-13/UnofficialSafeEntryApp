package com.suhas.unofficialsafeentry;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkTask extends AsyncTask<String, Void, Void> {
    // This is the JSON body of the post

    String postData;
    int requestType;
    String tenantId;
    String startTransactionUrl ="https://backend.safeentry-qr.gov.sg/api/v2/person";

    String nameUrl = "https://backend.safeentry-qr.gov.sg/api/v2/transaction/";
    // This is a constructor that allows you to pass in the JSON body
    public NetworkTask(String postData, String tenantId, int requestType) {
        if (postData != null) {
            this.postData = postData;
            this.requestType=requestType;
            this.tenantId=tenantId;
        }
    }



    private String convertInputStreamToString(InputStream inputStream) {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
    // This is a function that we are overriding from AsyncTask. It takes Strings as parameters because that is what we defined for the parameters of our async task
    @Override
    protected Void doInBackground(String... params) {

        try {
            // This is getting the url from the string we passed in
            URL url = new URL(startTransactionUrl);

            // Create the urlConnection
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();


            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            if (requestType==0) {
                urlConnection.setRequestProperty("accept", "*/*");
                urlConnection.setRequestProperty("accept-encoding", "gzip, deflate");
                urlConnection.setRequestProperty("content-type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty("connection", "keep-alive");
                urlConnection.setRequestMethod("POST");
                if (this.postData != null) {
                    OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                    writer.write(postData);
                    writer.flush();
                }
                int statusCode = urlConnection.getResponseCode();
                if (statusCode ==  200) {
                    InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    String response = convertInputStreamToString(inputStream);
                    JSONObject json = new JSONObject(response);
                    String transactionId= (String) json.getJSONObject("message").get("transactionId");
                    url = new URL(nameUrl+transactionId);
                    urlConnection.setDoInput(true);
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestMethod("GET");
                    statusCode=urlConnection.getResponseCode();

                } else {
                    Log.d("TEST", String.valueOf(statusCode));
                }
            }


        } catch (Exception e) {
            Log.d("TEST", e.getLocalizedMessage());
        }
        return null;
    }
}