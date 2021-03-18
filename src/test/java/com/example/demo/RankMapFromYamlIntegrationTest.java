package com.example.demo;

import com.example.demo.plugins.impl.RankClasses;
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
        assertEquals(rankClasses.getRankClasses().size(), 10);
        RankClasses.Rank testRank = rankClasses.getRankClasses().get("adelie");
        assertNotNull(testRank);
        assertEquals(testRank.getEn(), "Adelie");
        assertEquals(testRank.getLvl(), 5);
        assertEquals(testRank.getImg(), "src/main/resources/pingu_ranks/adelie.jpg");
    }

}