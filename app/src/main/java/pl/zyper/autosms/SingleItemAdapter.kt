package pl.zyper.autosms

import android.content.Context
import android.widget.ArrayAdapter

class SingleItemAdapter<T>(mContext: Context, var item: T) :
    ArrayAdapter<T>(mContext, android.R.layout.simple_list_item_1) {

    override fun getCount(): Int {
        return 1
    }

    override fun getItem(position: Int): T? {
        return item
    }

    fun setAdapterItem(newItem : T)
    {
        item = newItem
        notifyDataSetChanged()
    }
}