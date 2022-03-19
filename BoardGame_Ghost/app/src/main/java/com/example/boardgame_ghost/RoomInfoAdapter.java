package com.example.boardgame_ghost;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

public class RoomInfoAdapter extends BaseAdapter {
    Context context;
    ArrayList<RoomInfo> roomInfoArray = new ArrayList<>();

    public RoomInfoAdapter(Context context, ArrayList<RoomInfo> data){
        this.context = context;
        this.roomInfoArray = data;
    }

    @Override
    public int getCount() {
        return roomInfoArray.size();
    }

    @Override
    public Object getItem(int i) {
        return roomInfoArray.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint("ResourceType")
    @Override
    public View getView(int i, View view, ViewGroup parent) {
        view = LayoutInflater.from(context).inflate(R.layout.adapter_room_info, null);

        TextView roomNameText = view.findViewById(R.id.roomNameText);
        ImageView lockImage = view.findViewById(R.id.lockImageView);
        TextView playerCntText = view.findViewById(R.id.playerCntText);

        RoomInfo currentRoom = roomInfoArray.get(i);
        roomNameText.setText(currentRoom.getName());
        if(currentRoom.getUse_password().equals("N")){
            lockImage.setVisibility(View.INVISIBLE);
        }
        String playerCnt = currentRoom.getPlayerCnt() + " / 2";
        playerCntText.setText(playerCnt);

        if(currentRoom.getPlaying().equals("Y")){
            ConstraintLayout layout = view.findViewById(R.id.backgroundLayout);
            layout.setBackgroundColor(Color.rgb(218,227,243));
        }

        return view;
    }
}
