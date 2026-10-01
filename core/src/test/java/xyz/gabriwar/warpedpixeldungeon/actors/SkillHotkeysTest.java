package xyz.gabriwar.warpedpixeldungeon.actors;

import com.badlogic.gdx.Input;
import com.watabou.input.GameAction;
import com.watabou.input.InputHandler;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.utils.Signal;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.WPDAction;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import static org.junit.Assert.*;

public class SkillHotkeysTest {
	@Test public void shiftedNumbersUseSkillSlotsAndReleaseTheSameAction() {
		LinkedHashMap<Integer, GameAction> previous = KeyBindings.getAllBindings();
		Input input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[]{Input.class},
				(proxy, method, args) -> method.getReturnType() == boolean.class ? false : null);
		InputHandler handler = new InputHandler(input);
		ArrayList<GameAction> actions = new ArrayList<>();
		ArrayList<Boolean> presses = new ArrayList<>();
		Signal.Listener<KeyEvent> listener = event -> {
			actions.add(KeyBindings.getActionForKey(event));
			presses.add(event.pressed);
			return true;
		};
		KeyBindings.setAllBindings(WPDAction.getDefaults());
		KeyEvent.addKeyListener(listener);
		try {
			GameAction[] expected = {WPDAction.SKILL_SLOT_1, WPDAction.SKILL_SLOT_2, WPDAction.SKILL_SLOT_3,
					WPDAction.SKILL_SLOT_4, WPDAction.SKILL_SLOT_5, WPDAction.SKILL_SLOT_6};
			for (int shift : new int[]{Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT}) {
				for (int i = 0; i < expected.length; i++) {
					actions.clear(); presses.clear();
					handler.keyDown(shift);
					handler.keyDown(Input.Keys.NUM_1 + i);
					handler.keyUp(shift); // release Shift before the number
					handler.keyUp(Input.Keys.NUM_1 + i);
					KeyEvent.processKeyEvents();
					assertEquals(2, actions.size());
					assertSame(expected[i], actions.get(0));
					assertSame(expected[i], actions.get(1));
					assertTrue(presses.get(0));
					assertFalse(presses.get(1));
				}
			}
			actions.clear();
			handler.keyDown(Input.Keys.NUM_1);
			handler.keyUp(Input.Keys.NUM_1);
			KeyEvent.processKeyEvents();
			assertSame(WPDAction.QUICKSLOT_1, actions.get(0));
			assertSame(WPDAction.QUICKSLOT_1, actions.get(1));
			assertEquals("Shift+1", KeyBindings.getKeyName(Input.Keys.NUM_1 | KeyBindings.SHIFT_MODIFIER));
		} finally {
			KeyEvent.removeKeyListener(listener);
			KeyBindings.setAllBindings(previous);
		}
	}
}
