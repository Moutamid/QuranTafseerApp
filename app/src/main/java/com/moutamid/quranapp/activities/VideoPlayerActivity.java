package com.moutamid.quranapp.activities;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.fxn.stash.Stash;
import com.moutamid.quranapp.Constants;
import com.moutamid.quranapp.R;
import com.moutamid.quranapp.databinding.ActivityVideoPlayerBinding;
import com.moutamid.quranapp.model.QuranChapter;
import com.moutamid.quranapp.model.VideoModel;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

public class VideoPlayerActivity extends AppCompatActivity {
    private static final String TAG = "VideoPlayerActivity";

    private ActivityVideoPlayerBinding b;
    ArrayList<VideoModel> videoModelArrayList = Stash.getArrayList(Constants.LIST, VideoModel.class);

    private int currentPosition = 0;

    YouTubePlayer youTubePlayerObject;

    boolean isFirstTime = true;

    QuranChapter[] chaptersObjectArray = new QuranChapter[114];
    CharSequence[] chapterTexts = new CharSequence[114];

    boolean isSurahListInitialized = false;

    String currentState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityVideoPlayerBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        currentPosition = Stash.getInt(Constants.LAST_OPENED_POSITION, 0);
        getLifecycle().addObserver(b.youtubePlayerView);

        Objects.requireNonNull(getSupportActionBar())
                .setTitle(videoModelArrayList.get(currentPosition).title);

        b.youtubePlayerView.addYouTubePlayerListener(new YouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                youTubePlayer.loadVideo(videoModelArrayList.get(currentPosition).linkId, 0);
                youTubePlayerObject = youTubePlayer;
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState playerState) {
                Log.d(TAG, "onStateChange: " + playerState);

                currentState = playerState.toString();

                if (playerState == PlayerConstants.PlayerState.PLAYING && isFirstTime) {
                    isFirstTime = false;

                    int progresss = videoModelArrayList.get(currentPosition).progress;
                    if (progresss > 6)
                        progresss -= 5;

                    Log.d(TAG, "onStateChange: currentPosition: " + progresss);
                    youTubePlayer.seekTo((float) progresss);
                }

            }

