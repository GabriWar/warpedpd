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


import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.ClosingRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
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
	/** The spirit wolf's grey (ShoutFX.WOLF's), its lunge's length and its bite's: the jaws shut a
	 *  beat after it lands. */
	public static final int WOLF = 0x9EA4B0;
	public static final float LUNGE = 0.2f, BITE_AT = 0.12f, BITE_FOR = 0.35f;

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
			//her call (her own sparkle, never the level-up's stars), the brush parting round the prey
			//and the hunt's grey ring closing on it
			hero.sprite.emitter().burst( SkillBursts.FLOURISH, 4 );
			xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.get( prey.pos ).burst( xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle.GENERAL, 10 );
			marked( prey.sprite );
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

	/** The prey marked for the hunt: a ring of the wolf's grey closing on its feet. Any thread. */
	public static void marked( CharSprite prey ){
		ClosingRing.on( prey, 31, 9, 0.35f, WOLF );
	}

	/** The spirit wolf biting its prey at `prey`, the hero at `hero`: it lunges in from beyond the
	 *  prey (two cells out where the level has them, else one: a grey streak with speed lines), and
	 *  its jaws of light shut on it. Nothing of the game's Random. Any thread. */
	public static void bite( int prey, int hero ){
		if (!SkillVfx.valid( prey )) return;
		int near = beyond( prey, hero ), far = SkillVfx.valid( near ) ? beyond( near, hero ) : -1;
		if (!SkillVfx.valid( far ) || Dungeon.level.solid[far]) far = near;
		if (SkillVfx.valid( far )) StreakFX.dash( far, prey, WOLF, 0.5f, LUNGE, true );
		SkillSpectacleFX.fly( SkillSpectacleFX.JAW, prey, prey, BITE_AT, BITE_FOR, WOLF );
	}

	//the cell beside the prey on its far side from the hero (straight on from him through it)
	static int beyond( int prey, int hero ){
		int w = Dungeon.level.width();
		int dx = Integer.signum( prey % w - hero % w ), dy = Integer.signum( prey / w - hero / w );
		if (dx == 0 && dy == 0) dx = 1;
		int x = prey % w + dx, y = prey / w + dy;
		if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) return -1;
		return x + y * w;
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
				if (Dungeon.hero != null) bite( target.pos, Dungeon.hero.pos );
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
