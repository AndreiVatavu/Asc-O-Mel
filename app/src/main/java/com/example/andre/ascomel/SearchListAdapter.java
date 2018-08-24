package com.example.andre.ascomel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SearchListAdapter extends BaseAdapter {

    private Context context;
    private List<Pair<String, Bitmap>> videoInfo;
    LayoutInflater inflater;

    public SearchListAdapter(Context context, List<Pair<String, String>> videoInfo) {
        this.context = context;
        this.videoInfo = new ArrayList<>();
        for (Pair<String, String> iter : videoInfo) {
            try {
                // Get url of image
                URL imageUrl = new URL(iter.second);
                // Get image from url
                Bitmap bmp = BitmapFactory.decodeStream(imageUrl.openConnection().getInputStream());
                this.videoInfo.add(new Pair<>(iter.first, bmp));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        inflater = (LayoutInflater.from(context));
    }


    @Override
    public int getCount() {
        return videoInfo.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = inflater.inflate(R.layout.activity_listview, null);
        TextView name = (TextView) view.findViewById(R.id.video_name);
        ImageView icon = (ImageView) view.findViewById(R.id.icon);
        // Set the name of the video
        name.setText(videoInfo.get(position).first);
        // Set the image to the ImageView
        icon.setImageBitmap(videoInfo.get(position).second);

        return view;
    }
}
