/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
 *
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PillarRiseFX;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;

import java.util.*;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.*;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.*;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummonedPet;
import xyz.gabriwar.warpedpixeldungeon.effects.*;
import xyz.gabriwar.warpedpixeldungeon.scenes.*;
import xyz.gabriwar.warpedpixeldungeon.sprites.WraithSprite;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

public class GuardianSpirit extends ActiveSkill3 {
    private static final int WATCH=xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.FULL_CYCLE;
    {name="Guardian Spirit";castText="Watch over me";image=116;mana=10;tier=3;}
    @Override public boolean toggleable(){return false;}
    private SummonedPet guardian(){
        for(Mob mob:Dungeon.level.mobs)if(mob instanceof SummonedPet && mob.isAlive() && mob.alignment==Char.Alignment.ALLY
                && SkillInteractions.get(mob,SkillInteractions.Mark.GUARD)!=null)return (SummonedPet)mob;
        return null;
    }
    // At level 3 a living guardian can be recast for free to spend the halo it charged by guarding.
    private SkillInteractions.Mark chargedHalo(){
        SummonedPet spirit=guardian();if(spirit==null||level<MAX_LEVEL)return null;
        SkillInteractions.Mark guard=SkillInteractions.get(spirit,SkillInteractions.Mark.GUARD);
        return guard!=null&&guard.power>0?guard:null;
    }
    @Override public ArrayList<String> actions(Hero hero){
        ArrayList<String> out=new ArrayList<>();
        if(level>0&&(guardian()==null?canPayMana(hero,getManaCost()):level>=MAX_LEVEL))out.add(AC_SUMMON);return out;
    }
    @Override public void execute(Hero hero,String action){
        if(!AC_SUMMON.equals(action)||level<=0)return;
        GameScene.selectCell(new CellSelector.Listener(){
            @Override public String prompt(){return Messages.get(GuardianSpirit.class,guardian()==null?"place":"command");}
            @Override public void onSelect(Integer cell){
                if(cell==null||!SkillInteractions.valid(cell)||!Dungeon.level.heroFOV[cell])return;
                SummonedPet spirit=guardian();
                if(spirit==null){
                    if(!canPayMana(hero,getManaCost()))return;
                    if(SummonedPet.activeCount()>=3+hero.heroSkills.allSummonLimit()){warn("limit");return;}
                    if(Dungeon.level.distance(hero.pos,cell)>6||!SkillInteractions.clear(hero.pos,cell)){warn("too_far");return;}
                    if(!Dungeon.level.passable[cell]||Dungeon.level.pit[cell]||Actor.findChar(cell)!=null){warn("bad_tile");return;}
                    spirit=new SummonedPet(xyz.gabriwar.warpedpixeldungeon.sprites.SeraphGuardianSprite.class);spirit.name=Messages.get(GuardianSpirit.class,"name");
                    spirit.setLevel(level*2);spirit.setStats(2+level,5+3*level,level);spirit.setHealthShare(new float[]{1f,1.5f,2f}[Math.max(0,Math.min(2,level-1))]);spirit.setDamageShare(new float[]{.6f,.8f,1f}[Math.max(0,Math.min(2,level-1))]);spirit.pos=cell;
                    GameScene.add(spirit);
                    //the guardian fades in under a pillar of pale light, shafts rising round it
                    if(spirit.sprite!=null&&spirit.sprite.parent!=null){
                        spirit.sprite.alpha(0);
                        spirit.sprite.parent.add(new com.watabou.noosa.tweeners.AlphaTweener(spirit.sprite,1f,0.8f));
                        spirit.sprite.emitter().start(xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle.FACTORY,0.12f,5);
                    }
                    PillarRiseFX.show(cell,0xDAE9FF,()->Sample.INSTANCE.play(Assets.Sounds.CHARMS,1f,0.8f));
                    Sample.INSTANCE.play(Assets.Sounds.CHARGEUP,0.7f,0.9f);
                    // the watch lasts one in-game day
                    SkillInteractions.Mark guard=SkillInteractions.mark(spirit,SkillInteractions.Mark.GUARD,level,WATCH);
                    guard.cell=cell;Buff.prolong(spirit,Roots.class,WATCH);
                    payMana(hero,getManaCost());SkillInteractions.flare(cell,0xDAE9FF);
                    SkillInteractions.flare(cell,0xE9BC59);
                }else if(level>=MAX_LEVEL){
                    SkillInteractions.Mark guard=SkillInteractions.get(spirit,SkillInteractions.Mark.GUARD);
                    if(guard==null)return;
                    Char target=Actor.findChar(cell);
                    if(target==spirit)return;
                    if(target!=null){
                        // an enemy: the eye's laser always fires, and a halo charged by guarding adds to it
                        if(target.alignment!=Char.Alignment.ENEMY){warn("not_enemy");return;}
                        if(Dungeon.level.distance(spirit.pos,cell)>6||!SkillInteractions.clear(spirit.pos,cell)){warn("no_line");return;}
                        if(spirit.sprite instanceof xyz.gabriwar.warpedpixeldungeon.sprites.SeraphGuardianSprite)
                            ((xyz.gabriwar.warpedpixeldungeon.sprites.SeraphGuardianSprite)spirit.sprite).laser(cell);
                        target.damage(3+3*level,GuardianSpirit.this);
                        if(guard.power>0&&target.isAlive())target.damage(guard.power,SummonedPet.GuardianLaser.class);
                        guard.power=0;
                        SkillFX.flash(target);SkillInteractions.flare(cell,0xFFF1A1);
                    }else{
                        // any open tile you have seen on this floor: the eye blinks there and holds it
                        if(!Dungeon.level.visited[cell]&&!Dungeon.level.mapped[cell]){warn("unknown_tile");return;}
                        if(!Dungeon.level.passable[cell]||Dungeon.level.pit[cell]){warn("bad_tile");return;}
                        SkillInteractions.flare(spirit.pos,0xDAE9FF);
                        xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation.appear(spirit,cell);
                        Dungeon.level.occupyCell(spirit);
                        guard.cell=cell;Buff.prolong(spirit,Roots.class,guard.left);
                        SkillInteractions.flare(cell,0xE9BC59);
                        Dungeon.observe();GameScene.updateFog();
                    }
                }else return;
                hero.heroSkills.lastUsed=GuardianSpirit.this;hero.spendAndNext(TIME_TO_USE);
            }
        });
    }
    private static void warn(String key){xyz.gabriwar.warpedpixeldungeon.utils.GLog.w(Messages.get(GuardianSpirit.class,key));}
    // the watch is over: the eye closes and fades instead of lingering as a plain ally
    @Override public void onSkillMarkEnded(Char ch,SkillInteractions.Mark mark){
        if(mark.kind!=SkillInteractions.Mark.GUARD||!(ch instanceof SummonedPet)||!ch.isAlive())return;
        SkillInteractions.flare(ch.pos,0xDAE9FF);
        final Char eye=ch;
        SkillInteractions.defer(()->{if(eye.isAlive())eye.die(GuardianSpirit.this);});
    }
    @Override public int getManaCost(){return (int)Math.ceil(mana*(1+.5*level));}
    @Override protected boolean upgrade(){return true;}
}
