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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * A cast, in its school's own words, LIFE s: motes spiralling out about the caster (the kit's
 * CAST orbit, trailing) and the caster lit up for one frame (never the screen); then by school a
 * sanctuary glyph under the feet turning a quarter every GLYPH_TURN s (holy), wisps of ink
 * (shadow), leaves swirling (nature), embers spiralling up (fire; blood and fury's casts, the
 * silhouette 21, in the fury's crimson, never a fire's orange), flakes (frost), static
 * crackling over the caster (storm), or for a mana ward (Spirit Armor, Mana Shield) the ward's own
 * words: RUNES runes of mana locking round the feet one every WARD_GAP s (RuneFX.lock, RUNE_MANA: a
 * ward is runes locking, never drifting). Martial and steel casts are their orbit and light alone.
 *
 * A skill's school is its own (school(Skill)), not always its silhouette's: the mage's mana wards are
 * arcane, the warrior's stances martial, Blade Bind steel, Transcendence and Sanctuary holy, Meteor
 * Call fire; the silhouettes (the pictures) stay. Its colour is its school's (color), a mana ward's
 * mana blue. A skill with no look of its own (ownLook: sprite()'s default, the rogue's, huntress's,
 * warrior's and cleric's procs) is not the arcane's: it casts plain, its orbit and light in its
 * caster's class colour, no runes. At most QUADS quads (its orbit's 6 and its school's 6: the glyph,
 * the particles or the runes); one cast at a time per hero; no gameplay RNG.
 */
