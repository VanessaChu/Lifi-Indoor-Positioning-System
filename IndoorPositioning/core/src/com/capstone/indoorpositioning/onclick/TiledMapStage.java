package com.capstone.indoorpositioning.onclick;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.capstone.indoorpositioning.entities.User;

/**
 * Created by JackH_000 on 2017-03-23.
 */

public class TiledMapStage extends Stage {

    private TiledMap tiledMap;
    private User user;


    public TiledMapStage(TiledMap tiledMap, User user) {
        this.tiledMap = tiledMap;
        this.user = user;

        TiledMapTileLayer tiledLayer = (TiledMapTileLayer) tiledMap.getLayers().get(0);
        createActorsForLayer(tiledLayer);
    }

    private void createActorsForLayer(TiledMapTileLayer tiledLayer){
        for(int x = 0; x < tiledLayer.getWidth(); x++){
            for(int y = 0; y < tiledLayer.getHeight(); y++){

                TiledMapTileLayer.Cell cell = tiledLayer.getCell(x, y);
                TiledMapActor actor = new TiledMapActor(tiledMap, tiledLayer, cell);
                actor.setBounds(x * tiledLayer.getTileWidth(), y * tiledLayer.getTileHeight(),
                        tiledLayer.getTileWidth(), tiledLayer.getTileHeight());
                addActor(actor);
                EventListener eventListener = new TiledMapClickListener(actor, user);
                actor.addListener(eventListener);
            }
        }
    }
}
