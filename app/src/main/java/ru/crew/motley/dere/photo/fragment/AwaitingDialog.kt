package ru.crew.motley.dere.photo.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment

class AwaitingDialog : DialogFragment() {

    private var onDismiss: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = activity
        return AlertDialog.Builder(activity)
                .setMessage(arguments!!.getString(ARG_MESSAGE))
                .setCancelable(false)
                .create()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        onDismiss?.invoke()
    }

    companion object {

        private const val ARG_MESSAGE = "message"
        private const val MESSAGE_VALUE = "Calibrating location..."

        fun newInstance(message: String? = null, dismissCallback: (() -> Unit)? = null): AwaitingDialog {
            val dialog = AwaitingDialog()
            val args = Bundle()
            if (message == null)
                args.putString(ARG_MESSAGE, MESSAGE_VALUE)
            else
                args.putString(ARG_MESSAGE, message)
            dialog.arguments = args
            dialog.onDismiss = dismissCallback
            return dialog
        }
    }
}