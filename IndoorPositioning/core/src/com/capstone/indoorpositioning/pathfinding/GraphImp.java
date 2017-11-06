package com.capstone.indoorpositioning.pathfinding;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.utils.Array;
import com.capstone.indoorpositioning.screens.LevelManager;

/**
 * Created by JackH_000 on 2017-03-21.
 */

public class GraphImp implements IndexedGraph<Node> {
    private Array<Node> nodes = new Array<Node>();

    public GraphImp(Array<Node> nodes){
        this.nodes = nodes;
    }

    @Override
    public int getIndex(Node node) {
        return node.getIndex();
    }

    @Override
    public int getNodeCount() {
        return nodes.size;
    }

    @Override
    public Array<Connection<Node>> getConnections(Node fromNode) {
        return fromNode.getConnections();
    }

    public Node getNodeByXY(int x, int y){
        int modX = x / LevelManager.tilePixelWidth;
        int modY = y / LevelManager.tilePixelHeight;

        return nodes.get(LevelManager.lvlTileWidth * modY + modX);
    }
}
