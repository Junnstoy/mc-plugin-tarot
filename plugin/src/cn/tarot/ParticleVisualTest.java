package cn.tarot;

import java.util.*;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;

final class ParticleVisualTest {
    static void run(TarotPlugin plugin){
        Set<List<AlbumAura.Point>> distinct=new HashSet<>();
        for(int id=1;id<=22;id++){
            var art=CardParticleArt.get(id);check(art.size()==179,"complete card artwork");distinct.add(art);
            var frame=TarotCast.frame(5,1,art);check(frame.size()==283,"five star scene includes sparse circle, wings and complete artwork");
            for(var point:frame)check(Double.isFinite(point.right()+point.up()+point.back()),"finite geometry");
        }
        check(distinct.size()==22,"all 22 artworks are different");
        Location feet=new Location(Bukkit.getWorlds().getFirst(),12,80,24,0,0);
        var point=new AlbumAura.Point(1,2.1,.3,0xff0000,.5f);
        Location at=ParticleLines.position(feet,0,point);
        check(Math.abs(at.getX()-11)<1e-9&&Math.abs(at.getY()-82.1)<1e-9&&Math.abs(at.getZ()-23.7)<1e-9,"local axes and head height");
        check(feet.getX()==12&&feet.getY()==80&&feet.getZ()==24,"render never mutates player location");
        Particle.Trail trail=new Particle.Trail(at,Color.RED,ParticleLines.LIFETIME);
        check(trail.getDuration()==3&&trail.getTarget().equals(at),"stationary particles have just one tick of overlap at two-tick refresh");
        try{
            Class<?> craft=Class.forName("org.bukkit.craftbukkit.CraftParticle");
            var method=craft.getMethod("createParticleParam",Particle.class,Object.class);
            check(method.invoke(null,Particle.TRAIL,trail)!=null,"Paper serializes trail particle data");
        }catch(ReflectiveOperationException ex){throw new IllegalStateException("Cannot validate Paper particle data",ex);}
        ItemStack old=plugin.makeCard(22,5);old.unsetData(DataComponentTypes.CONSUMABLE);
        ItemStack migrated=plugin.migrateItem(old);
        check(migrated!=old&&ChargeController.isConfigured(migrated)&&plugin.migrateItem(migrated)==migrated,"old cards gain hold component idempotently");
        check(ChargeController.isConfigured(ItemStack.deserializeBytes(migrated.serializeAsBytes())),"native hold survives storage serialization");
        plugin.getLogger().info("TAROT PARTICLE SELFTEST PASS: 22 distinct card artworks, five-star scene, coordinate transform, Paper TRAIL serialization and old-card hold migration");
    }
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException("Particle selftest: "+message);}
}
