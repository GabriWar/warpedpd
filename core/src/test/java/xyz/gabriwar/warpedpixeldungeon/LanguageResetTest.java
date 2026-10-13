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

package xyz.gabriwar.warpedpixeldungeon;

import com.badlogic.gdx.Preferences;
import com.watabou.utils.GameSettings;
import org.junit.After;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.messages.Languages;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

//this update puts a game back in English once; a language chosen after that stays
public class LanguageResetTest {

	@After
	public void unplug(){
		GameSettings.set( null );
	}

	@Test
	public void englishOnceThenThePlayersChoice(){
		GameSettings.set( new MapPrefs() );
		WPDSettings.language( Languages.KOREAN );

		assertTrue( WPDSettings.resetLanguageOnce() );
		assertEquals( Languages.ENGLISH, WPDSettings.language() );

		WPDSettings.language( Languages.KOREAN );
		assertFalse( "only the first start resets", WPDSettings.resetLanguageOnce() );
		assertEquals( Languages.KOREAN, WPDSettings.language() );
	}

	//settings in memory: a test has no settings file
	private static class MapPrefs implements Preferences {
		private final HashMap<String, Object> map = new HashMap<>();

		@Override public Preferences putBoolean( String k, boolean v ){ map.put( k, v ); return this; }
		@Override public Preferences putInteger( String k, int v ){ map.put( k, v ); return this; }
		@Override public Preferences putLong( String k, long v ){ map.put( k, v ); return this; }
		@Override public Preferences putFloat( String k, float v ){ map.put( k, v ); return this; }
		@Override public Preferences putString( String k, String v ){ map.put( k, v ); return this; }
		@Override public Preferences put( Map<String, ?> vals ){ map.putAll( vals ); return this; }
		@Override public boolean getBoolean( String k ){ return getBoolean( k, false ); }
		@Override public int getInteger( String k ){ return getInteger( k, 0 ); }
		@Override public long getLong( String k ){ return getLong( k, 0 ); }
		@Override public float getFloat( String k ){ return getFloat( k, 0 ); }
		@Override public String getString( String k ){ return getString( k, "" ); }
		@Override public boolean getBoolean( String k, boolean d ){ Object v = map.get( k ); return v instanceof Boolean ? (Boolean) v : d; }
		@Override public int getInteger( String k, int d ){ Object v = map.get( k ); return v instanceof Integer ? (Integer) v : d; }
		@Override public long getLong( String k, long d ){ Object v = map.get( k ); return v instanceof Long ? (Long) v : d; }
		@Override public float getFloat( String k, float d ){ Object v = map.get( k ); return v instanceof Float ? (Float) v : d; }
		@Override public String getString( String k, String d ){ Object v = map.get( k ); return v instanceof String ? (String) v : d; }
		@Override public Map<String, ?> get(){ return map; }
		@Override public boolean contains( String k ){ return map.containsKey( k ); }
		@Override public void clear(){ map.clear(); }
		@Override public void remove( String k ){ map.remove( k ); }
		@Override public void flush(){}
	}
}
