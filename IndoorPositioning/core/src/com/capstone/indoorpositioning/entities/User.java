package com.capstone.indoorpositioning.entities;

import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.math.Vector2;
import com.capstone.indoorpositioning.pathfinding.GraphPathImp;
import com.capstone.indoorpositioning.pathfinding.HeuristicImp;
import com.capstone.indoorpositioning.pathfinding.Node;
import com.capstone.indoorpositioning.receiver.Receiver;
import com.capstone.indoorpositioning.receiver.RecordInput;
import com.capstone.indoorpositioning.screens.LevelManager;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;


/**
 * Created by JackH_000 on 3/9/17.
 */

public class User extends Sprite {

    /** the movement velocity **/
    private Vector2 velocity = new Vector2();
    private float speed = 60 * 2, gravity = 60 * 1.8f;

    private TiledMapTileLayer collisionLayer;

    // Path finding variables
    private IndexedAStarPathFinder<Node> pathFinder;
    public GraphPathImp resultPath = new GraphPathImp();
    private Vector2 destination = new Vector2();
    private boolean updatePath = false;

    /** Schedule Executor for receiver **/
    private ScheduledExecutorService scheduleTaskExecutor = Executors.newScheduledThreadPool(1);
    private Receiver mReceiver;
    private RecordInput mRecorder;
    private Thread RecorderThread;
    private int[] locationData = new int[8];

    private boolean updateLocation = false;

    public User(Sprite sprite, TiledMapTileLayer collisionLayer){
        super(sprite);
        this.collisionLayer = collisionLayer;

        this.mReceiver = new Receiver(this);
        this.mRecorder = new RecordInput(mReceiver);
        RecorderThread = new Thread(mRecorder);
        RecorderThread.start();
//        scheduleTaskExecutor.scheduleWithFixedDelay(mReceiver, 0, 0, TimeUnit.MICROSECONDS);
        scheduleTaskExecutor.scheduleAtFixedRate(mReceiver, 0, 125, TimeUnit.MICROSECONDS);
    }

    private static final String blockedKey = "blocked";

    @Override
    public void draw(Batch batch) {
        update(Gdx.graphics.getDeltaTime());
        super.draw(batch);
    }

    private void update(float delta){
        UserMovement(delta);

        if(updateLocation){
            String locationString = "";
            /** Insert code to manage and use the data we received **/
            for(int i = 0; i < locationData.length; i++){
                locationString = locationString + locationData[i];
            }

            Gdx.app.log("Location", "Data: " + locationString);

            for(int y = 0; y < LevelManager.lvlTileHeight; y++){
                for(int x = 0; x < LevelManager.lvlTileWidth; x++){
                    if(collisionLayer.getCell(x, y).getTile().getProperties().containsKey(locationString)){
                        Gdx.app.log("Cell", "X: " + x + "    Y: " + y );
                        setX(x * LevelManager.tilePixelWidth);
                        setY(y * LevelManager.tilePixelHeight);
                        break;
                    }
                }
            }

            updatePath = true;
            updateLocation = false;
        }

        if(updatePath){
            pathFinder = new IndexedAStarPathFinder<Node>(LevelManager.graph, false);

            Gdx.app.log("start", "X" + getX() + " Y" + getY());
            Gdx.app.log("end", "X" + destination.x + " Y" + destination.y);

            Node startNode = LevelManager.graph.getNodeByXY((int) getX(), (int) getY());
            Node endNode = LevelManager.graph.getNodeByXY((int) destination.x, (int) destination.y);

            resultPath.clear();
            pathFinder.searchNodePath(startNode, endNode, new HeuristicImp(), resultPath);
            Gdx.app.log("Path", "" + resultPath.getCount());

            updatePath = false;
        }
    }

    public void dispose(){

    }

    public void setLocationData(int sample, int index){
        synchronized (this){
            this.locationData[index] = sample;
        }
    }

    public void setUpdateLocation(boolean updateLocation) {
        this.updateLocation = updateLocation;
    }

