package com.ywwxhz.processer;

import android.app.Activity;
import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.ResponseHandlerInterface;
import com.melnykov.fab.FloatingActionButton;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.ywwxhz.adapter.NewsListAdapter;
import com.ywwxhz.app.NewsDetailActivity;
import com.ywwxhz.app.SettingsActivity;
import com.ywwxhz.cnbetareader.R;
import com.ywwxhz.entity.NewsItem;
import com.ywwxhz.entity.NewsListObject;
import com.ywwxhz.entity.ResponseObject;
import com.ywwxhz.lib.PagedLoader;
import com.ywwxhz.lib.handler.BaseProcesser;
import com.ywwxhz.lib.handler.NormalNewsListHandler;
import com.ywwxhz.lib.kits.FileCacheKit;
import com.ywwxhz.lib.kits.NetKit;
import com.ywwxhz.lib.kits.PrefKit;
import com.ywwxhz.lib.kits.Toolkit;

import java.util.ArrayList;
import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.Options;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * Created by ywwxhz on 2014/11/1.
 */
public class NewsListProcesser extends BaseProcesser implements OnRefreshListener {
    private int topSid;
    private int current;
    private boolean hasCached;
    private Activity mContext;
    private ListView mListView;
    private PagedLoader mLoader;
    private ProgressWheel mProgressBar;
    private NewsListAdapter mAdapter;
    private FloatingActionButton actionButton;
    private PullToRefreshLayout mPullToRefreshLayout;
    private ResponseHandlerInterface newsPage = new NormalNewsListHandler(this, new TypeToken<ResponseObject<NewsListObject>>() {
    });
    private PagedLoader.OnLoadListener loadListener = new PagedLoader.OnLoadListener() {
        @Override
        public void onLoading(PagedLoader pagedLoader, boolean isAutoLoad) {
            NetKit.getInstance().getNewslistByPage(current + 1, "all", newsPage);
        }
    };

