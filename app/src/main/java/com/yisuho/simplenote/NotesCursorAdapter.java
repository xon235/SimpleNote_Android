package com.yisuho.simplenote;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by xon23 on 2016-07-07.
 */
public class NotesCursorAdapter extends CursorAdapter {

    boolean mEnableImportant;

    public NotesCursorAdapter(Context context, Cursor c, int flags, boolean enableImportant) {
        super(context, c, flags);
        mEnableImportant = enableImportant;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(
                R.layout.note_list_item, viewGroup, false
        );
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        String noteText = cursor.getString(
          cursor.getColumnIndex(DBOpenHelper.NOTE_TEXT)
        );

        int pos = noteText.indexOf('\n');
        if(pos != -1) {
            noteText = noteText.substring(0, pos) + " ...";
        }

        TextView tvN = (TextView) view.findViewById(R.id.tvNote);
        tvN.setText(noteText);

        String dateText = cursor.getString(cursor.getColumnIndex(DBOpenHelper.NOTE_CREATED));

        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat("yyyy-MM-dd kk:mm:ss", Locale.getDefault());
        Date date =simpleDateFormat.parse(dateText, new ParsePosition(0));
        simpleDateFormat.applyPattern("MMM dd, yyyy");
        dateText = simpleDateFormat.format(date);
        TextView tvD = (TextView) view.findViewById(R.id.tvDate);
        tvD.setText(dateText);

        int important = cursor.getInt(cursor.getColumnIndex(DBOpenHelper.NOTE_IMPORTANT));
        final ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
        if(important == 1){
              imageView.setImageResource(R.drawable.ic_star);
        } else {
              imageView.setImageResource(R.drawable.ic_star_outline);
        }

        String noteId= cursor.getString(
                cursor.getColumnIndex(DBOpenHelper.NOTE_ID)
        );

        if(mEnableImportant){
            imageView.setTag(new MyTag(noteId, important));

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(context instanceof MainActivity){
                        MyTag myTag = (MyTag) view.getTag();
                        ContentValues values = new ContentValues();
                        values.put(DBOpenHelper.NOTE_IMPORTANT, (myTag.noteImportant == 1)? 0 : 1);
                        String noteFilter = DBOpenHelper.NOTE_ID + "=" + myTag.getNoteId();
                        context.getContentResolver().update(NotesProvider.CONTENT_URI_NOTES, values, noteFilter, null);
                        ((MainActivity) context).restartLoader();
                    }
                }
            });
        } else{
           imageView.setEnabled(false);
        }
    }

    private class MyTag{
        private String noteId;
        public int noteImportant;

        public MyTag(String noteId, int noteImportant){
            this.noteId = noteId;
            this.noteImportant = noteImportant;
        }

        public String getNoteId(){
            return this.noteId;
        }
    }
}
