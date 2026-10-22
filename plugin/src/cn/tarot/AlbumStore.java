package cn.tarot;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

/** One durable file per player; failed reads never become an empty overwrite. */
final class AlbumStore {
    static final class Data {
        final ItemStack[] cards=new ItemStack[110];
        boolean particles=true;
    }
    private final Path directory;
    AlbumStore(Path directory){this.directory=directory;}
    Data load(UUID id) throws IOException {
        Path file=directory.resolve(id+".yml");Data data=new Data();
        if(!Files.exists(file))return data;
        YamlConfiguration y=new YamlConfiguration();
        try{y.load(file.toFile());}catch(InvalidConfigurationException ex){throw new IOException("Invalid album file",ex);}
        if(y.getInt("format")!=1)throw new IOException("Unsupported album format");
        data.particles=y.getBoolean("particles",true);
        var section=y.getConfigurationSection("cards");
        if(section==null){if(y.contains("cards"))throw new IOException("Invalid cards section");return data;}
        for(String key:section.getKeys(false))try{
            int index=Integer.parseInt(key);if(index<0||index>=110)throw new IllegalArgumentException();
            ItemStack item=ItemStack.deserializeBytes(Base64.getDecoder().decode(section.getString(key,"")));
            if(item.isEmpty()||item.getAmount()!=1)throw new IllegalArgumentException();
            data.cards[index]=item;
        }catch(RuntimeException ex){throw new IOException("Invalid album entry "+key,ex);}
        return data;
    }
    void save(UUID id,Data data) throws IOException {
        Files.createDirectories(directory);
        YamlConfiguration y=new YamlConfiguration();y.set("format",1);y.set("particles",data.particles);
        for(int i=0;i<data.cards.length;i++)if(data.cards[i]!=null)
            y.set("cards."+i,Base64.getEncoder().encodeToString(data.cards[i].serializeAsBytes()));
        Path file=directory.resolve(id+".yml"),tmp=directory.resolve(id+".yml.tmp");
        y.save(tmp.toFile());
        try{Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
        catch(AtomicMoveNotSupportedException ex){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}
    }
}
