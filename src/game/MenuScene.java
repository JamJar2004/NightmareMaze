package game;

import engine.core.*;
import engine.ecs.*;
import engine.rendering.*;
import executing.components.*;
import executing.components.lights.*;
import executing.systems.rendererSystems.*;
import executing.systems.updaterSystems.*;
import jamJar.core.fileManagement.*;
import jamJar.core.math.*;
import jamJar.core.math.vectors.*;

import static game.NightmareMaze.*;
import static engine.core.DisplayDevice.*;

public class MenuScene extends Scene
{
    public static final String GRAPHICS_NAME = FILE_PATH + "graphics.properties";

    Menu m_mainMenu;
    Menu m_optionsMenu;

    Entity m_helpText;
    Entity m_title;
    Entity m_backText;
    Entity m_fullScreenText;
    Entity m_enterToApplyText;

    GraphicsMode[] m_graphicsModes;
    GraphicsMode   m_selectedMode;
    boolean        m_isFullScreen;

    int m_oldIndex;

    @Override
    protected void OnSceneStart()
    {
        if(!FileSystem.PathExists(GRAPHICS_NAME))
        {
            FileSystem.CreateDirectory(FILE_PATH);
            FileSystem.CreateFile(GRAPHICS_NAME);

            Save();
        }
        else
        {
            FileReader reader = new FileReader(GRAPHICS_NAME);
            SetGraphicsMode(reader.Read(DisplayDevice.GraphicsMode.class), reader.Read(Boolean.class));
            GetCamera().SetProjection(70.0f, GraphicsAspectRatio(), 0.1f, 1000f);
        }

        UseSystem(MeshRendererSystem.class);
        UseSystem(TextRendererSystem.class);
        UseSystem(RotaterSystem.class);

        AddToFilePath("nightmareMaze/");

        Material ceilingMaterial = new Material();
        Material wallMaterial    = new Material();
        Material floorMaterial   = new Material();

        ceilingMaterial.SetProperty("Texture", LoadTexture("ceiling.png"));
        wallMaterial.SetProperty("Texture", LoadTexture("bricks.png"));
        floorMaterial.SetProperty("Texture", LoadTexture("tiles.png"));

        ceilingMaterial.SetProperty("NormalMap", LoadTexture("ceilingNormal.png"));
        wallMaterial.SetProperty("NormalMap", LoadTexture("bricksNormal.png"));
        floorMaterial.SetProperty("NormalMap", LoadTexture("tilesNormal.png"));

        ceilingMaterial.SetProperty("Reflectivity", 1f);
        wallMaterial.SetProperty("Reflectivity", 1f);
        floorMaterial.SetProperty("Reflectivity", 1f);

        ceilingMaterial.SetProperty("Damping", 8f);
        wallMaterial.SetProperty("Damping", 8f);
        floorMaterial.SetProperty("Damping", 8f);

        ceilingMaterial.SetProperty("TilingFactor", new Vector2f(2));

        CreateEntity(new Transformation(new Vector3f( 0.0f, -1.0f, 1.8f), new Quaternion(new Vector3f(0, 0, 1),   0.0f), new Vector3f(1)), new MeshComponent(CreateQuad()), floorMaterial);
        CreateEntity(new Transformation(new Vector3f(-1.0f,  0.0f, 1.8f), new Quaternion(new Vector3f(0, 0, 1), -90.0f).Multiply(new Quaternion(new Vector3f(0, 1, 0), 90.0f)), new Vector3f(1)), new MeshComponent(CreateQuad()), wallMaterial);
        CreateEntity(new Transformation(new Vector3f( 1.0f,  0.0f, 1.8f), new Quaternion(new Vector3f(0, 0, 1),  90.0f).Multiply(new Quaternion(new Vector3f(0, 1, 0), 90.0f)), new Vector3f(1)), new MeshComponent(CreateQuad()), wallMaterial);
        CreateEntity(new Transformation(new Vector3f( 0.0f,  0.0f, 1.8f), new Quaternion(new Vector3f(1, 0, 0), -90.0f), new Vector3f(1)), new MeshComponent(CreateQuad()), wallMaterial);
        CreateEntity(new Transformation(new Vector3f( 0.0f,  1.0f, 1.8f), new Quaternion(new Vector3f(0, 0, 1),  180.0f), new Vector3f(1)), new MeshComponent(CreateQuad()), ceilingMaterial);

        Material keyMaterial = new Material();
        keyMaterial.SetProperty("Color", new Vector3f(1, 0.75f, 0));
        keyMaterial.SetProperty("Reflectivity", 4f);
        keyMaterial.SetProperty("Damping", 32f);

        CreateEntity(new Transformation(new Vector3f(0.0f, -0.4f, 1.0f), new Quaternion(), new Vector3f(2.0f)), new MeshComponent(LoadMesh("key.obj")), keyMaterial, new RotaterComponent(new Vector3f(0, 1, 1), 80f));

        GetCamera().GetTransformation().Move(0, -0.2f, 0);

        CreateEntity(new PointLight(new Vector3f(1, 1, 0.9f), 0.8f, new Attenuation(0, 0, 2), GetCamera().GetTransformation().GetPosition()));

        CreateEntity(new PointLight(new Vector3f(1, 0.75f, 0), 0.2f, new Attenuation(0f, 0f, 2f), new Vector3f(0.0f, -0.6f, 1.0f)));

        SetSetting("AmbientLight", new Vector3f());

        m_mainMenu = new Menu(this, LoadFont(NightmareMaze.FONT_ATLAS), "Start", "Help", "Options", "Exit");

        m_graphicsModes = GetAvailableGraphicsModes();
        String[] menuItems = new String[m_graphicsModes.length];

        int selectedIndex = 0;
        for(int i = 0; i < m_graphicsModes.length; i++)
        {
            if(m_graphicsModes[i] == GetSelectedGraphicsMode())
                selectedIndex = i;

            menuItems[i] = m_graphicsModes[i].GetWidth() + "x" + m_graphicsModes[i].GetHeight() + "x" + m_graphicsModes[i].GetColorDepth();
        }

        m_oldIndex = selectedIndex;

        m_optionsMenu = new Menu(this, LoadFont(NightmareMaze.FONT_ATLAS), menuItems);
        m_optionsMenu.SetSelectedIndex(selectedIndex);
        m_optionsMenu.Hide();

        m_backText = CreateEntity(new TextComponent("Press B to go Back.", LoadFont(NightmareMaze.FONT_ATLAS), TextComponent.TextAlignment.BOTTOM_LEFT));
        m_backText.Hide();

        m_enterToApplyText = CreateEntity(new TextComponent("Press Enter to apply.", LoadFont(NightmareMaze.FONT_ATLAS), TextComponent.TextAlignment.BOTTOM_RIGHT));
        m_enterToApplyText.Hide();

        m_fullScreenText = CreateEntity(new TextComponent("Press F to toggle fullscreen.\n", LoadFont(NightmareMaze.FONT_ATLAS), TextComponent.TextAlignment.TOP_LEFT));
        m_fullScreenText.Hide();

        m_helpText = CreateEntity(new TextComponent("-W key move forward\n" +
                                                    "-S key move backward\n" +
                                                    "-M key to toggle music on and off\n" +
                                                    "-Esc key to unlock mouse\n" +
                                                    "-P key to pause\n\n" +
                                                    "-Click in game to enable mouse control\n" +
                                                    "-Mouse controls player direction\n\n", LoadFont(NightmareMaze.FONT_ATLAS), TextComponent.TextAlignment.TOP_LEFT));

        m_helpText.Hide();

        m_title = CreateEntity(new TextComponent("NIGHTMARE MAZE!", LoadFont("chiller"), TextComponent.TextAlignment.TOP_CENTER, new Vector2f(), new Vector2f(2)));
    }

