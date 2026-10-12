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

package xyz.gabriwar.warpedpixeldungeon.homestead.building;

import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadStore;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Reflection;

import java.util.List;
import java.util.Locale;

/**
 * The words the build menu, the build bar and a look at a built cell use for a piece: its
 * placement rule, a list of materials, what it is made of, its health and the roof over it.
 */
public final class BuildInfo {

	private BuildInfo(){}

	/** The placement rule in words ("Only on water"). */
	public static String placement( BuildPiece piece ){
		return Messages.get( BuildInfo.class, "rule_" + piece.placement.name().toLowerCase( Locale.ENGLISH ) );
	}

	/** A material's name. */
	public static String material( Class<? extends Item> material ){
		Item i = Reflection.newInstance( material );
		return i == null ? "?" : i.name();
	}

	/** A list of materials and counts: "plank x2, rope x1". */
	public static String costs( List<BuildPiece.Cost> cost ){
		StringBuilder s = new StringBuilder();
		for (BuildPiece.Cost c : cost){
			if (s.length() > 0) s.append( ", " );
			s.append( Messages.get( BuildInfo.class, "cost", material( c.material ), c.quantity ) );
		}
		return s.toString();
	}

	/** The most health any piece has: the scale the menu's health bars are drawn against. */
	public static int strongest(){
		int most = 1;
		for (BuildPiece p : BuildCatalog.all()) most = Math.max( most, p.maxHp );
		return most;
	}

	/**
	 * What a look at a built cell adds (WndInfoCell, the inspect window): what the piece is made
	 * of, its health, "Burns" for a piece fire takes, and the roof over the cell with its health.
	 * Null where nothing is built and no roof stands.
	 */
	public static String cellLines( Level level, int cell ){
		if (!(level instanceof OverworldLevel)) return null;
		StringBuilder s = new StringBuilder();
		BuildPiece p = Homestead.pieceAt( level, cell );
		if (p != null){
			BuildPiece.Cost main = p.mainCost();
			if (main != null) s.append( Messages.get( BuildInfo.class, "made_of", material( main.material ) ) );
			if (p.destructible()){
				if (s.length() > 0) s.append( '\n' );
				s.append( Messages.get( BuildInfo.class, "health", Homestead.hpAt( level, cell ), p.maxHp ) );
			} else {
				if (s.length() > 0) s.append( '\n' );
				s.append( Messages.get( BuildInfo.class, "unbreakable" ) );
			}
			if (p.flammable) s.append( '\n' ).append( Messages.get( BuildInfo.class, "burns" ) );
		}
		String roof = roofLine( (OverworldLevel) level, cell );
		if (roof != null){
			if (s.length() > 0) s.append( '\n' );
			s.append( roof );
		}
		return s.length() == 0 ? null : s.toString();
	}

	/** The roof over a cell in words, with its health and whether it burns now; null for none. */
	public static String roofLine( OverworldLevel level, int cell ){
		int kind = Homestead.roofAt( level, cell );
		if (kind == -1) return null;
		BuildPiece roof = BuildCatalog.forRoof( kind );
		HomesteadStore.Roof r = level.homestead().roofAt( level.worldX + cell % level.width(), level.worldY + cell / level.width() );
		String name = roof != null ? Messages.capitalize( roof.name() ) : "?";
		String line = r != null && roof != null
				? Messages.get( BuildInfo.class, "roof", name, r.hp, roof.maxHp )
				: Messages.get( BuildInfo.class, "roof_plain", name );
		if (r != null && r.burning > 0) line += "\n" + Messages.get( BuildInfo.class, "roof_burning" );
		return line;
	}
}
