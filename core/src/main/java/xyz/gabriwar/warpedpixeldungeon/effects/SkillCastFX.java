package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.School;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * A cast, in its school's own words, LIFE s: motes spiralling out about the caster (the kit's
 * CAST orbit, trailing) and the caster lit up for one frame (never the screen); then by school a
 * sanctuary glyph under the feet turning a quarter every GLYPH_TURN s (holy), wisps of ink
 * (shadow), leaves swirling (nature), embers spiralling up (fire), flakes (frost) or static
 * crackling over the caster (storm). At most QUADS quads; one cast at a time per hero; no
 * gameplay RNG.
 */
public class SkillCastFX extends Gizmo {
    /** How long a cast shows, s; how often the holy glyph turns. */
    public static final float LIFE=.5f, GLYPH_TURN=.08f;
    /** The most quads one cast draws: its orbit's 6, its school's 6 (the glyph counted). */
    public static final int QUADS=12;
    //the sanctuary glyph of effects/skill_fields.png
    private static final int[] GLYPH={64,0,16,16};
    private static SkillCastFX current;
    private final Hero hero;
    private final School school;
    private final int color,depth,branch,count;
    private final Ramp tone;
    private FxPic glyph;
    private FxOrbit.Ring ring;
    private float age;
    private int spawned;
    private boolean begun;
    public static int sprite(Skill skill){
        String name=skill.getClass().getSimpleName();
        switch(name){
            case "Hurl":case "ShadowClone":case "ChargedShot":case "CinderTrail":return -1;
            case "FrostNova":case "FrostArrows":return 12;
            case "Spark":case "StormCall":case "Slipstream":return 14;
            case "SoulDetonation":case "SummonSkeleton":case "DarkPact":case "Hex":return 15;
            case "SpiritArmor":case "ManaShield":case "Shieldbearer":case "IronStance":case "Sanctuary":case "BladeBind":return 17;
            case "SummonRat":case "SummonCrab":case "WildCall":case "Deadfall":return 18;
            case "AimedShot":case "PiercingFocus":case "DoubleShot":return 23;
            case "Bombvoyage":case "TengusArsenal":case "EmberArrows":return 20;
            case "Blackout":case "Vanish":case "Blink":case "DreadHowl":return 19;
            case "BloodDance":case "BloodRush":case "RecklessFury":case "UndyingWill":return 21;
            case "WarCry":case "Challenge":case "Rampage":case "SecondWind":return 16;
            case "KnockBack":case "Smash":case "Leap":return 1;
            case "Earthshatter":return 0;
            case "PhantomStrike":case "DoubleStab":return 2;
            case "NinjaBomb":return 3;
            case "ArrowStorm":return 4;
            case "BearTrap":return 5;
            case "WhirlingFlurry":case "Fleche":case "Lunge":return 6;
            case "ImpalingThrust":return 7;
            case "HealingPrayer":case "MassHeal":case "GuardianSpirit":case "AvatarOfLight":case "Purify":return 8;
            case "HolySmite":case "PillarOfLight":case "HolyCharge":case "Reckoning":case "ScouringFlame":return 9;
            case "RiposteStance":case "Pirouette":case "DragonKick":return 22;
            case "MeteorCall":case "Transcendence":return 13;
            default:return 13;
        }
    }
    public static int color(int sprite){
        switch(sprite){
            case 0:case 1:case 16:return 0xD9AE75;
            case 2:case 3:case 13:case 15:case 19:return 0xB89CE4;
            case 4:case 5:case 10:case 18:case 23:return 0xB3D699;
            case 8:case 9:case 17:return 0xF1D99C;
            case 20:case 21:return 0xDF9979;
            default:return 0xB9DEEE;
        }
    }
    public static void play(Skill skill,Hero hero){
        int sprite=sprite(skill);
        if(sprite<0||hero==null||hero.sprite==null||hero.sprite.parent==null||skill.level<=0)return;
        final SkillCastFX fx=new SkillCastFX(hero,sprite);
        // Casts come from the actor thread too (a skill disarming a trap underfoot): the scene is
        // only touched on the render thread.
        Fx.post(()->{
            if(hero.sprite==null||hero.sprite.parent==null)return;
            // One cast per hero at a time: rapid toggle taps cannot pile up.
            if(current!=null)current.end();
            current=fx;
            hero.sprite.parent.add(fx);
        });
    }
    private SkillCastFX(Hero hero,int sprite){
        this.hero=hero;depth=Dungeon.depth;branch=Dungeon.branch;
        school=School.ofSilhouette(sprite);color=color(sprite);
        tone=new Ramp(0xFFFFFF,color);
        count=particles(school);
    }
    /** The school's own particles a cast throws (beside its orbit's motes). */
    public static int particles(School school){
        switch(school){
            case FIRE:case FROST:return 5;
            case STORM:return 6;
            case HOLY:case SHADOW:case NATURE:return 4;
            default:return 0;
        }
    }
    private void begin(CharSprite s){
        begun=true;
        ring=FxOrbit.attach(s,FxOrbit.Slot.CAST).motes(3,FxFrames.MOTE_2,color,true).trail(1).breath(.7f,1f,2f).speed(5f);
        SkillVfx.brighten(s,color,.35f);
        Group floor=GameScene.layer(GameScene.Layer.FLOOR);
        if(school==School.HOLY&&floor!=null){
            glyph=new FxPic("effects/skill_fields.png");
            glyph.show(GLYPH);
            glyph.origin.set(8,8);
            glyph.am=0;
            floor.add(glyph);
        }
    }
    @Override public void update(){
        super.update();
        CharSprite s=hero.sprite;
        if(s==null||s.parent==null||depth!=Dungeon.depth||branch!=Dungeon.branch||!hero.isAlive()){end();return;}
        if(!begun)begin(s);
        age+=Game.elapsed;
        if(age>=LIFE){end();return;}
        float x=SkillVfx.midX(s),y=SkillVfx.midY(s),feet=s.y+s.height()-3;
        if(glyph!=null){
            glyph.place(x,feet);
            glyph.angle=90*((int)(age/GLYPH_TURN)%4);
            glyph.am=.7f*FxCurves.tri(age/LIFE);
            glyph.visible=s.visible;
        }
        //the school's particles, spread over its first 0.3 s
        int due=Math.min(count,(int)Math.ceil(count*Math.min(1f,age/.3f)));
        while(spawned<due){
            if(s.visible)spawn(x,y,feet,s);
            spawned++;
        }
    }
    private void spawn(float x,float y,float feet,CharSprite s){
        float a=FxRandom.angle();
        FxParticle p;
        switch(school){
            case HOLY:
                p=SkillVfx.glow(x+FxRandom.Float(-6f,6f),feet-FxRandom.Float(0f,3f),FxBudget.P2);
                if(p!=null)p.look(FxFrames.MOTE_1).ramp(tone,true).vel(0,FxRandom.Float(-26f,-16f)).drag(1f)
                        .life(.45f).alpha(1f,.05f,.25f);
                break;
            case SHADOW:
                p=SkillVfx.matter(x+(float)Math.cos(a)*6f,y+4+(float)Math.sin(a)*2f,FxBudget.P2);
                if(p!=null)p.look(FxFrames.INK_WISP,6).paint(Element.SHADOW.ramp.step(.3f))
                        .vel((float)Math.cos(a)*-4f,FxRandom.Float(-12f,-7f)).turbulence(1.5f,4f).life(.45f).alpha(.75f,.05f,.25f).smooth();
                break;
            case NATURE:{
                int c1=Dungeon.level!=null?Dungeon.level.color1:Element.NATURE.ramp.stop(2);
                int c2=Dungeon.level!=null?Dungeon.level.color2:Element.NATURE.ramp.stop(3);
                p=SkillVfx.matter(x+(float)Math.cos(a)*8f,y+(float)Math.sin(a)*3f,FxBudget.P2);
                //swirling: round the caster, along the circle, and up
                if(p!=null)p.look(FxRandom.chance(.5f)?FxFrames.LEAF_D:FxFrames.LEAF_E,4).paint(FxRandom.chance(.5f)?c1:c2)
                        .vel(-(float)Math.sin(a)*26f,(float)Math.cos(a)*13f-10f).drag(2.5f).sway(3f,7f).life(.45f).alpha(1f,0,.25f);
                break;}
            case FIRE:
                p=SkillVfx.glow(x+(float)Math.cos(a)*7f,feet-2,FxBudget.P2);
                if(p!=null)p.look(FxRandom.chance(.6f)?FxFrames.EMBER_1:FxFrames.EMBER_2).ramp(Element.FIRE.ember,true)
                        .vel(-(float)Math.sin(a)*14f,FxRandom.Float(-30f,-20f)).sway(4f,9f).life(.45f).alpha(1f,0,.2f);
                break;
            case FROST:
                p=SkillVfx.matter(x+FxRandom.Float(-8f,8f),y-FxRandom.Float(4f,10f),FxBudget.P2);
                if(p!=null)p.look(FxRandom.chance(.5f)?FxFrames.MOTE_2:FxFrames.PLUS_3).paint(Element.FROST.ramp.stop(1))
                        .vel(FxRandom.Float(-4f,4f),FxRandom.Float(6f,12f)).sway(2f,5f).life(.45f).alpha(.9f,.05f,.25f);
                break;
            case STORM:{
                //static: a crackle over the caster's box, each lasting a blink
                float sx=s.x+FxRandom.Float(s.width()),sy=s.y+FxRandom.Float(s.height());
                p=SkillVfx.glow(sx,sy,FxBudget.P2);
                if(p!=null)p.look(FxRandom.chance(.5f)?FxFrames.SPARK_2:FxFrames.SPARK_3).ramp(Element.ELECTRIC.ember,true)
                        .vel((float)Math.cos(a)*30f,(float)Math.sin(a)*30f).drag(12f).orient()
                        .life(FxRandom.Float(.06f,.12f)).alpha(1f,0,.03f);
                break;}
            default:
                break;
        }
    }
    private void release(){
        if(ring!=null){ring.detach();ring=null;}
        if(glyph!=null){Fx.dispose(glyph);glyph=null;}
        if(current==this)current=null;
    }
    private void end(){
        release();
        killAndErase();
    }
    @Override public void destroy(){
        release();
        super.destroy();
    }
}
