package co.aeons.zombie.shooter.entities.bullets;

import co.aeons.zombie.shooter.managers.ResourceManager;

public class BRBullet extends Bullet {

    private int i;
    public BRBullet(float x, float y, int i) {
        super(x, y);
        this.i = i;
        this.bullet = ResourceManager.getBrBullet();
        this.damage = 7;
    }

    @Override
    public void update(float dt) {
        x += dx*dt;
        bulletBounds.setPosition(x, y);
        lifeTimer += dt;
        if(lifeTimer > lifeTime) {
            remove = true;
        }
    }

    @Override
    public void setX(float x) {
        // Space out bullets
        super.setX(x+i*30);
    }

    public String getType(){
        return "br";
    }
}
