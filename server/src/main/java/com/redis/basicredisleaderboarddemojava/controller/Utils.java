package com.redis.basicredisleaderboarddemojava.controller;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Utils {
    public static String resetData(boolean isDataReady, Jedis jedis, String dataReadyRedisKey,
                                   String redisLeaderboard, String keyPrefix) {
        boolean isOk = true;
        if (!isDataReady){
            System.out.println("resetData");

            try {
                JSONArray companyJsonArray = new JSONArray(readFile("src/main/resources/data.json"));
                JSONObject companyJson;
                String symbol;
                System.out.println("resetData1");

                for (int i = 0; i < companyJsonArray.length(); i++) {
                    companyJson = companyJsonArray.getJSONObject(i);
                    System.out.println("resetData2");
                    symbol = addPrefix(keyPrefix, companyJson.get("symbol").toString().toLowerCase());
                    System.out.println(symbol);
                    jedis.zadd(redisLeaderboard, Double.parseDouble(companyJson.get("marketCap").toString()), symbol);
                    jedis.hset(symbol, "company", companyJson.get("company").toString());
                    jedis.hset(symbol, "country", companyJson.get("country").toString());
                }
                jedis.set(dataReadyRedisKey, "true");
            } catch (Exception e) {
                isOk = false;
            }
        }
        return String.format("{\"succes\":%s}", isOk);
    }

    protected static String getRedisDataZrangeWithScores(int start, int end, Jedis jedis, String redisLeaderboard,
                                                         String prefix) {
        Set<Tuple> zrangeWithScores =  jedis.zrangeWithScores(redisLeaderboard, start, end);
        return resultList(zrangeWithScores, jedis,
                new AtomicInteger((zrangeWithScores.size() + 1) / (1 - start)),
                false, prefix);
    }

    protected static String getRedisDataZrevrangeWithScores(int start, int end, Jedis jedis, String redisLeaderboard,
                                                            String prefix) {
        return resultList(jedis.zrevrangeWithScores(redisLeaderboard, start, end), jedis,
                new AtomicInteger(start),
                true, prefix);
    }

    private static String readFile(String filename) {
        String result = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            result = sb.toString();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static String resultList(Set<Tuple> jedisRedis, Jedis jedis, AtomicInteger index, boolean isIncrease,
                                     String prefix) {
        List<JSONObject> resultList = new ArrayList<>();
        jedisRedis.forEach((k) -> {

            Map<String, String> company = jedis.hgetAll(k.getElement());
            JSONObject json = addDataToResult(company, ((Double) k.getScore()).longValue(), k.getElement(), prefix);
            try {
                json.put("rank",  isIncrease ? index.incrementAndGet() : index.decrementAndGet());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            resultList.add(json);
        });
        return resultList.toString();
    }

    protected static JSONObject addDataToResult(Map<String, String> company, Long marketCap, String symbol,
                                                String prefix){
        JSONObject json = new JSONObject();
        try {
            json.put("marketCap", (marketCap));
            json.put("symbol", deletePrefix(prefix, symbol));
            json.put("country", company.get("country"));
            json.put("company", company.get("company"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    protected static String addPrefix(String prefix, String symbol){
        return prefix+ ":" + symbol;
    }

    protected static String deletePrefix(String prefix, String symbol){
        return symbol.replace(prefix +":" , "");
    }
}
