package ru.iamdvz.divizionsc.input;

import org.bukkit.Input;

public record InputSnapshot(
        boolean forward,
        boolean backward,
        boolean left,
        boolean right,
        boolean jump,
        boolean sneak,
        boolean sprint
) {

    public static InputSnapshot from(Input input) {
        return new InputSnapshot(
                input.isForward(),
                input.isBackward(),
                input.isLeft(),
                input.isRight(),
                input.isJump(),
                input.isSneak(),
                input.isSprint()
        );
    }

    public static InputSnapshot empty() {
        return new InputSnapshot(false, false, false, false, false, false, false);
    }
}
