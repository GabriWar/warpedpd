/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package com.watabou.noosa.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.watabou.noosa.Game;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public enum Sample {

	INSTANCE;

	protected HashMap<Object, Sound> ids = new HashMap<>();

	private boolean enabled = true;
	private float globalVolume = 1f;

	//the ambience channel: a place's background sounds (water, small life, its air), with a
	//switch and a volume of their own, apart from the game's effects
	private boolean ambientEnabled = true;
	private float ambientVolume = 1f;

	public synchronized void reset() {

		for (Sound sound : ids.values()){
			sound.dispose();
		}
		
		ids.clear();
		delayedSFX.clear();

	}

	public synchronized void pause() {
		for (Sound sound : ids.values()) {
			sound.pause();
		}
	}

	public synchronized void resume() {
		for (Sound sound : ids.values()) {
			sound.resume();
		}
	}

	public synchronized void load( final String asset){
		if (asset != null) {
			try {
				Sound newSound = Gdx.audio.newSound(Gdx.files.internal(asset));
				ids.put(asset, newSound);
			} catch (Exception e){
				Game.reportException(e);
			}
		}
	}

	private static final LinkedList<String> loadingQueue = new LinkedList<>();

	//queues multiple assets for loading, which happens in update()
	// this prevents blocking while we load many assets
	public void load( final String[] assets ) {
		synchronized (loadingQueue) {
			for (String asset : assets) {
				if (!ids.containsKey(asset) && !loadingQueue.contains(asset)) {
					loadingQueue.add(asset);
				}
			}
		}
	}

	public void unload( Object src ) {
		//one still waiting in the queue is dropped from it, or it would load after all
		synchronized (loadingQueue) {
			loadingQueue.remove( src );
		}
		synchronized (this) {
			if (ids.containsKey( src )) {
				ids.get( src ).dispose();
				ids.remove( src );
			}
		}
	}

	/** Has this sound been loaded? On Android it may still be decoding for a moment after. */
	public synchronized boolean isLoaded( Object id ) {
		return ids.containsKey( id );
	}

	public long play( Object id ) {
		return play( id, 1 );
	}

	public long play( Object id, float volume ) {
		return play( id, volume, volume, 1 );
	}
	
	public long play( Object id, float volume, float pitch ) {
		return play( id, volume, volume, pitch );
	}
	
	public synchronized long play( Object id, float leftVolume, float rightVolume, float pitch ) {
		float volume = Math.max(leftVolume, rightVolume);
		float pan = rightVolume - leftVolume;
		if (enabled && ids.containsKey( id )) {
			return ids.get(id).play( globalVolume*volume, pitch, pan );
		} else {
			return -1;
		}
	}

	private class DelayedSoundEffect{
		Object id;
		float delay;

		float leftVol;
		float rightVol;
		float pitch;
	}

	private static final HashSet<DelayedSoundEffect> delayedSFX = new HashSet<>();

	public void playDelayed( Object id, float delay ){
		playDelayed( id, delay, 1 );
	}

	public void playDelayed( Object id, float delay, float volume ) {
		playDelayed( id, delay, volume, volume, 1 );
	}

	public void playDelayed( Object id, float delay, float volume, float pitch ) {
		playDelayed( id, delay, volume, volume, pitch );
	}

	public void playDelayed( Object id, float delay, float leftVolume, float rightVolume, float pitch ) {
		if (delay <= 0) {
			play(id, leftVolume, rightVolume, pitch);
			return;
		}
		DelayedSoundEffect sfx = new DelayedSoundEffect();
		sfx.id = id;
		sfx.delay = delay;
		sfx.leftVol = leftVolume;
		sfx.rightVol = rightVolume;
		sfx.pitch = pitch;
		synchronized (delayedSFX) {
			delayedSFX.add(sfx);
		}
	}

	public void update(){
		synchronized (loadingQueue) {
			if (!loadingQueue.isEmpty()) {
				load(loadingQueue.poll());
			}
		}

		synchronized (delayedSFX) {
			if (delayedSFX.isEmpty()) return;
			for (DelayedSoundEffect sfx : delayedSFX.toArray(new DelayedSoundEffect[0])) {
				sfx.delay -= Game.elapsed;
				if (sfx.delay <= 0) {
					delayedSFX.remove(sfx);
					play(sfx.id, sfx.leftVol, sfx.rightVol, sfx.pitch);
				}
			}
		}
	}

	/**
	 * Plays a sound on the ambience channel: scaled by the ambience volume instead of the effects'
	 * one, silent while ambience is off whatever the effects do. Pan runs -1 (left) to 1 (right);
	 * pitch is held to 0.5-2, the range every platform plays (Android's SoundPool clamps there).
	 */
	public synchronized long playAmbient( Object id, float volume, float pitch, float pan ) {
		if (ambientEnabled && ambientVolume > 0 && volume > 0 && ids.containsKey( id )) {
			return ids.get( id ).play( ambientVolume * volume,
					Math.max( 0.5f, Math.min( 2f, pitch ) ),
					Math.max( -1f, Math.min( 1f, pan ) ) );
		} else {
			return -1;
		}
	}

	public void ambientEnable( boolean value ) {
		ambientEnabled = value;
	}

	public void ambientVolume( float value ) {
		ambientVolume = value;
	}

	/** Is anything on the ambience channel audible at all (on, and not at zero)? */
	public boolean ambientAudible() {
		return ambientEnabled && ambientVolume > 0;
	}

	public void enable( boolean value ) {
		enabled = value;
	}

	public void volume( float value ) {
		globalVolume = value;
	}

	public boolean isEnabled() {
		return enabled;
	}
	
}