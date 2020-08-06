package com.atex.plugins.mailimporter;

import com.atex.onecms.content.ContentId;
import com.atex.onecms.content.ContentManager;
import com.atex.onecms.content.IdUtil;
import com.atex.onecms.content.Subject;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

	public static Pattern EMAIL_HTML_PATTERN = Pattern.compile("(.*)<p>(\\s*)<\\/p>(.*)");

	public static boolean isNull(String s) {
		return (!(notNull(s)));
	}

	public static boolean notNull(String s) {
		return ( ( s != null ) && (!(s.trim().toLowerCase().equalsIgnoreCase("null"))) &&  (!(s.trim().toLowerCase().equals(""))) );
	}

	public static boolean notNull(Object obj) {
		return ( obj != null );
	}

	public static boolean notNull(List<?> list) {
		return ( (list != null) && (!(list.isEmpty())) );
	}

	public static String dateToUTCDateString(Date date) {
		ZonedDateTime a = date.toInstant().atZone(ZoneId.of("UTC"));
		return a.format(DateTimeFormatter.ISO_INSTANT);
		//return longToUTCDateString(date.getTime());
	}

	public static String longToUTCDateString(Long longDate) {
		Date date = new Date(longDate);
		ZonedDateTime a = date.toInstant().atZone(ZoneId.of("UTC"));
		return a.format(DateTimeFormatter.ISO_INSTANT);
	}



	public static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	public static String getNormalizedDateString(String dateString) {

		String ret = null;

		String[] datePatterns = new String[]{"yyyy:MM:dd HH:mm:ss", "yyyy:MM:dd HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm",
				"yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss'Z'",
				"yyyy-MM-dd'T'HH:mm", "yyyy:MM:dd", "yyyy-MM-dd", "yyyy-MM", "yyyy", "yyyyMMdd"};

		SimpleDateFormat sdf = new SimpleDateFormat();
		SimpleDateFormat outputSdf = new SimpleDateFormat(ISO_DATE_FORMAT);
		for (int i = 0; i < datePatterns.length; i++) {
			try{
				sdf.applyPattern(datePatterns[i]);
				Date d = sdf.parse(dateString);
				ret = outputSdf.format(d);

				// if date in the future, take current
				if (d.after(new Date())){
					d = new Date();
					ret = outputSdf.format(d);
				}

				break;
			}catch(Exception ex){
				continue;
			}
		}
		return ret;

	}

	public static long getNormalizedDateLong(String dateString) {

		long ret = 0;

		String[] datePatterns = new String[]{"yyyy:MM:dd HH:mm:ss", "yyyy:MM:dd HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm",
				"yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss'Z'",
				"yyyy-MM-dd'T'HH:mm", "yyyy:MM:dd", "yyyy-MM-dd", "yyyy-MM", "yyyy", "yyyyMMdd"};

		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat();
		for (int i = 0; i < datePatterns.length; i++) {
			try{
				sdf.applyPattern(datePatterns[i]);
				d = sdf.parse(dateString);

				// if date in the future, take current
				if (d.after(new Date())){
					d = new Date();
				}
				ret = d.getTime();
				break;
			}catch(Exception ex){
				continue;
			}
		}
		return ret;

	}

	public static boolean isEnabled(String format, String granularity) {
		switch (granularity) {
			case "y":
				return format.indexOf('Y') != -1;
			case "M":
				return format.indexOf('M') != -1;
			case "d":
				return format.toLowerCase().indexOf('d') != -1;
			case "h":
			case "H":
				return format.toLowerCase().indexOf('h') != -1;
			case "m":
				return format.indexOf('m') != -1;
			case "s":
				return format.indexOf('s') != -1;
			default:
				return false;
		}
	}

	public static boolean  hasTime(String format) {
		return (isEnabled(format, "h") || isEnabled(format, "m") || isEnabled(format, "s"));
	}

	public static long getUTCDateLong(String dateString, String format) {
		long value = 0;
		try {
			SimpleDateFormat formatter = new SimpleDateFormat();
			formatter.applyPattern(format);
			Date date = formatter.parse(dateString);
			if (hasTime(format)) {
				value = date.getTime();
			} else {
				// The Date is now local, let's convert to utc midnight
				LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
				ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC"));
				value = zonedDateTime.toInstant().toEpochMilli();
			}
		} catch (ParseException exception) {
			exception.printStackTrace();
			LOGGER.error(exception.getMessage());
		} finally {
			return value;
		}
 	}

 	private static String getExtIdFromPlaceholder(String extIdPlaceHolder) {
		String extId = extIdPlaceHolder.startsWith("{{externalid/") ? extIdPlaceHolder.substring(13) : extIdPlaceHolder;
		extId = extId.endsWith("}}") ? extId.substring(0, extId.length() - 2) : extId;
		return extId;
	}

	public static String getContentId(ContentManager cm, String extIdPlaceHolder) {
		String extId = getExtIdFromPlaceholder(extIdPlaceHolder);
		ContentId id = cm.resolve(extId, Subject.NOBODY_CALLER).getContentId();
		return IdUtil.toIdString(id);
	}

	/**
	 * Transform the placeHolders inside input from {{externalid/my.ext.id}} into real content ids (i.e. policy:2.622)
	 */
 	public static String resolveExternalIdPlaceHolders(ContentManager cm, String input, boolean solrEscape) {
		Pattern pattern = Pattern.compile("\\{\\{externalid/(.*?)\\}\\}");
		Matcher matcher = pattern.matcher(input);
		while (matcher.find()) {
			String extIdPlaceHolder = matcher.group(0);
			String contentId = getContentId(cm, extIdPlaceHolder);
			if (solrEscape) {
				contentId = ClientUtils.escapeQueryChars(contentId);
			}
			StringBuilder stringBuilder = new StringBuilder(input);
			stringBuilder.replace(matcher.start(), matcher.end(), contentId);
			input = stringBuilder.toString();
			matcher = pattern.matcher(input);
		}
		return input;
	}

	public static void main(String[] args) {

		long l =  1480708800000L;
		Date dt = new Date(l);

		System.out.println(longToUTCDateString(l));
		System.out.println(dateToUTCDateString(dt));

//		l = 1497597331751L;
//		System.out.println(longToUTCDateString(l));
//		System.out.println(dateToUTCDateString(dt));
	}

	public static boolean isHtmlBody(String mailBody) {
		Matcher matcher = EMAIL_HTML_PATTERN.matcher(mailBody);
		return matcher.find();
	}

	protected static final Logger LOGGER = LoggerFactory.getLogger(StringUtils.class);
}

