package game;

import engine.audio.*;
import engine.core.*;
import engine.ecs.*;
import engine.rendering.*;
import executing.components.*;
import executing.components.lights.*;
import executing.systems.updaterSystems.*;
import jamJar.core.*;
import jamJar.core.dataStructures.*;
import jamJar.core.dataStructures.lists.ArrayList;
import jamJar.core.dataStructures.maps.HashMap;
import jamJar.core.dataStructures.sets.*;
import jamJar.core.math.*;
import jamJar.core.math.Math;
import jamJar.core.math.vectors.*;
import jamJar.rendering.*;

public class Level
{
    private static final float PLAYER_RADIUS        = 0.2f;
    private static final float INITIAL_SPEED        = 2.3f;
    public  static final float INITIAL_SLUDGE_LEVEL = -0.02f;
    private static final float DOOR_FLASH_TIME      = 0.4f;

    private static final int TEXTURE_SIZE = 224;

    private final HashMap<Vector2f, Pair<Entity, Entity>> m_keys;
    private final HashMap<Vector3f, Entity>               m_collectedKeys;

    private final HashMap<Vector2f, Entity>   m_doors;
    private final HashMap<Vector3f, Vector2f> m_doorLocations;
    private final HashSet<Vector3f>           m_openedDoors;

    private final int m_index;

    private final Scene  m_scene;
    private final Bitmap m_map;
    private final Bitmap m_keyMap;
    private final Bitmap m_lightMap;
    private final Bitmap m_doorMap;
    private final Entity m_entity;

    private int m_numTexturesExp;
    private int m_numTextures;

    private int m_winXLocation;
    private int m_winZLocation;

    private final KeyboardControl m_keyboardControl;

    private float m_speed;

    private final AudioSource m_music;

    private final AudioSource m_leftFootStepSource;
    private final AudioSource m_rightFootStepSource;
    private final AudioSource m_leftSplashSource;
    private final AudioSource m_rightSplashSource;
    private final AudioSource m_evilLaughSource;
    private final AudioSource m_keySource;
    private final AudioSource m_unlockSource;
    private final AudioSource m_doorOpenedSource;
    private final AudioSource m_doorLockedSource;
    private final AudioSource m_doorSplashSource;

    private Vector3f    m_oldPosition;
    private double      m_stepStartTime;
    private double      m_lastLaughTime;
    private boolean     m_rightStep;
    private boolean     m_playSplash = false;

    private Entity   m_collidingDoor;
    private Vector3f m_collidingDoorColor;

    private double   m_doorFlashStartTime;
    private Entity   m_flashingDoor;

    private final Entity m_playerOnMap;
    private final Entity m_passedLocation;
    private       Entity m_lastSpot;

    private final HashMap<Vector2f, Entity> m_passedLocations;

    private final Entity m_sludge;

    private final Entity  m_messageGUI;
    private       boolean m_isShownMessageTimed;
    private       String  m_oldMessage;
    private       double  m_messageStartTime;
    private       double  m_messageShowTime;

    private final Entity m_escapeText;

    private final Menu m_pauseMenu;
    private final Menu m_loseMenu;
    private final Menu m_confirmationMenu;

    private boolean m_restartLevel;
    private boolean m_restartGame;

    private Event m_yesEvent;
    private Event m_noEvent;

    private boolean m_wasMouseGrabbed;

