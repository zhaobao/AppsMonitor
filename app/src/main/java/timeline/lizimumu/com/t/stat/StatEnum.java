package timeline.lizimumu.com.t.stat;

/**
 * Stat Enum
 * Created by zb on 24/12/2017.
 */

public enum StatEnum {

    KEY_SHARE("share"),
    KEY_TOP1("top1");

    private final String mKey;

    StatEnum(String key) {
        mKey = key;
    }

    private String key() {
        return mKey;
    }
}
