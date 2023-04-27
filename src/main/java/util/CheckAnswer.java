package util;

public class CheckAnswer {
    public static double CheckAnswer(String msg, String answer){
        msg = msg.toLowerCase();
        answer = answer.toLowerCase();

        if(Similarity(msg, answer) >= 0.8d) return Similarity(msg, answer);

        String tmpTitle = answer;
        tmpTitle = tmpTitle.replaceAll("\\[.*?\\]","");
        tmpTitle = tmpTitle.replaceAll("\\-.*?\\-","");
        tmpTitle = tmpTitle.replaceAll("\\(.*?\\)","");
        tmpTitle = tmpTitle.replaceAll("\\<.*?\\>","");
        tmpTitle = tmpTitle.replaceAll("\\~.*?\\~","");

        tmpTitle = tmpTitle.split("feat.")[0];
        tmpTitle = tmpTitle.split("ft.")[0];
        tmpTitle = tmpTitle.split("ft ")[0];

        tmpTitle = tmpTitle.trim();


        return Similarity(msg, tmpTitle);
    }

    static private double Similarity(String s1, String s2) {
        String longer = s1, shorter = s2;

        if (s1.length() < s2.length()) {
            longer = s2;
            shorter = s1;
        }

        int longerLength = longer.length();
        if (longerLength == 0) return 1.0;
        return (longerLength - editDistance(longer, shorter)) / (double) longerLength;
    }
    static private int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        int[] costs = new int[s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else {
                    if (j > 0) {
                        int newValue = costs[j - 1];

                        if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                            newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                        }

                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }

            if (i > 0) costs[s2.length()] = lastValue;
        }

        return costs[s2.length()];
    }
}
