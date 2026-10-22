package cn.tarot;

import java.util.List;
import org.bukkit.*;
import org.bukkit.entity.Player;

/** Exact-position, short-lived points overlap between frames instead of flashing in bursts. */
final class ParticleLines {
    // Frames arrive every two ticks: one tick of overlap prevents blinking without
    // accumulating the three full layers produced by the former six-tick lifetime.
    static final int LIFETIME=3;
    private ParticleLines(){}
    static Location position(Location feet,double poseOffset,AlbumAura.Point point){
        double yaw=Math.toRadians(feet.getYaw()),cos=Math.cos(yaw),sin=Math.sin(yaw);
        return feet.clone().add(-cos*point.right()+sin*point.back(),
            point.up()+poseOffset,-sin*point.right()-cos*point.back());
    }
    static void draw(Player owner,List<AlbumAura.Point> points,boolean followHeight){
        if(points.isEmpty())return;
        Location feet=owner.getLocation();
        List<Player> viewers=owner.getWorld().getPlayers().stream()
            .filter(p->p.canSee(owner)&&p.getLocation().distanceSquared(feet)<=32*32).toList();
        if(viewers.isEmpty())return;
        double poseOffset=followHeight?owner.getHeight()-1.8:0;
        for(AlbumAura.Point point:points){
            Location at=position(feet,poseOffset,point);
            Particle.Trail trail=new Particle.Trail(at.clone(),Color.fromRGB(point.color()),LIFETIME);
            for(Player viewer:viewers)viewer.spawnParticle(Particle.TRAIL,at,1,0,0,0,0,trail);
        }
    }
}
