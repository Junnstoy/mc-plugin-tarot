package cn.tarot;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Small, pre-sampled versions of the 22 existing tarot artworks, packaged inside the plugin. */
final class CardParticleArt {
    private static final List<List<AlbumAura.Point>> CARDS=load();
    private static List<List<AlbumAura.Point>> load(){
        List<List<AlbumAura.Point>> cards=new ArrayList<>();for(int id=0;id<22;id++)cards.add(new ArrayList<>());
        try(InputStream stream=CardParticleArt.class.getResourceAsStream("/card-particles.csv")){
            if(stream==null)throw new IOException("Missing card-particles.csv");
            for(String line:new String(stream.readAllBytes(),StandardCharsets.UTF_8).split("\\R")){
                if(line.isBlank()||line.startsWith("#"))continue;
                String[] row=line.split(",");int id=Integer.parseInt(row[0]);
                cards.get(id-1).add(new AlbumAura.Point(Double.parseDouble(row[1]),Double.parseDouble(row[2]),1.05,Integer.parseInt(row[3],16),.55f));
            }
            for(List<AlbumAura.Point> card:cards)if(card.isEmpty()||card.size()>TarotCast.MAX_CARD_POINTS)throw new IOException("Invalid particle artwork size");
            return cards.stream().map(List::copyOf).toList();
        }catch(IOException|RuntimeException ex){throw new IllegalStateException("Cannot load tarot particle artwork",ex);}
    }
    static List<AlbumAura.Point> get(int id){return CARDS.get(id-1);}
}
