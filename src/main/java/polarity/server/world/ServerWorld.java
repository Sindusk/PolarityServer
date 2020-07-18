package polarity.server.world;

import polarity.shared.entity.Entity;
import polarity.shared.entity.Projectile;
import polarity.shared.events.ProjectileEvent;
import polarity.shared.world.GameWorld;

public class ServerWorld extends GameWorld {
    public ServerWorld(int seed) {
        super(seed);
    }
    public Projectile addProjectile(ProjectileEvent attack) {
        Projectile p = new Projectile(node, attack);        // Creates the projectile class data
        p.create(0.25f, attack.getStart(), attack.getTarget());    // Creates the projectile entity
        entities.add(p);    // Adds to the list of entities in the world
        return p;
    }
    public void destroyProjectile(int hashCode) {
        Projectile p;
        for (Entity e : entities) {
            if (e instanceof Projectile) {
                p = (Projectile) e;
                if (p.getEvent().getHashCode() == hashCode) {
                    p.destroy();
                    return;
                }
            }
        }
    }
}
