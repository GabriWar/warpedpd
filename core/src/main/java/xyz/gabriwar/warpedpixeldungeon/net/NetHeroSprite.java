package xyz.gabriwar.warpedpixeldungeon.net;

import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;

/**
 * Sprite for a remote player's hero rendered on the client side.
 *
 * Why not extend HeroSprite? HeroSprite.update() casts `ch` to Hero
 * (`((Hero)ch).resting`) and crashes because our SpectatorMob is a Mob.
 * super.super isn't expressible in Java, so we extend CharSprite and
 * replicate the bits of HeroSprite we actually need: the spritesheet
 * texture, the tier-based animation frames, and CharSprite's normal update.
 */
public class NetHeroSprite extends CharSprite {

	private static final int FRAME_WIDTH  = 12;
	private static final int FRAME_HEIGHT = 15;
	private static final int RUN_FRAMERATE = 20;

	public NetHeroSprite() {
		super();
	}

	@Override
	public void link(Char ch) {
		super.link(ch);
		if (ch instanceof SpectatorReceiver.NetHeroMob) {
			SpectatorReceiver.NetHeroMob nhm = (SpectatorReceiver.NetHeroMob) ch;
			texture(nhm.heroClass.spritesheet());
			setupAnimations(nhm.tier);
			idle();
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] NetHeroSprite linked id=" + nhm.hostId
					+ " cls=" + nhm.heroClass.title() + " tier=" + nhm.tier
					+ " pos=" + nhm.pos);
		} else {
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] NetHeroSprite linked to non-NetHeroMob ch="
					+ (ch == null ? "null" : ch.getClass().getSimpleName()));
		}
	}

	private void setupAnimations(int tier) {
		TextureFilm film = new TextureFilm(HeroSprite.tiers(), tier, FRAME_WIDTH, FRAME_HEIGHT);

		idle = new MovieClip.Animation(1, true);
		idle.frames(film, 0, 0, 0, 1, 0, 0, 1, 1);

		run = new MovieClip.Animation(RUN_FRAMERATE, true);
		run.frames(film, 2, 3, 4, 5, 6, 7);

		die = new MovieClip.Animation(20, false);
		die.frames(film, 8, 9, 10, 11, 12, 11);

		attack = new MovieClip.Animation(15, false);
		attack.frames(film, 13, 14, 15, 0);

		zap = attack.clone();

		operate = new MovieClip.Animation(8, false);
		operate.frames(film, 16, 17, 16, 17);
	}

	private String animName(MovieClip.Animation a) {
		if (a == null) return "null";
		if (a == idle) return "idle";
		if (a == run) return "run";
		if (a == die) return "die";
		if (a == attack) return "attack";
		if (a == zap) return "zap";
		if (a == operate) return "operate";
		return "?";
	}

	private int hostId() {
		return (ch instanceof SpectatorReceiver.NetHeroMob)
				? ((SpectatorReceiver.NetHeroMob) ch).hostId : -1;
	}

	@Override
	public void idle() {
		NetManager.log("[NET-CLI] NetHeroSprite.idle id=" + hostId()
				+ " from=" + animName(curAnim) + " pos=" + (ch == null ? -1 : ch.pos));
		super.idle();
	}

	@Override
	public void move(int from, int to) {
		NetManager.log("[NET-CLI] NetHeroSprite.move id=" + hostId()
				+ " from=" + from + " to=" + to + " curAnim=" + animName(curAnim));
		super.move(from, to);
	}

	@Override
	public void play(MovieClip.Animation anim, boolean force) {
		MovieClip.Animation prev = curAnim;
		super.play(anim, force);
		if (prev != anim) {
			NetManager.log("[NET-CLI] NetHeroSprite.play id=" + hostId()
					+ " " + animName(prev) + " -> " + animName(anim));
		}
	}
}
