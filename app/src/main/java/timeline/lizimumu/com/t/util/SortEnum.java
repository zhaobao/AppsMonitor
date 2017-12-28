package timeline.lizimumu.com.t.util;

/**
 * Sort Enum
 * Created by zb on 28/12/2017.
 */

public enum SortEnum {
    TODAY(0), THIS_WEEK(1), THIS_MONTH(2), THIS_YEAR(3);

    int sort;

    SortEnum(int sort) {
        this.sort = sort;
    }

    public static SortEnum getSortEnum(int sort) {
        switch (sort) {
            case 0:
                return SortEnum.TODAY;
            case 1:
                return SortEnum.THIS_WEEK;
            case 2:
                return SortEnum.THIS_MONTH;
            case 3:
                return SortEnum.THIS_YEAR;
        }
        return SortEnum.TODAY;
    }

    public int getValue() {
        return this.sort;
    }
}
