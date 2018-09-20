package com.habitrpg.android.habitica.ui.fragments.inventory.stable

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.habitrpg.android.habitica.R
import com.habitrpg.android.habitica.components.AppComponent
import com.habitrpg.android.habitica.data.InventoryRepository
import com.habitrpg.android.habitica.extensions.notNull
import com.habitrpg.android.habitica.helpers.RxErrorHandler
import com.habitrpg.android.habitica.ui.adapter.inventory.MountDetailRecyclerAdapter
import com.habitrpg.android.habitica.ui.fragments.BaseMainFragment
import com.habitrpg.android.habitica.ui.helpers.MarginDecoration
import com.habitrpg.android.habitica.ui.helpers.SafeDefaultItemAnimator
import com.habitrpg.android.habitica.ui.helpers.bindView
import io.reactivex.functions.Consumer

import javax.inject.Inject

class MountDetailRecyclerFragment : BaseMainFragment() {

    @Inject
    internal lateinit var inventoryRepository: InventoryRepository

    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)
    var adapter: MountDetailRecyclerAdapter? = null
    var animalType: String? = null
    var animalGroup: String? = null
    internal var layoutManager: GridLayoutManager? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.usesTabLayout = false
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_recyclerview, container, false)
    }

    override fun onDestroy() {
        inventoryRepository.close()
        super.onDestroy()
    }

    override fun injectFragment(component: AppComponent) {
        component.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        layoutManager = GridLayoutManager(activity, 2)
        recyclerView.layoutManager = layoutManager
        recyclerView.addItemDecoration(MarginDecoration(activity))

        adapter = recyclerView.adapter as MountDetailRecyclerAdapter?
        if (adapter == null) {
            adapter = MountDetailRecyclerAdapter(null, true)
            adapter?.itemType = this.animalType
            recyclerView.adapter = adapter
            recyclerView.itemAnimator = SafeDefaultItemAnimator()
            this.loadItems()

            adapter?.getEquipFlowable()?.flatMap { key -> inventoryRepository.equip(user, "mount", key) }
                    ?.subscribe(Consumer { }, RxErrorHandler.handleEmptyError()).notNull { compositeSubscription.add(it) }
        }

        if (savedInstanceState != null) {
            this.animalType = savedInstanceState.getString(ANIMAL_TYPE_KEY, "")
        }

        view.post { setGridSpanCount(view.width) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ANIMAL_TYPE_KEY, this.animalType)
    }


    private fun setGridSpanCount(width: Int) {
        var spanCount = 0
        if (context != null && context!!.resources != null) {
            val itemWidth: Float = context!!.resources.getDimension(R.dimen.pet_width)

            spanCount = (width / itemWidth).toInt()
        }
        if (spanCount == 0) {
            spanCount = 1
        }
        layoutManager?.spanCount = spanCount
        layoutManager?.requestLayout()
    }

    private fun loadItems() {
        if (animalType != null && animalGroup != null) {
            inventoryRepository.getMounts(animalType!!, animalGroup!!).firstElement().subscribe(Consumer { adapter?.updateData(it) }, RxErrorHandler.handleEmptyError())
        }
    }

    override fun customTitle(): String {
        return if (!isAdded) {
            ""
        } else getString(R.string.mounts)
    }

    companion object {
        private val ANIMAL_TYPE_KEY = "ANIMAL_TYPE_KEY"
    }
}
