package com.ugen.piano;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by WilsonCS30 on 3/21/2017.
 */

public class WorldRenderer {
    private SpriteBatch batch;
    private ShapeRenderer renderer;
    private long initTimeD, initTimeB, initHit, bounceTime;
    private Random rand;

    private BadGuyPool badGuyPool;
    private Array<BadGuyPool.PooledBadGuy> badGuys;

    private RangedBadGuyPool rangedBadGuyPool;
    private Array<RangedBadGuyPool.PooledRangedBadGuy> rangedBadGuys;

    private SpinningBadGuyPool sbgPool;
    private Array<SpinningBadGuyPool.PooledSpinningBadGuy> spinningBadGuys;

    private HexBadGuyPool hexBadGuyPool;
    private Array<HexBadGuyPool.PooledHexBadGuy> hexBadGuys;

    private ParticleSystemPool systemPool;
    private Array<ParticleSystemPool.PooledSystem> systems;

    private ParticlePool particlePool;
    private Array<ParticlePool.PooledParticle> pooledParticles;

    private float width, height, x1, y1, x2, y2;
    private OrthographicCamera cam;
    private GameWorld world;
    private Dude dude;
    private Touchpad touchPadR, touchPadL;
    private Stage stage;
    private int score, totalParticles;
    private BitmapFont font;
    private ArrayList<Rectangle> healthBlocks;
    private Rectangle boundingBox;
    private ArrayList<Hexagon> hexagons;
    private Vector2 bouncePos;
    private int bounceCase;

    public WorldRenderer(GameWorld world){
        this.world = world;

        initTimeD = initTimeB = System.currentTimeMillis();

        rand = new Random();

        Sprite particleSprite = new Sprite(new Texture("particle.png"));

        BadGuy bg = new BadGuy(new Vector2(0, 0));
        RangedBadGuy rbg = new RangedBadGuy(new Vector2(0, 0));
        SpinningBadGuy sbg = new SpinningBadGuy(new Vector2(0, 0));
        HexagonBadGuy hbg = new HexagonBadGuy(new Vector2(0, 0));
        Particle p = new Particle(particleSprite, false);

        badGuyPool = new BadGuyPool(bg, 10, 100);
        badGuys = new Array<BadGuyPool.PooledBadGuy>();
        rangedBadGuyPool = new RangedBadGuyPool(rbg, 10, 100);
        rangedBadGuys = new Array<RangedBadGuyPool.PooledRangedBadGuy>();
        sbgPool = new SpinningBadGuyPool(sbg, 10, 100);
        spinningBadGuys = new Array<SpinningBadGuyPool.PooledSpinningBadGuy>();
        hexBadGuyPool = new HexBadGuyPool(hbg, 10, 100);
        hexBadGuys = new Array<HexBadGuyPool.PooledHexBadGuy>();
        particlePool = new ParticlePool(p, 100, 1000);
        pooledParticles = new Array<ParticlePool.PooledParticle>();

        batch = new SpriteBatch();
        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);