    public Level(Scene scene, int index, KeyboardControl keyboardControl, LevelSaveData saveData)
    {
        m_index = index;

        m_keys          = new HashMap<>();
        m_collectedKeys = new HashMap<>();

        m_doors         = new HashMap<>();
        m_doorLocations = new HashMap<>();
        m_openedDoors   = new HashSet<>();

        m_openedDoors.AddRange(saveData.GetOpenedDoors());

        HashSet<Vector3f> collectedKeys = new HashSet<>();
        collectedKeys.AddRange(saveData.GetCollectedKeys());

        m_scene    = scene;
        m_map      = new Bitmap("./res/nightmareMaze/textures/Levels/" + index + "/levelMap.png");
        m_keyMap   = new Bitmap("./res/nightmareMaze/textures/Levels/" + index + "/keyMap.png");
        m_lightMap = new Bitmap("./res/nightmareMaze/textures/Levels/" + index + "/lightMap.png");
        m_doorMap  = new Bitmap("./res/nightmareMaze/textures/Levels/" + index + "/doorMap.png");
        m_entity   = BuildLevel(m_scene.LoadTexture("Levels/" + index + "/Textures.png"),
                                m_scene.LoadTexture("Levels/" + index + "/NormalMaps.png"),
                                m_scene.LoadTexture("door.png"),
                                m_scene.LoadTexture("doorNormals.png"), collectedKeys);


        m_scene.SetSetting("SamplingMode", RenderDevice.SamplingMode.NEAREST);


        Bitmap mapBitmap = new Bitmap(1, 1);
        mapBitmap.SetPixel(0, 0, Color.GREY);
        GUIComponent mapGUI = new GUIComponent(m_scene.CreateTexture(mapBitmap), GUIComponent.GUIAlignment.TOP_RIGHT, new Vector2f(), new Vector2f(0.5f));
        m_scene.CreateEntity(mapGUI);

        Bitmap playerBitmap = new Bitmap(1, 1);
        playerBitmap.SetPixel(0, 0, Color.RED);

        Bitmap uncoveredBitmap = new Bitmap(1, 1);
        uncoveredBitmap.SetPixel(0, 0, Color.BLACK);

        Vector2f mapSize = new Vector2f(m_map.GetWidth(), m_map.GetHeight());

        Vector2f pixelSize = mapGUI.GetScale().Divide(mapSize);

        Vector2f playerPosition = new Vector2f(Math.Floor(saveData.GetCameraPosition().GetX()), Math.Floor(saveData.GetCameraPosition().GetZ()));

        m_playerOnMap = m_scene.CreateEntity(new GUIComponent(m_scene.CreateTexture(playerBitmap),
                GUIComponent.GUIAlignment.TOP_RIGHT,
                playerPosition.Multiply(pixelSize),
                pixelSize));

        m_passedLocation = m_scene.CreateEntity(new GUIComponent(m_scene.CreateTexture(uncoveredBitmap),
                GUIComponent.GUIAlignment.TOP_RIGHT,
                pixelSize,
                pixelSize));

        m_passedLocation.Hide();

        m_passedLocations = new HashMap<>();
        for(Vector2f location : saveData.GetPassedLocations())
        {
            Entity copiedEntity = m_scene.CopyEntity(m_passedLocation);
            GUIComponent component = copiedEntity.GetComponent(GUIComponent.class);
            component.SetPosition(location.Multiply(pixelSize));

            if(location.Equals(playerPosition))
                copiedEntity.Hide();

            m_passedLocations.Place(location, copiedEntity);
        }

        m_scene.SetSetting("SamplingMode", RenderDevice.SamplingMode.LINEAR);

        m_keyboardControl = keyboardControl;

        m_speed = INITIAL_SPEED;

        Material sludgeMaterial = new Material();
        sludgeMaterial.SetProperty("Color", new Vector3f(0.15f, 0.2f, 0.1f));
        m_sludge = m_scene.CreateEntity(
                new Transformation(
                        new Vector3f(m_map.GetWidth() / 2.0f, INITIAL_SLUDGE_LEVEL, m_map.GetHeight() / 2.0f),
                        new Quaternion(), new Vector3f(m_map.GetWidth(), 1, m_map.GetHeight())),
                new MeshComponent(m_scene.CreateQuad()), sludgeMaterial);

        m_music = m_scene.CreateAudioSource(m_scene.LoadSound("music.wav"));
        m_music.SetVolume(0.5f);
        m_music.Loop();
        m_music.Play();

        Sound footStepSound = m_scene.LoadSound("footStep.wav", true);
        Sound splashSound   = m_scene.LoadSound("splash.wav", true);

        m_leftFootStepSource  = m_scene.CreateAudioSource(footStepSound);
        m_rightFootStepSource = m_scene.CreateAudioSource(footStepSound);

        m_leftSplashSource  = m_scene.CreateAudioSource(splashSound);
        m_rightSplashSource = m_scene.CreateAudioSource(splashSound);

        m_evilLaughSource = m_scene.CreateAudioSource(m_scene.LoadSound("monsterLaughing.wav", true));
        m_lastLaughTime = Clock.GetSeconds();

        m_keySource        = m_scene.CreateAudioSource(m_scene.LoadSound("keyPickup.wav"));
        m_unlockSource     = m_scene.CreateAudioSource(m_scene.LoadSound("doorUnlocked.wav"));
        m_doorOpenedSource = m_scene.CreateAudioSource(m_scene.LoadSound("doorOpened.wav"));
        m_doorLockedSource = m_scene.CreateAudioSource(m_scene.LoadSound("doorLocked.wav"));
        m_doorSplashSource = m_scene.CreateAudioSource(m_scene.LoadSound("doorSplash.wav"));

        GetCameraTransformation().SetPosition(saveData.GetCameraPosition());
        GetCameraTransformation().SetRotation(saveData.GetCameraRotation());
        m_sludge.GetComponent(Transformation.class).GetPosition().SetY(saveData.GetSludgeLevel());

        m_scene.CreateEntity(new PointLight(new Vector3f(1, 1, 0.9f), 0.4f, new Attenuation(0, 0, 2), m_scene.GetCamera().GetTransformation().GetPosition()));
        m_oldPosition = GetCameraPosition();

        m_flashingDoor = null;

        m_messageGUI = m_scene.CreateEntity(new TextComponent("", m_scene.LoadFont(NightmareMaze.FONT_ATLAS), TextComponent.TextAlignment.MIDDLE_CENTER, new Vector2f(), new Vector2f(1.5f)));
        m_messageGUI.Hide();

        m_oldMessage = null;
        m_isShownMessageTimed = false;

        m_escapeText = m_scene.CreateEntity(new TextComponent("Press Esc to unlock mouse.", m_scene.LoadFont(NightmareMaze.FONT_ATLAS), TextComponent.TextAlignment.BOTTOM_RIGHT, new Vector2f(), new Vector2f(0.5f)));
        m_escapeText.Hide();

        m_pauseMenu = new Menu(m_scene, scene.LoadFont(NightmareMaze.FONT_ATLAS), "Continue", "Restart Level", "Restart Game", "Main Menu", "Exit");
        m_pauseMenu.Hide();

        m_loseMenu = new Menu(m_scene, scene.LoadFont(NightmareMaze.FONT_ATLAS), "Restart Level", "Restart Game", "Main Menu", "Exit");
        m_loseMenu.Hide();

        m_confirmationMenu = new Menu(m_scene, scene.LoadFont(NightmareMaze.FONT_ATLAS), "Yes", "No");
        m_confirmationMenu.Hide();

        m_restartLevel = false;
        m_restartGame  = false;

        m_wasMouseGrabbed = true;
    }

    private void ShowMessage(String message) { ShowMessage(message, TextComponent.TextAlignment.MIDDLE_CENTER); }

