/*
 * Warped Pixel Dungeon — PortalGate sprite.
 *
 * Spritesheet: environment/portal_gate.png — 9 frames × 32×32.
 *   basic           [0]              – stone gate, dark center (LOCKED / DEAD)
 *   activation      [0,1,2,3]        – lighting up (one-shot)
 *   activatedLoop   [4,5,6,7,8]      – pulsing blue (ACTIVE)
 */

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate;
import com.watabou.noosa.TextureFilm;

public class PortalGateSprite extends MobSprite {

	private Animation basic;
	private Animation activation;
	private Animation activatedLoop;

	public PortalGateSprite() {
		super();

		texture(Assets.Environment.PORTAL_GATE);

		TextureFilm frames = new TextureFilm(texture, 16, 16);

		basic = new Animation(1, true);
		basic.frames(frames, 0);

		activation = new Animation(8, false);
		activation.frames(frames, 0, 1, 2, 3);

		activatedLoop = new Animation(8, true);
		activatedLoop.frames(frames, 4, 5, 6, 7, 8);

		// Required slots must be distinct instances — CharSprite.onComplete
		// compares by reference (anim == attack), and a looped idle aliased to
		// attack would fire onAttackComplete every cycle (NPE once ch detaches).
		idle = basic;
		run    = basic.clone();
		attack = basic.clone();
		die    = basic.clone();

		play(idle);
	}

	@Override
	public void link(Char ch) {
		super.link(ch);
		if (ch instanceof PortalGate) {
			syncToState(((PortalGate) ch).state);
		}
	}

	public void onStateChange(PortalGate.State prev, PortalGate.State next) {
		if (next == PortalGate.State.ACTIVE && prev == PortalGate.State.LOCKED) {
			play(activation);
			// queued loop kicks in via onComplete
		} else {
			syncToState(next);
		}
	}

	private void syncToState(PortalGate.State s) {
		switch (s) {
			case ACTIVE: play(activatedLoop); break;
			case LOCKED:
			case DEAD:
			default:     play(basic);         break;
		}
	}

	@Override
	public void onComplete(Animation anim) {
		super.onComplete(anim);
		if (anim == activation) {
			play(activatedLoop);
		}
	}
}