            @Override
            public void onPlaybackQualityChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlaybackQuality playbackQuality) {

            }

            @Override
            public void onPlaybackRateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlaybackRate playbackRate) {

            }

            @Override
            public void onError(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerError playerError) {

            }

            @Override
            public void onCurrentSecond(@NonNull YouTubePlayer youTubePlayer, float v) {
//                Log.d(TAG, "onCurrentSecond: " + (int) v);
                videoModelArrayList.get(currentPosition).progress = (int) v;

            }

            @Override
            public void onVideoDuration(@NonNull YouTubePlayer youTubePlayer, float v) {
                Log.d(TAG, "onVideoDuration: " + (int) v);
                videoModelArrayList.get(currentPosition).totalProgress = (int) v;

            }

            @Override
            public void onVideoLoadedFraction(@NonNull YouTubePlayer youTubePlayer, float v) {

            }

            @Override
            public void onVideoId(@NonNull YouTubePlayer youTubePlayer, @NonNull String s) {

            }

            @Override
            public void onApiChange(@NonNull YouTubePlayer youTubePlayer) {

            }
        });

        b.youtubePlayerView.enableBackgroundPlayback(true);

        fetchSurah();

        b.previousBtnVideo.setOnClickListener(v -> {
            if (currentPosition != 0) {
                currentPosition--;
                Stash.put(Constants.LAST_OPENED_POSITION, currentPosition);
                recreate();
            } else {
                Toast.makeText(VideoPlayerActivity.this, "Last", Toast.LENGTH_SHORT).show();
            }
        });

        b.nextBtnVideo.setOnClickListener(v -> {
            if (currentPosition != 0) {
                currentPosition++;
                Stash.put(Constants.LAST_OPENED_POSITION, currentPosition);
                recreate();
            } else {
                Toast.makeText(VideoPlayerActivity.this, "Last", Toast.LENGTH_SHORT).show();
            }
        });

        b.backwardBtnVideo.setOnClickListener(v -> {
            try {
                youTubePlayerObject.seekTo(videoModelArrayList.get(currentPosition).progress - 6);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        b.forwardBtnVideo.setOnClickListener(v -> {
            try {
                youTubePlayerObject.seekTo(videoModelArrayList.get(currentPosition).progress + 6);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        b.previousBtnAyah.setOnClickListener(v -> {
            if (getLastSurahNumber() != 1) {
                storeLastSurahNumber(getLastSurahNumber() - 1);
                fetchSurah();
            }
        });

        b.nextBtnAyah.setOnClickListener(v -> {
            if (getLastSurahNumber() != 114) {
                storeLastSurahNumber(getLastSurahNumber() + 1);
                fetchSurah();
            }
        });

        b.previousBtnAyah.setOnLongClickListener(v -> {
            showSurahDialog();
            return false;
        });

        b.nextBtnAyah.setOnLongClickListener(v -> {
            showSurahDialog();
            return false;
        });

        new Thread(() -> initSurahList()).start();

        b.playPauseBtnVideo.setOnClickListener(v -> {
            if (currentState.equals(PlayerConstants.PlayerState.PLAYING.toString())) {
                youTubePlayerObject.pause();
                b.playPauseBtnVideo.setImageResource(R.drawable.img);
            } else if (currentState.equals(PlayerConstants.PlayerState.PAUSED.toString())) {
                b.playPauseBtnVideo.setImageResource(R.drawable.img_1);
                youTubePlayerObject.play();
            }

        });

    }

    private void fetchSurah() {
        b.surahText.setText("Loading...");

        new Thread(() -> {
            String data = getHtmlString("http://api.alquran.cloud/v1/surah/" + getLastSurahNumber());
            Log.d(TAG, "onCreate: data: " + data);

            try {
                JSONObject jsonObject = new JSONObject(data);

                JSONObject dataObject = jsonObject.getJSONObject("data");

                int surahNumber = dataObject.getInt("number");
                String surahName = dataObject.getString("name");
                String surahNameEnglish = dataObject.getString("englishName");
                int numberOfAyahs = dataObject.getInt("numberOfAyahs");

                String finalSurahName = surahNumber
                        + ": " + surahName
                        + " [" + surahNameEnglish + "] "
                        + "(" + numberOfAyahs + ")";

                JSONArray ayahsArray = dataObject.getJSONArray("ayahs");
                String surahText = "";

                for (int i = 0; i < ayahsArray.length(); i++) {
                    JSONObject ayahObject = ayahsArray.getJSONObject(i);
                    surahText += ayahObject.getInt("numberInSurah") + ") " + ayahObject.getString("text") + "\n\n";
                }

                String finalSurahText = surahText;
                runOnUiThread(() -> {
                    b.surahText.setText(finalSurahText);
                    b.surahName.setText(finalSurahName);
                });

            } catch (JSONException e) {
                Log.d(TAG, "onCreate: ERROR while parsing the json: " + e.getMessage());
            }

        }).start();
    }

    private int getLastSurahNumber() {
        return Stash.getInt(Constants.LAST_SURAH_NUMBER, 1);
    }

    private void storeLastSurahNumber(int number) {
        Stash.put(Constants.LAST_SURAH_NUMBER, number);
    }

    private void initSurahList() {

// Initialize each element of the array with the corresponding chapter information
        chaptersObjectArray[0] = new QuranChapter(1, "Al-Fatihah");
        chaptersObjectArray[1] = new QuranChapter(2, "Al-Baqarah");
        chaptersObjectArray[2] = new QuranChapter(3, "Ali 'Imran");
        chaptersObjectArray[3] = new QuranChapter(4, "An-Nisa");
        chaptersObjectArray[4] = new QuranChapter(5, "Al-Ma'idah");
        chaptersObjectArray[5] = new QuranChapter(6, "Al-An'am");
        chaptersObjectArray[6] = new QuranChapter(7, "Al-A'raf");
        chaptersObjectArray[7] = new QuranChapter(8, "Al-Anfal");
        chaptersObjectArray[8] = new QuranChapter(9, "At-Tawbah");
        chaptersObjectArray[9] = new QuranChapter(10, "Yunus");
        chaptersObjectArray[10] = new QuranChapter(11, "Hud");
        chaptersObjectArray[11] = new QuranChapter(12, "Yusuf");
        chaptersObjectArray[12] = new QuranChapter(13, "Ar-Ra'd");
        chaptersObjectArray[13] = new QuranChapter(14, "Ibrahim");
        chaptersObjectArray[14] = new QuranChapter(15, "Al-Hijr");
        chaptersObjectArray[15] = new QuranChapter(16, "An-Nahl");
        chaptersObjectArray[16] = new QuranChapter(17, "Al-Isra");
        chaptersObjectArray[17] = new QuranChapter(18, "Al-Kahf");
        chaptersObjectArray[18] = new QuranChapter(19, "Maryam");
        chaptersObjectArray[19] = new QuranChapter(20, "Taha");
        chaptersObjectArray[20] = new QuranChapter(21, "Al-Anbya");
        chaptersObjectArray[21] = new QuranChapter(22, "Al-Hajj");
        chaptersObjectArray[22] = new QuranChapter(23, "Al-Mu'minun");
        chaptersObjectArray[23] = new QuranChapter(24, "An-Nur");
        chaptersObjectArray[24] = new QuranChapter(25, "Al-Furqan");
        chaptersObjectArray[25] = new QuranChapter(26, "Ash-Shu'ara");
        chaptersObjectArray[26] = new QuranChapter(27, "An-Naml");
        chaptersObjectArray[27] = new QuranChapter(28, "Al-Qasas");
        chaptersObjectArray[28] = new QuranChapter(29, "Al-'Ankabut");
        chaptersObjectArray[29] = new QuranChapter(30, "Ar-Rum");
        chaptersObjectArray[30] = new QuranChapter(31, "Luqman");
        chaptersObjectArray[31] = new QuranChapter(32, "As-Sajdah");
        chaptersObjectArray[32] = new QuranChapter(33, "Al-Ahzab");
        chaptersObjectArray[33] = new QuranChapter(34, "Saba");
        chaptersObjectArray[34] = new QuranChapter(35, "Fatir");
        chaptersObjectArray[35] = new QuranChapter(36, "Ya-Sin");
        chaptersObjectArray[36] = new QuranChapter(37, "As-Saffat");
        chaptersObjectArray[37] = new QuranChapter(38, "Sad");
        chaptersObjectArray[38] = new QuranChapter(39, "Az-Zumar");
        chaptersObjectArray[39] = new QuranChapter(40, "Ghafir");
        chaptersObjectArray[40] = new QuranChapter(41, "Fussilat");
        chaptersObjectArray[41] = new QuranChapter(42, "Ash-Shuraa");
        chaptersObjectArray[42] = new QuranChapter(43, "Az-Zukhruf");
        chaptersObjectArray[43] = new QuranChapter(44, "Ad-Dukhan");
        chaptersObjectArray[44] = new QuranChapter(45, "Al-Jathiyah");
        chaptersObjectArray[45] = new QuranChapter(46, "Al-Ahqaf");
        chaptersObjectArray[46] = new QuranChapter(47, "Muhammad");
        chaptersObjectArray[47] = new QuranChapter(48, "Al-Fath");
        chaptersObjectArray[48] = new QuranChapter(49, "Al-Hujurat");
        chaptersObjectArray[49] = new QuranChapter(50, "Qaf");
        chaptersObjectArray[50] = new QuranChapter(51, "Adh-Dhariyat");
        chaptersObjectArray[51] = new QuranChapter(52, "At-Tur");
        chaptersObjectArray[52] = new QuranChapter(53, "An-Najm");
        chaptersObjectArray[53] = new QuranChapter(54, "Al-Qamar");
        chaptersObjectArray[54] = new QuranChapter(55, "Ar-Rahman");
        chaptersObjectArray[55] = new QuranChapter(56, "Al-Waqi'ah");
        chaptersObjectArray[56] = new QuranChapter(57, "Al-Hadid");
        chaptersObjectArray[57] = new QuranChapter(58, "Al-Mujadila");
        chaptersObjectArray[58] = new QuranChapter(59, "Al-Hashr");
        chaptersObjectArray[59] = new QuranChapter(60, "Al-Mumtahanah");
        chaptersObjectArray[60] = new QuranChapter(61, "As-Saf");
        chaptersObjectArray[61] = new QuranChapter(62, "Al-Jumu'ah");
        chaptersObjectArray[62] = new QuranChapter(63, "Al-Munafiqun");
        chaptersObjectArray[63] = new QuranChapter(64, "At-Taghabun");
        chaptersObjectArray[64] = new QuranChapter(65, "At-Talaq");
        chaptersObjectArray[65] = new QuranChapter(66, "At-Tahrim");
        chaptersObjectArray[66] = new QuranChapter(67, "Al-Mulk");
        chaptersObjectArray[67] = new QuranChapter(68, "Al-Qalam");
        chaptersObjectArray[68] = new QuranChapter(69, "Al-Haqqah");
        chaptersObjectArray[69] = new QuranChapter(70, "Al-Ma'arij");
        chaptersObjectArray[70] = new QuranChapter(71, "Nuh");
        chaptersObjectArray[71] = new QuranChapter(72, "Al-Jinn");
        chaptersObjectArray[72] = new QuranChapter(73, "Al-Muzzammil");
        chaptersObjectArray[73] = new QuranChapter(74, "Al-Muddaththir");
        chaptersObjectArray[74] = new QuranChapter(75, "Al-Qiyamah");
        chaptersObjectArray[75] = new QuranChapter(76, "Al-Insan");
        chaptersObjectArray[76] = new QuranChapter(77, "Al-Mursalat");
        chaptersObjectArray[77] = new QuranChapter(78, "An-Naba");
        chaptersObjectArray[78] = new QuranChapter(79, "An-Nazi'at");
        chaptersObjectArray[79] = new QuranChapter(80, "'Abasa");
        chaptersObjectArray[80] = new QuranChapter(81, "At-Takwir");
        chaptersObjectArray[81] = new QuranChapter(82, "Al-Infitar");
        chaptersObjectArray[82] = new QuranChapter(83, "Al-Mutaffifin");
        chaptersObjectArray[83] = new QuranChapter(84, "Al-Inshiqaq");
        chaptersObjectArray[84] = new QuranChapter(85, "Al-Buruj");
        chaptersObjectArray[85] = new QuranChapter(86, "At-Tariq");
        chaptersObjectArray[86] = new QuranChapter(87, "Al-A'la");
        chaptersObjectArray[87] = new QuranChapter(88, "Al-Ghashiyah");
        chaptersObjectArray[88] = new QuranChapter(89, "Al-Fajr");
        chaptersObjectArray[89] = new QuranChapter(90, "Al-Balad");
        chaptersObjectArray[90] = new QuranChapter(91, "Ash-Shams");
        chaptersObjectArray[91] = new QuranChapter(92, "Al-Lail");
        chaptersObjectArray[92] = new QuranChapter(93, "Ad-Duhaa");
        chaptersObjectArray[93] = new QuranChapter(94, "Ash-Sharh");
        chaptersObjectArray[94] = new QuranChapter(95, "At-Tin");
        chaptersObjectArray[95] = new QuranChapter(96, "Al-'Alaq");
        chaptersObjectArray[96] = new QuranChapter(97, "Al-Qadr");
        chaptersObjectArray[97] = new QuranChapter(98, "Al-Bayyinah");
        chaptersObjectArray[98] = new QuranChapter(99, "Az-Zalzalah");
        chaptersObjectArray[99] = new QuranChapter(100, "Al-'Adiyat");
        chaptersObjectArray[100] = new QuranChapter(101, "Al-Qari'ah");
        chaptersObjectArray[101] = new QuranChapter(102, "At-Takathur");
        chaptersObjectArray[102] = new QuranChapter(103, "Al-'Asr");
        chaptersObjectArray[103] = new QuranChapter(104, "Al-Humazah");
        chaptersObjectArray[104] = new QuranChapter(105, "Al-Fil");
        chaptersObjectArray[105] = new QuranChapter(106, "Quraysh");
        chaptersObjectArray[106] = new QuranChapter(107, "Al-Ma'un");
        chaptersObjectArray[107] = new QuranChapter(108, "Al-Kawthar");
        chaptersObjectArray[108] = new QuranChapter(109, "Al-Kafirun");
        chaptersObjectArray[109] = new QuranChapter(110, "An-Nasr");
        chaptersObjectArray[110] = new QuranChapter(111, "Al-Masad");
        chaptersObjectArray[111] = new QuranChapter(112, "Al-Ikhlas");
        chaptersObjectArray[112] = new QuranChapter(113, "Al-Falaq");
        chaptersObjectArray[113] = new QuranChapter(114, "An-Nas");

        for (int i = 0; i < chaptersObjectArray.length; i++) {
            int index = chaptersObjectArray[i].getIndex();
            String title = chaptersObjectArray[i].getTitle();

            String text = index + ". " + title;
            chapterTexts[i] = text;
        }
        isSurahListInitialized = true;
    }


    private void showSurahDialog() {
        if (!isSurahListInitialized)
            return;

        AlertDialog dialog;

        AlertDialog.Builder builder = new AlertDialog.Builder(VideoPlayerActivity.this);

        builder.setItems(chapterTexts, (dialog1, position) -> {
            storeLastSurahNumber(position + 1);
            dialog1.dismiss();
            fetchSurah();
        });

        dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onPause() {
        Stash.put(Constants.LIST, videoModelArrayList);
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        Stash.put(Constants.LIST, videoModelArrayList);
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        Stash.put(Constants.LIST, videoModelArrayList);
        super.onStop();
    }

    private String getHtmlString(String url) {
        URL google = null;
        try {
            google = new URL(url);
        } catch (final MalformedURLException e) {
            e.printStackTrace();
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(google != null ? google.openStream() : null));
        } catch (final IOException e) {
            e.printStackTrace();

        }
        String input = null;
        StringBuffer stringBuffer = new StringBuffer();
        while (true) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if ((input = in != null ? in.readLine() : null) == null) break;
                }
            } catch (final IOException e) {
                e.printStackTrace();

            }
            stringBuffer.append(input);
        }
        try {
            if (in != null) {
                in.close();
            }
        } catch (final IOException e) {
            e.printStackTrace();

        }
        String htmlData = stringBuffer.toString();

        return htmlData;
    }

}