    private void ShowMessage(String message, TextComponent.TextAlignment alignment)
    {
        TextComponent textComponent = m_messageGUI.GetComponent(TextComponent.class);
        textComponent.SetText(message);
        textComponent.SetAlignment(alignment);

        if(m_messageGUI.IsVisible())
            m_oldMessage = textComponent.GetText();
        else
            m_messageGUI.Show();
    }

    private void HideMessage()
    {
        if(m_isShownMessageTimed)
        {
            double currentTime = Clock.GetSeconds();
            if(currentTime - m_messageStartTime < m_messageShowTime)
                return;
        }

        if(m_oldMessage != null)
        {
            m_messageGUI.GetComponent(TextComponent.class).SetText(m_oldMessage);
            m_oldMessage = null;
        }
        else
            m_messageGUI.Hide();
    }

    private void ShowMessage(String message, float seconds) { ShowMessage(message, TextComponent.TextAlignment.MIDDLE_CENTER, seconds); }

    private void ShowMessage(String message, TextComponent.TextAlignment alignment, float seconds)
    {
        TextComponent textComponent = m_messageGUI.GetComponent(TextComponent.class);
        textComponent.SetAlignment(alignment);

        if(m_messageGUI.IsVisible())
            m_oldMessage = textComponent.GetText();
        else
            m_messageGUI.Show();

        textComponent.SetText(message);

        m_isShownMessageTimed = true;

        m_messageStartTime = Clock.GetSeconds();
        m_messageShowTime  = seconds;
    }

    private Transformation GetCameraTransformation() { return m_scene.GetCamera().GetTransformation(); }

    private Vector3f GetCameraPosition() { return GetCameraTransformation().GetPosition(); }

