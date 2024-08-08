package context;

import java.text.MessageFormat;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private Utils() {
    }

    static Random randomGenerator = new Random();
    public static String applySafeRunning(String code) {
        String randomVariable = randomGenerator.nextInt(1000000) + "";
        return MessageFormat.format("let _{0}=this;{1}",
                randomVariable,
                applySafeRunning(code, "_" + randomVariable + ".setError(e, false)")
        );
    }
    public static String applySafeRunning(String code, String runInError) {
        String resultCode = code;
        Pattern pat1 = Pattern.compile("function\\s*\\([a-zA-Z0-9_.,$\\s]*\\)\\s*\\{", Pattern.DOTALL);
        Matcher mat1 = pat1.matcher(code);
        while(mat1.find()) {
            int endIndex = mat1.end();

            resultCode = resultCode.substring(0, endIndex) + "try{" + resultCode.substring(endIndex);

            int deep = 1;
            int lastIndex = endIndex;
            while(deep > 0) {
                int open = code.indexOf('{', lastIndex);
                int close = code.indexOf('}', lastIndex);
                if(close == -1) return code;
                if(open == -1 || close < open) {
                    deep--;
                    lastIndex = close + 1;
                } else {
                    deep++;
                    lastIndex = open + 1;
                }
            }
            lastIndex += 3;
            resultCode = resultCode.substring(0, lastIndex) + "}catch(e){"+runInError+"}" + resultCode.substring(lastIndex);
        }
        return resultCode;
    }

    public static byte compactifyBoolean(boolean ...values) {
        byte result = 0;
        for (int i = 0; i < values.length; i++) {
            if(values[i]) result |= 1 << i;
        }
        return result;
    }
}
