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
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Piece;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx;
import com.watabou.utils.Random;

public class TrueEdge extends Skill {

	//its damage is already a share of a blow, a hit or a health pool, so it grows with the hero on its own
	@Override
	public boolean weaponScaled(){ return true; }


	private static final float CRIT_MULTIPLIER = 1.5f;

	{
		tag = "PB4";
		name = "True Edge";
		image = 146;
		tier = 4;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//5% / 10% / 15% of melee hits crit; a crit on an adjacent enemy slashes on through
	//1 / 2 / 3 tiles behind it
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (ranged || level <= 0 || hero == null || enemy == null || !enemy.isAlive()
				|| Random.Int( 100 ) >= 5 * level){
			return damage;
		}
		int crit = Math.round( damage * CRIT_MULTIPLIER );
		xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.crit( enemy );
		xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.mulRound( enemy, xyz.gabriwar.warpedpixeldungeon.actors.ledger.Step.ATTACKER, this, damage, CRIT_MULTIPLIER, crit );
		boolean twist = false;
		if (enemy.sprite != null){
			//no word: the red X over it and on its card. The star burst it threw once drew the game's
			//Random once as it started: that one draw stays
			Random.Float();
			PieceFx.crit( hero, enemy );
			//its wound and its spin are the blow's twist (they lead it, or the wound is an accent)
			twist = Blow.offer( new Slash( enemy ) );
			if (!twist) Slash.draw( enemy );
			enemy.sprite.flash();
		}
		if (!twist) SpatialSound.play( Assets.Sounds.HIT_STRONG, enemy, 1f, 1.4f );
		if (level >= MAX_LEVEL){
			Buff.affect( enemy, Bleeding.class ).set( crit / 4f );
		}
		if (Dungeon.level.adjacent( hero.pos, enemy.pos )){
			slashThrough( hero, enemy, crit / 2 );
		}
		return crit;
	}

	/** True Edge's crit as a blow's twist: its wound and its spin, leading or on its accent's beat
	 *  (its sound with them either way). */
	static final class Slash extends Piece {

		Slash( Char enemy ){
			super( -1, Blow.TWIST, Blow.TARGET, enemy );
		}

		static void draw( Char enemy ){
			Wound.hit( enemy );
			ArcSpinFX.at( enemy.pos, 0xFFFFFF, 10, 0.3f, 220, 1100, 0.22f );
		}

		@Override
		public void lead( boolean mayShake ){
			draw( on );
		}

		@Override
		public void accent( float x, float y ){
			sting();
			draw( on );
		}

		@Override
		public void sting(){
			SpatialSound.play( Assets.Sounds.HIT_STRONG, on, 1f, 1.4f );
		}
	}

	//the cut is plain damage dealt by the hero, not another swing, so it can never proc skills again
	private void slashThrough( Hero hero, Char enemy, int damage ){
		int w = Dungeon.level.width();
		int dx = enemy.pos % w - hero.pos % w;
		int dy = enemy.pos / w - hero.pos / w;
		int end = enemy.pos;
		for (int i = 1; i <= level; i++){
			int x = enemy.pos % w + dx * i;
			int y = enemy.pos / w + dy * i;
			if (x < 0 || x >= w || y < 0 || y >= Dungeon.level.height()) break;
			int c = x + y * w;
			if (Dungeon.level.solid[c]) break;
			end = c;
			Char behind = Actor.findChar( c );
			if (behind == null || !behind.isAlive() || behind.alignment != Char.Alignment.ENEMY) continue;
			if (behind.sprite != null) Wound.hit( behind );
			if (level >= MAX_LEVEL) Buff.affect( behind, Bleeding.class ).set( Math.max( 1, damage ) / 4f );
			if (damage > 0) behind.damage( damage, hero );
		}
		if (end != enemy.pos && hero.sprite != null && hero.sprite.parent != null){
			//the edge running on through them: a stroke of steel from her to where it ends (never a beam),
			//sparks of metal scraping off it as it runs
			StreakFX.show( hero.pos, end, 0xB9DEEE, 0.45f, 0.25f, StreakFX.SPARKS );
			SpatialSound.play( Assets.Sounds.HIT_SLASH, hero, 1f, 0.8f );
		}
	}
}
