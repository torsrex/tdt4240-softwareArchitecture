package co.aeons.zombie.shooter.gamestates;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;

import java.util.ArrayList;
import java.util.Random;

import co.aeons.zombie.shooter.ZombieShooter;
import co.aeons.zombie.shooter.entities.Player;
import co.aeons.zombie.shooter.entities.Wall;
import co.aeons.zombie.shooter.entities.Zombie;
import co.aeons.zombie.shooter.entities.bullets.Bullet;
import co.aeons.zombie.shooter.entities.buttons.CycleDownButton;
import co.aeons.zombie.shooter.entities.buttons.CycleUpButton;
import co.aeons.zombie.shooter.entities.buttons.EffectButton;
import co.aeons.zombie.shooter.entities.buttons.FireButton;
import co.aeons.zombie.shooter.entities.buttons.InstaKill;
import co.aeons.zombie.shooter.entities.buttons.MuteButton;
import co.aeons.zombie.shooter.factories.RandomButtonFactory;
import co.aeons.zombie.shooter.factories.RandomZombieFactory;
import co.aeons.zombie.shooter.managers.GameStateManager;
import co.aeons.zombie.shooter.managers.Jukebox;
import co.aeons.zombie.shooter.managers.ResourceManager;
import co.aeons.zombie.shooter.managers.Save;
import co.aeons.zombie.shooter.utils.FrameRate;

import static co.aeons.zombie.shooter.ZombieShooter.cam;
import static co.aeons.zombie.shooter.ZombieShooter.gamePort;

public class PlayState extends GameState {

    protected SpriteBatch sb;
    protected ShapeRenderer sr;
    protected Stage stage;
    private FrameRate framerate;

    //Makes text easy to use
    private GlyphLayout layout;
    private BitmapFont scoreFont, magazineFont, wallHealthFont;

    //Background texture
    private Texture bg;

    //API
    protected String zombieAPI;
    private int currentZombieFlag;

    protected Player player;
    protected ArrayList<Bullet> bullets;
    protected ArrayList<Zombie> zombies;

    protected Wall wall;
    private long score;

    //Boundaries
    protected Rectangle playerLane;
    private Rectangle fireBounds;
    private Rectangle cycleUpButtonBounds;
    private Rectangle cycleDownButtonBounds;
    private Rectangle muteButtonBounds;

    //Factories
    private RandomButtonFactory buttonFactory;
    private RandomZombieFactory zombieFactory;

    //buttons
    private FireButton fireButton;
    private MuteButton muteButton;
    private CycleUpButton cycleUpButton;
    private CycleDownButton cycleDownButton;
    private EffectButton effectButton;


    //Spawndelay for powerups
    private int spawnDelay;
    private float effectButtonSpawnTimer;
    private float spawnCooldown;
    private int zombieSpawnTimer;

    private int zombieSpawnCount;

    //Duration of an effect
    private int effectTimer;

    //Used to increase damage on bullets
    protected int damageModifier;

    //Used to increase score
    protected int scoreModifier;

    private int level;

    //Flag to check if powerup is used
    private boolean effectButtonIsClicked;


    public PlayState(GameStateManager gsm) {
        super(gsm);
    }

