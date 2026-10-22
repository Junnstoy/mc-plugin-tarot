package cn.tarot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;

public final class TarotPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    record Bonus(Attribute attribute, String label, double amount, boolean multiply) {}
    record Card(int id, String name, int rarity, Bonus[] bonuses) {}
    static Bonus add(Attribute a,String n,double amount) { return new Bonus(a,n,amount,false); }
    static Bonus mult(Attribute a,String n,double amount) { return new Bonus(a,n,amount,true); }
    static final String[] RARITIES={"R","SR","SSR","SP","UR"};
    static final int[] RARITY_COLORS={0xff5252,0xff983e,0xffdc40,0x4bdf7f,0xb478ff};
    static final int[] RAINBOW={0xff5252,0xff983e,0xffdc40,0x4bdf7f,0x46e1e8,0x568bff,0xb478ff};
    static final double[] SCALE={1,1.2,1.45,1.75,2.1};
    static final Card[] CARDS={
        new Card(1,"愚者",0,new Bonus[]{mult(Attribute.MOVEMENT_SPEED,"移动速度",.10),add(Attribute.JUMP_STRENGTH,"跳跃力度",.10)}),
        new Card(2,"魔术师",1,new Bonus[]{mult(Attribute.ATTACK_SPEED,"攻击速度",.15),add(Attribute.ATTACK_DAMAGE,"攻击伤害",1)}),
        new Card(3,"女祭司",1,new Bonus[]{add(Attribute.MAX_HEALTH,"生命上限（点）",4),add(Attribute.LUCK,"幸运",1)}),
        new Card(4,"女皇",2,new Bonus[]{add(Attribute.MAX_HEALTH,"生命上限（点）",6),add(Attribute.ARMOR,"护甲",2)}),
        new Card(5,"皇帝",2,new Bonus[]{add(Attribute.ARMOR,"护甲",5),add(Attribute.KNOCKBACK_RESISTANCE,"击退抗性",.10)}),
        new Card(6,"教皇",1,new Bonus[]{add(Attribute.ARMOR,"护甲",2),add(Attribute.MAX_HEALTH,"生命上限（点）",2)}),
        new Card(7,"恋人",0,new Bonus[]{add(Attribute.MAX_HEALTH,"生命上限（点）",2),mult(Attribute.MOVEMENT_SPEED,"移动速度",.05)}),
        new Card(8,"战车",1,new Bonus[]{mult(Attribute.MOVEMENT_SPEED,"移动速度",.20),add(Attribute.STEP_HEIGHT,"跨步高度（格）",.5)}),
        new Card(9,"力量",2,new Bonus[]{add(Attribute.ATTACK_DAMAGE,"攻击伤害",3),add(Attribute.ATTACK_KNOCKBACK,"攻击击退",.3)}),
        new Card(10,"隐者",0,new Bonus[]{add(Attribute.SNEAKING_SPEED,"潜行速度",.15),mult(Attribute.MOVEMENT_SPEED,"移动速度",.05)}),
        new Card(11,"命运之轮",3,new Bonus[]{add(Attribute.LUCK,"幸运",4),mult(Attribute.ATTACK_SPEED,"攻击速度",.20)}),
        new Card(12,"正义",1,new Bonus[]{add(Attribute.ATTACK_DAMAGE,"攻击伤害",1),add(Attribute.ARMOR,"护甲",2)}),
        new Card(13,"倒吊人",0,new Bonus[]{add(Attribute.SAFE_FALL_DISTANCE,"安全跌落距离（格）",4),add(Attribute.JUMP_STRENGTH,"跳跃力度",.15)}),
        new Card(14,"死神",3,new Bonus[]{add(Attribute.ATTACK_DAMAGE,"攻击伤害",5),mult(Attribute.MOVEMENT_SPEED,"移动速度",.10)}),
        new Card(15,"节制",0,new Bonus[]{mult(Attribute.ATTACK_SPEED,"攻击速度",.10),add(Attribute.ARMOR,"护甲",1)}),
        new Card(16,"恶魔",2,new Bonus[]{add(Attribute.ATTACK_DAMAGE,"攻击伤害",4),mult(Attribute.ATTACK_SPEED,"攻击速度",.10)}),
        new Card(17,"高塔",3,new Bonus[]{add(Attribute.ARMOR,"护甲",6),add(Attribute.ATTACK_KNOCKBACK,"攻击击退",.8)}),
        new Card(18,"星星",0,new Bonus[]{add(Attribute.LUCK,"幸运",2),add(Attribute.SAFE_FALL_DISTANCE,"安全跌落距离（格）",3)}),
        new Card(19,"月亮",1,new Bonus[]{add(Attribute.SNEAKING_SPEED,"潜行速度",.25),add(Attribute.ATTACK_DAMAGE,"攻击伤害",1)}),
        new Card(20,"太阳",2,new Bonus[]{add(Attribute.MAX_HEALTH,"生命上限（点）",8),add(Attribute.ARMOR,"护甲",2)}),
        new Card(21,"审判",4,new Bonus[]{add(Attribute.ATTACK_DAMAGE,"攻击伤害",5),add(Attribute.ARMOR,"护甲",6)}),
        new Card(22,"世界",4,new Bonus[]{add(Attribute.ATTACK_DAMAGE,"攻击伤害",3),add(Attribute.ARMOR,"护甲",4),mult(Attribute.MOVEMENT_SPEED,"移动速度",.20),add(Attribute.MAX_HEALTH,"生命上限（点）",6)})
    };
    private final Map<UUID,Core.Active> active=new HashMap<>();
    // Independent deadlines survive clearing a buff; active records keep their
    // legacy unlock field so existing players.yml files remain readable.
    private final Map<UUID,Long> cooldowns=new HashMap<>();
    // Cache bypass while offline so an unlimited effect survives server restarts.
    // Online permission is always authoritative; original deadlines remain intact.
    private final Set<UUID> bypassPlayers=new HashSet<>();
    private final Map<UUID,Long> gestures=new HashMap<>();
    private final Map<UUID,Long> creativeMergeHints=new HashMap<>();
    private final Set<UUID> fakeIcon=new HashSet<>();
    private NamespacedKey cardKey,levelKey,packageKey;
    private long durationMillis,lockMillis;
    private boolean iconEnabled,barEnabled;
    private File stateFile;
    private int seconds;
    private AlbumService albums;
    private ChargeController charging;
    private Runnable placeholderCleanup;
    private final java.util.concurrent.ConcurrentMap<UUID,TarotPlaceholderValues.Snapshot> placeholderCache=new java.util.concurrent.ConcurrentHashMap<>();
    private final Set<UUID> placeholderPending=java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Override public void onEnable() {
        saveDefaultConfig();
        durationMillis=Math.max(1,Math.min(31536000,getConfig().getLong("duration-seconds",86400)))*1000L;
        lockMillis=Math.max(0,Math.min(31536000,getConfig().getLong("switch-lock-seconds",1800)))*1000L;
        iconEnabled=getConfig().getBoolean("status-icon",true); barEnabled=getConfig().getBoolean("actionbar",true);
        cardKey=new NamespacedKey(this,"card");levelKey=new NamespacedKey(this,"level");packageKey=new NamespacedKey(this,"package");
        stateFile=new File(getDataFolder(),"players.yml");loadState();
        getServer().getPluginManager().registerEvents(this,this);
        Objects.requireNonNull(getCommand("tarot")).setExecutor(this);getCommand("tarot").setTabCompleter(this);
        for(Player p:Bukkit.getOnlinePlayers()) restore(p);
        albums=new AlbumService(this);albums.start();
        charging=new ChargeController(this,s->cardId(s)>0,this::canCharge,this::renderCast,this::activateCard);charging.start();
        if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))placeholderCleanup=TarotPlaceholders.install(this);
        Bukkit.getScheduler().runTaskTimer(this,this::tick,20,20);
        getLogger().info("22 cards x 5 levels ready; real-time duration="+durationMillis/1000+"s, switch lock="+lockMillis/1000+"s. Remove legacy tarot datapacks before use.");
    }
    @Override public void onDisable() {
        if(placeholderCleanup!=null){placeholderCleanup.run();placeholderCleanup=null;}
        placeholderCache.clear();placeholderPending.clear();
        if(charging!=null)charging.shutdown();
        if(albums!=null)albums.shutdown();
        saveState();
        for(Player p:Bukkit.getOnlinePlayers()) { removeAttributes(p);clampHealth(p);removeIcon(p); }
    }
    private static Component text(String s,int color) {return Component.text(s,TextColor.color(color)).decoration(TextDecoration.ITALIC,false);}
    private void message(Player p,String s) {p.sendMessage(text("[塔罗] "+s,0xffdc70));}
    private static String number(double n) {return String.format(Locale.ROOT,"%.3f",n).replaceAll("0+$","").replaceAll("\\.$","");}
    private static String time(long ms) {long sec=(Math.max(0,ms)+999)/1000;return String.format(Locale.ROOT,"%02d:%02d:%02d",sec/3600,sec%3600/60,sec%60);}
    private static Card card(int id) {return CARDS[id-1];}
    private int value(ItemStack s,NamespacedKey key) {
        if(s==null||s.getType()!=Material.PAPER||!s.hasItemMeta())return 0;
        return s.getItemMeta().getPersistentDataContainer().getOrDefault(key,PersistentDataType.INTEGER,0);
    }
    int cardId(ItemStack s) {int id=value(s,cardKey),level=value(s,levelKey);return id>=1&&id<=22&&level>=1&&level<=5?id:0;}
    int cardLevel(ItemStack s) {return value(s,levelKey);}
    private String packageType(ItemStack s) {
        if(s==null||s.getType()!=Material.PAPER||!s.hasItemMeta())return "";
        String k=s.getItemMeta().getPersistentDataContainer().getOrDefault(packageKey,PersistentDataType.STRING,"");
        return Set.of("pack","box","case").contains(k)?k:"";
    }
    private boolean tarot(ItemStack s) {return cardId(s)>0||!packageType(s).isEmpty();}
    private static void appearance(ItemMeta meta,String model) {
        // A vanilla item model always exists, even when no resource pack is loaded.
        // The pack selects tarot artwork from this string; ordinary paper falls back.
        meta.setItemModel(null);
        var data=meta.getCustomModelDataComponent();data.setStrings(List.of("tarot:"+model));
        meta.setCustomModelDataComponent(data);
    }
    private static boolean vanillaPaperModel(ItemMeta meta) {
        return !meta.hasItemModel()||NamespacedKey.minecraft("paper").equals(meta.getItemModel());
    }
    public ItemStack makeCard(int id,int level) {
        Card c=card(id);ItemStack s=new ItemStack(Material.PAPER);ItemMeta m=s.getItemMeta();
        m.setMaxStackSize(1);appearance(m,"card_"+id+"_"+level);
        m.getPersistentDataContainer().set(cardKey,PersistentDataType.INTEGER,id);m.getPersistentDataContainer().set(levelKey,PersistentDataType.INTEGER,level);
        m.itemName(text(RARITIES[c.rarity]+" · "+c.name+" "+"★".repeat(level),RARITY_COLORS[c.rarity]));
        List<Component> lore=new ArrayList<>();
        for(Bonus b:c.bonuses) lore.add(text(b.label+" +"+number(b.amount*SCALE[level-1]*(b.multiply?100:1))+(b.multiply?"%（基础值）":""),0x8fffb2));
        lore.add(text("主手持牌 · 长按右键 5 秒激活",0x80ddff));
        lore.add(text("松手或换牌取消蓄力；成功后开始效果及冷却计时",0xa9a9bd));
        lore.add(text("持续 "+time(durationMillis)+" · 换牌锁定 "+time(lockMillis),0xffdc70));
        lore.add(text("现实时间计时，离线仍计时；移出主手仍生效",0xa9a9bd));
        lore.add(text(level<5?"生存／冒险背包：同名同星卡重叠升级":"已达到最高五星",0xe7bbff));
        if(Arrays.stream(c.bonuses).anyMatch(b->b.attribute==Attribute.LUCK))lore.add(text("幸运不会增加本卡包的稀有牌概率",0xaaaaaa));
        m.lore(lore);s.setItemMeta(m);ChargeController.configureCard(s);return s;
    }
    public ItemStack makePackage(String kind,int amount) {
        ItemStack s=new ItemStack(Material.PAPER,amount);ItemMeta m=s.getItemMeta();m.setMaxStackSize(64);
        m.getPersistentDataContainer().set(packageKey,PersistentDataType.STRING,kind);appearance(m,kind);
        String name=switch(kind){case "pack"->"彩虹塔罗卡包 · 5 张";case "box"->"彩虹塔罗卡盒 · 8 包";default->"彩虹塔罗卡箱 · 48 盒";};
        m.itemName(text(name,0xffdc70));m.lore(List.of(text("主手持有 · 右键拆开",0x80ddff),text("每包至少一张 SR 或更高；抽出均为一星",0xe7bbff)));
        s.setItemMeta(m);return s;
    }
    private boolean give(Player p,List<ItemStack> items) {
        ItemStack[] next=InventoryPlan.deposit(p.getInventory().getStorageContents(),items);
        if(next==null){message(p,"背包空间不足，发放未执行。请腾出空间后重试。");return false;}
        p.getInventory().setStorageContents(next);return true;
    }
    private void open(Player p,String kind) {
        PlayerInventory inventory=p.getInventory();int slot=inventory.getHeldItemSlot();
        ItemStack held=inventory.getItemInMainHand();
        if(!packageType(held).equals(kind))return;
        // The main hand is part of storage. Plan its consumption and all rewards
        // together so committing rewards cannot restore or overwrite the held item.
        ItemStack[] storage=inventory.getStorageContents().clone();
        ItemStack remaining=held.clone();remaining.setAmount(remaining.getAmount()-1);
        storage[slot]=remaining.getAmount()>0?remaining:null;
        List<ItemStack> rewards=new ArrayList<>();int best=0;List<String> drawn=new ArrayList<>();
        if(kind.equals("case"))rewards.add(makePackage("box",48));
        else if(kind.equals("box"))rewards.add(makePackage("pack",8));
        else {
            for(int i=0;i<5;i++) {
                int rarity=Core.rarity(ThreadLocalRandom.current().nextInt(10000),i==4);best=Math.max(best,rarity);
                Card[] pool=Arrays.stream(CARDS).filter(c->c.rarity==rarity).toArray(Card[]::new);
                Card c=pool[ThreadLocalRandom.current().nextInt(pool.length)];rewards.add(makeCard(c.id,1));drawn.add(RARITIES[c.rarity]+" "+c.name);
            }
        }
        ItemStack[] next=InventoryPlan.deposit(storage,rewards);
        if(next==null){message(p,kind.equals("pack")?"背包空间不足以容纳 5 张卡牌，卡包未消耗。请腾出空间后重试。":"背包空间不足，原包装未消耗。请腾出空间后重试。");return;}
        // All checks pass before either side of the exchange changes; events run on the server thread.
        inventory.setStorageContents(next);
        if(kind.equals("case"))message(p,"拆开一箱：获得 48 盒。");
        else if(kind.equals("box"))message(p,"拆开一盒：获得 8 包。");
        else {
            message(p,"开包获得："+String.join("、",drawn));
            p.showTitle(net.kyori.adventure.title.Title.title(text(RARITIES[best],RARITY_COLORS[best]),text("五张新牌 · 命运揭晓",0xffffff)));
            burst(p,Math.max(1,best+1));
            p.playSound(p.getLocation(),best>=2?Sound.UI_TOAST_CHALLENGE_COMPLETE:Sound.ENTITY_EXPERIENCE_ORB_PICKUP,.75f,1.1f);
        }
        p.playSound(p.getLocation(),Sound.ITEM_BUNDLE_DROP_CONTENTS,.7f,1);
    }
    private boolean handlePackage(Player p) {
        if(p.getGameMode()==GameMode.SPECTATOR)return false;
        migrate(p);
        String pack=packageType(p.getInventory().getItemInMainHand());if(pack.isEmpty())return false;
        long now=System.currentTimeMillis();Long previous=gestures.get(p.getUniqueId());
        if(previous!=null&&now-previous<250)return true;
        gestures.put(p.getUniqueId(),now);
        open(p,pack);return true;
    }
    boolean isCharging(Player p){return charging!=null&&charging.isCharging(p);}
    private boolean canCharge(Player p){
        if(p.getGameMode()==GameMode.SPECTATOR)return false;
        long cooldown=effectiveCooldown(p,System.currentTimeMillis());
        if(cooldown>0){message(p,"换牌仍锁定，"+time(cooldown)+" 后可切换或重新激活。");return false;}
        return true;
    }
    private void renderCast(Player p,double progress){
        ItemStack held=p.getInventory().getItemInMainHand();int id=cardId(held);if(id==0)return;
        int level=cardLevel(held);
        if(!p.isInvisible())ParticleLines.draw(p,TarotCast.frame(level,progress,CardParticleArt.get(id)),false);
        p.sendActionBar(text("「"+card(id).name+"」蓄力 "+String.format(Locale.ROOT,"%.1f / 5.0 秒",progress*5)+" · 松手取消",0xc9a1ff));
    }
    private void activateCard(Player p){
        ItemStack held=p.getInventory().getItemInMainHand();if(cardId(held)==0)return;
        long now=System.currentTimeMillis();
        int id=cardId(held),level=value(held,levelKey);
        Core.Active state=new Core.Active(id,level,now,now+durationMillis,now+lockMillis);
        active.put(p.getUniqueId(),state);
        boolean unlimited=bypass(p);
        cooldowns.put(p.getUniqueId(),Math.min(state.expires(),state.unlocks()));
        apply(p,state);updateIcon(p,state);saveState();renderCast(p,1);
        p.sendActionBar(text("「"+card(id).name+"」已激活",0x8fffb2));
        p.playSound(p.getLocation(),Sound.BLOCK_ENCHANTMENT_TABLE_USE,.8f,.8f+level*.12f);
        message(p,"已激活「"+card(id).name+"」"+"★".repeat(level)+(unlimited?"，权限豁免：效果永久，可立即换牌。":"，持续 "+time(durationMillis)+"；"+time(lockMillis)+" 后可换牌。"));
    }
    @EventHandler(priority=EventPriority.HIGH) public void interact(PlayerInteractEvent e) {
        if(e.getAction()!=Action.RIGHT_CLICK_AIR&&e.getAction()!=Action.RIGHT_CLICK_BLOCK)return;
        Player p=e.getPlayer();
        if(!packageType(p.getInventory().getItemInMainHand()).isEmpty()){
            if(e.getHand()!=EquipmentSlot.HAND||e.useItemInHand()==Event.Result.DENY)return;
            if(handlePackage(p)){e.setUseInteractedBlock(Event.Result.DENY);e.setUseItemInHand(Event.Result.DENY);e.setCancelled(true);}
            return;
        }
        if(e.getHand()==EquipmentSlot.HAND&&cardId(p.getInventory().getItemInMainHand())>0){
            if(e.useItemInHand()==Event.Result.DENY)return;
            migrate(p);
            e.setUseInteractedBlock(Event.Result.DENY);
            e.setUseItemInHand(charging.request(p)?Event.Result.ALLOW:Event.Result.DENY);
            return;
        }
        // Only the main hand can start a card cast. Keep offhand cards unconsumable.
        if(e.getHand()==EquipmentSlot.OFF_HAND&&cardId(p.getInventory().getItemInOffHand())>0){e.setUseItemInHand(Event.Result.DENY);return;}
        if(albums!=null&&albums.use(e.getPlayer())){e.setUseInteractedBlock(Event.Result.DENY);e.setUseItemInHand(Event.Result.DENY);e.setCancelled(true);return;}
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void entityInteract(PlayerInteractEntityEvent e) {
        if(!packageType(e.getPlayer().getInventory().getItemInMainHand()).isEmpty()){
            if(e.getHand()==EquipmentSlot.HAND&&handlePackage(e.getPlayer()))e.setCancelled(true);
            return;
        }
        if(albums!=null&&albums.use(e.getPlayer())){e.setCancelled(true);return;}
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void merge(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player p)||e.getClickedInventory()!=p.getInventory())return;
        if(e.getSlot()<0||e.getSlot()>35)return;
        ItemStack cursor=migrateItem(e.getCursor()),target=migrateItem(e.getCurrentItem());int a=cardId(cursor),b=cardId(target);
        // Creative events carry a client-proposed slot replacement, not a reliable
        // server cursor. CREATIVE is not a mouse ClickType, so explain this before
        // the mouse/pair guard. Leave ordinary creative inventory movement alone.
        if(e instanceof InventoryCreativeEvent||p.getGameMode()==GameMode.CREATIVE){
            if(a>0||b>0)creativeMergeHint(p);
            return;
        }
        if(a==0||b==0||(e.getClick()!=ClickType.LEFT&&e.getClick()!=ClickType.RIGHT))return;
        e.setCancelled(true);
        if(!Core.canMerge(a,value(cursor,levelKey),b,value(target,levelKey),cursor.getAmount(),target.getAmount())) {
            message(p,value(cursor,levelKey)==5&&value(target,levelKey)==5?"五星已经满级，不能继续升级。":"需要两张同名、同星级的一至四星卡，每格各一张。");return;
        }
        int level=value(target,levelKey)+1;
        e.setCurrentItem(makeCard(a,level));e.getView().setCursor(null);
        p.playSound(p.getLocation(),Sound.BLOCK_ENCHANTMENT_TABLE_USE,.8f,1.4f);burst(p,level);
        message(p,"升级成功：「"+card(a).name+"」"+"★".repeat(level)+"。已激活效果保持原星级，重新激活后更新。");
    }
    private void creativeMergeHint(Player p) {
        long now=System.currentTimeMillis();
        Long previous=creativeMergeHints.get(p.getUniqueId());
        if(previous!=null&&now-previous<2000)return;
        creativeMergeHints.put(p.getUniqueId(),now);
        message(p,"创造背包不支持合成升级。请切换生存或冒险模式，再将一张卡放到同名同星卡上；五星已满级。");
    }
    // Native drag distributes a stack between empty/non-full slots. Cards are
    // unstackable, so pickup + placement is handled above by InventoryClickEvent.
    // Do not synthesize an upgrade in a cancelled InventoryDragEvent: Minecraft
    // restores its pre-event cursor afterwards, which could retain the material.
    ItemStack migrateItem(ItemStack item) {
        if(albums!=null&&albums.isAlbum(item))return albums.migrateAppearance(item);
        if(item==null||item.getType()!=Material.PAPER)return item;
        if(tarot(item)){
            int id=cardId(item);String model=id>0?"card_"+id+"_"+value(item,levelKey):packageType(item);
            ItemMeta meta=item.getItemMeta();
            List<Component> lore=meta.lore();
            List<Component> updatedLore=lore==null?null:lore.stream().map(id>0?TarotPlugin::migrateCardLore:TarotPlugin::migratePackageLore).toList();
            if((id==0||ChargeController.isConfigured(item))&&vanillaPaperModel(meta)&&
                    meta.getCustomModelDataComponent().getStrings().equals(List.of("tarot:"+model))&&Objects.equals(lore,updatedLore))return item;
            ItemStack updated=item.clone();appearance(meta,model);
            meta.lore(updatedLore);
            updated.setItemMeta(meta);if(id>0)ChargeController.configureCard(updated);return updated;
        }
        try {
            var root=Bukkit.getUnsafe().serializeItemAsJson(item);
            var components=root.getAsJsonObject("components");if(components==null)return item;
            var raw=components.get("minecraft:custom_data");if(raw==null)return item;
            // Paper serializes custom_data as SNBT text. V2 uses simple compound/int/string
            // tags, which Gson's lenient reader accepts without changing the original item.
            var data=raw.isJsonPrimitive()?com.google.gson.JsonParser.parseString(raw.getAsString()).getAsJsonObject():raw.getAsJsonObject();
            var old=data.getAsJsonObject("tarot");if(old==null)return item;
            ItemStack result=null;
            if(old.has("card")&&old.has("level")){
                int id=old.get("card").getAsInt(),level=old.get("level").getAsInt();
                if(id>=1&&id<=22&&level>=1&&level<=5&&item.getAmount()==1)result=makeCard(id,level);
            } else if(old.has("package")) {
                String k=old.get("package").getAsString();
                if(Set.of("pack","box","case").contains(k)&&item.getAmount()<=64)result=makePackage(k,item.getAmount());
            }
            return result==null?item:result;
        } catch(RuntimeException ex){return item;}
    }
    private static Component migrateCardLore(Component line){
        for(String old:List.of("副手持牌 · 潜行 + 右键激活","副手持牌 · 长按右键 5 秒激活"))
            line=line.replaceText(net.kyori.adventure.text.TextReplacementConfig.builder()
                .matchLiteral(old).replacement("主手持牌 · 长按右键 5 秒激活").build());
        return line.replaceText(net.kyori.adventure.text.TextReplacementConfig.builder()
            .matchLiteral("移出副手仍生效").replacement("移出主手仍生效").build());
    }
    private static Component migratePackageLore(Component line){
        return line.replaceText(net.kyori.adventure.text.TextReplacementConfig.builder()
            .matchLiteral("副手持有 · 潜行 + 右键拆开").replacement("主手持有 · 右键拆开").build());
    }
    private int migrate(Player p) {
        int count=0;
        for(int i=0;i<p.getInventory().getSize();i++){
            ItemStack old=p.getInventory().getItem(i),next=migrateItem(old);
            if(next!=old){p.getInventory().setItem(i,next);count++;}
        }
        return count;
    }
    @EventHandler public void inventoryClose(InventoryCloseEvent e){if(isEnabled()&&e.getPlayer() instanceof Player p)Bukkit.getScheduler().runTask(this,()->{if(p.isOnline())migrate(p);});}
    private Set<Attribute> allAttributes() {return Arrays.stream(CARDS).flatMap(c->Arrays.stream(c.bonuses)).map(Bonus::attribute).collect(Collectors.toSet());}
    private void removeAttributes(Player p) {
        for(Attribute type:allAttributes()) {
            AttributeInstance attr=p.getAttribute(type);if(attr==null)continue;
            for(AttributeModifier m:new ArrayList<>(attr.getModifiers())) {
                NamespacedKey key=m.getKey();
                if(key.getNamespace().equals("tarotrainbow") || (key.getNamespace().equals("tarot")&&key.getKey().equals(type.getKey().getKey())))attr.removeModifier(m);
            }
        }
    }
    private void clampHealth(Player p) {AttributeInstance health=p.getAttribute(Attribute.MAX_HEALTH);if(health!=null&&!p.isDead()&&p.getHealth()>health.getValue())p.setHealth(health.getValue());}
    private void apply(Player p,Core.Active state) {
        removeAttributes(p);
        for(Bonus b:card(state.card()).bonuses) {
            AttributeInstance attr=p.getAttribute(b.attribute);if(attr==null)continue;
            AttributeModifier modifier=new AttributeModifier(new NamespacedKey(this,b.attribute.getKey().getKey()),b.amount*SCALE[state.level()-1],b.multiply?AttributeModifier.Operation.ADD_SCALAR:AttributeModifier.Operation.ADD_NUMBER);
            attr.addTransientModifier(modifier);
        }
        clampHealth(p);
    }
    private void updateIcon(Player p,Core.Active state) {
        if(!iconEnabled)return;
        PotionEffect real=p.getPotionEffect(PotionEffectType.LUCK);
        if(real!=null) {
            if(fakeIcon.remove(p.getUniqueId()))p.sendPotionEffectChange(p,real);
            return;
        }
        int ticks=bypass(p)?-1:(int)Math.max(1,Math.min(Integer.MAX_VALUE,(state.remaining(System.currentTimeMillis())+49)/50));
        p.sendPotionEffectChange(p,new PotionEffect(PotionEffectType.LUCK,ticks,0,true,false,true));fakeIcon.add(p.getUniqueId());
    }
    private void removeIcon(Player p) {
        if(!fakeIcon.remove(p.getUniqueId()))return;
        PotionEffect real=p.getPotionEffect(PotionEffectType.LUCK);
        if(real!=null)p.sendPotionEffectChange(p,real);else p.sendPotionEffectChangeRemove(p,PotionEffectType.LUCK);
    }
    private void restore(Player p) {
        migrate(p);
        removeAttributes(p);Core.Active s=active.get(p.getUniqueId());
        boolean unlimited=bypass(p);
        if(s!=null&&(unlimited||!s.expired(System.currentTimeMillis()))){apply(p,s);updateIcon(p,s);}else{active.remove(p.getUniqueId());removeIcon(p);clampHealth(p);}
    }
    private void tick() {
        long now=System.currentTimeMillis();boolean changed=false;seconds++;
        changed=cooldowns.entrySet().removeIf(e->e.getValue()<=now);
        Iterator<Map.Entry<UUID,Core.Active>> it=active.entrySet().iterator();
        while(it.hasNext()){
            var entry=it.next();Core.Active s=entry.getValue();Player p=Bukkit.getPlayer(entry.getKey());
            boolean before=bypassPlayers.contains(entry.getKey());
            boolean unlimited=p==null?before:bypass(p);
            changed|=before!=unlimited;
            if(!unlimited&&s.expired(now)){
                it.remove();changed=true;
                if(p!=null){removeAttributes(p);clampHealth(p);removeIcon(p);message(p,"「"+card(s.card()).name+"」效果已到期。");p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_CHIME,.5f,.8f);}
                continue;
            }
            if(p==null||p.isDead())continue;
            updateIcon(p,s);
            if(barEnabled&&!isCharging(p)&&(tarot(p.getInventory().getItemInMainHand())||p.hasPotionEffect(PotionEffectType.LUCK)))
                p.sendActionBar(text(card(s.card()).name+" "+"★".repeat(s.level())+" | 剩余 "+(unlimited?"永久":time(s.remaining(now)))+" | "+(effectiveCooldown(p,now)>0?"换牌 "+time(effectiveCooldown(p,now)):"可以换牌"),RARITY_COLORS[card(s.card()).rarity]));
        }
        if(changed||seconds%60==0)saveState();
        if(placeholderCleanup!=null)for(Player player:Bukkit.getOnlinePlayers())refreshPlaceholder(player.getUniqueId(),player);
    }
    @EventHandler public void join(PlayerJoinEvent e) {Bukkit.getScheduler().runTaskLater(this,()->{if(e.getPlayer().isOnline())restore(e.getPlayer());},10);}
    @EventHandler public void quit(PlayerQuitEvent e) {bypass(e.getPlayer());removeAttributes(e.getPlayer());fakeIcon.remove(e.getPlayer().getUniqueId());gestures.remove(e.getPlayer().getUniqueId());creativeMergeHints.remove(e.getPlayer().getUniqueId());saveState();if(placeholderCleanup!=null)refreshPlaceholder(e.getPlayer().getUniqueId(),null);}
    @EventHandler public void respawn(PlayerRespawnEvent e) {Bukkit.getScheduler().runTaskLater(this,()->{if(e.getPlayer().isOnline())restore(e.getPlayer());},2);}
    @EventHandler public void world(PlayerChangedWorldEvent e) {Bukkit.getScheduler().runTaskLater(this,()->{if(e.getPlayer().isOnline())restore(e.getPlayer());},2);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void effect(EntityPotionEffectEvent e) {
        if(e.getEntity() instanceof Player p&&e.getModifiedType()==PotionEffectType.LUCK)
            Bukkit.getScheduler().runTask(this,()->{Core.Active s=active.get(p.getUniqueId());if(p.isOnline()&&s!=null&&(bypass(p)||!s.expired(System.currentTimeMillis())))updateIcon(p,s);});
    }
    private void burst(Player p,int level) {
        UUID id=p.getUniqueId();
        new BukkitRunnable(){int frame;public void run(){
            Player current=Bukkit.getPlayer(id);if(current==null||current.isDead()||frame++>=10){cancel();return;}
            Location center=current.getLocation().add(0,.2+frame*.10,0);int count=14+level*3;
            for(int i=0;i<count;i++) {double angle=2*Math.PI*i/count+frame*.18;Location point=center.clone().add(Math.cos(angle)*(.65+level*.10),0,Math.sin(angle)*(.65+level*.10));
                current.getWorld().spawnParticle(Particle.DUST,point,1,0,0,0,0,new Particle.DustOptions(Color.fromRGB(RAINBOW[(i+frame)%7]),.8f+level*.1f));}
        }}.runTaskTimer(this,0,2);
    }
    private void loadState() {
        YamlConfiguration y=YamlConfiguration.loadConfiguration(stateFile);long now=System.currentTimeMillis();
        var section=y.getConfigurationSection("players");
        if(section!=null)for(String key:section.getKeys(false))try{
            String path="players."+key+".";int id=y.getInt(path+"card"),level=y.getInt(path+"level");
            if(id<1||id>22||level<1||level>5)continue;
            Core.Active s=new Core.Active(id,level,y.getLong(path+"activated"),y.getLong(path+"expires"),y.getLong(path+"unlocks"));
            boolean unlimited=y.getBoolean(path+"bypass",false);
            if(unlimited||!s.expired(now)){
                UUID uuid=UUID.fromString(key);active.put(uuid,s);
                if(unlimited)bypassPlayers.add(uuid);else bypassPlayers.remove(uuid);
                if(s.locked(now))cooldowns.put(uuid,Math.min(s.expires(),s.unlocks()));
            }
        }catch(IllegalArgumentException ex){getLogger().warning("Ignored invalid player state: "+key);}
        var locks=y.getConfigurationSection("cooldowns");
        if(locks!=null)for(String key:locks.getKeys(false))try{
            UUID id=UUID.fromString(key);long deadline=locks.getLong(key);
            if(!active.containsKey(id)&&deadline>now)cooldowns.put(id,deadline);
        }catch(IllegalArgumentException ex){getLogger().warning("Ignored invalid cooldown state: "+key);}
    }
    private void saveState() {
        YamlConfiguration y=new YamlConfiguration();active.forEach((id,s)->{String p="players."+id+".";y.set(p+"card",s.card());y.set(p+"level",s.level());y.set(p+"activated",s.activated());y.set(p+"expires",s.expires());y.set(p+"unlocks",s.unlocks());if(bypassPlayers.contains(id))y.set(p+"bypass",true);});
        long now=System.currentTimeMillis();
        cooldowns.forEach((id,deadline)->{if(!active.containsKey(id)&&deadline>now)y.set("cooldowns."+id,deadline);});
        try {File tmp=new File(getDataFolder(),"players.yml.tmp");y.save(tmp);try{Files.move(tmp.toPath(),stateFile.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(java.nio.file.AtomicMoveNotSupportedException ex){Files.move(tmp.toPath(),stateFile.toPath(),StandardCopyOption.REPLACE_EXISTING);}}
        catch(IOException ex){getLogger().severe("Cannot persist tarot activation deadlines: "+ex.getMessage());}
    }
    private long cooldownRemaining(UUID id,long now){return Math.max(0,cooldowns.getOrDefault(id,0L)-now);}
    private boolean bypass(Player p){
        boolean enabled=p.hasPermission("tarot.bypass.cd");
        if(enabled)bypassPlayers.add(p.getUniqueId());else bypassPlayers.remove(p.getUniqueId());
        return enabled;
    }
    private long effectiveCooldown(Player p,long now){return bypass(p)?0:cooldownRemaining(p.getUniqueId(),now);}
    /** PAPI may call from async scoreboards: only immutable snapshots leave the server thread. */
    TarotPlaceholderValues.Snapshot placeholderSnapshot(OfflinePlayer player){
        if(player==null)return TarotPlaceholderValues.Snapshot.empty();
        UUID id=player.getUniqueId();
        if(Bukkit.isPrimaryThread()){
            Player online=player instanceof Player p?p:player.getPlayer();
            if(online==null&&placeholderCache.containsKey(id))return placeholderCache.get(id);
            return refreshPlaceholder(id,online);
        }
        if(isEnabled()&&!placeholderCache.containsKey(id)&&placeholderPending.add(id))Bukkit.getScheduler().runTask(this,()->{
            try{refreshPlaceholder(id,Bukkit.getPlayer(id));}finally{placeholderPending.remove(id);}
        });
        return placeholderCache.getOrDefault(id,TarotPlaceholderValues.Snapshot.empty());
    }
    private TarotPlaceholderValues.Snapshot refreshPlaceholder(UUID id,Player player){
        Core.Active state=active.get(id);
        boolean unlimited=player==null?bypassPlayers.contains(id):bypass(player);
        AlbumService.PlaceholderAlbum collection=albums==null?new AlbumService.PlaceholderAlbum(new BitSet(),false):albums.placeholderAlbum(id);
        boolean carried=player!=null&&albums!=null&&albums.hasAlbum(player);
        boolean eligible=player!=null&&!player.isDead()&&player.getGameMode()!=GameMode.SPECTATOR;
        Card c=state==null?null:card(state.card());
        TarotPlaceholderValues.Snapshot snapshot=new TarotPlaceholderValues.Snapshot(c==null?0:c.id(),c==null?"":c.name(),
            state==null?0:state.level(),c==null?"":RARITIES[c.rarity()],state==null?0:state.expires(),
            cooldowns.getOrDefault(id,0L),unlimited,collection.progress(),carried,collection.particles(),eligible);
        placeholderCache.put(id,snapshot);return snapshot;
    }
    private void resetCooldown(Player p){
        cooldowns.remove(p.getUniqueId());
        active.computeIfPresent(p.getUniqueId(),(id,s)->new Core.Active(s.card(),s.level(),s.activated(),s.expires(),0));
        gestures.remove(p.getUniqueId());saveState();
        message(p,"管理员已重置换牌冷却，当前卡牌效果与到期时间保持不变。");
    }
    private void resetBuffer(Player p){
        active.remove(p.getUniqueId());removeAttributes(p);clampHealth(p);removeIcon(p);saveState();
        message(p,"管理员已清除当前塔罗卡牌效果；换牌冷却、收藏奖励和其他效果保持不变。");
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        return executeCommand(sender,args,Bukkit::getPlayerExact);
    }
    private static List<String> cardNames(){return Arrays.stream(CARDS).map(Card::name).toList();}
    boolean executeCommand(CommandSender sender,String[] args,java.util.function.Function<String,Player> lookup){
        TarotCommand.Request request;
        try{request=TarotCommand.parse(args,cardNames());}
        catch(IllegalArgumentException ex){sender.sendMessage(ex.getMessage());return true;}
        if(!CommandPermissions.allowed(request.kind(),sender::hasPermission)){
            sender.sendMessage("没有权限，需要 "+CommandPermissions.node(request.kind())+" 或 tarot.admin。");return true;
        }
        if(request.kind()==TarotCommand.Kind.HELP){help(sender);return true;}
        boolean admin=switch(request.kind()){case GIVE,RESET_CD,RESET_BUFFER,SELFTEST->true;default->false;};
        if(request.kind()==TarotCommand.Kind.SELFTEST){selftest();sender.sendMessage("TAROT SELFTEST PASS");return true;}
        if(admin){
            Player target=lookup.apply(request.player());
            if(target==null){sender.sendMessage("玩家必须在线："+request.player());return true;}
            switch(request.kind()){
                case GIVE->giveCommand(sender,target,request);
                case RESET_CD->{resetCooldown(target);sender.sendMessage("已重置 "+target.getName()+" 的换牌冷却。");}
                case RESET_BUFFER->{resetBuffer(target);sender.sendMessage("已清除 "+target.getName()+" 的塔罗卡牌效果。");}
                default->throw new AssertionError(request.kind());
            }
            return true;
        }
        if(!(sender instanceof Player player)){sender.sendMessage("该命令需要玩家执行；使用 /tarot help 查看管理指令。");return true;}
        switch(request.kind()){
            case ALBUM->albums.giveOrOpen(player);
            case COLLECTION->albums.status(player);
            case AURA->albums.toggleParticles(player);
            case MIGRATE->message(player,"已更新 "+migrate(player)+" 个旧版卡牌／包装／卡册。");
            case STATUS->status(player);
            default->throw new AssertionError(request.kind());
        }
        return true;
    }
    private void giveCommand(CommandSender sender,Player target,TarotCommand.Request request){
        if(request.item().equals("album")){
            if(albums.giveAlbum(target))sender.sendMessage("已为 "+target.getName()+" 保留一本个人收集册；重复册子自动去重。");
            else sender.sendMessage("发放失败：背包空间不足，请腾出一格。");
            return;
        }
        List<ItemStack> rewards=new ArrayList<>();String description;
        if(request.item().equals("card")){
            int first=request.card()==0?1:request.card(),last=request.card()==0?22:request.card();
            for(int id=first;id<=last;id++)for(int n=0;n<request.count();n++)rewards.add(makeCard(id,request.level()));
            description=(request.card()==0?"全部 22 种":card(request.card()).name)+" "+request.level()+" 星，每种 "+request.count()+" 张（共 "+rewards.size()+" 张）";
        }else{
            rewards.add(makePackage(request.item(),request.count()));description=request.item()+" × "+request.count();
        }
        if(!give(target,rewards)){
            sender.sendMessage("发放失败：背包空间不足，本批次全部取消。"+(request.item().equals("card")?"需要 "+rewards.size()+" 个空格。":""));return;
        }
        sender.sendMessage("已发放给 "+target.getName()+"："+description+"。");
        if(request.item().equals("card")&&target.getGameMode()==GameMode.CREATIVE)creativeMergeHint(target);
    }
    private void status(Player p){
        long now=System.currentTimeMillis(),remaining=effectiveCooldown(p,now);Core.Active s=active.get(p.getUniqueId());
        boolean unlimited=bypass(p);
        if(s==null||(!unlimited&&s.expired(now))){
            message(p,"当前没有激活的塔罗牌。"+(remaining>0?"换牌冷却剩余 "+time(remaining):"主手持牌，长按右键 5 秒激活。"));return;
        }
        message(p,card(s.card()).name+" "+"★".repeat(s.level())+" | 剩余 "+(unlimited?"永久":time(s.remaining(now)))+" | "+(remaining>0?"换牌锁定 "+time(remaining):"可以更换"));
        for(Bonus b:card(s.card()).bonuses)message(p,b.label+" +"+number(b.amount*SCALE[s.level()-1]*(b.multiply?100:1))+(b.multiply?"%（基础值）":""));
    }
    private void help(CommandSender sender){
        sender.sendMessage("彩虹塔罗 · 指令帮助");
        helpLine(sender,TarotCommand.Kind.HELP,"/tarot help — 显示帮助");
        helpLine(sender,TarotCommand.Kind.STATUS,"/tarot status — 查看卡牌效果与换牌冷却");
        helpLine(sender,TarotCommand.Kind.ALBUM,"/tarot album — 打开个人收藏；携带实体册子才有收藏加成");
        helpLine(sender,TarotCommand.Kind.COLLECTION,"/tarot collection — 查看进度与奖励");
        helpLine(sender,TarotCommand.Kind.AURA,"/tarot aura — 开关跟随粒子");
        helpLine(sender,TarotCommand.Kind.MIGRATE,"/tarot migrate — 更新随身旧版物品");
        sender.sendMessage("主手持牌长按右键 5 秒，松手取消蓄力");
        sender.sendMessage("卡包／盒／箱：主手持有，直接右键拆开，无需潜行");
        sender.sendMessage("生存／冒险背包：将同名同星的两张 1–4 星卡重叠升级；卡册内 Shift 点击背包卡牌存入，左键取回");
        helpLine(sender,TarotCommand.Kind.GIVE,"/tarot give <玩家> card <1–22|中文牌名|all> <1–5星> <1–64数量>");
        helpLine(sender,TarotCommand.Kind.GIVE,"/tarot give <玩家> <pack|box|case> <1–64数量>；/tarot give <玩家> album 1");
        helpLine(sender,TarotCommand.Kind.GIVE,"all = 全部 22 种，每种指定数量；整批空间不足会取消，不丢失物品");
        helpLine(sender,TarotCommand.Kind.RESET_CD,"/tarot reset cd <玩家> — 仅重置换牌冷却，保留效果及到期时间");
        helpLine(sender,TarotCommand.Kind.RESET_BUFFER,"/tarot reset buffer <玩家> — 仅清除当前塔罗卡牌效果，保留冷却及收藏奖励");
        helpLine(sender,TarotCommand.Kind.SELFTEST,"/tarot selftest — 执行服务端回归检查");
    }
    private void helpLine(CommandSender sender,TarotCommand.Kind kind,String line){if(CommandPermissions.allowed(kind,sender::hasPermission))sender.sendMessage(line);}
    @Override public List<String> onTabComplete(CommandSender sender,Command cmd,String alias,String[] args) {
        return TarotCommand.complete(args,cardNames(),Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),sender::hasPermission);
    }
    private void selftest() {
        java.util.function.Consumer<Boolean> check=b->{if(!b)throw new IllegalStateException("Tarot inventory test failed");};
        ItemStack[] full=new ItemStack[36];for(int i=0;i<36;i++)full[i]=new ItemStack(Material.STONE,64);
        List<ItemStack> five=java.util.stream.IntStream.range(1,6).mapToObj(i->makeCard(i,1)).toList();
        check.accept(InventoryPlan.deposit(full,five)==null);
        for(int i=0;i<4;i++)full[i]=null;check.accept(InventoryPlan.deposit(full,five)==null);
        check.accept(full[0]==null&&full[4].getAmount()==64);
        full[4]=null;ItemStack[] result=InventoryPlan.deposit(full,five);check.accept(result!=null);
        for(int i=0;i<5;i++)check.accept(cardId(result[i])==i+1&&full[i]==null);
        ItemStack[] filled=new ItemStack[36];for(int i=0;i<36;i++)filled[i]=new ItemStack(Material.STONE,64);
        filled[0]=makePackage("box",16);
        ItemStack[] box=InventoryPlan.deposit(filled,List.of(makePackage("box",48)));check.accept(box!=null&&box[0].getAmount()==64&&filled[0].getAmount()==16);
        filled[0]=makePackage("box",17);check.accept(InventoryPlan.deposit(filled,List.of(makePackage("box",48)))==null);
        for(int id=1;id<=22;id++)for(int level=1;level<=5;level++){
            ItemStack c=makeCard(id,level);check.accept(cardId(c)==id&&value(c,levelKey)==level&&c.getMaxStackSize()==1);
            check.accept(vanillaPaperModel(c.getItemMeta()));
            check.accept(c.getItemMeta().getCustomModelDataComponent().getStrings().equals(List.of("tarot:card_"+id+"_"+level)));
        }
        // Use the exact command-item parser for legacy component coverage.
        var legacy=Bukkit.getItemFactory().createItemStack("minecraft:paper[minecraft:custom_data={tarot:{card:8,level:3}},minecraft:max_stack_size=1]");
        ItemStack updated=migrateItem(legacy);
        check.accept(cardId(updated)==8&&value(updated,levelKey)==3);
        for(String k:List.of("pack","box","case")){
            var old=Bukkit.getItemFactory().createItemStack("minecraft:paper[minecraft:custom_data={tarot:{package:\""+k+"\"}},minecraft:max_stack_size=1]");
            check.accept(packageType(migrateItem(old)).equals(k));
            ItemStack previous=makePackage(k,48);ItemMeta meta=previous.getItemMeta();
            meta.setItemModel(new NamespacedKey("tarot",k));meta.setCustomModelDataComponent(null);previous.setItemMeta(meta);
            ItemStack next=migrateItem(previous);
            check.accept(next.getAmount()==48&&vanillaPaperModel(next.getItemMeta()));
            check.accept(next.getItemMeta().getCustomModelDataComponent().getStrings().equals(List.of("tarot:"+k)));
            check.accept(previous.getItemMeta().getItemModel().getNamespace().equals("tarot")&&migrateItem(next)==next);
        }
        ItemStack previous=makeCard(22,4);ItemMeta meta=previous.getItemMeta();
        meta.setItemModel(new NamespacedKey("tarot","card_22_4"));meta.setCustomModelDataComponent(null);
        meta.displayName(Component.text("kept custom name"));previous.setItemMeta(meta);
        ItemStack next=migrateItem(previous);
        check.accept(cardId(next)==22&&value(next,levelKey)==4&&next.getItemMeta().displayName().equals(meta.displayName()));
        check.accept(vanillaPaperModel(next.getItemMeta())&&migrateItem(next)==next);
        ItemStack oldHold=makeCard(8,3);ItemMeta oldHoldMeta=oldHold.getItemMeta();
        oldHoldMeta.lore(List.of(text("副手持牌 · 长按右键 5 秒激活",0x80ddff),
            text("现实时间计时，离线仍计时；移出副手仍生效",0xa9a9bd),Component.text("custom lore")));
        oldHold.setItemMeta(oldHoldMeta);ChargeController.configureCard(oldHold);
        ItemStack mainHold=migrateItem(oldHold);
        String newLore=net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
            .serialize(Component.join(net.kyori.adventure.text.JoinConfiguration.newlines(),mainHold.getItemMeta().lore()));
        check.accept(newLore.contains("主手持牌")&&newLore.contains("移出主手仍生效")&&!newLore.contains("副手"));
        check.accept(mainHold.getItemMeta().lore().contains(Component.text("custom lore"))&&migrateItem(mainHold)==mainHold);
        UpgradeEventTest.run(this);
        AlbumServiceTest.run(this);
        AlbumMenuTest.run(this);
        CommandEventTest.run(this);
        PackageOpenTest.run(this);
        ChargeControllerTest.run(this);
        ActivationStateTest.run(this);
        ParticleVisualTest.run(this);
        if(placeholderCleanup!=null)PapiIntegrationTest.run(this);
        getLogger().info("TAROT SELFTEST PASS: full/four/five free slots; stack16+48 and17+48; no mutation on reject;110 cards; legacy and V3.0.0 migration; vanilla paper fallback.");
    }
}
