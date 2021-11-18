package game;

import engine.apis.openGL.*;
import engine.core.*;

import java.io.*;

public class NightmareMaze extends Game
{
    public static final String FILE_PATH     = System.getProperty("user.home") + File.separator+ "Game.NightmareMaze" + File.separator;

    public static final String FONT_ATLAS = "arialClear";

    public NightmareMaze() { super(new OpenGLDisplay(1024, 768, 32, false, "Nightmare Maze!"), 60); }

    @Override
    protected void OnGameStart()
    {


        ChangeScene(MenuScene.class);
    }

    @Override
    protected void OnGameUpdate()
    {

    }

    @Override
    protected void OnGameExit()
    {

    }
}
