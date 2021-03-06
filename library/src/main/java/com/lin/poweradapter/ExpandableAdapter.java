package com.lin.poweradapter;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v7.widget.RecyclerView;

import com.lin.poweradapter.ParentExpandViewHolder.ParentViewHolderExpandCollapseListener;
import com.lin.poweradapter.model.Parent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Created by lin18 on 2017/4/24.
 */

public abstract class ExpandableAdapter<T, P extends Parent<C>, C, VH extends PowerViewHolder> extends MultiAdapter<T, VH> {

    @NonNull
    private List<RecyclerView> mAttachedRecyclerViewPool;
    @NonNull
    protected List<P> mAllItems;
    private Map<P, Boolean> mExpansionStateMap;
    @Nullable
    private ExpandCollapseListener mExpandCollapseListener;

    public ExpandableAdapter(@NonNull List<T> items) {
        super(items);
        init(items);
    }

    public ExpandableAdapter(@Nullable Object listener) {
        super(listener);
        init(new ArrayList<T>());
    }

    public ExpandableAdapter(@Nullable Object listener, @NonNull List<T> items) {
        super(listener, items);
        init(items);
    }

    private void init(@NonNull List<T> items) {
        mAllItems = (List<P>) items;
        mAttachedRecyclerViewPool = new ArrayList<>();
        mExpansionStateMap = new HashMap<>(items.size());
    }

    @Override
    public void onBindVHolder(VH holder, int position) {
        super.onBindVHolder(holder, position);
        if (getItemViewType(position) != LOAD_MORE
                && getItemViewType(position) != HEADER
                && getItemViewType(position) != FOOTER) {
            bind(holder, position);
        }
    }

    @Override
    public void onBindViewHolder(VH holder, int position, List<Object> payloads) {
        if (getItemViewType(position) == LOAD_MORE) {

        } else if (getItemViewType(position) == HEADER) {

        } else if (getItemViewType(position) == FOOTER) {

        } else {
            bind(holder, position);
            delegatesManager.onBindViewHolder(getItem(position), position, holder, payloads);
        }
    }

    private void bind(@NonNull final VH holder, final int position) {
        if (holder instanceof ParentExpandViewHolder) {
            final ParentExpandViewHolder parentViewHolder = (ParentExpandViewHolder) holder;
            if (parentViewHolder.shouldItemViewClickToggleExpansion()) {
                parentViewHolder.setMainItemClickToExpand();
            }
            parentViewHolder.mParent = (Parent) getItem(position);
            parentViewHolder.mExpandableAdapter = this;
            parentViewHolder.setParentViewHolderExpandCollapseListener(new ParentViewHolderExpandCollapseListener() {

                @Override
                public void onParentExpanded(final int flatParentPosition) {
                    updateExpandedParent(flatParentPosition, true);
                }

                @Override
                public void onParentCollapsed(int flatParentPosition) {
                    updateCollapsedParent(flatParentPosition, true);
                }
            });
        } else if (holder instanceof ChildExpandViewHolder){
            setListener(holder, position);
            final ChildExpandViewHolder childViewHolder = (ChildExpandViewHolder) holder;
            childViewHolder.mChild = getItem(position);
            childViewHolder.mExpandableAdapter = this;
        }
    }

