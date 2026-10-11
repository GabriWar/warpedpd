package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;

/**
 * Status silhouettes, each made of only what its kind needs: a chain (or a cut's link) sagging
 * between its bearer and its anchor, swaying and glinting where it is fixed; frost shards turning
 * a quarter at a time beside the head, catching the light; dazed stars circling over the head; a
 * guardian's bubble breathing about the bearer; and the rest (shards' charges, radiance, a leap's
 * or a lunge's mark) as motes in the kit's MARK orbit over the head, as many as their power
 * (several marks on one bearer share that orbit, each its motes in its own colour).
 * At most QUADS quads. The linked character is looked up every LOOKUP s, not every frame.
 */
public class SkillMarkFX extends Group {

    /** The most quads one mark draws. */
    public static final int QUADS=12;
    /** Chain links; how often the linked character is looked up, s; an anchor's spark, s. */
    public static final int LINKS=12;
    public static final float LOOKUP=.25f, SPARK_EVERY=1.5f;
    /** Frost shards turn a quarter this often, s; stagger stars circle at this rate, rad/s. */
    public static final float SHARD_TURN=.25f, STAR_SPIN=4f;
    /** The guard's bubble: its alpha, how far it breathes, how fast. */
    public static final float BUBBLE_A=.3f, BUBBLE_SWING=.1f, BUBBLE_HZ=.7f;

    private final SkillInteractions.Mark state;
    private final boolean chain,linked;
    //the chain's links (matter, with the sprites)
    private final Image[] links;
    //what shines (in the light layer): stars, the bubble, the shards' glint
    private final FxPic[] lit;
    //the frost shards (matter)
    private final FxPic[] shards;
    private final int color;
    //the motes over the head, in the MARK orbit it shares with the bearer's other marks
    private final MarkOrbit orbit;
    private float time,lookup,spark;
    private Char other;
    private boolean focusedIcon,litAdded;

    public SkillMarkFX(SkillInteractions.Mark state){
        this.state=state;
        int kind=state.kind;
        linked=kind==SkillInteractions.Mark.CUT;
        chain=kind==SkillInteractions.Mark.CHAIN||linked;
        color=kind==SkillInteractions.Mark.FROST?0xA4E9FF:kind==SkillInteractions.Mark.CUT?0xD94969:
                kind==SkillInteractions.Mark.STAGGER?0xCE9765:kind==SkillInteractions.Mark.GUARD?0xCCDEFF:0xFFE7A4;
        if(chain){
            links=new Image[LINKS];
            for(int i=0;i<LINKS;i++){links[i]=Effects.get(Effects.Type.ROPE);add(links[i]);}
        }else links=null;
        if(kind==SkillInteractions.Mark.FROST){
            shards=new FxPic[2];
            for(int i=0;i<shards.length;i++){
                shards[i]=new FxPic(SkillVfx.SHEET);shards[i].show(FxFrames.SHARD_ICE[i%2]);
                shards[i].origin.set(1,2);shards[i].lit(0xE8F6FF);add(shards[i]);
            }
            lit=new FxPic[]{star(0xFFFFFF)};
        }else if(kind==SkillInteractions.Mark.STAGGER){
            shards=null;
            int c=Ramp.lerp(color,0xFFF0C0,.5f);
            lit=new FxPic[]{star(c),star(c),star(c)};
        }else if(kind==SkillInteractions.Mark.GUARD){
            shards=null;
            FxPic bubble=new FxPic(SkillVfx.LIGHT_SHEET);bubble.additive=true;bubble.show(FxFrames.Light.BUBBLE_23);bubble.lit(color);
            lit=new FxPic[]{bubble};
        }else{
            shards=null;lit=null;
        }
        if(state.target!=null&&motes()>0)add(orbit=new MarkOrbit());
        else orbit=null;
    }

    private final class MarkOrbit extends SkillOrbit{
        MarkOrbit(){super(state.target,FxOrbit.Slot.MARK);}
        @Override protected int count(){return motes();}
        @Override protected int tint(){return color;}
        @Override protected void look(FxOrbit.Ring ring,int n){
            ring.motes(n,FxFrames.MOTE_2,color,true).trail(n<=2?1:0).breath(.75f,1f,.8f).speed(2.2f);
        }
    }

    private static FxPic star(int c){
        FxPic p=new FxPic(SkillVfx.SHEET);p.additive=true;p.show(FxFrames.STAR4_5);p.lit(c);p.visible=false;return p;
    }

    //how many motes the MARK orbit carries, by kind and power (none for the drawn kinds)
    private int motes(){
        switch(state.kind){
            case SkillInteractions.Mark.SHARDS:case SkillInteractions.Mark.GUARD:
                return Math.min(3,Math.max(1,(state.power+5)/6));
            case SkillInteractions.Mark.CHAIN:case SkillInteractions.Mark.CUT:
            case SkillInteractions.Mark.FROST:case SkillInteractions.Mark.STAGGER:
                return 0;
            default:return 2;
        }
    }

