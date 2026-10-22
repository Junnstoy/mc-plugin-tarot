package cn.tarot;

import java.io.IOException;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** Personal, withdrawable collections. The bundle is an access key, never the storage. */
final class AlbumService implements Listener {
    private final TarotPlugin plugin;
    private final AlbumStore store;
    private final NamespacedKey albumKey;
    private final Map<UUID,AlbumStore.Data> profiles=new HashMap<>();
    private final Set<UUID> failed=new HashSet<>();
    private final Map<UUID,List<AlbumRules.Bonus>> applied=new HashMap<>();
    private final Map<UUID,Long> gestures=new HashMap<>();
    private final AlbumMenu menu;
    private int frame;
    AlbumService(TarotPlugin plugin){
        this(plugin,new AlbumStore(plugin.getDataFolder().toPath().resolve("albums")));
    }
    AlbumService(TarotPlugin plugin,AlbumStore store){
        this.plugin=plugin;albumKey=new NamespacedKey(plugin,"album");
        this.store=store;
        menu=new AlbumMenu(plugin,this);
    }
    void start(){
        Bukkit.getPluginManager().registerEvents(this,plugin);
        Bukkit.getPluginManager().registerEvents(menu,plugin);
        for(Player p:Bukkit.getOnlinePlayers())restore(p);
        Bukkit.getScheduler().runTaskTimer(plugin,()->{
            frame++;for(Player p:Bukkit.getOnlinePlayers()){
                if(frame%2==0)refresh(p);
                aura(p);
            }
        },10,10);
    }
    void shutdown(){menu.closeAll();for(Player p:Bukkit.getOnlinePlayers()){remove(p);clamp(p);}profiles.clear();}
    static Component text(String s,int color){return Component.text(s,TextColor.color(color)).decoration(TextDecoration.ITALIC,false);}
    private void tell(Player p,String text){p.sendMessage(text("[收集册] "+text,0xffdc70));}
    ItemStack makeAlbum(){
        ItemStack item=new ItemStack(Material.BUNDLE);var meta=item.getItemMeta();
        meta.getPersistentDataContainer().set(albumKey,PersistentDataType.BYTE,(byte)1);
        albumAppearance(meta);
        meta.itemName(text("彩虹塔罗 · 个人收集册",0xb478ff));
        meta.setEnchantmentGlintOverride(true);
        meta.lore(List.of(text("右键打开 · 22 张牌 × 5 星",0x80ddff),
            text("卡牌可取回；仅存入的卡计入收藏",0x8fffb2),
            text("随身携带：数量奖励、羁绊与跟随特效",0xffdc70),
            text("个人收藏跟随玩家保存，多本不叠加",0xbebed2)));
        item.setItemMeta(meta);return item;
    }
    private static void albumAppearance(ItemMeta meta){
        // Keep the vanilla bundle model. The resource pack selects only our
        // tagged album, preserving normal bundles and the no-pack fallback.
        meta.setItemModel(null);
        var modelData=meta.getCustomModelDataComponent();modelData.setStrings(List.of("tarot:album"));
        meta.setCustomModelDataComponent(modelData);
    }
    ItemStack migrateAppearance(ItemStack item){
        if(!isAlbum(item))return item;
        var meta=item.getItemMeta();
        if((!meta.hasItemModel()||NamespacedKey.minecraft("bundle").equals(meta.getItemModel()))&&
                meta.getCustomModelDataComponent().getStrings().equals(List.of("tarot:album")))return item;
        ItemStack updated=item.clone();albumAppearance(meta);updated.setItemMeta(meta);return updated;
    }
    private void migrateCarriedAlbums(Player p){
        for(int slot=0;slot<p.getInventory().getSize();slot++){
            ItemStack previous=p.getInventory().getItem(slot),next=migrateAppearance(previous);
            if(next!=previous)p.getInventory().setItem(slot,next);
        }
    }
    boolean isAlbum(ItemStack item){return item!=null&&item.getType()==Material.BUNDLE&&item.hasItemMeta()&&
        item.getItemMeta().getPersistentDataContainer().has(albumKey,PersistentDataType.BYTE);}
    boolean hasAlbum(Player p){for(ItemStack item:p.getInventory().getContents())if(isAlbum(item))return true;return false;}
    boolean heldAlbum(Player p){return isAlbum(p.getInventory().getItemInMainHand())||isAlbum(p.getInventory().getItemInOffHand());}
    boolean use(Player p){
        if(!heldAlbum(p))return false;
        long now=System.currentTimeMillis();Long last=gestures.put(p.getUniqueId(),now);
        if(last==null||now-last>=250)Bukkit.getScheduler().runTask(plugin,()->{if(p.isOnline())open(p,1);});
        return true;
    }
    boolean giveOrOpen(Player p){
        if(!hasAlbum(p)){
            ItemStack[] next=InventoryPlan.deposit(p.getInventory().getStorageContents(),List.of(makeAlbum()));
            if(next==null){tell(p,"背包空间不足，请腾出一格再领取。");return false;}
            p.getInventory().setStorageContents(next);
        }
        open(p,1);return true;
    }
    void open(Player p,int star){
        migrateCarriedAlbums(p);
        if(!hasAlbum(p)){tell(p,"请先将收集册放进背包。");return;}
        if(profile(p)==null)return;menu.open(p,star);
    }
    private AlbumStore.Data profile(Player p){
        UUID id=p.getUniqueId();if(failed.contains(id))return null;
        if(profiles.containsKey(id))return profiles.get(id);
        try{
            AlbumStore.Data data=store.load(id);
            for(int i=0;i<110;i++)if(data.cards[i]!=null){
                if(cardIndex(data.cards[i])!=i)throw new IOException("Mismatched album slot "+i);
                data.cards[i]=plugin.migrateItem(data.cards[i]);
            }
            profiles.put(id,data);return data;
        }catch(IOException ex){
            failed.add(id);plugin.getLogger().severe("Cannot load album "+id+": "+ex.getMessage());
            tell(p,"收藏存档读取失败，已暂停存取以保护卡牌，请联系管理员。");return null;
        }
    }
    ItemStack[] snapshot(Player p){
        AlbumStore.Data d=profile(p);ItemStack[] result=new ItemStack[110];
        if(d!=null)for(int i=0;i<110;i++)if(d.cards[i]!=null)result[i]=d.cards[i].clone();return result;
    }
    BitSet progress(Player p){
        AlbumStore.Data d=profile(p);BitSet bits=new BitSet(110);
        if(d!=null)for(int i=0;i<110;i++)if(d.cards[i]!=null)bits.set(i);return bits;
    }
    private int cardIndex(ItemStack item){
        ItemStack card=plugin.migrateItem(item);int id=plugin.cardId(card);
        return id==0?-1:AlbumRules.index(id,plugin.cardLevel(card));
    }
    private boolean canEdit(Player p){
        if(!hasAlbum(p)){tell(p,"收集册已离开背包，无法存取。");return false;}
        if(p.getGameMode()!=GameMode.SURVIVAL&&p.getGameMode()!=GameMode.ADVENTURE){
            tell(p,"请在生存或冒险模式中存入、取出卡牌。");return false;}
        return true;
    }
    boolean depositSlot(Player p,int slot){
        if(slot<0||slot>35||!canEdit(p))return false;
        ItemStack original=p.getInventory().getItem(slot);int index=cardIndex(original);
        if(index<0){tell(p,"只能收录塔罗卡牌。");return false;}
        AlbumStore.Data d=profile(p);if(d==null)return false;
        if(d.cards[index]!=null){tell(p,"该牌的这个星级已收录，重复牌未消耗。");return false;}
        if(original.getAmount()!=1){tell(p,"请将卡牌分为每格一张再收录。");return false;}
        ItemStack preserved=original.clone();
        d.cards[index]=plugin.migrateItem(original).clone();p.getInventory().setItem(slot,null);
        if(!save(p,d)){d.cards[index]=null;p.getInventory().setItem(slot,preserved);return false;}
        changed(p,"已收录，随时可以取回。");return true;
    }
    boolean depositCursor(Player p,int index){
        if(index<0||index>=110||!canEdit(p))return false;
        ItemStack original=p.getOpenInventory().getCursor();
        if(cardIndex(original)!=index||original.getAmount()!=1){tell(p,"请放入此位置对应的同名、同星卡牌。");return false;}
        AlbumStore.Data d=profile(p);if(d==null)return false;
        if(d.cards[index]!=null){tell(p,"这个收藏位置已有卡牌。");return false;}
        ItemStack preserved=original.clone();d.cards[index]=plugin.migrateItem(original).clone();p.getOpenInventory().setCursor(null);
        if(!save(p,d)){d.cards[index]=null;p.getOpenInventory().setCursor(preserved);return false;}
        changed(p,"已收录，随时可以取回。");return true;
    }
    boolean withdraw(Player p,int index){
        if(index<0||index>=110||!canEdit(p))return false;
        AlbumStore.Data d=profile(p);if(d==null||d.cards[index]==null)return false;
        ItemStack[] before=p.getInventory().getStorageContents();ItemStack item=d.cards[index];
        ItemStack[] next=InventoryPlan.deposit(before,List.of(item));
        if(next==null){tell(p,"背包空间不足，卡牌仍保存在收集册中。");return false;}
        p.getInventory().setStorageContents(next);d.cards[index]=null;
        if(!save(p,d)){d.cards[index]=item;p.getInventory().setStorageContents(before);return false;}
        changed(p,"已取回卡牌，对应收藏奖励已重新计算。");return true;
    }
    private boolean save(Player p,AlbumStore.Data data){
        try{store.save(p.getUniqueId(),data);return true;}
        catch(IOException|RuntimeException ex){plugin.getLogger().severe("Cannot save album "+p.getUniqueId()+": "+ex.getMessage());
            tell(p,"保存失败，本次操作已撤回，卡牌仍保留。");return false;}
    }
    private void changed(Player p,String message){refresh(p);tell(p,message);p.playSound(p.getLocation(),Sound.ITEM_BOOK_PAGE_TURN,.6f,1.2f);}
    boolean particlesEnabled(Player p){AlbumStore.Data d=profile(p);return d!=null&&d.particles;}
    boolean toggleParticles(Player p){
        AlbumStore.Data d=profile(p);if(d==null)return false;
        d.particles=!d.particles;if(!save(p,d)){d.particles=!d.particles;return false;}
        tell(p,d.particles?"跟随特效已开启，集齐任一星级22张后显示。":"跟随特效已关闭，属性奖励不受影响。");return true;
    }
    void status(Player p){
        BitSet bits=progress(p);tell(p,"已收录 "+bits.cardinality()+"/110；"+(hasAlbum(p)?"正在携带，奖励生效":"未携带，奖励暂停"));
        for(String line:AlbumRules.rewardLines(bits))tell(p,line);
    }
    void refresh(Player p){
        if(p.isDead())return;
        migrateCarriedAlbums(p);
        List<AlbumRules.Bonus> next=hasAlbum(p)?AlbumRules.totals(progress(p)):List.of();
        if(Objects.equals(applied.get(p.getUniqueId()),next))return;
        remove(p);
        for(AlbumRules.Bonus bonus:next){
            Attribute attribute=attribute(bonus.attribute());AttributeInstance instance=p.getAttribute(attribute);
            if(instance!=null)instance.addTransientModifier(new AttributeModifier(new NamespacedKey("tarot_album",bonus.attribute()),bonus.amount(),
                bonus.multiply()?AttributeModifier.Operation.ADD_SCALAR:AttributeModifier.Operation.ADD_NUMBER));
        }
        applied.put(p.getUniqueId(),next);clamp(p);
    }
    private static Attribute attribute(String key){return switch(key){
        case "max_health"->Attribute.MAX_HEALTH;case "attack_damage"->Attribute.ATTACK_DAMAGE;
        case "armor"->Attribute.ARMOR;case "movement_speed"->Attribute.MOVEMENT_SPEED;case "luck"->Attribute.LUCK;
        default->throw new IllegalArgumentException(key);
    };}
    private void remove(Player p){
        for(Attribute type:List.of(Attribute.MAX_HEALTH,Attribute.ATTACK_DAMAGE,Attribute.ARMOR,Attribute.MOVEMENT_SPEED,Attribute.LUCK)){
            AttributeInstance a=p.getAttribute(type);if(a==null)continue;
            for(AttributeModifier m:new ArrayList<>(a.getModifiers()))if(m.getKey().getNamespace().equals("tarot_album"))a.removeModifier(m);
        }
        applied.remove(p.getUniqueId());
    }
    private static void clamp(Player p){AttributeInstance a=p.getAttribute(Attribute.MAX_HEALTH);if(!p.isDead()&&a!=null&&p.getHealth()>a.getValue())p.setHealth(a.getValue());}
    private void aura(Player p){
        if(p.isDead()||p.getGameMode()==GameMode.SPECTATOR||p.isInvisible()||!hasAlbum(p)||!particlesEnabled(p))return;
        int tier=AlbumRules.auraTier(progress(p));if(tier==0)return;
        Location origin=p.getLocation();int points=tier==6?20:6+tier*2;
        for(int i=0;i<points;i++){
            double angle=2*Math.PI*i/points+frame*.18;
            double height=tier==6?.2+(i%10)*.18:.12+(tier-1)*.13;
            Location at=origin.clone().add(Math.cos(angle)*.65,height,Math.sin(angle)*.65);
            int color=TarotPlugin.RAINBOW[tier==6?(i+frame)%7:Math.min(6,tier-1)];
            var dust=new Particle.DustOptions(Color.fromRGB(color),.7f);
            for(Player viewer:p.getWorld().getPlayers())if(viewer.canSee(p)&&viewer.getLocation().distanceSquared(origin)<=32*32)
                viewer.spawnParticle(Particle.DUST,at,1,0,0,0,0,dust);
        }
    }
    private void later(Player p){if(plugin.isEnabled())Bukkit.getScheduler().runTask(plugin,()->{if(p.isOnline())refresh(p);});}
    private void restore(Player p){remove(p);refresh(p);}
    @EventHandler public void join(PlayerJoinEvent e){Bukkit.getScheduler().runTaskLater(plugin,()->{if(e.getPlayer().isOnline())restore(e.getPlayer());},12);}
    @EventHandler public void respawn(PlayerRespawnEvent e){Bukkit.getScheduler().runTaskLater(plugin,()->{if(e.getPlayer().isOnline())restore(e.getPlayer());},3);}
    @EventHandler public void quit(PlayerQuitEvent e){Player p=e.getPlayer();remove(p);clamp(p);profiles.remove(p.getUniqueId());failed.remove(p.getUniqueId());gestures.remove(p.getUniqueId());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void drop(PlayerDropItemEvent e){later(e.getPlayer());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void pickup(EntityPickupItemEvent e){if(e.getEntity() instanceof Player p)later(p);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void swap(PlayerSwapHandItemsEvent e){later(e.getPlayer());}
    @EventHandler public void close(InventoryCloseEvent e){if(e.getPlayer() instanceof Player p)later(p);}
    @EventHandler(priority=EventPriority.LOW,ignoreCancelled=true) public void bundleClick(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return;
        ItemStack cursor=e.getCursor(),target=e.getCurrentItem();
        if(isAlbum(target)&&e.getClick()==ClickType.RIGHT&&(cursor==null||cursor.isEmpty())){
            e.setCancelled(true);Bukkit.getScheduler().runTask(plugin,()->{if(p.isOnline())open(p,1);});return;
        }
        if((isAlbum(target)&&cursor!=null&&!cursor.isEmpty())||(isAlbum(cursor)&&target!=null&&!target.isEmpty())){
            e.setCancelled(true);tell(p,"右键打开收集册，再在界面中收录卡牌。");return;
        }
        later(p);
    }
}
