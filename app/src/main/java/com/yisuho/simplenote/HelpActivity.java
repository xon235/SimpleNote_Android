package com.yisuho.simplenote;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        ViewPager vP = (ViewPager) findViewById(R.id.viewPager);
        vP.setAdapter(new HelpViewPagerAdapter(getLayoutInflater(), this));

        DotsIndicator dI = (DotsIndicator) findViewById(R.id.dots_indicator);
        dI.setViewPager(vP);
    }

}
