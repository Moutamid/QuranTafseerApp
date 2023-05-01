package com.moutamid.quranapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.fxn.stash.Stash;
import com.google.android.material.card.MaterialCardView;
import com.moutamid.quranapp.Constants;
import com.moutamid.quranapp.R;
import com.moutamid.quranapp.databinding.ActivityMainBinding;
import com.moutamid.quranapp.model.VideoModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private ActivityMainBinding b;
    ArrayList<VideoModel> videoModelArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(b.getRoot());

        if (Stash.getBoolean(Constants.IS_FIRST_TIME, true)) {
            Log.d(TAG, "onCreate: IS FIRST TIME");
            Stash.put(Constants.IS_FIRST_TIME, false);
            // IS FIRST TIME

            videoModelArrayList = new ArrayList<>();
            try {
                JSONArray jsonArray = new JSONArray(loadJSONFromAsset());

                for (int i = 0; i < jsonArray.length(); i++) {

                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                    VideoModel model = new VideoModel();
                    model.title = jsonObject.getString("title");
                    model.linkId = jsonObject.getString("link");

                    model.completed = false;
                    model.progress = 0;
                    model.totalProgress = 100;

                    videoModelArrayList.add(model);

                }

                Stash.put(Constants.LIST, videoModelArrayList);

                runOnUiThread(() -> initRecyclerView());

            } catch (JSONException e) {
                Log.d(TAG, "onCreate: ERROR: " + e.getMessage());
            }

        } else {
            Log.d(TAG, "onCreate: NOT FIRST TIME");
            // NOT FIRST TIME

            videoModelArrayList = Stash.getArrayList(Constants.LIST, VideoModel.class);

            initRecyclerView();
        }

    }

    private RecyclerView conversationRecyclerView;
    private RecyclerViewAdapterMessages adapter;

    private void initRecyclerView() {

        conversationRecyclerView = b.videosRv;
//        conversationRecyclerView.addItemDecoration(new DividerItemDecoration(conversationRecyclerView.getContext(), DividerItemDecoration.VERTICAL));
        adapter = new RecyclerViewAdapterMessages();// pay-device.com/developer
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        //linearLayoutManager.setReverseLayout(true);
        conversationRecyclerView.setLayoutManager(linearLayoutManager);
        conversationRecyclerView.setHasFixedSize(true);
        conversationRecyclerView.setNestedScrollingEnabled(false);

        conversationRecyclerView.setAdapter(adapter);

        int lastPosition = Stash.getInt(Constants.LAST_OPENED_POSITION, 0);
        if (lastPosition != 0)
            lastPosition--;

        conversationRecyclerView.scrollToPosition(lastPosition);

    }

    private class RecyclerViewAdapterMessages extends RecyclerView.Adapter
            <RecyclerViewAdapterMessages.ViewHolderRightMessage> {

        @NonNull
        @Override
        public ViewHolderRightMessage onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_video, parent, false);
            return new ViewHolderRightMessage(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolderRightMessage holder, int p) {
            int position1 = holder.getAdapterPosition();
            VideoModel model = videoModelArrayList.get(position1);

            Glide.with(getApplicationContext())
                    .load("https://i.ytimg.com/vi/" + model.linkId + "/default.jpg")
                    .apply(new RequestOptions()
                            .placeholder(R.color.grey)
                            .error(R.color.red)
                    )
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .into(holder.imageView);

            holder.title.setText(model.title);

            holder.progressBar.setMax(model.totalProgress);
            holder.progressBar.setProgress(model.progress);

            holder.checkBox.setChecked(model.completed);

            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    model.completed = isChecked;
//                    model.totalProgress = 100;
//                    model.progress = 100;
                    Stash.put(Constants.LIST, videoModelArrayList);
//                    notifyItemChanged(position1);
                }
            });

            holder.layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, VideoPlayerActivity.class));
                    Stash.put(Constants.LAST_OPENED_POSITION, position1);
                }
            });

            int lastPosition = Stash.getInt(Constants.LAST_OPENED_POSITION, 0);
            if (lastPosition == position1) {
                holder.layout.setCardBackgroundColor(getResources().getColor(R.color.grey));
            } else {
                holder.layout.setCardBackgroundColor(getResources().getColor(R.color.white));
            }

        }

        @Override
        public int getItemCount() {
            if (videoModelArrayList == null)
                return 0;
            return videoModelArrayList.size();
        }

        public class ViewHolderRightMessage extends RecyclerView.ViewHolder {

            TextView title;
            ImageView imageView;
            AppCompatCheckBox checkBox;
            ProgressBar progressBar;
            MaterialCardView layout;

            public ViewHolderRightMessage(@NonNull View v) {
                super(v);
                title = v.findViewById(R.id.titleTv);
                imageView = v.findViewById(R.id.imagevieww);
                checkBox = v.findViewById(R.id.completedCheckBox);
                progressBar = v.findViewById(R.id.progressBar);
                layout = v.findViewById(R.id.parentLayout);

            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        videoModelArrayList = Stash.getArrayList(Constants.LIST, VideoModel.class);
        initRecyclerView();
    }

    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("data.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

}