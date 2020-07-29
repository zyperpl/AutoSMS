package pl.zyper.autosms

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.isDigitsOnly
import androidx.recyclerview.widget.RecyclerView

class ContactsAdapter(
    private val mContext: Context,
    var map: MutableMap<Int, ContactModel>,
    private val imagePhoto: Drawable
) : RecyclerView.Adapter<ContactsAdapter.ViewHolder>(),
    View.OnClickListener,
    Filterable {
    var filteredMap: MutableMap<Int, ContactModel> = map
    var extraContact: ContactModel? = null

    fun setData(contacts: MutableMap<Int, ContactModel>) {
        map = contacts
        filteredMap = contacts
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                filterResults.values = if (constraint == null || constraint.isEmpty()) {
                    map
                } else {
                    val constraintLc = constraint.toString().toLowerCase()
                    map.filter {
                        it.value.name.toLowerCase().contains(constraintLc)
                                || it.value.number.contains(constraint)
                    }
                }

                return filterResults
            }

            override fun publishResults(constraint: CharSequence, results: FilterResults) {
                filteredMap = results.values as MutableMap<Int, ContactModel>

                extraContact = if (constraint.length >= 3 && constraint.isDigitsOnly()) {
                    ContactModel(constraint.toString(), constraint.toString(), null)
                } else {
                    null
                }

                notifyDataSetChanged()
            }
        }
    }

    var selectedPositions: MutableSet<Int> = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val v = LayoutInflater.from(mContext).inflate(R.layout.contact_item, parent, false)
        return ViewHolder(v, this)
    }

    override fun getItemCount(): Int {
        return filteredMap.size + (if (extraContact != null) 1 else 0)
    }

    fun getSelectedItems(): List<ContactModel> {
        val list = mutableListOf<ContactModel>()

        for (pos in selectedPositions) {
            if (pos == -1) {
                if (extraContact != null) {
                    list.add(extraContact!!)
                }
            } else {
                list.add(map.values.elementAt(pos))
            }
        }
        return list.toList()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val model =
            if (position >= filteredMap.size && extraContact != null)
                extraContact!!
            else filteredMap.values.elementAt(position)

        holder.name.text = model.name.ifBlank { model.number }
        holder.number.text = model.number
        holder.mapPosition = map.values.indexOf(model)

        var photo = model.photo

        if (photo == null) {
            photo = imagePhoto
        }

        if (photo is Drawable) {
            holder.icon.setImageDrawable(photo)
        } else if (photo is Bitmap) {
            holder.icon.setImageBitmap(photo)
        }

        holder.tick.alpha = 0.0f
        holder.tick.visibility = View.INVISIBLE

        if (holder.mapPosition in selectedPositions) {
            holder.tick.visibility = View.VISIBLE
            holder.tick.alpha = 1.0f
            //Log.d("ContactsAdapter", "holder.mapPosition in selectedPositions")
        }
        //Log.d("ContactsAdapter", "holder.tick=${holder.tick} \t alpha=${holder.tick.alpha}")
    }

    override fun onClick(v: View?) {
        val holder = (v!!.tag) as ViewHolder
        //Toast.makeText(mContext, "${holder.name.text} ${holder.number.text}", Toast.LENGTH_LONG).show()

        var newAdded = false

        if (holder.mapPosition == -1 && extraContact != null) {
            newAdded = true
            map[map.size] = extraContact!!
            holder.mapPosition = map.values.indexOf(extraContact!!)
            extraContact = null

            (mContext as TriggerCreationActivity).run {
                contactsSearch.run {
                    setQuery("", false)
                    isIconified = true
                    clearFocus()
                }
            }
        }

        if (holder.mapPosition in selectedPositions) {
            selectedPositions.remove(holder.mapPosition)
        } else {
            selectedPositions.add(holder.mapPosition)
        }

        Log.d("ContactsAdapter", "SelectedPosition=$selectedPositions")

        notifyDataSetChanged()

        if (newAdded) {
            (mContext as TriggerCreationActivity).run {
                Thread.sleep(100)
                runOnUiThread {
                    contactsList.run {
                        scrollToPosition(map.size - 1)
                    }
                }
            }
        }
    }

    fun selectItems(contacts: List<ContactModel>) {

        for (contact in contacts) {
            val pos =
                map.values.indexOfFirst { it.number.toPhoneNumber() == contact.number.toPhoneNumber() }
            if (pos >= 0) {
                selectedPositions.add(pos)
            } else {
                // not found
                map[map.size] = contact
                selectedPositions.add(map.values.indexOf(contact))
            }
        }

        notifyDataSetChanged()
    }

    class ViewHolder(view: View, clickListener: View.OnClickListener) :
        RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.contact_icon)
        val name = view.findViewById<TextView>(R.id.contact_name)!!
        val number = view.findViewById<TextView>(R.id.contact_number)!!
        val tick = view.findViewById<ImageView>(R.id.contact_selection_tick)!!

        var mapPosition: Int = -1

        init {
            view.tag = this
            view.setOnClickListener(clickListener)
        }
    }
}


