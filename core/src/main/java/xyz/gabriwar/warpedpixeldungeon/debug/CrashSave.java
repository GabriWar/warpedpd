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

package xyz.gabriwar.warpedpixeldungeon.debug;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import com.watabou.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * The run a crash happened in, sent with the report: the slot's folder (game.dat and every floor
 * of the run, as last saved) zipped beside it. Loaded back on the desktop
 * (./gradlew :desktop:debug -Psave=crash-save.zip) into a free slot and continued, it is the
 * player's run at its last save, to play up to the crash.
 */
public final class CrashSave {

	public static final String FILE = "crash-save.zip";

	private CrashSave(){}

	/** The current run's saves zipped into `out`; false if there is no run on disk to send. Called
	 *  as the game dies: nothing here may throw. */
	public static boolean zip( File out ){
		try {
			if (Dungeon.hero == null || GamesInProgress.curSlot <= 0) return false;
			File dir = FileUtils.getFileHandle( GamesInProgress.gameFolder( GamesInProgress.curSlot ) ).file();
			File[] files = dir.listFiles();
			if (files == null || files.length == 0) return false;
			if (out.getParentFile() != null) out.getParentFile().mkdirs();
			try (ZipOutputStream zip = new ZipOutputStream( new FileOutputStream( out ) )) {
				for (File f : files){
					if (!f.isFile()) continue;
					zip.putNextEntry( new ZipEntry( f.getName() ) );
					try (InputStream in = new FileInputStream( f )) {
						copy( in, zip );
					}
					zip.closeEntry();
				}
			}
			return true;
		} catch (Throwable t) {
			out.delete();
			return false;
		}
	}

	/** A sent run unpacked into the first empty slot: its number. A player's zip is not trusted:
	 *  only plain file names come out of it, into that slot's folder and nowhere else. */
	public static int unpack( File zip ) throws IOException {
		int slot = freeSlot();
		if (slot == -1) throw new IOException( "no empty slot to load the save into: free one first" );
		File dir = FileUtils.getFileHandle( GamesInProgress.gameFolder( slot ) ).file();
		dir.mkdirs();
		int n = 0;
		try (ZipInputStream in = new ZipInputStream( new FileInputStream( zip ) )) {
			for (ZipEntry e; (e = in.getNextEntry()) != null; ){
				String name = e.getName();
				if (e.isDirectory() || !name.matches( "[A-Za-z0-9_.-]+" ) || name.startsWith( "." )) continue;
				try (OutputStream out = new FileOutputStream( new File( dir, name ) )) {
					copy( in, out );
				}
				n++;
			}
		}
		if (n == 0) throw new IOException( "no save files in " + zip );
		GamesInProgress.setUnknown( slot );
		return slot;
	}

	//the first slot with nothing on disk at all: GamesInProgress.firstEmpty would also hand out a
	//slot whose game.dat does not read, and loading over it would lose that save for good
	private static int freeSlot(){
		for (int s = 1; s <= GamesInProgress.MAX_SLOTS; s++){
			String[] in = FileUtils.getFileHandle( GamesInProgress.gameFolder( s ) ).file().list();
			if (in == null || in.length == 0) return s;
		}
		return -1;
	}

	private static void copy( InputStream in, OutputStream out ) throws IOException {
		byte[] buf = new byte[8192];
		for (int r; (r = in.read( buf )) > 0; ) out.write( buf, 0, r );
	}

	//the save the command line asked for (desktop -Psave=): loaded and continued from the title
	private static String requested;

	public static void request( String path ){
		requested = path;
	}

	/** TitleScene: a requested save is loaded into a free slot and continued at once. */
	public static boolean startRequested(){
		if (requested == null) return false;
		String path = requested;
		requested = null;
		int slot;
		try {
			slot = unpack( new File( path ) );
		} catch (IOException e) {
			System.out.println( "[save] could not load " + path + ": " + e.getMessage() );
			return false;
		}
		System.out.println( "[save] " + path + " loaded into slot " + slot );
		GamesInProgress.curSlot = slot;
		Dungeon.hero = null;
		Dungeon.daily = Dungeon.dailyReplay = false;
		InterlevelScene.mode = InterlevelScene.Mode.CONTINUE;
		WarpedPixelDungeon.switchScene( InterlevelScene.class );
		return true;
	}
}