    private Entity BuildLevel(Texture textures, Texture normalMaps, Texture doorTexture, Texture doorNormalMap, HashSet<Vector3f> collectedKeys)
    {
        Mesh keyMesh = m_scene.LoadMesh("key.obj");

        VertexList doorVertices = new VertexList();
        doorVertices.AddVertex(new Vertex(new Vector3f(-1, -1, -1), new Vector3f(1), new Vector2f(0, 1), new Vector3f(0,  0, -1), new Vector3f()));
        doorVertices.AddVertex(new Vertex(new Vector3f( 1, -1, -1), new Vector3f(1), new Vector2f(1, 1), new Vector3f(0,  0, -1), new Vector3f()));
        doorVertices.AddVertex(new Vertex(new Vector3f(-1,  1, -1), new Vector3f(1), new Vector2f(0, 0), new Vector3f(0,  0, -1), new Vector3f()));
        doorVertices.AddVertex(new Vertex(new Vector3f( 1,  1, -1), new Vector3f(1), new Vector2f(1, 0), new Vector3f(0,  0, -1), new Vector3f()));
        doorVertices.AddVertex(new Vertex(new Vector3f(-1, -1,  1), new Vector3f(1), new Vector2f(0, 1), new Vector3f(0,  0,  1), new Vector3f()));
        doorVertices.AddVertex(new Vertex(new Vector3f( 1, -1,  1), new Vector3f(1), new Vector2f(1, 1), new Vector3f(0,  0,  1), new Vector3f()));
        doorVertices.AddVertex(new Vertex(new Vector3f(-1,  1,  1), new Vector3f(1), new Vector2f(0, 0), new Vector3f(0,  0,  1), new Vector3f()));
        doorVertices.AddVertex(new Vertex(new Vector3f( 1,  1,  1), new Vector3f(1), new Vector2f(1, 0), new Vector3f(0,  0,  1), new Vector3f()));

        int[] doorIndices = new int[] { 0, 2, 3,
                                        3, 1, 0,
                                        6, 4, 5,
                                        7, 6, 5,
                                        1, 3, 7,
                                        7, 5, 1,
                                        4, 6, 2,
                                        0, 4, 2 };

        doorVertices.CalculateTangents(0, 2, 4, doorIndices);

        Mesh doorMesh = m_scene.CreateMesh(doorVertices, doorIndices);

        m_numTexturesExp = textures.GetData().GetWidth() / TEXTURE_SIZE;
        m_numTextures    = m_numTexturesExp * m_numTexturesExp;

        VertexList         vertexList = new VertexList();
        ArrayList<Integer> indexList  = new ArrayList<>();

        for(int i = 1; i < m_map.GetWidth() - 1; i++)
        {
            for(int j = 1; j < m_map.GetHeight() - 1; j++)
            {
                Color levelPixel = m_map.GetPixel(i, j);

                float[] wallTexCoords    = CalcTexCoords(m_map.GetPixel(i, j).GetRed());
                float[] floorTexCoords   = CalcTexCoords(m_map.GetPixel(i, j).GetGreen());
                float[] ceilingTexCoords = CalcTexCoords(m_map.GetPixel(i, j).GetBlue());

                if(levelPixel.Equals(Color.BLACK))
                    continue;

                if(m_map.GetPixel(i - 1, j).Equals(Color.BLACK))
                    AddFace(vertexList, indexList, i, j, false, 0, wallTexCoords);

                if(m_map.GetPixel(i, j - 1).Equals(Color.BLACK))
                    AddFace(vertexList, indexList, i, j, false, 2, wallTexCoords);

                if(m_map.GetPixel(i + 1, j).Equals(Color.BLACK))
                    AddFace(vertexList, indexList, i, j, true, 0, wallTexCoords);

                if(m_map.GetPixel(i, j + 1).Equals(Color.BLACK))
                    AddFace(vertexList, indexList, i, j, true, 2, wallTexCoords);

                AddFace(vertexList, indexList, i, j, false, 1, floorTexCoords);
                AddFace(vertexList, indexList, i, j, true, 1, ceilingTexCoords);

                Color lightMapPixel = m_lightMap.GetPixel(i, j);
                Color keyMapPixel   = m_keyMap.GetPixel(i, j);
                Color doorMapPixel  = m_doorMap.GetPixel(i, j);

                if(lightMapPixel.GetRed() >= 1.0f || lightMapPixel.GetGreen() >= 1.0f || lightMapPixel.GetBlue() >= 1.0f)
                {
                    Vector3f lightColor = new Vector3f(lightMapPixel.GetRed(), lightMapPixel.GetGreen(), lightMapPixel.GetBlue());

                    int texIndex = (int)(levelPixel.GetRed() * m_numTextures);

                    if(texIndex == 5)
                    {
                        m_scene.CreateEntity(new PointLight(lightColor, 0.2f, new Attenuation(0f, 0f, 1f), new Vector3f(i + 0.5f, 0.5f, j + 0.5f)));

                        m_winXLocation = i;
                        m_winZLocation = j;
                    }
                    else
                        m_scene.CreateEntity(new PointLight(lightColor, 0.2f, new Attenuation(4f, 6f, 1f), new Vector3f(i + 0.5f, 0.9f, j + 0.5f)));
                }

                if(keyMapPixel.GetRed() >= 1.0f || keyMapPixel.GetGreen() >= 1.0f || keyMapPixel.GetBlue() >= 1.0f)
                {
                    Vector3f keyColor    = new Vector3f(keyMapPixel.GetRed(), keyMapPixel.GetGreen(), keyMapPixel.GetBlue());
                    Vector3f keyPosition = new Vector3f(i + 0.5f, 0.2f, j + 0.5f);

                    if(m_openedDoors.Contains(keyColor))
                        continue;

                    Material keyMaterial = new Material();
                    keyMaterial.SetProperty("Color", keyColor);
                    keyMaterial.SetProperty("Reflectivity", 4f);
                    keyMaterial.SetProperty("Damping", 32f);


                    Entity key = m_scene.CreateEntity(new Transformation(keyPosition),
                                                      new MeshComponent(keyMesh),
                                                      keyMaterial);


                    if(collectedKeys.Contains(keyColor))
                    {
                        float offset = 0.02f * m_collectedKeys.Count();
                        m_collectedKeys.Place(new Vector3f(keyColor), key);

                        float xOffset = m_scene.GraphicsAspectRatio() * 0.1f + 0.03f;

                        Transformation keyTransformation = key.GetComponent(Transformation.class);
                        keyTransformation.SetParent(GetCameraTransformation());
                        keyTransformation.SetPosition(offset - xOffset, 0.125f, 0.2f);
                        keyTransformation.SetRotation(new Vector3f(-0.5f, 0, 1f), 45f);
                        keyTransformation.SetScale(0.05f);

                        keyColor.DivideEquals(4f);
                    }
                    else
                    {
                        Entity light = m_scene.CreateEntity(new PointLight(keyColor, 0.2f, new Attenuation(0f, 0f, 2f), keyPosition));
                        m_keys.Place(new Vector2f(i, j), new Pair<>(key, light));

                        key.AddComponent(new RotaterComponent(Vector3f.UP.Add(Vector3f.LEFT), 80f));
                    }
                }

                if(doorMapPixel.GetRed() >= 1.0f || doorMapPixel.GetGreen() >= 1.0f || doorMapPixel.GetBlue() >= 1.0f)
                {
                    Vector3f doorColor = new Vector3f(doorMapPixel.GetRed(), doorMapPixel.GetGreen(), doorMapPixel.GetBlue());

                    if(m_openedDoors.Contains(doorColor))
                        continue;

                    Material doorMaterial = new Material();
                    doorMaterial.SetProperty("Texture", doorTexture);
                    doorMaterial.SetProperty("NormalMap", doorNormalMap);
                    doorMaterial.SetProperty("Color", doorColor);
                    doorMaterial.SetProperty("Reflectivity", 1f);
                    doorMaterial.SetProperty("Damping", 8f);

                    Vector3f   doorPosition = new Vector3f(i + 0.5f, 0.5f, j + 0.5f);
                    Quaternion doorRotation = new Quaternion();
                    Vector3f   doorScale    = new Vector3f(0.5f, 0.5f, 0.05f);

                    boolean rotateDoor = m_map.GetPixel(i, j - 1).Equals(Color.BLACK) && m_map.GetPixel(i, j + 1).Equals(Color.BLACK);

                    if(rotateDoor)
                        doorRotation = doorRotation.Turn(new Vector3f(0, 1, 0), 90f);

                    Animation animation = new Animation();
                    animation.AddFrame(1f, new Transformation(new Vector3f(doorPosition), new Quaternion(doorRotation), new Vector3f(doorScale)));

                    Vector3f subtraction = new Vector3f(1, 0, 0);
                    if(rotateDoor)
                        subtraction = new Vector3f(0, 0, -1);


                    animation.AddFrame(0f, new Transformation(doorPosition.Subtract(subtraction), new Quaternion(doorRotation), new Vector3f(doorScale)));

                    animation.SetDuration(1.0f);

                    Entity door = m_scene.CreateEntity(new Transformation(new Vector3f(doorPosition), new Quaternion(doorRotation), new Vector3f(doorScale)),
                                                       new MeshComponent(doorMesh),
                                                       doorMaterial,
                                                       animation);

                    door.AddHandler("OnAnimationFinish", new EventHandler()
                    {
                        @Override
                        public void OnRaise(Entity entity, Object... args)
                        {
                            m_doorOpenedSource.SetPosition(entity.GetComponent(Transformation.class).GetPosition());
                            m_doorOpenedSource.Play();
                            entity.Free();
                        }
                    });

                    Vector2f location = new Vector2f(i, j);

                    m_doors.Place(location, door);
                    m_doorLocations.Place(doorColor, location);
                }
            }
        }

        int[] indices = Array.ToIntArray(indexList.ToArray());

        vertexList.CalculateNormals(0, 3, indices);
        vertexList.CalculateTangents(0, 2, 4, indices);

        Mesh mesh = m_scene.CreateMesh(vertexList, indices);

        Material material = new Material();
        material.SetProperty("Texture", textures);
        material.SetProperty("NormalMap", normalMaps);

        material.SetProperty("Reflectivity", 0.5f);
        material.SetProperty("Damping", 4f);

        return m_scene.CreateEntity(new Transformation(), new MeshComponent(mesh), material);
    }

