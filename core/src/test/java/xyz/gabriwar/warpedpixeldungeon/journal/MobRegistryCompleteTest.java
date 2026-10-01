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

package xyz.gabriwar.warpedpixeldungeon.journal;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.Assert.fail;

/**
 * The Bestiary gives every monster a page: its hand-sorted lists come first and the
 * rest are placed from MobRegistry, a generated list of every public, constructible
 * Mob class (Android cannot scan its own classes at run time). This test keeps that
 * registry honest: it scans the compiled classes and, when the registry is stale,
 * REWRITES MobRegistry.java and fails once, so the next build carries every monster.
 */
public class MobRegistryCompleteTest {

	private static final String SOURCE = "src/main/java/xyz/gabriwar/warpedpixeldungeon/journal/MobRegistry.java";

	@Test
	public void everyRegisteredMonsterHasAPage(){
		//loading the Bestiary runs its catch-all: it must stand up, and afterwards
		//every monster is listed, converted to another's page, or deliberately hidden
		int pages = 0;
		for (Bestiary b : Bestiary.values()) pages += b.totalEntities();
		for (Class<?> cls : MobRegistry.ALL){
			boolean listed = false;
			for (Bestiary b : Bestiary.values()) if (b.entities().contains( cls )) listed = true;
			if (!listed && !Bestiary.accountedFor( cls )) fail( cls.getName() + " has no page in the journal" );
		}
		if (pages == 0) fail( "the journal is empty" );
	}

	@Test
	public void bestiaryKnowsEveryMonster() throws Exception {
		Path root = Paths.get( Mob.class.getProtectionDomain().getCodeSource().getLocation().toURI() );
		TreeSet<String> all = new TreeSet<>();
		try (Stream<Path> paths = Files.walk( root )){
			for (Path path : (Iterable<Path>) paths.filter( p -> p.toString().endsWith( ".class" ) )::iterator){
				String name = root.relativize( path ).toString()
						.replace( '/', '.' ).replace( '\\', '.' ).replace( ".class", "" );
				Class<?> cls;
				try {
					cls = Class.forName( name, false, getClass().getClassLoader() );
				} catch (Throwable t){
					continue;
				}
				if (!Mob.class.isAssignableFrom( cls ) || Modifier.isAbstract( cls.getModifiers() )
						|| !Modifier.isPublic( cls.getModifiers() ) || cls.isAnonymousClass() || cls.isLocalClass()
						|| cls.getCanonicalName() == null) continue;
				try { cls.getConstructor(); } catch (NoSuchMethodException e){ continue; }
				all.add( cls.getCanonicalName() );
			}
		}

		TreeSet<String> listed = new TreeSet<>();
		for (Class<?> c : MobRegistry.ALL) listed.add( c.getCanonicalName() );
		if (listed.equals( all )) return;

		TreeSet<String> missing = new TreeSet<>( all );
		missing.removeAll( listed );
		TreeSet<String> gone = new TreeSet<>( listed );
		gone.removeAll( all );
		StringBuilder out = new StringBuilder( "package xyz.gabriwar.warpedpixeldungeon.journal;\n\n"
				+ "// Generated: every public, constructible Mob class, for the Bestiary's catch-all.\n"
				+ "// Kept complete by MobRegistryCompleteTest, which rewrites this file when a mob is missing.\n"
				+ "final class MobRegistry {\n    static final Class<?>[] ALL = {\n" );
		for (String name : all) out.append( "        " ).append( name ).append( ".class,\n" );
		out.append( "    };\n    private MobRegistry() {}\n}\n" );
		Files.write( Paths.get( SOURCE ), out.toString().getBytes( "UTF-8" ) );
		fail( "MobRegistry.java was stale and has been regenerated (" + all.size() + " monsters); rebuild to pick it up."
				+ (missing.isEmpty() ? "" : "\nwas missing: " + missing)
				+ (gone.isEmpty() ? "" : "\nno longer monsters: " + gone) );
	}
}
