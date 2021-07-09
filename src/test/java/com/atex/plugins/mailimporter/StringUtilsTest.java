package com.atex.plugins.mailimporter;

import static com.atex.plugins.mailimporter.StringUtils.linesToParagraphs;
import static com.atex.plugins.mailimporter.StringUtils.paragraphsToLines;

import org.junit.Assert;
import org.junit.Test;

/**
 * StringUtilsTest
 *
 * @author mnova
 */
public class StringUtilsTest {

    @Test
    public void test_paragraphsToLines() {
        Assert.assertEquals("ciao", paragraphsToLines("ciao"));
        Assert.assertEquals("ciao\n", paragraphsToLines("<p>ciao</p>"));
        Assert.assertEquals("ciao\n", paragraphsToLines("<p>ciao</P>"));
        Assert.assertEquals("ciao\n", paragraphsToLines("<P>ciao</p>"));
        Assert.assertEquals("<b>ciao</b>\n", paragraphsToLines("<P><b>ciao</b></p>"));
        Assert.assertEquals("aaa\nciao\n", paragraphsToLines("aaa<p>ciao</p>"));
        Assert.assertEquals("1\n2\n\n3\n", paragraphsToLines("<p>1</p><p>2</p><p></p><p>3</p>"));
        Assert.assertEquals("1\n2\n\n3\n", paragraphsToLines("<p>1</p>\n<p>2</p>\n<p></p><p>3</p>\n"));
    }

    @Test
    public void test_linesToParagraphs() {
        Assert.assertEquals("<p>ciao</p>", linesToParagraphs("ciao"));
        Assert.assertEquals("<p>ciao</p>", linesToParagraphs("ciao\n"));
        Assert.assertEquals("<p>ciao</p>", linesToParagraphs("ciao\n\n"));
        Assert.assertEquals("<p>1</p>\n<p>2</p>\n<p></p>\n<p>3</p>", linesToParagraphs("1\n2\n\n3\n"));
    }

    @Test
    public void test_lines_conversion() {
        Assert.assertEquals("<p>ciao</p>", linesToParagraphs(paragraphsToLines("<p>ciao</p>")));
        Assert.assertEquals("<p>1</p>\n<p>2</p>\n<p></p>\n<p>3</p>", linesToParagraphs(paragraphsToLines("<p>1</p><p>2</p><p></p><p>3</p>")));
    }
}