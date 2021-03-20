package com.example.pingubot.plugins.impl.usermanager;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "ranks")
public class RankClasses {
    private Map<String, Rank> rankClasses;

    public Map<String, Rank> getRankClasses() {
        return rankClasses;
    }

    public void setRankClasses(Map<String, Rank> rankClasses) {
        this.rankClasses = rankClasses;
    }

    public static class Rank {
        private String img;
        private int lvl;
        private int cost;
        private String en;

        public void setImg(String img) {
            this.img = img;
        }

        public void setLvl(int lvl) {
            this.lvl = lvl;
        }

        public void setCost(int cost) {
            this.cost = cost;
        }

        public void setEn(String en) {
            this.en = en;
        }

        public String getImg() {
            return img;
        }

        public int getLvl() {
            return lvl;
        }

        public int getCost() {
            return cost;
        }

        public String getEn() {
            return en;
        }
    }
}