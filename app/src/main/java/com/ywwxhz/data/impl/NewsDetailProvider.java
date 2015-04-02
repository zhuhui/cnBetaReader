package com.ywwxhz.data.impl;

import android.app.Activity;

import com.loopj.android.http.TextHttpResponseHandler;
import com.ywwxhz.data.BaseDataProvider;
import com.ywwxhz.entitys.NewsItem;
import com.ywwxhz.lib.Configure;
import com.ywwxhz.lib.kits.FileCacheKit;
import com.ywwxhz.lib.kits.NetKit;
import com.ywwxhz.lib.kits.Toolkit;

import org.apache.http.Header;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;

/**
 * cnBetaReader
 * <p/>
 * Created by 远望の无限(ywwxhz) on 2015/3/31 11:13.
 */
public class NewsDetailProvider extends BaseDataProvider {

    private TextHttpResponseHandler handler =  new TextHttpResponseHandler() {

        @Override
        public void onStart() {
            if(callback!=null) {
                callback.onLoadStart();
            }
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
            if(callback!=null) {
                callback.onLoadFailure();
            }
        }

        @Override
        public void onSuccess(int statusCode, Header[] headers, String responseString) {
            if(callback!=null){
                callback.onLoadSuccess(responseString);
            }
        }
    };

    public NewsDetailProvider(Activity activity) {
        super(activity);
    }

    @Override
    public void loadData(boolean startup) {

    }

    public void loadNewsAsync(String sid){
        NetKit.getInstance().getNewsBySid(sid,handler);
    }

    public static boolean handleResponceString(NewsItem item,String resp){
        Document doc = Jsoup.parse(resp);
        Elements newsHeadlines = doc.select(".body");
        item.setFrom(newsHeadlines.select(".where").html());
        item.setInputtime(newsHeadlines.select(".date").html());
        Elements introduce = newsHeadlines.select(".introduction");
        introduce.select("div").remove();
        item.setHometext(introduce.html());
        item.setContent(newsHeadlines.select(".content").html());
        Matcher snMatcher = Configure.SN_PATTERN.matcher(resp);
        if (snMatcher.find())
            item.setSN(snMatcher.group(1));
        if(item.getContent()!=null&&item.getContent().length()>0){
            FileCacheKit.getInstance().put(item.getSid() + "", Toolkit.getGson().toJson(item));
            return true;
        }else{
            return false;
        }
    }
}
