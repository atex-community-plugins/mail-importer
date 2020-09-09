package com.atex.plugins.mailimporter;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.polopoly.common.lang.StringUtil;

public abstract class StringUtils {

    private static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final Pattern EMAIL_HTML_PATTERN = Pattern.compile("(.*)<p>(\\s*)<\\/p>(.*)");

    public static String dateToUTCDateString(Date date) {
        ZonedDateTime a = date.toInstant().atZone(ZoneId.of("UTC"));
        return a.format(DateTimeFormatter.ISO_INSTANT);
    }

    public static String getNormalizedDateString(String dateString) {

        String ret = null;

        String[] datePatterns = new String[]{"yyyy:MM:dd HH:mm:ss", "yyyy:MM:dd HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm",
                "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm", "yyyy:MM:dd", "yyyy-MM-dd", "yyyy-MM", "yyyy", "yyyyMMdd"};

        SimpleDateFormat sdf = new SimpleDateFormat();
        SimpleDateFormat outputSdf = new SimpleDateFormat(ISO_DATE_FORMAT);
        for (int i = 0; i < datePatterns.length; i++) {
            try {
                sdf.applyPattern(datePatterns[i]);
                Date d = sdf.parse(dateString);
                ret = outputSdf.format(d);

                // if date in the future, take current
                if (d.after(new Date())) {
                    d = new Date();
                    ret = outputSdf.format(d);
                }

                break;
            } catch (Exception ex) {
                continue;
            }
        }
        return ret;

    }

    public static boolean isHtmlBody(String mailBody) {
        Matcher matcher = EMAIL_HTML_PATTERN.matcher(mailBody);
        return matcher.find();
    }

    public static Optional<String> getFirstNotEmpty(final String...values) {
        for (final String s : values) {
            if (StringUtil.notEmpty(s)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }
    
}

