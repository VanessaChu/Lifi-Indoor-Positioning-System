package com.capstone.indoorpositioning.pathfinding;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.utils.Array;
import com.capstone.indoorpositioning.screens.LevelManager;

/**
 * Created by JackH_000 on 2017-03-21.
 */

public class GraphGenerator {

    public static GraphImp generateGraph(TiledMap map){
        Array<Node> nodes = new Array<Node>();
        TiledMapTileLayer collisionLayer = (TiledMapTileLayer)map.getLayers().get(0);
        int mapWidth = LevelManager.lvlTileWidth;
        int mapHeight = LevelManager.lvlTileHeight;

        // Loop over the tiles in the map, starting from the bottom left corner
        // iterating left to right, then down to up
        for (int y = 0; y < mapHeight; y++){
            for (int x = 0; x < mapWidth; x++){
                // Generate a Node for each tiles so that they all exist when we create connections
                Node node = new Node();
                node.type = Node.Type.REGULAR;
                nodes.add(node);
            }
        }

        for (int y = 0; y < mapHeight; y++){
            for (int x = 0; x < mapWidth; x++){
                TiledMapTileLayer.Cell target = collisionLayer.getCell(x, y);
                TiledMapTileLayer.Cell up = collisionLayer.getCell(x, y+1);
                TiledMapTileLayer.Cell down = collisionLayer.getCell(x, y-1);
                TiledMapTileLayer.Cell left = collisionLayer.getCell(x-1, y);
                TiledMapTileLayer.Cell right = collisionLayer.getCell(x+1, y);

                Node targetNode = nodes.get(mapWidth * y + x);
                if (!target.getTile().getProperties().containsKey("blocked")){
                    if (y != 0 && !down.getTile().getProperties().containsKey("blocked")){
                        Node downNode = nodes.get(mapWidth * (y - 1) + x);
                        targetNode.createConnection(downNode, 1);
                    }
                    if (x != 0 && !left.getTile().getProperties().containsKey("blocked")){
                        Node leftNode = nodes.get(mapWidth * y + x - 1);
                        targetNode.createConnection(leftNode, 1);
                    }
                    if (x != mapWidth - 1 && !right.getTile().getProperties().containsKey("blocked")){
                        Node rightNode = nodes.get(mapWidth * y + x + 1);
                        targetNode.createConnection(rightNode, 1);
                    }
                    if (y != mapHeight - 1 && !up.getTile().getProperties().containsKey("blocked")){
                        Node upNode = nodes.get(mapWidth * (y + 1) + x);
                        targetNode.createConnection(upNode, 1);
                    }
                }

            }
        }

        return new GraphImp(nodes);
    }
}
