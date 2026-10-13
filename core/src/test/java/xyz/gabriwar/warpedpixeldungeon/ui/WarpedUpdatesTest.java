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

package xyz.gabriwar.warpedpixeldungeon.ui;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxNativesLoader;
import xyz.gabriwar.warpedpixeldungeon.ui.changelist.WarpedUpdates;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** The updates list the Changes screen and the patched screen share. */
public class WarpedUpdatesTest {

	//the entries' icons name item sprites, and the item sheet reads its texture when first used
	private static final class AssetFiles implements com.badlogic.gdx.Files {
		private final File root = new File( "src/main/assets" );
		private FileHandle at( String path ){ return new FileHandle( new File( root, path ) ); }
		@Override public FileHandle getFileHandle( String path, FileType type ){ return at( path ); }
		@Override public FileHandle classpath( String path ){ return at( path ); }
		@Override public FileHandle internal( String path ){ return at( path ); }
		@Override public FileHandle external( String path ){ return at( path ); }
		@Override public FileHandle absolute( String path ){ return new FileHandle( new File( path ) ); }
		@Override public FileHandle local( String path ){ return at( path ); }
		@Override public String getExternalStoragePath(){ return root.getAbsolutePath(); }
		@Override public boolean isExternalStorageAvailable(){ return false; }
		@Override public String getLocalStoragePath(){ return root.getAbsolutePath(); }
		@Override public boolean isLocalStorageAvailable(){ return false; }
	}

	@BeforeClass
	public static void assets(){
		GdxNativesLoader.load();
		Gdx.files = new AssetFiles();
	}

	@Test
	public void newestFirstWithTextAndNeverTheDebugMenu(){
		WarpedUpdates.Entry[] all = WarpedUpdates.ALL;
		assertTrue( all.length > 0 );
		for (int i = 0; i < all.length; i++){
			WarpedUpdates.Entry e = all[i];
			if (i > 0) assertTrue( e.title + ": newest first", e.since <= all[i - 1].since );
			assertNotNull( e.icon );
			assertFalse( e.title.trim().isEmpty() || e.summary.trim().isEmpty() || e.body.trim().isEmpty() );
			for (String text : new String[]{ e.title, e.summary, e.body }){
				assertFalse( e.title + " mentions debugging", text.toLowerCase().contains( "debug" ) );
			}
		}
	}

	@Test
	public void theNewSinceAVersionAreThoseAfterIt(){
		int newest = WarpedUpdates.ALL[0].since;
		assertTrue( WarpedUpdates.since( newest ).isEmpty() );
		assertFalse( WarpedUpdates.since( newest - 1 ).isEmpty() );
		int n = 0;
		for (WarpedUpdates.Entry e : WarpedUpdates.ALL) if (e.since > 10562) n++;
		assertEquals( n, WarpedUpdates.since( 10562 ).size() );
	}

	@Test
	public void versionsGoUnderTheirMajorNewestFirst(){
		//as build.gradle names them: the code less 8787, as a.b.c
		assertEquals( "18.4.6", WarpedUpdates.versionName( 10633 ) );
		assertEquals( "17.7.6", WarpedUpdates.versionName( 10563 ) );
		assertEquals( 18, WarpedUpdates.major( 10633 ) );
		assertEquals( 17, WarpedUpdates.major( 10586 ) );

		java.util.ArrayList<Integer> majors = WarpedUpdates.majors();
		assertEquals( WarpedUpdates.major( WarpedUpdates.ALL[0].since ), (int) majors.get( 0 ) );
		for (int i = 1; i < majors.size(); i++) assertTrue( majors.get( i ) < majors.get( i - 1 ) );
		for (WarpedUpdates.Entry e : WarpedUpdates.ALL) assertTrue( e.title, majors.contains( WarpedUpdates.major( e.since ) ) );
	}
}
