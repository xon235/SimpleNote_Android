package com.yisuho.simplenote;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by YiSuHo on 20/07/2017.
 */

public class HelpViewPagerAdapter extends PagerAdapter {

    Context mContext;
    LayoutInflater mInflater;

    public HelpViewPagerAdapter(LayoutInflater inflater, Context context) {
        super();
        this.mContext = context;
        this.mInflater = inflater;
    }

    @Override
    public int getCount() {
        return 5;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View v = mInflater.inflate(R.layout.help_view_page, null);
        TextView headerTv = (TextView) v.findViewById(R.id.headerTv);
        ImageView backgroundIv = (ImageView) v.findViewById(R.id.backgroundIv);
        TextView subTextTv = (TextView) v.findViewById(R.id.subTextTv);
        switch (position){
            case 0:
                break;
            case 1:
                headerTv.setText("");
                backgroundIv.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.help_tags_text));
                subTextTv.setText(R.string.help_add_hashtag);
                break;
            case 2:
                headerTv.setText(R.string.help_export_import);
                backgroundIv.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.help_export_import_main));
                subTextTv.setText(R.string.help_you_can_now_also_export_import);
                break;
            case 3:
                headerTv.setText("");
                backgroundIv.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.help_export_import_json));
                subTextTv.setText(R.string.help_using_simple_json);
                break;
            case 4:
                headerTv.setText(R.string.help_all_set);
                backgroundIv.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.help_all_set));
                subTextTv.setText(R.string.help_press_the_back_button);
                break;
        }

        container.addView(v);
        return v;
    }

    @Override

    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View)object);
    }

    @Override
    public boolean isViewFromObject(View v, Object obj) {
        return v==obj;
    }
}
