package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

/**
 * A skill's field on the floor, under everyone: its painted mark on every cell, breathing gently,
 * and the life of its kind rising off it, at most one particle a cell every EVERY s and CAP for
 * every field at once: embers and a pool of heat (a fragment field's first POOLS cells), glints
 * and mist (ice), an arrow now and then twitching a pixel (arrows), the glyph turning a quarter
 * at a time and motes (sanctuary), streaks of wind, soul wisps, slow smoke, a trickle of dust off
 * a crack. Placed at world positions once (the floor slides with the overworld's window); it
 * reads the cell under each mark when it needs one.
 */
public class SkillFieldFX extends Group {

    /** A cell's particle at most this often, s; every field's particles at most CAP at once. */
    public static final float EVERY=.5f, MAX_LIFE=1.2f;
    public static final int CAP=60;
    /** Fragment fields: heat pools on at most this many cells. */
    public static final int POOLS=8;
    /** The marks breathe about this alpha, this far. */
    public static final float ALPHA=.7f, BREATH=.08f;

    //when each of the CAP particles any field threw dies (game seconds): a free one has passed
    private static final float[] DIES=new float[CAP];

    private final SkillField field;
    private final FxPic[] marks;
    private final float[] next;
    private final FxLight[] heat;
    private float time;

    public SkillFieldFX(SkillField field){
        this.field=field;
        int n=field.cells.length;
        marks=new FxPic[n];
        next=new float[n];
        for(int i=0;i<n;i++){
            FxPic mark=new FxPic("effects/skill_fields.png");
            mark.crop(new int[]{field.kind*16,0,16,16},0,0,16,16);
            mark.origin.set(8,8);
            mark.place(SkillVfx.cx(field.cells[i]),SkillVfx.cy(field.cells[i]));
            marks[i]=mark;add(mark);
            next[i]=FxRandom.Float(EVERY);
        }
        heat=field.kind==SkillField.FRAGMENT?new FxLight[Math.min(POOLS,n)]:null;
    }

    /** Onto the floor's layer (render thread; posted there from another). */
    public static void show(final SkillFieldFX fx){
        Fx.post(()->{
            Group floor=GameScene.layer(GameScene.Layer.FLOOR);
            if(floor!=null)floor.add(fx);
        });
    }

    /** A free place among every field's CAP particles, taken for `life` s: false when all are
     *  living. */
    static boolean take(float now,float life){
        synchronized(DIES){
            for(int i=0;i<CAP;i++){
                //free once it has died, or when it lies further ahead than any life (the scene's
                //clock started again)
                if(DIES[i]<=now||DIES[i]>now+MAX_LIFE){DIES[i]=now+life;return true;}
            }
            return false;
        }
    }

    /** Every place free again (a new scene, a test). */
    public static void clearCap(){
        synchronized(DIES){java.util.Arrays.fill(DIES,0f);}
    }

    @Override public void update(){
        super.update();
        float dt=Game.elapsed;
        time+=dt;
        if(!field.sameFloor()){visible=false;return;}
        visible=true;
        for(int i=0;i<marks.length;i++){
            FxPic m=marks[i];
            m.takeSlide();
            float x=m.midX(),y=m.midY();
            int cell=Fx.cellAt(x,y);
            boolean seen=SkillVfx.seen(cell);
            m.visible=seen;
            m.am=ALPHA+BREATH*(float)Math.sin(time*2.4f+i*.6f);
            if(field.kind==SkillField.SANCTUARY){
                //the glyph turns a quarter every half second
                m.angle=90*((int)((time+i*.13f)/.5f)%4);
            }else if(field.kind==SkillField.ARROWS){
                //now and then an arrow twitches a pixel, for a frame or two
                float beat=(time+i*1.37f)%(2f+(i%3));
                m.place(x,y);
                if(beat<.08f)m.nudge(0,1);
            }
            if(heat!=null&&i<heat.length){
                if(seen&&heat[i]==null){
                    heat[i]=FxLight.pool(x,y+3,23,Element.FIRE.light,.6f).persistent().flicker(FxLight.HEARTH);
                }else if(!seen&&heat[i]!=null){
                    heat[i].putOut(.3f);heat[i]=null;
                }
            }
            next[i]-=dt;
            if(next[i]>0)continue;
            next[i]=EVERY*FxRandom.Float(1f,1.6f);
            if(seen)emit(x,y);
        }
    }

