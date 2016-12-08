package els_toolkit.population;

import org.apache.mahout.math.jet.random.Uniform;

public final class FisherYatesShuffle {
    public static int[] makeRandoms(Uniform distribution, int inputLength) {
        synchronized (distribution) {
            int[] randoms = new int[inputLength]; // randoms is one-based
            for (int i = inputLength - 1; i > 0; i--)
                randoms[i] = distribution.nextIntFromTo(0, i);
            return randoms;
        }
    }

    /**
     * Based on http://rosettacode.org/wiki/Knuth_shuffle#Clojure
     * and http://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle#The_modern_algorithm
     */
    public static String shuffle(String input, int[] randoms) {
        char[] chars = input.toCharArray();
        for (int i = input.length() - 1; i > 0; i--) {
            int j = randoms[i];
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}