    public void init() {

        sb = new SpriteBatch();
        sr = new ShapeRenderer();
        scoreFont = new BitmapFont();
        wallHealthFont = new BitmapFont();
        magazineFont = new BitmapFont();
        layout = new GlyphLayout();
        bg = ResourceManager.getBg();
        framerate = new FrameRate();

        //sets up camera
        cam.position.set(cam.viewportWidth / 2, cam.viewportHeight / 2, 0);
        cam.update();
        stage = new Stage(gamePort, sb);

        bullets = new ArrayList<>();
        player = new Player(bullets);
        zombies = new ArrayList<>();
        wall = new Wall();

        level = 1;
        zombieSpawnTimer = 100;
        spawnCooldown = 1.0f;
        effectButtonSpawnTimer = 0;

        //spawnZombies();
        currentZombieFlag = 0;
        zombieAPI = currentZombieFlag +"#NONE";
        zombieSpawnCount = 10;

        //Set up variables for powerups
        spawnDelay = randInt(10, 20);
        effectButtonIsClicked = true;
        damageModifier = 1;
        effectTimer = 0;

        //Factory initialization
        buttonFactory = new RandomButtonFactory(cam);
        zombieFactory = new RandomZombieFactory();

        //Create bounds for buttons
        fireBounds = new Rectangle(
                cam.viewportWidth - 200,
                0,
                cam.viewportWidth / 8,
                cam.viewportHeight / 6
        );
        muteButtonBounds = new Rectangle(
                cam.viewportWidth - 50,
                cam.viewportHeight - 25,
                cam.viewportWidth / 16,
                cam.viewportHeight / 16
        );
        playerLane = new Rectangle(
                0,
                0,
                cam.viewportWidth / 3,
                cam.viewportHeight
        );
        cycleUpButtonBounds = new Rectangle(
                cam.viewportWidth - 300,
                0,
                cam.viewportWidth / 16,
                cam.viewportHeight / 12
        );
        cycleDownButtonBounds = new Rectangle(
                cam.viewportWidth - 350,
                0,
                cam.viewportWidth / 16,
                cam.viewportHeight / 12
        );

        //Create buttons with above bounds
        fireButton = new FireButton(fireBounds);
        muteButton = new MuteButton(muteButtonBounds);
        cycleUpButton = new CycleUpButton(cycleUpButtonBounds);
        cycleDownButton = new CycleDownButton(cycleDownButtonBounds);

        //Create empty button
        effectButton = new InstaKill(new Rectangle(0, 0, 0, 0));

        Gdx.input.setInputProcessor(this);
        Gdx.input.setCatchBackKey(true);
    }

    public void update(float dt) {
        checkZombieWallCollision();
        checkZombieBulletCollision();
        updateTimers(dt);
        spawnZombie();
        player.update(dt);
        updatePlayerBullets(dt);
        updateZombies(dt);
        spawnEffectButton();
        updateWallHealth();
        framerate.update();
    }

    protected void spawnEffectButton() {
        if (effectButtonSpawnTimer > spawnDelay && effectButtonIsClicked) {

            effectButton = buttonFactory.produceRandomEffectButton();

            this.stage.addActor(effectButton);

            //Reset variables for next spawning
            effectButtonIsClicked = false;
            spawnDelay = randInt(10, 20);
            effectButtonSpawnTimer = 0;
        }
    }

    protected void spawnZombie() {
        if (zombieSpawnTimer <= 0) {
            zombieSpawnTimer += 10;
            currentZombieFlag++;
            zombieAPI = currentZombieFlag + "#";
            for (int i = 0; i < 5; i++) {
                if (zombieSpawnCount <= 0) {
                    break;
                }
                Zombie currentZombie = zombieFactory.spawnRandomZombie();
                zombies.add(currentZombie);
                zombieAPI +=
                        currentZombie.getType() + ":" +
                                currentZombie.getx() + "," +
                                currentZombie.gety() + "," +
                                currentZombie.getId() + ";";

                zombieSpawnCount--;
            }

            if (zombieSpawnCount <= 0) {
                level += 2;
                zombieSpawnTimer = 500;
                zombieSpawnCount += 10 + level;
            }
        }

    }

    protected void updateTimers(float dt) {
        // Cooldown effectButtonSpawnTimer on effects gained by effect button
        effectTimer -= dt;
        if (effectTimer <= 0) {
            resetEffects();
        }

        // next level
        zombieSpawnTimer -= dt;
        // update spawn powerup button effectButtonSpawnTimer
        if (effectButtonIsClicked) {
            this.effectButtonSpawnTimer += dt;
        }
    }