    public NewsListProcesser(final Activity mContext) {
        this.hasCached = false;
        this.mContext = mContext;
        this.mPullToRefreshLayout = new PullToRefreshLayout(mContext);
        this.mProgressBar = (ProgressWheel) mContext.findViewById(R.id.loading);
        this.mListView = (ListView) mContext.findViewById(android.R.id.list);
        this.actionButton = (FloatingActionButton) mContext.findViewById(R.id.action);
        this.mAdapter = new NewsListAdapter(mContext, new ArrayList<NewsItem>());
        TextView view = (TextView) LayoutInflater.from(mContext).inflate(R.layout.type_head, mListView, false);
        view.setText("类型：全部资讯");
        this.mListView.addHeaderView(view, null, false);
        this.mLoader = PagedLoader.Builder.getInstance(mContext).setListView(mListView).setOnLoadListener(loadListener).builder();
        this.mLoader.setAdapter(mAdapter);
        this.mLoader.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), true, true
                , this.actionButton.attachToListView(this.mListView, null, false)));
        this.actionButton.setVisibility(View.VISIBLE);
        this.actionButton.setImageResource(R.mipmap.ic_settings);
        this.actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, SettingsActivity.class);
                mContext.startActivityForResult(intent, 100);
            }
        });
        this.actionButton.setScaleX(0);
        this.actionButton.setScaleY(0);
        this.actionButton.postDelayed(new Runnable() {
            @Override
            public void run() {
                actionButton.setVisibility(View.VISIBLE);
                actionButton.animate().scaleX(1).scaleY(1).setDuration(500).setInterpolator(
                        new AccelerateDecelerateInterpolator()).start();
            }
        }, 200);
        this.mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(NewsListProcesser.this.mContext, NewsDetailActivity.class);
                intent.putExtra(NewsDetailProcesser.NEWS_ITEM_KEY, mAdapter.getDataSetItem(i - 1));
                NewsListProcesser.this.mContext.startActivity(intent);
            }
        });
        ActionBarPullToRefresh.from(mContext)
                .insertLayoutInto((ViewGroup) mContext.findViewById(android.R.id.content))
                .theseChildrenArePullable(mListView)
                .listener(this)
                .options(Options.create().scrollDistance(0.2f).refreshOnUp(true).build())
                .setup(mPullToRefreshLayout);
        loadData(true);
    }

    private void loadData(boolean startup) {
        ArrayList<NewsItem> newsList = FileCacheKit.getInstance().getAsObject("newsList".hashCode() + "", "list", new TypeToken<ArrayList<NewsItem>>() {
        });
        if (mAdapter.getCount() == 0) {
            mProgressBar.setVisibility(View.VISIBLE);
        }
        if (newsList != null) {
            hasCached = true;
            topSid = newsList.get(1).getSid();
            mAdapter.setDataSet(newsList);
            mLoader.notifyDataSetChanged();
            mProgressBar.setVisibility(View.GONE);
        } else {
            this.hasCached = false;
        }
        this.current = 1;
        if (!hasCached || PrefKit.getBoolean(mContext, mContext.getString(R.string.pref_auto_reflush_key), false)) {
            mListView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPullToRefreshLayout.setRefreshing(true);
                    onRefreshStarted(null);
                }
            }, startup ? 400 : 0);
        }
    }

    public void onResume() {
        if (PrefKit.getBoolean(mContext, mContext.getString(R.string.pref_auto_page_key), false)) {
            this.mLoader.setMode(PagedLoader.Mode.AUTO_LOAD);
        } else {
            this.mLoader.setMode(PagedLoader.Mode.CLICK_TO_LOAD);
        }
    }

    public ListView getListView() {
        return mListView;
    }

    public Activity getContext() {
        return mContext;
    }

    public NewsListAdapter getAdapter() {
        return mAdapter;
    }

    public PagedLoader getLoader() {
        return mLoader;
    }

    public PullToRefreshLayout getPullToRefreshLayout() {
        return mPullToRefreshLayout;
    }

    @Override
    public void onRefreshStarted(View view) {
        NetKit.getInstance().getNewslistByPage(1, "all", newsPage);
    }

    public void callNewsPageLoadSuccess(NewsListObject listPage) {
        List<NewsItem> itemList = listPage.getList();
        List<NewsItem> dataSet = mAdapter.getDataSet();
        int size = 0;
        boolean find = false;
        for (int i = 0; i < itemList.size(); i++) {
            NewsItem item = itemList.get(i);
            if (itemList.get(i).getCounter() != null && item.getComments() != null) {
                int num = Integer.parseInt(item.getCounter());
                if (num > 9999) {
                    item.setCounter("9999+");
                }
                num = Integer.parseInt(item.getComments());
                if (num > 999) {
                    item.setComments("999+");
                }
            } else {
                item.setCounter("0");
                item.setComments("0");
            }
            StringBuilder sb = new StringBuilder(Html.fromHtml(item.getHometext().replaceAll("<.*?>|[\\r|\\n]", "")));
//            int index = sb.indexOf("。");
//            if(index >25 && index <50){
//                item.setSummary(sb.delete(index+1,sb.length()).toString());
//            }else
            if (sb.length() > 140) {
                item.setSummary(sb.replace(140, sb.length(), "...").toString());
            } else {
                item.setSummary(sb.toString());
            }
            if (item.getThumb().contains("thumb")) {
                item.setLargeImage(item.getThumb().replaceAll("(\\.\\w{3,4})?_100x100|thumb/mini/", ""));
            }
            if (!find && item.getSid() != topSid) {
                size++;
            } else if (!find) {
                find = true;
            }
        }
        if (!find) {
            size++;
        }

        if (!hasCached || listPage.getPage() == 1) {
            hasCached = true;
            mAdapter.setDataSet(itemList);
            topSid = itemList.get(1).getSid();
            showToastAndCache(itemList, size - 1);
        } else {
            dataSet.addAll(itemList);
        }
        current = listPage.getPage();
    }

    private void showToastAndCache(List<NewsItem> itemList, int size) {
        if (size < 1) {
            Crouton.makeText(mContext, mContext.getString(R.string.message_no_new_news), Style.CONFIRM).show();
        } else {
            Crouton.makeText(mContext, mContext.getString(R.string.message_new_news, size), Style.INFO).show();
        }
        FileCacheKit.getInstance().putAsync("newsList".hashCode() + "", Toolkit.getGson().toJson(itemList), "list", null);
    }

    public void setLoadFinish() {
        if (mLoader.getLoading()) {
            mLoader.setLoading(false);
        }
        mLoader.notifyDataSetChanged();
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            mProgressBar.setVisibility(View.GONE);
        }
        if (mPullToRefreshLayout.isRefreshing()) {
            mPullToRefreshLayout.setRefreshComplete();
        }
    }

    public View getFloatButtom() {
        return actionButton;
    }

    public void onReturn(int request, int response) {
        if (response == 200) {
            mAdapter.notifyDataSetChanged(true);
        }
    }
}