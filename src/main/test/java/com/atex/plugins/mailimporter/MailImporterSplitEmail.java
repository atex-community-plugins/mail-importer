package com.atex.plugins.mailimporter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MailImporterSplitEmail {

    @Test
    public void testSplitHtmlEmail() {

        String testBody = "Leichtathletik Athleten der LG Teck feiern bei verschiedenen Meetings vor allem in den Wurfdisziplinen zahlreiche Erfolge.\n" +
          "<p> </p>\n" +
          "<p>Weilheim. Beim Werferabend mit anschließenden Jugendsportfest in Gomaringen, wetteiferten insgesamt rund 260 Jugendliche aus 53 Vereinen aus den Landesverbänden Württemberg, Baden und Hessen um Siege, Bestleistungen und Normen. Mit dabei waren auch Athleten der LG Teck. Mit neuer persönlicher Bestleistung im Kugelstoßen der M14 beeindruckte Alexander Doll aus Oberlenningen mit 11,56 Metern. Mit dieser Weite belegt er den siebten Platz in der Deutschen M14-Rangliste. Max Schumacher (LG Teck) wurde mit 9,22 Metern Siebter. Zwei Podestplätze erreichte der Weilheimer „Techniker“ im Speerwerfen der M14 mit 30,35 Metern (2.) und im Diskuswerfen mit 31,39 Meter (3.).</p>\n" +
          "<p> </p>";

        MailParser mailParser = new MailParser();
        MailBean mailBean = new MailBean();
        mailParser.setHtmlContent(mailBean, testBody);
        assertEquals("Leichtathletik Athleten der LG Teck feiern bei verschiedenen Meetings vor allem in den Wurfdisziplinen zahlreiche Erfolge.", mailBean.getLead());
        assertEquals("<p>Weilheim. Beim Werferabend mit anschlie&szlig;enden Jugendsportfest in Gomaringen, wetteiferten insgesamt rund 260 Jugendliche aus 53 Vereinen aus den Landesverb&auml;nden W&uuml;rttemberg, Baden und Hessen um Siege, Bestleistungen und Normen. Mit dabei waren auch Athleten der LG Teck. Mit neuer pers&ouml;nlicher Bestleistung im Kugelsto&szlig;en der M14 beeindruckte Alexander Doll aus Oberlenningen mit 11,56 Metern. Mit dieser Weite belegt er den siebten Platz in der Deutschen M14-Rangliste. Max Schumacher (LG Teck) wurde mit 9,22 Metern Siebter. Zwei Podestpl&auml;tze erreichte der Weilheimer &bdquo;Techniker&ldquo; im Speerwerfen der M14 mit 30,35 Metern (2.) und im Diskuswerfen mit 31,39 Meter (3.).</p>\n" +
          "<p> </p>", mailBean.getBody());
    }

    @Test
    public void testSplitHtmlEmailMorePTags() {

        String testBody = "Leichtathletik Athleten der LG Teck feiern bei verschiedenen Meetings vor allem in den Wurfdisziplinen zahlreiche Erfolge.\n" +
          "<p> </p>\n" +
          "<p> </p>\n" +
          "<p> </p>\n" +
          "<p> </p>\n" +
          "<p>Weilheim. Beim Werferabend mit anschließenden Jugendsportfest in Gomaringen, wetteiferten insgesamt rund 260 Jugendliche aus 53 Vereinen aus den Landesverbänden Württemberg, Baden und Hessen um Siege, Bestleistungen und Normen. Mit dabei waren auch Athleten der LG Teck. Mit neuer persönlicher Bestleistung im Kugelstoßen der M14 beeindruckte Alexander Doll aus Oberlenningen mit 11,56 Metern. Mit dieser Weite belegt er den siebten Platz in der Deutschen M14-Rangliste. Max Schumacher (LG Teck) wurde mit 9,22 Metern Siebter. Zwei Podestplätze erreichte der Weilheimer „Techniker“ im Speerwerfen der M14 mit 30,35 Metern (2.) und im Diskuswerfen mit 31,39 Meter (3.).</p>\n" +
          "<p> </p>";

        MailParser mailParser = new MailParser();
        MailBean mailBean = new MailBean();
        mailParser.setHtmlContent(mailBean, testBody);
        assertEquals("Leichtathletik Athleten der LG Teck feiern bei verschiedenen Meetings vor allem in den Wurfdisziplinen zahlreiche Erfolge.", mailBean.getLead());
        assertEquals("<p> </p>\n" +
          "<p> </p>\n" +
          "<p> </p>\n" +
          "<p>Weilheim. Beim Werferabend mit anschlie&szlig;enden Jugendsportfest in Gomaringen, wetteiferten insgesamt rund 260 Jugendliche aus 53 Vereinen aus den Landesverb&auml;nden W&uuml;rttemberg, Baden und Hessen um Siege, Bestleistungen und Normen. Mit dabei waren auch Athleten der LG Teck. Mit neuer pers&ouml;nlicher Bestleistung im Kugelsto&szlig;en der M14 beeindruckte Alexander Doll aus Oberlenningen mit 11,56 Metern. Mit dieser Weite belegt er den siebten Platz in der Deutschen M14-Rangliste. Max Schumacher (LG Teck) wurde mit 9,22 Metern Siebter. Zwei Podestpl&auml;tze erreichte der Weilheimer &bdquo;Techniker&ldquo; im Speerwerfen der M14 mit 30,35 Metern (2.) und im Diskuswerfen mit 31,39 Meter (3.).</p>\n" +
          "<p> </p>", mailBean.getBody());
    }

    @Test
    public void testSplitHtmlEmailTabs() {

        String testBody = "Leichtathletik Athleten der LG Teck feiern bei verschiedenen Meetings vor allem in den Wurfdisziplinen zahlreiche Erfolge.\n" +
          "<p> \t</p>\n" +
          "<p>Weilheim. Beim Werferabend mit anschließenden Jugendsportfest in Gomaringen, wetteiferten insgesamt rund 260 Jugendliche aus 53 Vereinen aus den Landesverbänden Württemberg, Baden und Hessen um Siege, Bestleistungen und Normen. Mit dabei waren auch Athleten der LG Teck. Mit neuer persönlicher Bestleistung im Kugelstoßen der M14 beeindruckte Alexander Doll aus Oberlenningen mit 11,56 Metern. Mit dieser Weite belegt er den siebten Platz in der Deutschen M14-Rangliste. Max Schumacher (LG Teck) wurde mit 9,22 Metern Siebter. Zwei Podestplätze erreichte der Weilheimer „Techniker“ im Speerwerfen der M14 mit 30,35 Metern (2.) und im Diskuswerfen mit 31,39 Meter (3.).</p>\n" +
          "<p> </p>";

        MailParser mailParser = new MailParser();
        MailBean mailBean = new MailBean();
        mailParser.setHtmlContent(mailBean, testBody);
        assertEquals("Leichtathletik Athleten der LG Teck feiern bei verschiedenen Meetings vor allem in den Wurfdisziplinen zahlreiche Erfolge.", mailBean.getLead());
        assertEquals("<p>Weilheim. Beim Werferabend mit anschlie&szlig;enden Jugendsportfest in Gomaringen, wetteiferten insgesamt rund 260 Jugendliche aus 53 Vereinen aus den Landesverb&auml;nden W&uuml;rttemberg, Baden und Hessen um Siege, Bestleistungen und Normen. Mit dabei waren auch Athleten der LG Teck. Mit neuer pers&ouml;nlicher Bestleistung im Kugelsto&szlig;en der M14 beeindruckte Alexander Doll aus Oberlenningen mit 11,56 Metern. Mit dieser Weite belegt er den siebten Platz in der Deutschen M14-Rangliste. Max Schumacher (LG Teck) wurde mit 9,22 Metern Siebter. Zwei Podestpl&auml;tze erreichte der Weilheimer &bdquo;Techniker&ldquo; im Speerwerfen der M14 mit 30,35 Metern (2.) und im Diskuswerfen mit 31,39 Meter (3.).</p>\n" +
          "<p> </p>", mailBean.getBody());
    }

    @Test
    public void testSplitPlainTextEmail() {

        String testBody = "Leichtathletik Athleten der LG Teck feiern bei verschiedenen Meetings vor allem in den Wurfdisziplinen zahlreiche Erfolge.\n" +
          "\n" +
          "Weilheim. Beim Werferabend mit anschließenden Jugendsportfest in Gomaringen, wetteiferten insgesamt rund 260 Jugendliche aus 53 Vereinen aus den Landesverbänden Württemberg, Baden und Hessen um Siege, Bestleistungen und Normen. Mit dabei waren auch Athleten der LG Teck. Mit neuer persönlicher Bestleistung im Kugelstoßen der M14 beeindruckte Alexander Doll aus Oberlenningen mit 11,56 Metern. Mit dieser Weite belegt er den siebten Platz in der Deutschen M14-Rangliste. Max Schumacher (LG Teck) wurde mit 9,22 Metern Siebter. Zwei Podestplätze erreichte der Weilheimer „Techniker“ im Speerwerfen der M14 mit 30,35 Metern (2.) und im Diskuswerfen mit 31,39 Meter (3.).\n" +
          "\n";

        MailParser mailParser = new MailParser();
        MailBean mailBean = new MailBean();
        mailParser.setPlainTextContent(mailBean, testBody);
        assertEquals("Leichtathletik Athleten der LG Teck feiern bei verschiedenen Meetings vor allem in den Wurfdisziplinen zahlreiche Erfolge.", mailBean.getLead());
        assertEquals("<p>Weilheim. Beim Werferabend mit anschlie&szlig;enden Jugendsportfest in Gomaringen, wetteiferten insgesamt rund 260 Jugendliche aus 53 Vereinen aus den Landesverb&auml;nden W&uuml;rttemberg, Baden und Hessen um Siege, Bestleistungen und Normen. Mit dabei waren auch Athleten der LG Teck. Mit neuer pers&ouml;nlicher Bestleistung im Kugelsto&szlig;en der M14 beeindruckte Alexander Doll aus Oberlenningen mit 11,56 Metern. Mit dieser Weite belegt er den siebten Platz in der Deutschen M14-Rangliste. Max Schumacher (LG Teck) wurde mit 9,22 Metern Siebter. Zwei Podestpl&auml;tze erreichte der Weilheimer &bdquo;Techniker&ldquo; im Speerwerfen der M14 mit 30,35 Metern (2.) und im Diskuswerfen mit 31,39 Meter (3.).</p>", mailBean.getBody());
    }

}
