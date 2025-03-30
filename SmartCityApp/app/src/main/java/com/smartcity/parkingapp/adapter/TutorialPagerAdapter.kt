package com.smartcity.parkingapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.smartcity.parkingapp.R

/**
 * Adapter for the tutorial ViewPager2.
 * Handles the creation and binding of tutorial page views.
 */
class TutorialPagerAdapter(
    private val activity: AppCompatActivity,
    private val itemCount: Int
) : RecyclerView.Adapter<TutorialPagerAdapter.TutorialViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tutorial_page, parent, false)
        return TutorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: TutorialViewHolder, position: Int) {
        // We don't need to bind any data to the ViewHolder for this adapter
        // The parent activity will handle displaying content
        
        // Set accessibility description based on the page position
        val pageDescriptions = arrayOf(
            "Welcome page introducing Smart City Parking app",
            "Map feature page showing how to find parking spaces",
            "Payment feature page explaining how to pay for parking",
            "Notification feature page showing alerts and messages",
            "Getting started page with final instructions"
        )
        
        // Apply accessibility description to the item view
        if (position < pageDescriptions.size) {
            holder.itemView.contentDescription = pageDescriptions[position]
            holder.itemView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
    }

    override fun getItemCount(): Int = itemCount

    inner class TutorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
} 