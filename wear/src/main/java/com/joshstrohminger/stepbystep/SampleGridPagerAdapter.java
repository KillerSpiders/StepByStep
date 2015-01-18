package com.joshstrohminger.stepbystep;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Constructs fragments as requested by the GridViewPager. For each row a different background is provided.
 * <p>
 * Always avoid loading resources from the main thread. In this sample, the background images are
 * loaded from an background task and then updated using {@link #notifyRowBackgroundChanged(int)}
 * and {@link #notifyPageBackgroundChanged(int, int)}.
 */
public class SampleGridPagerAdapter extends FragmentGridPagerAdapter {
    private static final int TRANSITION_DURATION_MILLIS = 100;

    private final Context mContext;
    private GridViewPager pager;
    private List<Row> mRows;
    private ColorDrawable mDefaultBg;

    private ColorDrawable mClearBg;

    public SampleGridPagerAdapter(Context ctx, FragmentManager fm, GridViewPager pager, String title, String subtitle, String[] instructions) {
        super(fm);
        mContext = ctx;
        this.pager = pager;

        mRows = new ArrayList<>();

        mRows.add(new Row(cardFragment(ctx.getString(R.string.app_name), title)));
        for( int i = 0; i < instructions.length; ++i) {
            mRows.add(new Row(cardFragment((i+1) + " of " + instructions.length, instructions[i], R.drawable.logo_simple)));
        }
        mRows.add(new Row(cardFragment("Done", title)));

//        mRows.add(new Row(cardFragment(R.string.about_title, R.string.about_text)));
//        mRows.add(new Row(
//                cardFragment(R.string.cards_title, R.string.cards_text),
//                cardFragment(R.string.expansion_title, R.string.expansion_text)));

        mDefaultBg = new ColorDrawable(R.color.dark_grey);
        mClearBg = new ColorDrawable(android.R.color.transparent);
    }

    LruCache<Integer, Drawable> mRowBackgrounds = new LruCache<Integer, Drawable>(3) {
        @Override
        protected Drawable create(final Integer row) {
            int resid = BG_IMAGES[row % BG_IMAGES.length];
            new DrawableLoadingTask(mContext) {
                @Override
                protected void onPostExecute(Drawable result) {
                    TransitionDrawable background = new TransitionDrawable(new Drawable[] {
                            mDefaultBg,
                            result
                    });
                    mRowBackgrounds.put(row, background);
                    notifyRowBackgroundChanged(row);
                    background.startTransition(TRANSITION_DURATION_MILLIS);
                }
            }.execute(resid);
            return mDefaultBg;
        }
    };

    LruCache<Point, Drawable> mPageBackgrounds = new LruCache<Point, Drawable>(3) {
        @Override
        protected Drawable create(final Point page) {
            // place bugdroid as the background at row 2, column 1
//            if (page.y == 2 && page.x == 1) {
//                int resid = R.drawable.bugdroid_large;
//                new DrawableLoadingTask(mContext) {
//                    @Override
//                    protected void onPostExecute(Drawable result) {
//                        TransitionDrawable background = new TransitionDrawable(new Drawable[] {
//                                mClearBg,
//                                result
//                        });
//                        mPageBackgrounds.put(page, background);
//                        notifyPageBackgroundChanged(page.y, page.x);
//                        background.startTransition(TRANSITION_DURATION_MILLIS);
//                    }
//                }.execute(resid);
//            }
            return GridPagerAdapter.BACKGROUND_NONE;
        }
    };

    private Fragment cardFragment(String title, String text, int icon) {
        CardFragment fragment = CardFragment.create(title, text, icon);
        fragment.setCardMarginBottom(mContext.getResources().getDimensionPixelSize(R.dimen.card_margin_bottom));
        return fragment;
    }

    private Fragment cardFragment(String title, String text) {
        CardFragment fragment = CardFragment.create(title, text);
        fragment.setCardMarginBottom(mContext.getResources().getDimensionPixelSize(R.dimen.card_margin_bottom));
        return fragment;
    }

    static final int[] BG_IMAGES = new int[] {
            R.drawable.debug_background_1,
            R.drawable.debug_background_2,
            R.drawable.debug_background_3,
            R.drawable.debug_background_4,
            R.drawable.debug_background_5
    };

    /** A convenient container for a row of fragments. */
    private class Row {
        final List<Fragment> columns = new ArrayList<>();

        public Row(Fragment... fragments) {
            for (Fragment f : fragments) {
                add(f);
            }
        }

        public void add(Fragment f) {
            columns.add(f);
        }

        Fragment getColumn(int i) {
            return columns.get(i);
        }

        public int getColumnCount() {
            return columns.size();
        }
    }

    @Override
    public Fragment getFragment(int row, int col) {
        Row adapterRow = mRows.get(row);
        return adapterRow.getColumn(col);
    }

    @Override
    public Drawable getBackgroundForRow(final int row) {
        return mRowBackgrounds.get(row);
    }

    @Override
    public Drawable getBackgroundForPage(final int row, final int column) {
        return mPageBackgrounds.get(new Point(column, row));
    }

    @Override
    public int getRowCount() {
        return mRows.size();
    }

    @Override
    public int getColumnCount(int rowNum) {
        return mRows.get(rowNum).getColumnCount();
    }

    class DrawableLoadingTask extends AsyncTask<Integer, Void, Drawable> {
        private static final String TAG = "Loader";
        private Context context;

        DrawableLoadingTask(Context context) {
            this.context = context;
        }

        @Override
        protected Drawable doInBackground(Integer... params) {
            Log.d(TAG, "Loading asset 0x" + Integer.toHexString(params[0]));
            return context.getResources().getDrawable(params[0]);
        }
    }
}