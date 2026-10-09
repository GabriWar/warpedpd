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


import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;

import java.util.ArrayList;

public class WildCall extends SubSkill3 {

	{
		name = "Wild Call";
		castText = "To me!";
		image = 181;
		mana = 10;
		tier = 3;
	}

	public static final float HUNT_TURNS = 10f;

	@Override
	public boolean toggleable(){ return false; }

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.selectCell( new Marker() );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	//mark a visible enemy: a spirit wolf bursts from the brush and hunts only that prey
	private class Marker extends xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector.Listener {

		@Override
		public void onSelect( Integer target ){
			if (target == null) return;
			Hero hero = Dungeon.hero;
			if (level <= 0 || hero.MP < getManaCost()) return;
			xyz.gabriwar.warpedpixeldungeon.actors.Char prey = SkillInteractions.valid( target ) && Dungeon.level.heroFOV[target]
					? xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar( target ) : null;
			if (prey == null || prey == hero || prey.alignment != xyz.gabriwar.warpedpixeldungeon.actors.Char.Alignment.ENEMY){
				xyz.gabriwar.warpedpixeldungeon.utils.GLog.w( xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( WildCall.class, "no_target" ) );
				return;
			}
			Buff.affect( prey, SpiritHunt.class ).set( level, HUNT_TURNS );
			hero.MP -= getManaCost();
			castTextYell();
			SpatialSound.play( Assets.Sounds.TRAMPLE, prey, 1f, 0.9f );
			SpatialSound.playDelayed( Assets.Sounds.GRASS, 0.2f, prey, 1f, 1.1f );
			hero.sprite.emitter().burst( Speck.factory( Speck.STAR ), 4 );
			xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.get( prey.pos ).burst( xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle.GENERAL, 10 );
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
		}

		@Override
		public String prompt(){
			return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( WildCall.class, "prompt" );
		}
	}

	//+3: when its prey falls, the wolf picks up the trail of the nearest enemy in sight
	@Override
	public void onEnemyDeath( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob mob, Object cause ){
		if (level < MAX_LEVEL || mob == null) return;
		SpiritHunt hunt = mob.buff( SpiritHunt.class );
		if (hunt == null || hunt.left <= 0) return;
		final float left = hunt.left;
		final int from = mob.pos;
		xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob next = null;
		for (xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob m : Dungeon.level.mobs.toArray( new xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob[0] )){
			if (m == mob || !m.isAlive() || m.alignment != xyz.gabriwar.warpedpixeldungeon.actors.Char.Alignment.ENEMY
					|| !Dungeon.level.heroFOV[m.pos] || m.buff( SpiritHunt.class ) != null) continue;
			if (next == null || Dungeon.level.distance( from, m.pos ) < Dungeon.level.distance( from, next.pos )) next = m;
		}
		if (next == null) return;
		final xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob prey = next;
		SkillInteractions.defer( () -> {
			if (!prey.isAlive()) return;
			Buff.affect( prey, SpiritHunt.class ).set( level, left );
			xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.get( prey.pos ).burst( xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle.GENERAL, 8 );
			SpatialSound.play( Assets.Sounds.TRAMPLE, prey, 0.8f, 1.2f );
		} );
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }

	/** the spirit wolf on this enemy's trail: it cannot be struck, and it bites every turn until the hunt ends */
	public static class SpiritHunt extends Buff implements SkillInteractions.HeroDamageSource {

		{
			type = buffType.NEGATIVE;
		}

		int rank = 1;
		float left = HUNT_TURNS;

		public void set( int rank, float turns ){
			this.rank = rank;
			this.left = turns;
		}

		@Override
		public boolean rangedSource(){ return false; }

		@Override
		public boolean act(){
			if (!target.isAlive()){
				detach();
				return true;
			}
			int bite = com.watabou.utils.Random.NormalIntRange( 2 + 2 * rank, 7 + 2 * rank );
			target.damage( bite, this );
			xyz.gabriwar.warpedpixeldungeon.effects.Wound.hit( target );
			if (target.sprite != null && Dungeon.level.heroFOV[target.pos]){
				target.sprite.emitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle.GENERAL, 4 );
				SpatialSound.play( Assets.Sounds.HIT_SLASH, target, 0.7f, 0.8f );
			}
			left -= TICK;
			if (left <= 0 || !target.isAlive()) detach();
			else spend( TICK );
			return true;
		}

		@Override
		public int icon(){ return xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator.PREPARATION; }

		@Override
		public void tintIcon( com.watabou.noosa.Image icon ){ icon.hardlight( 0.5f, 0.9f, 0.4f ); }

		@Override
		public String desc(){
			return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( this, "desc", (int) left );
		}

		@Override
		public void storeInBundle( com.watabou.utils.Bundle bundle ){
			super.storeInBundle( bundle );
			bundle.put( "rank", rank );
			bundle.put( "left", left );
		}

		@Override
		public void restoreFromBundle( com.watabou.utils.Bundle bundle ){
			super.restoreFromBundle( bundle );
			rank = bundle.getInt( "rank" );
			left = bundle.getFloat( "left" );
		}
	}
}
