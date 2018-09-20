package com.habitrpg.android.habitica.ui.adapter.tasks;


import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.habitrpg.android.habitica.R;
import com.habitrpg.android.habitica.models.shops.ShopItem;
import com.habitrpg.android.habitica.models.tasks.Task;
import com.habitrpg.android.habitica.models.user.User;
import com.habitrpg.android.habitica.ui.viewHolders.ShopItemViewHolder;
import com.habitrpg.android.habitica.ui.viewHolders.tasks.RewardViewHolder;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.functions.Action;
import io.reactivex.subjects.PublishSubject;
import io.realm.OrderedRealmCollection;

public class RewardsRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements TaskRecyclerViewAdapter {
    private static int VIEWTYPE_CUSTOM_REWARD = 0;
    private static int VIEWTYPE_HEADER = 1;
    private static int VIEWTYPE_IN_APP_REWARD = 2;


    private final Context context;
    private OrderedRealmCollection<Task> customRewards;
    private OrderedRealmCollection<ShopItem> inAppRewards;
    private final int layoutResource;
    @Nullable
    private User user;

    private PublishSubject<String> errorButtonEvents = PublishSubject.create();

    public RewardsRecyclerViewAdapter(@Nullable OrderedRealmCollection<Task> data, Context context, int layoutResource, @Nullable User user) {
        this.context = context;
        this.layoutResource = layoutResource;
        this.customRewards = data;
        this.user = user;
    }

        private View getContentView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(layoutResource, parent, false);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEWTYPE_CUSTOM_REWARD) {
            return new RewardViewHolder(getContentView(parent));
        } else {
            return new ShopItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_shopitem, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (customRewards != null && position < customRewards.size()) {
            Task reward = customRewards.get(position);
            Double gold = 0.0;
            if (user != null && user.getStats() != null) {
                gold = user.getStats().getGp();
            }
            ((RewardViewHolder)holder).bindHolder(reward, position, reward.getValue() < gold);
        } else if (inAppRewards != null) {
            ShopItem item = inAppRewards.get(position-getCustomRewardCount());
            ((ShopItemViewHolder)holder).bind(item, item.canAfford(user));
            ((ShopItemViewHolder)holder).setPinned(true);
            ((ShopItemViewHolder) holder).hidePinIndicator();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (customRewards != null && position < customRewards.size()) {
            return VIEWTYPE_CUSTOM_REWARD;
        } else {
            return VIEWTYPE_IN_APP_REWARD;
        }
    }

    @Override
    public void setIgnoreUpdates(boolean ignoreUpdates) {

    }

    @Override
    public boolean getIgnoreUpdates() {
        return false;
    }

    @Override
    public void updateUnfilteredData(@org.jetbrains.annotations.Nullable OrderedRealmCollection<Task> data) {
        updateData(data);
    }

    @Override
    public Flowable<String> getErrorButtonEvents() {
        return errorButtonEvents.toFlowable(BackpressureStrategy.DROP);
    }

    @Override
    public int getItemCount() {
        int rewardCount = getCustomRewardCount();
        rewardCount += getInAppRewardCount();
        return rewardCount;
    }

    private int getInAppRewardCount() {
        return inAppRewards != null ? inAppRewards.size() : 0;
    }

    private int getCustomRewardCount() {
        return customRewards != null ? customRewards.size() : 0;
    }

    @Override
    public void updateData(OrderedRealmCollection<Task> data) {
        this.customRewards = data;
        notifyDataSetChanged();
    }

    public void updateItemRewards(OrderedRealmCollection<ShopItem> items) {
        if (items.size() > 0) {
            if (Task.class.isAssignableFrom(items.first().getClass())) {
                //this catches a weird bug where the observable gets a list of tasks for no apparent reason.
                return;
            }
        }
        this.inAppRewards = items;
        notifyDataSetChanged();
    }

    @Override
    public void filter() {
    }

    @Override
    public String getTaskIDAt(int position) {
        return customRewards.get(position).getId();
    }
}
