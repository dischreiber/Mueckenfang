package com.example.mueckenfang;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String HIGHSCORE_SERVER_BASE_URL = "https://myhighscoreserver.appspot.com/highscoreserver";
    private static final String HIGHSCORE_GAME_ID = "mueckenfang";
    private Animation animationEinblenden;
    private Animation animationWackeln;
    private Button startButton;
    private Handler handler = new Handler();
    private Runnable wackelnRunnable = new WackleButton();
    private LinearLayout namenseingabe;
    private Button speichern;
    private List<String> highscoreList = new ArrayList<String>();
    private ListView listview;
    private TopListAdapter adapter;
    private Spinner schwierigkeitsgrad;
    private ArrayAdapter<String> schwierigkeitsgradAdapter;

    @Override
    protected void onResume() {
        super.onResume();
        highScoreAnzeigen();
        View v = findViewById(R.id.wurzel);
        v.startAnimation(animationEinblenden);
        handler.postDelayed(wackelnRunnable, 1000*10);
        TextView tv = (TextView) findViewById(R.id.highscore);
        tv.setText(Integer.toString(leseHighscore()));
        internetHighscores("", 0);
    }

    private int leseHighscore() {
        SharedPreferences pref = getSharedPreferences("GAME", 0);
        return pref.getInt("HIGHSCORE", 0);
    }

    private void highScoreAnzeigen() {
        int highscore = leseHighscore();
        TextView tv = findViewById(R.id.highscore);
        if (highscore > 0) {
            tv.setText(Integer.toString(highscore) + " von " + leseHighScoreName());
        } else {
            tv.setText("-");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(wackelnRunnable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        internetHighscores("", 0);
        namenseingabe = (LinearLayout) findViewById(R.id.namenseingabe);
        speichern = (Button) findViewById(R.id.speichern);
        speichern.setOnClickListener(this);
        namenseingabe.setVisibility(View.INVISIBLE);
        listview = findViewById(R.id.listView);
        adapter = new TopListAdapter(this, 0);
        listview.setAdapter(adapter);

        schwierigkeitsgrad = (Spinner) findViewById(R.id.schwierigkeitsgrad);
        schwierigkeitsgradAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[]{"leicht", "mittel", "schwer"});
        schwierigkeitsgradAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        schwierigkeitsgrad.setAdapter(schwierigkeitsgradAdapter);

        Button button = findViewById(R.id.startButton);
        button.setOnClickListener(this);
        animationEinblenden = AnimationUtils.loadAnimation(this, R.anim.anim);
        animationWackeln = AnimationUtils.loadAnimation(this, R.anim.wackeln);
        startButton = (Button) findViewById(R.id.startButton);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.startButton) {
            int s = schwierigkeitsgrad.getSelectedItemPosition();
            Intent i = new Intent(this, GameActivity.class);
            i.putExtra("schwierigkeitsgrad", s);
            startActivityForResult(i, 1);
        } else if (v.getId() == R.id.speichern) {
            schreibeHighScoreName();
            highScoreAnzeigen();
            internetHighscores(leseHighScoreName(), leseHighscore());
            namenseingabe.setVisibility(View.INVISIBLE);
        }
    }

    private void schreibeHighScoreName() {
        EditText editText = (EditText) findViewById(R.id.spielerName);
        String name = editText.getText().toString().trim();
        SharedPreferences pref = getSharedPreferences("GAME", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("HIGHSCORE_NAME", name);
        editor.commit();
    }

    private String leseHighScoreName() {
        SharedPreferences pref = getSharedPreferences("GAME", 0);
        return pref.getString("HIGHSCORE_NAME", "");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode > leseHighscore()) {
                schreibeHighScore(resultCode);
                namenseingabe.setVisibility(View.VISIBLE);
            }
        }
    }

    private void schreibeHighScore(int highscore) {
        SharedPreferences pref = getSharedPreferences("GAME", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("HIGHSCORE", highscore);
        editor.commit();
    }

    private class WackleButton implements Runnable {
        @Override
        public void run() {
            startButton.startAnimation(animationWackeln);
        }
    }

    private void internetHighscores(final String name, final int points) {
        (new Thread(() -> {
            try {
                Log.i("Liste start", "test");
                URL url = new URL(HIGHSCORE_SERVER_BASE_URL
                        + "?game=" + HIGHSCORE_GAME_ID
                        + "&name=" + URLEncoder.encode(name, "utf-8")
                        + "&points=" + points
                        + "&max=100");
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream(), "UTF8");
                BufferedReader reader = new BufferedReader(inputStreamReader, 2000);

                highscoreList.clear();
                String line = reader.readLine();
                while (line != null) {
                    highscoreList.add(line);
                    Log.i("Liste", line);
                    line = reader.readLine();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            runOnUiThread(() -> {
                adapter.notifyDataSetInvalidated();
                    }

            );
        })).start();
    }

    class TopListAdapter extends ArrayAdapter<String> {

        public TopListAdapter(@NonNull Context context, int resource) {
            super(context, resource);
        }
        @Override
        public int getCount() {
            return highscoreList.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.toplist_element, null);
            }
            TextView tvPlatz = (TextView) convertView.findViewById(R.id.platz);
            tvPlatz.setText(Integer.toString(position+1) + ".");
            TextUtils.SimpleStringSplitter sss = new TextUtils.SimpleStringSplitter(',');
            sss.setString(highscoreList.get(position));
            TextView tvName = (TextView) convertView.findViewById(R.id.name);
            tvName.setText(sss.next());
            TextView tvPunkte = (TextView) findViewById(R.id.punkte);
            tvPunkte.setText(sss.next());
            return convertView;
        }
    }
}