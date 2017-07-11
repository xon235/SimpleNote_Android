package com.yisuho.simplenote;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hendraanggrian.socialview.commons.Hashtag;
import com.hendraanggrian.socialview.commons.HashtagAdapter;
import com.hendraanggrian.support.utils.view.Views;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by YiSuHo on 07/07/2017.
 */

public class MyHashtagAdapter extends HashtagAdapter {
    public MyHashtagAdapter(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(com.hendraanggrian.socialview.commons.R.layout.widget_socialview_hashtag, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Hashtag item = getItem(position);
        if (item != null) {
            holder.textViewHashtag.setText(item.getHashtag());
            if (Views.setVisible(holder.textViewCount, item.getCount() > -1)) {
                holder.textViewCount.setText(item.getCount() < 2
                        ? item.getCount() + getContext().getString(R.string.post)
                        : NumberFormat.getNumberInstance(Locale.US).format(item.getCount()) + getContext().getString(R.string.posts));
            }
        }
        return convertView;
    }

    private static class ViewHolder {
        @NonNull private final TextView textViewHashtag;
        @NonNull private final TextView textViewCount;

        private ViewHolder(@NonNull View view) {
            textViewHashtag = (TextView) view.findViewById(com.hendraanggrian.socialview.commons.R.id.textViewHashtag);
            textViewCount = (TextView) view.findViewById(com.hendraanggrian.socialview.commons.R.id.textViewCount);
        }
    }
}
