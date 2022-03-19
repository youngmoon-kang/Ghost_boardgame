package com.example.boardgame_ghost;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.boardgame_ghost.common.GameInfo;
import com.example.boardgame_ghost.common.GameTimer;
import com.example.boardgame_ghost.common.Pair;
import com.example.boardgame_ghost.common.SoundManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    TextView myTime;
    TextView oppTime;
    Button startButton;
    Socket socket;
    Button oppositeState;
    Boolean isConnection;
    Boolean start;//시작버튼 누르면 -> True
    Boolean oppExit = false;//상대방 나감 true -> 마지막 한번밖에 안쓰임
    Boolean connect = false;//연결상태 flag
    LinearLayout messageLayout;
    //이동을 위해 선택된 말 위치
    int x_choice;
    int y_choice;

    int my_red_point = 0;
    int my_blue_point =0;
    int opp_red_point = 0;
    int opp_blue_point = 0;

    TextView my_blue_text_view;
    TextView my_red_text_view ;
    TextView opp_blue_text_view;
    TextView opp_red_text_view;

    public ArrayList<ArrayList<Board_info>> BoardInfoArray;//현재 게임판 정보를 저장함
    public ImageView[][] boardImageViewArray;//게임판 그림 이미지뷰
    public int red_cnt = 0;

    boolean my_turn = false;//자신의 턴이면 true -> 보드 클릭 가능
    boolean time_over; // 시간초과 유무 따짐 - my turn일때만 상대는 상대가 알아서 보냄

    SoundPool soundPool;
    SoundManager soundManager;

    GameTimer_Inner myTimer;
    GameTimer_Inner oppTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageLayout = findViewById(R.id.messageLayout);

        start = false;//시작버튼 누르면 -> True

        //연결시작
        isConnection = false;
        EntranceThread entranceThread = new EntranceThread();
        entranceThread.start();

        try {
            entranceThread.join();

            if(!connect){
                messageLayout.setVisibility(View.VISIBLE);
                TextView messageText = findViewById(R.id.messageTextView);
                messageText.setText("서버와의 연결 오류");

                //----------------json 생성----------------------------------
                JSONObject obj = new JSONObject();
                try {
                    obj.put("func", "exit_room");
                    obj.put("data", " ");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                ErrorExitSocketThread exitThread = new ErrorExitSocketThread(obj);
                exitThread.start();

                Button exitButton = findViewById(R.id.confirmButton);
                exitButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        finish();
                    }
                });
            }

            else
                isConnection = true;
        }
        catch (Exception e)
        {
            Toast.makeText(MainActivity.this, "error: " + e.toString(), Toast.LENGTH_LONG).show();
        }

        oppositeState = findViewById(R.id.oppositeStateButton);
        oppositeState.setVisibility(View.INVISIBLE);

        if(isConnection) {
            ListenSocket listenSocket = new ListenSocket();
            listenSocket.start();
        }

        startButton = findViewById(R.id.startButton);
        startButton.setText("남은 빨간 말: 04");
        startButton.setBackgroundColor(Color.RED);

        // ================이미지뷰를 배열로 저장===============================
        boardImageViewArray = new ImageView[6][6];
        for(int i = 0; i < 6; i++)
        {
            for(int j = 0; j < 6; j++)
            {
                int iResId = getResources().getIdentifier(String.format("board_%d_%d", i, j), "id", this.getPackageName() );
                ImageView imageView = findViewById( iResId );
                boardImageViewArray[i][j] = imageView;
            }
        }
        board_click_false();
        // ================이미지뷰를 배열로 저장 끝===============================

        setup_board(); // 보드 정보 초기화
        draw_board(); //보드 그리기

        final FrameLayout oppositFrame = findViewById(R.id.opposite_layout);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View oppView = inflater.inflate(R.layout.player, oppositFrame, true);

        FrameLayout myFrame = findViewById(R.id.my_layout);
        View myView = inflater.inflate(R.layout.player, myFrame, true);

        oppositFrame.setBackgroundColor(Color.parseColor("#FFB8B8"));
        myFrame.setBackgroundColor(Color.parseColor("#CBD8EF"));

        oppTime = oppView.findViewById(R.id.leftTimeText);
        oppTime.setText("남은시간: 00");

        myTime = myView.findViewById(R.id.leftTimeText);
        myTime.setText("남은시간: 00");

        myTimer = new GameTimer_Inner(10000, 1000, myTime);
        oppTimer = new GameTimer_Inner(10000, 1000, oppTime);

        my_blue_text_view = myView.findViewById(R.id.bluePointText);
        my_red_text_view = myView.findViewById(R.id.redPointText);
        opp_blue_text_view = oppView.findViewById(R.id.bluePointText);
        opp_red_text_view = oppView.findViewById(R.id.redPointText);

        //point text view에 각 점수 바인딩
        set_point_text_view();

        //소리 셋팅
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder().build();
        }
        soundManager = new SoundManager(this, soundPool);
        soundManager.addSound(0,R.raw.move_sound);
        soundManager.addSound(1, R.raw.click_sound);


        //시작버튼(상대에게 ready 보내기)
        startButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                soundManager.playSound(1);

                if(red_cnt == 4){//빨간 말 4개를 다 골랐을 경우
                    //start = true;
                    String red_point ="";// x,y/x,y/x,y/x,y/

                    //상대방에게 보낼 정보 문자열로 생성
                    for(int i = 4 ; i < 6; i++)
                    {
                        for(int j = 1; j < 6; j++)
                        {
                            if(BoardInfoArray.get(i).get(j).getState().equals("red")){
                                red_point += Integer.toString(Math.abs(i - 5));
                                red_point += ",";
                                red_point += Integer.toString(Math.abs(j - 5));
                                red_point += "/";
                            }
                        }
                    }
                    //----------------json 생성----------------------------------
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("state", "ready");
                        obj.put("red_point", red_point);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    //----------------json 생성 끝----------------------------------

                    //상대방에게 ready알림
                    SendThread readyThread = new SendThread(obj);
                    readyThread.start();
                    try {
                        readyThread.join();
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                    }

                    //상대방이 이미 ready인 경우 -> 버튼 다 안보이기
                    my_turn = true;
                    if(oppositeState.getText().equals("준비완료"))
                    {
                        oppositeState.setVisibility(View.INVISIBLE);
                        startButton.setVisibility(View.INVISIBLE);

                        my_turn = false;//상대가 먼저 ready하면 상대부터 시작
                        game_start();
                    }
                    else
                    {
                        startButton.setVisibility(View.INVISIBLE);
                    }
                    // 상대방에게 말 받아서 시작
                }
            }
        });
    }

    //게임 재식작
    public void restart()
    {
        start = false;
        time_over = true;

        my_red_point = 0;
        my_blue_point =0;
        opp_red_point = 0;
        opp_blue_point = 0;

        my_red_text_view.setTextColor(Color.BLACK);
        my_blue_text_view.setTextColor(Color.BLACK);
        opp_red_text_view.setTextColor(Color.BLACK);
        opp_blue_text_view.setTextColor(Color.BLACK);

        myTime.setTextColor(Color.BLACK);
        oppTime.setTextColor(Color.BLACK);

        red_cnt = 0;
        my_turn = false;//자신의 턴이면 true -> 보드 클릭 가능

        startButton.setVisibility(View.VISIBLE);
        startButton.setText("남은 빨간 말: 04");
        startButton.setBackgroundColor(Color.RED);

        messageLayout.setVisibility(View.INVISIBLE);

        board_click_false();

        setup_board(); // 보드 정보 초기화
        draw_board(); //보드 그리기

        oppTime.setText("남은시간: 00");
        myTime.setText("남은시간: 00");

        //point text view에 각 점수 바인딩
        set_point_text_view();
    }

    //게임 시작 시 초기 셋팅
    public void game_start()
    {
        start = true;
        time_over = true;

        if(my_turn)
        {
            myTime.setTextColor(Color.RED);
            oppTime.setTextColor(Color.BLACK);
            myTimer.start();
        }
        else
        {
            myTime.setTextColor(Color.BLACK);
            oppTime.setTextColor(Color.RED);
            oppTimer.start();
        }

        my_red_point = 4;
        my_blue_point = 4;
        opp_red_point = 4;
        opp_blue_point = 4;
        set_point_text_view();
    }

    //point text view에 각 점수 바인딩 + 빨간색 4개 먹었을 경우 끝남
    public void set_point_text_view()
    {
        my_blue_text_view.setText(Integer.toString(my_blue_point));
        my_red_text_view.setText(Integer.toString((my_red_point)));
        opp_blue_text_view.setText(Integer.toString(opp_blue_point));
        opp_red_text_view.setText(Integer.toString((opp_red_point)));

        //상대방이 내 빨간 말 다 먹음
        if(my_red_point == 0 && start){
            messageLayout.setVisibility(View.VISIBLE);
            TextView messageText = findViewById(R.id.messageTextView);
            messageText.setText("YOU WIN!");
            board_click_false();
        }

        //내가 상대방 빨간 말 다 먹음
        else if(opp_red_point == 0 && start){
            messageLayout.setVisibility(View.VISIBLE);
            TextView messageText = findViewById(R.id.messageTextView);
            messageText.setText("YOU LOSE!");
            board_click_false();
        }

        //상대방이 내 파란 말 다 먹음
        else if(my_blue_point == 0 && start){
            messageLayout.setVisibility(View.VISIBLE);
            TextView messageText = findViewById(R.id.messageTextView);
            messageText.setText("YOU LOSE!");
            board_click_false();
        }

        //내가 상대방 빨간 말 다 먹음
        else if(opp_blue_point == 0 && start){
            messageLayout.setVisibility(View.VISIBLE);
            TextView messageText = findViewById(R.id.messageTextView);
            messageText.setText("YOU WIN!");
            board_click_false();
        }

        //----------------json 생성----------------------------------
        final JSONObject obj = new JSONObject();
        try {
            obj.put("state", "end");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(isConnection) {
            Button confirmButton = findViewById(R.id.confirmButton);
            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    restart();
                    SendThread thread = new SendThread(obj);
                    thread.start();
                }
            });
        }
    }

    // 시작 시 초기 보드 정보 설정
    public void setup_board()
    {
        BoardInfoArray = new ArrayList<>();
        for(int i = 0; i < 6; i++)
        {
            ArrayList<Board_info> BoardRowArray = new ArrayList<>();

            for(int j = 0; j < 6; j++)
            {
                if((i == 0 || i == 5) && (j == 0 || j == 5))//보드 모서리
                {
                    Board_info temp = new Board_info(0, "goal");
                    BoardRowArray.add(temp);
                }

                else if((i == 4 || i == 5) && !(j == 0 || j == 5))
                {
                    Board_info temp = new Board_info(1, "blue");
                    BoardRowArray.add(temp);
                }

                else
                {
                    Board_info temp = new Board_info(0, "none");
                    BoardRowArray.add(temp);
                }
            }

            BoardInfoArray.add(BoardRowArray);
        }
    }

    //이미지뷰에 그림 설정
    public void draw_board()
    {
        for(int i = 0; i < 6; i++)
        {
            for(int j = 0; j < 6; j++)
            {
                switch(BoardInfoArray.get(i).get(j).getImage())
                {
                    case "board_blue":
                        boardImageViewArray[i][j].setImageResource(R.drawable.board_blue);
                        break;
                    case "board_red":
                        boardImageViewArray[i][j].setImageResource(R.drawable.board_red);
                        break;
                    case "board_goal":
                        boardImageViewArray[i][j].setImageResource(R.drawable.board_goal);
                        break;
                    case "board":
                        boardImageViewArray[i][j].setImageResource(R.drawable.board);
                        break;
                    case "board_white":
                        boardImageViewArray[i][j].setImageResource(R.drawable.board_white);
                        break;

                    case "board_blue_yellow":
                        boardImageViewArray[i][j].setImageResource(R.drawable.board_blue_yellow);
                        break;
                    case "board_red_yellow":
                        boardImageViewArray[i][j].setImageResource(R.drawable.board_red_yellow);
                        break;
                    case "board_goal_point":
                        boardImageViewArray[i][j].setImageResource(R.drawable.board_goal_point);
                        break;
                    case "board_yellow":
                        boardImageViewArray[i][j].setImageResource(R.drawable.board_yellow);
                        break;
                    case "board_white_yellow":
                        boardImageViewArray[i][j].setImageResource(R.drawable.board_white_yellow);
                        break;

                    default:
                        boardImageViewArray[i][j].setImageResource(R.drawable.board);
                        break;
                }
            }
        }
    }

    // ImageView이름을 위치로
    public Pair getPos(String name)
    {
        Pair ret = new Pair();

        char c_x = name.charAt(6);
        char c_y = name.charAt(8);

        ret.x = c_x - '0';
        ret.y = c_y - '0';

        return ret;
    }

    //상대방 말 정보를 받아 자신 보드에 그리기
    public void set_opposite_board(String str)
    {
        //위 2줄은 상대영역
        //일단 파란색으로 설정
        for(int i = 0; i  < 2; i++)
        {
            for(int j = 1; j < 5; j++)
            {
                BoardInfoArray.get(i).get(j).setTeam(2);
                BoardInfoArray.get(i).get(j).setState("blue");
            }
        }

        //서버에서 받은 정보 = red말 위치
        //빨간색으로 설정
        int[][] pos = {{6,8}, {10, 12}, {14, 16}, {18,20}};

        for(int i = 0; i < 4; i++)
        {
            char c_x = str.charAt(pos[i][0]);
            char c_y = str.charAt(pos[i][1]);
            int x = c_x - '0';
            int y = c_y - '0';

            BoardInfoArray.get(x).get(y).setState("red");
        }

        draw_board();
    }

    //상대방에게 움직임 정보가 오면
    public void move_opposite_board(String str) //'move 2,3/2,4\n'
    {
        oppTimer.cancel();
        oppTimer.onFinish();
        boolean goal_in = false;

        int from_x = str.charAt(5) - '0';
        int from_y = str.charAt(7) - '0';
        int to_x = str.charAt(9) - '0';
        int to_y = str.charAt(11) - '0';

        if(from_x == to_x && from_y == to_y) {

        }

        else{
            Board_info fromBoard = BoardInfoArray.get(from_x).get(from_y);
            Board_info toBoard = BoardInfoArray.get(to_x).get(to_y);

            if(toBoard.getState().equals("red"))
            {
                //opp_red_point += 1;
                my_red_point -= 1;
                Red_to_Black red_to_black = new Red_to_Black(my_red_text_view);
                red_to_black.start();
            }
            else if(toBoard.getState().equals("blue"))
            {
                my_blue_point -= 1;
                Red_to_Black red_to_black = new Red_to_Black(my_blue_text_view);
                red_to_black.start();
            }

            toBoard.copy_board(fromBoard);
            fromBoard.reset_board();

            if(to_x == 5 && (to_y == 0 || to_y == 5)) goal_in = true;

            draw_board();

            if(goal_in) {
                //상대방 blue point 1 상승
                //opp_blue_point += 1;

                Goal_in(to_x, to_y, 2);
            }//상대방 골인

            set_point_text_view();
            //소리재생
            soundManager.playSound(0);
        }
        if(!goal_in) {
            my_turn = true;//자기차례
            myTimer.start();
            myTime.setTextColor(Color.RED);
            oppTime.setTextColor(Color.BLACK);
        }
    }

    //말 클릭 시 이벤트 - xml에 선언
    public void click_board(View view)
    {
        boolean goal_in = false; //자신의 말이 골 안으로 들어감
        //TODO: 사용자가 말을 어디에 놓을 지 선택
        ((ImageView) view).setImageResource(R.drawable.board_blue);
        String name = view.getResources().getResourceEntryName(view.getId());

        //ex) name: board_4_0
        Pair pos = getPos(name);
        int x = pos.x;
        int y = pos.y;

        String current_state = BoardInfoArray.get(x).get(y).getState();
        int current_team = BoardInfoArray.get(x).get(y).getTeam();

        // 시작전까지 클릭 -> 처음 준비시간동안 사용자가 빨간색을 선택하는 부분
        if(startButton.getVisibility() == View.VISIBLE) init_red_point(current_state, current_team, x, y);

        //이동 가능한 구역을 선택 - 말 이동 => 상대에게 전송
        else if(BoardInfoArray.get(x).get(y).isMoveable())
        {
            // 제한시간 전에 누름
            time_over = false;
            myTimer.cancel();
            myTimer.onFinish();

            //소리재생
            soundManager.playSound(0);

            if(current_state.equals("red"))
            {
                //my_red_point += 1;
                opp_red_point -= 1;
                Red_to_Black red_to_black = new Red_to_Black(opp_red_text_view);
                red_to_black.start();
            }
            else if(current_state.equals("blue"))
            {
                opp_blue_point -= 1;
                Red_to_Black red_to_black = new Red_to_Black(opp_blue_text_view);
                red_to_black.start();
            }

            //선택한 영역에 기존 말 복사
            BoardInfoArray.get(x).get(y).copy_board(BoardInfoArray.get(x_choice).get(y_choice));

            //기존 말 초기화
            BoardInfoArray.get(x_choice).get(y_choice).reset_board();

            //1. moveable초기화
            for(int i = 0; i < 6; i++)
            {
                for(int j = 0; j < 6; j++)
                {
                    BoardInfoArray.get(i).get(j).setMoveable(false);
                }
            }

            if(current_state.equals("goal"))
            {
                goal_in = true;
            }

            // 상대방에게 보낼 json정보 생성
            int rev_x_choice = Math.abs(x_choice - 5);
            int rev_y_choice = Math.abs(y_choice - 5);
            int rev_x = Math.abs(x - 5);
            int rev_y = Math.abs(y - 5);

            String from_string = "";
            from_string += Integer.toString(rev_x_choice);
            from_string += ",";
            from_string += Integer.toString(rev_y_choice);

            String to_string = "";
            to_string += Integer.toString(rev_x);
            to_string += ",";
            to_string += Integer.toString(rev_y);

            JSONObject obj = new JSONObject();
            try {
                obj.put("state", "move");
                obj.put("from", from_string);
                obj.put("to", to_string);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //상대방에게 data전송
            SendThread moveThread = new SendThread(obj);
            moveThread.start();
            try {
                moveThread.join();
            }catch(Exception e)
            {
                e.printStackTrace();
            }

            if(!goal_in) {
                my_turn = false;//상대방 차례
                oppTimer.start();
                myTime.setTextColor(Color.BLACK);
                oppTime.setTextColor(Color.RED);
            }
        }

        //이동할 말 선택
        else if(start && current_team == 1 && my_turn == true)
        {
            //선택 말 저장
            x_choice = x;
            y_choice = y;

            //1. moveable초기화
            for(int i = 0; i < 6; i++)
            {
                for(int j = 0; j < 6; j++)
                {
                    BoardInfoArray.get(i).get(j).setMoveable(false);
                }
            }

            //2. 전체탐색으로 갈수있는곳 결정
            for(int i = 0; i < 6; i++)
            {
                for(int j = 0; j < 6; j++)
                {
                    if( !((i == x && j == y - 1) || (i == x && j == y + 1) ||
                            (i == x + 1 && j == y) || (i == x - 1 && j == y)))
                        continue;

                    if( (i == 5 && j == 0) || (i == 5 && j == 5))//자신의 goal에 못들어감
                        continue;

                    if(BoardInfoArray.get(i).get(j).getTeam() != 1)//이동할 곳이 자신의 말인경우 이동 못함
                    {
                        if(current_state == "red" && BoardInfoArray.get(i).get(j).getState() == "goal")//선택한 말이 red인 경우 goal못들어감
                            continue;
                        BoardInfoArray.get(i).get(j).setMoveable(true);
                    }
                }
            }
        }

        draw_board();

        if(goal_in)//골로 들어갔을 경우
        {
            Goal_in(x, y, 1);
            //my_blue_point += 1;
        }
        set_point_text_view();
    }

    // 골인일 경우
    public void Goal_in(int x, int y, int team)
    {
        start = false;
        final int f_x = x;
        final int f_y = y;
        if(team == 1)
            boardImageViewArray[f_x][f_y].setImageResource(R.drawable.board_blue_black);
        else if(team == 2)
            boardImageViewArray[f_x][f_y].setImageResource(R.drawable.board_white_black);

        //1초 뒤 말 모양 바뀌기 - 사용안함 (2개가 목표일 경우 사용)
        /*
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                BoardInfoArray.get(f_x).get(f_y).setTeam(0);
                BoardInfoArray.get(f_x).get(f_y).setState("goal");
                BoardInfoArray.get(f_x).get(f_y).setMoveable(false);

                draw_board();
            }
        }, 1000);
        */

        //본인 승
        if(x == 0){
            messageLayout.setVisibility(View.VISIBLE);
            TextView messageText = findViewById(R.id.messageTextView);
            messageText.setText("YOU WIN!");
            board_click_false();
        }
        //상대방 승
        else{
            messageLayout.setVisibility(View.VISIBLE);
            TextView messageText = findViewById(R.id.messageTextView);
            messageText.setText("YOU LOSE~");
            board_click_false();
        }

        //----------------json 생성----------------------------------
        final JSONObject obj = new JSONObject();
        try {
            obj.put("state", "end");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(isConnection) {
            Button confirmButton = findViewById(R.id.confirmButton);
            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    restart();
                    SendThread thread = new SendThread(obj);
                    thread.start();
                }
            });
        }
    }

    //빨간색 선택하는 함수
    public void init_red_point(String current_state, int current_team, int x, int y)
    {
        if(current_state.equals("blue") && current_team == 1 && red_cnt < 4)
        {
            BoardInfoArray.get(x).get(y).setState("red");
            red_cnt++;
            startButton.setText("남은 빨간 말: 0" + Integer.toString(4 - red_cnt));
            if(red_cnt == 4)
            {
                startButton.setBackgroundColor(Color.BLUE);
                startButton.setText("시  작");
            }
            else
                startButton.setBackgroundColor(Color.RED);
        }
        else if(current_state.equals("red") && current_team == 1)
        {
            BoardInfoArray.get(x).get(y).setState("blue");
            red_cnt--;
            startButton.setText("남은 빨간 말: 0" + Integer.toString(4 - red_cnt));
            startButton.setBackgroundColor(Color.RED);
        }
    }

    //보드 클릭 가능하게
    public void board_click_true()
    {
        for(int i = 0; i < 6; i++){
            for(int j = 0; j < 6; j++){
                boardImageViewArray[i][j].setClickable(true);
            }
        }
        Log.i("board_click", "true");
    }

    //보드 클릭 잠그기
    public void board_click_false()
    {
        for(int i = 0; i < 6; i++){
            for(int j = 0; j < 6; j++){
                boardImageViewArray[i][j].setClickable(false);
            }
        }
        Log.i("board_click", "false");
    }

    //red색 textView를 black으로 서서히 바꾸게 함
    public class Red_to_Black extends Thread{
        TextView textView;

        public Red_to_Black(TextView view)
        {
            this.textView = view;
        }

        @Override
        public void run()
        {
            //textView.setTextColor(Color.RED);
            int red = 255;

            while(red > 0){
                red -= 1;
                final int final_red = red;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setTextColor(Color.rgb(final_red, 0, 0));
                    }
                });
                try {
                    sleep(8);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            textView.setTextColor(Color.BLACK);

            return;
        }
    }

    //게임 방 진입 서버에 알림 - 소켓연결
    public class EntranceThread extends Thread{
        String host = GameInfo.ip;
        @Override
        public void run()
        {
            try{
                int port = GameInfo.game_port;
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 3000);
                connect = true;
            }
            catch(Exception e)
            {
                connect = false;
                e.printStackTrace();
            }
        }
    }

    //게임서버와 연결이 되지 않을때 방 나가기
    public class ErrorExitSocketThread extends Thread{
        String host = GameInfo.ip;
        JSONObject data;

        public ErrorExitSocketThread(JSONObject data)
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
                String response = instream.readLine();
                socket.close();
                instream.close();
            }
            catch(Exception e)
            {
                String response = "error";
                e.printStackTrace();
            }
        }
    }

    //ready정보 상대에게 알림
    public class SendThread extends Thread{
        JSONObject data;

        public SendThread(JSONObject data)
        {
            this.data = data;
        }

        @Override
        public void run()
        {
            try{
                PrintWriter outstream = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8")),true);
                outstream.println(data);
                outstream.flush();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public class ListenSocket extends Thread{
        DataInputStream dis;

        public ListenSocket(){
            try {
                InputStream is = socket.getInputStream();
                dis = new DataInputStream(is);
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void run()
        {
            try{
                while(isConnection) {
                    BufferedReader instream = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                    final String response = instream.readLine();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(response == null)
                            {
                                return;
                            }
                            if (response.equals("come")) {//1: come
                                //상대방 참석
                                oppositeState.setVisibility(View.VISIBLE);
                                oppositeState.setText("상대방 입장");

                                //시작 전 게임서버에서 waiting상태 해제
                                //----------------json 생성----------------------------------
                                JSONObject obj = new JSONObject();
                                try {
                                    obj.put("state", "waiting_end");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                SendThread waiting_end_Thread = new SendThread(obj);
                                waiting_end_Thread.start();

                                board_click_true();
                            } else if (response.startsWith("ready")) {//2: ready
                                //상대방 준비 완료
                                oppositeState.setText("준비완료");

                                //자신이 이미 ready인 경우 -> 버튼 다 안보이기
                                if(startButton.getVisibility() == View.INVISIBLE)
                                {
                                    oppositeState.setVisibility(View.INVISIBLE);

                                    game_start();
                                }
                                set_opposite_board(response);
                            } else if(response.startsWith("move")) {
                                move_opposite_board(response);
                            } else if(response.startsWith("exit")) {
                                //상대방이 나갈경우
                                messageLayout.setVisibility(View.VISIBLE);
                                TextView messageText = findViewById(R.id.messageTextView);
                                messageText.setText("상대방 나감");
                                board_click_false();
                                oppExit = true;

                                Button confirmButton = findViewById(R.id.confirmButton);
                                confirmButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        onBackPressed();
                                    }
                                });
                            }
                        }
                    });
                }
            }
            catch(NetworkOnMainThreadException e)
            {
                e.printStackTrace();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed(){
        soundManager.playSound(1);

        final boolean[] exit = {false};
        
        //상대방이 아직 나가지 않았을 때
        if(!oppExit) {
            //다이얼로그 생성: 게임을 나가겠습니까?
            final LinearLayout exitLayout = findViewById(R.id.exitLayout);
            exitLayout.setVisibility(View.VISIBLE);
            Button cancelButton = findViewById(R.id.exitCancelButton);
            Button exitButton = findViewById(R.id.exitConfirmButton);

            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    soundManager.playSound(1);
                    exitLayout.setVisibility(View.INVISIBLE);
                    exit[0] = false;
                }
            });

            exitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    soundManager.playSound(1);
                    exit[0] = true;

                    try {
                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("state", "exit");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        SendThread thread = new SendThread(obj);
                        thread.start();
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    finish();
                    overridePendingTransition(0, 0);
                }
            });
        }

        //상대방이 나갔을 때
        else {
            try {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("state", "exit");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                SendThread thread = new SendThread(obj);
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            finish();
            overridePendingTransition(0, 0);
        }
    }

    // 타이머 구현
    public class GameTimer_Inner extends CountDownTimer {
        TextView textView;

        public GameTimer_Inner(long millisInFuture, long countDownInterval, TextView textView){
            super(millisInFuture, countDownInterval);
            this.textView = textView;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            textView.setText("남은시간: " + Long.toString(millisUntilFinished/1000));
        }

        @Override
        public void onFinish() {
            textView.setText("남은시간: 00");
            // 타이머가 다 끝나서 바뀐것과 말을 이동해서 바뀐것 구분
            if(time_over && my_turn){

                //yellow없애기
                //1. moveable초기화
                for(int i = 0; i < 6; i++)
                {
                    for(int j = 0; j < 6; j++)
                    {
                        BoardInfoArray.get(i).get(j).setMoveable(false);
                    }
                }
                draw_board();

                //Move: none(from 0,0 to 0,0)보내기
                JSONObject obj = new JSONObject();
                try {
                    obj.put("state", "move");
                    obj.put("from", "0,0");
                    obj.put("to", "0,0");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //상대방에게 data전송
                SendThread moveThread = new SendThread(obj);
                moveThread.start();
                try {
                    moveThread.join();
                }catch(Exception e)
                {
                    e.printStackTrace();
                }

                my_turn = false;//상대방 차례

                oppTimer.start();
                myTime.setTextColor(Color.BLACK);
                oppTime.setTextColor(Color.RED);
            }

            time_over = true;
        }
    }

}
