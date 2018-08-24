package com.example.andre.ascomel;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaActivity extends AppCompatActivity {

    private ArrayList<String> songs = new ArrayList<>();
    private MediaPlayer mediaPlayer = new MediaPlayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);
        findViewById(R.id.play_button).setEnabled(false);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String youTubeId = intent.getStringExtra("searchPattern");
//        String youTubeId = youTubeLink.substring(youTubeLink.indexOf('=') + 1);

        String savedeoUrl = "http://savedeo.online/download?url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3D";

        String url = savedeoUrl + youTubeId;

        new Page().execute(url);
    }

    public void startMedia(View view) {
        mediaPlayer.start();
    }

    public void pauseMedia(View view) {
        mediaPlayer.pause();
    }

    public void stopMedia(View view) {
        mediaPlayer.stop();
    }

    private class Page extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {
            String html = "";
            try {
                html = Jsoup.connect(url[0]).get().html();
            } catch (IOException e) {
                e.printStackTrace();
            }

            int start = html.indexOf("<th><span class=\"fa-music\">");
            html = html.substring(start);
            BufferedReader reader = new BufferedReader(new StringReader(html));
            String str = "";
            try {
                while ((str = reader.readLine()) != null) {
                    if (str.contains("href")) {
                        int st = str.indexOf("\"") + 1;
                        int sf = str.indexOf("\"", st);
                        String link = str.substring(st, sf).replaceAll("amp;", "");
                        songs.add(link);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }


            String url3 = "https://ascomel.000webhostapp.com/wp-content/uploads/2018/08/Anne-Marie-2002-Official-Video-192-kbps.mp3";
            try {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDataSource(songs.get(0));
                mediaPlayer.prepare(); // might take long! (for buffering, etc)
            } catch (IllegalArgumentException | IllegalStateException | IOException e) {
                e.printStackTrace();
            }
            return html;
        }

        @Override
        protected void onPostExecute(String html) {
            findViewById(R.id.play_button).setEnabled(true);
        }
    }
}
