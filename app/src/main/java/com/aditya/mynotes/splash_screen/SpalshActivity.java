package com.aditya.mynotes.splash_screen;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;

import com.aditya.mynotes.R;
import com.aditya.mynotes.activities.MainActivity;

public class SpalshActivity extends AppCompatActivity {

    ProgressBar SplashprogressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spalsh);

        SplashprogressBar = findViewById(R.id.SplashprogressBar);
        // SplashprogressBar.setVisibility(View.INVISIBLE);

        //setting up our progress bar visible while saving data into our database
        SplashprogressBar.setVisibility(View.VISIBLE);

        //for setting up the colour in progress bar. This modifies the appearance of all progress bars in your app
       // SplashprogressBar.getProgressDrawable().setColorFilter(
                //Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);

        //To only modify one specific progress bar, do this:
        Drawable progressDrawable = SplashprogressBar.getProgressDrawable().mutate();
        progressDrawable.setColorFilter(Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN);
        SplashprogressBar.setProgressDrawable(progressDrawable);

        //animating our progress bar
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(SplashprogressBar, "progress", 0, 100);
        progressAnimator.setDuration(7000);
        progressAnimator.setInterpolator(new LinearInterpolator());
        progressAnimator.start();

        //the code below will open the main Activity after 2 seconds after the splash screen is deployed
        Handler handler = new Handler();
        //now the handler class have the method post delayed that will delay the launching of our main activity
        //here Runnable is the part of the thread this is going to this is going to run for the given seconds
        /*
        So in a nutShell the code below will open our main Activity after 2 seconds
         */
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //here we will write the code that will open our main Activity
                //the code here will only execute after 2 seconds i.e = delayMillis: 2000 have passed
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
                //adding animation when opening new activity
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        },7000);

    }
    }