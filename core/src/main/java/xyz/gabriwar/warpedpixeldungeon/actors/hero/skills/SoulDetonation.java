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


import com.watabou.noosa.Camera;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummonedPet;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class SoulDetonation extends Skill {

	private static final int MAX_DETONATIONS = 3;

	{
		tag = "A4";
		name = "Soul Detonation";
		castText = "Serve me one last time!";
		tier = 4;
		image = 39;
		mana = 10;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_CAST);
		return actions;
	}

    @Override public void execute(Hero hero,String action){
        if(!AC_CAST.equals(action)||level<=0||hero.MP<getManaCost())return;
        ArrayList<SummonedPet> pets=new ArrayList<>();
        for(Mob mob:Dungeon.level.mobs.toArray(new Mob[0]))
            if(mob instanceof SummonedPet && mob.alignment==Char.Alignment.ALLY && mob.isAlive() && Dungeon.level.heroFOV[mob.pos])pets.add((SummonedPet)mob);
        if(pets.isEmpty()){GLog.w(Messages.get(this,"no_summons"));return;}
        String[] options=new String[pets.size()+1];
        for(int i=0;i<pets.size();i++)options[i]=pets.get(i).name()+" ("+(i+1)+")";
        options[pets.size()]=Messages.get(this,"detonate_all");
        xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.show(new xyz.gabriwar.warpedpixeldungeon.windows.WndOptions(name(),Messages.get(this,"choose"),options){
            @Override protected void onSelect(int index){
                if(hero.MP<getManaCost())return;
                ArrayList<SummonedPet> selected=new ArrayList<>();
                if(index==pets.size())selected.addAll(pets.subList(0,Math.min(MAX_DETONATIONS,pets.size())));
                else if(index>=0&&index<pets.size())selected.add(pets.get(index));
                selected.removeIf(p->!p.isAlive()||!Dungeon.level.mobs.contains(p));
                if(selected.isEmpty())return;
                int beat=0;
                for(SummonedPet pet:selected){
                    int origin=pet.pos;final int order=beat++;
                    boolean rat=pet.sprite instanceof xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
                    boolean skeleton=pet.sprite instanceof xyz.gabriwar.warpedpixeldungeon.sprites.SkeletonSprite;
                    if(pet.sprite!=null)pet.sprite.parent.add(new xyz.gabriwar.warpedpixeldungeon.effects.Beam.HealthRay(pet.sprite.center(),hero.sprite.center()));
                    CellEmitter.bottom(origin).burst(ShadowParticle.UP,12);
                    SkillInteractions.blast(origin,skeleton?2:1,4+3*level,0xBBA7EE);
                    if(rat)for(int c:SkillInteractions.area(origin,1)){
                        Char enemy=Actor.findChar(c);
                        if(enemy!=null&&enemy.alignment==Char.Alignment.ENEMY)xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.affect(enemy,xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison.class).set(2+level);
                    }
                    pet.die(SoulDetonation.this);
                    //the souls go up one after another: a violet flare where each stood, a jolt, the boom climbing, then its light reaching the mage
                    if(hero.sprite!=null&&hero.sprite.parent!=null)new xyz.gabriwar.warpedpixeldungeon.effects.Flare(6,16).color(0xBBA7EE,true)
                        .show(hero.sprite.parent,xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld(origin),0.5f);
                    xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX.after(0.1f*order,()->{
                        Camera.main.shake(2,0.2f);Sample.INSTANCE.play(Assets.Sounds.BLAST,0.8f,0.9f+0.1f*order);
                    });
                    xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX.after(0.1f*order+0.3f,()->{
                        if(hero.sprite!=null)hero.sprite.emitter().burst(Speck.factory(Speck.BLUE_LIGHT),3);
                    });
                    //at mastery each sacrifice leaves a soul mote that returns mana when stepped on
                    if(level>=MAX_LEVEL)xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField.place(hero,
                        xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField.SOUL,level,6,java.util.Collections.singleton(origin));
                }
                hero.MP-=getManaCost();castTextYell();xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility.dispel();
                hero.spendAndNext(TIME_TO_USE);
            }
        });
        hero.heroSkills.lastUsed=this;
    }

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public String info(){
		return Messages.get(this, "desc") + "\n"
				+ costUpgradeInfo();
	}
}