    private void UserMovement (float delta){
        //apply gravity
        velocity.y -= gravity * delta;

        // clamp velocity
        if(velocity.y > speed )
            velocity.y = speed;
        else if(velocity.y < -speed)
            velocity.y = -speed;

        //save old position
        float oldX = getX(), oldY = getY(), tileWidth = collisionLayer.getTileWidth(), tileHeight = collisionLayer.getTileHeight();
        boolean collisionX = false, collisionY = false;

        setX(getX() + velocity.x * delta);

        collisionLayer.getCell((int) (getX()/tileWidth), (int) (getY()/tileHeight)).getTile().setBlendMode(TiledMapTile.BlendMode.ALPHA);

        if(velocity.x < 0){
            //top left
            collisionX = collisionLayer.getCell((int) (getX()/tileWidth), (int) ((getY() + getHeight())/tileHeight)).
                    getTile().getProperties().containsKey("blocked");

            //middle left
            if(!collisionX)
                collisionX = collisionLayer.getCell((int) (getX()/tileWidth), (int) ((getY() + getHeight()/2)/tileHeight)).
                        getTile().getProperties().containsKey("blocked");

            //bottom left
            if(!collisionX)
                collisionX = collisionLayer.getCell((int) (getX()/tileWidth), (int) (getY()/tileHeight)).
                        getTile().getProperties().containsKey("blocked");

        } else if(velocity.x > 0) {
            //top right
            collisionX = collisionLayer.getCell((int) ((getX() + getWidth())/tileWidth), (int) ((getY() + getHeight())/tileHeight)).
                    getTile().getProperties().containsKey("blocked");

            //middle right
            if(!collisionX)
                collisionX = collisionLayer.getCell((int) ((getX() + getWidth())/tileWidth), (int) ((getY() + getHeight()/2)/tileHeight)).
                        getTile().getProperties().containsKey("blocked");

            //bottom right
            if(!collisionX)
                collisionX = collisionLayer.getCell((int) ((getX() + getWidth())/tileWidth), (int) (getY()/tileHeight)).
                        getTile().getProperties().containsKey("blocked");
        }

        //react to x collision
        if(collisionX){
            setX(oldX);
            velocity.x = 0;
        }

        //move on y
        setY(getY() + velocity.y * delta);

        if(velocity.y < 0){
            //bottom left
            collisionY = collisionLayer.getCell((int)(getX()/tileWidth), (int)(getY()/tileHeight)).
                    getTile().getProperties().containsKey("blocked");

            //bottom middle
            if(!collisionY)
                collisionY = collisionLayer.getCell((int)((getX() + getWidth()/2)/tileWidth), (int)(getY()/tileHeight)).
                        getTile().getProperties().containsKey("blocked");

            //bottom right;
            if(!collisionY)
                collisionY = collisionLayer.getCell((int)((getX() + getWidth())/tileWidth), (int)(getY()/tileHeight)).
                        getTile().getProperties().containsKey("blocked");

        } else if(velocity.y > 0){
            //top left
            collisionY = collisionLayer.getCell((int)(getX()/tileWidth), (int)((getY()+ getHeight())/tileHeight)).
                    getTile().getProperties().containsKey("blocked");
            //top middle
            if(!collisionY)
                collisionY = collisionLayer.getCell((int)((getX() + getWidth()/2)/tileWidth), (int)((getY()+ getHeight())/tileHeight)).
                        getTile().getProperties().containsKey("blocked");
            //top right
            if(!collisionY)
                collisionY = collisionLayer.getCell((int)(getX() + getWidth()/tileWidth), (int)((getY()+ getHeight())/tileHeight)).
                        getTile().getProperties().containsKey("blocked");

        }

        //react to y collision
        if(collisionY) {
            setY(oldY);
            velocity.y = 0;
        }
    }

    private boolean isCellBlocked(float x, float y) {
        Cell cell = collisionLayer.getCell((int) (x / collisionLayer.getTileWidth()), (int) (y / collisionLayer.getTileHeight()));
        return cell != null && cell.getTile() != null && cell.getTile().getProperties().containsKey(blockedKey);
    }

    public Vector2 getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector2 velocity) {
        this.velocity = velocity;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getGravity() {
        return gravity;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public TiledMapTileLayer getCollisionLayer() {
        return collisionLayer;
    }

    public void setCollisionLayer(TiledMapTileLayer collisionLayer) {
        this.collisionLayer = collisionLayer;
    }
    public void setResultPath(GraphPathImp resultPath) {
        this.resultPath = resultPath;
    }

    public void setDestination(int x, int y) {
        this.destination.set(x, y);
    }

    public void setUpdatePath(boolean updatePath) {
        this.updatePath = updatePath;
    }
}