    private void emit(float x,float y){
        float now=Game.timeTotal;
        FxParticle p;
        switch(field.kind){
            case SkillField.FRAGMENT:
                if(!take(now,1.2f))return;
                p=SkillVfx.glow(x+FxRandom.Float(-5f,5f),y+FxRandom.Float(-2f,4f),FxBudget.P3);
                if(p!=null)p.look(FxRandom.chance(.7f)?FxFrames.EMBER_1:FxFrames.EMBER_2).ramp(Element.FIRE.ember,true)
                        .vel(FxRandom.Float(-3f,3f),FxRandom.Float(-14f,-8f)).sway(2f,5f).drag(.8f).life(FxRandom.Float(.8f,1.2f))
                        .alpha(1f,0,.4f);
                break;
            case SkillField.ICE:
                if(!take(now,1f))return;
                if(FxRandom.chance(.5f)){
                    p=SkillVfx.glow(x+FxRandom.Float(-6f,6f),y+FxRandom.Float(-5f,5f),FxBudget.P3);
                    if(p!=null)p.look(FxRandom.chance(.6f)?FxFrames.PLUS_3:FxFrames.STAR4_5).paint(Element.FROST.light)
                            .life(.35f).alpha(.9f,.08f,.2f).snap();
                }else{
                    p=SkillVfx.onFloor(x+FxRandom.Float(-4f,4f),y+FxRandom.Float(-2f,4f),FxBudget.P3);
                    if(p!=null)p.look(FxFrames.EDGE_WISP[FxRandom.Int(2)]).paint(Element.FROST.smoke.step(0f))
                            .vel(FxRandom.Float(-4f,4f),0).life(1f).alpha(.35f,.3f,.5f).smooth();
                }
                break;
            case SkillField.SANCTUARY:
                if(!take(now,.9f))return;
                p=SkillVfx.glow(x+FxRandom.Float(-6f,6f),y+FxRandom.Float(-3f,4f),FxBudget.P3);
                if(p!=null)p.look(FxFrames.MOTE_1).ramp(Element.HOLY.ember,true).vel(0,FxRandom.Float(-14f,-9f)).drag(.6f)
                        .life(.9f).alpha(.9f,.1f,.4f);
                break;
            case SkillField.WIND:
                if(!take(now,.8f))return;
                p=SkillVfx.matter(x-8,y+FxRandom.Float(-6f,6f),FxBudget.P3);
                if(p!=null)p.look(FxFrames.EDGE_WISP[FxRandom.Int(2)]).paint(0xFFFFFF).vel(20f,0).life(.8f)
                        .alpha(.4f,.15f,.3f).smooth();
                break;
            case SkillField.SOUL:
                if(!take(now,1.1f))return;
                p=SkillVfx.glow(x+FxRandom.Float(-5f,5f),y+FxRandom.Float(-2f,4f),FxBudget.P3);
                if(p!=null)p.look(FxRandom.chance(.5f)?FxFrames.MOTE_1:FxFrames.MOTE_2).ramp(Element.SACRIFICIAL.ember,true)
                        .vel(0,FxRandom.Float(-10f,-6f)).sway(3f,3f).life(1.1f).alpha(.8f,.2f,.5f);
                break;
            case SkillField.SMOKE:
                if(!take(now,1.2f))return;
                p=SkillVfx.matter(x+FxRandom.Float(-4f,4f),y,FxBudget.P3);
                if(p!=null)p.look(FxFrames.PUFF_M[FxRandom.Int(2)]).paint(Element.SMOKE.ramp.step(.2f))
                        .vel(FxRandom.Float(-2f,2f),FxRandom.Float(-5f,-3f)).life(1.2f).alpha(.45f,.3f,.6f).smooth();
                break;
            case SkillField.CRACK:
                if(!take(now,.6f))return;
                p=SkillVfx.matter(x+FxRandom.Float(-5f,5f),y-FxRandom.Float(0f,3f),FxBudget.P3);
                if(p!=null)p.look(FxRandom.chance(.5f)?FxFrames.PEBBLE_2:FxFrames.MOTE_1).paint(Element.EARTH.ramp.stop(2))
                        .vel(FxRandom.Float(-3f,3f),FxRandom.Float(-10f,-4f)).gravity(160f).ground(y+4,FxParticle.SETTLE,0)
                        .life(.6f).alpha(1f,0,.2f);
                break;
            default:
                break;
        }
    }

    private void release(){
        if(heat!=null)for(int i=0;i<heat.length;i++)if(heat[i]!=null){heat[i].putOut(.3f);heat[i]=null;}
    }

    @Override public void kill(){
        release();
        super.kill();
    }

    @Override public void destroy(){
        release();
        super.destroy();
    }
}
