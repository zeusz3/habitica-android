package com.habitrpg.android.habitica.ui.fragments.tasks

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.habitrpg.android.habitica.R
import com.habitrpg.android.habitica.components.AppComponent
import com.habitrpg.android.habitica.data.ApiClient
import com.habitrpg.android.habitica.data.InventoryRepository
import com.habitrpg.android.habitica.data.TaskRepository
import com.habitrpg.android.habitica.data.UserRepository
import com.habitrpg.android.habitica.events.commands.AddNewTaskCommand
import com.habitrpg.android.habitica.helpers.RxErrorHandler
import com.habitrpg.android.habitica.helpers.TaskFilterHelper
import com.habitrpg.android.habitica.models.tasks.Task
import com.habitrpg.android.habitica.models.user.User
import com.habitrpg.android.habitica.modules.AppModule
import com.habitrpg.android.habitica.ui.activities.MainActivity
import com.habitrpg.android.habitica.ui.adapter.tasks.*
import com.habitrpg.android.habitica.ui.fragments.BaseFragment
import com.habitrpg.android.habitica.ui.helpers.SafeDefaultItemAnimator
import com.habitrpg.android.habitica.ui.viewHolders.tasks.BaseTaskViewHolder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.fragment_refresh_recyclerview.*
import org.greenrobot.eventbus.EventBus
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

open class TaskRecyclerViewFragment : BaseFragment(), View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {
    var recyclerAdapter: TaskRecyclerViewAdapter? = null
    @field:[Inject Named(AppModule.NAMED_USER_ID)]
    lateinit var userID: String
    @Inject
    lateinit var apiClient: ApiClient
    @Inject
    lateinit var taskFilterHelper: TaskFilterHelper
    @Inject
    lateinit var userRepository: UserRepository
    @Inject
    lateinit var inventoryRepository: InventoryRepository
    @Inject
    lateinit var taskRepository: TaskRepository

    internal var layoutManager: RecyclerView.LayoutManager? = null

    internal var classType: String? = null
    internal var user: User? = null
    private var mItemTouchCallback: ItemTouchHelper.Callback? = null

    internal val className: String
        get() = this.classType ?: ""

    // TODO needs a bit of cleanup
    private fun setInnerAdapter() {
        val adapter: RecyclerView.Adapter<*>? = when (this.classType) {
            Task.TYPE_HABIT -> {
                HabitsRecyclerViewAdapter(null, true, R.layout.habit_item_card, taskFilterHelper)
            }
            Task.TYPE_DAILY -> {
                DailiesRecyclerViewHolder(null, true, R.layout.daily_item_card, taskFilterHelper)
            }
            Task.TYPE_TODO -> {
                TodosRecyclerViewAdapter(null, true, R.layout.todo_item_card, taskFilterHelper)
            }
            Task.TYPE_REWARD -> {
                RewardsRecyclerViewAdapter(null, context, R.layout.reward_item_card, user)
            }
            else -> null
        }

        if (classType != Task.TYPE_REWARD) {
            allowReordering()
        }

        recyclerAdapter = adapter as? TaskRecyclerViewAdapter
        recyclerView.adapter = adapter

        recyclerAdapter?.errorButtonEvents?.subscribe(Consumer {
            taskRepository.syncErroredTasks().subscribe(Consumer {}, RxErrorHandler.handleEmptyError())
        }, RxErrorHandler.handleEmptyError())

        if (this.classType != null) {
            taskRepository.getTasks(this.classType ?: "", userID).firstElement().subscribe(Consumer { this.recyclerAdapter?.updateUnfilteredData(it)
                this.recyclerAdapter?.filter()
            }, RxErrorHandler.handleEmptyError())
        }
    }

    private fun allowReordering() {
        val itemTouchHelper = ItemTouchHelper(mItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        if (Task.TYPE_DAILY == classType) {
            if (user != null && user?.preferences?.dailyDueDefaultView == true) {
                taskFilterHelper.setActiveFilter(Task.TYPE_DAILY, Task.FILTER_ACTIVE)
            }
        } else if (Task.TYPE_TODO == classType) {
            taskFilterHelper.setActiveFilter(Task.TYPE_TODO, Task.FILTER_ACTIVE)
        }

        mItemTouchCallback = object : ItemTouchHelper.Callback() {
            private var fromPosition: Int? = null
            private var movingTaskID: String? = null

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (viewHolder != null) {
                    viewHolder.itemView.setBackgroundColor(Color.LTGRAY)
                    if (fromPosition == null) {
                        fromPosition = viewHolder.adapterPosition
                    }
                    if (movingTaskID == null && (viewHolder as? BaseTaskViewHolder)?.task?.isValid == true) {
                        movingTaskID = (viewHolder as? BaseTaskViewHolder)?.task?.id
                    }
                }
                refreshLayout.isEnabled = false
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                recyclerAdapter?.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
                //taskRepository.swapTaskPosition(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            //defines the enabled move directions in each state (idle, swiping, dragging).
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                return ItemTouchHelper.Callback.makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                        ItemTouchHelper.DOWN or ItemTouchHelper.UP)
            }

            override fun isItemViewSwipeEnabled(): Boolean = false

            override fun isLongPressDragEnabled(): Boolean = true

            override fun clearView(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                refreshLayout?.isEnabled = true

                val fromPosition = fromPosition
                val movingTaskID = movingTaskID
                if (fromPosition != null && movingTaskID != null) {
                    recyclerAdapter?.ignoreUpdates = true
                    taskRepository.updateTaskPosition(classType ?: "", movingTaskID, viewHolder.adapterPosition)
                            .delay(1, TimeUnit.SECONDS)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(Consumer { recyclerAdapter?.ignoreUpdates = false
                            recyclerAdapter?.notifyDataSetChanged()}, RxErrorHandler.handleEmptyError())
                }
                this.fromPosition = null
                this.movingTaskID = null
            }
        }
        if (savedInstanceState != null) {
            this.classType = savedInstanceState.getString(CLASS_TYPE_KEY, "")
        }

