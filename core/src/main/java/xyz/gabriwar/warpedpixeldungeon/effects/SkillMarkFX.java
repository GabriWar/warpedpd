package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxCurves;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LinkFX;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * Status silhouettes, each made of only what its kind needs: a chain sagging between its bearer and
 * its anchor, swaying and glinting where it is fixed (Bear Trap's real chain); Shadow Link's pair
 * bound by a held bond of shadow (LinkFX.held: links of violet-black light, motes fed from both
 * bodies, a crimson glint where they meet: the shared cut), a lone linked enemy a violet ring
 * breathing at its feet (which one is linked, never a target's crosshair); frost shards turning
 * a quarter at a time beside the head, catching the light; a stagger as footing lost (a ring of
 * light on the floor under its feet rocking left and right, dust skidding out on the side it rocks
 * to: never stars over the head, which are a real stun's); a guardian's bubble breathing about the
 * bearer; and the rest (shards' charges, radiance, a leap's or a lunge's mark) as motes in the
 * kit's MARK orbit over the head, as many as their power (several marks on one bearer share that
 * orbit, each its motes in its own colour).
 * At most QUADS quads. The linked character is looked up every LOOKUP s, not every frame.
 * Also tether(): a one-off line of links of light thrown between two characters (a bind).
 */
public class SkillMarkFX extends Group {

    /** The most quads one mark draws. */
    public static final int QUADS=12;
    /** Chain links; how often the linked character is looked up, s; an anchor's spark, s. */
    public static final int LINKS=12;
    public static final float LOOKUP=.25f, SPARK_EVERY=1.5f;
    /** Frost shards turn a quarter this often, s. */
    public static final float SHARD_TURN=.25f;
    /** The stagger: its ring's colour and alpha, how far it rocks (whole px) and how fast (Hz), how
     *  often dust skids out from under it (s, so at most SKID_MOST puffs at once). */
    public static final int STAGGER_RING=0xE0B98A, STAGGER_ROCK=2, SKID_MOST=2;
    public static final float STAGGER_ALPHA=.7f, STAGGER_HZ=2.4f, SKID_EVERY=.6f, SKID_LIFE=.45f;
    /** The guard's bubble: its alpha, how far it breathes, how fast. */
    public static final float BUBBLE_A=.3f, BUBBLE_SWING=.1f, BUBBLE_HZ=.7f;
    /** A cut's colour (a shared wound: SkillPalette.BLOOD's crimson); a lone linked enemy's ring
     *  (SkillPalette.SHADOW's A070E0, a RING_13: wide enough to show round a body, never a target's
     *  brackets) at its feet, its alpha breathing FOCUS_A +- FOCUS_SWING at FOCUS_HZ. */
    public static final int CUT_COLOR=0xD01818, FOCUS_RING=0xA070E0;
    public static final float FOCUS_A=.45f, FOCUS_SWING=.1f, FOCUS_HZ=.6f;

    private final SkillInteractions.Mark state;
    private final boolean chain,linked;
    //the chain's links (matter, with the sprites)
    private final Image[] links;
    //a linked pair's held bond (LinkFX.held, in the light layer) and the sprites it was made for
    private Gizmo bond;
    private CharSprite bondA,bondB;
    //what shines (in the light layer): the bubble, the shards' glint
    private final FxPic[] lit;
    //what lies on the floor under the bearer (the stagger's ring)
    private final FxPic ground;
    //the frost shards (matter)
    private final FxPic[] shards;
    private final int color;
    //the motes over the head, in the MARK orbit it shares with the bearer's other marks
    private final MarkOrbit orbit;
    private float time,lookup,spark,skid=SKID_EVERY;
    private Char other;
    private boolean litAdded,groundAdded;

    public SkillMarkFX(SkillInteractions.Mark state){
        this.state=state;
        int kind=state.kind;
        linked=kind==SkillInteractions.Mark.CUT;
        chain=kind==SkillInteractions.Mark.CHAIN;
        color=kind==SkillInteractions.Mark.FROST?0xA4E9FF:kind==SkillInteractions.Mark.CUT?CUT_COLOR:
                kind==SkillInteractions.Mark.STAGGER?0xCE9765:kind==SkillInteractions.Mark.GUARD?0xCCDEFF:0xFFE7A4;
        if(chain){
            links=new Image[LINKS];
            for(int i=0;i<LINKS;i++){links[i]=Effects.get(Effects.Type.ROPE);add(links[i]);}
        }else links=null;
        FxPic onFloor=null;
        if(kind==SkillInteractions.Mark.FROST){
            shards=new FxPic[2];
            for(int i=0;i<shards.length;i++){
                shards[i]=new FxPic(SkillVfx.SHEET);shards[i].show(FxFrames.SHARD_ICE[i%2]);
                shards[i].origin.set(1,2);shards[i].lit(0xE8F6FF);add(shards[i]);
            }
            lit=new FxPic[]{star(0xFFFFFF)};
        }else if(kind==SkillInteractions.Mark.STAGGER){
            //footing lost: a ring of light under its feet (on the floor, under the body)
            shards=null;lit=null;
            onFloor=new FxPic(SkillVfx.SHEET);onFloor.additive=true;onFloor.show(FxFrames.RING_13);
            onFloor.lit(STAGGER_RING);onFloor.am=STAGGER_ALPHA;onFloor.visible=false;
        }else if(linked){
            //the focus: a ring of shadow at its feet (on the floor, under the body); the pair's bond is
            //made on the render thread once its partner is found
            shards=null;lit=null;
            onFloor=new FxPic(SkillVfx.SHEET);onFloor.additive=true;onFloor.show(FxFrames.RING_13);
            onFloor.lit(FOCUS_RING);onFloor.am=FOCUS_A;onFloor.visible=false;
        }else if(kind==SkillInteractions.Mark.GUARD){
            shards=null;
            FxPic bubble=new FxPic(SkillVfx.LIGHT_SHEET);bubble.additive=true;bubble.show(FxFrames.Light.BUBBLE_23);bubble.lit(color);
            lit=new FxPic[]{bubble};
        }else{
            shards=null;lit=null;
        }
        ground=onFloor;
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
        if(ground!=null&&!groundAdded){
            groundAdded=true;
            Group floor=GameScene.layer(GameScene.Layer.FLOOR);
            if(floor!=null)floor.add(ground);
        }
        visible=s.visible;
        float cx=SkillVfx.midX(s),cy=SkillVfx.midY(s);
        if(linked){cut(s,cx);return;}
        if(chain){chain(s,cx,cy);return;}
        if(shards!=null)frost(s,cx);
        else if(state.kind==SkillInteractions.Mark.STAGGER)stagger(s,cx);
        else if(state.kind==SkillInteractions.Mark.GUARD)guard(s,cx,cy);
    }

    //Shadow Link: the pair's bond held between them by the one of the two with the lower id (a held
    //LinkFX, made once a partner is found, let go of when it changes, dies or the mark goes); a lone
    //linked enemy (the focus) shows a ring of shadow breathing at its feet instead
    private void cut(CharSprite s,float cx){
        lookup-=Game.elapsed;
        if(lookup<=0){
            lookup=LOOKUP;
            Actor a=Actor.findById(state.other);
            other=a instanceof Char?(Char)a:null;
        }
        boolean alone=other==state.target;
        if(alone){
            letBond();
            ground.place(cx,s.y+s.height()-3);
            ground.am=FOCUS_A+FOCUS_SWING*(float)Math.sin(2*Math.PI*FOCUS_HZ*time);
            ground.visible=s.visible;
            return;
        }
        ground.visible=false;
        if(other==null||!other.isAlive()||other.sprite==null||!other.sprite.exists||state.target.id()>other.id()){
            letBond();
            return;
        }
        if(bond==null||bondA!=s||bondB!=other.sprite){
            letBond();
            bond=LinkFX.held(s,other.sprite,LinkFX.SHADOW);
            bondA=s;
            bondB=other.sprite;
        }
    }

    private void letBond(){
        if(bond!=null){
            Fx.dispose(bond);
            bond=null;
        }
        bondA=bondB=null;
    }

    private void chain(CharSprite s,float cx,float cy){
        if(!SkillInteractions.valid(state.cell)){for(Image l:links)l.visible=false;return;}
        float ax=SkillVfx.cx(state.cell);
        float ay=SkillVfx.cy(state.cell);
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
            link.alpha(alpha);link.visible=true;
        }
        //where it is fixed, now and then a glint
        spark-=Game.elapsed;
        if(spark<=0){
            spark=SPARK_EVERY*FxRandom.Float(.8f,1.2f);
            FxParticle p=SkillVfx.glow(ax,ay,FxBudget.P3);
            if(p!=null)p.look(FxFrames.PLUS_3).paint(0xFFF0C0).life(.25f).alpha(1f,0,.15f).snap().source(ax,ay);
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

    /** How far (whole px) the stagger's ring has rocked `time` s in: STAGGER_ROCK either way on
     *  a STAGGER_HZ sine. */
    public static int rock(float time){
        return Math.round(STAGGER_ROCK*(float)Math.sin(2*Math.PI*STAGGER_HZ*time));
    }

    //off balance: the ring under its feet rocks left and right, and now and then dust skids out
    //on the side it is rocking toward (a body shifting its weight, never stars over its head)
    private void stagger(CharSprite s,float cx){
        float feet=s.y+s.height()-3;
        ground.place(cx+rock(time),feet);
        ground.visible=s.visible;
        skid-=Game.elapsed;
        if(skid>0)return;
        skid=SKID_EVERY;
        if(!s.visible||!Fx.seen(cx,feet))return;
        //the way it is rocking now (the sine's slope)
        float side=Math.cos(2*Math.PI*STAGGER_HZ*time)>=0?1f:-1f;
        float x=cx+side*4f;
        FxParticle p=SkillVfx.matter(x,feet,FxBudget.P3);
        if(p!=null)p.look(FxFrames.PUFF_S[FxRandom.Int(2)]).paint(Element.EARTH.smoke.step(.4f))
                .vel(side*FxRandom.Float(10f,16f),FxRandom.Float(-3f,-1f)).drag(3f).life(SKID_LIFE)
                .alpha(.5f,0,.25f).source(x,feet);
    }

    private void guard(CharSprite s,float cx,float cy){
        FxPic b=lit[0];
        b.place(cx,cy);
        b.am=BUBBLE_A+BUBBLE_SWING*(float)Math.sin(time*2*Math.PI*BUBBLE_HZ);
        b.visible=s.visible;
    }

    private void release(){
        letBond();
        if(orbit!=null)orbit.kill();
        if(lit!=null)for(FxPic p:lit)Fx.dispose(p);
        if(ground!=null)Fx.dispose(ground);
    }

    // ------------------------------------------------------------------ the tether

    /** A tether's links (quads), how long it takes to be thrown out (s), its sag thrown and drawn tight (px). */
    public static final int TETHER_LINKS=12;
    public static final float TETHER_OUT=.08f, TETHER_SAG=5f, TETHER_TIGHT=1.5f;

    /** A one-off tether between two characters for `life` s: a line of links of light in a colour
     *  (every other one paler) thrown out from a to b in TETHER_OUT s, sagging and drawn tight as
     *  it goes, a glint where it takes hold, held, then fading over the last 40% of its life. It
     *  follows both. Never a chain item's or the Ethereal Chains' look. Any thread. */
    public static void tether(Char a,Char b,int color,float life){
        tether(a,b,color,life,0);
    }

    /** The same, `offset` px below both middles (two tethers side by side). */
    public static void tether(final Char a,final Char b,final int color,final float life,final float offset){
        if(a==null||b==null)return;
        final Tether t=new Tether(a,b,color,life,offset);
        Fx.post(()->{
            if(a.sprite==null||a.sprite.parent==null||GameScene.layer(GameScene.Layer.LIGHTS)==null)return;
            GameScene.light(t);
        });
    }

    /** Where a tether thrown `time` s ago has reached, 0..1 of the way. */
    public static float tetherOut(float time){
        return FxCurves.easeOut(Math.min(1f,Math.max(0f,time/TETHER_OUT)));
    }

    private static final class Tether extends Group{
        private final Char a,b;
        private final float life,offset;
        private final FxPic[] links=new FxPic[TETHER_LINKS];
        private float time;
        private boolean held;

        Tether(Char a,Char b,int color,float life,float offset){
            this.a=a;this.b=b;
            this.life=Math.max(.1f,life);
            this.offset=offset;
            int pale=Ramp.lerp(color,0xFFFFFF,.45f);
            for(int i=0;i<TETHER_LINKS;i++){
                FxPic p=new FxPic(SkillVfx.SHEET);p.additive=true;p.show(FxFrames.SPARK_3);
                p.origin.set(.5f,1.5f);p.lit(i%2==0?color:pale);p.visible=false;
                links[i]=p;add(p);
            }
        }

        @Override public void update(){
            super.update();
            time+=Game.elapsed;
            CharSprite sa=a.sprite,sb=b.sprite;
            if(time>=life||sa==null||sb==null||!sa.exists||!sb.exists){Fx.dispose(this);return;}
            float ax=SkillVfx.midX(sa),ay=SkillVfx.midY(sa)+offset,bx=SkillVfx.midX(sb),by=SkillVfx.midY(sb)+offset;
            float dx=bx-ax,dy=by-ay,len=(float)Math.sqrt(dx*dx+dy*dy);
            float out=tetherOut(time);
            float hang=TETHER_SAG+(TETHER_TIGHT-TETHER_SAG)*out;
            float alpha=time<life*.6f?1f:Math.max(0f,(life-time)/(life*.4f));
            boolean shown=sa.visible||sb.visible;
            int n=Math.max(3,Math.min(TETHER_LINKS,Math.round(len/3f)));
            for(int i=0;i<TETHER_LINKS;i++){
                FxPic l=links[i];
                float t=(i+.5f)/n;
                if(i>=n||t>out){l.visible=false;continue;}
                float x=ax+dx*t,y=ay+dy*t+catenary(t)*hang;
                //along the curve: its slope there
                float tx=dx,ty=dy+hang*4f*(1f-2f*t);
                l.place(x,y);
                l.angle=(float)Math.toDegrees(Math.atan2(ty,tx))+90;
                l.am=alpha*(i%2==0?1f:.7f);
                l.visible=shown;
            }
            //where it takes hold, a glint
            if(!held&&out>=1f){
                held=true;
                if(shown){
                    FxParticle g=SkillVfx.glow(bx,by,FxBudget.P1);
                    if(g!=null)g.look(FxFrames.PLUS_3).paint(0xFFFFFF).life(.2f).alpha(1f,0,.12f).snap().source(bx,by);
                }
            }
        }
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
