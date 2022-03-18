package com.defendend.criminalintent

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import java.io.File

private const val ARG_FILE = "file"

class PhotoLargeFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return activity?.let {


            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)

            //get the layout inflater
            val inflater = requireActivity().layoutInflater

            //get a dialog picture view reference
            // Pass null as the parent view because its going in the dialog layout
            val view = inflater.inflate(R.layout.image_dialog_fragment, null)

            // Inflate and set the layout for the dialog
            builder.setView(view)

            //get reference to crimePicture image view
            val crimePicture = view.findViewById(R.id.imageScaledView) as ImageView

            //get the image file path argument
            val photoFilePath = arguments?.getSerializable(ARG_FILE) as String

            //get the scaled image
            val bitmap = getScaledBitmap(photoFilePath, requireActivity())

            //set the picture in the crimePicture view
            crimePicture.setImageBitmap(bitmap)


            //set the dialog characteristics
            builder.setNegativeButton(R.string.close_photo,
                DialogInterface.OnClickListener{ _, _ -> dialog?.cancel() } )


            // Create the AlertDialog object and return it
            builder.create()

        } ?: throw IllegalStateException("Activity cannot be null")

    }


    companion object {
        fun newInstance(photoFilePath : String) : PhotoLargeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_FILE, photoFilePath)
            }
            return PhotoLargeFragment().apply {
                arguments = args
            }
        }
    }
}