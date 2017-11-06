package com.capstone.indoorpositioning;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.capstone.indoorpositioning.screens.Play;

public class IndoorPositioning extends Game{

	public static final String TITLE = "Indoor Positioning", VERSION = "0.0.0.0";
	//SpriteBatch batch;
	//Texture img;

	@Override
	public void create () {
		//Gdx.app.log(TITLE, "create()");
		setScreen(new Play());
		//batch = new SpriteBatch();
		//img = new Texture("badlogic.jpg");
	}

	@Override
	public void render () {
		//Gdx.app.log(TITLE, "render()");
		super.render();
		//Gdx.gl.glClearColor(1, 0, 0, 1);
		//Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		//batch.begin();
		//batch.draw(img, 0, 0);
		//batch.end();
	}

	@Override
	public void dispose () {
		//Gdx.app.log(TITLE, "dispose()");
		super.dispose();
		//batch.dispose();
		//img.dispose();
	}

	@Override
	public void resize(int width, int height){
		//Gdx.app.log(TITLE, "resize()");
		super.resize(width, height);
	}

	@Override
	public void pause(){
		//Gdx.app.log(TITLE, "pause()");
		super.pause();
	}

	@Override
	public void resume(){
		//Gdx.app.log(TITLE, "resume()");
		super.resume();
	}
}
