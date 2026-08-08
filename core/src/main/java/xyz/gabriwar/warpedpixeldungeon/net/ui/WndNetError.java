package xyz.gabriwar.warpedpixeldungeon.net.ui;

import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

public class WndNetError extends Window {

	private static final int WIDTH = 140;
	private static final int MARGIN = 4;
	private static final int BTN_HEIGHT = 18;

	public WndNetError(String title, String message, Runnable onDismiss) {
		super();

		float pos = MARGIN;

		RenderedTextBlock titleBlock = PixelScene.renderTextBlock(title, 9);
		titleBlock.hardlight(0xFF4444);
		titleBlock.maxWidth(WIDTH - MARGIN * 2);
		titleBlock.setPos((WIDTH - titleBlock.width()) / 2, pos);
		add(titleBlock);
		pos = titleBlock.bottom() + MARGIN;

		RenderedTextBlock msgBlock = PixelScene.renderTextBlock(message, 6);
		msgBlock.maxWidth(WIDTH - MARGIN * 2);
		msgBlock.setPos(MARGIN, pos);
		add(msgBlock);
		pos = msgBlock.bottom() + MARGIN * 2;

		RedButton btnOk = new RedButton("OK") {
			@Override
			protected void onClick() {
				hide();
				if (onDismiss != null) {
					onDismiss.run();
				}
			}
		};
		btnOk.setRect(MARGIN, pos, WIDTH - MARGIN * 2, BTN_HEIGHT);
		add(btnOk);
		pos = btnOk.bottom() + MARGIN;

		resize(WIDTH, (int) pos);
	}
}
