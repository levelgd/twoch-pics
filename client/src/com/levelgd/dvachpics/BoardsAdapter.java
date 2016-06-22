package com.levelgd.dvachpics;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by lvlgd on 13.01.2016.
 */
public class BoardsAdapter extends ArrayAdapter<String> {

    static class ViewHolder{
        private TextView textViewB;
        private TextView textViewD;
    }

    private final Activity context;
    private final String[] boards;
    private final String[] descs;

    public BoardsAdapter(Activity context, String[] boards, String[] descs) {
        super(context, R.layout.list_boards, boards);
        this.context = context;
        this.boards = boards;
        this.descs = descs;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        ViewHolder viewHolder = null;

        if(view == null){

            viewHolder = new ViewHolder();

            view = context.getLayoutInflater().inflate(R.layout.list_boards_item, null);

            viewHolder.textViewB = (TextView) view.findViewById(R.id.textViewBoard);
            viewHolder.textViewD = (TextView) view.findViewById(R.id.textViewBoardDesc);

            view.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder)view.getTag();
        }

        viewHolder.textViewB.setText(boards[position]);
        viewHolder.textViewD.setText(descs[position]);

        return view;
    }
}
