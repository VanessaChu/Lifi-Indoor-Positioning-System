package com.capstone.indoorpositioning.pathfinding;

import com.badlogic.gdx.ai.pfa.Heuristic;
import com.capstone.indoorpositioning.screens.LevelManager;

/**
 * Created by JackH_000 on 2017-03-22.
 */

public class HeuristicImp implements Heuristic<Node> {

    @Override
    public float estimate(Node node, Node endNode) {
        int startIndex  = node.getIndex();
        int endIndex = endNode.getIndex();

        int startY = startIndex / LevelManager.lvlTileWidth;
        int startX = startIndex % LevelManager.lvlTileWidth;

        int endY = endIndex / LevelManager.lvlTileWidth;
        int endX = endIndex % LevelManager.lvlTileWidth;

        // Manhattan distance for Heuristic calculations since the application
        // does not have diagonal movements (not valid if diagonal movements)
        float distance = Math.abs(startX - endX) + Math.abs(startY - endY);

        return distance;
    }
}
