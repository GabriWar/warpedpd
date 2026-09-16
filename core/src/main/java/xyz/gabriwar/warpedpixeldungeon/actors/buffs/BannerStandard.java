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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

/**
 * Banner: the Champion's standard, planted on the tile where an enemy fell. It stands
 * 6 / 8 / 10 turns; each turn the hero and allies within its reach raise a barrier toward
 * 5% / 10% / 15% of their max health. At +3 enemies inside its reach are Dazed.
 * The hero carries it as a buff so it saves, and so only one banner ever stands.
 */
public class BannerStandard extends Buff {

	public static final int REACH = 2;
	private static final int GOLD = 0xFFD060;

	{
		type = buffType.POSITIVE;
	}

	private int cell = -1;
	private int left;
	private int rank;
	private int depth;
	private int branch;

	private ItemSprite pole;
	private Emitter glow;

	public static void plant( Hero hero, int cell, int rank ){
		BannerStandard banner = hero.buff( BannerStandard.class );
		if (banner == null){
			banner = Buff.affect( hero, BannerStandard.class );
		} else if (banner.onFloor()){
			//pulled up from the old spot
			CellEmitter.get( banner.cell ).burst( Speck.factory( Speck.DUST ), 6 );
		}
		banner.cell = cell;
		banner.rank = rank;
		banner.left = 4 + 2 * rank;
		banner.depth = Dungeon.depth;
		banner.branch = Dungeon.branch;
		banner.fx( true );
		SkillFX.pillar( cell, GOLD );
		Sample.INSTANCE.play( Assets.Sounds.CHARGEUP, 0.7f, 1.3f );
	}

	private boolean onFloor(){
		return cell >= 0 && Dungeon.level != null && cell < Dungeon.level.length()
				&& depth == Dungeon.depth && branch == Dungeon.branch;
	}

	@Override
	public boolean act(){
		if (!onFloor() || !target.isAlive()){
			detach();
			return true;
		}
		for (Char ch : Actor.chars()){
			if (!ch.isAlive() || Dungeon.level.distance( cell, ch.pos ) > REACH
					|| (ch.pos != cell && !SkillInteractions.clear( cell, ch.pos ))) continue;
			if (ch == target || ch.alignment == Char.Alignment.ALLY){
				int cap = Math.max( 1, Math.round( ch.HT * 0.05f * rank ) );
				Barrier barrier = ch.buff( Barrier.class );
				int now = barrier == null ? 0 : barrier.shielding();
				if (now < cap){
					Buff.affect( ch, Barrier.class ).incShield( Math.min( cap - now, Math.max( 1, cap / 4 ) ) );
					if (ch.sprite != null) ch.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 2 );
				}
			} else if (rank >= Skill.MAX_LEVEL && ch.alignment == Char.Alignment.ENEMY){
				Buff.prolong( ch, Daze.class, 2f );
			}
		}
		if (--left <= 0){
			CellEmitter.get( cell ).burst( Speck.factory( Speck.DUST ), 8 );
			Sample.INSTANCE.play( Assets.Sounds.TRAMPLE, 1f, 0.8f );
			detach();
		} else {
			spend( TICK );
		}
		return true;
	}

	//a golden spear-pole planted on the tile with light pouring up from its foot
	@Override
	public void fx( boolean on ){
		if (pole != null){
			pole.killAndErase();
			pole = null;
		}
		if (glow != null){
			glow.on = false;
			glow = null;
		}
		if (!on || !onFloor() || target == null || target.sprite == null || target.sprite.parent == null){
			return;
		}
		pole = new ItemSprite( ItemSpriteSheet.SPEAR );
		pole.hardlight( GOLD );
		pole.place( cell );
		target.sprite.parent.add( pole );
		glow = CellEmitter.center( cell );
		glow.pour( Speck.factory( Speck.YELLOW_LIGHT ), 0.4f );
	}

	private static final String CELL   = "banner_cell";
	private static final String LEFT   = "banner_left";
	private static final String RANK   = "banner_rank";
	private static final String DEPTH  = "banner_depth";
	private static final String BRANCH = "banner_branch";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( CELL, cell );
		bundle.put( LEFT, left );
		bundle.put( RANK, rank );
		bundle.put( DEPTH, depth );
		bundle.put( BRANCH, branch );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		cell = bundle.getInt( CELL );
		left = bundle.getInt( LEFT );
		rank = bundle.getInt( RANK );
		depth = bundle.getInt( DEPTH );
		branch = bundle.getInt( BRANCH );
	}
}
