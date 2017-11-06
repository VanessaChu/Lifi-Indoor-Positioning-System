package com.capstone.indoorpositioning.pathfinding;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.utils.Array;

/**
 * Created by JackH_000 on 2017-03-21.
 */

public class Node {
    private Array<Connection<Node>> connections = new Array<Connection<Node>>();
    public int type;
    public int index;

    public Node() {
        index = Node.Indexer.getIndex();
    }

    public int getIndex(){
        return index;
    }

    public Array<Connection<Node>> getConnections() {
        return connections;
    }

    public void createConnection(Node toNode, float cost){
        connections.add(new ConnectionImp(this, toNode, cost));
    }

    private static class Indexer {
        private static int index = 0;

        public static int getIndex(){
            return index++;
        }
    }

    public static class Type {
        public static final int REGULAR = 1;
    }

}
