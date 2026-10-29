package dev.blockanim;

public enum AnimMode {
    VANILLA("1.8"),
    OLD("1.7"),
    EXHIBITION("Exhibition"),
    SWANK("Swank"),
    SWING("Swing"),
    SIGMA("Sigma"),
    PUSH("Push"),
    SLIDE("Slide"),
    TAP("Tap"),
    AVATAR("Avatar"),
    SPIN("Spin");

    public final String displayName;

    AnimMode(String displayName) {
        this.displayName = displayName;
    }

    public AnimMode next() {
        AnimMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public AnimMode prev() {
        AnimMode[] all = values();
        return all[(ordinal() - 1 + all.length) % all.length];
    }

    public static AnimMode byName(String name) {
        for (AnimMode m : values()) {
            if (m.name().equalsIgnoreCase(name) || m.displayName.equalsIgnoreCase(name)) {
                return m;
            }
        }
        return EXHIBITION;
    }
}
