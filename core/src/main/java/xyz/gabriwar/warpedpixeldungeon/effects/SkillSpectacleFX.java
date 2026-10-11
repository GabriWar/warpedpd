package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;

/**
 * Visual-only timelines. Combat never waits for particles or animation callbacks.
 *
 * Each kind moves the way its thing would: a hammer (or a spire) drops, squashes a pixel for one
 * frame as it lands and cracks the ground under it in dust; shurikens and sabres turn a quarter
 * every SPIN s with two ghosts behind them; thorns and lances grow out of the ground (revealed
 * from the bottom up) throwing pebbles, a thorn leaving leaves and a crack where it struck; wings
 * bob and shed feathers; a jaw's two halves snap shut over HALVES_CLOSE s in sparks; a slash is
 * the blade arc's crescent; a shadow sheds wisps of ink. At most CAP at once (counted, not looked
 * for), each at most 6 quads besides its own picture.
 */
public class SkillSpectacleFX extends Group {
    public static final int ROCK=0,HAMMER=1,SHADOW=2,SHURIKEN=3,RAPTOR=4,JAW=5,SABER=6,LANCE=7,WINGS=8,SPIRE=9,THORN=10,SLASH=11;
    /** At most this many at once. */
    public static final int CAP=96;
    /** A blade turns a quarter this often, s; a jaw's halves close in this long; a drop takes
     *  this share of its life to land. */
    public static final float SPIN=.03f, HALVES_CLOSE=.08f, DROP=.35f;
    private static int live;

    private final FxPic pic,ghost1,ghost2,lower;
    private final float fromX,fromY,toX,toY;
    private final int kind,cell,depth,branch;
    private final float delay,duration;
    private final int[] frame;
    private float elapsed;
    private boolean landed,started,counted=true;
    private int squash;

    private SkillSpectacleFX(int kind,int fromCell,int toCell,float delay,float duration){
        this.kind=kind;cell=toCell;this.delay=delay;this.duration=duration;
        depth=Dungeon.depth;branch=Dungeon.branch;
        fromX=SkillVfx.cx(fromCell);fromY=SkillVfx.cy(fromCell);toX=SkillVfx.cx(toCell);toY=SkillVfx.cy(toCell);
        if(kind==ROCK){
            pic=new FxPic("effects/skill_fields.png");frame=new int[]{0,0,16,16};
        }else{
            pic=new FxPic("effects/skill_spectacles.png");frame=new int[]{kind*12,0,12,12};
        }
        pic.show(frame);pic.origin.set(frame[2]/2f,frame[3]/2f);pic.visible=false;
        add(pic);
        if(kind==SHURIKEN||kind==SABER){
            ghost1=ghost();ghost2=ghost();
        }else{
            ghost1=ghost2=null;
        }
        if(kind==JAW){
            lower=new FxPic("effects/skill_spectacles.png");lower.visible=false;add(lower);
        }else lower=null;
    }

    private FxPic ghost(){
        FxPic g=new FxPic("effects/skill_spectacles.png");
        g.show(frame);g.origin.set(6,6);g.visible=false;
        addToBack(g);
        return g;
    }

    /** How many are on show. */
    public static synchronized int live(){return live;}

    private static synchronized boolean enter(){
        if(live>=CAP)return false;
        live++;return true;
    }

    private synchronized void leave(){
        if(!counted)return;
        counted=false;
        synchronized(SkillSpectacleFX.class){live--;}
    }

    public static void show(int kind,int cell){fly(kind,cell,cell,0,.65f);}
    public static void fly(int kind,int from,int to,float delay,float duration){
        if(!SkillInteractions.valid(from)||!SkillInteractions.valid(to)||Dungeon.hero==null||Dungeon.hero.sprite==null
                ||Dungeon.hero.sprite.parent==null||!Dungeon.level.heroFOV[to])return;
        if(!enter())return;
        final SkillSpectacleFX fx=new SkillSpectacleFX(kind,from,to,delay,duration);
        Fx.post(()->{
            Group layer=SkillVfx.heroLayer();
            if(layer==null){fx.leave();return;}
            layer.add(fx);
        });
    }

