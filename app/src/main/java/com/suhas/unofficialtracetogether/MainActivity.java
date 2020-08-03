package com.suhas.unofficialtracetogether;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.time.Instant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.sort;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    public static RequestQueue requestQueue;
    private static Context context;
    private static LocationAdapter adapter;
    private static List<SafeEntryLocation> locations;
    private static Gson gson;
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;




    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.INTERNET
    };



    public static void addItem(SafeEntryLocation location, int index) {
        locations.add(index,location);
        adapter.notifyDataSetChanged();
    }
    public static void moveItem(int from, int to) {
        if (from < 0 || to < 0 || from > locations.size()-1 || to > locations.size()-1 || from == to) {

        }
        else {
            SafeEntryLocation location = locations.get(from);
            locations.remove(from);
            locations.add(0,location);
            adapter.notifyDataSetChanged();
        }

    }
    public static void removeItem(int index) {
        locations.remove(index);
        adapter.notifyItemRemoved(index);
    }
    public static LocationAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(LocationAdapter adapter) {
        this.adapter = adapter;
    }
    public void updatePosition(int index) {
        adapter.notifyItemChanged(index);
    }

    public static List<SafeEntryLocation> getLocations() {
        return locations;
    }

    public void setLocations(List<SafeEntryLocation> locations) {
        this.locations = locations;
    }

    public static Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    private Button camera;
    public void startQrScan() {
        IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
        integrator.setOrientationLocked(false);
        integrator.setPrompt("");
        integrator.setBeepEnabled(false);
        integrator.initiateScan();
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        String MATCH_PHRASE = "safeentry-qr.gov.sg/tenant";
        String tenantId;
        if (resultCode == RESULT_OK) {
            String contents = intent.getStringExtra("SCAN_RESULT");
            if (contents.indexOf(MATCH_PHRASE) != -1) {
                tenantId = contents.split("/tenant/")[1];
                if (tenantId.length() != 0 && tenantId != null) {
                    SafeEntryLocation location = new SafeEntryLocation(tenantId);
                    try {
                        location.addLocationName();
                        location.check(true);


                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            } else if (resultCode == RESULT_CANCELED) {

            }
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this,
                        "Camera Permission Granted",
                        Toast.LENGTH_SHORT)
                        .show();
            }
            else {
                Toast.makeText(MainActivity.this,
                        "Camera Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }
        else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this,
                        "Storage Permission Granted",
                        Toast.LENGTH_SHORT)
                        .show();
            }
            else {
                Toast.makeText(MainActivity.this,
                        "Storage Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    public static Gson getGson() {
        return gson;
    }


    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }


    public static SharedPreferences.Editor getEditor() {
        return editor;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context=MainActivity.this;
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap
        Network network = new BasicNetwork(new HurlStack());
        this.requestQueue = new RequestQueue(cache, network);
        this.requestQueue.start();
        RecyclerView rvLocation = (RecyclerView) findViewById(R.id.locationList);
        gson = new Gson();
        sharedPreferences=getApplicationContext().getSharedPreferences("location_info", MODE_PRIVATE);
        editor=sharedPreferences.edit();
        Map<String, ?> storedLocations = sharedPreferences.getAll();
        locations = new ArrayList<SafeEntryLocation>();
        for (Map.Entry<String, ?> entry : storedLocations.entrySet()) {
            SafeEntryLocation location = gson.fromJson(String.valueOf(entry.getValue()), SafeEntryLocation.class);
            locations.add(location);
        }
        Collections.sort(locations);
        adapter = new LocationAdapter(locations);
        rvLocation.setAdapter(adapter);
        rvLocation.setLayoutManager(new LinearLayoutManager(this));
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
       camera = findViewById(R.id.camera);
       camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                startQrScan();

            }
        });
    }
}
