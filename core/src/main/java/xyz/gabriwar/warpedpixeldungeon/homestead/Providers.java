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


package xyz.gabriwar.warpedpixeldungeon.homestead;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The providers of one of the homesteads' registries (RoomTraits, HeatSources, HomeAnchors,
 * AwaySettlers, HomeActions): the classes named in the registry's fixed list, looked up BY NAME the
 * first time it is read - a part of the homesteads that is not built yet is skipped without a word,
 * so each part adds its own class and edits nothing here - and after them any made in code (a test,
 * a debug scene) with add. A named provider is a public class with a public no-argument
 * constructor implementing the registry's interface; android/proguard-rules.pro keeps the whole
 * homestead package, since the shrinker cannot see a class named in a string.
 */
final class Providers<T> {

	private final String[] names;
	private final Class<T> type;
	private ArrayList<T> loaded;
	private final ArrayList<T> added = new ArrayList<>();
	//what all() hands out until a provider is added or removed
	private List<T> all;

	Providers( Class<T> type, String... names ){
		this.type = type;
		this.names = names;
	}

	/** The named providers that exist, in their list's order, then the ones added in code. */
	synchronized List<T> all(){
		if (all != null) return all;
		if (loaded == null){
			loaded = new ArrayList<>();
			for (String name : names){
				Class<?> cls;
				try {
					cls = Class.forName( name );
				} catch (ClassNotFoundException e){
					continue;
				}
				try {
					loaded.add( type.cast( cls.getConstructor().newInstance() ) );
				} catch (ReflectiveOperationException | ClassCastException e){
					throw new IllegalStateException( name + " is not a public " + type.getName(), e );
				}
			}
		}
		ArrayList<T> out = new ArrayList<>( loaded );
		out.addAll( added );
		all = Collections.unmodifiableList( out );
		return all;
	}

	synchronized void add( T provider ){
		if (added.contains( provider )) return;
		added.add( provider );
		all = null;
	}

	synchronized void remove( T provider ){
		if (added.remove( provider )) all = null;
	}
}
