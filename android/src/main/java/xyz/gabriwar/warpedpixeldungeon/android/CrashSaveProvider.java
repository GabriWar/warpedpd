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

package xyz.gabriwar.warpedpixeldungeon.android;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

import xyz.gabriwar.warpedpixeldungeon.debug.CrashSave;

/**
 * Hands the email app the crashed run's save (core CrashSave) to attach to the report: one file,
 * read only, to whoever the report screen grants it to. Nothing else of the game's is served.
 */
public class CrashSaveProvider extends ContentProvider {

	/** Where the crash handler writes the zip. */
	static File file( Context c ){
		return new File( new File( c.getCacheDir(), "crash" ), CrashSave.FILE );
	}

	static Uri uri( Context c ){
		return Uri.parse( "content://" + c.getPackageName() + ".crashsave/" + CrashSave.FILE );
	}

	@Override
	public boolean onCreate(){
		return true;
	}

	@Override
	public String getType( Uri uri ){
		return "application/zip";
	}

	@Override
	public ParcelFileDescriptor openFile( Uri uri, String mode ) throws FileNotFoundException {
		if (!"r".equals( mode )) throw new FileNotFoundException( "read only" );
		return ParcelFileDescriptor.open( file( getContext() ), ParcelFileDescriptor.MODE_READ_ONLY );
	}

	//the name and size an email app shows for the attachment
	@Override
	public Cursor query( Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder ){
		MatrixCursor c = new MatrixCursor( new String[]{ OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE } );
		c.addRow( new Object[]{ CrashSave.FILE, file( getContext() ).length() } );
		return c;
	}

	@Override
	public Uri insert( Uri uri, ContentValues values ){
		throw new UnsupportedOperationException();
	}

	@Override
	public int delete( Uri uri, String selection, String[] selectionArgs ){
		throw new UnsupportedOperationException();
	}

	@Override
	public int update( Uri uri, ContentValues values, String selection, String[] selectionArgs ){
		throw new UnsupportedOperationException();
	}
}