    @Override
    @UiThread
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        if (mAttachedRecyclerViewPool != null && recyclerView != null)
            mAttachedRecyclerViewPool.add(recyclerView);
    }

    @Override
    @UiThread
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (mAttachedRecyclerViewPool != null && recyclerView != null)
            mAttachedRecyclerViewPool.remove(recyclerView);
    }

    @UiThread
    public void setExpandCollapseListener(@Nullable ExpandCollapseListener expandCollapseListener) {
        mExpandCollapseListener = expandCollapseListener;
    }

    @UiThread
    private void updateExpandedParent(int position, boolean expansionTriggeredByListItemClick) {
        final P parent = (P) getItem(position);
        mExpansionStateMap.put(parent, true);

        final List<C> childList = parent.getChildList();
        if (childList != null) {
            int childCount = childList.size();
            for (int i = 0; i < childCount; i++) {
                getItems().add(position + i + 1, (T) childList.get(i));
            }

            notifyItemRangeInserted(position + 1, childCount);
        }

        if (expansionTriggeredByListItemClick && mExpandCollapseListener != null) {
            mExpandCollapseListener.onParentExpanded(getNearestParentPosition(position));
        }
    }

    @UiThread
    private void updateCollapsedParent(int position, boolean collapseTriggeredByListItemClick) {
        final P parent = (P) getItem(position);
        mExpansionStateMap.put(parent, false);

        final List<C> childList = parent.getChildList();
        if (childList != null) {
            int childCount = childList.size();
            for (int i = childCount - 1; i >= 0; i--) {
                getItems().remove(position + i + 1);
            }

            notifyItemRangeRemoved(position + 1, childCount);
        }

        if (collapseTriggeredByListItemClick && mExpandCollapseListener != null) {
            mExpandCollapseListener.onParentCollapsed(getNearestParentPosition(position));
        }
    }

    @UiThread
    int getNearestParentPosition(int flatPosition) {
        if (flatPosition == 0) {
            return 0;
        }

        int parentCount = -1;
        for (int i = 0; i <= flatPosition; i++) {
            if (getItem(flatPosition) instanceof Parent) {
                parentCount++;
            }
        }
        return parentCount;
    }

    @UiThread
    int getChildPosition(@IntRange(from = 0) int flatPosition) {
        if (flatPosition == 0) {
            return 0;
        }

        int childCount = 0;
        for (int i = 0; i < flatPosition; i++) {
            if (!(getItem(flatPosition) instanceof Parent)) {
                childCount = 0;
            } else {
                childCount++;
            }
        }
        return childCount;
    }

    @UiThread
    public void expandParent(@NonNull P parent) {
        expandParent(mAllItems.indexOf(parent));
    }

    @UiThread
    public void expandParent(int parentPosition) {
        if (parentPosition < 0 || parentPosition >= getItemCount()) return;
        expandViews(parentPosition);
    }

    @UiThread
    public void expandParentRange(int startParentPosition, int parentCount) {
        int endParentPosition = startParentPosition + parentCount;
        for (int i = startParentPosition; i < endParentPosition; i++) {
            expandParent(i);
        }
    }

    @UiThread
    public void expandAllParents() {
        for (P parent : mAllItems) {
            expandParent(parent);
        }
    }

    @UiThread
    private void expandViews(int position) {
        for (RecyclerView recyclerView : mAttachedRecyclerViewPool) {
            final VH holder = (VH) recyclerView.findViewHolderForAdapterPosition(position);
            if (holder != null && holder instanceof ParentExpandViewHolder) {
                final ParentExpandViewHolder viewHolder = (ParentExpandViewHolder) holder;
                if (!viewHolder.isExpanded()) {
                    viewHolder.setExpanded(true);
                    viewHolder.onExpansionToggled(false);
                }
                updateExpandedParent(position, false);
            }
        }
    }

    @UiThread
    public void collapseParent(@NonNull P parent) {
        collapseParent(mAllItems.indexOf(parent));
    }

    @UiThread
    public void collapseParent(int parentPosition) {
        if (parentPosition < 0 || parentPosition >= getItemCount()) return;
        collapseParent(parentPosition);
    }

    @UiThread
    public void collapseParentRange(int startParentPosition, int parentCount) {
        int endParentPosition = startParentPosition + parentCount;
        for (int i = startParentPosition; i < endParentPosition; i++) {
            collapseParent(i);
        }
    }

    @UiThread
    public void collapseAllParents() {
        final int size = getItemCount();
        for (int i = 0; i < size; i++) {
            collapseParent(i);
        }
    }

    @UiThread
    private void collapseViews(int position) {
        for (RecyclerView recyclerView : mAttachedRecyclerViewPool) {
            final VH holder = (VH) recyclerView.findViewHolderForAdapterPosition(position);
            if (holder != null && holder instanceof ParentExpandViewHolder) {
                final ParentExpandViewHolder viewHolder = (ParentExpandViewHolder) holder;
                if (viewHolder.isExpanded()) {
                    viewHolder.setExpanded(false);
                    viewHolder.onExpansionToggled(true);
                }
                updateExpandedParent(position, false);
            }
        }
    }

    public interface ExpandCollapseListener {

        @UiThread
        void onParentExpanded(int parentPosition);

        @UiThread
        void onParentCollapsed(int parentPosition);
    }

}