    protected void updateWallHealth() {
        if (wall.getCurrentWallHealth() <= 0) {
            wall.playSound();
            Jukebox.getIngameMusic().stop();
            Jukebox.playGameoverMusic();
            Save.gd.setTentativeScore(this.getScore());
            gsm.setState(GameStateManager.GAMEOVER);
        }
    }

    //Update and remove zombies
    protected void updateZombies(float dt) {
        for (int i = 0; i < zombies.size(); i++) {
            zombies.get(i).update(dt);
            if (zombies.get(i).shouldRemove()) {
                zombies.remove(i);
                i--;
            }
        }
    }

    protected void updatePlayerBullets(float dt) {
        for (int i = 0; i < bullets.size(); i++) {
            bullets.get(i).update(dt);
            if (bullets.get(i).shouldRemove()) {
                bullets.remove(i);
                i--;
            }
        }
    }

    //Insert new modifierresets here
    private void resetEffects() {
        this.damageModifier = 1;
        this.effectTimer = 0;
        this.scoreModifier = 1;
    }

    protected void checkZombieBulletCollision() {
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            for (int j = 0; j < zombies.size(); j++) {
                Zombie a = zombies.get(j);
                if (a.collide(b)) {
                    bullets.remove(i);
                    i--;
                    a.getHurt(b.getDamage() * damageModifier);
                    if (a.getHealth() <= 0) {
                        a.deathSound();
                        zombies.remove(j);
                        j--;
                        this.incrementScore(a.getScore());
                    }
                    break;
                }
            }
        }
    }

    protected void checkZombieWallCollision() {
        for (int i = 0; i < zombies.size(); i++) {
            Zombie zombie = zombies.get(i);
            if (zombie.collide(wall)) {
                zombie.setStopped(true);
                wall.takeDamage(zombie.attack());
            }
        }
    }

    public void draw() {

        sb.setProjectionMatrix(cam.combined);
        sr.setProjectionMatrix(cam.combined);

//        Disabling blending for background covering entire screen improves performance
        sb.disableBlending();
        sb.begin();
        //draw background
        sb.draw(bg, 0, 0, ZombieShooter.WIDTH, ZombieShooter.HEIGHT);
        sb.end();

//        Re-enable blending to avoid black boxes around rest of sprites
        sb.enableBlending();
        // draw player
        player.draw(sb);

        // draw bullets
        for (int i = 0; i < bullets.size(); i++) {
            bullets.get(i).draw(sb);
        }

        // draw zombies
        for (int i = 0; i < zombies.size(); i++) {
            zombies.get(i).draw(sb);
        }

        // draw wall
        wall.draw(sb);

        //Draw firebutton background rect
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(Color.DARK_GRAY);
        sr.rect(fireBounds.x, fireBounds.y, fireBounds.width, fireBounds.height);
        sr.end();

        // draw buttons
        sb.begin();

//        Various HUDs
//        Score
        String scoreOutput = "Score:";
        this.layout.setText(scoreFont, scoreOutput);
        scoreFont.draw(sb, layout, (this.wall.getx() - layout.width) / 2, cam.viewportHeight - 5);
        float scoreHeight = layout.height;
        scoreOutput = Long.toString(this.getScore());
        this.layout.setText(scoreFont, scoreOutput);
        scoreFont.draw(sb, layout, (this.wall.getx() - layout.width) / 2, cam.viewportHeight - 10 - scoreHeight);
//        Magazine-size
        String magazineOutput = Integer.toString(player.getCurrentWeapon().getRemainingBullets()) + "/" + Integer.toString(player.getCurrentWeapon().getClipSize());
        this.layout.setText(magazineFont, magazineOutput);
        scoreFont.draw(sb, layout, cam.viewportWidth - 250, (fireButton.getY() + fireBounds.getHeight()) / 2);
//        Wall-health
        String wallHealthOutput = String.format("%d",  (long) this.wall.getCurrentWallHealth());
        this.layout.setText(wallHealthFont, wallHealthOutput);
        wallHealthFont.draw(sb, layout, (this.wall.getx() + this.wall.getRectangle().getWidth() + layout.width+10)/2, ZombieShooter.HEIGHT/2 + 15);

        fireButton.draw(sb, 1);
        muteButton.draw(sb, 1);
        effectButton.draw(sb, 1);
        cycleUpButton.draw(sb, 1);
        cycleDownButton.draw(sb, 1);
        sb.end();

        // draw buttons
        this.stage.addActor(fireButton);
        this.stage.addActor(muteButton);
        this.stage.act();
        this.stage.draw();
        framerate.render();
    }

    public void dispose() {
        sb.dispose();
        sr.dispose();
    }

    // helper function to generate random integer
    public static int randInt(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
    }

    //Method called when FireButton pressed
    protected void onFireButtonPressed() {
        player.shoot();
    }

    //Method called when FireButton pressed
    private void onMuteButtonPressed() {
        Jukebox.toggleMuteMusic();
        muteButton.loadTextureRegion();
    }

    private void onEffectButtonPressed() {
        effectButton.effect(this);
        effectButton.playSound();
        effectButton.remove();
        effectButton = new InstaKill(new Rectangle(0, 0, 0, 0));
        effectButtonIsClicked = true;
    }

    protected void onCycleUpPressed() {
        player.nextWeapon();
        reloadFireButtonTexture();
    }

    protected void onCycleDownPressed() {
        player.prevWeapon();
        reloadFireButtonTexture();
    }

    protected void reloadFireButtonTexture() {
        fireButton.setTexturePath(player.getCurrentWeapon().getTexturePath());
        fireButton.loadTextureRegion();
    }

    //Getters and setters
    public ArrayList<Zombie> getZombies() {
        return zombies;
    }


    public void setDamageModifier(int damageModifier) {
        this.damageModifier = damageModifier;
    }

    public void setScoreModifier(int scoreModifier) {
        this.scoreModifier = scoreModifier;
    }

    public void setEffectTimer(int effectTimer) {
        this.effectTimer = effectTimer;
    }

    public Wall getWall() {
        return wall;
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        //Need to have this or buttons won't work
        Vector2 tmpVec2 = new Vector2();
        stage.getViewport().unproject(tmpVec2.set(x, y));


        //Fire button
        if (fireButton.getBounds().contains(tmpVec2.x, tmpVec2.y)) {
            onFireButtonPressed();
        }

        //Mute button
        if (muteButton.getBounds().contains(tmpVec2.x, tmpVec2.y)) {
            onMuteButtonPressed();
        }

        //Instakill button
        if (effectButton.getBounds().contains(tmpVec2.x, tmpVec2.y)) {
            //stage.touchDown(x, y, pointer, button);
            onEffectButtonPressed();
        }

        if (cycleUpButton.getBounds().contains(tmpVec2.x, tmpVec2.y)) {
            onCycleUpPressed();
        }

        if (cycleDownButton.getBounds().contains(tmpVec2.x, tmpVec2.y)) {
            onCycleDownPressed();
        }

        return true;
    }

    @Override
    public boolean touchDragged(int x, int y, int pointer) {
        Vector2 tmpVec2 = new Vector2();
        stage.getViewport().unproject(tmpVec2.set(x, y));

        if (playerLane.contains(tmpVec2.x, tmpVec2.y)) {
            //player.setTransform(new Vector2(player.getUserData().getRunningPosition().x, tmpVec2.y / B2DConstants.PPM), 0);
            player.setPosition(player.getx(), tmpVec2.y);
        }
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }

    public long getScore() {
        return score;
    }

    public void incrementScore(long score) {
        this.score += score * scoreModifier;
    }

    public void setScore(long score) {
        this.score = score;
    }
}
