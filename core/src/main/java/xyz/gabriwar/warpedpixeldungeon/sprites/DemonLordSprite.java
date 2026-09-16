
package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;

import java.util.ArrayList;
import java.util.Collection;

//the frozen branch boss, redrawn: frost-skinned, bone-horned, cape and black iron
public class DemonLordSprite extends MobSprite {

	private Animation inhale;
	private Animation breath;

	private final ArrayList<Emitter> warnings = new ArrayList<>();

	public DemonLordSprite() {
		super();

		texture( Assets.Sprites.ICE_DEMON_LORD );

		TextureFilm frames = new TextureFilm( texture, 20, 20 );

		idle = new Animation( 3, true );
		idle.frames( frames, 0, 0, 0, 1 );

		run = new Animation( 10, true );
		run.frames( frames, 2, 3, 4, 5 );

		attack = new Animation( 14, false );
		attack.frames( frames, 6, 7, 8 );

		inhale = new Animation( 6, true );
		inhale.frames( frames, 9, 10 );

		breath = new Animation( 12, false );
		breath.frames( frames, 11, 11, 11, 11 );

		die = new Animation( 8, false );
		die.frames( frames, 12, 13, 14 );

		play( idle );
	}

	//frost gathers over every cell the coming breath will cover, and each one is
	//marked on the ground until the breath lands
	public void inhale( Collection<Integer> cells, int turns ) {
		play( inhale );
		for (int cell : cells) {
			GameScene.targetedCell( cell, turns );
			Emitter e = CellEmitter.get( cell );
			e.pour( SnowParticle.FACTORY, 0.06f );
			warnings.add( e );
		}
		Sample.INSTANCE.play( Assets.Sounds.CHARGEUP, 1f, 0.8f );
	}

	//each turn of the charge: keep the markers up and thicken the falling frost
	public void holdBreath( Collection<Integer> cells, int turns ) {
		if (curAnim != inhale) play( inhale );
		for (int cell : cells) {
			GameScene.targetedCell( cell, turns );
			CellEmitter.get( cell ).burst( SnowParticle.FACTORY, 2 );
		}
		Sample.INSTANCE.play( Assets.Sounds.CHARGEUP, 0.8f, 0.8f + 0.15f * (3 - turns) );
	}

	//the release: every marked tile erupts in a blast of ice shards, snow and light
	public void breathe( Collection<Integer> cells ) {
		play( breath );
		for (int cell : cells) {
			CellEmitter.center( cell ).burst( SnowParticle.FACTORY, 24 );
			CellEmitter.get( cell ).burst( Speck.factory( Speck.LIGHT ), 4 );
			CellEmitter.center( cell ).burst( Speck.factory( Speck.ROCK ), 3 );
			Splash.at( cell, 0xCFF2FF, 10 );
		}
	}

	private void clearWarnings() {
		for (Emitter e : warnings) {
			e.on = false;
		}
		warnings.clear();
	}

	@Override
	public void play( Animation anim ) {
		if (anim != inhale) {
			clearWarnings();
		}
		super.play( anim );
	}

	@Override
	public void onComplete( Animation anim ) {
		super.onComplete( anim );
		if (anim == breath) {
			idle();
		}
	}

	@Override
	public int blood() {
		return 0xFF8FD8FF;
	}
}
