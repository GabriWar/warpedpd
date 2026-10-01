package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.*;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;

/** Individual silhouettes for casts, stances and subclass skills; no gameplay RNG. */
public class SkillCastFX extends Group {
    private final Hero hero;
    private final Image[] images;
    private final int depth,branch,pattern;
    private float age;
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
        // One active cast halo per hero: rapid toggle taps cannot accumulate sprites.
        for(Gizmo child:hero.sprite.parent.membersView().toArray(new Gizmo[0]))
            if(child instanceof SkillCastFX){child.killAndErase();child.destroy();}
        hero.sprite.parent.add(new SkillCastFX(hero,skill,sprite));
    }
    private SkillCastFX(Hero hero,Skill skill,int sprite){
        this.hero=hero;depth=Dungeon.depth;branch=Dungeon.branch;pattern=sprite%3;
        images=new Image[2+Math.min(3,skill.level)/2];
        for(int i=0;i<images.length;i++){
            Image image=new Image("effects/skill_motes.png");image.frame(new TextureFilm(image.texture,4,4).get(sprite%4));
            image.hardlight(color(sprite));
            image.origin.set(2,2);images[i]=image;add(image);
        }
    }
    @Override public void update(){
        super.update();age+=Game.elapsed;
        if(age>=.4f||depth!=Dungeon.depth||branch!=Dungeon.branch||!hero.isAlive()){killAndErase();destroy();return;}
        float p=age/.4f;
        for(int i=0;i<images.length;i++){
            Image image=images[i];float a=(float)(i*2*Math.PI/images.length+p*(pattern==1?-1:1)*1.4);
            float radius=4+4*p;
            image.x=Math.round(hero.sprite.center().x+(float)Math.cos(a)*radius)-2;
            image.y=Math.round(hero.sprite.center().y+(float)Math.sin(a)*radius*.45f-4*p)-2;
            image.alpha(Math.min(1,p*8)*Math.min(1,(1-p)*4)*.6f);image.visible=hero.sprite.visible;
        }
    }
}