    @Override public void update(){
        super.update();
        time+=Game.elapsed;
        CharSprite s=state.target==null?null:state.target.sprite;
        if(s==null||!s.exists){Fx.dispose(this);return;}
        if(lit!=null&&!litAdded){
            litAdded=true;
            Group lights=GameScene.layer(GameScene.Layer.LIGHTS);
            for(FxPic p:lit)if(lights!=null)lights.add(p);
        }
        visible=s.visible;
        float cx=SkillVfx.midX(s),cy=SkillVfx.midY(s);
        if(chain){chain(s,cx,cy);return;}
        if(shards!=null)frost(s,cx);
        else if(state.kind==SkillInteractions.Mark.STAGGER)stagger(s,cx);
        else if(state.kind==SkillInteractions.Mark.GUARD)guard(s,cx,cy);
    }

    private void chain(CharSprite s,float cx,float cy){
        lookup-=Game.elapsed;
        if(linked&&lookup<=0){
            lookup=LOOKUP;
            Actor a=Actor.findById(state.other);
            other=a instanceof Char?(Char)a:null;
        }
        if(linked&&(other==null||!other.isAlive()||other.sprite==null||!other.sprite.visible||state.target.id()>other.id())){
            visible=false;return;
        }
        if(linked&&other==state.target){
            if(!focusedIcon){links[0].copy(Icons.get(Icons.TARGET));focusedIcon=true;}
            for(int i=0;i<links.length;i++)links[i].visible=i==0;
            links[0].x=cx-links[0].width()/2;links[0].y=cy-links[0].height()/2;
            links[0].hardlight(0xD66AAB);links[0].alpha(.7f);return;
        }
        if(!linked&&!SkillInteractions.valid(state.cell)){for(Image l:links)l.visible=false;return;}
        float ax=linked?SkillVfx.midX(other.sprite):SkillVfx.cx(state.cell);
        float ay=linked?SkillVfx.midY(other.sprite):SkillVfx.cy(state.cell);
        float dx=ax-cx,dy=ay-cy,len=(float)Math.sqrt(dx*dx+dy*dy);
        float sag=sag(len);
        float sway=(float)Math.sin(time*Math.PI)*1f;
        float alpha=.85f+.1f*(float)Math.sin(time*4*Math.PI);
        for(int i=0;i<links.length;i++){
            Image link=links[i];
            float t=(i+1f)/(links.length+1f);
            float lx=cx+dx*t,ly=cy+dy*t+catenary(t)*sag;
            float t2=Math.min(1f,t+.04f);
            float nx=cx+dx*t2,ny=cy+dy*t2+catenary(t2)*sag;
            link.x=Math.round(lx+sway*catenary(t)*1f)-link.width()/2;link.y=Math.round(ly)-link.height()/2;
            link.angle=(float)Math.toDegrees(Math.atan2(ny-ly,nx-lx))+90;
            if(linked)link.hardlight(0xD66AAB);
            link.alpha(alpha);link.visible=true;
        }
        //where it is fixed, now and then a glint
        spark-=Game.elapsed;
        if(spark<=0){
            spark=SPARK_EVERY*FxRandom.Float(.8f,1.2f);
            FxParticle p=SkillVfx.glow(ax,ay,FxBudget.P3);
            if(p!=null)p.look(FxFrames.PLUS_3).paint(linked?0xFFB0D0:0xFFF0C0).life(.25f).alpha(1f,0,.15f).snap().source(ax,ay);
        }
    }

    /** How far a chain of this length sags at its middle, px. */
    public static float sag(float length){
        return Math.min(6f,2f+length*.05f);
    }

    /** The chain's hang at a share t of its length: 0 at both ends, 1 at its middle (a parabola,
     *  the catenary near enough at these sags). */
    public static float catenary(float t){
        return 4f*t*(1f-t);
    }

    private void frost(CharSprite s,float cx){
        float headY=s.y+2;
        int turns=(int)(time/SHARD_TURN);
        for(int i=0;i<shards.length;i++){
            FxPic p=shards[i];
            float a=time*1.7f+i*(float)Math.PI;
            p.place(cx+(float)Math.cos(a)*5f,headY+(float)Math.sin(a)*2f);
            p.angle=90*((turns+i)%4);
            p.visible=s.visible;
        }
        //a glint on one shard, a moment in every 1.2 s
        FxPic g=lit[0];
        float phase=time%1.2f;
        g.visible=s.visible&&phase<.12f;
        if(g.visible){
            FxPic on=shards[((int)(time/1.2f))%2];
            g.place(on.x+on.width/2f,on.y+on.height/2f);
            g.am=1f-phase/.12f;
        }
    }

    private void stagger(CharSprite s,float cx){
        float y=s.y-1;
        for(int i=0;i<lit.length;i++){
            float a=time*STAR_SPIN+i*(float)(Math.PI*2/lit.length);
            lit[i].place(cx+(float)Math.cos(a)*8f,y+(float)Math.sin(a)*3f);
            //the far side dimmer
            lit[i].am=Math.sin(a)<0?.55f:1f;
            lit[i].visible=s.visible;
        }
    }

    private void guard(CharSprite s,float cx,float cy){
        FxPic b=lit[0];
        b.place(cx,cy);
        b.am=BUBBLE_A+BUBBLE_SWING*(float)Math.sin(time*2*Math.PI*BUBBLE_HZ);
        b.visible=s.visible;
    }

    private void release(){
        if(orbit!=null)orbit.kill();
        if(lit!=null)for(FxPic p:lit)Fx.dispose(p);
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