    @Override public void update(){
        super.update();elapsed+=Game.elapsed;
        if(depth!=Dungeon.depth||branch!=Dungeon.branch||elapsed>=delay+duration){Fx.dispose(this);return;}
        if(elapsed<delay){pic.visible=false;return;}
        float p=Math.min(1,(elapsed-delay)/duration);
        boolean seen=SkillVfx.seen(cell);
        if(!started){started=true;if(seen)begin();}
        float x=fromX+(toX-fromX)*p,y=fromY+(toY-fromY)*p-2;
        pic.visible=seen&&kind!=SLASH;
        pic.angle=0;
        switch(kind){
            case ROCK:
                pic.show(frame);pic.place(toX,toY);pic.am=Math.min(1,(1-p)*3)*.65f;
                break;
            case THORN:case LANCE:{
                //grows out of the ground over its first quarter: its rows revealed from the bottom
                int h=Math.max(1,Math.round(12*Math.min(1f,p*4)));
                pic.crop(frame,0,12-h,12,h);
                pic.place(x,y+6-h/2f);
                pic.am=Math.min(1,p*5)*Math.min(1,(1-p)*4);
                break;}
            case HAMMER:case SPIRE:{
                float q=Math.min(1f,p/DROP);
                if(q>=1f&&!landed){landed=true;squash=2;if(seen)land();}
                if(squash>0){
                    //one frame a pixel shorter: the blow
                    pic.crop(frame,0,1,12,11);pic.place(x,y+.5f);squash--;
                }else{
                    pic.show(frame);pic.place(x,y-Math.round(14*(1-q)*(1-q)));
                }
                pic.am=Math.min(1,p*7)*Math.min(1,(1-p)*5);
                break;}
            case SHURIKEN:case SABER:{
                pic.show(frame);pic.place(x,y);
                int turn=(int)(elapsed/SPIN);
                pic.angle=90*(turn%4);
                pic.am=Math.min(1,(1-p)*5);
                trail(ghost1,p-SPIN/duration,turn-1,.5f,seen);
                trail(ghost2,p-2*SPIN/duration,turn-2,.25f,seen);
                break;}
            case WINGS:case RAPTOR:
                pic.show(frame);pic.place(x,y-Math.round(Math.sin(p*Math.PI)*4));
                pic.am=Math.min(1,p*6)*Math.min(1,(1-p)*5);
                break;
            case JAW:{
                //the two halves close from 3 px apart, then hold shut
                float t=elapsed-delay;
                int gap=Math.round(3*Math.max(0f,1f-t/HALVES_CLOSE));
                pic.crop(frame,0,0,12,6);pic.place(x,y-3-gap);
                lower.crop(frame,0,6,12,6);lower.place(x,y+3+gap);
                lower.visible=pic.visible;
                float a=Math.min(1,p*7)*Math.min(1,(1-p)*5);
                pic.am=lower.am=a;
                if(gap==0&&!landed){landed=true;if(seen)snap(x,y);}
                break;}
            default:
                pic.show(frame);pic.place(x,y);
                pic.am=Math.min(1,p*7)*Math.min(1,(1-p)*5);
                break;
        }
    }

    //a blade's ghost where it was a moment ago, turned as it was then
    private void trail(FxPic g,float p,int turn,float alpha,boolean seen){
        if(p<0||turn<0){g.visible=false;return;}
        float x=fromX+(toX-fromX)*p,y=fromY+(toY-fromY)*p-2;
        g.place(x,y);g.angle=90*(turn%4);g.am=pic.am*alpha;g.visible=seen;
    }

