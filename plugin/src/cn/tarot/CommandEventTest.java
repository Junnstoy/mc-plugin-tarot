package cn.tarot;

import java.io.File;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.util.*;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;

/** Tests real command dispatch and real Paper item/attribute objects without connected clients. */
final class CommandEventTest {
    private static int checks;
    @SuppressWarnings("unchecked")
    static void run(TarotPlugin plugin){
        checks=0;
        Map<UUID,Core.Active> active=null,oldActive=null;Map<UUID,Long> locks=null,oldLocks=null;
        File previous=null,temp=null;
        try{
            Field state=field("stateFile");previous=(File)state.get(plugin);
            temp=Files.createTempFile(plugin.getDataFolder().toPath(),"command-test-",".yml").toFile();state.set(plugin,temp);
            active=(Map<UUID,Core.Active>)field("active").get(plugin);oldActive=new HashMap<>(active);
            locks=(Map<UUID,Long>)field("cooldowns").get(plugin);oldLocks=new HashMap<>(locks);
            Fixture f=new Fixture();
            command(plugin,f,"give Tester card all 3 1");
            check(items(f)==22,"all gives 22 cards");
            Set<Integer> ids=new HashSet<>();
            for(ItemStack item:f.storage.getContents())if(item!=null){ids.add(plugin.cardId(item));check(plugin.cardLevel(item)==3&&item.getAmount()==1,"all level and quantity");}
            check(ids.size()==22&&!ids.contains(0),"all kinds distinct");
            f.storage.clear();command(plugin,f,"give Tester card 世界 5 2");
            check(items(f)==2&&plugin.cardId(f.storage.getItem(0))==22&&plugin.cardLevel(f.storage.getItem(1))==5,"Chinese kind and count");
            f.storage.clear();command(plugin,f,"give Tester card all 1 2");check(items(f)==0,"44 cards rejected atomically");
            for(int i=0;i<15;i++)f.storage.setItem(i,new ItemStack(Material.STONE,64));
            ItemStack[] before=f.storage.getContents();command(plugin,f,"give Tester card all 1 1");
            check(Arrays.equals(before,f.storage.getContents()),"21 free slots leaves original inventory unchanged");
            f.storage.clear();command(plugin,f,"give Tester pack 8");check(f.storage.getItem(0).getAmount()==8,"package quantity");
            before=f.storage.getContents();
            for(String bad:List.of("give Tester card all 1 0","give Tester card all 6 1","give Tester pack 1 extra","give Tester album 2","card Tester 1 1","clear Tester"))command(plugin,f,bad);
            check(Arrays.equals(before,f.storage.getContents()),"invalid and retired commands cannot mutate inventory");
            f.admin=false;command(plugin,f,"give Tester card all 1 1");check(Arrays.equals(before,f.storage.getContents()),"permission guard");
            f.messages.clear();command(plugin,f,"help");check(f.messages.stream().noneMatch(s->s.contains("/tarot give")),"player help hides admin commands");
            f.admin=true;f.messages.clear();command(plugin,f,"help");check(f.messages.stream().anyMatch(s->s.contains("reset buffer")),"admin help new syntax");
            plugin.executeCommand(f.player,new String[]{"give","Missing","card","all","1","1"},name->null);
            check(Arrays.equals(before,f.storage.getContents()),"offline target gives nothing");

            long now=System.currentTimeMillis(),unlock=now+1800000;
            Core.Active original=new Core.Active(4,3,now,now+86400000,unlock);
            active.put(f.uuid,original);locks.put(f.uuid,unlock);
            AttributeModifier own=new AttributeModifier(new NamespacedKey("tarotrainbow","max_health"),6,AttributeModifier.Operation.ADD_NUMBER);
            AttributeModifier album=new AttributeModifier(new NamespacedKey("tarot_album","max_health"),2,AttributeModifier.Operation.ADD_NUMBER);
            AttributeModifier other=new AttributeModifier(new NamespacedKey("other_plugin","health"),1,AttributeModifier.Operation.ADD_NUMBER);
            f.modifiers.addAll(List.of(own,album,other));
            f.admin=false;command(plugin,f,"reset buffer Tester");check(active.get(f.uuid).equals(original)&&locks.get(f.uuid)==unlock,"reset permission guard");f.admin=true;
            command(plugin,f,"reset cd Tester");
            Core.Active reset=active.get(f.uuid);
            check(!locks.containsKey(f.uuid)&&reset.unlocks()==0&&reset.expires()==original.expires()&&reset.activated()==now&&reset.card()==4&&reset.level()==3,"reset cd only unlocks");
            check(f.modifiers.size()==3,"reset cd preserves attributes");
            // Reconstruct saved state to detect a reset that was only held in memory.
            active.clear();locks.clear();invoke(plugin,"loadState");
            check(active.get(f.uuid).equals(reset)&&!locks.containsKey(f.uuid),"reset cd survives load");
            active.put(f.uuid,original);locks.put(f.uuid,unlock);
            command(plugin,f,"reset buffer Tester");
            check(!active.containsKey(f.uuid)&&locks.get(f.uuid)==unlock,"reset buffer retains exact deadline");
            check(!f.modifiers.contains(own)&&f.modifiers.containsAll(List.of(album,other)),"reset buffer preserves collection and other attributes");
            active.clear();locks.clear();invoke(plugin,"loadState");
            check(!active.containsKey(f.uuid)&&locks.get(f.uuid)==unlock,"independent cooldown survives load");
            f.messages.clear();command(plugin,f,"status");check(f.messages.stream().anyMatch(s->s.contains("换牌冷却")),"status reports remaining cooldown without buff");
            command(plugin,f,"reset cd Tester");active.clear();locks.clear();invoke(plugin,"loadState");
            check(!active.containsKey(f.uuid)&&!locks.containsKey(f.uuid),"independent cooldown can also be reset");
            plugin.getLogger().info("TAROT COMMAND EVENT SELFTEST PASS: "+checks+" checks; batch all-or-nothing, permissions, reset isolation and persisted independent cooldowns.");
        }catch(Exception ex){throw new IllegalStateException("Command integration selftest failed",ex);}
        finally{
            if(active!=null&&oldActive!=null){active.clear();active.putAll(oldActive);}
            if(locks!=null&&oldLocks!=null){locks.clear();locks.putAll(oldLocks);}
            try{if(previous!=null)field("stateFile").set(plugin,previous);if(temp!=null)Files.deleteIfExists(temp.toPath());}
            catch(Exception ex){throw new IllegalStateException("Command test cleanup failed",ex);}
        }
    }
    private static void command(TarotPlugin plugin,Fixture f,String command){plugin.executeCommand(f.player,command.split(" "),name->name.equals("Tester")?f.player:null);}
    private static int items(Fixture f){return Arrays.stream(f.storage.getContents()).filter(Objects::nonNull).mapToInt(ItemStack::getAmount).sum();}
    private static Field field(String name)throws Exception{Field f=TarotPlugin.class.getDeclaredField(name);f.setAccessible(true);return f;}
    private static void invoke(TarotPlugin plugin,String name)throws Exception{Method m=TarotPlugin.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(plugin);}
    private static void check(boolean ok,String message){checks++;if(!ok)throw new IllegalStateException(message);}
    private static <T>T proxy(Class<T> type,InvocationHandler h){return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},h));}
    private static Object object(Object obj,String method,Object[] args){return switch(method){case "equals"->obj==args[0];case "hashCode"->System.identityHashCode(obj);case "toString"->"CommandTest";default->throw new UnsupportedOperationException(method);};}
    private static final class Fixture{
        final UUID uuid=UUID.randomUUID();final Inventory storage=Bukkit.createInventory(null,36);
        final List<String> messages=new ArrayList<>();final List<AttributeModifier> modifiers=new ArrayList<>();
        boolean admin=true;double health=20;
        final AttributeInstance maxHealth=proxy(AttributeInstance.class,(o,m,a)->switch(m.getName()){
            case "getModifiers"->List.copyOf(modifiers);case "removeModifier"->{modifiers.remove(a[0]);yield null;}
            case "getValue"->20+modifiers.stream().mapToDouble(AttributeModifier::getAmount).sum();
            default->object(o,m.getName(),a);
        });
        final PlayerInventory inventory=proxy(PlayerInventory.class,(o,m,a)->switch(m.getName()){
            case "getStorageContents","getContents"->storage.getContents();
            case "setStorageContents"->{storage.setContents((ItemStack[])a[0]);yield null;}
            default->object(o,m.getName(),a);
        });
        final Player player=proxy(Player.class,(o,m,a)->switch(m.getName()){
            case "getInventory"->inventory;case "getUniqueId"->uuid;case "getName"->"Tester";
            case "hasPermission"->admin;case "getGameMode"->GameMode.SURVIVAL;case "isDead"->false;
            case "sendMessage"->{messages.add(Arrays.toString(a));yield null;}
            case "getAttribute"->a[0]==Attribute.MAX_HEALTH?maxHealth:null;
            case "getHealth"->health;case "setHealth"->{health=(Double)a[0];yield null;}
            default->object(o,m.getName(),a);
        });
    }
}
