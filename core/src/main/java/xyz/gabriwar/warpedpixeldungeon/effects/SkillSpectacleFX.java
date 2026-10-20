package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxCurves;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LanceFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * Visual-only timelines. Combat never waits for particles or animation callbacks.
 *
 * Each kind is drawn with the kit's own pictures and moves the way its thing would; none is an
 * icon popping up or rising out of the floor (a trap's, an item's read):
 * - LANCE: a spectral lance stabbing through the cell at chest height (LanceFX.spectral).
 * - THORN: a vine snaking over the floor toward the cell, thorns flicked up, two leaves, a crack.
 * - HAMMER: a smite of light: a column falling from above onto the cell (revealed top-down over
 *   HAMMER_FALL s), landing in the ground's crack and dust, a jolt, a pulse and a flare, fading.
 * - SPIRE: a short spire of light rising from the cell, a pool, holy sparks at its foot.
 * - SABER: a blade of light: a stroke from where it came, then a crescent over the cell.
 * - SHADOW: a shade: ink laid along its way, then the hero's shape in violet at its end.
 * - RAPTOR: a spirit swoop of light dipping onto the cell, feathers shed.
 * - JAW: a spectral bite: two crescents of light snapping shut over HALVES_CLOSE s in sparks, from
 *   JAW_OPEN px apart each to JAW_SHUT, where their tips meet (never past: one white blot).
 * - WINGS: wings of light opening behind the one at the cell from their roots (on the floor's
 *   layer, under the sprites; their glow in the light), bobbing, feathers.
 * - SLASH: the blade arc's crescent. SHURIKEN (unused) and ROCK keep their pictures.
 * fly(..., color) draws a kind in a colour of the caller's; each has its own otherwise. At most CAP
 * at once (counted, not looked for; LANCE counts with LanceFX's), each at most 6 quads besides its
 * own pictures (2 at most), a glow or a pool.
 */
public class SkillSpectacleFX extends Group {
    public static final int ROCK=0,HAMMER=1,SHADOW=2,SHURIKEN=3,RAPTOR=4,JAW=5,SABER=6,LANCE=7,WINGS=8,SPIRE=9,THORN=10,SLASH=11;
    /** At most this many at once. */
    public static final int CAP=96;
    /** A blade turns a quarter this often, s; a jaw's halves close in this long; a drop takes
     *  this share of its life to land (ROCK, SHURIKEN). */
    public static final float SPIN=.03f, HALVES_CLOSE=.08f, DROP=.35f;
    /** The smite's column falls in HAMMER_FALL s and fades over HAMMER_FADE; the spire rises in
     *  SPIRE_RISE and fades over SPIRE_FADE; the wings open in WINGS_OPEN; the swoop takes SWOOP. */
    public static final float HAMMER_FALL=.08f, HAMMER_FADE=.2f, SPIRE_RISE=.12f, SPIRE_FADE=.3f, WINGS_OPEN=.12f, SWOOP=.35f;
    /** A jaw's halves start this far from its middle each, px, and shut this far (their tips
     *  touching: SLASH_16 6's tips are its rows 8-9, 2's its rows 6-7). */
    public static final int JAW_OPEN=6, JAW_SHUT=2;
    /** The columns' heights, px; the wings' scale (x2: wings that frame the body). */
    public static final int HAMMER_H=40, SPIRE_H=36, WING_SCALE=2;
    /** Each kind's own colour (by kind; -1: none of its own). */
    public static final int[] COLORS={-1,0xF1D99C,0x6B4F94,-1,xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette.NATURE_LIGHT,0xB9DEEE,0xB9DEEE,-1,0xF1D99C,0xF1D99C,0x6F9A52,0xFFFFFF};
    /** The vine's colour under a thorn. */
    public static final int VINE=0x5E8C44;
    private static int live;

    private final FxPic pic,ghost1,ghost2,lower;
    private final float fromX,fromY,toX,toY;
    private final int kind,cell,from,depth,branch,color;
    private final float delay,duration,life;
    private final int[] frame;
    private float elapsed,swoopX,swoopY;
    private boolean landed,started,counted=true;
    //a shade's wisps laid so far
    private int laid;

    private SkillSpectacleFX(int kind,int fromCell,int toCell,float delay,float duration,int color){
        this.kind=kind;cell=toCell;from=fromCell;this.delay=delay;this.duration=duration;this.color=color;
        depth=Dungeon.depth;branch=Dungeon.branch;
        fromX=SkillVfx.cx(fromCell);fromY=SkillVfx.cy(fromCell);toX=SkillVfx.cx(toCell);toY=SkillVfx.cy(toCell);
        FxPic p=null,g1=null,g2=null,lo=null;int[] f=null;
        switch(kind){
            case ROCK:
                p=new FxPic("effects/skill_fields.png");f=new int[]{0,0,16,16};
                break;
            case SHURIKEN:
                p=new FxPic("effects/skill_spectacles.png");f=new int[]{kind*12,0,12,12};
                break;
            case HAMMER:case SPIRE:
                //the soft column and its core, cropped as they show
                p=light(FxFrames.Light.PILLAR,color,true);
                lo=light(FxFrames.Light.PILLAR_CORE,Ramp.lerp(color,0xFFFFFF,.75f),true);
                break;
            case JAW:
                p=light(FxFrames.SLASH_16[6],color,false);
                lo=light(FxFrames.SLASH_16[2],color,false);
                break;
            case WINGS:
                p=light(FxFrames.WING_9,color,false);
                lo=light(FxFrames.WING_9,color,false);
                lo.flipHorizontal=true;
                p.scale.set(WING_SCALE);lo.scale.set(WING_SCALE);
                break;
            case RAPTOR:
                g1=light(FxFrames.STREAK_TAPER,color,false);
                p=light(FxFrames.STREAK_TAPER,Ramp.lerp(color,0xFFFFFF,.5f),false);
                p.origin.set(FxFrames.STREAK_TAPER[2],FxFrames.STREAK_TAPER[3]/2f);
                g1.origin.set(p.origin.x,p.origin.y);
                break;
            default:
                break;
        }
        if(f!=null){
            p.show(f);p.origin.set(f[2]/2f,f[3]/2f);p.visible=false;add(p);
            if(kind==SHURIKEN){g1=ghost(f);g2=ghost(f);}
        }
        pic=p;ghost1=g1;ghost2=g2;lower=lo;frame=f;
        switch(kind){
            case HAMMER: life=HAMMER_FALL+.06f+HAMMER_FADE; break;
            case SPIRE: life=SPIRE_RISE+.08f+SPIRE_FADE; break;
            case SABER: life=.3f; break;
            case SHADOW: life=.55f; break;
            case RAPTOR: life=SWOOP+.05f; break;
            case JAW: life=HALVES_CLOSE+.1f+.15f; break;
            case THORN: life=.3f; break;
            default: life=duration; break;
        }
    }

    private FxPic light(int[] f,int c,boolean lightSheet){
        FxPic p=new FxPic(lightSheet?SkillVfx.LIGHT_SHEET:SkillVfx.SHEET);
        p.additive=true;p.show(f);p.lit(c);p.origin.set(f[2]/2f,f[3]/2f);p.visible=false;
        add(p);
        return p;
    }

    private FxPic ghost(int[] f){
        FxPic g=new FxPic("effects/skill_spectacles.png");
        g.show(f);g.origin.set(6,6);g.visible=false;
        addToBack(g);
        return g;
    }

    /** How many are on show. */
    public static synchronized int live(){return live;}

    /** A new scene (a test, the preview dump): none counted. */
    public static synchronized void reset(){live=0;}

    private static synchronized boolean enter(){
        if(live>=CAP)return false;
        live++;return true;
    }

    private synchronized void leave(){
        if(!counted)return;
        counted=false;
        synchronized(SkillSpectacleFX.class){live=Math.max(0,live-1);}
    }

    public static void show(int kind,int cell){fly(kind,cell,cell,0,.65f);}
    public static void fly(int kind,int from,int to,float delay,float duration){
        fly(kind,from,to,delay,duration,kind>=0&&kind<COLORS.length?COLORS[kind]:-1);
    }
    /** A kind in a colour of the caller's (its own for -1). */
    public static void fly(int kind,int from,int to,float delay,float duration,int color){
        if(!SkillInteractions.valid(from)||!SkillInteractions.valid(to)||Dungeon.hero==null||Dungeon.hero.sprite==null
                ||Dungeon.hero.sprite.parent==null||!Dungeon.level.heroFOV[to])return;
        if(kind==LANCE){
            //the spectral lance counts with the lances
            if(color==-1)LanceFX.spectral(from,to,delay);
            else LanceFX.spectral(from,to,delay,new int[]{0xFFFFFF,Ramp.lerp(color,0xFFFFFF,.5f),color,Ramp.lerp(color,0x000000,.4f)});
            return;
        }
        if(!enter())return;
        int c=color!=-1?color:(kind>=0&&kind<COLORS.length&&COLORS[kind]!=-1?COLORS[kind]:0xFFFFFF);
        final SkillSpectacleFX fx=new SkillSpectacleFX(kind,from,to,delay,duration,c);
        Fx.post(()->{
            //pictures of light over the night's tint; the old pictures with the hero
            //(wings behind the body: on the floor's layer, under everyone, their glow in the light)
            Group layer=kind==ROCK||kind==SHURIKEN?SkillVfx.heroLayer()
                    :GameScene.layer(kind==WINGS?GameScene.Layer.FLOOR:GameScene.Layer.LIGHTS);
            if(layer==null){fx.leave();return;}
            layer.add(fx);
        });
    }

    @Override public void update(){
        super.update();elapsed+=Game.elapsed;
        if(depth!=Dungeon.depth||branch!=Dungeon.branch||elapsed>=delay+life){Fx.dispose(this);return;}
        if(elapsed<delay){if(pic!=null)pic.visible=false;if(lower!=null)lower.visible=false;return;}
        float t=elapsed-delay;
        float p=Math.min(1,t/life);
        boolean seen=SkillVfx.seen(cell);
        if(!started){started=true;if(seen)begin();}
        switch(kind){
            case ROCK:{
                pic.visible=seen;pic.show(frame);pic.place(toX,toY-2);pic.am=Math.min(1,(1-p)*3)*.65f;
                break;}
            case SHURIKEN:{
                float x=fromX+(toX-fromX)*p,y=fromY+(toY-fromY)*p-2;
                pic.visible=seen;pic.show(frame);pic.place(x,y);
                int turn=(int)(elapsed/SPIN);
                pic.angle=90*(turn%4);
                pic.am=Math.min(1,(1-p)*5);
                trail(ghost1,p-SPIN/duration,turn-1,.5f,seen);
                trail(ghost2,p-2*SPIN/duration,turn-2,.25f,seen);
                break;}
            case HAMMER:hammer(t,seen);break;
            case SPIRE:spire(t,seen);break;
            case JAW:jaw(t,seen);break;
            case WINGS:wings(t,seen);break;
            case RAPTOR:swoop(t,seen);break;
            case SHADOW:shade(t,seen);break;
            case THORN:if(!landed&&t>=.12f){landed=true;if(seen)thorns();}break;
            case SABER:if(!landed&&t>=(from!=cell?.07f:0)){landed=true;if(seen)saber();}break;
            default:break;
        }
    }

    //a shuriken's ghost where it was a moment ago, turned as it was then
    private void trail(FxPic g,float p,int turn,float alpha,boolean seen){
        if(p<0||turn<0){g.visible=false;return;}
        float x=fromX+(toX-fromX)*p,y=fromY+(toY-fromY)*p-2;
        g.place(x,y);g.angle=90*(turn%4);g.am=pic.am*alpha;g.visible=seen;
    }

    // ------------------------------------------------------------------ the kinds

    //the smite: the column revealed from its top down as it falls, the blow as it lands, fading
    private void hammer(float t,boolean seen){
        float foot=toY+5;
        int[] f=FxFrames.Light.PILLAR,c=FxFrames.Light.PILLAR_CORE;
        //the column's soft top HAMMER_H px, its leading edge coming down
        int h=Math.max(1,Math.round(HAMMER_H*Math.min(1f,t/HAMMER_FALL)));
        pic.crop(f,0,0,f[2],h);pic.place(toX,foot-HAMMER_H+h/2f);
        lower.crop(c,0,0,c[2],h);lower.place(toX,foot-HAMMER_H+h/2f);
        if(!landed&&t>=HAMMER_FALL){landed=true;if(seen)smite();}
        float a=t<HAMMER_FALL+.06f?1f:Math.max(0f,1f-(t-HAMMER_FALL-.06f)/HAMMER_FADE);
        pic.am=.9f*a;lower.am=a;pic.visible=lower.visible=seen;
    }

    //the smite landing: the ground's crack and dust, a jolt, a pulse of light and a flare
    private void smite(){
        WarriorImpactFX.show(cell);
        Fx.shake(1,.1f);
        PulseRingFX.at(cell,color,14,.35f);
        PixelFlare.show(toX,toY,color,PixelFlare.M,.3f);
    }

    //the spire: a short column revealed from the ground up, fading
    private void spire(float t,boolean seen){
        float foot=toY+5;
        int[] f=FxFrames.Light.PILLAR,c=FxFrames.Light.PILLAR_CORE;
        //the column's soft top SPIRE_H px, risen out of the ground: its base first, its tip last
        int h=Math.max(1,Math.round(SPIRE_H*FxCurves.easeOut(Math.min(1f,t/SPIRE_RISE))));
        pic.crop(f,0,SPIRE_H-h,f[2],h);pic.place(toX,foot-h/2f);
        lower.crop(c,0,SPIRE_H-h,c[2],h);lower.place(toX,foot-h/2f);
        float a=t<SPIRE_RISE+.08f?1f:Math.max(0f,1f-(t-SPIRE_RISE-.08f)/SPIRE_FADE);
        pic.am=.85f*a;lower.am=a;pic.visible=lower.visible=seen;
    }

    //the bite: the upper crescent and the lower closing on each other, then holding, fading
    private void jaw(float t,boolean seen){
        float x=toX,y=toY-2;
        //from JAW_OPEN px apart each to JAW_SHUT: the teeth meeting tip to tip
        float k=FxCurves.easeIn(Math.min(1f,t/HALVES_CLOSE));
        int gap=Math.round(JAW_OPEN-(JAW_OPEN-JAW_SHUT)*k);
        pic.place(x,y-gap);lower.place(x,y+gap);
        float a=t<HALVES_CLOSE+.1f?1f:Math.max(0f,1f-(t-HALVES_CLOSE-.1f)/.15f);
        pic.am=lower.am=a;pic.visible=lower.visible=seen;
        if(!landed&&t>=HALVES_CLOSE){landed=true;if(seen)snap(x,y);}
    }

    //the wings: opening from their roots either side of the body, bobbing, fading over their end
    private void wings(float t,boolean seen){
        float x=toX,chest=toY-3;
        int[] f=FxFrames.WING_9;
        int w=Math.max(1,Math.round(f[2]*Math.min(1f,t/WINGS_OPEN)));
        int bob=Math.round(2*(float)Math.sin(t*2*Math.PI/.6f));
        //the left wing: its root its right edge, rows 5-8; the right its mirror
        pic.crop(f,f[2]-w,0,w,f[3]);pic.origin.set(w,6.5f);
        pic.x=x-3-pic.origin.x;pic.y=chest-bob-pic.origin.y;
        lower.crop(f,f[2]-w,0,w,f[3]);lower.origin.set(0,6.5f);
        lower.x=x+3-lower.origin.x;lower.y=chest-bob-lower.origin.y;
        float a=t<life*.6f?Math.min(1f,t/.06f):Math.max(0f,(life-t)/(life*.4f));
        pic.am=lower.am=.85f*a;pic.visible=lower.visible=seen;
    }

    //the swoop: a streak of light dipping onto the cell and up again, its tail behind it
    private void swoop(float t,boolean seen){
        float u=Math.min(1f,t/SWOOP);
        float x=toX-20+40*u,y=toY-16+14*(1-(2*u-1)*(2*u-1));
        float dx=40,dy=14*(-4*(2*u-1));
        float ang=(float)Math.toDegrees(Math.atan2(dy,dx));
        ghost1.x=swoopX-ghost1.origin.x;ghost1.y=swoopY-ghost1.origin.y;ghost1.angle=pic.angle;
        pic.x=x-pic.origin.x;pic.y=y-pic.origin.y;pic.angle=ang;
        float a=u<.8f?1f:(1-u)/.2f;
        pic.am=a;ghost1.am=.45f*a;pic.visible=seen;ghost1.visible=seen&&t>0;
        swoopX=x;swoopY=y;
        if(!landed&&u>=.5f){landed=true;if(seen)feathers(toX,toY-4,0xC8B89A);}
    }

    //the shade: ink laid along its way, wisp by wisp, then the hero's shape at its end
    private void shade(float t,boolean seen){
        int due=Math.min(5,1+(int)(t/.03f));
        while(laid<due){
            float k=(laid+1)/5f;
            if(seen)wisp(fromX+(toX-fromX)*k,fromY+(toY-fromY)*k+2);
            laid++;
        }
        if(!landed&&t>=.15f){
            landed=true;
            if(!seen)return;
            CharSprite h=Dungeon.hero.sprite;
            if(h!=null&&h.visible&&h.parent!=null){
                //his shape as it stands on his cell, moved onto this one
                Afterimage.of(h,color,.5f,.35f,toX-SkillVfx.cx(Dungeon.hero.pos),toY-SkillVfx.cy(Dungeon.hero.pos));
            }
            for(int i=0;i<3;i++)wisp(toX+FxRandom.Float(-5f,5f),toY+FxRandom.Float(-2f,4f));
        }
    }

    private void wisp(float x,float y){
        FxParticle p=SkillVfx.matter(x,y,FxBudget.P3);
        if(p!=null)p.look(FxFrames.INK_WISP,5).paint(Element.SHADOW.ramp.step(.25f))
                .vel(FxRandom.Float(-3f,3f),FxRandom.Float(3f,7f)).turbulence(1.5f,3f).life(.5f).alpha(.7f,.05f,.3f).smooth();
    }

    //what each kind does as it appears
    private void begin(){
        switch(kind){
            case THORN:{
                //the vine snaking over the floor toward the cell
                boolean right=from==cell||toX>=fromX;
                float dx=from==cell?-1:(fromX-toX),dy=from==cell?0:(fromY-toY);
                float l=Math.max(1f,(float)Math.sqrt(dx*dx+dy*dy));
                FxDecal vine=FxDecal.at(toX+dx/l*4,toY+5+dy/l*2,FxFrames.VINE[FxRandom.Int(3)],VINE,1f,1.2f,.8f,!right);
                if(vine!=null)vine.revealCrop(right?FxDecal.FROM_LEFT:FxDecal.FROM_RIGHT,.12f);
                break;}
            case SPIRE:{
                FxLight.pool(toX,toY+5,23,color,.8f).life(0,.12f,.3f);
                FxRecipes.sparks(toX,toY+3,Element.HOLY,4);
                break;}
            case HAMMER:
                FxLight.glow(toX,toY-6,15,color,.8f).life(HAMMER_FALL,.05f,.2f);
                break;
            case WINGS:
                FxLight.glow(toX,toY-3,23,color,.6f).life(.1f,life*.4f,life*.5f);
                feathers(toX,toY-6,0xF3EBD7);
                break;
            case SLASH:
                ArcSpinFX.at(cell,0xFFFFFF,10,.25f,FxRandom.Int(8)*45,FxRandom.chance(.5f)?900:-900,Math.min(.22f,duration));
                break;
            case SABER:
                if(from!=cell)StreakFX.show(from,cell,color,.35f,.2f);
                break;
            case ROCK:
                FxRecipes.dust(toX,toY+4,2);
                break;
            default:
                break;
        }
    }

    //a thorn's strike: thorns flicked up at the cell, two leaves, the ground split
    private void thorns(){
        for(int i=0;i<3;i++){
            FxParticle p=SkillVfx.matter(toX+FxRandom.Float(-4f,4f),toY+3,FxBudget.P3);
            if(p!=null)p.look(FxFrames.BLADE_4).paint(i==1?color:VINE).vel(FxRandom.Float(-15f,15f),FxRandom.Float(-50f,-35f))
                    .gravity(200f).spin(.06f).ground(toY+6,FxParticle.SETTLE,0).life(.25f).alpha(1f,0,.1f);
        }
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

    //a sabre's crescent over the cell, turning from the way it came
    private void saber(){
        float a=from!=cell?(float)Math.toDegrees(Math.atan2(toY-fromY,toX-fromX)):FxRandom.Int(8)*45;
        ArcSpinFX.at(cell,color,10,.25f,a,FxRandom.chance(.5f)?900:-900,.22f);
    }

    private static void feathers(float x,float y,int c){
        for(int i=0;i<2;i++){
            FxParticle p=SkillVfx.matter(x+FxRandom.Float(-5f,5f),y,FxBudget.P3);
            if(p!=null)p.look(FxFrames.FEATHER).paint(c)
                    .vel(FxRandom.Float(-10f,10f),FxRandom.Float(-8f,-2f)).gravity(28f).drag(2.5f).sway(4f,3f).spin(.3f)
                    .ground(y+12,FxParticle.SETTLE,0).life(1.4f).alpha(1f,0,.4f);
        }
    }

    //a jaw snapping shut: sparks off its teeth
    private void snap(float x,float y){
        FxRecipes.sparks(x,y,Element.METAL,3);
        FxLight.glow(x,y,11,color,.8f).life(0,.04f,.15f);
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
