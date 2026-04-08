package nz.co.jammehcow.jenkinsdiscord;

public enum StatusColor {
    /**
     * Green "you're sweet as" color.
     */
    GREEN(1681177),
    /**
     * Yellow "go, but I'm watching you" color.
     */
    YELLOW(16776970),
    /**
     * Red "something ain't right" color.
     */
    RED(11278871),
    /**
     * Grey. Just grey.
     */
    GREY(13487565);

    private final int code;

    StatusColor(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
