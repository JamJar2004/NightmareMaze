package game;

import jamJar.core.fileManagement.*;
import jamJar.core.fileManagement.FileReader;
import jamJar.core.fileManagement.FileWriter;
import jamJar.core.math.*;
import jamJar.core.math.vectors.*;

import static game.NightmareMaze.*;

public class LevelSaveData
{
    public static final String FILE_NAME = FILE_PATH + "data.properties";

    private final int m_index;

    private final Vector3f   m_cameraPosition;
    private final Quaternion m_cameraRotation;
    private final Vector3f[] m_collectedKeys;
    private final Vector3f[] m_openedDoors;
    private final Vector2f[] m_passedLocations;

    private final float m_sludgeLevel;

    public LevelSaveData(int index, Vector3f cameraPosition, Quaternion cameraRotation, Vector3f[] collectedKeys, Vector3f[] openedDoors, Vector2f[] passedLocations, float sludgeLevel)
    {
        m_index = index;

        m_cameraPosition  = cameraPosition;
        m_cameraRotation  = cameraRotation;
        m_collectedKeys   = collectedKeys   == null ? new Vector3f[0] : collectedKeys;
        m_openedDoors     = openedDoors     == null ? new Vector3f[0] : openedDoors;
        m_passedLocations = passedLocations == null ? new Vector2f[0] : passedLocations;

        m_sludgeLevel = sludgeLevel;
    }

    public LevelSaveData()
    {
        if(!FileSystem.PathExists(FILE_NAME))
        {
            m_index = 0;

            m_cameraPosition  = new Vector3f(1.5f, 0.3f, 1.5f);
            m_cameraRotation  = new Quaternion();
            m_collectedKeys   = new Vector3f[0];
            m_openedDoors     = new Vector3f[0];
            m_passedLocations = new Vector2f[0];

            m_sludgeLevel = Level.INITIAL_SLUDGE_LEVEL;

            FileSystem.CreateDirectory(FILE_PATH);
            FileSystem.CreateFile(FILE_NAME);

            Save();
            return;
        }

        FileReader reader = new FileReader(FILE_NAME);

        m_index = reader.Read(Integer.class);

        m_cameraPosition  = reader.Read(Vector3f.class);
        m_cameraRotation  = reader.Read(Quaternion.class);
        m_collectedKeys   = reader.Read(Vector3f[].class);
        m_openedDoors     = reader.Read(Vector3f[].class);
        m_passedLocations = reader.Read(Vector2f[].class);

        m_sludgeLevel = reader.Read(Float.class);
    }

    public int GetIndex() { return m_index; }

    public Vector3f   GetCameraPosition()  { return m_cameraPosition;  }
    public Quaternion GetCameraRotation()  { return m_cameraRotation;  }
    public Vector3f[] GetCollectedKeys()   { return m_collectedKeys;   }
    public Vector3f[] GetOpenedDoors()     { return m_openedDoors;     }
    public Vector2f[] GetPassedLocations() { return m_passedLocations; }

    public float GetSludgeLevel() { return m_sludgeLevel; }

    public void Save()
    {
        FileWriter writer = new FileWriter(FILE_NAME);
        writer.Write(m_index);

        writer.Write(m_cameraPosition);
        writer.Write(m_cameraRotation);
        writer.Write(m_collectedKeys);
        writer.Write(m_openedDoors);
        writer.Write(m_passedLocations);

        writer.Write(m_sludgeLevel);
    }
}
