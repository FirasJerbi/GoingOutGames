package com.goinoutgames.www.goinoutgames;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Firas Jerbi on 06/06/2017.
 */

public class MyAdapter extends BaseExpandableListAdapter {
    private Context context;
    private List<String> listDataHeader;
    private HashMap<String, List<String>> listHashMap;
    String[] s;

    public MyAdapter(Context context, List<String> listDataHeader, HashMap<String, List<String>> listHashMap) {
        this.context = context;
        this.listDataHeader = listDataHeader;
        this.listHashMap = listHashMap;
    }

    @Override
    public int getGroupCount() {
        return listDataHeader.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return listHashMap.get(listDataHeader.get(i)).size();
    }

    @Override
    public Object getGroup(int i) {
        return listDataHeader.get(i);
    }

    @Override
    public Object getChild(int i, int i1) {
        return listHashMap.get(listDataHeader.get(i)).get(i1);
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i1) {
        return i1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
        String headerTitle = (String) getGroup(i);
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.single_event, null);
        }
        TextView headingItem = (TextView) view.findViewById(R.id.heading_item);
        headingItem.setTypeface(null, Typeface.BOLD);
        headingItem.setText(headerTitle);

        return view;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View view, ViewGroup viewGroup) {
        String childTitle = (String) getChild(i, i1);
        s = childTitle.split(":", 10);
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.event_details, null);
        }

        TextView ownerName = (TextView) view.findViewById(R.id.ownerName);
        TextView place = (TextView) view.findViewById(R.id.place);
        TextView startDate = (TextView) view.findViewById(R.id.startDate);
        TextView startTime = (TextView) view.findViewById(R.id.startTime);
        TextView endDate = (TextView) view.findViewById(R.id.endDate);
        TextView endTime = (TextView) view.findViewById(R.id.endTime);
        ownerName.setText(s[0]);
        place.setText(s[1]);
        startDate.setText(s[2]);
        startTime.setText(s[3]+":"+s[4]);
        endDate.setText(s[5]);
        endTime.setText(s[6]+":"+s[7]);
        place.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse("http://maps.google.com/maps?daddr="+s[8]+","+s[9]));
                context.startActivity(intent);
            }
        });


        return view;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }
}
