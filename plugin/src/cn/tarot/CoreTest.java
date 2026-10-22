package cn.tarot;
import java.util.Arrays;
public final class CoreTest {
    private static void check(boolean b) { if (!b) throw new AssertionError(); }
    public static void main(String[] args) {
        long start=12345678L;
        Core.Active s=new Core.Active(1,1,start,start+86400000L,start+1800000L);
        check(s.locked(start)); check(s.locked(start+1799999)); check(!s.locked(start+1800000));
        check(!s.expired(start+86399999)); check(s.expired(start+86400000));
        check(s.remaining(start+90000000)==0); check(!s.locked(start+90000000));
        // Epoch timestamps survive logout/restart; reconstructing does not renew the lease.
        Core.Active restored=new Core.Active(s.card(),s.level(),s.activated(),s.expires(),s.unlocks());
        check(restored.remaining(start+3600000)==82800000);
        for(int id=1;id<=22;id++) for(int level=1;level<=5;level++) {
            check(Core.canMerge(id,level,id,level,1,1)==(level<5));
            check(!Core.canMerge(id,level,id%22+1,level,1,1));
            check(!Core.canMerge(id,level,id,level,2,1));
            check(!Core.canMerge(id,level,id,level%5+1,1,1));
        }
        int[] normal=new int[5], guarantee=new int[5];
        for(int roll=0;roll<10000;roll++) {normal[Core.rarity(roll,false)]++;guarantee[Core.rarity(roll,true)]++;}
        check(Arrays.equals(normal,new int[]{8000,1800,170,25,5}));
        check(Arrays.equals(guarantee,new int[]{0,9400,500,90,10}));
        System.out.println("PASS: deadline boundaries, persistent epoch semantics, 110 merge variants, 20000 probability positions");
    }
}
