package com.habitrpg.android.habitica.ui.viewHolders.tasks

import android.view.View

import com.habitrpg.android.habitica.models.tasks.Task

import java.text.DateFormat

class TodoViewHolder(itemView: View) : ChecklistedViewHolder(itemView) {

    private val dateFormatter: DateFormat = android.text.format.DateFormat.getDateFormat(context)

    override fun bindHolder(newTask: Task, position: Int) {
        this.task = newTask
        if (newTask.completed) {
            checklistIndicatorWrapper.setBackgroundColor(taskGray)
        } else {
            checklistIndicatorWrapper.setBackgroundColor(newTask.lightTaskColor)
        }
        super.bindHolder(newTask, position)
    }

    override fun configureSpecialTaskTextView(task: Task) {
        if (task.dueDate != null) {
            this.specialTaskTextView?.text = dateFormatter.format(task.dueDate)
            this.specialTaskTextView?.visibility = View.VISIBLE
        } else {
            this.specialTaskTextView?.visibility = View.INVISIBLE
        }
    }

    override fun shouldDisplayAsActive(newTask: Task): Boolean {
        return !newTask.completed
    }
}
