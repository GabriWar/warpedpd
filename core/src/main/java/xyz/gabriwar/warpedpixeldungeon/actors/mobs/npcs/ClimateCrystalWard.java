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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.ClimateCrystal;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ClimateCrystalSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

/**
 * A Climate Crystal set into the ground. For one in-game day it pulls each open tile
 * within RADIUS toward a mild temperature, feeding heat into the cold ones and drawing
 * it out of the hot ones through the same tile-heat field fire and frost write into.
 * Then it goes dim and does nothing until the hero picks it back up, empty. It cannot
 * be hurt or targeted.
 */
public class ClimateCrystalWard extends NPC {

	public static final int DURATION = DayNightCycle.FULL_CYCLE;
	public static final int RADIUS = 2;
	public static final float TARGET_TEMP = 20f;
	//how hard each turn pulls toward the target; under ~2 it settles without flickering
	private static final float PULL = 1.5f;

	{
		spriteClass = ClimateCrystalSprite.class;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;
	}

	private ClimateCrystal crystal;
	private int left = DURATION;

	public void hold( ClimateCrystal crystal, int turns ) {
		this.crystal = crystal;
		left = turns;
	}

	public boolean dormant() {
		return left <= 0;
	}

	@Override
	protected boolean act() {
		if (dormant()) {
			spend( TICK );
			return true;
		}

		int w = Dungeon.level.width(), x0 = pos % w, y0 = pos / w;
		boolean warming = false, cooling = false;
		for (int dy = -RADIUS; dy <= RADIUS; dy++) {
			for (int dx = -RADIUS; dx <= RADIUS; dx++) {
				int x = x0 + dx, y = y0 + dy;
				if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) continue;
				int cell = x + y * w;
				if (Dungeon.level.solid[cell]) continue;
				float diff = TARGET_TEMP - TileTemperature.tileTemp( cell );
				TileTemperature.depositHeat( cell, diff * PULL );
				if (diff > 2f) warming = true;
				else if (diff < -2f) cooling = true;
			}
		}

		//a slow pulse shows which way it is working: embers against the cold, snow against the heat
		if (left % 4 == 0 && sprite != null && Dungeon.level.heroFOV[pos]) {
			if (warming) CellEmitter.get( pos ).burst( FlameParticle.FACTORY, 3 );
			if (cooling) CellEmitter.get( pos ).burst( SnowParticle.FACTORY, 3 );
		}

		if (--left <= 0) {
			if (sprite instanceof ClimateCrystalSprite) ((ClimateCrystalSprite) sprite).dim();
			if (Dungeon.level.heroFOV[pos]) GLog.i( Messages.get( this, "dimmed" ) );
		}

		spend( TICK );
		return true;
	}

	@Override
	public String description() {
		return super.description() + "\n\n" + (dormant()
				? Messages.get( this, "desc_dim" )
				: Messages.get( this, "desc_left", left ));
	}

	@Override
	public int defenseSkill( Char enemy ) {
		return INFINITE_EVASION;
	}

	@Override
	public void damage( int dmg, Object src ) {
		//a crystal of held weather; nothing breaks it
	}

	@Override
	public boolean add( Buff buff ) {
		return false;
	}

	@Override
	public boolean reset() {
		return true;
	}

	//picking it up hands the artifact back, emptied to 0%
	@Override
	public boolean interact( Char c ) {
		if (!(c instanceof Hero)) return true;
		Hero hero = (Hero) c;

		ClimateCrystal item = crystal != null ? crystal : new ClimateCrystal();
		item.empty();
		if (!item.collect( hero.belongings.backpack )) {
			Dungeon.level.drop( item, hero.pos ).sprite.drop();
		}
		GLog.i( Messages.get( this, "picked_up" ) );
		SpatialSound.play( Assets.Sounds.ITEM, hero );
		CellEmitter.get( pos ).burst( Speck.factory( Speck.LIGHT ), 6 );

		crystal = null;
		destroy();
		if (sprite != null) sprite.killAndErase();
		hero.spendAndNext( 1f );
		return true;
	}

	private static final String LEFT = "left";
	private static final String CRYSTAL = "crystal";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LEFT, left );
		if (crystal != null) bundle.put( CRYSTAL, crystal );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		left = bundle.getInt( LEFT );
		if (bundle.contains( CRYSTAL )) crystal = (ClimateCrystal) bundle.get( CRYSTAL );
	}
}