    private void AddFace(VertexList vertices, ArrayList<Integer> indices, int i, int j, boolean direction, int axis, float[] texCoords)
    {
        int offset = vertices.GetVertexCount();

        float side = direction ? 1.0f : 0.0f;

        int axis1 = (axis + 1) % 3;
        int axis2 = (axis + 2) % 3;

        Vector3f pos1 = new Vector3f(0.0f);
        Vector3f pos2 = new Vector3f(0.0f);
        Vector3f pos3 = new Vector3f(0.0f);
        Vector3f pos4 = new Vector3f(0.0f);

        pos1.SetValue(axis, side);
        pos2.SetValue(axis, side);
        pos3.SetValue(axis, side);
        pos4.SetValue(axis, side);

        pos2.SetValue(axis1, 1.0f);
        pos3.SetValue(axis2, 1.0f);

        pos4.SetValue(axis1, 1.0f);
        pos4.SetValue(axis2, 1.0f);

        Vector2f texCoords1 = new Vector2f();
        Vector2f texCoords2 = new Vector2f();
        Vector2f texCoords3 = new Vector2f();
        Vector2f texCoords4 = new Vector2f();

        if(axis == 0)
        {
            texCoords1 = new Vector2f(texCoords[0], texCoords[2]);
            texCoords2 = new Vector2f(texCoords[0], texCoords[3]);
            texCoords3 = new Vector2f(texCoords[1], texCoords[2]);
            texCoords4 = new Vector2f(texCoords[1], texCoords[3]);
        }
        else if(axis == 1)
        {
            texCoords1 = new Vector2f(texCoords[0], texCoords[2]);
            texCoords2 = new Vector2f(texCoords[0], texCoords[3]);
            texCoords3 = new Vector2f(texCoords[1], texCoords[2]);
            texCoords4 = new Vector2f(texCoords[1], texCoords[3]);
        }
        else if(axis == 2)
        {
            texCoords1 = new Vector2f(texCoords[0], texCoords[2]);
            texCoords2 = new Vector2f(texCoords[1], texCoords[2]);
            texCoords3 = new Vector2f(texCoords[0], texCoords[3]);
            texCoords4 = new Vector2f(texCoords[1], texCoords[3]);
        }

        vertices.AddVertex(new Vertex(pos1.Add(new Vector3f(i, 0, j)), new Vector3f(1), texCoords1, new Vector3f(), new Vector3f()));
        vertices.AddVertex(new Vertex(pos2.Add(new Vector3f(i, 0, j)), new Vector3f(1), texCoords2, new Vector3f(), new Vector3f()));
        vertices.AddVertex(new Vertex(pos3.Add(new Vector3f(i, 0, j)), new Vector3f(1), texCoords3, new Vector3f(), new Vector3f()));
        vertices.AddVertex(new Vertex(pos4.Add(new Vector3f(i, 0, j)), new Vector3f(1), texCoords4, new Vector3f(), new Vector3f()));

        if(direction)
        {
            indices.Add(offset + 2);
            indices.Add(offset + 1);
            indices.Add(offset    );
            indices.Add(offset + 1);
            indices.Add(offset + 2);
            indices.Add(offset + 3);
        }
        else
        {
            indices.Add(offset    );
            indices.Add(offset + 1);
            indices.Add(offset + 2);
            indices.Add(offset + 3);
            indices.Add(offset + 2);
            indices.Add(offset + 1);
        }
    }

    private float[] CalcTexCoords(float value)
    {
        int index = (int)(value * m_numTextures);

        int texX = index % m_numTexturesExp;
        int texY = index / m_numTexturesExp;

        float[] result = new float[4];

        result[1] = (texX + 0.002f) / (float)m_numTexturesExp;
        result[0] = (texX + 0.998f) / (float)m_numTexturesExp;
        result[3] = (texY + 0.002f) / (float)m_numTexturesExp;
        result[2] = (texY + 0.998f) / (float)m_numTexturesExp;

        return result;
    }

    private void HandleCollision(Vector3f movement, boolean xWall, boolean zWall)
    {
        float x = xWall ? movement.GetX() : 0;
        float z = zWall ? movement.GetZ() : 0;

        GetCameraPosition().SubtractEquals(x, 0, z);
    }

