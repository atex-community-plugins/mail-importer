package com.atex.plugins.mailimporter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.atex.onecms.content.ContentManager;
import com.polopoly.model.ModelDomain;

public class MailImporterMailFormatCheckerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    ContentManager contentManager;

    @Mock
    ModelDomain modelDomain;

    @Test
    public void testHtmlMail() {
        String testBody = "Leichtathletik Athleten der LG Teck feiern bei verschiedenen Meetings vor allem in den Wurfdisziplinen zahlreiche Erfolge.\n" +
          "<p> </p>\n" +
          "<p>Weilheim. Beim Werferabend mit anschließenden Jugendsportfest in Gomaringen, wetteiferten insgesamt rund 260 Jugendliche aus 53 Vereinen aus den Landesverbänden Württemberg, Baden und Hessen um Siege, Bestleistungen und Normen. Mit dabei waren auch Athleten der LG Teck. Mit neuer persönlicher Bestleistung im Kugelstoßen der M14 beeindruckte Alexander Doll aus Oberlenningen mit 11,56 Metern. Mit dieser Weite belegt er den siebten Platz in der Deutschen M14-Rangliste. Max Schumacher (LG Teck) wurde mit 9,22 Metern Siebter. Zwei Podestplätze erreichte der Weilheimer „Techniker“ im Speerwerfen der M14 mit 30,35 Metern (2.) und im Diskuswerfen mit 31,39 Meter (3.).</p>\n" +
          "<p> </p>";

        MailProcessorUtils mailProcessorUtils = new MailProcessorUtils(contentManager, modelDomain);
        assertTrue(StringUtils.isHtmlBody(testBody));
    }

    @Test
    public void testPlainTextMail() {
        String testBody = "Leichtathletik Athleten der LG Teck feiern bei verschiedenen Meetings vor allem in den Wurfdisziplinen zahlreiche Erfolge.\n" +
          "Weilheim. Beim Werferabend mit anschließenden Jugendsportfest in Gomaringen, wetteiferten insgesamt rund 260 Jugendliche aus 53 Vereinen aus den Landesverbänden Württemberg, Baden und Hessen um Siege, Bestleistungen und Normen. Mit dabei waren auch Athleten der LG Teck. Mit neuer persönlicher Bestleistung im Kugelstoßen der M14 beeindruckte Alexander Doll aus Oberlenningen mit 11,56 Metern. Mit dieser Weite belegt er den siebten Platz in der Deutschen M14-Rangliste. Max Schumacher (LG Teck) wurde mit 9,22 Metern Siebter. Zwei Podestplätze erreichte der Weilheimer „Techniker“ im Speerwerfen der M14 mit 30,35 Metern (2.) und im Diskuswerfen mit 31,39 Meter (3.).\n" +
          "\n";

        MailProcessorUtils mailProcessorUtils = new MailProcessorUtils(contentManager, modelDomain);
        assertFalse(StringUtils.isHtmlBody(testBody));
    }
}
