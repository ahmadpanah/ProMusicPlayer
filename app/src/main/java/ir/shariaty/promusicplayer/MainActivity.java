package ir.shariaty.promusicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SongChangeListener {

    private final List<MusicList> musicLists = new ArrayList<>();
    private RecyclerView musicRecyclerView;
    private MediaPlayer mediaPlayer;
    private TextView endTime, startTime;
    private boolean isPlaying = false;
    private ImageView playPauseImg;
    private SeekBar playerSeekBar;
    private Timer timer;
    private int currentSongListPosition = 0;
    private MusicAdapter musicAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View decodeView = getWindow().getDecorView();

        int options = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decodeView.setSystemUiVisibility(options);


        setContentView(R.layout.activity_main);

        final LinearLayout searchBtn = findViewById(R.id.searchBtn);
        final LinearLayout menuBtn = findViewById(R.id.menuBtn);
        musicRecyclerView = findViewById(R.id.musicRecyclerView);
        final CardView playPauseCard = findViewById(R.id.playPauseCard);
        playPauseImg = findViewById(R.id.playPauseImg);
        final ImageView nextBtn = findViewById(R.id.nextBtn);
        final ImageView prevBtn = findViewById(R.id.previousBtn);

        startTime = findViewById(R.id.startTime);
        endTime = findViewById(R.id.endTime);

        playerSeekBar = findViewById(R.id.playerSeekBar);

        musicRecyclerView.setHasFixedSize(true);
        musicRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mediaPlayer = new MediaPlayer();


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                getMusicFiles();
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE} , 11);
            }
            else {
                getMusicFiles();
            }
        }

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int nextSongListPosition = currentSongListPosition + 1;

                if (nextSongListPosition >= musicLists.size()) {
                    nextSongListPosition = 0;
                }

                musicLists.get(currentSongListPosition).setPlaying(false);
                musicLists.get(nextSongListPosition).setPlaying(true);

                musicAdapter.updateList(musicLists);

                musicRecyclerView.scrollToPosition(nextSongListPosition);
                onChanged(nextSongListPosition);
            }
        });

        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int prevSongListPosition = currentSongListPosition - 1;

                if (prevSongListPosition < 0) {
                    prevSongListPosition = musicLists.size() - 1;
                }

                musicLists.get(currentSongListPosition).setPlaying(false);
                musicLists.get(prevSongListPosition).setPlaying(true);

                musicAdapter.updateList(musicLists);

                musicRecyclerView.scrollToPosition(prevSongListPosition);
                onChanged(prevSongListPosition);
            }
        });

        playPauseCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlaying) {
                    isPlaying = false;

                    mediaPlayer.pause();
                    playPauseImg.setImageResource(R.drawable.play_icon);
                }
                else {
                    isPlaying = true;
                    mediaPlayer.start();
                    playPauseImg.setImageResource(R.drawable.ic_pause);
                }
            }
        });

        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (isPlaying) {
                        mediaPlayer.seekTo(progress);
                    }
                    else {
                        mediaPlayer.seekTo(0);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private void getMusicFiles() {

        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        Cursor cursor = contentResolver.query(uri, null , MediaStore.Audio.Media.DATA+" LIKE?", new String[] {"%.mp3%"},null);

        if (cursor == null) {
            Toast.makeText(this, "Something Went Wrong!", Toast.LENGTH_SHORT).show();
        }
        else if (!cursor.moveToNext()) {
            Toast.makeText(this, "No Music Found!", Toast.LENGTH_SHORT).show();
        }
        else {
            while (cursor.moveToNext()) {

                final String getMusicFileName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                final String getArtistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));

                long cursorId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

                Uri musicFileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursorId);

                String getDuration = "00:00";

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    getDuration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION));
                }

                final MusicList musicList = new MusicList(getMusicFileName, getArtistName , getDuration, false , musicFileUri);
                musicLists.add(musicList);

            }

            musicAdapter = new MusicAdapter(musicLists, MainActivity.this);
            musicRecyclerView.setAdapter(musicAdapter);

        }

        cursor.close();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getMusicFiles();
        }
        else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            View decodeView = getWindow().getDecorView();

            int options = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            decodeView.setSystemUiVisibility(options);
        }
    }

    @Override
    public void onChanged(int position) {

        currentSongListPosition = position;

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            mediaPlayer.reset();
        }

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    mediaPlayer.setDataSource(MainActivity.this, musicLists.get(position).getMusicFile());
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Unable to Play Music", Toast.LENGTH_SHORT).show();
                }
            }
        }).start();

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                final int getTotalDuration = mp.getDuration();

                String generateDuration = String.format(Locale.getDefault(),"%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(getTotalDuration),
                        TimeUnit.MILLISECONDS.toSeconds(getTotalDuration) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getTotalDuration)));

                endTime.setText(generateDuration);
                isPlaying = true;

                mp.start();

                playerSeekBar.setMax(getTotalDuration);

                playPauseImg.setImageResource(R.drawable.ic_pause);
            }
        });

        timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final int getCurrentDuration = mediaPlayer.getCurrentPosition();

                        String generateDuration = String.format(Locale.getDefault(),"%02d:%02d",
                                TimeUnit.MILLISECONDS.toMinutes(getCurrentDuration),
                                TimeUnit.MILLISECONDS.toSeconds(getCurrentDuration) -
                                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getCurrentDuration)));

                        playerSeekBar.setProgress(getCurrentDuration);

                        startTime.setText(generateDuration);
                    }
                });
            }
        }, 1000,1000);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {

                mediaPlayer.reset();

                timer.purge();
                timer.cancel();

                playPauseImg.setImageResource(R.drawable.play_icon);

                playerSeekBar.setProgress(0);

                //When the music is over, play the next music.
                int nextSongListPosition = currentSongListPosition + 1;

                if (nextSongListPosition >= musicLists.size()) {
                    nextSongListPosition = 0;
                }

                musicLists.get(currentSongListPosition).setPlaying(false);
                musicLists.get(nextSongListPosition).setPlaying(true);

                musicAdapter.updateList(musicLists);

                musicRecyclerView.scrollToPosition(nextSongListPosition);
                onChanged(nextSongListPosition);
            }
        });
    }
}