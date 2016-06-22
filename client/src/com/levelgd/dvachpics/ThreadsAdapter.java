package com.levelgd.dvachpics;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by lvlgd on 17.01.2016.
 */
public class ThreadsAdapter extends ArrayAdapter<String> {
    static class ViewHolder{
        private TextView textViewN;
        private TextView textViewT;
        private TextView textViewP;
    }

    private final Activity context;
    private final String[] numbers;
    private final String[] titles;
    private final String[] posts;

    public ThreadsAdapter(Activity context, String[] numbers, String[] titles, String[] posts) {
        super(context, R.layout.list_boards, numbers);
        this.context = context;
        this.numbers = numbers;
        this.titles = titles;
        this.posts = posts;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        ViewHolder viewHolder = null;

        if(view == null){

            viewHolder = new ViewHolder();

            view = context.getLayoutInflater().inflate(R.layout.list_threads_item, null);

            viewHolder.textViewN = (TextView) view.findViewById(R.id.textViewThreadNumber);
            viewHolder.textViewT = (TextView) view.findViewById(R.id.textViewThreadTitle);
            viewHolder.textViewP = (TextView) view.findViewById(R.id.textViewThreadPost);

            view.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder)view.getTag();
        }

        viewHolder.textViewN.setText(numbers[position]);
        viewHolder.textViewT.setText(titles[position]);
        viewHolder.textViewP.setText(posts[position]);

        return view;
    }
}
