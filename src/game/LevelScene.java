package game;

import engine.core.*;
import engine.ecs.*;
import executing.components.*;
import executing.systems.rendererSystems.*;
import executing.systems.updaterSystems.*;
import jamJar.core.*;
import jamJar.core.fileManagement.*;
import jamJar.core.math.*;
import jamJar.core.math.vectors.*;

public class LevelScene extends Scene
{
    private static final double LEVEL_DISPLAY_TIME = 2;

    private LevelSaveData m_levelSaveData;

    KeyboardControl  m_keyboardControl;
    MouseLookControl m_mouseLookControl;

    Level m_level;
    int   m_levelIndex;

    double m_levelDisplayStartTime;
    Entity m_levelDisplayText;

    @Override
    protected void OnSceneStart()
    {
        m_levelIndex = 0;

        UseSystem(MeshRendererSystem.class);
        UseSystem(AudioManagerSystem.class);
        UseSystem(RotaterSystem.class);
        UseSystem(AnimationSystem.class);
        UseSystem(TextRendererSystem.class);
        UseSystem(GUIRendererSystem.class);

        AddToFilePath("nightmareMaze/");

        m_mouseLookControl = new MouseLookControl(InputDevice.Key.ESCAPE, 0.5f);
        m_mouseLookControl.DisablePitchRotation();

        m_keyboardControl = new KeyboardControl();

        GetCamera().AddComponent(m_mouseLookControl);
        GetCamera().AddComponent(m_keyboardControl);

        SetSetting("AmbientLight", new Vector3f());

        m_levelSaveData = new LevelSaveData();
        m_levelIndex = m_levelSaveData.GetIndex();
        StartLevel();
    }

    private void StartLevel()
    {
        ClearScene();

        if(!FileSystem.PathExists("./res/nightmareMaze/textures/Levels/" + m_levelIndex))
        {
            ResetScene(WinScene.class);
            ChangeScene(WinScene.class);

            m_levelIndex = 0;
            m_levelSaveData = new LevelSaveData(m_levelIndex, new Vector3f(1.5f, 0.3f, 1.5f), new Quaternion(), null, null, null, Level.INITIAL_SLUDGE_LEVEL);
            m_levelSaveData.Save();

            return;
        }

        DisableSystem(KeyboardControlSystem.class);
        DisableSystem(MouseLookSystem.class);

        m_levelDisplayStartTime = Clock.GetSeconds();
        m_levelDisplayText      = CreateEntity(new TextComponent("Level " + (m_levelIndex + 1), LoadFont(NightmareMaze.FONT_ATLAS), TextComponent.TextAlignment.MIDDLE_CENTER, new Vector2f(), new Vector2f(2)));
    }

    @Override
    protected void OnSceneUpdate(InputDevice inputDevice, float delta)
    {
        if(m_levelDisplayText != null)
        {
            double currentTime = Clock.GetSeconds();
            if(currentTime - m_levelDisplayStartTime >= LEVEL_DISPLAY_TIME)
            {
                m_levelDisplayText.Free();
                m_levelDisplayText = null;

                UseSystem(MouseLookSystem.class);
                UseSystem(KeyboardControlSystem.class);

                m_level = new Level(this, m_levelIndex++, m_keyboardControl, m_levelSaveData);
            }
            return;
        }

        if(m_level == null)
            return;

        LevelEvent event = m_level.Update(inputDevice, delta);
        switch(event)
        {
            case NEXT_LEVEL:
                inputDevice.SetMouseGrabbed(false);
                m_levelSaveData = new LevelSaveData(m_levelIndex, new Vector3f(1.5f, 0.3f, 1.5f), new Quaternion(), null, null, null, Level.INITIAL_SLUDGE_LEVEL);
                m_levelSaveData.Save();
                StartLevel();
                break;
            case RESTART_LEVEL:
                inputDevice.SetMouseGrabbed(false);
                m_levelIndex--;
                m_levelSaveData = new LevelSaveData(m_levelIndex, new Vector3f(1.5f, 0.3f, 1.5f), new Quaternion(), null, null, null, Level.INITIAL_SLUDGE_LEVEL);
                m_levelSaveData.Save();
                StartLevel();
                break;
            case RESTART_GAME:
                inputDevice.SetMouseGrabbed(false);
                m_levelIndex = 0;
                m_levelSaveData = new LevelSaveData(m_levelIndex, new Vector3f(1.5f, 0.3f, 1.5f), new Quaternion(), null, null, null, Level.INITIAL_SLUDGE_LEVEL);
                m_levelSaveData.Save();
                StartLevel();
                break;
        }
    }

    @Override
    protected void OnSceneExit()
    {
        if(!m_level.IsLost())
            m_level.Save();
    }

    public enum LevelEvent
    {
        NONE,
        RESTART_LEVEL,
        RESTART_GAME,
        NEXT_LEVEL,
    }
}
