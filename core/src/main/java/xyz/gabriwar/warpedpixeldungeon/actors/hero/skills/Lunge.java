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
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillSequence;

import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.*;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.*;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.*;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfForce;
import xyz.gabriwar.warpedpixeldungeon.scenes.*;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

public class Lunge extends ActiveSkill1 {

	//damage comes from the weapon or strength, which already grow with the hero
	@Override
	public boolean weaponScaled(){ return true; }

    {name="Lunge";castText="Lunge!";image=97;mana=3;}
    @Override public boolean toggleable(){return false;}
    @Override public void restoreInBundle(com.watabou.utils.Bundle bundle){super.restoreInBundle(bundle);active=false;}
    @Override public ArrayList<String> actions(Hero hero){
        ArrayList<String> out=new ArrayList<>();if(level>0&&hero.MP>=getManaCost())out.add(AC_CAST);return out;
    }
    @Override public void execute(Hero hero,String action){
        if(!AC_CAST.equals(action)||level<=0)return;
        GameScene.selectCell(new CellSelector.Listener(){
            @Override public String prompt(){return Messages.get(Lunge.class,"prompt");}
            @Override public void onSelect(Integer cell){
                if(cell==null||hero.rooted||!SkillInteractions.valid(cell)||!Dungeon.level.heroFOV[cell])return;
                SkillInteractions.Mark follow=SkillInteractions.get(hero,SkillInteractions.Mark.LUNGE);
                Char enemy=Actor.findChar(cell);
                if(enemy==null||enemy.alignment!=Char.Alignment.ENEMY||!enemy.isAlive()||hero.MP<getManaCost()
                        ||Dungeon.level.distance(hero.pos,cell)>2+level||!SkillInteractions.clear(hero.pos,cell))return;
                if(follow!=null&&(level<2||follow.other==enemy.id()||follow.power>=level))return;
                int landing=-1,from=hero.pos;
                for(int c:SkillInteractions.area(cell,1))if((Actor.findChar(c)==null||c==hero.pos)&&Dungeon.level.passable[c]
                        &&!Dungeon.level.pit[c]&&SkillInteractions.clear(from,c)){
                    if(landing<0||Dungeon.level.distance(from,c)>Dungeon.level.distance(from,landing))landing=c;
                }
                if(landing<0)return;
                int chain=follow==null?1:follow.power+1;
                if(follow!=null)follow.detach();hero.MP-=getManaCost();hero.busy();Invisibility.dispel();
                final int land=landing;
                //the run: a whoosh and a thin white streak shooting ahead of the duelist
                SpatialSound.play(Assets.Sounds.MISS,hero,1f,1.3f+0.1f*(chain-1));
                StreakFX.show(from,land,0xFFFFFF,0.5f,0.35f);
                hero.sprite.jump(from,land,()->{
                    hero.move(land,false);hero.sprite.place(land);Dungeon.observe();
                    KindOfWeapon weapon=hero.belongings.weapon();
                    int damage=Math.round((weapon==null?RingOfForce.damageRoll(hero):weapon.damageRoll(hero))*1.25f);
                    if(weapon!=null)damage=weapon.proc(hero,enemy,damage);
                    enemy.damage(damage,hero);SkillFX.flash(enemy);
                    SkillSpectacleFX.fly(SkillSpectacleFX.SABER,from,land,0,.4f);
                    //the landing: a puff of dust under her feet, the point driven home with a stab
                    CellEmitter.bottom(land).burst(Speck.factory(Speck.DUST),4);
                    if(enemy.sprite!=null)enemy.sprite.emitter().burst(Speck.factory(Speck.STAR),4);
                    SpatialSound.play(Assets.Sounds.HIT_STAB,enemy,1f,1.1f+0.1f*(chain-1));
                    hero.sprite.showStatus(xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite.NEUTRAL,chain>1?castText()+" x"+chain:castText());
                    if(level>=MAX_LEVEL){
                        java.util.ArrayList<Integer> lane=new xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica(from,land,
                                xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica.STOP_TARGET|xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica.STOP_SOLID).path;
                        SkillSequence.start(hero,SkillSequence.CROSSCUT,level,from,Math.max(1,damage/3),1,
                                lane.subList(0,Math.min(lane.size(),Dungeon.level.distance(from,land)+1)));
                    }
                    {SkillInteractions.Mark step=SkillInteractions.mark(hero,SkillInteractions.Mark.LUNGE,level,2);step.power=chain;step.other=enemy.id();}
                    hero.heroSkills.lastUsed=Lunge.this;hero.spendAndNext(TIME_TO_USE);
                });
            }
        });
    }
    static void stanceTaken(Hero hero){if(hero.sprite!=null)SkillInteractions.flare(hero.pos,0xCCDFFF);}
    @Override public int getManaCost(){return (int)Math.ceil(mana*(1+.55*level));}
    @Override protected boolean upgrade(){return true;}
}
