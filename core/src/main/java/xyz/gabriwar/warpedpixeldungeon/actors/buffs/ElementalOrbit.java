package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.effects.ElementalOrbitFX;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

/** Per-orb state survives saves and level transitions. Heat advances on game turns only. */
public class ElementalOrbit extends Buff {
    public static final int MAX_HEAT = 3, MAX_ORBS = 5;
    private static final int[] STAGES={0,5,15,30};
    private int[] elements = new int[0], heat = new int[0], ages = new int[0];
    public int skillLevel;
    public int flareSerial;
    private ElementalOrbitFX visual;
    { type = buffType.POSITIVE; }

    public ElementalOrbit set(int level){
        boolean fresh=elements.length==0;
        skillLevel = Math.max(1, Math.min(3, level));
        elements = new int[MAX_ORBS];
        heat = new int[elements.length];
        ages = new int[elements.length];
        for (int i = 0; i < elements.length; i++) elements[i] = Random.Int(2);
        if(fresh)spend(TICK);
        return this;
    }
    public int count(){ return elements.length; }
    public int element(int i){ return elements[i]; }
    public int heat(int i){ return heat[i]; }
    public void warm(){
        for(int i=0;i<heat.length;i++){
            ages[i]=Math.min(30,ages[i]+1);
            heat[i]=ages[i]>=30?3:ages[i]>=15?2:ages[i]>=5?1:0;
        }
    }
    private void flare(){
        for(int i=0;i<heat.length;i++){
            heat[i]=Math.min(MAX_HEAT,heat[i]+1);
            ages[i]=Math.max(ages[i],STAGES[heat[i]]);
        }
        flareSerial++;
    }
    public int count(int element){
        int count=0;for(int value:elements)if(value==element)count++;return count;
    }
    /** Removes the oldest orb of the chosen element, preserving every other orb's heat. */
    public int[] launch(int element){
        int index=-1;
        for(int i=0;i<elements.length;i++)if(elements[i]==element){index=i;break;}
        if(index<0)return null;
        int[] shot={elements[index],heat[index],skillLevel};
        int[] remaining=new int[elements.length-1], warmth=new int[heat.length-1], elapsed=new int[ages.length-1];
        for(int i=0,j=0;i<elements.length;i++)if(i!=index){
            remaining[j]=elements[i];elapsed[j]=ages[i];warmth[j++]=heat[i];
        }
        elements=remaining;heat=warmth;ages=elapsed;
        if(skillLevel>=3&&shot[1]==MAX_HEAT)flare();
        if(count()==0&&target!=null)detach();
        return shot;
    }
    public int[] launch(){return count()==0?null:launch(elements[0]);}
    @Override public boolean act(){
        if (count() == 0){ detach(); return true; }
        warm(); spend(TICK); return true;
    }
    @Override public void fx(boolean on){
        if (visual != null){ visual.killAndErase(); visual = null; }
        if (on && target.sprite != null && target.sprite.parent != null){
            visual = new ElementalOrbitFX(target, this);
            target.sprite.parent.add(visual);
        }
    }
    @Override public int icon(){ return BuffIndicator.IMBUE; }
    @Override public String iconTextDisplay(){ return Integer.toString(count()); }
    @Override public String desc(){ return Messages.get(this, "desc", count(), count() > 0 ? heat[0] : 0); }
    @Override public void storeInBundle(Bundle b){
        super.storeInBundle(b); b.put("elements", elements); b.put("heat", heat); b.put("ages",ages); b.put("skill_level", skillLevel);
    }
    @Override public void restoreFromBundle(Bundle b){
        super.restoreFromBundle(b); elements = b.getIntArray("elements"); heat = b.getIntArray("heat"); skillLevel = b.getInt("skill_level");
        if (elements.length != heat.length){ elements = new int[0]; heat = new int[0]; }
        if(elements.length>MAX_ORBS){elements=java.util.Arrays.copyOf(elements,MAX_ORBS);heat=java.util.Arrays.copyOf(heat,MAX_ORBS);}
        ages=b.contains("ages")?b.getIntArray("ages"):new int[0];
        if(ages.length!=heat.length){
            ages=new int[heat.length];
            for(int i=0;i<heat.length;i++)ages[i]=STAGES[Math.max(0,Math.min(MAX_HEAT,heat[i]))];
        }
    }
}
