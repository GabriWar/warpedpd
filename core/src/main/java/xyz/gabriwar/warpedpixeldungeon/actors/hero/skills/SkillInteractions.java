package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;

import java.util.*;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.*;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.*;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.*;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

/** Explicit, bounded interactions shared by active skills. Secondary damage cannot recurse. */
public final class SkillInteractions {
    private SkillInteractions(){}
    private static boolean secondary;

    /** a damage source (Buff, effect object) that belongs to a hero skill: its kills are the hero's
     *  kills and Vulnerable targets take x1.33 from it. Skill itself implements this */
    public interface HeroDamageSource { boolean rangedSource(); }

    /** true when src is a hero skill's damage: a Skill, a Skill class, a HeroDamageSource, or one of
     *  the skill-only effects below. Never true for the hero himself or damage over time */
    public static boolean heroSkillSource(Object src){
        if(src==null||Skill.isTickDamage(src))return false;
        if(src instanceof HeroDamageSource)return true;
        if(src instanceof Class){
            Class<?> c=(Class<?>)src;
            return c==SkillInteractions.class||c==ManaShieldWard.class||Skill.class.isAssignableFrom(c);
        }
        return src instanceof SkillSequence||src instanceof SkillField||src instanceof ArrowRain
                ||src instanceof HeartseekerArrow||src instanceof MeteorFall||src instanceof SacredWeaponSwords
                ||src instanceof DivineWrathGround||src instanceof AvatarOfLightHalo||src instanceof UndyingWillWard
                ||src instanceof ShieldOfTheFaithfulWard||src instanceof SpiritArmorMotes;
    }
    /** the hit that counts as 100% of the hero's damage: flat skill numbers are written against it, so a
     *  skill doing "5" deals 100% of the hero's average hit and every number converts to a round 20% step */
    public static final float BASE_HIT=5f;

    /** how much harder flat skill numbers hit for the hero's own damage: the average hit of the
     *  weapon in hand (or bare fists) against a starting weapon's. A skill written as "_N damage_"
     *  therefore deals N/5 of the hero's average hit, which is what its description shows */
    public static float heroPower(){
        Hero h=Dungeon.hero;
        if(h==null||h.belongings==null)return 1f;
        xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon w=h.belongings.weapon();
        float avg;
        if(w!=null)avg=(w.min()+w.max())/2f;
        else{int total=0;for(int i=0;i<6;i++)total+=xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfForce.damageRoll(h);avg=total/6f;}
        return avg/BASE_HIT;
    }

    /** the hero's average hit: the weapon in hand, or bare fists */
    public static float heroAverageHit(){
        return heroPower()*BASE_HIT;
    }

    /** a skill's heal or shield written as a percentage of a max health, never below 1 */
    public static int ofHealth(int maxHealth,float fraction){
        return Math.max(1,Math.round(maxHealth*fraction));
    }

    /** a hero skill hit whose numbers are flat, so they grow with the hero's damage; skills built on
     *  the weapon or strength already do and are left alone */
    @SuppressWarnings("unchecked")
    public static boolean levelScaledSource(Object src){
        if(!heroSkillSource(src))return false;
        //these already hit for a share of a blow, a hit or the target's health: scaling them again would double it
        if(src instanceof ArrowRain||src instanceof SacredWeaponSwords||src instanceof HeartseekerArrow
                ||src instanceof ShieldOfTheFaithfulWard||src instanceof Sorcerer.Overload)return false;
        if(src instanceof Skill)return !((Skill)src).weaponScaled();
        if(src instanceof Class&&Skill.class.isAssignableFrom((Class<?>)src)&&Dungeon.hero!=null&&Dungeon.hero.heroSkills!=null){
            Skill s=Dungeon.hero.heroSkills.get((Class<Skill>)src);
            return s==null||!s.weaponScaled();
        }
        return true;
    }

