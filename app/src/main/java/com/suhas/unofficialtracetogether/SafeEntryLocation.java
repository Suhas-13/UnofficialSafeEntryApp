package com.suhas.unofficialtracetogether;
import java.time.Instant;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static android.content.Context.MODE_PRIVATE;

public class SafeEntryLocation implements Comparable<SafeEntryLocation>{
    private boolean checkedIn;
    private String locationName = null;
    private String tenantId;
    private String locationId;
    private static String baseBackendUrl = "https://backend.safeentry-qr.gov.sg";
    private static String baseUrl = "https://www.safeentry-qr.gov.sg";
    private String transactionId;
    private String statusUrl;
    private String[] titleList;
    private String[] idList;
    private long currentTime;
    private static boolean buttonsEnabled = false;

    public static boolean isButtonsEnabled() {
        return buttonsEnabled;
    }

    public static void setButtonsEnabled(boolean buttonsEnabled) {
        SafeEntryLocation.buttonsEnabled = buttonsEnabled;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("checkedIn",checkedIn);
        json.put("locationName",locationName);
        json.put("tenantId",tenantId);
        json.put("transactionId",transactionId);
        json.put("statusUrl",statusUrl);
        return json;
    }
    public String getStatusUrl() {
        return statusUrl;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime() {
        this.currentTime=System.currentTimeMillis()/1000;
    }

    public void setStatusUrl(String statusUrl) {
        this.statusUrl = statusUrl;
        saveObject();
    }
    public void showStatus() {
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.getContext());
        alert.setTitle("");
        WebView wv = new WebView(MainActivity.getContext());
        wv.getSettings().setJavaScriptEnabled(true);
        wv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1)
            {
                return true;
            }
        });
        wv.setInitialScale(250);
        wv.getSettings().setSupportZoom(true);
        wv.getSettings().setDisplayZoomControls(true);
        wv.loadUrl(baseUrl + "/complete/" + this.tenantId + "/" + this.transactionId);
        alert.setView(wv);
        alert.setNegativeButton("Done", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int id){
            }
        });
        alert.show();
    }
    public void saveObject() {
        String locationJson = MainActivity.getGson().toJson(SafeEntryLocation.this);
        SharedPreferences sharedPreferences = MainActivity.getContext().getSharedPreferences("location_info", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(tenantId, locationJson);
        editor.apply();
    }
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        saveObject();
    }


    public SafeEntryLocation(String tenantId) {
        setCurrentTime();
        this.tenantId=tenantId.toUpperCase();
        this.checkedIn=false;
        this.buttonsEnabled=true;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
        saveObject();
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
        saveObject();
    }

    public boolean isCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(boolean checkedIn) {
        this.checkedIn = checkedIn;
        saveObject();
    }

    public void setup() {

        if (MainActivity.getNric() == "" || MainActivity.getPhone() == "") {
            return;
        }
        StringRequest nameRequest = new StringRequest(

                Request.Method.GET, baseBackendUrl + "/api/v2/building?client_id=" + tenantId,
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                public void onResponse(String response) {
                    JSONObject json = null;
                    try {
                        json = new JSONObject(response);
                        JSONArray tenantList = json.getJSONObject("temperaturepass").getJSONArray("tenants");
                        Log.d("TEST", String.valueOf(tenantList.length()));
                        if (tenantList.length() == 1) {
                            Log.d("TEST","ONLY 1");
                            JSONObject jsonObject = (JSONObject) tenantList.get(0);
                            SafeEntryLocation.this.locationName=(String) json.getString("venueName");
                            Log.d("TEST", SafeEntryLocation.this.locationName);
                            SafeEntryLocation.this.locationId=jsonObject.getString("id");
                            MainActivity.addItem(SafeEntryLocation.this, 0);
                            try {
                                check(true);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            titleList= new String[tenantList.length()];
                            idList= new String[tenantList.length()];
                            for (int i=0; i<tenantList.length(); i++) {
                                JSONObject currentItem = (JSONObject) tenantList.get(i);
                                titleList[i]= currentItem.getString("name");
                                idList[i]=currentItem.getString("id");
                            }
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getContext());
                            builder.setTitle("Select location");
                            builder.setItems(titleList, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    locationId=idList[which];
                                    locationName=titleList[which];
                                    saveObject();
                                    MainActivity.addItem(SafeEntryLocation.this, 0);
                                    try {
                                        check(true);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("TEST", String.valueOf(error));
                }
            }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("accept", "*/*");
                headers.put("accept-encoding", "gzip, deflate, br");
                headers.put("connection", "keep-alive");
                return headers;
            }
        };
        MainActivity.requestQueue.add(nameRequest);

    }

    @Override
    public int compareTo(SafeEntryLocation o) {
        return (new Long(getCurrentTime()).compareTo(new Long(o.getCurrentTime()))/-1);
    }
    public void check(boolean checkIn) throws IOException, JSONException {
        setButtonsEnabled(false);
        String phoneNumber=MainActivity.getPhone();
        String NRIC=MainActivity.getNric();
        MainActivity.getLocations().indexOf(SafeEntryLocation.this);
        if (phoneNumber == "" || NRIC == "") {
            return;
        }
        setCurrentTime();
        int currentIndex=MainActivity.getLocations().indexOf(SafeEntryLocation.this);
        MainActivity.moveItem(currentIndex, 0);
        final String actionType;
        phoneNumber = android.util.Base64.encodeToString(phoneNumber.getBytes("UTF-8"), Base64.NO_WRAP);
        if (checkIn) {
            actionType="checkin";
        }
        else {
            actionType = "checkout";
        }
        setCheckedIn(!checkedIn);
        final String data = "mobileno=" + phoneNumber + "=&client_id=" + this.tenantId + "&subentity=" + locationId + "&hostname=null&systemType=safeentry&mobilenoEncoded=true&sub=" + NRIC + "&actionType=" + actionType + "&subType=uinfin&rememberMe=false";
        StringRequest transactionRequest = new StringRequest(
                Request.Method.POST, baseBackendUrl + "/api/v2/person",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        JSONObject json = null;
                        try {
                            json = new JSONObject(response);
                            transactionId = (String) json.getJSONObject("message").get("transactionId");
                            statusUrl = (baseUrl + "/complete/" + tenantId + "/" + transactionId);
                            showStatus();
                            setButtonsEnabled(true);
                            saveObject();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("TEST", String.valueOf(error));
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("accept", "*/*");
                headers.put("accept-encoding", "gzip, deflate");
                headers.put("content-type", "application/x-www-form-urlencoded");
                headers.put("connection", "keep-alive");
                return headers;
            }
            @Override
            public byte[] getBody() {
                try {
                    return data == null ? null : data.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    Log.d("TEST","ERROR REQUEST");
                    return null;
                }
            }
        };
        MainActivity.requestQueue.add(transactionRequest);
    }
}
