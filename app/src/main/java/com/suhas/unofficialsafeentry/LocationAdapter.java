package com.suhas.unofficialsafeentry;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

public class LocationAdapter extends
        RecyclerView.Adapter<LocationAdapter.ViewHolder> {

    private List<SafeEntryLocation> mLocations;
    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView nameTextView;
        public Button check_button;
        public Button open_status;
        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            nameTextView = (TextView) itemView.findViewById(R.id.location_name);
            check_button = (Button) itemView.findViewById(R.id.check_button);
            open_status = (Button) itemView.findViewById(R.id.open_status);
        }

    }

    public LocationAdapter(List<SafeEntryLocation> locations) {
        this.mLocations = locations;
    }

    @Override
    public LocationAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View contactView = inflater.inflate(R.layout.location_row, parent, false);
        ViewHolder viewHolder = new ViewHolder(contactView);
        return viewHolder;
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(LocationAdapter.ViewHolder holder, final int position) {
        final SafeEntryLocation location = mLocations.get(position);
        TextView textView = holder.nameTextView;
        textView.setText(location.getLocationName());
        Button check_button = holder.check_button;
        Button status_button = holder.open_status;
        if (mLocations.get(position).isCheckedIn()) {
            check_button.setText("Check out");
        } else {
            check_button.setText("Check in");
        }
        status_button.setText("Open Status");

        check_button.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void onClick(View view) {
                try {
                    LocationAdapter.this.mLocations.get(position).check(!location.isCheckedIn());
                    notifyItemChanged(position);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        status_button.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                location.showStatus();
            }
        });

        check_button.setEnabled(!location.isButtonsEnabled());
        status_button.setEnabled(!location.isButtonsEnabled());
    }


    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mLocations.size();
    }

}