package ru.crew.motley.dere.photo.fragment


import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.dialog_comment.*
import ru.crew.motley.dere.R


class CommentingDialog : DialogFragment() {

    interface DlgCallback {
        fun onConfirm(comment: String)
        fun onCancel()
    }

    companion object {
        fun getInstance(
                comment: String,
                callback: DlgCallback) = CommentingDialog().apply {
            arguments = Bundle()
            arguments?.putString(ARG_COMMENT, comment)
            this.callback = callback
//            onEdit = editCallback
//            onDismiss = dismissCallback
        }

        private const val ARG_COMMENT = "photoComment"
    }

//    lateinit var onEdit: (String) -> Unit
//    lateinit var onDismiss: () -> Unit
    private lateinit var callback: DlgCallback

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return activity!!.layoutInflater.inflate(R.layout.dialog_comment, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        comment.setText(arguments?.getString(ARG_COMMENT))
        comment.setSelection(comment.text.length)
        saveComment.setOnClickListener {
            callback.onConfirm(comment.text.toString())
            dismiss()
        }
        cancelComment.setOnClickListener{
//            callback.onCancel()
            dismiss()
        }
//        dialog.setOnKeyListener { dialog, keyCode, event ->
//            if (keyCode == KeyEvent.KEYCODE_BACK) {
//                dismiss()
//                true
//            } else
//                true
//        }
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