        cam = new OrthographicCamera(1.0f, (float) Gdx.graphics.getHeight() / (float)Gdx.graphics.getWidth());
        Viewport viewport = new ExtendViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), cam);
        viewport.apply();
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        width = cam.viewportWidth;
        height = cam.viewportHeight;
        boundingBox = new Rectangle(0, 0, width, height);

        dude = new Dude(new Vector2(width/2, height/2));

        ParticleSystem ps = new ParticleSystem(new Vector2(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2),
                boundingBox, 100);

        systemPool = new ParticleSystemPool(ps, 10, 100);
        systems = new Array<ParticleSystemPool.PooledSystem>();

        Skin touchpadSkin = new Skin();
        touchpadSkin.add("touchBackground", AssetManager.getJoystickBackground());
        touchpadSkin.add("touchForeground", AssetManager.getJoystickForeground());

        Touchpad.TouchpadStyle touchpadStyle = new Touchpad.TouchpadStyle();

        Drawable touchpadBack = touchpadSkin.getDrawable("touchBackground");
        Drawable touchpadFront = touchpadSkin.getDrawable("touchForeground");

        touchpadStyle.background = touchpadBack;
        touchpadStyle.knob = touchpadFront;

        touchPadL = new Touchpad(0, touchpadStyle);
        touchPadL.setBounds(100, height / 2 - 100, 200, 200);

        touchPadR = new Touchpad(0, touchpadStyle);
        touchPadR.setBounds(width - 300, height / 2 - 100, 200, 200);

        stage = new Stage(viewport, batch);
        stage.addActor(touchPadL);
        stage.addActor(touchPadR);
        Gdx.input.setInputProcessor(stage);

        initHit = 0;
        score = 0;

        font = new BitmapFont();
        font.getData().setScale(10);

        healthBlocks = new ArrayList<Rectangle>();
        int rectNum = dude.getHealth() / 10;

        for(int i = 0; i < rectNum; i++){
            healthBlocks.add(new Rectangle((4 * i + 1) * width / 123, height - 50, width / 41, width / 41));
        }

        hexagons = new ArrayList<Hexagon>();

        int hexLength = 200;

        for(int i = 0; i < 10; i++){
            for(int j = 0; j < 10; j++){
                if(i % 2 == 0)
                    hexagons.add(new Hexagon(((2-(float)Math.cos(Math.PI/3))*i*hexLength),
                            (float)Math.sqrt(3)*j*hexLength, hexLength));
                else
                    hexagons.add(new Hexagon((2-(float)Math.cos(Math.PI/3))*i*hexLength,
                            (float)Math.sqrt(3)*j*hexLength - (float)Math.sqrt(3)*hexLength/2, hexLength));
            }
        }
    }

    public void render(float delta){
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.setColor(new Color(0, 0, 1, 1));
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setProjectionMatrix(cam.combined);

        if(System.currentTimeMillis() - initTimeD > 1000){
            initTimeD = System.currentTimeMillis();

            float tempF = rand.nextFloat();


            if(tempF < .25f) {
                badGuys.add(badGuyPool.obtain());
                badGuys.get(badGuys.size - 1).setPosition(new Vector2(rand.nextFloat() * width + boundingBox.getX(),
                        rand.nextFloat() * height + boundingBox.getY()));
            }

            else if(tempF > .25f && tempF < .50f){
                rangedBadGuys.add(rangedBadGuyPool.obtain());
                rangedBadGuys.get(rangedBadGuys.size - 1).setPosition(new Vector2(rand.nextFloat() * width + boundingBox.getX(),
                        rand.nextFloat() * height + boundingBox.getY()));

            }

            else if(tempF > .50f && tempF < .75f){
                spinningBadGuys.add(sbgPool.obtain());
                spinningBadGuys.get(spinningBadGuys.size - 1).setPosition(new Vector2(rand.nextFloat() * width + boundingBox.getX(),
                        rand.nextFloat() * height + boundingBox.getY()));
            }

            else{
                hexBadGuys.add(hexBadGuyPool.obtain());
                hexBadGuys.get(hexBadGuys.size - 1).setPosition(new Vector2(rand.nextFloat() * width + boundingBox.getX(),
                        rand.nextFloat() * height + boundingBox.getY()));
            }
        }

        if(System.currentTimeMillis() - initTimeB > 300 && Math.abs(touchPadR.getKnobPercentX() + touchPadR.getKnobPercentY()) > 0){
            initTimeB = System.currentTimeMillis();
            pooledParticles.add(particlePool.obtain());
            dude.shoot(new Vector2(dude.getPosition().x + touchPadR.getKnobPercentX(), dude.getPosition().y + touchPadR.getKnobPercentY()),
                   pooledParticles.get(pooledParticles.size - 1));
        }

        totalParticles = 0;

        renderer.setColor(0.0f, 0.0f, 1.0f, 1.0f);

        x1 = dude.getPosition().x;
        y1 = dude.getPosition().y;

        dude.setAcceleration(new Vector2(touchPadL.getKnobPercentX(), touchPadL.getKnobPercentY()));
        //dude.setVelocity(new Vector2(touchPadL.getKnobPercentX() * 10, touchPadL.getKnobPercentY() * 10));
        dude.update();

        if(dude.getPosition().x < hexagons.get(0).getX()){
            bounceCase = 0;
            bounceTime = System.currentTimeMillis();
            bouncePos = new Vector2(dude.getPosition().x, dude.getPosition().y);
            dude.setVelocity(new Vector2(5, dude.getVelocity().y));
            dude.setAcceleration(new Vector2(0, 0));
            dude.update();
        }

        if(dude.getPosition().x > hexagons.get(91).getX()){
            bounceCase = 0;
            bounceTime = System.currentTimeMillis();
            bouncePos = new Vector2(dude.getPosition().x, dude.getPosition().y);
            dude.setVelocity(new Vector2(-5, dude.getVelocity().y));
            dude.setAcceleration(new Vector2(0, 0));
            dude.update();
        }

        if(dude.getPosition().y < hexagons.get(0).getY()){
            bounceCase = 1;
            bounceTime = System.currentTimeMillis();
            bouncePos = new Vector2(dude.getPosition().x, dude.getPosition().y);
            dude.setVelocity(new Vector2(dude.getVelocity().x, 5));
            dude.setAcceleration(new Vector2(0, 0));
            dude.update();
        }

        if(dude.getPosition().y > hexagons.get(89).getY()){
            bounceCase = 1;
            bounceTime = System.currentTimeMillis();
            bouncePos = new Vector2(dude.getPosition().x, dude.getPosition().y);
            dude.setVelocity(new Vector2(dude.getVelocity().x, -5));
            dude.setAcceleration(new Vector2(0, 0));
            dude.update();
        }

        if(System.currentTimeMillis() - bounceTime < 1000){
            renderer.setColor(new Color(0.2f, 0.2f, 0.5f, 1.0f - (float) (System.currentTimeMillis() - bounceTime) / 1000));

            if(bounceCase == 0) {
                //Gdx.app.log("DEBUG", "BOUNCE: " + (1.0f - (float) (System.currentTimeMillis() - bounceTime) / 1000));
                renderer.line(bouncePos.x, bouncePos.y + 150.0f - 150.0f * ((float) (System.currentTimeMillis() - bounceTime) / 1000),
                        bouncePos.x, bouncePos.y - 150.0f + 150.0f * ((float) (System.currentTimeMillis() - bounceTime) / 1000));
            }
            else if(bounceCase == 1){
                renderer.line(bouncePos.x + 150.0f - 150.0f * ((float) (System.currentTimeMillis() - bounceTime) / 1000), bouncePos.y,
                        bouncePos.x - 150.0f + 150.0f * ((float) (System.currentTimeMillis() - bounceTime) / 1000), bouncePos.y);

            }
        }

        dude.draw(renderer, batch, false);

        x2 = dude.getPosition().x;
        y2 = dude.getPosition().y;

        scroll(x2-x1, y2-y1);

        for(BadGuy b : badGuys){
            b.update(new Vector2(dude.getPosition().x - b.getHitbox().width/2, dude.getPosition().y - b.getHitbox().height/2));
            b.draw(renderer);
            if(System.currentTimeMillis() - initHit > dude.getDamageTimer()) {
                if (dude.intersects(b.getHitbox())) {
                    Gdx.app.log("DEBUG", "OW");
                    dude.setHealth(dude.getHealth() - 5);
                    initHit = System.currentTimeMillis();
                }
            }
        }

        for(RangedBadGuy b : rangedBadGuys){
            b.update(new Vector2(dude.getPosition().x - b.getHitbox().width/2,
                    dude.getPosition().y - b.getHitbox().height/2), batch);
            b.draw(renderer);

            if(System.currentTimeMillis() - b.getLastShot() > 1000/b.getFireRate()){
                pooledParticles.add(particlePool.obtain());
                b.shoot(dude.getPosition(), pooledParticles.get(pooledParticles.size - 1));
            }

            if(System.currentTimeMillis() - initHit > dude.getDamageTimer()) {
                if (dude.intersects(b.getHitbox())) {
                    Gdx.app.log("DEBUG", "OW");
                    dude.setHealth(dude.getHealth() - 5);
                    initHit = System.currentTimeMillis();
                }
            }
        }

        for(int i = spinningBadGuys.size - 1; i >= 0; i--){
            SpinningBadGuyPool.PooledSpinningBadGuy b = spinningBadGuys.get(i);
            b.update(new Vector2(dude.getPosition().x, dude.getPosition().y));
            b.draw(renderer);
            if(System.currentTimeMillis() - initHit > dude.getDamageTimer()) {
                if (dude.intersects(b.getHitBox())) {
                    Gdx.app.log("DEBUG", "OW");
                    dude.setHealth(dude.getHealth() - 20);
                    initHit = System.currentTimeMillis();
                    spinningBadGuys.removeIndex(i);
                    b.free();
                    ParticleSystemPool.PooledSystem temp = systemPool.obtain();
                    temp.setBoundary(boundingBox);
                    temp.setPosition(new Vector2(b.getX(), b.getY()));
                    systems.add(temp);
                }
            }
        }

        for(HexagonBadGuy b : hexBadGuys){
            b.update(new Vector2(dude.getPosition().x - b.getHitbox().width/2, dude.getPosition().y - b.getHitbox().height/2));
            b.draw(renderer);
            if(System.currentTimeMillis() - initHit > dude.getDamageTimer()) {
                if (dude.intersects(b.getHitbox())) {
                    Gdx.app.log("DEBUG", "OW");
                    dude.setHealth(dude.getHealth() - 5);
                    initHit = System.currentTimeMillis();
                }
            }
        }

        if(dude.getHealth() < healthBlocks.size()*10){
            healthBlocks.remove(healthBlocks.size()-1);
        }

        for(int i = pooledParticles.size - 1; i >= 0; i--){
            ParticlePool.PooledParticle p = pooledParticles.get(i);

            p.update();
            p.draw(batch);

            if(p.getX() < boundingBox.getX() || p.getX() > boundingBox.getX() + width
                    || p.getY() < boundingBox.getY() || p.getY() > boundingBox.getY() + height){
                p.free();
                pooledParticles.removeIndex(i);
            }
        }

        for(int i = systems.size - 1; i >= 0; i--){
            ParticleSystemPool.PooledSystem system = systems.get(i);
            system.setBoundary(boundingBox);
            system.draw(batch, delta);

            totalParticles += system.getActiveParticles();

            if(system.isComplete()){
                system.free();
                systems.removeIndex(i);
            }
        }

        font.setColor(Color.WHITE);
        font.draw(batch, "SCORE: " + score, 0,0);
        //font.getData().setScale(5.0f);
        drawBackground();

        renderer.end();
        batch.end();

        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();

        checkBulletCollisions();

        while(dude.isDead()){

        }

        //log();
    }

    public void log(){
        Gdx.app.log("DEBUG", "FPS: " + Gdx.graphics.getFramesPerSecond() +  " , FREE: " + systemPool.getFree()
                + " , IN USE: " + systems.size + " , MAX: " + systemPool.getMax() + " , TOTAL PARTICLES: " + totalParticles);
        Gdx.app.log("DEBUG", "BOUNDINGX: " + boundingBox.getX() + " , BOUNDINGY: " + boundingBox.getY()
                + " , MAXX: " + boundingBox.getX() + boundingBox.getWidth() + " , MAXY: " + boundingBox.getY() + boundingBox.getHeight());
    }

    private void drawBackground(){
        for(Hexagon hex : hexagons){
            hex.draw(renderer, new Color(0.3f, 0.2f, 0.7f, 0.7f));
        }
    }

    private void scroll(float dx, float dy){
        drawHealthBar(dx, dy);
        cam.translate(dx, dy);
        touchPadL.moveBy(dx, dy);
        touchPadR.moveBy(dx, dy);
        boundingBox.setPosition(boundingBox.getX() + dx, boundingBox.getY() + dy);
    }

    private void drawHealthBar(float x, float y){
        for(Rectangle r : healthBlocks){
            r.setPosition(r.getX() + x, r.getY() + y);
            renderer.rect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }
    }

    private void killBadGuy(int ii, ParticlePool.PooledParticle p){
        ParticleSystemPool.PooledSystem temp = systemPool.obtain();
        temp.setBoundary(boundingBox);
        temp.setPosition(new Vector2(p.getX(), p.getY()));
        systems.add(temp);

        p.free();
        pooledParticles.removeIndex(ii);

        score += 420;
    }

    private void checkBulletCollisions(){
        boolean hit = false;

        for(int ii = pooledParticles.size - 1; ii >= 0; ii--){
            ParticlePool.PooledParticle p = pooledParticles.get(ii);

            if(p.getFaction().equals("bad")){
                if(dude.intersects(p.getBoundingRectangle()))   {
                    Gdx.app.log("DEBUG", "OW");
                    dude.setHealth(dude.getHealth() - 5);
                    p.free();
                    pooledParticles.removeIndex(ii);
                }
            }

            else if(p.getFaction().equals("good")){
                for (int i = badGuys.size - 1; i >= 0; i--) {
                    BadGuyPool.PooledBadGuy tempbg = badGuys.get(i);

                    if (p.intersects(badGuys.get(i).getHitbox())) {
                        badGuys.removeIndex(i);
                        tempbg.free();
                        killBadGuy(ii,  p);
                        hit = true;
                        break;
                    }
                }

                if(hit){
                    continue;
                }

                for (int i = rangedBadGuys.size - 1; i >= 0; i--) {
                    RangedBadGuyPool.PooledRangedBadGuy tempbg = rangedBadGuys.get(i);

                    if (p.intersects(rangedBadGuys.get(i).getHitbox())) {
                        rangedBadGuys.removeIndex(i);
                        tempbg.free();
                        killBadGuy(ii,  p);
                        hit = true;
                        break;
                    }
                }

                if(hit){
                    continue;
                }

                for(int i = spinningBadGuys.size - 1; i >= 0; i--){
                    SpinningBadGuyPool.PooledSpinningBadGuy tempbg = spinningBadGuys.get(i);

                    if (p.intersects(spinningBadGuys.get(i).getHitBox())) {
                        spinningBadGuys.removeIndex(i);
                        tempbg.free();
                        killBadGuy(ii,  p);
                        hit = true;
                        break;
                    }
                }

                if(hit) {
                    continue;
                }

                for(int i = hexBadGuys.size - 1; i >= 0; i--){
                    HexBadGuyPool.PooledHexBadGuy tempbg = hexBadGuys.get(i);

                    if (p.intersects(hexBadGuys.get(i).getHitBox())) {
                        tempbg.free();

                        Array<SpinningBadGuyPool.PooledSpinningBadGuy> tempA = hexBadGuys.get(i).explode(sbgPool);
                        UgenUtils.concatArrays(tempA.toArray(), spinningBadGuys.toArray());
                        for(int j = 0; j < 6; j++) {
                            spinningBadGuys.add(tempA.get(j));
                        }

                        hexBadGuys.removeIndex(i);
                        ParticleSystemPool.PooledSystem temp = systemPool.obtain();
                        temp.setPosition(new Vector2(p.getX(), p.getY()));
                        systems.add(temp);

                        p.free();
                        pooledParticles.removeIndex(ii);
                        score += 420;

                        break;
                    }
                }
            }
            hit = false;
        }
    }

    public OrthographicCamera getCam(){
        return cam;
    }

    public float getWidth(){
        return width;
    }

    public float getHeight(){
        return height;
    }

    public Dude getDude(){
        return dude;
    }
}
