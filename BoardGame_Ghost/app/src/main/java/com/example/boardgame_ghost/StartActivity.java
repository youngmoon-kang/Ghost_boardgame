package com.example.boardgame_ghost;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.boardgame_ghost.common.GameInfo;
import com.example.boardgame_ghost.common.SocketThread;
import com.example.boardgame_ghost.common.SoundManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;

public class StartActivity extends AppCompatActivity {
    String response = "";
    
    SoundPool soundPool;
    SoundManager soundManager;
    
    //Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        //소리설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder().build();
        }
        soundManager = new SoundManager(this, soundPool);
        soundManager.addSound(0,R.raw.move_sound);
        soundManager.addSound(1, R.raw.click_sound);

        ImageButton startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                soundManager.playSound(1);
                
                response = "";
                String temp_id = getRandomId();
                if(temp_id.equals("error"))
                {
                    Toast.makeText(StartActivity.this, "연결 오류", Toast.LENGTH_LONG).show();
                }
                else
                {
                    GameInfo.id = temp_id;
                    Intent intent = new Intent(getApplicationContext(), RoomSearchActivity.class);
                    startActivity(intent);

                    overridePendingTransition(0, 0);
                }
            }
        });
    }

    String getRandomId()
    {
        String ret = "";

        while(!response.equals("true")) {
            ret = "";

            // id 랜덤 생성
            for (int i = 0; i < 5; i++) {
                char t = (char) ((Math.random() * 100) % 26 + 65);
                ret += t;
            }

            //----------------json 생성----------------------------------
            JSONObject obj = new JSONObject();
            try {
                obj.put("func", "select_user");

                JSONObject dataObj = new JSONObject();
                dataObj.put("id", ret);
                obj.put("data", dataObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //----------------json 생성 끝----------------------------------

            //중복 id확인
            SocketThread thread = new SocketThread(obj);
            thread.start();

            try {
                thread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (response.equals("error"))
            {
                return "error";
            }
        }

        return ret;
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
            }
            catch(Exception e)
            {
                response = "error";
                e.printStackTrace();
            }
        }
    }
}
