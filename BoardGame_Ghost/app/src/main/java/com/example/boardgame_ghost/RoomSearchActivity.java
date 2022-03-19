package com.example.boardgame_ghost;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.example.boardgame_ghost.common.GameInfo;
import com.example.boardgame_ghost.common.SoundManager;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

public class RoomSearchActivity extends AppCompatActivity {
    String response;
    ArrayList<RoomInfo> roomInfoList;
    RoomInfoAdapter adapter;

    SoundPool soundPool;
    SoundManager soundManager;

    @Override
    protected void onRestart() {
        super.onRestart();
        try {
            searchRoom();
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(RoomSearchActivity.this, "정보 조회 오류", Toast.LENGTH_LONG);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_search);

        //소리설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder().build();
        }
        soundManager = new SoundManager(this, soundPool);
        soundManager.addSound(0,R.raw.move_sound);
        soundManager.addSound(1, R.raw.click_sound);

        ImageButton createDialogButton = findViewById(R.id.createRoomButton);
        ImageButton refreshButton = findViewById(R.id.refreshButton);
        ListView roomListView = findViewById(R.id.roomListView);

        roomInfoList = new ArrayList<>();

        //액티비티 실행 시 제일 먼저 방 정보 조회
        try {
            searchRoom();
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(RoomSearchActivity.this, "정보 조회 오류", Toast.LENGTH_LONG);
        }

        adapter = new RoomInfoAdapter(this, roomInfoList);
        roomListView.setAdapter(adapter);

        final Dialog createRoomDialog = new Dialog(RoomSearchActivity.this);
        createRoomDialog.setContentView(R.layout.dialog_create_room);

        //DB에 id삽입
        {
            //----------------json 생성----------------------------------
            JSONObject obj = makeJason("insert_user", GameInfo.id);

            //------------------DB에 id 입력--------------------------
            SocketThread thread = new SocketThread(obj);
            thread.start();
            try {
                thread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!response.equals("true")) {
                Toast.makeText(RoomSearchActivity.this, "서버  통신 오류", Toast.LENGTH_LONG);
            }
        }

        createDialogButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                soundManager.playSound(1);
                //dialog생성 및 방 생성
                showDialog(createRoomDialog);
            }
        });

        //방 목록 새로고침
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                soundManager.playSound(1);

                try {
                    searchRoom();
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(RoomSearchActivity.this, "정보 조회 오류", Toast.LENGTH_LONG);
                }
                adapter.notifyDataSetChanged();
            }
        });

        //방 클릭 이벤트
        roomListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                soundManager.playSound(1);

                final RoomInfo currentRoomInfo = roomInfoList.get(i);
                boolean room_ok = true; //참여 가능하면 true

                if(currentRoomInfo.getPlayerCnt() == 2){
                    AlertDialog.Builder builder = new AlertDialog.Builder(RoomSearchActivity.this);
                    builder.setTitle("참여 불가").setMessage("인원이 가득 찼습니다.");
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                    room_ok = false;
                }
                else if(currentRoomInfo.getPlaying().equals("Y")){
                    AlertDialog.Builder builder = new AlertDialog.Builder(RoomSearchActivity.this);
                    builder.setTitle("참여 불가").setMessage("이미 게임중인 방입니다.");
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                    room_ok = false;
                }
                else if(currentRoomInfo.getUse_password().equals("Y")){
                    final Dialog insertPasswordDialog = new Dialog(RoomSearchActivity.this);
                    insertPasswordDialog.setContentView(R.layout.dialog_insert_password);
                    room_ok = showInsertDialog(insertPasswordDialog, currentRoomInfo); //비밀번호 확인
                }
                //진입 불가한 방이면
                if(!room_ok){
                    try {
                        searchRoom();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    adapter.notifyDataSetChanged();
                    return;
                }

                enterRoom(currentRoomInfo);
            }
        });
    }

    //들어가고싶은 room정보 조회 및 입장
    public void enterRoom(RoomInfo currentRoomInfo){
        boolean room_ok = true;

        //room 정보 조회
        JSONObject obj = makeJason("get_room_info", currentRoomInfo.getId());
        SocketThread thread = new SocketThread(obj);
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //방 정보를 찾지 못할 때 => 방 찾을 수 없음 알림 -> 새로고침
        if(response.equals("false")){
            AlertDialog.Builder builder = new AlertDialog.Builder(RoomSearchActivity.this);
            builder.setTitle("참여 불가").setMessage("방 정보를 찾을 수 없습니다.");
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            room_ok = false;
        }

        else {
            Gson gson = new Gson();
            RoomInfo roomInfo = gson.fromJson(response, RoomInfo.class);

            //게임 시작중인 경우
            if (roomInfo.getPlaying().equals("Y")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(RoomSearchActivity.this);
                builder.setTitle("참여 불가").setMessage("이미 게임중인 방입니다.");
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                room_ok = false;
            }

            //방 인원이 꽉 찬 경우
            else if (roomInfo.getPlayerCnt() == 2) {
                AlertDialog.Builder builder = new AlertDialog.Builder(RoomSearchActivity.this);
                builder.setTitle("참여 불가").setMessage("인원이 가득 찼습니다.");
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                room_ok = false;
            }
        }

        //진입 불가한 방이면
        if(!room_ok){
            try {
                searchRoom();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            adapter.notifyDataSetChanged();
            return;
        }

        //입장 가능한 방
        obj = new JSONObject();
        try {
            obj.put("func", "enter_room");

            JSONObject dataObj = new JSONObject();
            dataObj.put("room_id", currentRoomInfo.getId());
            dataObj.put("user_id", GameInfo.id);
            obj.put("data", dataObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        thread = new SocketThread(obj);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(response.equals("true")){
            Intent intent= new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
        else{
            Toast.makeText(RoomSearchActivity.this, "방에 참여할 수 없습니다", Toast.LENGTH_LONG);
        }
    }

    //room정보 조회
    public void searchRoom() throws JSONException {
        roomInfoList.clear();
        JSONObject obj = makeJason("search_room", "");
        SocketThread thread = new SocketThread(obj);
        thread.start();
        try{
            thread.join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = new JSONObject(response);
        JSONArray jsonArray = jsonObject.getJSONArray("room_list");
        Gson gson = new Gson();
        for(int i = 0; i < jsonArray.length(); i++){
            RoomInfo roomInfo_temp = gson.fromJson(jsonArray.get(i).toString(), RoomInfo.class);
            roomInfoList.add(roomInfo_temp);
        }
    }

    //dialog생성 및 방 생성
    public void showDialog(final Dialog dialog){
        dialog.show();

        response = "";
        final EditText passwordEdit = dialog.findViewById(R.id.passwordEdit);
        final EditText roomNameEdit = dialog.findViewById(R.id.roomNameEdit);
        final CheckBox checkBox = dialog.findViewById(R.id.checkBox);
        Button createButton = dialog.findViewById(R.id.createRoomButton);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(compoundButton.getId() == R.id.checkBox){
                    if(isChecked){
                        passwordEdit.setFocusableInTouchMode(true);
                        passwordEdit.setFocusable(true);
                    }
                    else{
                        passwordEdit.setFocusable(false);
                        passwordEdit.setClickable(false);
                    }
                }
            }
        });

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                soundManager.playSound(1);

                String use_password;
                String password = "";
                if(checkBox.isChecked()){
                    use_password = "Y";
                    password = passwordEdit.getText().toString();
                }
                else{
                    use_password = "N";
                }

                JSONObject obj = new JSONObject();
                try {
                    obj.put("func", "make_room");

                    JSONObject dataObj = new JSONObject();
                    dataObj.put("name", roomNameEdit.getText());
                    dataObj.put("use_password", use_password);
                    dataObj.put("password", password);
                    dataObj.put("user", GameInfo.id);
                    obj.put("data", dataObj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                SocketThread thread = new SocketThread(obj);
                thread.start();
                try{
                    thread.join();
                }catch (Exception e) {
                    e.printStackTrace();
                }

                if(!response.equals("false")){
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                }

                dialog.dismiss();
            }
        });
    }

    //비밀번호 입력 다이얼로그 생성 및 비교
    public boolean showInsertDialog(final Dialog dialog, final RoomInfo currentRoomInfo){
        dialog.show();
        final boolean[] ret = new boolean[1];

        final String[] insert_password = {""};
        Button okButton = dialog.findViewById(R.id.passwordOkButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText passwordEditText = dialog.findViewById(R.id.passwordEditText);
                insert_password[0] = passwordEditText.getText().toString();
                dialog.dismiss();

                JSONObject obj = new JSONObject();
                try {
                    obj.put("func", "password_ok");

                    JSONObject dataObj = new JSONObject();
                    dataObj.put("room_id", currentRoomInfo.getId());
                    dataObj.put("password", insert_password[0]);
                    obj.put("data", dataObj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                SocketThread thread = new SocketThread(obj);
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(response.equals("false")){
                    AlertDialog.Builder builder = new AlertDialog.Builder(RoomSearchActivity.this);
                    builder.setTitle("참여 불가").setMessage("비밀번호가 다릅니다.");
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                    ret[0] = false;
                }
                else{
                    enterRoom(currentRoomInfo);
                    ret[0] = true;
                }
            }
        });

        return ret[0];
    }

    // Jason생성
    public JSONObject makeJason(String func_name, String data){
        JSONObject obj = new JSONObject();
        try {
            obj.put("func", func_name);

            JSONObject dataObj = new JSONObject();
            dataObj.put("id", data);
            obj.put("data", dataObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public class SocketThread extends Thread{
        String host = GameInfo.ip;
        JSONObject data;

        public SocketThread(JSONObject data)
        {
            this.data = data;
        }

        @Override
        public void run()
        {
            try{
                int port = GameInfo.port;
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 3000);

                PrintWriter outstream = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8")),true);
                outstream.println(data);
                outstream.flush();

                BufferedReader instream = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                response = instream.readLine();
                socket.close();
                instream.close();
            }
            catch(Exception e)
            {
                response = "error";
                e.printStackTrace();
            }
        }
    }

    //종료시 DB에 user정보 삭제
    @Override
    public void finish(){
        JSONObject obj = makeJason("delete_user", GameInfo.id);

        SocketThread thread = new SocketThread(obj);
        thread.start();

        super.finish();
        overridePendingTransition(0, 0);
    }
}
