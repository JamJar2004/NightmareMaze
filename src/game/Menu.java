package game;

import engine.ecs.*;
import engine.rendering.*;
import executing.components.*;
import jamJar.core.dataStructures.*;
import jamJar.core.dataStructures.lists.*;
import jamJar.core.math.vectors.*;

public class Menu
{
    private final Scene                           m_scene;
    private final ArrayList<Pair<String, Entity>> m_menuItems;

    private final float m_lineHeight;

    private int     m_selectedIndex;
    private boolean m_isVisible;

    public Menu(Scene scene, TextAtlas font, String... menuItems)
    {
        m_scene     = scene;
        m_menuItems = new ArrayList<>();

        m_selectedIndex = 0;
        m_isVisible     = true;

        float menuItemHeight = 1.0f / menuItems.length;

        m_lineHeight = font.GetLineHeight() / 600.0f;// / (float)m_scene.GetGame().GraphicsHeight();

        int columns = 1;
        if(menuItemHeight < m_lineHeight)
        {
            menuItemHeight = m_lineHeight;
            columns = (int)(m_lineHeight * menuItems.length);
        }

        int halfLength  = menuItems.length / 2;
        int halfColumns = columns / 2;

        float x = halfColumns * 0.5f;
        float y = (menuItemHeight * halfLength) / columns;
        if(menuItems.length % 2 == 0)
            y -= menuItemHeight * 0.5f;

        if(columns % 2 > 0 && columns != 1)
            x += 0.5f / columns;

        Vector2f position;
        for(int i = 0; i < menuItems.length; i++)
        {
            String menuItem = menuItems[i];

            position = new Vector2f(x, y);
            m_menuItems.Add(new Pair<>(menuItem, m_scene.CreateEntity(new TextComponent(menuItem, font, TextComponent.TextAlignment.MIDDLE_CENTER, new Vector2f(position), new Vector2f(1)))));
            y -= menuItemHeight;

            if((i + 1) % (menuItems.length / columns) == 0 && i != 0)
            {
                y = (menuItemHeight * halfLength) / columns;
                if(menuItems.length % 2 == 0)
                    y -= menuItemHeight * 0.5f;

                x -= 2.0f / columns;
            }
        }

        UpdateSelectedItem();
    }

    public void Show()
    {
        for(Pair<String, Entity> menuItem : m_menuItems)
            menuItem.GetSecond().Show();

        m_isVisible = true;
    }

    public void Hide()
    {
        for(Pair<String, Entity> menuItem : m_menuItems)
            menuItem.GetSecond().Hide();

        m_isVisible = false;
    }

    public boolean IsVisible() { return m_isVisible; }

    public int GetSelectedIndex() { return m_selectedIndex; }

    public void SetSelectedIndex(int index) { m_selectedIndex = index; UpdateSelectedItem(); }

    public void PreviousItem()
    {
        --m_selectedIndex;
        if(m_selectedIndex < 0)
            m_selectedIndex = m_menuItems.Count() - 1;

        UpdateSelectedItem();
    }

    public void NextItem()
    {
        ++m_selectedIndex;
        if(m_selectedIndex >= m_menuItems.Count())
            m_selectedIndex = 0;

        UpdateSelectedItem();
    }

    private void UpdateSelectedItem()
    {
        int index = 0;
        for(int i = 0; i < m_menuItems.Count(); i++)
        {
            Pair<String, Entity> menuItem = m_menuItems.Get(i);

            String text = index == m_selectedIndex ? ">" + menuItem.GetFirst() + "<" : "⠀" + menuItem.GetFirst() + "⠀";
            TextComponent textComponent = menuItem.GetSecond().GetComponent(TextComponent.class);
            textComponent.SetText(text);
            ++index;
        }
    }
}
