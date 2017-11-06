package com.capstone.indoorpositioning.screens;

import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.capstone.indoorpositioning.pathfinding.GraphGenerator;
import com.capstone.indoorpositioning.pathfinding.GraphImp;

/**
 * Created by JackH_000 on 2017-03-21.
 */

public class LevelManager {
    public static int lvlTileWidth, lvlTileHeight;
    public static int lvlPixelWidth, lvlPixelHeight;
    public static int tilePixelWidth, tilePixelHeight;

    public static TiledMap map;
    public static GraphImp graph;

    public static void loadLevel(String filePath){
        map = new TmxMapLoader().load(filePath);

        MapProperties properties = map.getProperties();
        lvlTileWidth = properties.get("width", Integer.class);
        lvlTileHeight = properties.get("height", Integer.class);
        tilePixelWidth = properties.get("tilewidth", Integer.class);
        tilePixelHeight = properties.get("tileheight", Integer.class);
        lvlPixelWidth = lvlTileWidth * tilePixelWidth;
        lvlPixelHeight = lvlTileHeight * tilePixelHeight;

        graph = GraphGenerator.generateGraph(map);

    }
}
