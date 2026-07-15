// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.generic;

import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Utilities {
    public static int getInsecureRandomNumber(int min, int max) {
        //return new Random().nextInt(min, max+1);
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
    }

    public static boolean isValidRegExPattern(String regexString) {
        boolean result = false;
        try {
            Pattern.compile(regexString);
            result = true;
        } catch (PatternSyntaxException exception) {
            //ignore
        }
        return result;
    }
}