    //what each kind throws as it appears
    private void begin(){
        switch(kind){
            case THORN:case LANCE:
                for(int i=0;i<4;i++){
                    FxParticle p=SkillVfx.matter(toX+FxRandom.Float(-4f,4f),toY+4,FxBudget.P3);
                    if(p!=null)p.look(FxRandom.chance(.5f)?FxFrames.PEBBLE_2:FxFrames.PEBBLE_3).paint(Element.EARTH.ember.step(FxRandom.Float()))
                            .vel(FxRandom.Float(-20f,20f),FxRandom.Float(-45f,-25f)).gravity(200f)
                            .ground(toY+6,FxParticle.BOUNCE,.3f).life(.7f).alpha(1f,0,.25f);
                }
                if(kind==THORN){
                    //where a thorn strikes: two leaves and the ground split
                    FxDecal crack=FxDecal.at(toX,toY+5,FxFrames.CRACK_11[FxRandom.Int(2)],Element.EARTH.ramp.stop(4),.6f,1.5f,1f,FxRandom.chance(.5f));
                    if(crack!=null)crack.revealCrop(FxDecal.FROM_MIDDLE,.1f);
                    int c1=Dungeon.level.color1,c2=Dungeon.level.color2;
                    for(int i=0;i<2;i++){
                        FxParticle p=SkillVfx.matter(toX+FxRandom.Float(-3f,3f),toY-4,FxBudget.P3);
                        if(p!=null)p.look(i==0?FxFrames.LEAF_D:FxFrames.LEAF_E,4).paint(i==0?c1:c2)
                                .vel(FxRandom.Float(-14f,14f),FxRandom.Float(-24f,-14f)).gravity(28f).drag(2.5f).sway(3f,4f)
                                .ground(toY+6,FxParticle.SETTLE,0).life(1.2f).alpha(1f,0,.3f);
                    }
                }
                break;
            case WINGS:case RAPTOR:
                for(int i=0;i<2;i++){
                    FxParticle p=SkillVfx.matter(toX+FxRandom.Float(-5f,5f),toY-6,FxBudget.P3);
                    if(p!=null)p.look(FxFrames.FEATHER).paint(kind==WINGS?0xF3EBD7:0xC8B89A)
                            .vel(FxRandom.Float(-10f,10f),FxRandom.Float(-8f,-2f)).gravity(28f).drag(2.5f).sway(4f,3f).spin(.3f)
                            .ground(toY+6,FxParticle.SETTLE,0).life(1.4f).alpha(1f,0,.4f);
                }
                break;
            case SLASH:
                ArcSpinFX.at(cell,0xFFFFFF,10,.25f,FxRandom.Int(8)*45,FxRandom.chance(.5f)?900:-900,Math.min(.22f,duration));
                break;
            case SHADOW:
                for(int i=0;i<3;i++){
                    FxParticle p=SkillVfx.matter(toX+FxRandom.Float(-5f,5f),toY+FxRandom.Float(-2f,4f),FxBudget.P3);
                    if(p!=null)p.look(FxFrames.INK_WISP,5).paint(Element.SHADOW.ramp.step(.25f))
                            .vel(FxRandom.Float(-3f,3f),FxRandom.Float(-12f,-6f)).turbulence(1.5f,3f).life(.6f).alpha(.7f,.05f,.3f).smooth();
                }
                break;
            case ROCK:
                FxRecipes.dust(toX,toY+4,2);
                break;
            default:
                break;
        }
    }

    //a hammer's landing: the crack, the dust and a jolt; a spire of light's: holy sparks, no crack
    private void land(){
        if(kind==HAMMER){
            WarriorImpactFX.show(cell);
            Fx.shake(1,.1f);
        }else{
            FxRecipes.sparks(SkillVfx.cx(cell),SkillVfx.cy(cell)+3,Element.HOLY,4);
        }
    }

    //a jaw snapping shut: sparks off its teeth
    private void snap(float x,float y){
        FxRecipes.sparks(x,y,Element.METAL,3);
    }

    @Override public void kill(){
        leave();
        super.kill();
    }

    @Override public void destroy(){
        leave();
        super.destroy();
    }
}
