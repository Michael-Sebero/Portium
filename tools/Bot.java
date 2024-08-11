import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/** Tiny input helper: Bot click X Y | lclick | rclick | dclick X Y | move X Y | key NAME | type TEXT | wait MS, several per call. */
public class Bot {
    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        robot.setAutoDelay(40);
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "click": robot.mouseMove(Integer.parseInt(args[++i]), Integer.parseInt(args[++i])); click(robot); break;
                case "dclick": robot.mouseMove(Integer.parseInt(args[++i]), Integer.parseInt(args[++i])); click(robot); click(robot); break;
                case "rclick": robot.mousePress(InputEvent.BUTTON3_DOWN_MASK); robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK); break;
                case "lclick": click(robot); break;
                case "move": robot.mouseMove(Integer.parseInt(args[++i]), Integer.parseInt(args[++i])); break;
                case "key": press(robot, KeyEvent.class.getField("VK_" + args[++i]).getInt(null)); break;
                case "type": for (char c : args[++i].toCharArray()) type(robot, c); break;
                case "wait": Thread.sleep(Long.parseLong(args[++i])); break;
                default: throw new IllegalArgumentException(args[i]);
            }
        }
    }

    static void click(Robot robot) {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    static void press(Robot robot, int code) {
        robot.keyPress(code);
        robot.keyRelease(code);
    }

    /** Types printable ASCII on a US layout. */
    static void type(Robot robot, char c) {
        String shifted = "~!@#$%^&*()_+{}|:\"<>?";
        String plain = "`1234567890-=[]\\;',./";
        boolean shift = Character.isUpperCase(c) || shifted.indexOf(c) >= 0;
        char base = shift && shifted.indexOf(c) >= 0 ? plain.charAt(shifted.indexOf(c)) : Character.toUpperCase(c);
        int code;
        switch (base) {
            case ' ': code = KeyEvent.VK_SPACE; break;
            case '`': code = KeyEvent.VK_BACK_QUOTE; break;
            case '-': code = KeyEvent.VK_MINUS; break;
            case '=': code = KeyEvent.VK_EQUALS; break;
            case '[': code = KeyEvent.VK_OPEN_BRACKET; break;
            case ']': code = KeyEvent.VK_CLOSE_BRACKET; break;
            case '\\': code = KeyEvent.VK_BACK_SLASH; break;
            case ';': code = KeyEvent.VK_SEMICOLON; break;
            case '\'': code = KeyEvent.VK_QUOTE; break;
            case ',': code = KeyEvent.VK_COMMA; break;
            case '.': code = KeyEvent.VK_PERIOD; break;
            case '/': code = KeyEvent.VK_SLASH; break;
            default: code = base;
        }
        if (shift) robot.keyPress(KeyEvent.VK_SHIFT);
        press(robot, code);
        if (shift) robot.keyRelease(KeyEvent.VK_SHIFT);
    }
}
