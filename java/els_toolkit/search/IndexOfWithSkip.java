package els_toolkit.search;

import java.lang.reflect.Field;

public final class IndexOfWithSkip {
    static final Field valueField;

    static {
        try {
            valueField = String.class.getDeclaredField("value");
            valueField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static int indexOf(String sourceStr, String targetStr,
            int fromIndex, int skip) throws IllegalAccessException {
        char[] source = (char[])valueField.get(sourceStr);
        char[] target = (char[])valueField.get(targetStr);
        return indexOf(source, source.length, target,
            target.length, fromIndex, skip);
    }

    /** Copied from java.lang.String with the skip argument added. */
    static int indexOf(char[] source, int sourceCount, char[] target,
            int targetCount, int fromIndex, int skip) {
        if (fromIndex >= sourceCount) {
            return (targetCount == 0 ? sourceCount : -1);
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (targetCount == 0) {
            return fromIndex;
        }
        if (skip < 1) {
            skip = 1;
        }

        char first = target[0];
        int max = sourceCount - ((targetCount - 1) * skip) - 1;

        for (int i = fromIndex; i <= max; i = i + skip) {
            /* Look for first character. */
            if (source[i] != first) {
                while ((i = i + skip) <= max && source[i] != first);
            }

            /* Found first character, now look at the rest of v2 */
            if (i <= max) {
                int j = i + skip;
                int end = j + ((targetCount - 1) * skip);
                for (int k = 1; j < end && source[j]
                        == target[k]; j = j + skip, k++);

                if (j == end) {
                    /* Found whole string. */
                    return i;
                }
            }
        }
        return -1;
    }
}
