package game;


import engine.core.*;
import engine.ecs.*;
import engine.rendering.*;
import executing.components.*;
import executing.components.lights.*;
import executing.systems.rendererSystems.*;
import executing.systems.updaterSystems.*;
import jamJar.core.math.*;
import jamJar.core.math.vectors.*;

public class WinScene extends Scene
{
    Menu m_menu;

    @Override
    protected void OnSceneStart()
    {
        UseSystem(MeshRendererSystem.class);
        UseSystem(TextRendererSystem.class);
        UseSystem(RotaterSystem.class);

        AddToFilePath("nightmareMaze/");

        Material ceilingMaterial = new Material();
        Material wallMaterial    = new Material();
        Material floorMaterial   = new Material();

        ceilingMaterial.SetProperty("Texture", LoadTexture("winTexture.png"));
        wallMaterial.SetProperty("Texture", LoadTexture("winTexture.png"));
        floorMaterial.SetProperty("Texture", LoadTexture("winTexture.png"));

        ceilingMaterial.SetProperty("NormalMap", LoadTexture("winNormal.png"));
        wallMaterial.SetProperty("NormalMap", LoadTexture("winNormal.png"));
        floorMaterial.SetProperty("NormalMap", LoadTexture("winNormal.png"));

        ceilingMaterial.SetProperty("Reflectivity", 4f);
        wallMaterial.SetProperty("Reflectivity", 4f);
        floorMaterial.SetProperty("Reflectivity", 4f);

        ceilingMaterial.SetProperty("Damping", 32f);
        wallMaterial.SetProperty("Damping", 32f);
        floorMaterial.SetProperty("Damping", 32f);

        ceilingMaterial.SetProperty("TilingFactor", new Vector2f(2));

        CreateEntity(new Transformation(new Vector3f( 0.0f, -1.0f, 1.8f), new Quaternion(new Vector3f(0, 0, 1),   0.0f), new Vector3f(1)), new MeshComponent(CreateQuad()), floorMaterial);
        CreateEntity(new Transformation(new Vector3f(-1.0f,  0.0f, 1.8f), new Quaternion(new Vector3f(0, 0, 1), -90.0f).Multiply(new Quaternion(new Vector3f(0, 1, 0), 90.0f)), new Vector3f(1)), new MeshComponent(CreateQuad()), wallMaterial);
        CreateEntity(new Transformation(new Vector3f( 1.0f,  0.0f, 1.8f), new Quaternion(new Vector3f(0, 0, 1),  90.0f).Multiply(new Quaternion(new Vector3f(0, 1, 0), 90.0f)), new Vector3f(1)), new MeshComponent(CreateQuad()), wallMaterial);
        CreateEntity(new Transformation(new Vector3f( 0.0f,  0.0f, 1.8f), new Quaternion(new Vector3f(1, 0, 0), -90.0f), new Vector3f(1)), new MeshComponent(CreateQuad()), wallMaterial);
        CreateEntity(new Transformation(new Vector3f( 0.0f,  1.0f, 1.8f), new Quaternion(new Vector3f(0, 0, 1),  180.0f), new Vector3f(1)), new MeshComponent(CreateQuad()), ceilingMaterial);

        Material keyMaterial = new Material();
        keyMaterial.SetProperty("Color", new Vector3f(1, 0.75f, 0).Multiply(0.3f));
        keyMaterial.SetProperty("Reflectivity", 4f);
        keyMaterial.SetProperty("Damping", 32f);

        CreateEntity(new Transformation(new Vector3f(0.0f, -0.2f, 1.0f), new Quaternion(), new Vector3f(0.002f)), new MeshComponent(LoadMesh("trophie.obj")), keyMaterial, new RotaterComponent(new Vector3f(0, 1, 0.5f), 80f));

        GetCamera().GetTransformation().Move(0, -0.2f, 0);

        CreateEntity(new PointLight(new Vector3f(1), 0.2f, new Attenuation(0f, 0f, 0.1f), new Vector3f(0.0f, -0.5f, 0.2f)));

        CreateEntity(new TextComponent("YOU WON!", LoadFont(NightmareMaze.FONT_ATLAS), TextComponent.TextAlignment.TOP_CENTER, new Vector2f(), new Vector2f(2)));

        SetSetting("AmbientLight", new Vector3f());

        m_menu = new Menu(this, LoadFont(NightmareMaze.FONT_ATLAS), "Play Again", "Main Menu", "Exit");
    }

    @Override
    protected void OnSceneUpdate(InputDevice inputDevice, float delta)
    {
        if(m_menu.IsVisible())
        {
            if(inputDevice.IsKeyPressed(InputDevice.Key.UP))
                m_menu.PreviousItem();
            else if(inputDevice.IsKeyPressed(InputDevice.Key.DOWN))
                m_menu.NextItem();
            else if(inputDevice.IsKeyPressed(InputDevice.Key.RETURN))
            {
                switch(m_menu.GetSelectedIndex())
                {
                    case 0:
                        ResetScene(LevelScene.class);
                        ChangeScene(LevelScene.class);
                        break;
                    case 1:
                        ResetScene(MenuScene.class);
                        ChangeScene(MenuScene.class);
                        break;
                    case 2:
                        Exit();
                }
            }
        }
    }

    @Override
    protected void OnSceneExit()
    {

    }
}