    /** ranged = the killing blow travelled to its target (arrows, bolts, falling rock, flung shards);
     *  melee = delivered at the hero's side or under a melee target (blades, quakes, auras, pushes) */
    @SuppressWarnings("unchecked")
    public static boolean rangedSource(Object src){
        if(src instanceof HeroDamageSource)return ((HeroDamageSource)src).rangedSource();
        if(src instanceof Class){
            Class<?> c=(Class<?>)src;
            if(c==ManaShieldWard.class)return true;
            if(Skill.class.isAssignableFrom(c)&&Dungeon.hero!=null&&Dungeon.hero.heroSkills!=null){
                Skill s=Dungeon.hero.heroSkills.get((Class<Skill>)c);
                return s!=null&&s.rangedSource();
            }
            return false;
        }
        if(src instanceof SkillSequence){int k=((SkillSequence)src).kind;return k==SkillSequence.SMOKE_BLADES||k==SkillSequence.LANCES;}
        if(src instanceof SkillField)return ((SkillField)src).kind==SkillField.ARROWS;
        return src instanceof ArrowRain||src instanceof HeartseekerArrow||src instanceof MeteorFall
                ||src instanceof UndyingWillWard||src instanceof ShieldOfTheFaithfulWard||src instanceof SpiritArmorMotes;
    }

    /** runs r once the current action (the swing, move or zap being resolved) is over, before anyone else acts */
    public static void defer(Runnable r){
        Actor.add(new Actor(){
            {actPriority=VFX_PRIO;}
            @Override protected boolean act(){Actor.remove(this);r.run();return true;}
        });
    }
    /** applies a FlavourBuff after the current hit has landed, so the hit's own damage can't break it
     *  (Frost, Paralysis, Terror recover on damage); does nothing if ch died meanwhile */
    public static <T extends FlavourBuff> void affectAfterHit(Char ch,Class<T> cls,float turns){
        //skills never stun or freeze bosses and minibosses solid
        if((cls==Paralysis.class||cls==Frost.class)&&(Char.hasProp(ch,Char.Property.BOSS)||Char.hasProp(ch,Char.Property.MINIBOSS)))return;
        defer(()->{if(ch.isAlive())Buff.affect(ch,cls,turns);});
    }

    private static float heroMovedAt=-10f;
    /** the hero changed tiles (any move) during the last game turn */
    public static boolean heroMovedLastTurn(){return Actor.now()-heroMovedAt<=1f;}
    /** whole turns the hero has stood still, counted from the last tile change: the ramp
     *  the Guard and Bulwark rarity perks read */
    public static int turnsStill(){float still=Actor.now()-heroMovedAt;return still<=0f?0:(int)Math.min(99f,still);}

    /** Char.attack miss branch, when the defender is the hero */
    public static void heroMissedBy(Char attacker){
        Hero hero=Dungeon.hero;if(hero==null||hero.heroSkills==null||attacker==null)return;
        final boolean melee=Dungeon.level.adjacent(attacker.pos,hero.pos);
        defer(()->{if(hero.isAlive()&&attacker.isAlive())hero.heroSkills.onHeroMissed(attacker,melee);});
    }
    /** Mob AI: mob noticed the hero */
    public static void heroNoticed(Mob mob,boolean wasSleeping){
        Hero hero=Dungeon.hero;if(hero==null||hero.heroSkills==null)return;
        defer(()->{if(hero.isAlive()&&mob.isAlive())hero.heroSkills.onHeroNoticed(mob,wasSleeping);});
    }