        return inflater.inflate(R.layout.fragment_refresh_recyclerview, container, false)
    }

    protected open fun getLayoutManager(context: Context?): LinearLayoutManager = LinearLayoutManager(context)

    override fun onDestroy() {
        userRepository.close()
        inventoryRepository.close()
        super.onDestroy()
    }

    override fun injectFragment(component: AppComponent) {
        component.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.adapter = recyclerAdapter as? RecyclerView.Adapter<*>
        recyclerAdapter?.filter()

        layoutManager = recyclerView.layoutManager

        if (layoutManager == null) {
            layoutManager = getLayoutManager(context)

            recyclerView.layoutManager = layoutManager
        }
        if (recyclerView.adapter == null) {
            this.setInnerAdapter()
        }

        val bottomPadding = (recyclerView.paddingBottom + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f, resources.displayMetrics)).toInt()
        recyclerView.setPadding(0, 0, 0, bottomPadding)
        recyclerView.itemAnimator = SafeDefaultItemAnimator()

        refreshLayout.setOnRefreshListener(this)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    refreshLayout?.isEnabled = (activity as MainActivity).isAppBarExpanded
                }
            }
        })

        if (this.classType != null) {
            when (this.classType) {
                Task.TYPE_HABIT -> {
                    this.emptyViewTitle.setText(R.string.empty_title_habits)
                    this.emptyViewDescription.setText(R.string.empty_description_habits)
                }
                Task.TYPE_DAILY -> {
                    this.emptyViewTitle.setText(R.string.empty_title_dailies)
                    this.emptyViewDescription.setText(R.string.empty_description_dailies)
                }
                Task.TYPE_TODO -> {
                    this.emptyViewTitle.setText(R.string.empty_title_todos)
                    this.emptyViewDescription.setText(R.string.empty_description_todos)
                }
                Task.TYPE_REWARD -> {
                    this.emptyViewTitle.setText(R.string.empty_title_rewards)
                }
            }
        }

        if (Task.TYPE_REWARD == className) {
            compositeSubscription.add(taskRepository.getTasks(this.className, userID)
                    .subscribe(Consumer { recyclerAdapter?.updateData(it) }, RxErrorHandler.handleEmptyError()))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CLASS_TYPE_KEY, this.classType)
    }

    override fun onClick(v: View) {
        val event = AddNewTaskCommand()
        event.taskType = this.classType

        EventBus.getDefault().post(event)
    }

    override val displayedClassName: String?
        get() = this.classType + super.displayedClassName

    override fun onRefresh() {
        refreshLayout.isRefreshing = true
        userRepository.retrieveUser(true, true)
                .doOnTerminate {
                    refreshLayout?.isRefreshing = false
                }.subscribe(Consumer { }, RxErrorHandler.handleEmptyError())
    }

    fun setActiveFilter(activeFilter: String) {
        taskFilterHelper.setActiveFilter(classType ?: "", activeFilter)
        recyclerAdapter?.filter()

        if (activeFilter == Task.FILTER_COMPLETED) {
            compositeSubscription.add(taskRepository.retrieveCompletedTodos(userID).subscribe(Consumer {}, RxErrorHandler.handleEmptyError()))
        }
    }

    companion object {
        private const val CLASS_TYPE_KEY = "CLASS_TYPE_KEY"

        fun newInstance(context: Context?, user: User?, classType: String): TaskRecyclerViewFragment {
            val fragment = TaskRecyclerViewFragment()
            fragment.retainInstance = true
            fragment.user = user
            fragment.classType = classType
            var tutorialTexts: List<String>? = null
            if (context != null) {
                when (fragment.classType) {
                    Task.TYPE_HABIT -> {
                        fragment.tutorialStepIdentifier = "habits"
                        tutorialTexts = Arrays.asList(context.getString(R.string.tutorial_overview),
                                context.getString(R.string.tutorial_habits_1),
                                context.getString(R.string.tutorial_habits_2),
                                context.getString(R.string.tutorial_habits_3),
                                context.getString(R.string.tutorial_habits_4))
                    }
                    Task.FREQUENCY_DAILY -> {
                        fragment.tutorialStepIdentifier = "dailies"
                        tutorialTexts = Arrays.asList(context.getString(R.string.tutorial_dailies_1),
                                context.getString(R.string.tutorial_dailies_2))
                    }
                    Task.TYPE_TODO -> {
                        fragment.tutorialStepIdentifier = "todos"
                        tutorialTexts = Arrays.asList(context.getString(R.string.tutorial_todos_1),
                                context.getString(R.string.tutorial_todos_2))
                    }
                }
            }

            if (tutorialTexts != null) {
                fragment.tutorialTexts = ArrayList(tutorialTexts)
            }
            fragment.tutorialCanBeDeferred = false

            return fragment
        }
    }
}
