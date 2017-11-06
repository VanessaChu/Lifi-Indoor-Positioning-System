package com.capstone.indoorpositioning.onclick;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.capstone.indoorpositioning.entities.User;

/**
 * Created by JackH_000 on 2017-03-23.
 */

public class TiledMapClickListener extends ClickListener {
    private TiledMapActor actor;
    private User user;

//    private IndexedAStarPathFinder<Node> pathFinder;
//    private GraphPathImp resultPath = new GraphPathImp();

    public TiledMapClickListener (TiledMapActor actor, User user) {
        this.actor = actor;
        this.user = user;
    }

    @Override
    public void clicked(InputEvent event, float x, float y) {

        user.setDestination((int) actor.getX(), (int) actor.getY());
        user.setUpdatePath(true);

//        pathFinder = new IndexedAStarPathFinder<Node>(LevelManager.graph, false);
//
//        int startX = (int) user.getX();
//        int startY = (int) user.getY();
//
//        int endX = (int) actor.getX();
//        int endY = (int) actor.getY();
//
//      //  Gdx.app.log("tile", "width" + LevelManager.tilePixelWidth + " height" + LevelManager.tilePixelHeight);
//
//        Gdx.app.log("start", "X" + startX + " Y" + startY);
//        Gdx.app.log("end", "X" + endX + " Y" + endY);
//
//        Node startNode = LevelManager.graph.getNodeByXY(startX, startY);
//        Node endNode = LevelManager.graph.getNodeByXY(endX, endY);
//
//        Gdx.app.log("start", "" + startNode.getIndex());
//        Gdx.app.log("end", "" + endNode.getIndex());
//
//        resultPath.clear();
//        pathFinder.searchNodePath(startNode, endNode, new HeuristicImp(), resultPath);
//        Gdx.app.log("Path", "" + resultPath.getCount());


    }
}
