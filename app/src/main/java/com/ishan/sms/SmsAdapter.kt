package com.ishan.sms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SmsAdapter(private val onClick: (Sms) -> Unit) : RecyclerView.Adapter<SmsAdapter.VH>() {

    private var items: List<Sms> = emptyList()

    fun submit(list: List<Sms>) { items = list; notifyDataSetChanged() }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val method: TextView = v.findViewById(R.id.tvMethod)
        val amount: TextView = v.findViewById(R.id.tvAmount)
        val trx: TextView = v.findViewById(R.id.tvTrx)
        val from: TextView = v.findViewById(R.id.tvFrom)
        val time: TextView = v.findViewById(R.id.tvTime)
        val state: TextView = v.findViewById(R.id.tvState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_sms, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val s = items[position]
        h.method.text = Ui.methodName(s.method)
        Ui.pill(h.method, "#FFFFFF", Ui.methodColor(s.method))
        h.time.text = Ui.time(s.time)

        if (s.isPayment) {
            h.amount.text = "৳ ${Ui.money(s.amount)}"
            h.trx.text = "TrxID: ${s.trx}"
            h.from.visibility = View.VISIBLE
            h.from.text = if (s.from.isEmpty()) "প্রেরক: অজানা" else "প্রেরক: ${s.from}"
            when {
                s.used -> { h.state.text = "ব্যবহৃত"; Ui.pill(h.state, "#1B7F55", "#CDF0E1") }
                !s.uploaded -> { h.state.text = "আপলোড বাকি"; Ui.pill(h.state, "#9A5B00", "#FFE9BF") }
                else -> { h.state.text = "অব্যবহৃত"; Ui.pill(h.state, "#C2255C", "#FFD9E3") }
            }
        } else {
            h.amount.text = ""
            h.trx.text = s.body
            h.from.visibility = View.GONE
            h.state.text = "অন্যান্য"
            Ui.pill(h.state, "#6B6785", "#ECE9F7")
        }
        h.itemView.setOnClickListener { onClick(s) }
    }
}