    private static boolean magicDispatch;
    /** call before a hero wand zap or bolt spell damages target; pass the result to afterMagicHit */
    public static int beforeMagicHit(Char target,Object source){
        Hero hero=Dungeon.hero;
        if(target==null)return 0;
        int before=target.HP+target.shielding();
        if(magicDispatch||hero==null||hero.heroSkills==null||target==hero)return before;
        magicDispatch=true;
        try{hero.heroSkills.beforeMagicHit(target,source);}finally{magicDispatch=false;}
        return before;
    }
    /** call right after the zap or spell resolved; fires onMagicDamage when an enemy lost health or shield */
    public static void afterMagicHit(Char target,int before,Object source){
        Hero hero=Dungeon.hero;
        if(magicDispatch||target==null||hero==null||hero.heroSkills==null||target.alignment!=Char.Alignment.ENEMY)return;
        int dealt=before-(target.isAlive()?target.HP+target.shielding():0);
        if(dealt<=0)return;
        magicDispatch=true;
        try{hero.heroSkills.onMagicDamage(target,dealt,source);}finally{magicDispatch=false;}
    }
    public static boolean valid(int c){return Dungeon.level!=null && c>=0 && c<Dungeon.level.length();}
    public static List<Integer> area(int center,int radius){
        ArrayList<Integer> out=new ArrayList<>();
        if(!valid(center))return out;
        int w=Dungeon.level.width(),x=center%w,y=center/w;
        for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++){
            int nx=x+dx,ny=y+dy;
            if(nx<0||nx>=w||ny<0||ny>=Dungeon.level.height())continue;
            int c=nx+ny*w;
            if(!Dungeon.level.solid[c] && clear(center,c))out.add(c);
        }
        return out;
    }
    public static boolean clear(int from,int to){return valid(from)&&valid(to)&&!Dungeon.level.solid[to]
            && new Ballistica(from,to,Ballistica.STOP_TARGET|Ballistica.STOP_SOLID).collisionPos==to;}
    public static void flare(int cell,int color){
        if(!valid(cell)||Dungeon.hero==null||Dungeon.hero.sprite==null||Dungeon.hero.sprite.parent==null||!Dungeon.level.heroFOV[cell])return;
        new Flare(3,4).color(color,true).show(Dungeon.hero.sprite.parent,DungeonTilemap.tileCenterToWorld(cell),.2f);
    }
    private static void hurt(Char victim,int damage){
        if(victim==null||!victim.isAlive()||damage<=0)return;
        boolean previous=secondary;secondary=true;
        try{victim.damage(damage,SkillInteractions.class);}finally{secondary=previous;}
    }
    public static void blast(int cell,int radius,int damage,int color){
        for(int c:area(cell,radius)){
            Char ch=Actor.findChar(c);
            if(ch!=null && ch.alignment==Char.Alignment.ENEMY)hurt(ch,damage);
            flare(c,color);
        }
    }
    public static Mark get(Char ch,int kind){for(Mark m:ch.buffs(Mark.class))if(m.kind==kind&&m.sameFloor())return m;return null;}
    public static Mark mark(Char ch,int kind,int rank,int turns){
        Mark m=get(ch,kind);if(m==null)m=Buff.append(ch,Mark.class);
        m.depth=Dungeon.depth;m.branch=Dungeon.branch;m.kind=kind;m.rank=rank;m.left=turns;m.type=(kind<=2||kind==4||kind==5)?Buff.buffType.NEGATIVE:Buff.buffType.POSITIVE;m.fx(true);return m;
    }
    public static void push(Char ch,int from,int distance,int damage){
        if(ch==null||!ch.isAlive()||ch.properties().contains(Char.Property.IMMOVABLE))return;
        int w=Dungeon.level.width(),dx=Integer.signum(ch.pos%w-from%w),dy=Integer.signum(ch.pos/w-from/w);
        if(dx==0&&dy==0)return;
        Mark stagger=get(ch,Mark.STAGGER);
        if(stagger!=null){distance+=1+stagger.rank/2;stagger.detach();}
        for(int i=0;i<distance&&ch.isAlive();i++){
            int x=ch.pos%w+dx,y=ch.pos/w+dy,c=x+y*w;
            if(x<0||x>=w||y<0||y>=Dungeon.level.height())break;
            Char other=Actor.findChar(c);
            if(Dungeon.level.solid[c]||other!=null){
                hurt(ch,damage);if(other!=null&&other.alignment==Char.Alignment.ENEMY)hurt(other,damage/2);
                if(stagger!=null){blast(ch.pos,1,damage/2,0xDEB37C);if(other!=null&&stagger.rank>=2)mark(other,Mark.STAGGER,stagger.rank,3);}
                if(ch.isAlive())Buff.prolong(ch,Roots.class,1f);
                flare(ch.pos,0xDEB37C);break;
            }
            if(!Dungeon.level.passable[c] || Dungeon.level.pit[c])break;
            ch.move(c,false);if(ch.sprite!=null)ch.sprite.snapToPosition(c);
        }
    }
    public static void lure(int center,int rank){
        for(Mob mob:Dungeon.level.mobs.toArray(new Mob[0])){
            if(mob.alignment==Char.Alignment.ENEMY&&Dungeon.level.distance(center,mob.pos)<=3+rank
                    && !mob.properties().contains(Char.Property.BOSS)
                    && (mob.fieldOfView==null || !mob.fieldOfView[Dungeon.hero.pos] || Dungeon.hero.invisible>0)){
                mob.beckon(center);
            }
        }
    }
    public static void detonateRadiance(SkillField field){
        for(Char ch:Actor.chars().toArray(new Char[0])){
            Mark mark=get(ch,Mark.RADIANT);
            if(mark!=null&&mark.other==field.id()){
                int damage=mark.power*(2+field.rank);mark.detach();hurt(ch,damage);flare(ch.pos,0xFFF1A1);
            }
        }
    }
    public static void smite(Char victim){
        Hero hero=Dungeon.hero;if(hero==null)return;
        for(SkillField f:hero.buffs(SkillField.class).toArray(new SkillField[0]))
            if(f.kind==SkillField.SANCTUARY&&f.contains(victim.pos)){detonateRadiance(f);f.detach();}
    }
    public static boolean snapAnchor(Hero hero,int cell){
        for(Char victim:Actor.chars()){
            Mark chain=get(victim,Mark.CHAIN);
            if(chain==null||chain.rank<2||chain.cell!=cell||!victim.isAlive())continue;
            chain.detach();
            if(valid(cell)&&Dungeon.level.passable[cell]&&Actor.findChar(cell)==null&&clear(victim.pos,cell)){
                victim.move(cell,false);if(victim.sprite!=null)victim.sprite.snapToPosition(cell);
            }
            hurt(victim,5+3*chain.rank);flare(cell,0xC6D1DE);return true;
        }
        return false;
    }
    public static void onMove(Char ch,int previous){onMove(ch,previous,true);}
    public static void onMove(Char ch,int previous,boolean travelling){
        Hero hero=Dungeon.hero;if(hero==null||Dungeon.level==null)return;
        if(ch==hero){Parkour.heroMoved();heroMovedAt=Actor.now();}
        if(hero.heroSkills!=null){
            hero.heroSkills.onCharMoved(ch,previous,travelling);
            if(ch!=hero&&travelling&&ch.alignment==Char.Alignment.ENEMY&&ch.isAlive()&&Dungeon.level.adjacent(ch.pos,hero.pos)
                    &&!(valid(previous)&&Dungeon.level.adjacent(previous,hero.pos))){
                final int from=previous, arrival=ch.pos, movement=ch.movementVersion();
                final xyz.gabriwar.warpedpixeldungeon.levels.Level floor=Dungeon.level;
                defer(()->{if(Dungeon.level==floor&&Dungeon.hero==hero&&hero.isAlive()&&ch.isAlive()
                        &&ch.pos==arrival&&ch.movementVersion()==movement&&Dungeon.level.adjacent(ch.pos,hero.pos))
                    hero.heroSkills.onEnemyStepsAdjacent(ch,from);});
            }
        }
        Mark chain=get(ch,Mark.CHAIN);
        if(chain!=null && Dungeon.level.distance(chain.cell,ch.pos)>1){
            if(valid(previous)&&Actor.findChar(previous)==null){ch.pos=previous;if(ch.sprite!=null)ch.sprite.snapToPosition(previous);}
            hurt(ch,2+chain.rank);chain.power++;
            if(chain.power>=(ch.properties().contains(Char.Property.BOSS)?1:2+chain.rank))chain.detach();
            flare(ch.pos,0xB1BDCC);
        }
        int iceRank=0;
        for(SkillField f:hero.buffs(SkillField.class).toArray(new SkillField[0])){
            if(!f.contains(ch.pos))continue;
            if(f.kind==SkillField.ICE && ch.pos!=previous && ch.alignment==Char.Alignment.ENEMY)
                iceRank=Math.max(iceRank,f.rank);
            if(f.kind==SkillField.ARROWS && ch.alignment==Char.Alignment.ENEMY){hurt(ch,2+f.rank);Buff.prolong(ch,Cripple.class,2f);f.removeCell(ch.pos);}
            if(f.kind==SkillField.SOUL && ch==hero){hero.MP=Math.min(hero.MT+xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic.manaBonus(hero),hero.MP+Math.min(20,Math.round(hero.MT*.04f)));f.detach();flare(ch.pos,0xBCA9FF);}
        }
        // Overlapping novas use the strongest spikes, once per entered tile.
        if(iceRank>0&&ch.isAlive()){
            hurt(ch,1+2*iceRank);
            if(ch.isAlive())Buff.prolong(ch,Chill.class,2f);
            flare(ch.pos,0x86DDF3);
        }
    }
    public static int beforeDamage(Char ch,int damage,Object source){
        if(secondary || damage<=0 || Dungeon.hero==null || Skill.isTickDamage(source))return damage;
        Hero hero=Dungeon.hero;
        Mark deflect=get(ch,Mark.DEFLECT);
        if(deflect!=null && source instanceof Char && !Dungeon.level.adjacent(ch.pos,((Char)source).pos)){
            Char attacker=(Char)source;
            if(deflect.rank>=3){Char aimed=Actor.findChar(deflect.cell);if(aimed!=null&&aimed.alignment==Char.Alignment.ENEMY&&clear(ch.pos,aimed.pos))attacker=aimed;}
            hurt(attacker,Math.max(1,damage/2));flare(ch.pos,0xDCEBFF);if(--deflect.power<=0)deflect.detach();return 0;
        }
        Mark shards=get(ch,Mark.SHARDS);
        if(shards!=null){
            int blocked=Math.min(damage,Math.min(3+shards.rank,shards.power));
            damage-=blocked;shards.power-=blocked;flare(ch.pos,0xFFF3B3);
            Char closest=null;
            for(Char other:Actor.chars())if(other.alignment==Char.Alignment.ENEMY&&other.isAlive()
                    && Dungeon.level.distance(ch.pos,other.pos)<=3&&clear(ch.pos,other.pos)){closest=other;break;}
            if(closest!=null)hurt(closest,blocked+shards.rank);
            if(shards.power<=0)shards.detach();
        }
        return damage;
    }
    public static void onHeroHit(Hero hero,Char enemy,int damage,boolean ranged){
        if(secondary||enemy==null)return;
        for(SkillField f:hero.buffs(SkillField.class).toArray(new SkillField[0])){
            if(!f.sameFloor())continue;
            if(ranged && f.kind==SkillField.ARROWS && f.rank>=2){
                for(int cell:f.cells.clone())if(Dungeon.level.distance(cell,enemy.pos)<=2&&clear(cell,enemy.pos)){
                    hurt(enemy,2+f.rank);f.removeCell(cell);flare(cell,0xBDD798);break;
                }
            }
            if(f.kind==SkillField.SMOKE&&f.contains(hero.pos)){
                for(Char other:Actor.chars())if(other!=enemy&&other.alignment==Char.Alignment.ENEMY&&other.isAlive()
                        && Dungeon.level.distance(f.origin,other.pos)<=3){hurt(other,damage/2);flare(other.pos,0xBBB0DD);break;}
                f.detach();break;
            }
        }
    }
    public static class LinkedDamage {}
    /** Copy actual health damage once, including spells and DOT; never echo an echo. */
    public static void shareLinkedDamage(Char victim,int damage,Object source){
        if(damage<=0||source instanceof LinkedDamage||victim.alignment!=Char.Alignment.ENEMY)return;
        Mark link=get(victim,Mark.CUT);if(link==null)return;
        Actor actor=Actor.findById(link.other);
        if(!(actor instanceof Char))return;
        Char other=(Char)actor;Mark reciprocal=get(other,Mark.CUT);
        if(!other.isAlive()||other.alignment!=Char.Alignment.ENEMY||reciprocal==null||reciprocal.other!=victim.id())return;
        int shared=Math.round(damage*(other==victim?DoubleStab.focusedPercent(link.rank):DoubleStab.sharePercent(link.rank))/100f);
        if(shared>0){other.damage(shared,new LinkedDamage());flare(other.pos,0xD66AAB);}
    }
    public static class Mark extends Buff {
        public static final int STAGGER=0,FROST=1,RADIANT=2,SHARDS=3,CUT=4,CHAIN=5,GUARD=6,LEAP=7,LUNGE=8,DEFLECT=9,RETREAT=10;
        public int kind,rank,left=3,power,cell=-1,other=-1;
        private SkillMarkFX visual;
        private int depth=Dungeon.depth,branch=Dungeon.branch;
        public boolean sameFloor(){return (kind!=CHAIN&&kind!=RETREAT&&kind!=DEFLECT&&kind!=CUT) || depth==Dungeon.depth&&branch==Dungeon.branch;}
        {type=buffType.POSITIVE;}
        @Override public boolean act(){
            if(!sameFloor()||--left<=0){detach();return true;}
            if(kind==CUT){
                Actor linked=Actor.findById(other);
                if(!(linked instanceof Char)||!((Char)linked).isAlive()||((Char)linked).alignment!=Char.Alignment.ENEMY){detach();return true;}
            }
            if(kind==SHARDS&&rank>=2&&power>0){
                for(Char ally:Actor.chars())if(ally!=target&&ally.alignment==Char.Alignment.ALLY&&ally.HP<ally.HT
                        && Dungeon.level.distance(target.pos,ally.pos)<=2 && SkillInteractions.clear(target.pos,ally.pos)){
                    int heal=Math.min(ally.HT-ally.HP,Math.min(power,rank));ally.HP+=heal;power-=heal;flare(ally.pos,0xFFF1AB);break;
                }
            }
            spend(TICK);return true;
        }
        @Override public String name(){return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get(Mark.class,"name_"+kind);}
        @Override public String desc(){
            if(kind==CUT)return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get(DoubleStab.class,
                    target!=null&&other==target.id()?"focus_desc":"link_desc",
                    target!=null&&other==target.id()?DoubleStab.focusedPercent(rank):DoubleStab.sharePercent(rank),left);
            return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get(Mark.class,"desc",name(),left,power);
        }
        @Override public int icon(){return BuffIndicator.NONE;}
        @Override public void detach(){
            Char carrier=target;super.detach();
            Hero hero=Dungeon.hero;
            if(carrier!=null&&hero!=null&&hero.heroSkills!=null)hero.heroSkills.onSkillMarkEnded(carrier,this);
        }
        @Override public void fx(boolean on){
            if(visual!=null){visual.killAndErase();visual=null;}
            if(on&&target!=null&&target.sprite!=null&&target.sprite.parent!=null){
                visual=new SkillMarkFX(this);target.sprite.parent.add(visual);
            }
        }
        @Override public void storeInBundle(Bundle b){super.storeInBundle(b);b.put("mark_depth",depth);b.put("mark_branch",branch);b.put("kind",kind);b.put("rank",rank);b.put("left",left);b.put("power",power);b.put("cell",cell);b.put("other",other);}
        @Override public void restoreFromBundle(Bundle b){super.restoreFromBundle(b);depth=b.contains("mark_depth")?b.getInt("mark_depth"):Dungeon.depth;branch=b.contains("mark_branch")?b.getInt("mark_branch"):Dungeon.branch;kind=b.getInt("kind");rank=b.getInt("rank");left=b.getInt("left");power=b.getInt("power");cell=b.getInt("cell");other=b.getInt("other");}
    }
}