    private boolean HandleCollision(Vector3f oldPosition, Vector3f newPosition)
    {
        int oldX = (int)oldPosition.GetX();
        int oldY = (int)oldPosition.GetZ();

        float offsetX = (newPosition.GetX() - oldPosition.GetX()) < 0 ? -PLAYER_RADIUS : PLAYER_RADIUS;
        float offsetZ = (newPosition.GetZ() - oldPosition.GetZ()) < 0 ? -PLAYER_RADIUS : PLAYER_RADIUS;

        int x = (int)(newPosition.GetX() + offsetX);
        int y = (int)(newPosition.GetZ() + offsetZ);

        if(x == m_winXLocation && y == m_winZLocation)
            return true;

        Vector2f playerLocation = new Vector2f(Math.Floor(newPosition.GetX()), Math.Floor(newPosition.GetZ()));

        GUIComponent playerGUI = m_playerOnMap.GetComponent(GUIComponent.class);
        playerGUI.SetPosition(playerGUI.GetScale().Multiply(playerLocation));

        Entity foundSpot = m_passedLocations.Get(playerLocation);

        if(foundSpot == null)
        {
            Entity copiedEntity = m_scene.CopyEntity(m_passedLocation);
            m_passedLocations.Place(playerLocation, copiedEntity);

            GUIComponent passedGUI = copiedEntity.GetComponent(GUIComponent.class);
            passedGUI.SetPosition(passedGUI.GetScale().Multiply(playerLocation));
        }
        else
        {
            if(m_lastSpot != null)
                m_lastSpot.Show();

            foundSpot.Hide();

            m_lastSpot = foundSpot;
        }

        if(m_map.GetPixel(x, y).Equals(Color.BLACK) || m_map.GetPixel(oldX, y).Equals(Color.BLACK) || m_map.GetPixel(x, oldY).Equals(Color.BLACK))
        {
            boolean xWall = m_map.GetPixel(x, oldY).Equals(Color.BLACK);
            boolean zWall = m_map.GetPixel(oldX, y).Equals(Color.BLACK);
            HandleCollision(newPosition.Subtract(oldPosition), xWall, zWall);
        }

        Vector2f position = new Vector2f(oldX, oldY);

        Pair<Entity, Entity> keyPair = m_keys.Get(position);
        if(keyPair != null)
        {
            m_keys.Remove(position);
            keyPair.GetSecond().Free();

            Entity key = keyPair.GetFirst();
            Vector3f keyColor = key.GetComponent(Material.class).GetProperty("Color");

            float offset = 0.02f * m_collectedKeys.Count();
            m_collectedKeys.Place(new Vector3f(keyColor), key);

            key.RemoveComponent(RotaterComponent.class);

            float xOffset = m_scene.GraphicsAspectRatio() * 0.1f + 0.03f;

            Transformation keyTransformation = key.GetComponent(Transformation.class);
            keyTransformation.SetParent(GetCameraTransformation());
            keyTransformation.SetPosition(offset - xOffset, 0.125f, 0.2f);
            keyTransformation.SetRotation(new Vector3f(-0.5f, 0, 1f), 45f);
            keyTransformation.SetScale(0.05f);

            keyColor.DivideEquals(4f);

            Save();
            m_keySource.Play();
        }

        Entity door = m_doors.Get(position);
        if(door != null)
        {
            Transformation transformation = door.GetComponent(Transformation.class);

            if(!transformation.GetRotation().Equals(new Quaternion()))
            {
                if(newPosition.GetX() - oldX > transformation.GetScale().GetZ() * 0.5f && (oldX + 1) - newPosition.GetX() > transformation.GetScale().GetZ() * 0.5f)
                    GetCameraPosition().SetX(oldPosition.GetX());
            }
            else
            {
                if(newPosition.GetZ() - oldY > transformation.GetScale().GetZ() * 0.5f && (oldY + 1) - newPosition.GetZ() > transformation.GetScale().GetZ() * 0.5f)
                    GetCameraPosition().SetZ(oldPosition.GetZ());
            }

            ShowMessage("Press O to try\nopening the door.");
            m_collidingDoor = door;
        }
        else
        {
            HideMessage();
            m_collidingDoor = null;
        }

        return false;
    }

    private void ChangeSludgeLevel(float amount) { m_sludge.GetComponent(Transformation.class).Move(0, amount, 0); }

    private float GetSludgeLevel() { return m_sludge.GetComponent(Transformation.class).GetPosition().GetY(); }

    public void Save()
    {
        new LevelSaveData(m_index, GetCameraPosition(), GetCameraTransformation().GetRotation(), m_collectedKeys.GetKeys().ToArray(), m_openedDoors.ToArray(), m_passedLocations.GetKeys().ToArray(), GetSludgeLevel()).Save();
    }

    private void ShowConfirmation(String message, Event yesEvent, Event noEvent)
    {
        ShowMessage(message, TextComponent.TextAlignment.TOP_CENTER);
        m_confirmationMenu.Show();
        m_yesEvent = yesEvent;
        m_noEvent  = noEvent;
    }

    private void Lose(InputDevice inputDevice)
    {
        ShowMessage("You Lost!", TextComponent.TextAlignment.TOP_CENTER);
        m_music.Stop();
        m_loseMenu.Show();
        m_scene.DisableSystem(KeyboardControlSystem.class);
        m_scene.DisableSystem(MouseLookSystem.class);

        inputDevice.SetMouseGrabbed(false);
    }

    private void Pause(InputDevice inputDevice)
    {
        //Save();

        ShowMessage("Paused.", TextComponent.TextAlignment.TOP_CENTER);
        m_pauseMenu.Show();
        m_scene.DisableSystem(KeyboardControlSystem.class);
        m_scene.DisableSystem(MouseLookSystem.class);

        inputDevice.SetMouseGrabbed(false);
    }

    private void Continue()
    {
        HideMessage();
        m_pauseMenu.Hide();
        m_scene.UseSystem(KeyboardControlSystem.class);
        m_scene.UseSystem(MouseLookSystem.class);
    }

    public boolean IsLost() { return m_loseMenu.IsVisible(); }

