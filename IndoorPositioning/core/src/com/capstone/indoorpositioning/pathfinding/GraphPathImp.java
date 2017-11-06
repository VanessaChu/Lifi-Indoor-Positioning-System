package com.capstone.indoorpositioning.pathfinding;

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.utils.Array;

import java.util.Iterator;

/**
 * Created by JackH_000 on 2017-03-21.
 */

public class GraphPathImp implements GraphPath<Node> {
    private Array<Node> nodes = new Array<Node>();

    public GraphPathImp(){

    }

    @Override
    public int getCount() {
        return nodes.size;
    }

    @Override
    public Node get(int index) {
        return nodes.get(index);
    }

    @Override
    public void add(Node node) {
        nodes.add(node);
    }

    @Override
    public void clear() {
        nodes.clear();
    }

    @Override
    public void reverse() {
        nodes.reverse();
    }

    @Override
    public Iterator<Node> iterator() {
        return nodes.iterator();
    }
}
