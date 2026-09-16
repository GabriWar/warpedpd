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

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.TownInteriorLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.VillageHouseLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

//a roof and a fire: inside the town's buildings and the village houses the hero is
//comfy. The weather stays outside (ClimateManager reads it as an ideal room), and
//neither the cold nor the heat can take hold while it lasts
public class Comfy extends Buff {

	public static final float IDEAL_TEMP = 21f;

	{
		type = buffType.POSITIVE;
		announced = true;
		immunities.add( Hypothermia.class );
		immunities.add( Heatstroke.class );
	}

	//the surface's interiors: the town's six buildings and the village houses
	public static boolean indoors() {
		return indoors( Dungeon.level );
	}

	public static boolean indoors( Level level ) {
		return level instanceof TownInteriorLevel || level instanceof VillageHouseLevel;
	}

	//once a hero turn: on while indoors, gone the moment they step out
	public static void check( Hero hero ) {
		if (indoors()) {
			if (hero.buff( Comfy.class ) == null) Buff.affect( hero, Comfy.class );
		} else {
			Comfy comfy = hero.buff( Comfy.class );
			if (comfy != null) comfy.detach();
		}
	}

	@Override
	public boolean attachTo( Char target ) {
		if (!super.attachTo( target )) return false;
		//whatever the weather had done to you ends at the door
		boolean wasCold = target.buff( Hypothermia.class ) != null;
		Buff.detach( target, Hypothermia.class );
		Buff.detach( target, Heatstroke.class );
		if (wasCold) Badges.validateCameInFromCold();
		return true;
	}

	@Override
	public boolean act() {
		spend( TICK );
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.WELL_FED;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight( 0.6f, 1f, 0.6f );
	}

	@Override
	public String toString() {
		return Messages.get( this, "name" );
	}

	@Override
	public String desc() {
		return Messages.get( this, "desc" );
	}
}