    public LevelScene.LevelEvent Update(InputDevice inputDevice, float delta)
    {
        if(m_restartGame)
            return LevelScene.LevelEvent.RESTART_GAME;

        if(m_restartLevel)
            return LevelScene.LevelEvent.RESTART_LEVEL;

        if(m_confirmationMenu.IsVisible())
        {
            if(inputDevice.IsKeyPressed(InputDevice.Key.UP))
                m_confirmationMenu.PreviousItem();
            else if(inputDevice.IsKeyPressed(InputDevice.Key.DOWN))
                m_confirmationMenu.NextItem();
            else if(inputDevice.IsKeyPressed(InputDevice.Key.RETURN))
            {
                if(m_confirmationMenu.GetSelectedIndex() == 0)
                    m_yesEvent.Raise();
                else
                    m_noEvent.Raise();


                HideMessage();
                m_confirmationMenu.Hide();
            }
            return LevelScene.LevelEvent.NONE;
        }

        if(m_loseMenu.IsVisible())
        {
            if(inputDevice.IsKeyPressed(InputDevice.Key.UP))
                m_loseMenu.PreviousItem();
            else if(inputDevice.IsKeyPressed(InputDevice.Key.DOWN))
                m_loseMenu.NextItem();
            else if(inputDevice.IsKeyPressed(InputDevice.Key.RETURN))
            {
                switch(m_loseMenu.GetSelectedIndex())
                {
                    case 0:
                        m_loseMenu.Hide();
                        ShowConfirmation("Are you sure you want to\nrestart this level?", new Event()
                        {
                            @Override
                            public void Raise(Object... args)
                            {
                                m_restartLevel = true;
                            }
                        }, new Event()
                        {
                            @Override
                            public void Raise(Object... args)
                            {
                                m_restartLevel = false;
                                Lose(inputDevice);
                            }
                        });
                        break;
                    case 1:
                        m_loseMenu.Hide();
                        ShowConfirmation("Are you sure you want to\nrestart the entire game?", new Event()
                        {
                            @Override
                            public void Raise(Object... args)
                            {
                                m_restartGame = true;
                            }
                        }, new Event()
                        {
                            @Override
                            public void Raise(Object... args)
                            {
                                m_restartGame = false;
                                Lose(inputDevice);
                            }
                        });
                        break;
                    case 2:
                        m_scene.ClearScene();
                        m_scene.ResetScene(MenuScene.class);
                        m_scene.ChangeScene(MenuScene.class);
                        break;
                    case 3:
                        m_scene.Exit();
                        break;
                }
            }
            return LevelScene.LevelEvent.NONE;
        }

        if(inputDevice.IsKeyPressed(InputDevice.Key.M))
        {
            if(m_music.IsPlaying())
            {
                m_music.Pause();
                ShowMessage("Music Off", 1);
            }
            else
            {
                m_music.Play();
                ShowMessage("Music On", 1);
            }
        }

        if(inputDevice.IsKeyPressed(InputDevice.Key.P))
        {
            if(m_pauseMenu.IsVisible())
                Continue();
            else
                Pause(inputDevice);

            return LevelScene.LevelEvent.NONE;
        }

        if(m_pauseMenu.IsVisible())
        {
            if(inputDevice.IsKeyPressed(InputDevice.Key.UP))
                m_pauseMenu.PreviousItem();
            else if(inputDevice.IsKeyPressed(InputDevice.Key.DOWN))
                m_pauseMenu.NextItem();
            else if(inputDevice.IsKeyPressed(InputDevice.Key.RETURN))
            {
                switch(m_pauseMenu.GetSelectedIndex())
                {
                    case 0:
                        Continue();
                        break;
                    case 1:
                        m_pauseMenu.Hide();
                        ShowConfirmation("Are you sure you want to\nrestart this level?", new Event()
                        {
                            @Override
                            public void Raise(Object... args)
                            {
                                m_restartLevel = true;
                            }
                        }, new Event()
                        {
                            @Override
                            public void Raise(Object... args)
                            {
                                m_restartLevel = false;
                                Pause(inputDevice);
                            }
                        });
                        break;
                    case 2:
                        m_pauseMenu.Hide();
                        ShowConfirmation("Are you sure you want to\nrestart the entire game?", new Event()
                        {
                            @Override
                            public void Raise(Object... args)
                            {
                                m_restartGame = true;
                            }
                        }, new Event()
                        {
                            @Override
                            public void Raise(Object... args)
                            {
                                m_restartGame = false;
                                Pause(inputDevice);
                            }
                        });
                        break;
                    case 3:
                        Save();
                        m_scene.ClearScene();
                        m_scene.ResetScene(MenuScene.class);
                        m_scene.ChangeScene(MenuScene.class);
                        break;
                    case 4:
                        Save();
                        m_scene.Exit();
                        break;
                }
            }
            return LevelScene.LevelEvent.NONE;
        }

        if(m_wasMouseGrabbed != inputDevice.IsMouseGrabbed())
        {
            if(inputDevice.IsMouseGrabbed())
            {
                HideMessage();
                m_escapeText.Show();
            }
            else
            {
                ShowMessage("Click to control with mouse.");
                m_escapeText.Hide();
            }
            m_wasMouseGrabbed = inputDevice.IsMouseGrabbed();
        }

        Vector3f newPosition = GetCameraPosition();

        if(m_isShownMessageTimed)
        {
            double currentTime = Clock.GetSeconds();
            if(currentTime - m_messageStartTime >= m_messageShowTime)
            {
                HideMessage();
                m_isShownMessageTimed = false;
            }
        }

        ChangeSludgeLevel(0.002f * delta);

        float sludgeLevel = GetSludgeLevel();

        m_keyboardControl.SetKeyControl(InputDevice.Key.W, new Vector3f(0, 0,  1), m_speed);
        m_keyboardControl.SetKeyControl(InputDevice.Key.S, new Vector3f(0, 0, -1), m_speed);

        if(sludgeLevel > 0.0f)
        {
            float maxSludgeLevel = newPosition.GetY() - 0.05f;

            float sludgeDist = 1.0f - (sludgeLevel / maxSludgeLevel);

            m_speed = (INITIAL_SPEED * (sludgeDist * 0.8f + 0.2f));

            m_leftSplashSource.SetVolume((1.0f - sludgeDist) * 0.8f + 0.2f);
            m_leftSplashSource.SetPitch(m_speed / INITIAL_SPEED);

            m_rightSplashSource.SetVolume((1.0f - sludgeDist) * 0.8f + 0.2f);
            m_rightSplashSource.SetPitch(m_speed / INITIAL_SPEED);

            m_leftFootStepSource.SetVolume(sludgeDist * 0.8f + 0.2f);
            m_rightFootStepSource.SetVolume(sludgeDist * 0.8f + 0.2f);

            m_playSplash = true;
            if(sludgeLevel > newPosition.GetY() - 0.05f)
            {
                Lose(inputDevice);
                return LevelScene.LevelEvent.NONE;
            }
        }

        if(!newPosition.Equals(m_oldPosition))
        {
            if(HandleCollision(m_oldPosition, newPosition))
                return LevelScene.LevelEvent.NEXT_LEVEL;

            if(Clock.GetSeconds() > m_stepStartTime + 0.6 / m_speed)
            {
                m_stepStartTime = Clock.GetSeconds();

                Vector3f leftPosition  = newPosition.Add(new Vector3f(-1, 0, 0));
                Vector3f rightPosition = newPosition.Add(new Vector3f( 1, 0, 0));

                m_leftFootStepSource.SetPosition(leftPosition);
                m_rightFootStepSource.SetPosition(rightPosition);

                m_leftSplashSource.SetPosition(leftPosition);
                m_rightSplashSource.SetPosition(rightPosition);

                if(m_rightStep)
                {
                    m_rightFootStepSource.Play();
                    if(m_playSplash)
                        m_rightSplashSource.Play();
                }
                else
                {
                    m_leftFootStepSource.Play();
                    if(m_playSplash)
                        m_leftSplashSource.Play();
                }

                m_rightStep = !m_rightStep;

            }
        }

        if(Clock.GetSeconds() > m_lastLaughTime + 15.0f + Math.Random(0f, 5f))
        {
            m_evilLaughSource.SetPosition(GetCameraPosition().Add(new Vector3f(Math.Random(-2, 2), 0, Math.Random(-2, 2))));
            m_evilLaughSource.Play();
            m_lastLaughTime = Clock.GetSeconds();
        }

        if(inputDevice.IsKeyPressed(InputDevice.Key.O) && m_collidingDoor != null)
        {
            Material  doorMaterial  = m_collidingDoor.GetComponent(Material.class);
            Animation doorAnimation = m_collidingDoor.GetComponent(Animation.class);

            if(m_flashingDoor == null)
            {
                Vector3f doorColor = doorMaterial.GetProperty("Color");

                Entity key = m_collectedKeys.Get(doorColor);
                if(key == null)
                {
                    m_doorFlashStartTime = Clock.GetSeconds();
                    m_flashingDoor       = m_collidingDoor;
                    m_collidingDoorColor = new Vector3f(doorColor);

                    ShowMessage("Door Locked!\nFind the key!", DOOR_FLASH_TIME);

                    m_doorLockedSource.SetPosition(m_collidingDoor.GetComponent(Transformation.class).GetPosition());
                    m_doorLockedSource.Play();
                }
                else
                {
                    m_collidingDoor = null;

                    m_unlockSource.Play();
                    doorAnimation.Play();

                    Vector2f doorPosition = m_doorLocations.Get(doorColor);
                    m_doorLocations.Remove(doorColor);
                    m_doors.Remove(doorPosition);
                    m_openedDoors.Add(doorColor);

                    m_collectedKeys.Remove(doorColor);

                    int index = 0;
                    for(Entity entity : m_collectedKeys.GetValues())
                    {
                        float offset = 0.02f * index;
                        Transformation keyTransformation = entity.GetComponent(Transformation.class);
                        keyTransformation.SetPosition(-0.17f + offset, 0.125f, 0.2f);
                        ++index;
                    }

                    key.Free();

                    ChangeSludgeLevel(-0.1f);
                    Transformation sludgeTransformation = m_sludge.GetComponent(Transformation.class);

                    if(sludgeTransformation.GetPosition().GetY() < 0)
                        sludgeTransformation.GetPosition().SetY(0);

                    Save();
                    m_doorSplashSource.Play();
                }
            }
        }

        if(m_flashingDoor != null)
        {
            Vector3f doorColor = m_flashingDoor.GetComponent(Material.class).GetProperty("Color");

            double timePassed = Clock.GetSeconds() - m_doorFlashStartTime;

            double secondTime = timePassed % 0.2f;

            Vector3f invertColor = new Vector3f(1).Subtract(m_collidingDoorColor);

            if(secondTime < 0.1f)
                doorColor.Set(m_collidingDoorColor.Lerp(invertColor, (float)secondTime * 10f));
            else if(secondTime >= 0.1f)
                doorColor.Set(m_collidingDoorColor.Lerp(invertColor, 1.0f - (((float)secondTime - 0.1f) * 10f)));

            if(timePassed >= DOOR_FLASH_TIME)
            {
                m_flashingDoor = null;
                doorColor.Set(m_collidingDoorColor);
            }
        }

        m_oldPosition = new Vector3f(newPosition);

        return LevelScene.LevelEvent.NONE;
    }
}