public class SkillCastFX extends Gizmo {
    /** How long a cast shows, s; how often the holy glyph turns. */
    public static final float LIFE=.5f, GLYPH_TURN=.08f;
    /** The most quads one cast draws: its orbit's 6, its school's 6 (the glyph, or the runes, counted). */
    public static final int QUADS=12;
    /** A mana ward's runes: its ellipse round the feet (px), the gap between two runes locking (s)
     *  and how long each holds (s) before it fades (RuneFX.LOCK_FADE). */
    public static final int RUNES=6;
    public static final float WARD_RX=10f, WARD_RY=4f, WARD_GAP=.08f, WARD_HOLD=.3f;
    /** A mana ward's runes lie where he stood: when he steps off while they hold, they fade where
     *  they are over WARD_LEAVE s (never left lying on the floor). The cast stays (its orbit and
     *  light gone) to watch them till their course ends, WARD_END s. */
    public static final float WARD_LEAVE=.15f, WARD_END=(RUNES-1)*WARD_GAP+WARD_HOLD+RuneFX.LOCK_FADE;
    //the sanctuary glyph of effects/skill_fields.png
    private static final int[] GLYPH={64,0,16,16};
    //blood and fury's embers: the fury's red darkening, never its pale flash (pink as it lingers)
    private static final Ramp FURY_EMBERS=new Ramp(SkillPalette.FURY.stop(1),SkillPalette.FURY.stop(2),SkillPalette.FURY.stop(3));
    private static SkillCastFX current;
    private final Hero hero;
    private final School school;
    private final boolean ward,glyphed;
    private final int color,depth,branch,count;
    private final Ramp tone,embers;
    private FxPic glyph;
    private RuneFX.Runes runes;
    private FxOrbit.Ring ring;
    private float age,wardX,wardY;
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
    /** A silhouette's colour: its school's (SkillPalette's meanings: fire FF8418, the arcane 9070F0,
     *  nature A8D880, frost A4E9FF, a storm 7FA6FF), blood and fury's (21) the fury's red. */
    public static int color(int sprite){
        return sprite==21?SkillPalette.FURY_LIGHT:School.ofSilhouette(sprite).color;
    }
    /** A skill's school: its own where its silhouette's would say otherwise (its picture stays the
     *  silhouette's), else the silhouette's. */
    public static School school(Skill skill){
        School own=named(skill);
        return own!=null?own:School.ofSilhouette(sprite(skill));
    }
    //the schools named for a skill over its silhouette's; null: none
    private static School named(Skill skill){
        switch(skill.getClass().getSimpleName()){
            case "SpiritArmor":case "ManaShield":return School.ARCANE;
            case "Shieldbearer":case "IronStance":return School.MARTIAL;
            case "BladeBind":return School.STEEL;
            case "Transcendence":case "Sanctuary":return School.HOLY;
            case "MeteorCall":return School.FIRE;
            //a chain of the storm's lightning: its static, never the rogue's ink of fear and stealth
            case "Blink":return School.STORM;
            default:return null;
        }
    }
    /** Whether a skill has a cast look of its own: a silhouette sprite() names, or a school named
     *  here. The rest fall to sprite()'s default (13: the rogue's, the huntress's, the warrior's and
     *  the cleric's procs, Venom to Last Rites), which is not the arcane's: they cast plain. */
    public static boolean ownLook(Skill skill){
        return named(skill)!=null||sprite(skill)!=13;
    }
    /** A caster's class colour (its school's: the warrior's earth, the mage's arcane, the rogue's
     *  shadow, the huntress's leaf green, the cleric's holy gold, the duelist's steel). */
    public static int classColor(Hero caster){
        return SkillVfx.classColor(caster);
    }
    /** Whether a skill's cast lies on the turning holy glyph: a holy one's, but Sanctuary's (its
     *  ground is its own consecrated seal, still: Sanctum). */
    public static boolean holyGlyph(Skill skill){
        return school(skill)==School.HOLY&&!skill.getClass().getSimpleName().equals("Sanctuary");
    }
    /** Whether a skill's cast raises a mana ward (its runes mana blue, locking round the feet). */
    public static boolean manaWard(Skill skill){
        String name=skill.getClass().getSimpleName();
        return name.equals("SpiritArmor")||name.equals("ManaShield");
    }
    /** A skill's cast colour: a mana ward's mana blue, a skill with no look of its own its caster's
     *  class colour, else its school's (its silhouette's when that is its school). */
    public static int color(Skill skill,Hero caster){
        if(manaWard(skill))return SkillPalette.mid(SkillPalette.RUNE_MANA);
        if(!ownLook(skill))return classColor(caster);
        School s=school(skill);
        int sprite=sprite(skill);
        return s==School.ofSilhouette(sprite)?color(sprite):s.color;
    }
    public static void play(Skill skill,Hero hero){
        int sprite=sprite(skill);
        if(sprite<0||hero==null||hero.sprite==null||hero.sprite.parent==null||skill.level<=0)return;
        final SkillCastFX fx=new SkillCastFX(hero,skill);
        // Casts come from the actor thread too (a skill disarming a trap underfoot): the scene is
        // only touched on the render thread.
        Fx.post(()->{
            if(hero.sprite==null||hero.sprite.parent==null)return;
            // One cast per hero at a time: rapid toggle taps cannot pile up (its runes go with it).
            if(current!=null)current.end(true);
            current=fx;
            hero.sprite.parent.add(fx);
        });
    }
    private SkillCastFX(Hero hero,Skill skill){
        this.hero=hero;depth=Dungeon.depth;branch=Dungeon.branch;
        school=school(skill);ward=manaWard(skill);glyphed=holyGlyph(skill);color=color(skill,hero);
        //its motes from a pale of its colour (never white) to its colour
        tone=new Ramp(Ramp.lerp(color,0xFFFFFF,.5f),color);
        embers=sprite(skill)==21?FURY_EMBERS:Element.FIRE.ember;
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
        if(glyphed&&floor!=null){
            glyph=new FxPic("effects/skill_fields.png");
            glyph.show(GLYPH);
            glyph.origin.set(8,8);
            glyph.am=0;
            floor.add(glyph);
        }
        //a mana ward's words round the feet, locking in
        if(ward){
            wardX=SkillVfx.midX(s);wardY=s.y+s.height()-2;
            runes=RuneFX.lock(wardX,wardY,RUNES,SkillPalette.RUNE_MANA,WARD_RX,WARD_RY,WARD_GAP,WARD_HOLD);
        }
    }
    @Override public void update(){
        super.update();
        CharSprite s=hero.sprite;
        if(s==null||s.parent==null||depth!=Dungeon.depth||branch!=Dungeon.branch||!hero.isAlive()){end(true);return;}
        if(!begun)begin(s);
        age+=Game.elapsed;
        //he steps off the ward's runes: they fade where they lie
        if(runes!=null&&(Math.abs(SkillVfx.midX(s)-wardX)>=1f||Math.abs(s.y+s.height()-2-wardY)>=1f)){
            runes.lift(0f,WARD_LEAVE);
            runes=null;
        }
        if(age>=LIFE){
            //its runes still holding: it stays, its orbit gone, to watch them till their course ends
            //(the cast itself is over: the next one never cuts them)
            if(runes!=null&&age<WARD_END){
                settle();
                if(current==this)current=null;
                return;
            }
            end(false);return;
        }
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
                if(p!=null)p.look(FxRandom.chance(.6f)?FxFrames.EMBER_1:FxFrames.EMBER_2).ramp(embers,true)
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
    //its orbit and glyph let go of
    private void settle(){
        if(ring!=null){ring.detach();ring=null;}
        if(glyph!=null){Fx.dispose(glyph);glyph=null;}
    }
    //cut short (another cast, the caster gone): its runes go with it; run its course: a ward's
    //runes finish their hold and fade by themselves
    private void release(boolean early){
        settle();
        if(runes!=null){if(early)Fx.dispose(runes);runes=null;}
        if(current==this)current=null;
    }
    private void end(boolean early){
        release(early);
        killAndErase();
    }
    @Override public void destroy(){
        release(false);
        super.destroy();
    }
}
