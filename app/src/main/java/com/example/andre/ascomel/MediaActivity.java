package com.example.andre.ascomel;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;


import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

public class MediaActivity extends AppCompatActivity {

    private ArrayList<String> songs = new ArrayList<>();
    private MediaPlayer mediaPlayer = new MediaPlayer();

    private SeekBar positionBar;
    private TextView elapsedTime;
    private TextView totalTime;
    private Button previousSong;
    private Button nextSong;
    private Button playBtn;
    private SeekBar volumeBar;
    private int totalStreamTime;
    private Thread seekBarThread = null;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int currentPosition = msg.what;
            // Update position bar
            positionBar.setProgress(currentPosition);

            // Update elapsed time
            String elapsed = getElapsedTime(currentPosition);
            elapsedTime.setText(elapsed);
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);

        elapsedTime = findViewById(R.id.elapsed_time);
        totalTime = findViewById(R.id.total_time);
        previousSong = findViewById(R.id.previous_song_button);
        nextSong = findViewById(R.id.next_song_button);
        playBtn = findViewById(R.id.play_button);
        playBtn.setEnabled(false);

        positionBar = findViewById(R.id.position_bar);
        positionBar.setEnabled(false);
        positionBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress);
                    positionBar.setProgress(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        // Volume bar
        volumeBar = findViewById(R.id.volume_bar);
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volumeNum = progress / 100f;
                mediaPlayer.setVolume(volumeNum, volumeNum);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String youTubeId = intent.getStringExtra("searchPattern");
//        String youTubeId = youTubeLink.substring(youTubeLink.indexOf('=') + 1);

        String savedeoUrl = "http://savedeo.online/download?url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3D";

        String url = savedeoUrl + youTubeId;

        new Page().execute(url);
    }

    private String getElapsedTime(int time) {
        String elapsed;
        int min = time / 1000 / 60;
        int sec = time / 1000 % 60;
        elapsed = min + ":";
        if (sec < 10 ) {
            elapsed += "0";
        }
        elapsed += sec;
        return elapsed;
    }

    public void startStopMedia(View view) throws InterruptedException {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            playBtn.setBackgroundResource(R.drawable.stop);
        } else {
            mediaPlayer.pause();
            playBtn.setBackgroundResource(R.drawable.play);
        }
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
            if (start == -1) {
                cancel(true);
                return null;
            }
            html = html.substring(start);
            BufferedReader reader = new BufferedReader(new StringReader(html));
            String str;
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


            String url3 = "https://youtubemp3api.com/@download/320-5b86c7ab9252b-11200000-280/mp3/nbsZAIkG9Hs/Florin%2BSalam%2B-%2BBuzunarul%2Bmeu%2Bvorbeste%2B%255Boficial%2Bvideo%255D%2B2018.mp3";
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
            playBtn.setEnabled(true);
            totalStreamTime = mediaPlayer.getDuration();
            positionBar.setEnabled(true);
            positionBar.setMax(totalStreamTime);

            totalTime.setText(getElapsedTime(totalStreamTime));

            // Thread (Update position bar and elapsed time)
            if (seekBarThread != null) {
                seekBarThread.interrupt();
            }
            seekBarThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mediaPlayer != null) {
                        try {
                            Message msg = new Message();
                            msg.what = mediaPlayer.getCurrentPosition();
                            handler.sendMessage(msg);
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            seekBarThread.start();
        }
    }
}
