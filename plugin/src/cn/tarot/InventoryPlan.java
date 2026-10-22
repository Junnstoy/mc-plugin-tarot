package cn.tarot;
import java.util.List;
import org.bukkit.inventory.ItemStack;

/** Build an all-or-nothing storage update, never mutating caller stacks. */
public final class InventoryPlan {
    private InventoryPlan() {}
    public static ItemStack[] deposit(ItemStack[] storage, List<ItemStack> rewards) {
        ItemStack[] next=new ItemStack[storage.length];
        for(int i=0;i<storage.length;i++) next[i]=storage[i]==null?null:storage[i].clone();
        for(ItemStack reward:rewards) {
            int remaining=reward.getAmount();
            int max=Math.min(64,reward.getMaxStackSize());
            for(int i=0;i<next.length&&remaining>0;i++) {
                ItemStack s=next[i];
                if(s!=null&&!s.getType().isAir()&&s.isSimilar(reward)) {
                    int moved=Math.min(remaining,Math.max(0,max-s.getAmount()));
                    s.setAmount(s.getAmount()+moved);remaining-=moved;
                }
            }
            for(int i=0;i<next.length&&remaining>0;i++) {
                if(next[i]==null||next[i].getType().isAir()) {
                    int moved=Math.min(remaining,max);next[i]=reward.clone();next[i].setAmount(moved);remaining-=moved;
                }
            }
            if(remaining>0)return null;
        }
        return next;
    }
}