    private void Save()
    {
        FileWriter writer = new FileWriter(GRAPHICS_NAME);
        writer.Write(GetSelectedGraphicsMode());
        writer.Write(GraphicsIsFullScreen());
    }

    private void UpdateFullScreenText()
    {
        int index = m_optionsMenu.GetSelectedIndex();
        TextComponent textComponent = m_fullScreenText.GetComponent(TextComponent.class);
        if(m_graphicsModes[index].IsFullScreenCapable())
            textComponent.SetText("Press F to toggle fullscreen.\n" + (m_isFullScreen ? "Fullscreen" : "Windowed"));
        else
            textComponent.SetText("Fullscreen Unavailable.");
    }

    @Override
    protected void OnSceneUpdate(InputDevice inputDevice, float delta)
    {
        if(m_mainMenu.IsVisible())
        {
            if(inputDevice.IsKeyPressed(InputDevice.Key.UP))
                m_mainMenu.PreviousItem();
            else if(inputDevice.IsKeyPressed(InputDevice.Key.DOWN))
                m_mainMenu.NextItem();
            else if(inputDevice.IsKeyPressed(InputDevice.Key.RETURN))
            {
                switch(m_mainMenu.GetSelectedIndex())
                {
                    case 0:
                        ResetScene(LevelScene.class);
                        ChangeScene(LevelScene.class);
                        break;
                    case 1:
                        m_title.Hide();
                        m_mainMenu.Hide();
                        m_helpText.Show();
                        m_backText.Show();
                        break;
                    case 2:
                        m_title.Hide();
                        m_mainMenu.Hide();
                        m_optionsMenu.Show();
                        m_enterToApplyText.Show();
                        m_fullScreenText.Show();
                        m_backText.Show();
                        m_isFullScreen = GraphicsIsFullScreen();
                        UpdateFullScreenText();
                        break;
                    case 3:
                        Exit();
                        break;
                }
            }
        }

        if(m_optionsMenu.IsVisible())
        {
            if(inputDevice.IsKeyPressed(InputDevice.Key.B))
            {
                m_optionsMenu.Hide();
                m_backText.Hide();
                m_enterToApplyText.Hide();
                m_fullScreenText.Hide();
                m_mainMenu.Show();
                m_title.Show();
                m_optionsMenu.SetSelectedIndex(m_oldIndex);
                UpdateFullScreenText();
            }

            if(inputDevice.IsKeyPressed(InputDevice.Key.F))
            {
                m_isFullScreen = !m_isFullScreen;
                UpdateFullScreenText();
            }

            if(inputDevice.IsKeyPressed(InputDevice.Key.UP))
            {
                m_optionsMenu.PreviousItem();
                UpdateFullScreenText();
            }
            else if(inputDevice.IsKeyPressed(InputDevice.Key.DOWN))
            {
                m_optionsMenu.NextItem();
                UpdateFullScreenText();
            }
            else if(inputDevice.IsKeyPressed(InputDevice.Key.RETURN))
            {
                int index = m_optionsMenu.GetSelectedIndex();
                m_selectedMode = m_graphicsModes[index];
                SetGraphicsMode(m_selectedMode, m_isFullScreen);
                GetCamera().SetProjection(70.0f, GraphicsAspectRatio(), 0.1f, 1000f);

                m_oldIndex = index;

                Save();
            }
        }

        if(m_helpText.IsVisible())
        {
            if(inputDevice.IsKeyPressed(InputDevice.Key.B))
            {
                m_helpText.Hide();
                m_backText.Hide();
                m_mainMenu.Show();
                m_title.Show();
            }
        }
    }

    @Override
    protected void OnSceneExit()
    {

    }
}
