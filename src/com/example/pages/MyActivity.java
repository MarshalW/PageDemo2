package com.example.pages;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.*;

public class MyActivity extends Activity {

    List<PageAnimationView> views = new ArrayList<PageAnimationView>();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        ViewGroup rootView = (ViewGroup) findViewById(R.id.rootView);

        for (int i = 0; i < 5; i++) {
            ViewGroup viewGroup = (ViewGroup) View.inflate(this, R.layout.item, null);
            rootView.addView(viewGroup);

            final PageAnimationView pageAnimationView = new PageAnimationView(this);
            ViewGroup parentView = (ViewGroup) viewGroup.findViewById(R.id.parentView);
            pageAnimationView.setAnimationListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    Toast.makeText(MyActivity.this, "动画结束", Toast.LENGTH_SHORT).show();
                }
            });
            parentView.addView(pageAnimationView);
            views.add(pageAnimationView);

//            View view = new View(this);
//            view.setBackgroundColor(Color.BLUE);
//            view.setAlpha(0.2f);
//            parentView.addView(view);

            View targetView = viewGroup.findViewById(R.id.targetView);
            targetView.setVisibility(View.INVISIBLE);
            pageAnimationView.setTargetView(targetView);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    pageAnimationView.startAnimation();
                }
            }, 1000);
        }
    }

//    PageAnimationView pageAnimationView;

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    protected void onPause() {
        ViewGroup rootView = (ViewGroup) findViewById(R.id.rootView);
        rootView.removeAllViews();

        for (PageAnimationView view : views) {
            view.onPause();
        }
        views.clear();

        super.onPause();
    }
}
