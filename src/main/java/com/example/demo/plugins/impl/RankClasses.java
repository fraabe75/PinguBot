package com.example.demo.plugins.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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
        private String de;
        private String en;

        public void setImg(String img) {
            this.img = img;
        }

        public void setLvl(int lvl) {
            this.lvl = lvl;
        }

        public void setDe(String de) {
            this.de = de;
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

        public String getDe() {
            return de;
        }

        public String getEn() {
            return en;
        }
    }
}