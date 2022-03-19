package com.example.boardgame_ghost;

public class Board_info {
    private int team; //어디 팀인지 0: 빈곳 1: 자기 말 2: 상대 말
    private String state; //blue, red, goal, none
    private String image; //현재 위치의 이미지 이름
    private boolean moveable; //이곳으로 이동할 수 있다. -> yellow or dot

    public Board_info(int team, String state)
    {
        this.team = team;
        this.state = state;
        setImage();
        moveable = false;
    }

    public void setMoveable(boolean moveable) {
        this.moveable = moveable;
        setImage();
    }

    public void setTeam(int team)
    {
        this.team = team;
        setImage();
    }

    public void setState(String state)
    {
        this.state = state;
        setImage();
    }

    private void setImage()
    {
        if(this.team == 2 && moveable == false)
        {
            this.image = "board_white";
        }

        else if(this.team == 1 && this.state == "blue" && moveable == false)
        {
            this.image = "board_blue";
        }

        else if(this.team == 1 && this.state == "red" && moveable == false)
        {
            this.image = "board_red";
        }

        else if(this.team == 0 && this.state == "none" && moveable == false)
        {
            this.image = "board";
        }

        else if(this.team == 0 && this.state == "goal" && moveable == false)
        {
            this.image = "board_goal";
        }

        else if(this.team == 2 && moveable == true)
        {
            this.image = "board_white_yellow";
        }

        else if(this.team == 1 && this.state == "blue" && moveable == true)
        {
            this.image = "board_blue_yellow";
        }

        else if(this.team == 1 && this.state == "red" && moveable == true)
        {
            this.image = "board_red_yellow";
        }

        else if(this.team == 0 && this.state == "none" && moveable == true)
        {
            this.image = "board_yellow";
        }

        else if(this.team == 0 && this.state == "goal" && moveable == true)
        {
            this.image = "board_goal_point";
        }
    }

    public int getTeam()
    {
        return this.team;
    }

    public String getState()
    {
        return this.state;
    }

    public String getImage()
    {
        return this.image;
    }

    public boolean isMoveable() {return moveable;}

    public void copy_board(Board_info target)
    {
        this.setMoveable(false);
        this.team = target.getTeam();
        this.state = target.getState();
        setImage();
    }

    public void reset_board()
    {
        this.moveable = false;
        this.team = 0;
        this.state = "none";
        setImage();
    }
}
