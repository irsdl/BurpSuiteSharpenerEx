// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.generic;

import java.io.InputStream;
import java.util.Properties;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

public class PropertiesHelper {
    public static Properties readProperties(Class<?> claz, String resourcePath) {
        Properties prop = new Properties();
        try {
            InputStream stream = claz.getResourceAsStream(resourcePath);
            prop.load(stream);
        } catch (Exception e) {
            System.err.println(e.getMessage() + "\r\n" + getStackTrace(e));
        }
        return prop;
    }
}
