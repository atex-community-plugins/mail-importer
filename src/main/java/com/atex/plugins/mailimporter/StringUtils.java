package com.atex.plugins.mailimporter;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.polopoly.util.StringUtil;

public abstract class StringUtils {

    private static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final List<String> EMAIL_HTML_TAGS = Arrays.asList(
            "p",
            "br",
            "div",
            "b",
            "em",
            "i",
            "strong",
            "u"
    );
    public static final Pattern EMAIL_HTML_PATTERN = Pattern.compile(
            "(.*)(" +
                    EMAIL_HTML_TAGS.stream()
                                   .map(s -> "<" + s + "\\s*?>")
                                   .collect(Collectors.joining("|"))
                    + ")(.*)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

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

    public static boolean notEmpty(final String value) {
        return !StringUtil.isEmpty(value);
    }

    public static boolean isHtmlBody(String mailBody) {
        Matcher matcher = EMAIL_HTML_PATTERN.matcher(trim(mailBody));
        return matcher.find();
    }

    public static String trimLines(final String body) {
        final String[] lines = org.apache.commons.lang.StringUtils.split(body, '\n');
        if (lines != null && lines.length > 0) {
            final StringBuilder sb = new StringBuilder();
            for (int idx = 0; idx < lines.length; idx++) {
                sb.append(lines[idx].trim());
                if (idx < (lines.length - 1)) {
                    sb.append("\n");
                }
            }
            return sb.toString();
        }
        return body.trim();
    }

    public static String linesToParagraphs(final String body) {
        //final String[] lines = org.apache.commons.lang.StringUtils.split(body, '\n');
        final String[] lines = Optional.ofNullable(body)
                                       .map(s -> s.split("\n"))
                                       .orElseGet(() -> new String[] {});
        if (lines.length > 0) {
            final StringBuilder sb = new StringBuilder();
            for (int idx = 0; idx < lines.length; idx++) {
                final String line = lines[idx];
                if (!line.toLowerCase().startsWith("<p>")) {
                    sb.append("<p>");
                }
                sb.append(line);
                if (!line.trim().toLowerCase().endsWith("</p>")) {
                    sb.append("</p>");
                }
                if (idx < (lines.length - 1)) {
                    sb.append("\n");
                }
            }
            return sb.toString();
        }
        return "<p>" + body + "</p>";
    }

    public static String paragraphsToLines(final String body) {
        if (body != null) {
            final StringBuilder sb = new StringBuilder();
            String s = body;
            if (s.toLowerCase().contains("<p>")) {
                s = normalizeLineEndings(s).replace("\n", "");
            }
            while (true) {
                int idx = s.toLowerCase().indexOf("<p>");
                if (idx >= 0) {
                    if (idx > 0) {
                        sb.append(s, 0, idx);
                        sb.append('\n');
                    }
                    s = s.substring(idx + 3);
                    int end = s.toLowerCase().indexOf("</p>", idx);
                    if (end < 0) {
                        end = s.length();
                    }
                    sb.append(s, 0, end);
                    sb.append('\n');
                    s = s.substring(end + 4);
                    if (s.length() == 0) {
                        break;
                    }
                } else {
                    sb.append(s);
                    break;
                }
            }
            return sb.toString();
        }
        return null;
    }

    public static String normalizeLineEndings(final String s) {
        return s.replace("\n ", "\n")
                .replace(" \n", "\n")
                .replace("\r\n", "\n")
                .replace("\n\r", "\n")
                .replace("\r", "\n");
    }

    public static String trim(String trim) {
        if (trim == null) {
            return "";
        } else {
            trim = trim.trim();
            return trim;
        }
    }

    public static Optional<String> getFirstNotEmpty(final String...values) {
        for (final String s : values) {
            if (notEmpty(s)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }
    
}

