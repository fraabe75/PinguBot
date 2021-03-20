package de.penguins.pingubot;

import de.penguins.pingubot.plugins.impl.usermanager.RankClasses;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RankMapFromYamlIntegrationTest {

    @Autowired
    private RankClasses rankClasses;

    @Test
    public void whenYamlFileProvidedThenInjectMap() {
        assertEquals(14, rankClasses.getRankClasses().size(), 10);
        RankClasses.Rank testRank = rankClasses.getRankClasses().get("adelie");
        assertNotNull(testRank);
        assertEquals("Adelie penguin", testRank.getEn());
        assertEquals(7, testRank.getLvl());
        assertEquals("pingu_ranks/adelie.jpg", testRank.getImg());
    }

}