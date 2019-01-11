package ru.crew.motley.dere.photo.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.dialog_save.view.*
import ru.crew.motley.dere.R



class ConfirmationDialog : DialogFragment() {

    interface DlgCallback {
        fun onConfirm()
        fun onCancel()
    }

    private lateinit var callback: DlgCallback

    companion object {

        fun newInstance(callback: DlgCallback): ConfirmationDialog {
            val dialog = ConfirmationDialog()
            dialog.callback = callback
            return dialog
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return activity!!.layoutInflater.inflate(R.layout.dialog_save, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.delete.setOnClickListener {
            callback.onConfirm()
            dismiss()
        }
        view.cancel.setOnClickListener {
            callback.onCancel()
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface?) {
        callback.onCancel()
        super.onDismiss(dialog)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog)
    }

}
