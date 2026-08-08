package xyz.gabriwar.warpedpixeldungeon.net;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import com.watabou.noosa.Image;

/**
 * Lightweight buff for spectator display. Stores only the visual data
 * needed by BuffIndicator (icon, type, fade, text). No gameplay logic.
 */
public class SpectatorBuff extends Buff {

	private int iconId;
	private float fade;
	private String text;

	public SpectatorBuff(int icon, int typeOrdinal, float fade, String text) {
		this.iconId = icon;
		buffType[] all = buffType.values();
		this.type = (typeOrdinal >= 0 && typeOrdinal < all.length) ? all[typeOrdinal] : buffType.NEUTRAL;
		this.fade = fade;
		this.text = text;
	}

	/** Update visual data in-place without detach/reattach */
	public void updateVisuals(int icon, int typeOrdinal, float fade, String text) {
		this.iconId = icon;
		buffType[] all = buffType.values();
		this.type = (typeOrdinal >= 0 && typeOrdinal < all.length) ? all[typeOrdinal] : buffType.NEUTRAL;
		this.fade = fade;
		this.text = text;
	}

	public int getIconId() { return iconId; }

	@Override
	public int icon() { return iconId; }

	@Override
	public float iconFadePercent() { return fade; }

	@Override
	public String iconTextDisplay() { return text; }

	@Override
	public void tintIcon(Image icon) {}

	@Override
	public boolean act() { return true; }

	@Override
	public void fx(boolean on) {}
}
