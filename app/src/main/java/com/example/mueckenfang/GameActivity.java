package com.example.mueckenfang;

import android.app.Dialog;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Date;
import java.util.Random;

public class GameActivity extends AppCompatActivity implements View.OnClickListener, Runnable {

    public static final int DELAY_MILLIS = 100;
    public static final int ZEITSCHEIBEN = 600;
    private static final long HOECHSTALTER_MS = 2000;
    Boolean spielLaeuft;
    int runde, punkte, muecken, gefangeneMuecken, zeit;
    private float massstab;
    private Random zufallsgenerator = new Random();
    private ViewGroup spielbereich;
    private Handler handler = new Handler();
    private MediaPlayer mp;
    private int schwierigkeitsgrad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);
        massstab = getResources().getDisplayMetrics().density;
        spielbereich = findViewById(R.id.spielbereich);
        mp = MediaPlayer.create(this, R.raw.summen);
        schwierigkeitsgrad = getIntent().getIntExtra("schwierigkeitsgrad", 0);
        spielStarten();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(this);
        mp.release();
    }

    public void spielStarten() {
        spielLaeuft = true;
        runde = 0;
        punkte = 0;
        starteRunde();
    }

    public void starteRunde() {
        runde = runde + 1;
        muecken = runde * (10+schwierigkeitsgrad*10);
        gefangeneMuecken = 0;
        zeit = ZEITSCHEIBEN;
        bildschirmAktualisieren();
        handler.postDelayed(this, DELAY_MILLIS);
    }

    private void bildschirmAktualisieren() {

        FrameLayout hitsFL = findViewById(R.id.bar_hits);
        FrameLayout timeFL = findViewById(R.id.bar_time);

        ViewGroup.LayoutParams hitsLP = hitsFL.getLayoutParams();
        hitsLP.width = Math.round(massstab * 300 * Math.min(gefangeneMuecken,muecken) / muecken);

        ViewGroup.LayoutParams timeLP = timeFL.getLayoutParams();
        timeLP.width = Math.round(massstab * zeit * 300 / ZEITSCHEIBEN);

        TextView pointsTV = findViewById(R.id.pointsTextView);
        TextView roundsTV = findViewById(R.id.roundsTextView);
        TextView timeTV = findViewById(R.id.timeTextView);
        TextView hitsTV = findViewById(R.id.hitsTextView);

        pointsTV.setText(Integer.toString(punkte));
        roundsTV.setText("Runde "+ Integer.toString(runde));
        timeTV.setText(Integer.toString(zeit/(1000/DELAY_MILLIS)));
        hitsTV.setText(Integer.toString(gefangeneMuecken));
    }

    private void zeitHerunterzaehlen() {
        zeit = zeit - 1;
        if (zeit %(1000/DELAY_MILLIS) == 0) {
            float zufallszahl = zufallsgenerator.nextFloat();
            double wahrscheinlickeit = muecken * 1.5 / 60;
            if (wahrscheinlickeit > 1) {
                eineMueckeAnzeigen();
                if (zufallszahl < wahrscheinlickeit - 1) {
                    eineMueckeAnzeigen();
                }
            } else {
                if (zufallszahl < wahrscheinlickeit) {
                    eineMueckeAnzeigen();
                }
            }
        }
        mueckenVerschwinden();
        mueckeBewegen();
        bildschirmAktualisieren();
        if (!pruefeSpielende()) {
            if (!pruefeRundenende()) {
                handler.postDelayed(this::zeitHerunterzaehlen, DELAY_MILLIS);
            }
        }
    }

    private void mueckenVerschwinden() {
        int nummer = 0;
        while (nummer < spielbereich.getChildCount()) {
            ImageView muecke = (ImageView) spielbereich.getChildAt(nummer);
            Date geburtsdatum = (Date) muecke.getTag(R.id.geburtsdatum);
            long alter = (new Date().getTime() - geburtsdatum.getTime());

            if (alter > HOECHSTALTER_MS) {
                spielbereich.removeView(muecke);
            }
            nummer++;
        }
    }

    private void eineMueckeAnzeigen() {
        int breite = spielbereich.getWidth();
        int hoehe = spielbereich.getHeight();
        int muecke_breite = Math.round(massstab*50);
        int muecke_hoehe = Math.round(massstab*42);
        int links = zufallsgenerator.nextInt(breite - muecke_breite );
        int oben = zufallsgenerator.nextInt(hoehe - muecke_breite  );

        // Mücke erzeugen
        ImageView muecke = new ImageView(this);
        muecke.setOnClickListener(this);
        muecke.setTag(R.id.geburtsdatum, new Date());

        // Bewegungsvektor erzeugen
        int vx, vy;
        do {
            vx = zufallsgenerator.nextInt(3)-1;
            vy = zufallsgenerator.nextInt(3)-1;
        } while (vx == 0 && vy ==0);

        muecke.setTag(R.id.vx, new Integer(vx));
        muecke.setTag(R.id.vy, new Integer(vy));

        setzeBild(muecke, vx, vy);

        Log.i("vx vor aufruf", Integer.toString(vx));
        Log.i("vy vor aufruf", Integer.toString(vy));

        vx = (int) Math.round(massstab*vx);
        vy = (int) Math.round(massstab*vy);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(muecke_breite, muecke_hoehe);
        params.leftMargin = links;
        params.topMargin = oben;
        params.gravity = Gravity.TOP + Gravity.LEFT;
        spielbereich.addView(muecke, params);

        // Summen starten
        mp.seekTo(0);
        mp.start();
    }

    private boolean pruefeRundenende() {
        if (gefangeneMuecken >= muecken) {
            starteRunde();
            return true;
        }
        return false;
    }

    private boolean pruefeSpielende() {
        if (zeit == 0 && gefangeneMuecken < muecken) {
            gameOver();
            return true;
        }
        return false;
    }

    private void gameOver() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.gameover);
        dialog.show();
        spielLaeuft = false;
        setResult(punkte);
    }

    @Override
    public void onClick(View muecke) {
        Animation animationTreffer = AnimationUtils.loadAnimation(this, R.anim.treffer);
        animationTreffer.setAnimationListener(new MueckeAnimationListener(muecke));
        muecke.startAnimation(animationTreffer);
        gefangeneMuecken++;
        punkte += 100 + (schwierigkeitsgrad*100);
        bildschirmAktualisieren();
        mp.pause();
        muecke.setOnClickListener(null);
    }

    @Override
    public void run() {
        zeitHerunterzaehlen();
    }

    @Override
    protected void onDestroy() {
        mp.release();
        super.onDestroy();
    }

    private class MueckeAnimationListener implements Animation.AnimationListener {

        private View muecke;
        public MueckeAnimationListener(View m) {
            muecke = m;
        }

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    spielbereich.removeView(muecke);
                }
            });
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

    private void mueckeBewegen() {
        int nummer = 0;
        while (nummer < spielbereich.getChildCount()) {
            ImageView muecke = (ImageView) spielbereich.getChildAt(nummer);
            int vx = (Integer) muecke.getTag(R.id.vx);
            int vy = (Integer) muecke.getTag(R.id.vy);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) muecke.getLayoutParams();
            params.leftMargin += vx*runde;
            params.topMargin += vy*runde;
            muecke.setLayoutParams(params);
            nummer++;
        }
    }

    private void setzeBild (ImageView muecke, int vx, int vy) {
        Log.i("vx", Integer.toString(vx));
        Log.i("vy", Integer.toString(vy));
        if (vx == -1 && vy == -1) {
            muecke.setImageResource(R.drawable.muecke_nw);
            Log.i("Richtung", "NW");
        }
        if (vx == -1 && vy == 0) {
            muecke.setImageResource(R.drawable.muecke_w);
            Log.i("Richtung", "W");
        }
        if (vx == -1 && vy == 1) {
            muecke.setImageResource(R.drawable.muecke_sw);
            Log.i("Richtung", "SW");
        }
        if (vx == 0 && vy == -1) {
            muecke.setImageResource(R.drawable.muecke_n);
            Log.i("Richtung", "N");
        }
        if (vx == 0 && vy == 1) {
            muecke.setImageResource(R.drawable.muecke_s);
            Log.i("Richtung", "S");
        }
        if (vx == 1 && vy == -1) {
            muecke.setImageResource(R.drawable.muecke_no);
            Log.i("Richtung", "NO");
        }
        if (vx == 1 && vy == 0) {
            muecke.setImageResource(R.drawable.muecke_o);
            Log.i("Richtung", "O");
        }
        if (vx == 1 && vy == 1) {
            muecke.setImageResource(R.drawable.muecke_so);
            Log.i("Richtung", "SO");
        }
    }
}